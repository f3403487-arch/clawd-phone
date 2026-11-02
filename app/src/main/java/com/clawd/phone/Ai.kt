package com.clawd.phone

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiConfig(val provider: Int, val key: String, val model: String, val url: String, val persona: String)

class AiHttpError(val code: Int, val body: String) : Exception("http $code")

class AiReply(val say: String?, val mood: String?, val action: String?, val remember: String?)

/** Запросы к нейросетям: Gemini, OpenAI-совместимые и Claude — тот же протокол, что в плагине. */
object Ai {
    val PROVIDERS = listOf("Google Gemini (бесплатно)", "OpenAI-совместимый", "Anthropic Claude")
    private const val CLAUDE_URL = "https://api.anthropic.com/v1/messages"
    private const val CLAUDE_MODEL = "claude-haiku-5-5"
    private const val CLAUDE_VERSION = "2023-06-01"
    private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta"
    private const val GEMINI_MODEL = "gemini-3.5-flash-lite"
    private const val OPENAI_URL = "https://api.openai.com/v1"
    private const val OPENAI_MODEL = "gpt-4o-mini"
    private const val TIMEOUT = 15_000
    private const val TIMEOUT_LOCAL = 30_000
    const val MAX_CHARS = 80

    fun raw(cfg: AiConfig, system: String, prompt: String, maxOut: Int = 300, json: Boolean = true): String = when (cfg.provider) {
        1 -> openai(cfg, system, prompt, maxOut)
        2 -> claude(cfg, system, prompt, maxOut)
        else -> gemini(cfg, system, prompt, maxOut, json)
    }

    private fun claude(cfg: AiConfig, system: String, prompt: String, maxOut: Int): String {
        val headers = mapOf("x-api-key" to cfg.key, "anthropic-version" to CLAUDE_VERSION)
        // Новые модели думают перед ответом, поэтому лимит с запасом, а усилие — минимальное.
        val body = JSONObject()
            .put("model", cfg.model.ifBlank { CLAUDE_MODEL })
            .put("max_tokens", maxOf(1024, maxOut * 4))
            .put("system", system)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            .put("output_config", JSONObject().put("effort", "low"))
        val data = try {
            http(CLAUDE_URL, headers, body, TIMEOUT * 2)
        } catch (e: AiHttpError) {
            if (e.code == 400 && "effort" in e.body.lowercase()) {
                body.remove("output_config")
                http(CLAUDE_URL, headers, body, TIMEOUT * 2)
            } else throw e
        }
        if (data.optString("stop_reason") == "refusal") return ""
        val content = data.optJSONArray("content") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val block = content.optJSONObject(i) ?: continue
            if (block.optString("type") == "text") sb.append(block.optString("text"))
        }
        return sb.toString()
    }

    private fun gemini(cfg: AiConfig, system: String, prompt: String, maxOut: Int, json: Boolean): String {
        val model = cfg.model.ifBlank { GEMINI_MODEL }.removePrefix("models/")
        val url = "$GEMINI_URL/models/$model:generateContent"
        val headers = mapOf("x-goog-api-key" to cfg.key)
        val gen = JSONObject()
            .put("maxOutputTokens", maxOut)
            .put("temperature", 1.0)
            .put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal"))
        if (json) gen.put("responseMimeType", "application/json")
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", gen)
        val data = try {
            http(url, headers, body, TIMEOUT)
        } catch (e: AiHttpError) {
            if (e.code == 400 && "thinking" in e.body.lowercase()) {
                gen.remove("thinkingConfig")
                http(url, headers, body, TIMEOUT)
            } else throw e
        }
        val parts = data.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            if (!p.optBoolean("thought")) sb.append(p.optString("text"))
        }
        return sb.toString()
    }

    private fun openai(cfg: AiConfig, system: String, prompt: String, maxOut: Int): String {
        var base = cfg.url.ifBlank { OPENAI_URL }.trim().trimEnd('/')
        if (!base.lowercase().startsWith("http://") && !base.lowercase().startsWith("https://")) base = "https://$base"
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val headers = if (cfg.key.isNotBlank()) mapOf("Authorization" to "Bearer ${cfg.key}") else emptyMap()
        val body = JSONObject()
            .put("model", cfg.model.ifBlank { OPENAI_MODEL })
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", prompt)),
            )
            .put("max_tokens", maxOut)
            .put("temperature", 0.95)
        if ("anthropic.com" in url.lowercase()) {
            // Claude через OpenAI-совместимый слой: новые модели отклоняют temperature < 1 и думают перед ответом.
            body.remove("temperature")
            body.put("max_tokens", maxOf(1024, maxOut * 4))
        }
        val timeout = if (url.lowercase().startsWith("http://")) TIMEOUT_LOCAL else TIMEOUT
        val data = http(url, headers, body, timeout)
        return data.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
    }

    private fun http(url: String, headers: Map<String, String>, body: JSONObject, timeout: Int): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = timeout
            conn.readTimeout = timeout
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Clawd for Android)")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val detail = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (_: Exception) {
                    ""
                }
                throw AiHttpError(code, detail)
            }
            return JSONObject(conn.inputStream.bufferedReader().readText())
        } finally {
            conn.disconnect()
        }
    }

    fun parse(raw: String?): AiReply {
        val text = (raw ?: "").trim()
        var say: String? = null
        var mood: String? = null
        var action: String? = null
        var remember: String? = null
        Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL).find(text)?.let { m ->
            try {
                val o = JSONObject(m.value)
                say = o.optString("say")
                mood = o.optString("mood").trim().lowercase()
                action = o.optString("action").trim().lowercase()
                remember = o.optString("remember").trim()
            } catch (_: Exception) {
                say = null
            }
        }
        if (say == null && "\"say\"" in text) {
            fun grab(key: String) = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.get(1)?.trim()
            say = grab("say") ?: ""
            mood = grab("mood")?.lowercase()
            action = grab("action")?.lowercase()
            remember = grab("remember")
        }
        if (say == null) say = if (text.trimStart().startsWith("{") || text.trimStart().startsWith("`")) "" else text
        if (mood !in Phrases.moods) mood = null
        if (action !in Phrases.actions) action = null
        return AiReply(cleanLine(say), mood, action, remember?.ifBlank { null })
    }

    fun cleanLine(text: String?, limit: Int = MAX_CHARS): String? {
        var t = (text ?: "").trim()
        if (t.isEmpty()) return null
        t = t.lines().first().trim().trim('"', '\'', '«', '»', '“', '”', '„', ' ')
        if (t.length > limit) t = t.take(limit - 1).trimEnd() + "…"
        return t.ifEmpty { null }
    }

    /** Ответ на задачу (вопрос, пересказ): без markdown, обрезанный. */
    fun cleanTask(text: String?, limit: Int = 600): String? {
        var t = (text ?: "").trim()
        t = t.replace(Regex("^```\\w*\\s*|```$"), "").replace("**", "").replace(Regex("(?m)^#+\\s*"), "").trim()
        if (t.length > limit) t = t.take(limit - 1).trimEnd() + "…"
        return t.ifEmpty { null }
    }
}
