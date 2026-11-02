package com.clawd.phone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Настройки Clawd — тот же набор и вид, что у плагина в exteraGram. */
class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var ui: SettingsBuilder
    private var waitingOverlay = false
    private var forgetArmed = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ui = SettingsBuilder(this, root, prefs)
        val t = ui.t
        window.statusBarColor = t.cell
        window.navigationBarColor = t.gray
        if (!t.isDark) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        build()

        val bar = TextView(this).apply {
            text = "CLAWD"
            textSize = 22f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(t.accent)
            setShadowLayer(0.01f, ui.dp(2).toFloat(), ui.dp(2).toFloat(), t.shadow)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ui.dp(21), 0, ui.dp(21), 0)
            // шапка с пиксельной полосой снизу
            background = android.graphics.drawable.LayerDrawable(
                arrayOf(
                    android.graphics.drawable.ColorDrawable(t.divider),
                    android.graphics.drawable.ColorDrawable(t.cell),
                ),
            ).apply { setLayerInset(1, 0, 0, 0, ui.dp(3)) }
        }
        settingsPage = ScrollView(this).apply { addView(root) }
        wardrobePage = WardrobeView(this, prefs).apply { visibility = View.GONE }
        val pages = android.widget.FrameLayout(this).apply {
            addView(settingsPage)
            addView(wardrobePage)
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.gray)
            addView(bar, LinearLayout.LayoutParams(-1, ui.dp(56)))
            addView(tabBar(), LinearLayout.LayoutParams(-1, ui.dp(50)))
            addView(pages, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        setContentView(page)
        if (intent?.getBooleanExtra("wardrobe", false) == true) showTab(1)
    }

    private lateinit var settingsPage: ScrollView
    private lateinit var wardrobePage: WardrobeView
    private val tabViews = ArrayList<TextView>()

    /** Две вкладки под шапкой: «Настройки» и «Гардероб». */
    private fun tabBar(): LinearLayout {
        val t = ui.t
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(t.cell)
            setPadding(ui.dp(12), ui.dp(6), ui.dp(12), ui.dp(6))
        }
        for ((i, name) in listOf("⚙ НАСТРОЙКИ", "👕 ГАРДЕРОБ").withIndex()) {
            val tab = TextView(this).apply {
                text = name
                textSize = 14f
                letterSpacing = 0.06f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                gravity = Gravity.CENTER
                setOnClickListener { showTab(i) }
            }
            tabViews.add(tab)
            row.addView(tab, LinearLayout.LayoutParams(0, -1, 1f).apply { if (i == 0) marginEnd = ui.dp(6) })
        }
        styleTabs(0)
        return row
    }

    private fun styleTabs(on: Int) {
        val t = ui.t
        val d = resources.displayMetrics.density
        tabViews.forEachIndexed { i, v ->
            v.setTextColor(if (i == on) 0xFF1A0F0A.toInt() else t.sub)
            v.background = if (i == on) PixelDrawable(t.accent2, null, null, step = 3 * d) else null
        }
    }

    private fun showTab(i: Int) {
        styleTabs(i)
        settingsPage.visibility = if (i == 0) View.VISIBLE else View.GONE
        wardrobePage.visibility = if (i == 1) View.VISIBLE else View.GONE
        if (i == 1) wardrobePage.refresh() else ui.refresh()
    }

    override fun onResume() {
        super.onResume()
        if (waitingOverlay && Settings.canDrawOverlays(this)) {
            waitingOverlay = false
            enable()
        }
        if (prefs.enabled && Settings.canDrawOverlays(this) && PetService.instance == null) PetService.start(this)
        ui.refresh()
    }

    private fun pet(action: (PetService) -> Unit) {
        val s = PetService.instance
        if (s == null) Toast.makeText(this, "Сначала включи питомца", Toast.LENGTH_SHORT).show() else action(s)
    }

    private fun build() {
        val brain = PetService.brain(this)
        ui.custom(PreviewView(this, prefs).apply { layoutParams = LinearLayout.LayoutParams(-1, ui.dp(130)) })

        ui.header("Питомец")
        ui.switch(
            "enabled", "Показывать питомца", "Живёт поверх всех приложений. Спрятать можно из уведомления", "pet",
            get = { prefs.enabled && Settings.canDrawOverlays(this) },
            set = { on ->
                if (!on) {
                    prefs.enabled = false
                    PetService.stop(this)
                } else if (Settings.canDrawOverlays(this)) enable() else {
                    waitingOverlay = true
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            },
        )
        ui.selector("size", "Размер", PetService.SIZE_ITEMS, "size")
        ui.selector(
            "color", "Окрас",
            Sprite.colors.mapIndexed { i, c ->
                if (Sprite.colorOpen(i, Achievements.done(prefs))) c.name
                else "${c.name} 🔒 ${Wardrobe.needText(Sprite.colorNeed[i])}"
            },
            "color",
        )
        ui.text("Гардероб", "Шапки, очки, крылья, узоры, ауры — больше сотни вещей", "crown", accent = true) { showTab(1) }

        ui.header("Разрешения")
        ui.text("Поверх других приложений", icon = "screen", sub = { if (Settings.canDrawOverlays(this)) "✓ Разрешено" else "Нажми, чтобы разрешить" }) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        ui.text(
            "Спецвозможности", icon = "eye",
            sub = {
                if (ClawdAccess.enabled(this)) "✓ Видит экран: приложения, кнопки, текст, клавиатуру"
                else "Нужно, чтобы видеть экран, стоять на кнопках и есть их"
            },
        ) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        ui.text("Доступ к уведомлениям", icon = "bell", sub = { if (listenerAllowed()) "✓ Видит, кто пишет и что играет" else "Нужно, чтобы реагировать на сообщения и музыку" }) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        ui.text("Не усыплять в фоне", icon = "battery", sub = {
            if (getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)) "✓ Система его не выключит"
            else "Иначе Android может убрать Clawd с экрана"
        }) {
            @SuppressLint("BatteryLife")
            val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            startActivity(i)
        }
        ui.divider(
            "Если Android пишет «Доступ запрещён»: Настройки → Приложения → Clawd → ⋮ → «Разрешить ограниченные настройки», " +
                "потом включи снова. В спецвозможностях Clawd в списке «Скачанные приложения».",
        )

        ui.header("Поведение")
        ui.switch("gravity", "Гравитация", "Падает вниз, стоит на кнопках, тексте и клавиатуре, съезжает при наклоне", "gravity")
        ui.switch("rotate", "Поворачивается с телефоном", "Повернёшь телефон с выключенным автоповоротом — Clawd переберётся на край, который внизу", "rotate")
        ui.switch("wander", "Гуляет сам", "Ходит, прыгает, машет, а от скуки прячется за край экрана", "walk")
        ui.switch("phrases", "Разговаривает", "Иногда показывает реплики в облачке", "talk")
        ui.selector("sleep", "Засыпает", PetService.SLEEP_ITEMS, "sleep")
        ui.switch("eat", "Ест интерфейс", "Иногда съедает кнопки и иконки в приложениях. Тапни его — выплюнет обратно. Нужны спецвозможности", "food")
        ui.switch("poop", "Какает", "После 3–5 съеденных кнопок оставляет кучку. Тапни её — уберёшь", "poop")
        ui.switch("shake", "Чувствует тряску", "Потряси телефон — Clawd полетит кувырком. Использует акселерометр", "shake")
        ui.switch("events", "Реагирует на телефон", "Уведомления и разблокировка", "bell")
        ui.switch(
            "battery", "Следит за зарядом",
            "20% — просит зарядку, 10% — грустит, 5% — паникует и носится. Радуется зарядке, на 100% скажет, что можно отключать",
            "battery",
        )
        ui.switch("notify", "Реагирует на сообщения", "«Маша пишет!», «привет», «люблю», «ахаха» в уведомлениях", "talk")
        ui.switch("dance", "Танцует под музыку", "Пока на телефоне играет музыка", "music")
        ui.switch("haptics", "Вибрация", null, "vibro")
        ui.switch(
            "moods_chat", "Настроение по тексту",
            "Пугается ругани, грустит от грустного, удивляется «вау» — в уведомлениях, на экране и в том, что печатаешь. Всё на телефоне",
            "heart",
        )

        ui.header("Экран")
        ui.switch("screen_react", "Замечает приложения", "Комментирует, что ты открыл: видео, игры, мессенджеры, камеру…", "eye")
        ui.switch("screen_text", "Читает экран", "Замечает смешное, грустное и ругань в видимом тексте. Только на телефоне", "text")
        ui.switch("screen_time", "Следит за временем", "Каждые 40 минут в одном приложении предлагает перерыв", "clock")
        ui.switch(
            "merge", "Сливается с Clawd из Telegram",
            "В exteraGram / AyuGram с плагином Clawd+ (2.0-plus.3 и новее) влетает в того Clawd, а при выходе отклеивается и выпрыгивает обратно",
            "buddy",
        )
        ui.switch("hide_fullscreen", "Прячется в играх и видео", "Уходит, когда игра или видео на весь экран (горизонтально)", "game")
        ui.divider("Всё это работает через спецвозможности. Тексты с экрана никуда не отправляются, пока ты сам не разрешишь это для ИИ ниже.")

        ui.header("Голод")
        ui.switch("hunger", "Хочет есть", "Как тамагочи: со временем голодает, просит рыбку, а голодный чаще ест кнопки и обижается", "food")
        ui.text("Сытость", icon = "stats", sub = { "${PetService.foodLevel(prefs).roundToInt()}%" })
        ui.text("Покормить", icon = "food", accent = true) { pet { it.feed() } }

        ui.header("Состояние")
        ui.text("Настроение", icon = "heart", sub = { "${prefs.i("mood_level") / 10}% — растёт от ласки, игр и еды" })
        ui.text("Энергия", icon = "battery", sub = { "${prefs.i("energy_level") / 10}% — копится во сне" })
        ui.text("Чистота", icon = "star", sub = { "${prefs.i("clean_level") / 10}% — грязный начинает пахнуть" })
        ui.text("Помыть", icon = "star", accent = true) { pet { it.bath() } }
        ui.text("Уложить спать", icon = "sleep", accent = true) { pet { it.goToBed() } }

        ui.header("Игры")
        ui.text("Мячик", "Кидай и подбрасывай пальцем — Clawd гоняет и пинает", "target", accent = true) { pet { it.startBall() } }
        ui.text("Догонялки", "Поймай его 3 раза за 20 секунд", "walk", accent = true) { pet { it.startTag() } }
        ui.text("Прятки", "Спрячется за кнопкой или краем экрана — найди и тапни", "eye", accent = true) { pet { it.startSeek() } }

        ui.header("Уголок и время суток")
        ui.switch("home", "Свой уголок", "Кроватка, миска и мячик внизу экрана. Тап по миске — покормить, по кровати — уложить, по мячу — играть", "pet")
        ui.divider("С 22 до 7 Clawd в ночном колпаке, бормочет во сне, утром потягивается и здоровается.")

        ui.header("Погода")
        ui.switch("weather", "Чувствует погоду", "В дождь — с зонтом, в снег и мороз — в шарфе, в жару обмахивается. Данные с open-meteo.com", "color")
        ui.input("city", "Город", "Например: Москва", "target")

        ui.header("Учёба и напоминания")
        ui.text("Помодоро", icon = "clock", accent = true, sub = {
            "25 минут учимся (Clawd тихо сидит в очках и ругается на соцсети), потом перерыв"
        }) { pet { it.startPomodoro() } }
        ui.text("Остановить помодоро", icon = "clock") { pet { it.stopPomodoro() } }
        ui.text("Напоминания", icon = "bell", sub = {
            Reminders.load(prefs).joinToString("\n") {
                SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(it.at)) + " — " + it.text
            }.ifEmpty { "Нет. Скажи «напомни через 20 минут выключить чайник»" }
        }) { editReminders() }
        ui.switch("tts", "Читает сообщения вслух", "Clawd озвучивает входящие сообщения (кроме беззвучного режима)", "talk")

        ui.header("Достижения")
        for (a in Achievements.all) {
            ui.text(a.title, icon = "star", sub = {
                val done = a.key in Achievements.done(prefs)
                val p = a.progress(this, prefs).coerceAtMost(a.goal)
                (if (done) "✓ " else "$p/${a.goal} · ") + a.desc + (a.reward?.let { " → $it" } ?: "")
            })
        }

        ui.header("Дружок")
        ui.switch("buddy", "Второй питомец", "Маленький Clawd бегает за большим, болтает с ним и радуется тапам. Зажми его — откроется «Попросить»", "buddy")
        ui.input("buddy_name", "Имя дружка", null, "edit")
        ui.selector("buddy_color", "Окрас дружка", Sprite.colors.map { it.name }, "color")
        ui.text("Пусть поболтают", icon = "talk", accent = true) { pet { it.startDuet(force = true) } }

        ui.header("Помощник")
        ui.switch(
            "commands", "Команды .clawd",
            "Напиши в любом поле ввода «.clawd …» и подожди пару секунд — Clawd сотрёт команду и ответит сам. «.clawd» без слов — список команд",
            "talk",
        )
        ui.switch(
            "suggest_auto", "Сам предлагает ответы",
            "На новое сообщение в открытом мессенджере покажет 3 варианта ответа. Текст с экрана уходит провайдеру ИИ",
            "edit",
        )
        ui.text("Попросить Clawd", "Напиши или скажи, что сделать", "star", accent = true) {
            startActivity(Intent(this, AskActivity::class.java))
        }
        ui.text("Сказать голосом", icon = "mic", accent = true) {
            startActivity(Intent(this, AskActivity::class.java).putExtra("voice", true))
        }
        ui.divider(
            "Без ИИ Clawd понимает: «закрой все окна», «открой YouTube», «фонарик», «громче / тише / без звука», " +
                "«пауза», «следующий трек», «таймер на 5 минут», «будильник на 7:30», «скриншот», «домой», «назад», " +
                "«заблокируй», «уведомления», «вай-фай», «найди …», «нажми …», «листай вниз». Можно через «и»: «открой ютуб и сделай громче». " +
                "С ИИ — что угодно: он смотрит на экран и сам решает, куда нажать (до 6 шагов). " +
                "Вопросы, пересказ и подсказки тоже через ИИ; тогда провайдеру уходит текст с экрана. " +
                "Попросить можно из уведомления Clawd или командой «.clawd …» в любом поле.",
        )

        ui.header("Мозг")
        ui.switch("brain_enabled", "Помнит тебя и свои действия", "Копит память: что ты с ним делал, какие приложения открываешь, как к нему относишься. Хранится только на телефоне", "book")
        ui.input("brain_name", "Как тебя зовут", "Clawd будет иногда обращаться по имени", "edit")
        ui.input("brain_about", "Расскажи о себе", "Например: учусь на программиста, люблю котов, ложусь поздно. Clawd будет это учитывать", "edit")
        ui.switch("brain_learn", "ИИ сам запоминает факты", "Нейросеть может записать в память новое о тебе. Видит она только то, что ей отправляют", "ai")
        ui.switch("brain_share", "Передавать память ИИ", "Короткая выжимка памяти уходит провайдеру ИИ вместе с запросом. Выключи — Clawd будет помнить, но реплики пойдут без памяти", "lock")
        ui.text("Привязанность", icon = "heart", sub = { "${brain.bondLabel()} (${brain.bond.toInt()}/100)" })
        ui.text("Вместе", icon = "stats", sub = {
            val d = brain.daysTogether()
            "$d ${Brain.plural(d, "день", "дня", "дней")}, заходов: ${brain.sessions}"
        })
        ui.text("Запомнено фактов", icon = "edit", sub = { "${brain.notesCount} шт." })
        ui.text("Забыть всё", "Тапни дважды в течение 5 секунд", "trash", red = true) {
            val now = SystemClock.uptimeMillis()
            if (now - forgetArmed < 5000) {
                forgetArmed = 0
                val s = PetService.instance
                if (s != null) s.forgetBrain() else {
                    brain.forget()
                    brain.save()
                }
                Toast.makeText(this, "Память стёрта", Toast.LENGTH_SHORT).show()
                ui.refresh()
            } else {
                forgetArmed = now
                Toast.makeText(this, "Тапни ещё раз, чтобы стереть память", Toast.LENGTH_SHORT).show()
            }
        }
        ui.text("Что Clawd помнит", icon = "book", sub = { brain.describe(prefs.s("brain_name").ifBlank { null }) })

        ui.header("Общая память с Telegram")
        ui.switch("sync", "Одна память с Clawd из плагина", "Clawd на телефоне и в Telegram делят привязанность, факты о тебе, дни вместе и сытость. Обмен идёт только внутри телефона (127.0.0.1)", "link")
        ui.text("Код синхронизации", icon = "lock", sub = { SyncServer.code(prefs) + " — впиши его в плагине: Мозг → Код синхронизации. Тапни, чтобы скопировать" }) {
            getSystemService(android.content.ClipboardManager::class.java)
                .setPrimaryClip(android.content.ClipData.newPlainText("Clawd", SyncServer.code(prefs)))
            Toast.makeText(this, "Код скопирован", Toast.LENGTH_SHORT).show()
        }
        ui.text("Последний обмен", icon = "stats", sub = {
            val t = prefs.l("sync_last")
            if (t <= 0) "Ещё не было" else Brain.relTime((System.currentTimeMillis() - t) / 1000.0)
        })

        ui.header("Связь с компом")
        ui.switch("pc_link", "Один Клодик на двоих", "Отошёл от компа — Клодик прибегает в телефон, сел за комп — убегает туда. Нужен тот же код синхронизации в настройках Clawd на ПК", "link")
        ui.switch("pc_notify", "Записки на комп", "Пока ты за компом, Клодик там приносит записку: кто и где тебе написал", "talk")
        ui.switch("pc_notify_text", "Показывать текст сообщения", "Иначе на компе видно только «кто» и «где»", "eye")
        ui.text("Комп", icon = "stats", sub = {
            val t = prefs.l("pc_seen")
            if (t <= 0) "Ещё не связывались — впиши код на компе (ПКМ по Клодику → Настройки → Телефон)"
            else "${prefs.s("pc_ip")} · был на связи ${Brain.relTime((System.currentTimeMillis() - t) / 1000.0)}"
        })

        ui.header("ИИ-реплики")
        ui.switch("ai_enabled", "Отвечает через ИИ", "Реплики генерирует нейросеть. Уходят тип ситуации, названия приложений и треков. Текст — только если включить «Видит текст»", "ai")
        ui.selector("ai_provider", "Провайдер", Ai.PROVIDERS, "ai")
        ui.input("ai_key", "API-ключ", "Gemini (бесплатно): aistudio.google.com/apikey · Claude: platform.claude.com/settings/keys", "lock", secret = true)
        ui.input("ai_model", "Модель", "Пусто — по умолчанию (gemini-3.5-flash-lite, gpt-4o-mini или claude-haiku-5-5)", "stats")
        ui.input("ai_url", "Адрес API (только для OpenAI-совместимого)", "Пусто — api.openai.com. Например https://openrouter.ai/api/v1", "link")
        ui.selector("ai_chat", "Болтовня от скуки", PetService.AI_CHAT_ITEMS, "talk")
        ui.switch("ai_moods", "ИИ выбирает анимации", "Эмоция, прыжок, сердечки, ноты подбираются под ситуацию", "heart")
        ui.switch("ai_move", "ИИ управляет движением", "Нейросеть сама решает, куда пойти: к краю, в центр, спрятаться, прыгнуть или лечь спать", "walk")
        ui.switch("ai_text", "Видит текст", "Clawd комментирует текст уведомлений и экрана. Текст уходит провайдеру ИИ, включай осознанно", "lock")
        ui.input("ai_persona", "Доп. характер", "Необязательно: например «ты грубоватый пират»", "edit")

        ui.header("Резервный ИИ")
        ui.input("ai2_url", "Адрес резервного API", "OpenAI-совместимый. Включается, когда основной упёрся в лимит или не отвечает. Например https://api.groq.com/openai/v1", "link")
        ui.input("ai2_key", "Ключ резервного API", "Для своего локального сервера можно оставить пустым", "lock", secret = true)
        ui.input("ai2_model", "Модель резервного ИИ", "Название модели у этого провайдера, например llama-3.3-70b-versatile", "stats")

        ui.header("Сбои и проверка")
        ui.switch("ai_fallback", "Запасные фразы при сбое", "Если ИИ недоступен или упёрся в лимит, Clawd скажет заготовленную фразу", "retry")
        ui.text("Проверить ИИ", icon = "retry", accent = true) { pet { it.aiTest() } }
        ui.divider()

        ui.header("Разное")
        ui.text("Позвать в центр экрана", icon = "target", accent = true) { pet { it.recenter() } }
        ui.text("Вернуть съеденное", icon = "back") { pet { it.giveBack() } }
        ui.text("Погладили", icon = "heart", sub = { "${prefs.i("pets_count")} раз" })
        ui.text("Съедено элементов", icon = "stats", sub = { "${prefs.i("eaten_count")} шт." })
        ui.text("Убрано кучек", icon = "trash", sub = { "${prefs.i("poops_cleaned")} шт." })
        ui.divider(
            "Тап — ткнуть, двойной тап — прыжок, удержание — погладить, перетаскивание — перенести, резкий бросок — полёт. " +
                "Если Clawd что-то съел — тапни его, и он вернёт всё на место. " +
                "Потряси телефон, чтобы подбросить его, а если подкинуть к статус-бару — повиснет.",
        )
    }

    private fun enable() {
        prefs.enabled = true
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        PetService.start(this)
    }

    /** Список напоминаний: тап по пункту — удалить. */
    private fun editReminders() {
        val list = Reminders.load(prefs)
        if (list.isEmpty()) {
            Toast.makeText(this, "Напоминаний нет", Toast.LENGTH_SHORT).show()
            return
        }
        val fmt = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        android.app.AlertDialog.Builder(this)
            .setTitle("Тапни, чтобы удалить")
            .setItems(list.map { fmt.format(Date(it.at)) + " — " + it.text }.toTypedArray()) { _, i ->
                Reminders.save(prefs, list.filterIndexed { j, _ -> j != i })
                ui.refresh()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun listenerAllowed(): Boolean {
        val me = ComponentName(this, NotifyListener::class.java).flattenToString()
        return Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(me) == true
    }
}

/** Живое превью питомца в настройках: моргает, машет, оглядывается, топает. */
@SuppressLint("ViewConstructor")
class PreviewView(context: Context, private val prefs: Prefs) : View(context) {
    private val sprites = SpriteCache()
    private val unit = (6 * resources.displayMetrics.density).roundToInt()
    private val start = SystemClock.uptimeMillis()

    override fun onDraw(canvas: Canvas) {
        val t = SystemClock.uptimeMillis() - start
        val inPhase = t % 3000
        val acc = PetService.accessoryFor(context, prefs)
        val pose = when ((t / 3000) % 4) {
            0L -> Pose(eyes = if (inPhase in 2000L..2150L) "blink" else "open", acc = acc)
            1L -> Pose(eyes = "happy", arms = if ((t / 250) % 2 == 0L) "wave1" else "wave2", mouth = "line", acc = acc)
            2L -> Pose(look = if ((t / 700) % 2 == 0L) -1 else 1, acc = acc)
            else -> Pose(legs = if ((t / 150) % 2 == 0L) "walk1" else "walk2", look = 1, acc = acc)
        }
        val bmp = sprites.get(pose, unit, PetService.colorFor(prefs))
        // в высокой шляпе или с крыльями кадр больше — ужимаем, чтобы влез целиком
        val s = minOf(1f, width.toFloat() / bmp.width, height.toFloat() / bmp.height)
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(s, s)
        canvas.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, null)
        canvas.restore()
        postInvalidateDelayed(50)
    }
}
