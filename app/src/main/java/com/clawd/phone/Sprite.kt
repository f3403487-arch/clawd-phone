package com.clawd.phone

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Поза питомца: из неё рисуется один кадр. */
data class Pose(
    val eyes: String = "open",
    val look: Int = 0,
    val arms: String = "normal",
    val legs: String = "stand",
    val squash: Int = 0,
    val blush: Boolean = false,
    val mouth: String = "",
    val acc: String? = null,
)

class PetColors(val name: String, val body: Int, val eye: Int)

/** Аксессуар: открывается привязанностью (need), достижением (ach) или сезоном (оба null). */
class Accessory(val key: String, val label: String, val need: Float?, val ach: String? = null)

/** Пиксельный Клодик 18×13 клеток, нарисованный прямоугольниками — как в плагине для exteraGram. */
object Sprite {
    const val GRID_W = 18
    const val GRID_H = 13

    val colors = listOf(
        PetColors("Claude", 0xFFD97757.toInt(), 0xFF1A1A1A.toInt()),
        PetColors("Мятный", 0xFF4FBF98.toInt(), 0xFF12241D.toInt()),
        PetColors("Лавандовый", 0xFF9B8AEE.toInt(), 0xFF1B1631.toInt()),
        PetColors("Небесный", 0xFF4FA8E8.toInt(), 0xFF0E1C29.toInt()),
        PetColors("Розовый", 0xFFEE85AE.toInt(), 0xFF2A1220.toInt()),
        PetColors("Графит", 0xFF5E6670.toInt(), 0xFFF2F2F2.toInt()),
        // открываются достижениями
        PetColors("Золотой", 0xFFE8B84A.toInt(), 0xFF2A1E05.toInt()),
        PetColors("Лаймовый", 0xFF9BD45A.toInt(), 0xFF16240A.toInt()),
        PetColors("Призрак", 0xFFE9ECF2.toInt(), 0xFF3A3A44.toInt()),
        PetColors("Ночной", 0xFF2E3A6E.toInt(), 0xFFF2F2F2.toInt()),
    )

    /** Какое достижение открывает окрас (индекс в colors). */
    val colorAch = mapOf(6 to "pet100", 7 to "eat50", 8 to "clean10", 9 to "pomo5")

    val accessories = listOf(
        Accessory("bow", "Бантик", 15f),
        Accessory("party", "Колпак", 25f),
        Accessory("glasses", "Очки", 35f),
        Accessory("phones", "Наушники", 60f),
        Accessory("crown", "Корона", 85f),
        Accessory("santa", "Новогодняя шапка", null),
        Accessory("flower", "Цветок", null, "days7"),
        Accessory("halo", "Нимб", null, "days30"),
        Accessory("eyepatch", "Пиратская повязка", null, "ball30"),
    )

    /** Шапки и всё, что на макушке: одновременно носится только одна. */
    val headAcc = setOf("bow", "party", "crown", "santa", "nightcap", "umbrella", "halo", "flower")

    private val accColors = mapOf(
        "bow" to (0xFFFF6FA8.toInt() to 0xFFD94C88.toInt()),
        "party" to (0xFF6FB8FF.toInt() to 0xFFFFC94D.toInt()),
        "glasses" to (0xFF1A1A1A.toInt() to 0xFF5E6670.toInt()),
        "phones" to (0xFF3A3A3A.toInt() to 0xFF5E6670.toInt()),
        "crown" to (0xFFFFC94D.toInt() to 0xFFFF5C7A.toInt()),
        "santa" to (0xFFE53935.toInt() to 0xFFFFFFFF.toInt()),
        "nightcap" to (0xFF4A6FD0.toInt() to 0xFFFFFFFF.toInt()),
        "umbrella" to (0xFFE5534B.toInt() to 0xFF3A3A3A.toInt()),
        "scarf" to (0xFFE5534B.toInt() to 0xFFFFC94D.toInt()),
        "flower" to (0xFFFF6FA8.toInt() to 0xFFFFC94D.toInt()),
        "eyepatch" to (0xFF141414.toInt() to 0xFF141414.toInt()),
        "halo" to (0xFFFFE08A.toInt() to 0xFFFFC94D.toInt()),
        "file" to (0xFFFFFFFF.toInt() to 0xFF8FA3BF.toInt()),
    )

