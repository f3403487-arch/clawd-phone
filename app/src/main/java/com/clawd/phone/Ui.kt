package com.clawd.phone

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/** Стиль Clawd Pixel (как на ПК): тёплый тёмный фон, рамки и акцент цвета самого Clawd, углы «лесенкой». */
class TgTheme(@Suppress("UNUSED_PARAMETER") context: Context) {
    val isDark = true

    val gray = 0xFF15110E.toInt()
    val cell = 0xFF211B17.toInt()
    val cell2 = 0xFF2E241F.toInt()
    val text = 0xFFF6EDE4.toInt()
    val sub = 0xFF9B8A7C.toInt()
    val accent = 0xFFF0A07A.toInt()
    val accent2 = 0xFFD97757.toInt()
    val icon = 0xFFB8A698.toInt()
    val divider = 0xFF3D312A.toInt()
    val shadow = 0xFF0A0807.toInt()
    val red = 0xFFC94A3F.toInt()
    val ripple = 0x33F0A07A
}

/** Плашка с углами «лесенкой», рамкой и жёсткой тенью со сдвигом — как в пиксельных играх. */
class PixelDrawable(
    private val fill: Int,
    private val border: Int? = null,
    private val shadow: Int? = null,
    private val step: Float = 6f,
    private val bw: Float = 4f,
    private val shadowOff: Float = 8f,
) : android.graphics.drawable.Drawable() {
    private val paint = android.graphics.Paint()
    private val path = android.graphics.Path()

    private fun stepped(l: Float, t: Float, r: Float, b: Float, s: Float) {
        path.reset()
        path.moveTo(l + 2 * s, t)
        path.lineTo(r - 2 * s, t); path.lineTo(r - 2 * s, t + s); path.lineTo(r - s, t + s); path.lineTo(r - s, t + 2 * s)
        path.lineTo(r, t + 2 * s); path.lineTo(r, b - 2 * s); path.lineTo(r - s, b - 2 * s); path.lineTo(r - s, b - s)
        path.lineTo(r - 2 * s, b - s); path.lineTo(r - 2 * s, b); path.lineTo(l + 2 * s, b); path.lineTo(l + 2 * s, b - s)
        path.lineTo(l + s, b - s); path.lineTo(l + s, b - 2 * s); path.lineTo(l, b - 2 * s); path.lineTo(l, t + 2 * s)
        path.lineTo(l + s, t + 2 * s); path.lineTo(l + s, t + s); path.lineTo(l + 2 * s, t + s)
        path.close()
    }

    override fun draw(canvas: android.graphics.Canvas) {
        val bnd = bounds
        val off = if (shadow != null) shadowOff else 0f
        val l = bnd.left.toFloat()
        val t = bnd.top.toFloat()
        val r = bnd.right - off
        val b = bnd.bottom - off
        if (shadow != null) {
            stepped(l + off, t + off, r + off, b + off, step)
            paint.color = shadow
            canvas.drawPath(path, paint)
        }
        if (border != null) {
            stepped(l, t, r, b, step)
            paint.color = border
            canvas.drawPath(path, paint)
            stepped(l + bw, t + bw, r - bw, b - bw, step)
        } else stepped(l, t, r, b, step)
        paint.color = fill
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}

/**
 * Строитель экрана настроек: Header / Switch / Selector / Input / Text / Divider — те же элементы,
 * что в настройках плагина exteraGram.
 */
class SettingsBuilder(private val context: Context, private val root: LinearLayout, private val prefs: Prefs) {
    val t = TgTheme(context)
    private val refreshers = ArrayList<() -> Unit>()
    private var section: LinearLayout? = null

    fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    fun refresh() = refreshers.forEach { it() }

    private fun currentSection(): LinearLayout = section ?: LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val d = context.resources.displayMetrics.density
        background = PixelDrawable(t.cell, t.divider, t.shadow, step = 3 * d, bw = 2 * d, shadowOff = 4 * d)
        // отступ под рамку и тень, чтобы содержимое их не закрывало
        setPadding(dp(2), dp(2), dp(6), dp(6))
        root.addView(this, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), 0, dp(8), 0) })
        section = this
    }

    /** Новая секция: пиксельный квадратик и заголовок капсом цвета Clawd. */
    fun header(text: String) {
        endSection(gap = true)
        currentSection().addView(TextView(context).apply {
            this.text = text.uppercase()
            textSize = 14f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(t.accent)
            val sq = android.graphics.drawable.GradientDrawable().apply {
                setColor(t.accent2)
                setSize(dp(7), dp(7))
            }
            setCompoundDrawablesWithIntrinsicBounds(sq, null, null, null)
            compoundDrawablePadding = dp(9)
            setPadding(dp(19), dp(15), dp(19), dp(6))
        })
    }

    private fun endSection(gap: Boolean) {
        if (section != null && gap) root.addView(View(context), LinearLayout.LayoutParams(-1, dp(12)))
        section = null
    }

    /** Серая подпись под секцией. */
    fun divider(text: String? = null) {
        section = null
        if (text.isNullOrEmpty()) {
            root.addView(View(context), LinearLayout.LayoutParams(-1, dp(12)))
            return
        }
        root.addView(TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(t.sub)
            setLineSpacing(0f, 1.15f)
            setPadding(dp(21), dp(10), dp(21), dp(17))
        })
    }

    fun custom(view: View) = currentSection().addView(view)

    private fun row(icon: String?, title: String, subtitle: String?, color: Int = t.text): Row {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(if (subtitle != null) 64 else 50)
            setPadding(dp(19), dp(8), dp(19), dp(8))
            background = RippleDrawable(ColorStateList.valueOf(t.ripple), ColorDrawable(t.cell), null)
            isClickable = true
        }
        val iconView = ImageView(context)
        if (icon != null) {
            iconView.setImageDrawable(Icons.get(icon, if (color == t.text) t.icon else color))
            row.addView(iconView, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(27) })
        }
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val titleView = TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(color)
        }
        val subView = TextView(context).apply {
            textSize = 13f
            setTextColor(t.sub)
            setLineSpacing(0f, 1.1f)
            setPadding(0, dp(2), 0, 0)
            visibility = if (subtitle != null) View.VISIBLE else View.GONE
            text = subtitle ?: ""
        }
        texts.addView(titleView)
        texts.addView(subView)
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        currentSection().addView(row, LinearLayout.LayoutParams(-1, -2))
        return Row(row, titleView, subView)
    }

    class Row(val view: LinearLayout, val title: TextView, val sub: TextView) {
        fun setSub(text: String?) {
            sub.text = text ?: ""
            sub.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun tgSwitch() = Switch(context).apply {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        thumbTintList = ColorStateList(states, intArrayOf(t.accent, t.sub))
        trackTintList = ColorStateList(states, intArrayOf(0xFF6B3A26.toInt(), t.divider))
        isClickable = false
        isFocusable = false
    }

    fun switch(
        key: String, title: String, subtitle: String? = null, icon: String? = null,
        get: () -> Boolean = { prefs.b(key) },
        set: (Boolean) -> Unit = { prefs.put(key, it) },
    ): Row {
        val r = row(icon, title, subtitle)
        val sw = tgSwitch()
        r.view.addView(sw, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12) })
        refreshers.add { sw.isChecked = get() }
        sw.isChecked = get()
        r.view.setOnClickListener {
            set(!get())
            refresh()
        }
        return r
    }

    fun selector(key: String, title: String, items: List<String>, icon: String? = null, onChange: ((Int) -> Unit)? = null): Row {
        val r = row(icon, title, null)
        val value = TextView(context).apply {
            textSize = 16f
            setTextColor(t.accent)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(170)
        }
        r.view.addView(value, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12) })
        val update = { value.text = items.getOrNull(prefs.i(key)) ?: items.first() }
        refreshers.add(update)
        update()
        r.view.setOnClickListener {
            AlertDialog.Builder(context, dialogTheme())
                .setTitle(title)
                .setSingleChoiceItems(items.toTypedArray(), prefs.i(key)) { d, which ->
                    prefs.put(key, which)
                    onChange?.invoke(which)
                    d.dismiss()
                    refresh()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
        return r
    }

    fun input(key: String, title: String, subtitle: String? = null, icon: String? = null, secret: Boolean = false): Row {
        val r = row(icon, title, subtitle)
        val update = {
            val v = prefs.s(key)
            r.setSub(
                when {
                    v.isEmpty() -> subtitle
                    secret -> "•".repeat(minOf(12, v.length)) + "  ✓ задан"
                    else -> v
                },
            )
        }
        refreshers.add(update)
        update()
        r.view.setOnClickListener {
            val edit = EditText(context).apply {
                setText(prefs.s(key))
                setSelection(text.length)
                setTextColor(t.text)
                inputType = if (secret) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            val box = FrameLayout(context).apply {
                setPadding(dp(20), dp(8), dp(20), 0)
                addView(edit)
            }
            AlertDialog.Builder(context, dialogTheme())
                .setTitle(title)
                .setMessage(subtitle)
                .setView(box)
                .setPositiveButton("Сохранить") { _, _ ->
                    prefs.put(key, edit.text.toString().trim())
                    refresh()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
        return r
    }

    fun text(
        title: String, subtitle: String? = null, icon: String? = null, accent: Boolean = false,
        red: Boolean = false, sub: (() -> String?)? = null, onClick: (() -> Unit)? = null,
    ): Row {
        val color = when {
            red -> t.red
            accent -> t.accent
            else -> t.text
        }
        val r = row(icon, title, subtitle ?: sub?.invoke(), color)
        if (sub != null) refreshers.add { r.setSub(sub()) }
        if (onClick != null) r.view.setOnClickListener { onClick() } else r.view.isClickable = false
        return r
    }

    private fun dialogTheme() =
        if (t.isDark) android.R.style.Theme_DeviceDefault_Dialog_Alert else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
}
