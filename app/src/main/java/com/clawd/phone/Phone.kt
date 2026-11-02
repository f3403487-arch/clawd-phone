package com.clawd.phone

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent

/** Что Clawd умеет делать на телефоне по просьбе: системные действия, приложения, фонарик, звук, таймеры, нажатия. */
object Phone {

    class Step(val act: String, val arg: String = "") {
        override fun toString() = if (arg.isEmpty()) act else "$act($arg)"
    }

    val ACTIONS = setOf(
        "home", "back", "recents", "notifications", "quick_settings", "lock", "screenshot", "power_menu",
        "clear_recents", "kill_background", "open_app", "tap", "tap_rect", "type", "scroll_down", "scroll_up",
        "swipe_left", "swipe_right", "torch_on", "torch_off", "volume_up", "volume_down", "volume_max", "mute", "unmute",
        "media_play_pause", "media_next", "media_prev", "timer", "alarm", "wifi", "bluetooth", "internet",
        "settings", "battery", "search", "open_url", "wait",
        "remind", "ball", "tag", "seek", "bath", "sleep", "pomodoro", "pomodoro_stop", "translate",
        "tg_search", "tg_send",
    )

    // ---------- Telegram: «найди в тг …», «напиши Васе в тг …» ----------

    /** Telegram-клиенты, по порядку предпочтения (AyuGram первым — он у хозяина основной). */
    private val TG_PKGS = listOf(
        "com.radolyn.ayugram", "com.exteragram.messenger", "org.telegram.messenger", "org.telegram.messenger.web",
        "org.telegram.plus", "tw.nekomimi.nekogram", "org.thunderdog.challegram",
    )
    private val TG_WORD = Regex("(?<!\\p{L})(?:(?:в|во|через)\\s+)?(?:тг|телег\\p{L}*|телеграм\\p{L}*|telegram|аюграм\\p{L}*|ayugram)(?!\\p{L})", RegexOption.IGNORE_CASE)

    /** «Васе» → «Вас», «с Петей» → «Пет»: так поиск Telegram найдёт и «Вася», и «Петя». */
    private fun who(word: String): String {
        val w = word.trim(' ', ',', ':', '?', '!', '.')
        if (w.length >= 5 && w.takeLast(2).lowercase() in setOf("ей", "ой", "ом", "ем", "ам", "ям")) return w.dropLast(2)
        return if (w.length >= 4 && w.last().lowercaseChar() in "еуюиыя") w.dropLast(1) else w
    }

    private fun parseTg(orig: String): Step? {
        if (!TG_WORD.containsMatchIn(orig)) return null
        val clean = orig.replace(TG_WORD, " ").replace(Regex("\\s+"), " ").trim()
        Regex("^(?i)(?:напиши|отправь|скинь|ответь|передай)\\s+(.+)").find(clean)?.let { m ->
            val rest = m.groupValues[1].trim()
            val (to, msg) = if (':' in rest) rest.substringBefore(':') to rest.substringAfter(':') else {
                val parts = rest.split(Regex("\\s+"), 2)
                if (parts.size == 2 && Regex("(?i)[еуюи]$").containsMatchIn(parts[0]) &&
                    !Regex("(?i)^(что|привет|пока)$").matches(parts[0])
                ) parts[0] to parts[1] else "" to rest
            }
            val text = msg.trim().replace(Regex("^(?i)что\\s+"), "").trim(' ', '«', '»', '"')
            return Step("tg_send", "${who(to)}|$text")
        }
        Regex("^(?i)(?:найди|поищи|ищи|открой|зайди(?:\\s+в)?|покажи)\\s+(?:(?:чат|канал|переписку|диалог)\\s+(?:с\\s+)?)?(.+)").find(clean)?.let { m ->
            val q = m.groupValues[1].trim()
            return Step("tg_search", if (q.split(' ').size == 1) who(q) else q)
        }
        if (Regex("^(?i)(открой|зайди|запусти)$").matches(clean)) return Step("tg_search", "")
        return null
    }