    val patterns = mapOf(
        "heart" to listOf(".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."),
        "zzz" to listOf("XXXXX", "...X.", "..X..", ".X...", "XXXXX"),
        "spark" to listOf("..X..", "..X..", "XX.XX", "..X..", "..X.."),
        "note" to listOf("..XXXXX", "..X...X", "..X...X", "..X...X", "XXX.XXX", "XXX.XXX"),
        "fish" to listOf("..XXX..o", ".XXXXXoo", "XkXXXXo.", ".XXXXXoo", "..XXX..o"),
        "poop" to listOf(
            "....X....", "...XXo...", "..XXXXX..", ".XXXXXXo.", ".XwkXwkX.", "XXXXXXXXo", "XXXXXXXXX",
        ),
        "drop" to listOf("X", "X"),
        "flake" to listOf(".X.", "XXX", ".X."),
        "sweat" to listOf(".X", "XX"),
        "stink" to listOf("X..", ".X.", "..X", ".X.", "X.."),
        "bubble" to listOf(".XX.", "X.oX", "X..X", ".XX."),
        "ball" to listOf(".XXXX.", "XXwwXX", "XwwXXX", "XXXXXX", "XXXXwX", ".XXXX."),
        "file" to listOf("XXXX.", "X..XX", "X...X", "X.k.X", "X...X", "XXXXX"),
        "bed" to listOf(
            "XX..............",
            "XX..............",
            "XXwwwwbbbbbbbbbb",
            "XXwwwwbbbbbbbbbb",
            "XXXXXXXXXXXXXXXX",
            "X..............X",
        ),
        "bowl" to listOf("X......X", ".XXXXXX.", "..XXXX.."),
        "bowl_full" to listOf("X.fffo.X", ".XXXXXX.", "..XXXX.."),
    )

    fun particleColor(kind: String, ch: Char): Int = when (kind) {
        "heart" -> 0xFFFF5C7A.toInt()
        "zzz" -> 0xFF8FA3BF.toInt()
        "spark" -> 0xFFFFC94D.toInt()
        "note" -> 0xFF6FB8FF.toInt()
        "fish" -> when (ch) {
            'o' -> 0xFF4F86AD.toInt()
            'k' -> 0xFF1A1A1A.toInt()
            else -> 0xFF7FB3D5.toInt()
        }
        "poop" -> when (ch) {
            'o' -> 0xFFB27D4E.toInt()
            'w' -> 0xFFFFFFFF.toInt()
            'k' -> 0xFF1A1A1A.toInt()
            else -> 0xFF8A5A33.toInt()
        }
        "drop" -> 0xFF6FB8FF.toInt()
        "flake" -> 0xFFFFFFFF.toInt()
        "sweat" -> 0xFF9BD3FF.toInt()
        "stink" -> 0xFF8BC34A.toInt()
        "bubble" -> if (ch == 'o') 0xFFFFFFFF.toInt() else 0xFFBFE6FF.toInt()
        "ball" -> if (ch == 'w') 0xFFFFFFFF.toInt() else 0xFFE5534B.toInt()
        "file" -> if (ch == 'k') 0xFF8FA3BF.toInt() else 0xFFFFFFFF.toInt()
        "bed" -> when (ch) {
            'w' -> 0xFFF2F2F2.toInt()
            'b' -> 0xFF4A6FD0.toInt()
            else -> 0xFF8A5A33.toInt()
        }
        "bowl", "bowl_full" -> when (ch) {
            'f' -> 0xFF7FB3D5.toInt()
            'o' -> 0xFF4F86AD.toInt()
            else -> 0xFFD97757.toInt()
        }
        else -> 0xFFFFFFFF.toInt()
    }

