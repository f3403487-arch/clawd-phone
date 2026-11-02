package com.clawd.phone

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.ImageView
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Маленькое окно «Спросить Clawd»: вопрос, «напиши за меня», пересказ экрана, подсказка ответа. */
class AskActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TgTheme(this)
        fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

        val input = EditText(this).apply {
            hint = "Например: закрой все окна в фоне"
            setTextColor(t.text)
            setHintTextColor(t.sub)
            textSize = 16f
            minLines = 2
            background = PixelDrawable(t.gray, t.divider, step = dp(2).toFloat(), bw = dp(2).toFloat())
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // рамка цвета Clawd, тень со сдвигом — то же окошко, что на ПК
            setPadding(dp(20), dp(18), dp(24), dp(16))
            isClickable = true
            background = PixelDrawable(t.cell, t.accent2, t.shadow, step = dp(3).toFloat(), bw = dp(2).toFloat(),
                shadowOff = dp(4).toFloat())
        }
        box.addView(TextView(this).apply {
            text = "Что сделать?"
            textSize = 19f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(t.text)
            val colors = Sprite.colors[PetService.colorFor(Prefs(this@AskActivity))]
            val pet = android.graphics.drawable.BitmapDrawable(resources, Sprite.render(Pose(), dp(2), colors.body, colors.eye))
            setCompoundDrawablesWithIntrinsicBounds(pet, null, null, null)
            compoundDrawablePadding = dp(10)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        })
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(ImageView(this).apply {
            setImageDrawable(Icons.get("mic", t.accent))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            contentDescription = "Сказать голосом"
            setOnClickListener { listen() }
        }, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginStart = dp(6) })
        box.addView(row, LinearLayout.LayoutParams(-1, -2))
        this.input = input

        fun go(needText: Boolean, action: (PetService, String) -> Unit) {
            val text = input.text.toString().trim()
            if (needText && text.isEmpty()) {
                input.error = "Напиши, что нужно"
                return
            }
            val pet = PetService.instance
            if (pet == null) {
                Toast.makeText(this, "Сначала включи питомца", Toast.LENGTH_SHORT).show()
                return
            }
            finish()
            // ждём, пока вернётся прошлое приложение, чтобы Clawd видел его экран и поле ввода
            Handler(Looper.getMainLooper()).postDelayed({ action(pet, text) }, 600)
        }

        fun button(label: String, icon: String, onClick: () -> Unit) = TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(t.accent)
            gravity = Gravity.CENTER_VERTICAL
            compoundDrawablePadding = dp(16)
            val d = Icons.get(icon, t.accent).apply { setBounds(0, 0, dp(22), dp(22)) }
            setCompoundDrawables(d, null, null, null)
            setPadding(dp(4), dp(13), dp(4), dp(13))
            setOnClickListener { onClick() }
        }
        doIt = { go(true) { p, s -> p.command(s) } }
        box.addView(button("Сделай / ответь", "star") { doIt() })
        box.addView(button("Просто спросить", "ai") { go(true) { p, s -> p.ask(s) } })
        box.addView(button("Напиши за меня в поле ввода", "edit") { go(true) { p, s -> p.writeForMe(s) } })
        box.addView(button("Перескажи, что на экране", "eye") { go(false) { p, _ -> p.summarizeScreen() } })
        box.addView(button("Предложи ответ", "talk") { go(false) { p, _ -> p.suggestReplies() } })
        box.addView(button("Покормить", "food") { go(false) { p, _ -> p.feed() } })
        // игры и забота — компактной строкой
        val games = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun mini(label: String, action: (PetService) -> Unit) = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(t.accent)
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(10), dp(6), dp(10))
            background = PixelDrawable(t.cell2, t.divider, step = dp(2).toFloat(), bw = dp(2).toFloat())
            setOnClickListener { go(false) { p, _ -> action(p) } }
        }
        listOf<Pair<String, (PetService) -> Unit>>(
            "⚽ Мяч" to { it.startBall() },
            "🏃 Догонялки" to { it.startTag() },
            "🙈 Прятки" to { it.startSeek() },
            "🛁 Помыть" to { it.bath() },
            "📚 Учёба" to { it.startPomodoro() },
        ).forEachIndexed { i, (label, action) ->
            games.addView(mini(label, action), LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) marginStart = dp(4) })
        }
        box.addView(games, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        clipBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(clipBox, 2)
        this.button = ::button
        this.goRef = { needText, action -> go(needText, action) }

        setContentView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener { finish() }
            addView(box, LinearLayout.LayoutParams(-1, -2))
        })
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        input.requestFocus()
        if (intent.getBooleanExtra("voice", false)) listen()
    }

    private var input: EditText? = null
    private var doIt: () -> Unit = {}
    private var clipBox: LinearLayout? = null
    private var clipChecked = false
    private var button: ((String, String, () -> Unit) -> TextView)? = null
    private var goRef: ((Boolean, (PetService, String) -> Unit) -> Unit)? = null

    /** Буфер обмена можно прочитать, только когда окно в фокусе: ссылка — пересказать, текст — перевести или объяснить. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || clipChecked) return
        clipChecked = true
        val clip = try {
            getSystemService(android.content.ClipboardManager::class.java)
                .primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        } catch (_: Exception) {
            null
        } ?: return
        if (clip.isEmpty()) return
        val box = clipBox ?: return
        val b = button ?: return
        val go = goRef ?: return
        val url = Regex("https?://\\S+").find(clip)?.value
        val short = clip.replace(Regex("\\s+"), " ").take(40) + if (clip.length > 40) "…" else ""
        if (url != null) {
            box.addView(b("Перескажи скопированную ссылку", "link") { go(false) { p, _ -> p.summarizeUrl(url) } })
            box.addView(b("Открыть ссылку", "link") {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                } catch (_: Exception) {
                }
                finish()
            })
        } else {
            val latin = clip.count { it in 'a'..'z' || it in 'A'..'Z' }
            val letters = clip.count { it.isLetter() }.coerceAtLeast(1)
            if (latin.toFloat() / letters > 0.6f) {
                box.addView(b("Переведи: «$short»", "text") { go(false) { p, _ -> p.translate(clip) } })
            } else {
                box.addView(b("Переведи на английский: «$short»", "text") { go(false) { p, _ -> p.translate(clip) } })
            }
            box.addView(b("Объясни: «$short»", "ai") { go(false) { p, _ -> p.ask("Объясни простыми словами: $clip") } })
        }
    }

    /** Голосовой ввод через системное распознавание речи; сказанное сразу выполняется. */
    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи, что сделать")
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(i, 7)
        } catch (_: Exception) {
            Toast.makeText(this, "На телефоне нет распознавания речи", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 7 || resultCode != RESULT_OK) return
        val said = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
        input?.setText(said)
        doIt()
    }
}