    /** Открыть Telegram и вписать запрос в поиск; дальше — по цепочке, потому что приложению нужно время. */
    private fun tgSearch(ctx: Context, a: ClawdAccess, query: String, then: (Boolean) -> Unit) {
        val pm = ctx.packageManager
        val current = a.currentPkg
        val pkg = TG_PKGS.firstOrNull { it == current } ?: TG_PKGS.firstOrNull { pm.getLaunchIntentForPackage(it) != null }
        if (pkg == null) return then(false)
        if (pkg != current) launch(ctx, pm.getLaunchIntentForPackage(pkg)!!)
        handler.postDelayed({
            if (query.isEmpty()) return@postDelayed then(true)
            fun searchButton() = a.findByDesc("Search") ?: a.findByDesc("Поиск")
            val r = searchButton()
            if (r == null) {
                // открыт чат — шаг назад к списку чатов и ещё раз
                a.global(AccessibilityService.GLOBAL_ACTION_BACK)
                handler.postDelayed({
                    val r2 = searchButton() ?: return@postDelayed then(false)
                    a.tap(r2.exactCenterX(), r2.exactCenterY())
                    handler.postDelayed({ then(a.insertText(query)) }, 700)
                }, 800)
                return@postDelayed
            }
            a.tap(r.exactCenterX(), r.exactCenterY())
            handler.postDelayed({ then(a.insertText(query)) }, 700)
        }, if (pkg == current) 200 else 1600)
    }

    /** Найти чат, открыть первый результат и вставить текст. Отправку оставляем человеку — чтобы не улетело не туда. */
    private fun tgSend(ctx: Context, a: ClawdAccess, arg: String, done: (String) -> Unit) {
        val to = arg.substringBefore('|').trim()
        val msg = arg.substringAfter('|', "").trim()
        if (msg.isEmpty()) return done("А что написать?")
        fun typeIn() {
            val field = a.nodes().lastOrNull { it.editable }
            if (field != null) a.tap(field.rect.exactCenterX(), field.rect.exactCenterY())
            handler.postDelayed({
                done(if (a.insertText(msg)) "Вставил текст — проверь и нажми «отправить» ✉️" else "Не нашёл поле ввода 😿")
            }, 500)
        }
        if (to.isEmpty()) {
            tgSearch(ctx, a, "") { ok -> if (ok) handler.postDelayed({ typeIn() }, 300) else done("Не нашёл Telegram") }
            return
        }
        tgSearch(ctx, a, to) { ok ->
            if (!ok) return@tgSearch done("Не получилось открыть поиск в Telegram")
            handler.postDelayed({
                val stem = to.lowercase()
                val hit = a.nodes().firstOrNull { !it.editable && it.text?.lowercase()?.contains(stem) == true && it.rect.height() > 0 }
                if (hit == null) return@postDelayed done("Не нашёл «$to» в Telegram 🤔")
                a.tap(hit.rect.exactCenterX(), hit.rect.exactCenterY())
                handler.postDelayed({ typeIn() }, 1200)
            }, 1300)
        }
    }

    /** Шаги, которые выполняет сам Clawd (игры, напоминания, учёба). */
    private val PET_ACTIONS = setOf("remind", "ball", "tag", "seek", "bath", "sleep", "pomodoro", "pomodoro_stop", "translate")

    /** «через 20 минут», «через час», «через полчаса» → секунды. */
    private fun durationSec(num: String, unit: String?): Int? {
        val n = when (num) {
            "полчаса" -> return 1800
            "час" -> return 3600
            "минуту" -> return 60
            else -> num.toIntOrNull() ?: return null
        }
        val u = unit ?: "мин"
        return when {
            u.startsWith("сек") -> n
            u.startsWith("час") -> n * 3600
            else -> n * 60
        }
    }

