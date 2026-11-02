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
        // как на ПК: 10–15 открыты сразу, дальше — за число ачивок или за конкретную
        PetColors("Вишнёвый", 0xFFC2405A.toInt(), 0xFF1A0A10.toInt()),
        PetColors("Апельсин", 0xFFF28A2E.toInt(), 0xFF2A1405.toInt()),
        PetColors("Банан", 0xFFF2D04A.toInt(), 0xFF2A2205.toInt()),
        PetColors("Изумруд", 0xFF2FA36B.toInt(), 0xFF08201A.toInt()),
        PetColors("Бирюза", 0xFF2BC4C4.toInt(), 0xFF062424.toInt()),
        PetColors("Персик", 0xFFF5B08A.toInt(), 0xFF2A160C.toInt()),
        PetColors("Шоколад", 0xFF7A4A2E.toInt(), 0xFFF2E6DC.toInt()),
        PetColors("Снежный", 0xFFFFFFFF.toInt(), 0xFF1A1A2A.toInt()),
        PetColors("Сапфир", 0xFF3156C9.toInt(), 0xFFF2F2F2.toInt()),
        PetColors("Сирень", 0xFFC08AE8.toInt(), 0xFF22142E.toInt()),
        PetColors("Фуксия", 0xFFE04FB0.toInt(), 0xFF2A0A20.toInt()),
        PetColors("Кофе с молоком", 0xFFB08A6A.toInt(), 0xFF24180F.toInt()),
        PetColors("Песок", 0xFFD9C08A.toInt(), 0xFF2A2010.toInt()),
        PetColors("Морской", 0xFF1F7A8C.toInt(), 0xFFE8FBFF.toInt()),
        PetColors("Клубника", 0xFFFF5A7A.toInt(), 0xFFFFFFFF.toInt()),
        PetColors("Оливка", 0xFF8A9A3A.toInt(), 0xFF16180A.toInt()),
        PetColors("Ледяной", 0xFFBFE8FF.toInt(), 0xFF1A3A5A.toInt()),
        PetColors("Розовое золото", 0xFFE6A89A.toInt(), 0xFF3A1A14.toInt()),
        PetColors("Мята-шоколад", 0xFFA6E6C8.toInt(), 0xFF4A2A1A.toInt()),
        PetColors("Тыква", 0xFFFF8C1A.toInt(), 0xFF1A0F05.toInt()),
        PetColors("Уголь", 0xFF26262C.toInt(), 0xFFFF5A5A.toInt()),
        PetColors("Неон", 0xFF39FF8A.toInt(), 0xFF06200F.toInt()),
        PetColors("Кибер", 0xFF00E5FF.toInt(), 0xFF1A0033.toInt()),
        PetColors("Лава", 0xFFFF4A1A.toInt(), 0xFFFFE14A.toInt()),
    )

    /** Замок окраса (индекс в colors): id ачивки или сколько ачивок всего; нет в списке — открыт. */
    val colorNeed: Map<Int, Any> = mapOf(
        6 to "pet100", 7 to "eat50", 8 to "clean10", 9 to "pomo5",
        16 to 1, 17 to 2, 18 to 3, 19 to 4, 20 to 5, 21 to 6, 22 to 7, 23 to 8, 24 to 9, 25 to 10, 26 to 11, 27 to 12,
        28 to 13, 29 to 14, 30 to "throw50", 31 to "seek10", 32 to "tag5", 33 to "style10",
    )

    fun colorOpen(i: Int, done: Set<String>) = Wardrobe.needOpen(colorNeed[i], done)

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

    /** Значки частиц ауры: «aura|значок|цвет» — например aura|star|-16384. */
    private val auraGlyphs = mapOf(
        "dot" to listOf("XX", "XX"),
        "ring" to listOf(".X.", "X.X", ".X."),
        "oval" to listOf(".X.", "XXX", ".X."),
        "star" to listOf(".X.", "XXX", ".X."),
        "spark" to listOf("..X..", "..X..", "XX.XX", "..X..", "..X.."),
        "note" to listOf("..XXX", "..X.X", "..X.X", "XXX.X", "XXX.."),
        "money" to listOf(".X.", "XXX", "X..", "XXX", "..X", "XXX", ".X."),
        "zap" to listOf("..X", ".X.", "XXX", ".X.", "X.."),
    )

    /** Какой значок у символа ауры с ПК (там это текст). */
    fun auraGlyph(shape: String, ch: Char?): String = when (shape) {
        "ring" -> "ring"
        "oval" -> "oval"
        "text" -> when (ch) {
            '♪', '♫' -> "note"
            '$' -> "money"
            'ϟ' -> "zap"
            '✦' -> "spark"
            else -> "star"
        }
        else -> "dot"
    }

    fun patternBitmap(kind: String, s: Int): Bitmap {
        val aura = kind.startsWith("aura|")
        val rows = if (aura) auraGlyphs.getValue(kind.split('|')[1]) else patterns.getValue(kind)
        val auraColor = if (aura) kind.split('|')[2].toInt() else 0
        val bmp = Bitmap.createBitmap(rows[0].length * s, rows.size * s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        for ((y, row) in rows.withIndex()) for ((x, ch) in row.withIndex()) {
            if (ch == '.') continue
            paint.color = if (aura) auraColor else particleColor(kind, ch)
            canvas.drawRect((x * s).toFloat(), (y * s).toFloat(), ((x + 1) * s).toFloat(), ((y + 1) * s).toFloat(), paint)
        }
        return bmp
    }

    /**
     * Кадр 18×13 клеток плюс одинаковый запас pad со всех сторон под высокие шапки, крылья и вещи в лапе
     * (pad = Wardrobe.pad(p.acc); без одежды — 0, кадр как раньше).
     */
    fun render(p: Pose, s: Int, body: Int, eye: Int): Bitmap {
        val pad = Wardrobe.pad(p.acc)
        val bmp = Bitmap.createBitmap((GRID_W + 2 * pad) * s, (GRID_H + 2 * pad) * s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        fun px(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
            paint.color = color
            canvas.drawRect(((x0 + pad) * s).toFloat(), ((y0 + pad) * s).toFloat(), ((x1 + pad) * s).toFloat(), ((y1 + pad) * s).toFloat(), paint)
        }
        val worn = p.acc?.split(',').orEmpty()

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
        val ey = top + 2
        // крылья, плащ, хвост — за телом
        Wardrobe.draw(::px, worn, "back", left, right, top, bottom, ey, p.look, body, eye)
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
        Wardrobe.draw(::px, worn, "pattern", left, right, top, bottom, ey, p.look, body, eye)

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

        if (p.mouth.isNotEmpty()) {
            val mx = (left + right) / 2
            val my = ey + 2
            if (p.mouth == "open") px(mx - 1, my, mx + 1, maxOf(my + 1, minOf(my + 2, bottom)), eye)
            else px(mx - 1, my, mx + 1, my + 1, eye)
        }
        // одежда через запятую: например «tophat,round,scarf»; в поднятой лапе вещь не держит
        val carry = "file" in worn
        Wardrobe.draw(::px, worn, "front", left, right, top, bottom, ey, p.look, body, eye, handsFree = !carry && rightArm != "up")
        if (carry) drawFile(::px, left, right, top)
        return bmp
    }

    /** Не из гардероба: листок с файлом над головой (руки подняты). */
    private fun drawFile(px: (Int, Int, Int, Int, Int) -> Unit, left: Int, right: Int, top: Int) {
        val mx = (left + right) / 2
        px(mx - 2, top - 3, mx + 2, top, 0xFFFFFFFF.toInt())
        px(mx - 1, top - 2, mx + 1, top - 1, 0xFF8FA3BF.toInt())
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
