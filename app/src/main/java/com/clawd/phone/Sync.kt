package com.clawd.phone

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Локальный сервер на 127.0.0.1:8766 — через него Clawd из приложения делится памятью
 * с Clawd из плагина Telegram и узнаёт о событиях PhotoSync.
 *
 *  POST /sync  (заголовок X-Clawd-Code = код из настроек) — обмен памятью, см. [Brain.syncImport]
 *  POST /event {"type": "send_start|send_done|recv", "name": "файл"} — без кода, только анимация
 *  GET  /ping — жив ли сервер
 *
 * Слушает только loopback, снаружи телефона недоступен.
 */
object SyncServer {
    const val PORT = 8766
    private const val MAX_BODY = 64 * 1024

    @Volatile
    private var socket: ServerSocket? = null

    fun code(prefs: Prefs): String {
        var c = prefs.s("sync_code")
        if (c.length != 6) {
            c = (100000 + Random.nextInt(900000)).toString()
            prefs.put("sync_code", c)
        }
        return c
    }

    @Synchronized
    fun start(context: Context) {
        if (socket != null) return
        val app = context.applicationContext
        val server = try {
            ServerSocket(PORT, 8, InetAddress.getByName("127.0.0.1"))
        } catch (_: Exception) {
            return
        }
        socket = server
        thread(name = "clawd-sync", isDaemon = true) {
            while (!server.isClosed) {
                val client = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                thread(isDaemon = true) { serve(app, client) }
            }
        }
    }

