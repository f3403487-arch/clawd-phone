package com.clawd.phone

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Достижение: прогресс берётся из счётчиков, награда — окрас или аксессуар. */
class Ach(
    val key: String,
    val title: String,
    val desc: String,
    val goal: Int,
    val reward: String?,
    val progress: (Context, Prefs) -> Int,
)

object Achievements {
    val all = listOf(
        // pc_* — то же самое, сделанное с Clawd на компе (общие ачивки)
        Ach("pet100", "Гладильщик", "Погладить 100 раз", 100, "окрас «Золотой»") { _, p -> p.i("pets_count") + p.i("pc_pets") },
        Ach("days7", "Неделя вместе", "7 дней вместе", 7, "цветок") { c, _ -> PetService.brain(c).daysTogether() },
        Ach("days30", "Месяц дружбы", "30 дней вместе", 30, "нимб") { c, _ -> PetService.brain(c).daysTogether() },
        Ach("eat50", "Обжора", "Съесть 50 кнопок", 50, "окрас «Лаймовый»") { _, p -> p.i("eaten_count") },
        Ach("clean10", "Чистюля", "Убрать 10 кучек", 10, "окрас «Призрак»") { _, p -> p.i("poops_cleaned") + p.i("pc_clean") },
        Ach("ball30", "Футболист", "30 ударов по мячу", 30, "пиратская повязка") { _, p -> p.i("ball_kicks") },
        Ach("pomo5", "Отличник", "5 помидорок учёбы", 5, "окрас «Ночной»") { _, p -> p.i("pomo_done") },
        Ach("feed20", "Кормилец", "Покормить 20 раз", 20, null) { c, _ -> PetService.brain(c).stat("feed") },
        Ach("bath10", "Банщик", "Помыть 10 раз", 10, null) { _, p -> p.i("bath_count") },
        Ach("tag5", "Ловкач", "5 побед в догонялках", 5, null) { _, p -> p.i("tag_wins") },
        Ach("seek10", "Сыщик", "Найти Clawd в прятках 10 раз", 10, null) { _, p -> p.i("seek_found") },
        Ach("remind5", "Ничего не забуду", "5 напоминаний", 5, null) { _, p -> p.i("remind_done") },
        Ach("throw50", "Метатель", "Бросить Clawd 50 раз", 50, null) { c, p -> PetService.brain(c).stat("throw") + p.i("pc_throws") },
    )

    fun done(prefs: Prefs): Set<String> = prefs.s("ach").split(',').filter { it.isNotEmpty() }.toSet()

    fun mark(prefs: Prefs, key: String) = prefs.put("ach", (done(prefs) + key).joinToString(","))
}

/** Погода с open-meteo.com (без ключа): город → координаты → текущая погода. */
object Weather {
    class Now(val kind: String, val temp: Int, val desc: String)

    private fun get(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "Clawd for Android")
            return JSONObject(c.inputStream.bufferedReader().readText())
        } finally {
            c.disconnect()
        }
    }

    fun fetch(prefs: Prefs): Now? = try {
        val city = prefs.s("city").trim()
        if (prefs.s("geo_city") != city || prefs.s("lat").isEmpty()) {
            val q = URLEncoder.encode(city, "UTF-8")
            val r = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=ru&format=json")
                .optJSONArray("results")?.optJSONObject(0)
            if (r != null) {
                prefs.put("lat", r.optDouble("latitude").toString())
                prefs.put("lon", r.optDouble("longitude").toString())
                prefs.put("geo_city", city)
            }
        }
        val lat = prefs.s("lat")
        val lon = prefs.s("lon")
        if (lat.isEmpty() || lon.isEmpty()) null else {
            val cur = get("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code")
                .getJSONObject("current")
            val code = cur.optInt("weather_code")
            val temp = Math.round(cur.optDouble("temperature_2m")).toInt()
            val (kind, desc) = when (code) {
                in 51..67, in 80..82 -> "rain" to "дождь"
                in 95..99 -> "rain" to "гроза"
                in 71..77, 85, 86 -> "snow" to "снег"
                45, 48 -> "clear" to "туман"
                in 1..3 -> "clear" to "облачно"
                else -> "clear" to "ясно"
            }
            val k = when {
                kind != "clear" -> kind
                temp >= 28 -> "heat"
                temp <= -5 -> "cold"
                else -> "clear"
            }
            Now(k, temp, desc)
        }
    } catch (_: Exception) {
        null
    }
}

/** Напоминания хранятся в настройках, чтобы пережить перезапуск. */
object Reminders {
    class Item(val at: Long, val text: String)

    fun load(prefs: Prefs): MutableList<Item> = try {
        val a = JSONArray(prefs.s("reminders").ifEmpty { "[]" })
        (0 until a.length()).mapNotNull { i ->
            a.optJSONObject(i)?.let { Item(it.optLong("t"), it.optString("x")) }
        }.sortedBy { it.at }.toMutableList()
    } catch (_: Exception) {
        ArrayList()
    }

    fun save(prefs: Prefs, items: List<Item>) {
        val a = JSONArray()
        items.forEach { a.put(JSONObject().put("t", it.at).put("x", it.text)) }
        prefs.put("reminders", a.toString())
    }
}

/** Скачать страницу и вытащить из неё текст (для пересказа ссылки). */
object Page {
    fun text(url: String, limit: Int = 6000): Pair<String, String>? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 12_000
            c.readTimeout = 12_000
            c.instanceFollowRedirects = true
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Clawd")
            val html = c.inputStream.bufferedReader().use { r ->
                val buf = CharArray(400_000)
                val n = r.read(buf)
                if (n > 0) String(buf, 0, n) else ""
            }
            val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.get(1)?.trim().orEmpty()
            val body = html
                .replace(Regex("<(script|style|noscript)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace(Regex("\\s+"), " ").trim()
            title to body.take(limit)
        } finally {
            c.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}
