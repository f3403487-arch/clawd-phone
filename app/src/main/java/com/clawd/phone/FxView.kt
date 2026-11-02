package com.clawd.phone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Окошко рядом с питомцем: облачко с репликой и частицы — всё как в плагине.
 * Облачко: шрифт Telegram (Roboto Medium), плашка цвета фона Telegram, скругление 12dp, обводка, тень,
 * висит над макушкой (у верхнего края — под Clawd). Частицы живут в координатах мира и не едут за Clawd:
 * вылетают из головы, растут с 0.5 до 1, поднимаются и гаснут.
 */
class FxView(
    context: Context,
    textDp: Float = 13f,
    private val maxDp: Int = 220,
    private val longMaxDp: Int = 270,
    private val padHDp: Float = 10f,
    private val padVDp: Float = 6f,
) : View(context) {

    private companion object {
        const val FADE_IN = 150L
        const val FADE_OUT = 180L
        const val MAX_PARTICLES = 14
        // стиль Clawd Pixel — как облачко на ПК
        const val BG = 0xFF211B17.toInt()
        const val BORDER = 0xFFE0855F.toInt()
        const val SHADOW = 0xFF0A0807.toInt()
        const val TEXT = 0xFFF6EDE4.toInt()
        const val POP = 280L
        const val TYPE_CPS = 60f
    }

    /** Частица-анимация: из точки в точку, с масштабом, поворотом и прозрачностью. */
    private class Tween(
        val bmp: Bitmap,
        val x0: Float, val y0: Float, val x1: Float, val y1: Float,
        val s0: Float, val s1: Float, val r0: Float, val r1: Float,
        val a0: Float, val a1: Float,
        val start: Long, val dur: Long, val accel: Boolean,
    )

    private val tweens = ArrayList<Tween>()
    private val bitmaps = HashMap<String, Bitmap>()
    private val density = resources.displayMetrics.density

    /** Размер одного пикселя частиц. */
    var pixel = 6f
        set(value) {
            if (value != field) bitmaps.clear()
            field = value
        }

    /** Поворот содержимого (когда Clawd стоит на боковом или верхнем краю). */
    var angle = 0f

    /** Где это окно в координатах мира (левый верхний угол), чтобы частицы оставались на месте. */
    var originX = 0f
    var originY = 0f

    /** Облачко: центр по горизонтали и низ (или верх, если below) в координатах окна. */
    var bubbleCX = 0f
    var bubbleY = 0f
    var below = false

    private var bubble: StaticLayout? = null
    private var bubbleUntil = 0L
    private var appearAt = 0L
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textDp, resources.displayMetrics)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val fill = Paint().apply { color = BG }
    private val stroke = Paint().apply { color = BORDER }
    private val shade = Paint().apply { color = SHADOW }
    private val path = android.graphics.Path()

    /** Текст печатается по буквам: держим отдельную раскладку для уже показанной части. */
    private var fullText = ""
    private var typed: StaticLayout? = null
    private var typedN = 0
    private var layoutW = 1

    private val step get() = 2 * density
    private val bw get() = 2 * density
    private val tailH get() = 7 * density
    private val shadowOff get() = 3 * density
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()

    val busy get() = bubble != null || tweens.isNotEmpty()

    /** Идёт анимация (частицы или проявление/угасание облачка) — нужны частые кадры. */
    fun animating(now: Long) = tweens.isNotEmpty() ||
        (bubble != null && (now - appearAt < max(POP, (fullText.length / TYPE_CPS * 1000).toLong()) + 50 || now > bubbleUntil - 50))
    val hasBubble get() = bubble != null

    private val padH get() = padHDp * density
    private val padV get() = padVDp * density

    /** Ширина и высота облачка вместе с отступами (0, если его нет). */
    fun bubbleW() = bubble?.let { it.width + padH * 2 + shadowOff } ?: 0f
    fun bubbleH() = bubble?.let { it.height + padV * 2 + tailH + shadowOff } ?: 0f

    /** Как в плагине: обычная реплика 2 с + 0.05 с на символ, длинная — от 6 до 30 с. */
    fun showBubble(text: String, long: Boolean = false): Long {
        val maxW = ((if (long) longMaxDp else maxDp) * density - padH * 2).toInt()
        val wanted = text.lines().maxOf { ceil(textPaint.measureText(it)).toInt() }
        val w = min(maxW, wanted).coerceAtLeast(1)
        bubble = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, w)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.05f)
            .build()
        fullText = text
        layoutW = w
        typed = null
        typedN = 0
        val now = SystemClock.uptimeMillis()
        // каждая новая реплика выпрыгивает и печатается заново
        appearAt = now
        val dur = if (long) ((3.0 + text.length * 0.06).coerceIn(6.0, 30.0) * 1000).toLong()
        else ((2.0 + text.length * 0.05) * 1000).toLong()
        bubbleUntil = now + dur
        invalidate()
        return dur
    }

    /** Плавно спрятать облачко. */
    fun hideBubble() {
        val now = SystemClock.uptimeMillis()
        if (bubble != null && bubbleUntil > now) bubbleUntil = now
    }

    private fun bitmap(kind: String): Bitmap = bitmaps.getOrPut(kind) {
        val s = if (kind == "fish" || kind == "poop") pixel * 1.2f else pixel
        Sprite.patternBitmap(kind, max(2, s.toInt()))
    }

    fun particleW(kind: String) = bitmap(kind).width.toFloat()
    fun particleH(kind: String) = bitmap(kind).height.toFloat()

    /** Частица как в плагине: из (wx, wy) мира, сдвиг (dx, dy), рост 0.5→1, гаснет. */
    fun addFloat(kind: String, wx: Float, wy: Float, dx: Float, dy: Float, dur: Long, rot: Float) {
        if (tweens.size >= MAX_PARTICLES) return
        tweens.add(Tween(bitmap(kind), wx, wy, wx + dx, wy + dy, 0.5f, 1f, rot, rot, 1f, 0f, SystemClock.uptimeMillis(), dur, false))
    }

    /** Произвольная анимация (рыбка падает в рот). */
    fun addTween(
        kind: String, x0: Float, y0: Float, x1: Float, y1: Float, s0: Float, s1: Float,
        r0: Float, r1: Float, dur: Long, accel: Boolean,
    ) {
        tweens.add(Tween(bitmap(kind), x0, y0, x1, y1, s0, s1, r0, r1, 1f, 1f, SystemClock.uptimeMillis(), dur, accel))
    }

    fun clearParticles() = tweens.clear()

    fun step(now: Long) {
        if (bubble != null && now > bubbleUntil + FADE_OUT) bubble = null
        tweens.removeAll { now - it.start >= it.dur }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val swap = abs(angle) == 90f
        val lw = if (swap) height else width
        val lh = if (swap) width else height
        canvas.save()
        if (angle != 0f) {
            canvas.translate(width / 2f, height / 2f)
            canvas.rotate(angle)
            canvas.translate(-lw / 2f, -lh / 2f)
        }
        drawContent(canvas, lw)
        canvas.restore()
    }

    /** Прямоугольник с углами «лесенкой» в [path]. */
    private fun stepped(l: Float, t: Float, r: Float, b: Float) {
        val s = step
        path.reset()
        path.moveTo(l + 2 * s, t)
        path.lineTo(r - 2 * s, t); path.lineTo(r - 2 * s, t + s); path.lineTo(r - s, t + s); path.lineTo(r - s, t + 2 * s)
        path.lineTo(r, t + 2 * s); path.lineTo(r, b - 2 * s); path.lineTo(r - s, b - 2 * s); path.lineTo(r - s, b - s)
        path.lineTo(r - 2 * s, b - s); path.lineTo(r - 2 * s, b); path.lineTo(l + 2 * s, b); path.lineTo(l + 2 * s, b - s)
        path.lineTo(l + s, b - s); path.lineTo(l + s, b - 2 * s); path.lineTo(l, b - 2 * s); path.lineTo(l, t + 2 * s)
        path.lineTo(l + s, t + 2 * s); path.lineTo(l + s, t + s); path.lineTo(l + 2 * s, t + s)
        path.close()
    }

    /** Хвостик лесенкой: от края плашки y к Clawd (вниз или, если облачко под ним, вверх); g — раздуть под рамку. */
    private fun tail(tx: Float, y: Float, g: Float) {
        val u = 2.4f * density
        val dir = if (below) -1 else 1
        path.reset()
        path.moveTo(tx - 3 * u - g, y)
        path.lineTo(tx + 3 * u + g, y)
        path.lineTo(tx + 3 * u + g, y + dir * u)
        path.lineTo(tx + 2 * u + g, y + dir * u)
        path.lineTo(tx + 2 * u + g, y + dir * 2 * u)
        path.lineTo(tx + u + g, y + dir * 2 * u)
        path.lineTo(tx + u + g, y + dir * (3 * u + g))
        path.lineTo(tx - u - g, y + dir * (3 * u + g))
        path.lineTo(tx - u - g, y + dir * 2 * u)
        path.lineTo(tx - 2 * u - g, y + dir * 2 * u)
        path.lineTo(tx - 2 * u - g, y + dir * u)
        path.lineTo(tx - 3 * u - g, y + dir * u)
        path.close()
    }

    private fun drawContent(canvas: Canvas, width: Int) {
        val now = SystemClock.uptimeMillis()
        bubble?.let { l ->
            val tIn = ((now - appearAt) / FADE_IN.toFloat()).coerceIn(0f, 1f)
            val tOut = if (now > bubbleUntil) ((now - bubbleUntil) / FADE_OUT.toFloat()).coerceIn(0f, 1f) else 0f
            val alpha = (tIn * (1f - tOut) * 255).toInt()
            fill.alpha = alpha
            stroke.alpha = alpha
            shade.alpha = alpha
            textPaint.alpha = alpha
            // выпрыгивает с пружинкой (back-out)
            val u = ((now - appearAt) / POP.toFloat()).coerceIn(0f, 1f)
            val c = 2.2f
            val ease = 1 + (c + 1) * (u - 1).pow(3) + c * (u - 1).pow(2)
            val pop = (1 - ease) * 8 * density * (if (below) -1 else 1)
            val boxW = l.width + padH * 2
            val boxH = l.height + padV * 2
            val margin = 4 * density
            val left = (bubbleCX - boxW / 2).coerceIn(margin, max(margin, width - boxW - margin - shadowOff))
            val top = (if (below) bubbleY + tailH else bubbleY - tailH - boxH - shadowOff) + pop
            val bottom = top + boxH
            // хвостик лесенкой смотрит на Clawd, даже если облачко прижато к краю
            val tx = bubbleCX.coerceIn(left + 6 * step, left + boxW - 6 * step)
            // тень, рамка, заливка: сначала тень плашки и хвостика
            stepped(left + shadowOff, top + shadowOff, left + boxW + shadowOff, bottom + shadowOff)
            canvas.drawPath(path, shade)
            tail(tx + shadowOff, if (below) top + shadowOff else bottom + shadowOff, bw)
            canvas.drawPath(path, shade)
            stepped(left, top, left + boxW, bottom)
            canvas.drawPath(path, stroke)
            tail(tx, if (below) top else bottom, bw)
            canvas.drawPath(path, stroke)
            stepped(left + bw, top + bw, left + boxW - bw, bottom - bw)
            canvas.drawPath(path, fill)
            tail(tx, if (below) top + bw + 1 else bottom - bw - 1, 0f)
            canvas.drawPath(path, fill)
            // печатаем по буквам
            val n = min(fullText.length, ((now - appearAt) / 1000f * TYPE_CPS).toInt() + 1)
            if (n >= fullText.length) typed = null
            else if (n != typedN || typed == null) {
                typedN = n
                typed = StaticLayout.Builder.obtain(fullText, 0, n, textPaint, layoutW)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.05f)
                    .build()
            }
            canvas.save()
            canvas.translate(left + padH, top + padV)
            (typed ?: l).draw(canvas)
            canvas.restore()
        }
        for (p in tweens) {
            val t = ((now - p.start).toFloat() / p.dur).coerceIn(0f, 1f)
            // как у ViewPropertyAnimator: по умолчанию «разгон-торможение», у рыбки — разгон
            val e = if (p.accel) t.pow(2.8f) else (cos((t + 1) * Math.PI) / 2 + 0.5).toFloat()
            val px = p.x0 + (p.x1 - p.x0) * e - originX
            val py = p.y0 + (p.y1 - p.y0) * e - originY
            val s = p.s0 + (p.s1 - p.s0) * e
            val rot = p.r0 + (p.r1 - p.r0) * e
            bmpPaint.alpha = ((p.a0 + (p.a1 - p.a0) * e) * 255).toInt().coerceIn(0, 255)
            val cx = px + p.bmp.width / 2f
            val cy = py + p.bmp.height / 2f
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(rot)
            canvas.scale(s, s)
            canvas.drawBitmap(p.bmp, -p.bmp.width / 2f, -p.bmp.height / 2f, bmpPaint)
            canvas.restore()
        }
    }
}