    fun patternBitmap(kind: String, s: Int): Bitmap {
        val rows = patterns.getValue(kind)
        val bmp = Bitmap.createBitmap(rows[0].length * s, rows.size * s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        for ((y, row) in rows.withIndex()) for ((x, ch) in row.withIndex()) {
            if (ch == '.') continue
            paint.color = particleColor(kind, ch)
            canvas.drawRect((x * s).toFloat(), (y * s).toFloat(), ((x + 1) * s).toFloat(), ((y + 1) * s).toFloat(), paint)
        }
        return bmp
    }

    fun render(p: Pose, s: Int, body: Int, eye: Int): Bitmap {
        val bmp = Bitmap.createBitmap(GRID_W * s, GRID_H * s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        fun px(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
            paint.color = color
            canvas.drawRect((x0 * s).toFloat(), (y0 * s).toFloat(), (x1 * s).toFloat(), (y1 * s).toFloat(), paint)
        }

        val blushColor = mix(body, 0xFFFF4F79.toInt(), 0.5f)
        var left = 3
        var right = 15
        var top = 3
        var bottom = 10
        if (p.squash > 0) {
            left = 2; right = 16; top = 5
        } else if (p.squash < 0) {
            left = 4; right = 14; top = 2
        }
        if (p.legs == "tuck") {
            top += 1
            bottom += 1
        }

        val heights = when (p.legs) {
            "walk1" -> intArrayOf(1, 2, 1, 2)
            "walk2" -> intArrayOf(2, 1, 2, 1)
            "dangle" -> intArrayOf(3, 3, 3, 3)
            "tuck" -> intArrayOf(1, 1, 1, 1)
            else -> intArrayOf(2, 2, 2, 2)
        }
        val cols = intArrayOf(left + 1, left + 3, right - 4, right - 2)
        for (i in 0..3) px(cols[i], bottom, cols[i] + 1, bottom + heights[i], body)

        fun arm(side: Int, style: String) {
            when (style) {
                "up" -> if (side < 0) {
                    px(left - 2, top - 1, left - 1, top + 3, body)
                    px(left - 1, top + 2, left, top + 3, body)
                } else {
                    px(right + 1, top - 1, right + 2, top + 3, body)
                    px(right, top + 2, right + 1, top + 3, body)
                }
                "tuck" -> if (side < 0) px(left - 1, top + 3, left, top + 5, body)
                else px(right, top + 3, right + 1, top + 5, body)
                else -> if (side < 0) px(left - 2, top + 3, left, top + 5, body)
                else px(right, top + 3, right + 2, top + 5, body)
            }
        }
        val (leftArm, rightArm) = when (p.arms) {
            "wave1" -> "up" to "normal"
            "wave2" -> "normal" to "up"
            else -> p.arms to p.arms
        }
        arm(-1, leftArm)
        arm(1, rightArm)

        px(left, top, right, bottom, body)

        val ey = top + 2
        for ((side, cx) in listOf(-1 to left + 3 + p.look, 1 to right - 4 + p.look)) {
            when (p.eyes) {
                "blink" -> px(cx, ey + 1, cx + 1, ey + 2, eye)
                "happy" -> {
                    px(cx - 1, ey + 1, cx, ey + 2, eye)
                    px(cx, ey, cx + 1, ey + 1, eye)
                    px(cx + 1, ey + 1, cx + 2, ey + 2, eye)
                }
                "surprised" -> px(cx, ey - 1, cx + 1, ey + 2, eye)
                "dizzy" -> for ((dx, dy) in listOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1)) {
                    px(cx + dx, ey + dy, cx + dx + 1, ey + dy + 1, eye)
                }
                "sleep" -> px(cx - 1, ey + 1, cx + 2, ey + 2, eye)
                "angry" -> {
                    px(cx, ey, cx + 1, ey + 2, eye)
                    val brow = if (side < 0) cx - 1 else cx + 1
                    px(brow, ey - 1, brow + 1, ey, eye)
                }
                "sad" -> {
                    px(cx, ey, cx + 1, ey + 2, eye)
                    val brow = if (side < 0) cx + 1 else cx - 1
                    px(brow, ey - 1, brow + 1, ey, eye)
                }
                else -> px(cx, ey, cx + 1, ey + 2, eye)
            }
            if (p.blush) {
                if (side < 0) px(cx - 2, ey + 2, cx, ey + 3, blushColor)
                else px(cx + 1, ey + 2, cx + 3, ey + 3, blushColor)
            }
        }

        // аксессуары через запятую: например «nightcap,scarf»
        p.acc?.split(',')?.forEach { drawAccessory(::px, it, left, right, top, ey, p.look) }

        if (p.mouth.isNotEmpty()) {
            val mx = (left + right) / 2
            val my = ey + 2
            if (p.mouth == "open") px(mx - 1, my, mx + 1, maxOf(my + 1, minOf(my + 2, bottom)), eye)
            else px(mx - 1, my, mx + 1, my + 1, eye)
        }
        return bmp
    }

    private fun drawAccessory(
        px: (Int, Int, Int, Int, Int) -> Unit, acc: String, left: Int, right: Int, top: Int, ey: Int, look: Int,
    ) {
        val (main, extra) = accColors[acc] ?: return
        val mx = (left + right) / 2
        when (acc) {
            "bow" -> {
                val x0 = right - 5
                px(x0, top - 2, x0 + 1, top - 1, main)
                px(x0 + 3, top - 2, x0 + 4, top - 1, main)
                px(x0, top - 1, x0 + 4, top, main)
                px(x0 + 1, top - 1, x0 + 3, top, extra)
            }
            "party" -> {
                px(mx - 2, top - 1, mx + 2, top, main)
                px(mx - 1, top - 2, mx + 1, top - 1, extra)
                if (top >= 3) px(mx - 1, top - 3, mx + 1, top - 2, 0xFFFFFFFF.toInt())
            }
            "crown" -> {
                px(left + 3, top - 1, right - 3, top, main)
                px(left + 3, top - 2, left + 4, top - 1, main)
                px(mx - 1, top - 2, mx + 1, top - 1, main)
                px(right - 4, top - 2, right - 3, top - 1, main)
                px(mx - 1, top - 1, mx + 1, top, extra)
            }
            "santa" -> {
                px(left + 1, top - 1, right - 1, top, extra)
                px(left + 3, top - 2, right - 3, top - 1, main)
                px(right - 3, top - 2, right - 1, top - 1, extra)
            }
            "phones" -> {
                px(left + 1, top - 1, right - 1, top, main)
                px(left - 1, top + 1, left + 1, top + 4, extra)
                px(right - 1, top + 1, right + 1, top + 4, extra)
            }
            "nightcap" -> {
                px(left + 2, top - 1, right - 2, top, main)
                px(left + 4, top - 2, right - 3, top - 1, main)
                px(right - 4, top - 3, right - 2, top - 2, main)
                px(right - 2, top - 3, right - 1, top - 2, extra)
                px(left + 2, top - 1, left + 3, top, extra)
            }
            "umbrella" -> {
                px(left + 2, top - 3, right - 2, top - 2, main)
                px(left - 1, top - 2, right + 1, top - 1, main)
                px(left + 3, top - 2, left + 4, top - 1, 0xFFFFFFFF.toInt())
                px(right - 4, top - 2, right - 3, top - 1, 0xFFFFFFFF.toInt())
                px(mx, top - 1, mx + 1, top, extra)
            }
            "scarf" -> {
                px(left, top + 5, right, top + 6, main)
                var cx = left + 1
                while (cx < right) {
                    px(cx, top + 5, cx + 1, top + 6, extra)
                    cx += 3
                }
                px(right - 3, top + 6, right - 2, top + 8, main)
                px(right - 2, top + 6, right - 1, top + 7, extra)
            }
            "flower" -> {
                px(right - 2, top - 2, right - 1, top - 1, main)
                px(right - 3, top - 1, right - 2, top, main)
                px(right - 1, top - 1, right, top, main)
                px(right - 2, top, right - 1, top + 1, main)
                px(right - 2, top - 1, right - 1, top, extra)
            }
            "eyepatch" -> {
                val rcx = right - 4 + look
                px(rcx - 1, ey - 1, rcx + 2, ey + 2, main)
                if (rcx - 1 > left) px(left, ey - 1, rcx - 1, ey, main)
                if (right > rcx + 2) px(rcx + 2, ey - 1, right, ey, main)
            }
            "halo" -> {
                px(mx - 3, top - 3, mx + 3, top - 2, main)
                px(mx - 4, top - 2, mx - 3, top - 1, extra)
                px(mx + 3, top - 2, mx + 4, top - 1, extra)
            }
            "file" -> {
                // держит файл над головой (руки подняты)
                px(mx - 2, top - 3, mx + 2, top, main)
                px(mx - 1, top - 2, mx + 1, top - 1, extra)
            }
            "glasses" -> {
                val lcx = left + 3 + look
                val rcx = right - 4 + look
                for (cx in intArrayOf(lcx, rcx)) {
                    px(cx - 1, ey, cx + 2, ey + 2, main)
                    px(cx - 1, ey, cx, ey + 1, extra)
                }
                if (rcx - 1 > lcx + 2) px(lcx + 2, ey, rcx - 1, ey + 1, main)
                if (lcx - 1 > left) px(left, ey, lcx - 1, ey + 1, main)
                if (right > rcx + 2) px(rcx + 2, ey, right, ey + 1, main)
            }
        }
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int): Int {
            val x = (a shr shift) and 0xFF
            val y = (b shr shift) and 0xFF
            return (x + (y - x) * t).toInt() and 0xFF
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}

/** Кэш кадров под один размер и цвет, чтобы не рисовать одну и ту же позу заново. */
class SpriteCache {
    private val frames = LinkedHashMap<Pose, Bitmap>()
    private var key = ""

    fun get(pose: Pose, unit: Int, color: Int): Bitmap {
        val k = "$unit/$color"
        if (k != key) {
            frames.clear()
            key = k
        }
        frames[pose]?.let { return it }
        if (frames.size >= 64) frames.remove(frames.keys.first())
        val c = Sprite.colors.getOrElse(color) { Sprite.colors[0] }
        return Sprite.render(pose, unit, c.body, c.eye).also { frames[pose] = it }
    }
}

/** Пиксельные иконки 9×9 для настроек — в стиле самого Clawd. */
object Icons {
    private val map = mapOf(
        "pet" to listOf(".........", ".XXXXXXX.", ".X.XXX.X.", "XXXXXXXXX", ".XXXXXXX.", ".XXXXXXX.", ".X.X.X.X.", ".X.X.X.X.", "........."),
        "size" to listOf("XXXX.....", "XX.......", "X.X......", "X..X.....", "....X....", ".....X..X", "......X.X", ".......XX", ".....XXXX"),
        "color" to listOf("....X....", "...XXX...", "..XXXXX..", ".XXXXXXX.", ".XXXXXXX.", "XXXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX.."),
        "gravity" to listOf("...XXX...", "...XXX...", "...XXX...", "...XXX...", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "...XXX...", "....X...."),
        "walk" to listOf("XX.......", "XX.......", "XX...XX..", "X....XX..", ".....XX..", "XX...X...", "XX.......", ".....XX..", ".....XX.."),
        "talk" to listOf(".........", "XXXXXXXXX", "X.......X", "X.X.X.X.X", "X.......X", "XXXXXXXXX", ".XX......", ".X.......", "........."),
        "sleep" to listOf("...XXXX..", "..XXX....", ".XXX.....", ".XXX.....", ".XXX.....", ".XXX.....", "..XXX....", "...XXXXXX", "....XXXX."),
        "food" to listOf(".........", ".........", "..XXX...X", ".XXXXX.XX", "X.XXXXXX.", ".XXXXX.XX", "..XXX...X", ".........", "........."),
        "poop" to listOf("....X....", "...XX....", "..XXXX...", ".XXXXXX..", ".X.XX.XX.", "XXXXXXXXX", "XXXXXXXXX", ".........", "........."),
        "shake" to listOf(".........", "X...X...X", ".X.X.X.X.", "..X...X..", ".........", "X...X...X", ".X.X.X.X.", "..X...X..", "........."),
        "bell" to listOf("....X....", "...XXX...", "..XXXXX..", "..XXXXX..", "..XXXXX..", ".XXXXXXX.", "XXXXXXXXX", ".........", "...XXX..."),
        "music" to listOf("..XXXXXXX", "..X.....X", "..X.....X", "..X.....X", "..X.....X", "XXX...XXX", "XXX...XXX", "XXX...XXX", "........."),
        "vibro" to listOf("..XXXXX..", "X.X...X.X", "X.X...X.X", "..X...X..", "X.X...X.X", "X.X...X.X", "..X...X..", "..XXXXX..", "........."),
        "heart" to listOf(".........", ".XX...XX.", "XXXX.XXXX", "XXXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "...XXX...", "....X...."),
        "eye" to listOf(".........", "..XXXXX..", ".X.....X.", "X..XXX..X", "X..XXX..X", ".X.....X.", "..XXXXX..", ".........", "........."),
        "text" to listOf("XXXXXXXXX", "XXXXXXXXX", "...XXX...", "...XXX...", "...XXX...", "...XXX...", "...XXX...", "...XXX...", "........."),
        "clock" to listOf("..XXXXX..", ".X..X..X.", "X...X...X", "X...X...X", "X...XXX.X", "X.......X", ".X.....X.", "..XXXXX..", "........."),
        "screen" to listOf("XXXXXXXXX", "X.......X", "X.......X", "X.......X", "X.......X", "XXXXXXXXX", "....X....", "..XXXXX..", "........."),
        "crown" to listOf(".........", "X...X...X", "XX.XXX.XX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX", ".........", ".........", "........."),
        "star" to listOf("....X....", "....X....", "...XXX...", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "..XX.XX..", ".XX...XX.", "........."),
        "stats" to listOf(".........", ".......XX", ".......XX", "....XX.XX", "....XX.XX", ".XX.XX.XX", ".XX.XX.XX", "XXXXXXXXX", "........."),
        "book" to listOf("XXXX.XXXX", "X..X.X..X", "X..X.X..X", "X..X.X..X", "X..X.X..X", "X..X.X..X", "XXXX.XXXX", ".........", "........."),
        "lock" to listOf("..XXXXX..", ".X.....X.", ".X.....X.", "XXXXXXXXX", "XXXX.XXXX", "XXXX.XXXX", "XXXXXXXXX", "XXXXXXXXX", "........."),
        "edit" to listOf(".......XX", "......XXX", ".....XXX.", "....XXX..", "...XXX...", "..XXX....", ".XXX.....", "XX.......", "X........"),
        "link" to listOf(".........", ".XXX.XXX.", "X...X...X", "X..XXX..X", "X...X...X", ".XXX.XXX.", ".........", ".........", "........."),
        "trash" to listOf("...XXX...", "XXXXXXXXX", ".........", ".X.X.X.X.", ".X.X.X.X.", ".X.X.X.X.", ".X.X.X.X.", ".XXXXXXX.", "........."),
        "ai" to listOf("....X....", "....X....", "...XXX...", "XXXXXXXXX", "...XXX...", "....X..X.", "....X.XXX", ".......X.", "........."),
        "battery" to listOf("..XXXXX..", "XXXXXXXXX", "X.......X", "X.XXXXX.X", "X.XXXXX.X", "X.XXXXX.X", "X.......X", "XXXXXXXXX", "........."),
        "target" to listOf("...XXX...", ".XX...XX.", ".X.....X.", "X...X...X", "X..XXX..X", "X...X...X", ".X.....X.", ".XX...XX.", "...XXX..."),
        "retry" to listOf("..XXXX.X.", ".X....XX.", "X....XXX.", "X........", "X.......X", "X.......X", ".X.....X.", "..XXXXX..", "........."),
        "back" to listOf(".........", "...X.....", "..XX.....", ".XXXXXXX.", "XXXXXXXX.", ".XXXXXXX.", "..XX.....", "...X.....", "........."),
        "buddy" to listOf(".........", "XXXX.....", "X.XX.XXX.", "XXXX.X.XX", "XXXX.XXXX", ".X.X.XXXX", "......X.X", ".........", "........."),
        "shield" to listOf("XXXXXXXXX", "X.......X", "X...X...X", "X..XXX..X", "X...X...X", ".X.....X.", "..X...X..", "...XXX...", "........."),
        "mic" to listOf("...XXX...", "..XXXXX..", "..XXXXX..", "..XXXXX..", "..XXXXX..", "X.XXXXX.X", ".X.....X.", "..XXXXX..", "....X...."),
        "rotate" to listOf("XXXXX....", "X...X....", "X...X..X.", "X...X...X", "X...X..X.", "X...X....", "XXXXXXXXX", "....X...X", "....XXXXX"),
        "game" to listOf(".........", ".XXXXXXX.", "XX.XXXXXX", "X...XX.XX", "XX.XXXXXX", "XXXXX.XXX", "XX.....XX", ".........", "........."),
    )

    fun get(name: String, color: Int): Drawable = PixelIcon(map[name] ?: map.getValue("pet"), color)

    private class PixelIcon(private val rows: List<String>, color: Int) : Drawable() {
        private val paint = Paint().apply { this.color = color }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val cell = minOf(b.width(), b.height()) / 9f
            val ox = b.left + (b.width() - cell * 9) / 2
            val oy = b.top + (b.height() - cell * 9) / 2
            for ((y, row) in rows.withIndex()) for ((x, ch) in row.withIndex()) {
                if (ch != 'X') continue
                canvas.drawRect(ox + x * cell, oy + y * cell, ox + (x + 1) * cell, oy + (y + 1) * cell, paint)
            }
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