    @Synchronized
    fun stop() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    private fun serve(context: Context, client: Socket) {
        client.use { s ->
            try {
                s.soTimeout = 5000
                val input = BufferedInputStream(s.getInputStream())
                val head = readLine(input) ?: return
                val parts = head.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1].substringBefore('?')
                val headers = HashMap<String, String>()
                while (true) {
                    val line = readLine(input) ?: break
                    if (line.isEmpty()) break
                    val i = line.indexOf(':')
                    if (i > 0) headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
                }
                val len = headers["content-length"]?.toIntOrNull()?.coerceIn(0, MAX_BODY) ?: 0
                val body = ByteArray(len)
                var read = 0
                while (read < len) {
                    val n = input.read(body, read, len - read)
                    if (n < 0) break
                    read += n
                }
                val text = String(body, 0, read, Charsets.UTF_8)
                val (status, reply) = handle(context, method, path, headers, text)
                val out = reply.toString().toByteArray(Charsets.UTF_8)
                val resp = "HTTP/1.1 $status\r\nContent-Type: application/json; charset=utf-8\r\n" +
                    "Content-Length: ${out.size}\r\nConnection: close\r\n\r\n"
                s.getOutputStream().apply {
                    write(resp.toByteArray(Charsets.US_ASCII))
                    write(out)
                    flush()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun readLine(input: BufferedInputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString().trimEnd('\r')
            if (sb.length > 8192) return null
            sb.append(c.toChar())
        }
    }

    private fun handle(context: Context, method: String, path: String, headers: Map<String, String>, body: String): Pair<String, JSONObject> {
        val prefs = Prefs(context)
        val obj = try {
            if (body.isBlank()) JSONObject() else JSONObject(body)
        } catch (_: Exception) {
            return "400 Bad Request" to JSONObject().put("error", "bad json")
        }
        return when {
            path == "/ping" -> "200 OK" to JSONObject().put("ok", true).put("app", "clawd").put("sync", prefs.b("sync"))
            path == "/event" && method == "POST" -> {
                val type = obj.optString("type")
                val name = obj.optString("name").take(60)
                PetService.instance?.let { pet -> android.os.Handler(android.os.Looper.getMainLooper()).post { pet.onExternalEvent(type, name) } }
                "200 OK" to JSONObject().put("ok", true)
            }
            path == "/sync" && method == "POST" -> {
                if (!prefs.b("sync")) return "403 Forbidden" to JSONObject().put("error", "sync off")
                if (headers["x-clawd-code"] != code(prefs)) return "401 Unauthorized" to JSONObject().put("error", "bad code")
                val brain = PetService.brain(context)
                // голод: побеждает тот, кто кормил позже
                obj.optJSONObject("food")?.let { f ->
                    val ts = (f.optDouble("ts", 0.0) * 1000).toLong()
                    if (ts > prefs.l("food_ts")) {
                        prefs.put("food_level", (f.optDouble("level", 70.0) * 10).toInt().coerceIn(0, 1000))
                        prefs.put("food_ts", ts)
                    }
                }
                if (prefs.s("brain_name").isBlank()) obj.optString("name").takeIf { it.isNotBlank() }?.let { prefs.put("brain_name", it.take(30)) }
                if (prefs.s("brain_about").isBlank()) obj.optString("about").takeIf { it.isNotBlank() }?.let { prefs.put("brain_about", it.take(200)) }
                brain.syncImport(obj)
                brain.save()
                prefs.put("sync_last", System.currentTimeMillis())
                PetService.instance?.let { pet -> android.os.Handler(android.os.Looper.getMainLooper()).post { pet.onSynced() } }
                val reply = brain.syncExport()
                reply.put("food", JSONObject().put("level", PetService.foodLevel(prefs).toDouble()).put("ts", prefs.l("food_ts") / 1000.0))
                reply.put("name", prefs.s("brain_name")).put("about", prefs.s("brain_about"))
                "200 OK" to reply
            }
            else -> "404 Not Found" to JSONObject().put("error", "not found")
        }
    }

    // ---------- связь с Clawd на компе (по Wi-Fi) ----------

    /** Порт, на котором телефон ждёт события от компа (Clawd убежал в телефон / вернулся на комп). */
    const val LAN_PORT = 8768

    @Volatile
    private var lanSocket: ServerSocket? = null

    /** Слушает Wi-Fi: только POST /pc с тем же кодом синхронизации. */
    @Synchronized
    fun startLan(context: Context) {
        if (lanSocket != null) return
        val app = context.applicationContext
        val server = try {
            ServerSocket(LAN_PORT, 8)
        } catch (_: Exception) {
            return
        }
        lanSocket = server
        thread(name = "clawd-lan", isDaemon = true) {
            while (!server.isClosed) {
                val client = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                thread(isDaemon = true) { serveLan(app, client) }
            }
        }
    }

    @Synchronized
    fun stopLan() {
        try {
            lanSocket?.close()
        } catch (_: Exception) {
        }
        lanSocket = null
    }

    private fun serveLan(context: Context, client: Socket) {
        client.use { s ->
            try {
                s.soTimeout = 5000
                val input = BufferedInputStream(s.getInputStream())
                val head = readLine(input) ?: return
                val headers = HashMap<String, String>()
                while (true) {
                    val line = readLine(input) ?: break
                    if (line.isEmpty()) break
                    val i = line.indexOf(':')
                    if (i > 0) headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
                }
                val len = headers["content-length"]?.toIntOrNull()?.coerceIn(0, 8192) ?: 0
                val body = ByteArray(len)
                var read = 0
                while (read < len) {
                    val n = input.read(body, read, len - read)
                    if (n < 0) break
                    read += n
                }
                val prefs = Prefs(context)
                val ok = head.startsWith("POST /pc ") && prefs.b("pc_link") && headers["x-clawd-code"] == code(prefs)
                if (ok) {
                    val obj = try {
                        JSONObject(String(body, 0, read, Charsets.UTF_8))
                    } catch (_: Exception) {
                        JSONObject()
                    }
                    // запоминаем, где комп, чтобы слать ему уведомления
                    prefs.put("pc_ip", s.inetAddress.hostAddress ?: "")
                    prefs.put("pc_port", obj.optInt("port", 8769))
                    prefs.put("pc_seen", System.currentTimeMillis())
                    val event = obj.optString("event")
                    // общие ачивки: счётчики с компа и уже открытые там ачивки
                    obj.optJSONObject("stats")?.let { st ->
                        prefs.put("pc_pets", st.optInt("pets", prefs.i("pc_pets")))
                        prefs.put("pc_clean", st.optInt("cleaned", prefs.i("pc_clean")))
                        prefs.put("pc_throws", st.optInt("throws", prefs.i("pc_throws")))
                    }
                    obj.optJSONArray("ach")?.let { a ->
                        val ids = Achievements.all.map { it.key }.toSet()
                        for (i in 0 until a.length()) {
                            val id = a.optString(i)
                            if (id in ids && id !in Achievements.done(prefs)) Achievements.mark(prefs, id)
                        }
                    }
                    PetService.instance?.let { pet ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post { pet.onPcEvent(event) }
                    }
                }
                val out = if (ok) {
                    val brain = PetService.brain(context)
                    JSONObject()
                        .put("pets", prefs.i("pets_count"))
                        .put("cleaned", prefs.i("poops_cleaned"))
                        .put("throws", brain.stat("throw"))
                        .put("days", brain.daysTogether())
                        .put("ach", JSONArray(Achievements.done(prefs).toList()))
                        .toString().toByteArray(Charsets.UTF_8)
                } else ByteArray(0)
                val resp = if (ok) "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${out.size}\r\nConnection: close\r\n\r\n"
                else "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                s.getOutputStream().apply {
                    write(resp.toByteArray(Charsets.US_ASCII))
                    write(out)
                    flush()
                }
            } catch (_: Exception) {
            }
        }
    }

    /** Отправить событие на комп (уведомление). Вызывать не из главного потока. */
    fun sendToPc(context: Context, obj: JSONObject): Boolean {
        val prefs = Prefs(context)
        val ip = prefs.s("pc_ip")
        if (ip.isEmpty()) return false
        return try {
            val conn = java.net.URL("http://$ip:${prefs.i("pc_port")}/phone").openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 3000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Clawd-Code", code(prefs))
            conn.outputStream.use { it.write(obj.toString().toByteArray(Charsets.UTF_8)) }
            val ok = conn.responseCode == 200
            conn.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }

    /** Строки с событиями из другого Clawd для промпта. */
    fun lines(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        return (0 until minOf(a.length(), 8)).mapNotNull { a.optString(it).takeIf { s -> s.isNotBlank() }?.take(120) }
    }
}