    /** Напоминание: «напомни через 20 минут выключить чайник», «напомни в 18:30 позвонить маме». */
    private fun parseRemind(t: String, orig: String): Step? {
        if (!t.startsWith("напомни")) return null
        val head = Regex("^напомни(?:\\s+мне)?\\s+")
        val o = orig.replace(Regex("^(?i)напомни(?:\\s+мне)?\\s+"), "")
        val now = System.currentTimeMillis()
        Regex("^через\\s+(\\d+|полчаса|час|минуту)\\s*(сек\\S*|мин\\S*|час\\S*)?\\s*(.*)$").find(t.replace(head, ""))?.let { m ->
            val sec = durationSec(m.groupValues[1], m.groupValues[2].ifEmpty { null }) ?: return null
            val text = o.substring(o.length - m.groupValues[3].length).trim().ifEmpty { "ты просил напомнить" }
            return Step("remind", "${now + sec * 1000L}|$text")
        }
        Regex("^в\\s+(\\d{1,2})[:. ](\\d{2})\\s*(.*)$").find(t.replace(head, ""))?.let { m ->
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, m.groupValues[1].toInt())
            cal.set(java.util.Calendar.MINUTE, m.groupValues[2].toInt())
            cal.set(java.util.Calendar.SECOND, 0)
            if (cal.timeInMillis <= now) cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            val text = o.substring(o.length - m.groupValues[3].length).trim().ifEmpty { "ты просил напомнить" }
            return Step("remind", "${cal.timeInMillis}|$text")
        }
        Regex("^(.+?)\\s+через\\s+(\\d+|полчаса|час|минуту)\\s*(сек\\S*|мин\\S*|час\\S*)?$").find(t.replace(head, ""))?.let { m ->
            val sec = durationSec(m.groupValues[2], m.groupValues[3].ifEmpty { null }) ?: return null
            val text = o.take(m.groupValues[1].length).trim()
            return Step("remind", "${now + sec * 1000L}|$text")
        }
        return null
    }

    private const val NEED_ACCESS = "Включи мне спецвозможности — без них я не дотянусь"
    private val CLEAR_ALL = Regex("(очистить|закрыть|убрать|удалить)\\s+вс[её]|clear all|close all|dismiss all", RegexOption.IGNORE_CASE)
    private val handler = Handler(Looper.getMainLooper())
    private var torchOn = false

    // ---------- понимание простых команд без ИИ ----------

    /** Разобрать просьбу на шаги. null — не понял (тогда можно спросить ИИ). */
    fun parseLocal(text: String): List<Step>? {
        val orig = text.replace(Regex("[!?.]+$"), "").trim()
            .replace(Regex("^(?i)(пожалуйста|clawd|клод)[ ,]*"), "").trim()
        if (orig.isEmpty()) return null
        parseOne(norm(orig), orig)?.let { return listOf(it) }
        val parts = orig.split(Regex("\\s*(?:,|;|\\s+и потом\\s+|\\s+потом\\s+|\\s+затем\\s+|\\s+а потом\\s+|\\s+и\\s+)\\s*", RegexOption.IGNORE_CASE))
            .map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        return parts.map { parseOne(norm(it), it) ?: return null }
    }

    private fun norm(s: String) = s.lowercase().replace('ё', 'е')

    private fun parseOne(t: String, orig: String): Step? {
        fun has(vararg w: String) = w.any { it in t }
        fun re(p: String) = Regex(p).find(t)
        parseRemind(t, orig)?.let { return it }
        // про Telegram — ищем и пишем в нём, а не в интернете
        parseTg(orig)?.let { return it }
        return when {
            re("^(?:переведи|перевести)\\s+(.+)") != null ->
                Step("translate", orig.replace(Regex("^(?i)(переведи|перевести)\\s+"), ""))
            has("стоп помодоро", "хватит учиться", "закончим учиться", "выключи помодоро", "останови помодоро") -> Step("pomodoro_stop")
            has("помодоро", "давай учиться", "режим учебы", "будем учиться", "фокус-режим") || t == "учимся" -> Step("pomodoro")
            has("мяч", "мячик") -> Step("ball")
            has("догонялк", "поймай меня", "салки") -> Step("tag")
            has("прятки", "спрячься") -> Step("seek")
            has("помой", "помыть", "купаться", "искупай", "ванн") -> Step("bath")
            t == "спать" || has("иди спать", "ложись спать", "уложи", "баиньки", "в кроватку") -> Step("sleep")
            has("закрой все", "закрыть все", "закрой окна", "закрой фон", "очисти фон", "очисти недавн", "очисти память",
                "очисти оперативк", "выгрузи", "убери все прилож", "закрой прилож", "закрой вкладки приложений") ||
                (has("закр", "очист", "убер") && has("фон", "окна", "недавн", "прилож")) -> Step("clear_recents")
            has("фонар") -> Step(if (has("выкл", "отключ", "погас", "убери")) "torch_off" else if (has("вкл")) "torch_on" else if (torchOn) "torch_off" else "torch_on")
            has("скриншот", "снимок экрана", "скрин экрана") || t == "скрин" -> Step("screenshot")
            has("заблокир", "блокировк", "выключи экран", "погаси экран") -> Step("lock")
            has("домой", "на главн", "рабочий стол", "главный экран") -> Step("home")
            t == "назад" || has("вернись назад", "шаг назад", "нажми назад") -> Step("back")
            has("недавн", "многозадач") -> Step("recents")
            has("быстрые настройки", "быстрых настро") -> Step("quick_settings")
            has("уведомлен", "шторк") -> Step("notifications")
            has("меню питания", "выключи телефон", "перезагрузи") -> Step("power_menu")
            has("без звука", "выключи звук", "замолчи", "отключи звук", "убери звук") -> Step("mute")
            has("включи звук", "верни звук") -> Step("unmute")
            has("на максимум", "на всю", "максимальн") && has("громк", "звук", "сделай") -> Step("volume_max")
            has("громче", "прибавь", "погромче", "увеличь громк") -> Step("volume_up")
            has("тише", "убавь", "потише", "уменьши громк") -> Step("volume_down")
            has("листай вниз", "прокрути вниз", "пролистай вниз", "скролль вниз", "листай дальше") -> Step("scroll_down")
            has("листай вверх", "прокрути вверх", "пролистай вверх", "скролль вверх") -> Step("scroll_up")
            has("следующ", "переключи трек", "другой трек", "дальше") -> Step("media_next")
            has("предыдущ", "прошлый трек", "верни трек") -> Step("media_prev")
            has("пауз", "останови музык", "стоп музык", "продолжи музык", "включи музык", "останови трек", "продолжи трек") -> Step("media_play_pause")
            has("таймер") -> {
                val m = re("(\\d+)\\s*(сек|мин|час)") ?: return null
                val n = m.groupValues[1].toInt()
                val sec = when {
                    m.groupValues[2].startsWith("сек") -> n
                    m.groupValues[2].startsWith("час") -> n * 3600
                    else -> n * 60
                }
                Step("timer", sec.toString())
            }
            has("будильник", "разбуди") -> {
                val m = re("(\\d{1,2})[:. ](\\d{2})") ?: re("(\\d{1,2})\\s*(час|утра|вечера)")?.let { null }
                if (m != null) Step("alarm", "${m.groupValues[1]}:${m.groupValues[2]}") else {
                    val h = re("на\\s+(\\d{1,2})")?.groupValues?.get(1) ?: return null
                    Step("alarm", "$h:00")
                }
            }
            has("вай-фай", "вайфай", "wi-fi", "wifi", "вай фай") -> Step("wifi")
            has("блютуз", "bluetooth", "блютус") -> Step("bluetooth")
            has("интернет", "мобильные данные", "мобильный интернет") -> Step("internet")
            has("батаре", "аккумулятор") && has("открой", "покажи", "настрой") -> Step("battery")
            re("^(?:найди|загугли|погугли|поищи)(?: в интернете| в гугле)?\\s+(.+)") != null ->
                Step("search", re("^(?:найди|загугли|погугли|поищи)(?: в интернете| в гугле)?\\s+(.+)")!!.groupValues[1])
            re("^(?:нажми|тапни|кликни|ткни)(?: на)?\\s+(.+)") != null ->
                Step("tap", re("^(?:нажми|тапни|кликни|ткни)(?: на)?\\s+(.+)")!!.groupValues[1])
            re("^(?:напиши|введи|напечатай)\\s+(.+)") != null ->
                Step("type", orig.replace(Regex("^(?i)(напиши|введи|напечатай)\\s+"), ""))
            re("^(?:открой|запусти|включи|зайди в|перейди в|покажи)\\s+(.+)") != null -> {
                val what = re("^(?:открой|запусти|включи|зайди в|перейди в|покажи)\\s+(.+)")!!.groupValues[1].trim()
                if (what.startsWith("настройк")) Step("settings") else Step("open_app", what)
            }
            else -> null
        }
    }

    /** Сколько подождать после шага, прежде чем делать следующий. */
    fun pauseAfter(s: Step): Long = when (s.act) {
        "open_app", "search", "open_url" -> 1800
        "tg_search", "tg_send" -> 600
        "home", "back", "recents", "notifications", "quick_settings", "tap", "tap_rect", "settings", "wifi", "bluetooth", "internet" -> 900
        "clear_recents" -> 600
        else -> 400
    }

    // ---------- выполнение ----------

    fun exec(ctx: Context, s: Step, done: (String) -> Unit) {
        val a = ClawdAccess.instance
        fun global(action: Int, ok: String) {
            if (a == null) done(NEED_ACCESS) else done(if (a.global(action)) ok else "Не получилось")
        }
        if (s.act in PET_ACTIONS) {
            done(PetService.instance?.petAction(s.act, s.arg) ?: "Сначала включи Clawd")
            return
        }
        try {
            when (s.act) {
                "home" -> global(AccessibilityService.GLOBAL_ACTION_HOME, "Готово, ты дома")
                "back" -> global(AccessibilityService.GLOBAL_ACTION_BACK, "Назад!")
                "recents" -> global(AccessibilityService.GLOBAL_ACTION_RECENTS, "Вот недавние")
                "notifications" -> global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Вот уведомления")
                "quick_settings" -> global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, "Быстрые настройки")
                "lock" -> global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "Спокойной ночи, экран")
                "screenshot" -> global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "Щёлк! Скриншот готов")
                "power_menu" -> global(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, "Вот меню питания")
                "clear_recents" -> clearRecents(ctx, done)
                "kill_background" -> {
                    killBackground(ctx)
                    done("Выгрузил фоновые приложения")
                }
                "open_app" -> done(openApp(ctx, s.arg))
                "tap" -> done(
                    when {
                        a == null -> NEED_ACCESS
                        a.clickText(s.arg) -> "Нажал «${s.arg}»"
                        else -> "Не нашёл «${s.arg}» на экране"
                    },
                )
                "tap_rect" -> {
                    val p = s.arg.split(',').mapNotNull { it.trim().toIntOrNull() }
                    done(
                        when {
                            a == null -> NEED_ACCESS
                            p.size == 4 && a.clickRect(Rect(p[0], p[1], p[2], p[3])) -> ""
                            else -> "Не получилось нажать"
                        },
                    )
                }
                "type" -> done(if (a?.insertText(s.arg) == true) "Написал" else "Не нашёл поле ввода — тапни в него")
                "scroll_down" -> done(if (a?.scroll(true) == true) "" else NEED_ACCESS)
                "scroll_up" -> done(if (a?.scroll(false) == true) "" else NEED_ACCESS)
                "swipe_left", "swipe_right" -> {
                    val dm = ctx.resources.displayMetrics
                    val y = dm.heightPixels / 2f
                    val l = dm.widthPixels * 0.15f
                    val r = dm.widthPixels * 0.85f
                    val ok = if (s.act == "swipe_left") a?.swipe(r, y, l, y) else a?.swipe(l, y, r, y)
                    done(if (ok == true) "" else NEED_ACCESS)
                }
                "torch_on", "torch_off" -> done(torch(ctx, s.act == "torch_on"))
                "volume_up", "volume_down", "volume_max", "mute", "unmute" -> done(volume(ctx, s.act))
                "media_play_pause" -> done(media(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Пауза / играем"))
                "media_next" -> done(media(ctx, KeyEvent.KEYCODE_MEDIA_NEXT, "Следующий трек"))
                "media_prev" -> done(media(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Предыдущий трек"))
                "timer" -> {
                    val sec = s.arg.toIntOrNull() ?: 60
                    launch(
                        ctx,
                        Intent(AlarmClock.ACTION_SET_TIMER)
                            .putExtra(AlarmClock.EXTRA_LENGTH, sec)
                            .putExtra(AlarmClock.EXTRA_MESSAGE, "Clawd")
                            .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
                    )
                    done("Таймер на ${human(sec)} ⏱")
                }
                "alarm" -> {
                    val p = s.arg.split(':')
                    val h = p.getOrNull(0)?.trim()?.toIntOrNull() ?: 7
                    val m = p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                    launch(
                        ctx,
                        Intent(AlarmClock.ACTION_SET_ALARM)
                            .putExtra(AlarmClock.EXTRA_HOUR, h)
                            .putExtra(AlarmClock.EXTRA_MINUTES, m)
                            .putExtra(AlarmClock.EXTRA_MESSAGE, "Clawd")
                            .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
                    )
                    done("Будильник на %d:%02d ⏰".format(h, m))
                }
                "wifi" -> done(if (launch(ctx, Intent(Settings.Panel.ACTION_WIFI))) "Вот Wi-Fi" else "Не открылось")
                "internet" -> done(if (launch(ctx, Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))) "Вот интернет" else "Не открылось")
                "bluetooth" -> done(if (launch(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) "Вот Bluetooth" else "Не открылось")
                "settings" -> done(if (launch(ctx, Intent(Settings.ACTION_SETTINGS))) "Открываю настройки" else "Не открылось")
                "battery" -> done(if (launch(ctx, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))) "Вот батарея" else "Не открылось")
                "search" -> {
                    val ok = launch(ctx, Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, s.arg)) ||
                        launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(s.arg))))
                    done(if (ok) "Ищу «${s.arg}»" else "Не открылось")
                }
                "open_url" -> {
                    val url = if (s.arg.startsWith("http")) s.arg else "https://${s.arg}"
                    done(if (launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) "Открываю" else "Не открылось")
                }
                "wait" -> handler.postDelayed({ done("") }, ((s.arg.toFloatOrNull() ?: 1f).coerceIn(0.2f, 8f) * 1000).toLong())
                "tg_search" -> if (a == null) done(NEED_ACCESS) else tgSearch(ctx, a, s.arg.trim()) { ok ->
                    done(
                        when {
                            !ok -> "Не получилось открыть поиск в Telegram 😿"
                            s.arg.isBlank() -> "Открыл Telegram"
                            else -> "Ищу «${s.arg.trim()}» в Telegram 🔎"
                        },
                    )
                }
                "tg_send" -> if (a == null) done(NEED_ACCESS) else tgSend(ctx, a, s.arg, done)
                else -> done("Такое я не умею: ${s.act}")
            }
        } catch (e: Exception) {
            done("Не получилось: ${e.message?.take(60) ?: "ошибка"}")
        }
    }

    private fun human(sec: Int) = when {
        sec % 3600 == 0 -> "${sec / 3600} ч"
        sec % 60 == 0 -> "${sec / 60} мин"
        else -> "$sec сек"
    }

    private fun launch(ctx: Context, intent: Intent): Boolean = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }

    /** Открыть недавние, нажать «Очистить всё» и выгрузить фон из памяти. */
    private fun clearRecents(ctx: Context, done: (String) -> Unit) {
        val a = ClawdAccess.instance
        if (a == null) {
            killBackground(ctx)
            done("Без спецвозможностей могу только выгрузить фон из памяти — сделал")
            return
        }
        a.global(AccessibilityService.GLOBAL_ACTION_RECENTS)
        fun attempt(i: Int) {
            if (a.clickRegex(CLEAR_ALL)) {
                handler.postDelayed({
                    killBackground(ctx)
                    done("Закрыл все окна ✨")
                }, 700)
                return
            }
            if (i >= 4) {
                killBackground(ctx)
                a.global(AccessibilityService.GLOBAL_ACTION_HOME)
                done("Кнопку «Очистить всё» не нашёл, но фон из памяти выгрузил")
                return
            }
            // в некоторых оболочках кнопка появляется только в конце списка — листаем
            val dm = ctx.resources.displayMetrics
            a.swipe(dm.widthPixels * 0.2f, dm.heightPixels * 0.45f, dm.widthPixels * 0.9f, dm.heightPixels * 0.45f)
            handler.postDelayed({ attempt(i + 1) }, 650)
        }
        handler.postDelayed({ attempt(0) }, 1100)
    }

    fun killBackground(ctx: Context) {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val pm = ctx.packageManager
        val pkgs = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }.toSet() - ctx.packageName
        for (p in pkgs) {
            try {
                am.killBackgroundProcesses(p)
            } catch (_: Exception) {
            }
        }
    }

    private val ALIASES = mapOf(
        "телеграм" to "telegram", "телегу" to "telegram", "телега" to "telegram", "тг" to "telegram",
        "ютуб" to "youtube", "ютюб" to "youtube", "ватсап" to "whatsapp", "вотсап" to "whatsapp",
        "инстаграм" to "instagram", "инсту" to "instagram", "инста" to "instagram", "тикток" to "tiktok",
        "хром" to "chrome", "браузер" to "chrome", "вк" to "vk", "вконтакте" to "vk", "спотифай" to "spotify",
        "дискорд" to "discord", "яндекс" to "yandex", "карты" to "maps", "почту" to "mail", "почта" to "mail",
        "галерею" to "gallery", "галерея" to "gallery", "фото" to "photos", "плей маркет" to "play store",
        "маркет" to "play store", "калькулятор" to "calculator", "часы" to "clock", "календарь" to "calendar",
        "экстраграм" to "extera", "экстераграм" to "extera", "камеру" to "camera", "камера" to "camera",
    )

    fun openApp(ctx: Context, name: String): String {
        val q = name.lowercase().trim().removePrefix("приложение").trim().trim('«', '»', '"')
        if (q.isEmpty()) return "Какое приложение открыть?"
        val variants = LinkedHashSet<String>().apply {
            add(q)
            ALIASES[q]?.let { add(it) }
            if (q.length > 4 && q.last() in "уюаы") add(q.dropLast(1))
        }
        val pm = ctx.packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        var best: Pair<String, String>? = null
        var bestScore = 0
        for (ri in apps) {
            val label = ri.loadLabel(pm).toString()
            val l = label.lowercase()
            val pkg = ri.activityInfo.packageName.lowercase()
            var score = 0
            for (v in variants) {
                score = maxOf(
                    score,
                    when {
                        l == v -> 100
                        l.startsWith(v) -> 80
                        v in l -> 60
                        v.replace(" ", "") in pkg -> 40
                        else -> 0
                    },
                )
            }
            if (score > bestScore) {
                bestScore = score
                best = ri.activityInfo.packageName to label
            }
        }
        val (pkg, label) = best ?: return "Не нашёл приложение «$name»"
        val intent = pm.getLaunchIntentForPackage(pkg) ?: return "«$label» не открывается"
        return if (launch(ctx, intent)) "Открываю $label" else "«$label» не открылось"
    }

    private fun torch(ctx: Context, on: Boolean): String {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return "У телефона нет фонарика"
        cm.setTorchMode(id, on)
        torchOn = on
        return if (on) "Свечу! 🔦" else "Фонарик выключен"
    }

    private fun volume(ctx: Context, act: String): String {
        val am = ctx.getSystemService(AudioManager::class.java)
        val s = AudioManager.STREAM_MUSIC
        val ui = AudioManager.FLAG_SHOW_UI
        return when (act) {
            "volume_up" -> {
                repeat(2) { am.adjustStreamVolume(s, AudioManager.ADJUST_RAISE, ui) }
                "Погромче!"
            }
            "volume_down" -> {
                repeat(2) { am.adjustStreamVolume(s, AudioManager.ADJUST_LOWER, ui) }
                "Потише…"
            }
            "volume_max" -> {
                am.setStreamVolume(s, am.getStreamMaxVolume(s), ui)
                "На всю!"
            }
            "mute" -> {
                am.adjustStreamVolume(s, AudioManager.ADJUST_MUTE, ui)
                "Тсс…"
            }
            else -> {
                am.adjustStreamVolume(s, AudioManager.ADJUST_UNMUTE, ui)
                "Звук вернул"
            }
        }
    }

    private fun media(ctx: Context, code: Int, text: String): String {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return text
    }
}
