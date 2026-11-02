package com.clawd.phone

import android.content.Context

/** Частицы ауры: форма (rect/ring/oval/heart/text), цвета, раз в сколько секунд, падают ли сверху. */
class WAura(val shape: String, val colors: IntArray, val text: String, val every: Float, val fall: Boolean)

/**
 * Вещь из гардероба. Нарисована строками: буква — цвет из pal, «.» — пусто,
 * B/D/L — цвет тела (как есть, темнее, светлее), E — цвет глаз. Подробно — в clawd_wardrobe.py на ПК.
 */
class WItem(
    val key: String,
    val slot: String,
    val name: String,
    /** null — открыто сразу, "id" — нужна ачивка, число — сколько ачивок всего. */
    val need: Any?,
    val rows: List<String> = emptyList(),
    val pal: Map<Char, Int> = emptyMap(),
    val dx: Int = 0,
    val dy: Int = 0,
    val left: List<String> = emptyList(),
    val right: List<String> = emptyList(),
    val bridge: List<Pair<Int, Char>> = emptyList(),
    val edge: List<Pair<Int, Char>> = emptyList(),
    val aura: WAura? = null,
    /** Надевается сам (зонт, колпак, шапка Санты) — в шкафу не показываем. */
    val auto: Boolean = false,
)

/** Гардероб Clawd: те же 7 слотов и вещи, что у Clawd на компе. Что надето — в настройках w_<слот>. */
object Wardrobe {
    val slots get() = WardrobeData.SLOTS
    val items get() = WardrobeData.ITEMS
    val slotKeys = WardrobeData.SLOTS.map { it.first }

    /** Старые аксессуары открывались привязанностью — им она засчитывается и сейчас. */
    private val legacyBond = mapOf("bow" to 15f, "party" to 25f, "glasses" to 35f, "phones" to 60f, "crown" to 85f)

    fun pref(slot: String) = "w_$slot"

    fun needText(need: Any?): String = when (need) {
        null -> ""
        is Int -> "$need " + Brain.plural(need, "ачивка", "ачивки", "ачивок")
        else -> "ачивка «" + (Achievements.all.firstOrNull { it.key == need }?.title ?: need) + "»"
    }

    fun needOpen(need: Any?, done: Set<String>): Boolean = when (need) {
        null -> true
        is Int -> done.size >= need
        else -> need in done
    }

    fun isOpen(key: String, done: Set<String>, bond: Float?): Boolean {
        val it = items[key] ?: return false
        if (it.auto || needOpen(it.need, done)) return true
        val b = legacyBond[key] ?: return false
        return bond == null || bond >= b
    }

    fun openKeys(context: Context, prefs: Prefs): Set<String> {
        val done = Achievements.done(prefs)
        val bond = PetService.bondValue(context, prefs)
        return items.keys.filter { isOpen(it, done, bond) }.toSet()
    }

    /** Что выбрано и открыто: слот → вещь (без авто-вещей по погоде, их добавляет сервис). */
    fun worn(context: Context, prefs: Prefs): Map<String, String> {
        migrate(prefs)
        val done = Achievements.done(prefs)
        val bond = PetService.bondValue(context, prefs)
        val out = LinkedHashMap<String, String>()
        for (slot in slotKeys) {
            val key = prefs.s(pref(slot))
            val it = items[key] ?: continue
            if (it.slot == slot && !it.auto && isOpen(key, done, bond)) out[slot] = key
        }
        return out
    }

    /** Раньше был один аксессуар (номер в списке) — переносим его в слот. */
    private fun migrate(prefs: Prefs) {
        if (prefs.b("w_migrated")) return
        prefs.put("w_migrated", true)
        val idx = prefs.i("acc")
        val key = if (idx >= 2) Sprite.accessories.getOrNull(idx - 2)?.key else null
        val it = items[key ?: return] ?: return
        if (slotKeys.none { s -> prefs.s(pref(s)).isNotEmpty() }) prefs.put(pref(it.slot), it.key)
    }

    /** Надеть (или снять, если key пустой). Новые вещи идут в зачёт «Модника». */
    fun wear(prefs: Prefs, slot: String, key: String) {
        prefs.put(pref(slot), key)
        if (key.isEmpty()) return
        val tried = prefs.s("w_tried").split(',').filter { it.isNotEmpty() }.toMutableSet()
        if (tried.add(key)) {
            prefs.put("w_tried", tried.joinToString(","))
            prefs.put("tried_count", tried.size)
        }
    }

    // ---------- рисование (тот же алгоритм, что clawd_wardrobe.draw) ----------

    fun shade(c: Int, t: Float): Int {
        fun ch(shift: Int): Int {
            val v = (c shr shift) and 0xFF
            return Math.round(if (t < 0) v * (1 + t) else v + (255 - v) * t).coerceIn(0, 255)
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun color(ch: Char, pal: Map<Char, Int>, body: Int, eye: Int): Int? = when (ch) {
        'B' -> body
        'D' -> shade(body, -0.28f)
        'L' -> shade(body, 0.32f)
        'E' -> eye
        else -> pal[ch]
    }

    private fun stamp(px: (Int, Int, Int, Int, Int) -> Unit, rows: List<String>, pal: Map<Char, Int>, x0: Int, y0: Int, body: Int, eye: Int) {
        for ((r, row) in rows.withIndex()) {
            var c = 0
            while (c < row.length) {
                val ch = row[c]
                if (ch == '.') {
                    c++
                    continue
                }
                var end = c + 1
                while (end < row.length && row[end] == ch) end++
                color(ch, pal, body, eye)?.let { px(x0 + c, y0 + r, x0 + end, y0 + r + 1, it) }
                c = end
            }
        }
    }

    private fun fill(px: (Int, Int, Int, Int, Int) -> Unit, a: Int, b: Int, y: Int, ch: Char, pal: Map<Char, Int>, body: Int, eye: Int) {
        if (b > a) color(ch, pal, body, eye)?.let { px(a, y, b, y + 1, it) }
    }

    /** phase: back — до тела, pattern — по телу, front — поверх глаз и рта. */
    fun draw(
        px: (Int, Int, Int, Int, Int) -> Unit, keys: List<String>, phase: String,
        left: Int, right: Int, top: Int, bottom: Int, ey: Int, look: Int, body: Int, eye: Int, handsFree: Boolean = true,
    ) {
        val mx = (left + right) / 2
        val worn = keys.mapNotNull { items[it] }
        fun cx(it: WItem) = mx - it.rows[0].length / 2 + it.dx
        when (phase) {
            "back" -> worn.filter { it.slot == "back" }.forEach { stamp(px, it.rows, it.pal, cx(it), top + it.dy, body, eye) }
            "pattern" -> for (it in worn) {
                if (it.slot != "pattern") continue
                val h = it.rows.size
                val w = it.rows[0].length
                for (y in top until bottom) {
                    val row = String(CharArray(right - left) { i -> it.rows[(y - top) % h][i % w] })
                    stamp(px, listOf(row), it.pal, left, y, body, eye)
                }
            }
            else -> {
                val order = listOf("face", "neck", "head", "hand")
                for (it in worn.filter { it.slot in order }.sortedBy { order.indexOf(it.slot) }) when (it.slot) {
                    "face" -> {
                        val lcx = left + 3 + look
                        val rcx = right - 4 + look
                        val y0 = ey - 1 + it.dy
                        stamp(px, it.left, it.pal, lcx - 2, y0, body, eye)
                        stamp(px, it.right, it.pal, rcx - 2, y0, body, eye)
                        for ((row, ch) in it.bridge) fill(px, lcx + 3, rcx - 2, y0 + row, ch, it.pal, body, eye)
                        for ((row, ch) in it.edge) {
                            fill(px, left, lcx - 2, y0 + row, ch, it.pal, body, eye)
                            fill(px, rcx + 3, right, y0 + row, ch, it.pal, body, eye)
                        }
                    }
                    "neck" -> stamp(px, it.rows, it.pal, cx(it), top + 5 + it.dy, body, eye)
                    "head" -> stamp(px, it.rows, it.pal, cx(it), top - it.rows.size + it.dy, body, eye)
                    "hand" -> if (handsFree) stamp(px, it.rows, it.pal, right + 1 + it.dx, top + 5 - it.rows.size + it.dy, body, eye)
                }
            }
        }
    }

    private val padCache = HashMap<String, Int>()

    /**
     * Сколько клеток запаса нужно вокруг кадра 18×13, чтобы вещи не обрезались (одинаково со всех сторон —
     * кадр рисуется по центру окна и поворачивается). Считается по всем позам тела.
     */
    fun pad(acc: String?): Int {
        if (acc.isNullOrEmpty()) return 0
        return padCache.getOrPut(acc) {
            val keys = acc.split(',')
            var minX = 0
            var minY = 0
            var maxX = Sprite.GRID_W
            var maxY = Sprite.GRID_H
            val px = { x0: Int, y0: Int, x1: Int, y1: Int, _: Int ->
                minX = minOf(minX, x0); minY = minOf(minY, y0); maxX = maxOf(maxX, x1); maxY = maxOf(maxY, y1)
            }
            // обычный, сплющенный, вытянутый, свернувшийся во сне
            for ((l, r, t, b) in listOf(listOf(3, 15, 3, 10), listOf(2, 16, 5, 10), listOf(4, 14, 2, 10), listOf(3, 15, 4, 11))) {
                for (look in -1..1) for (phase in listOf("back", "pattern", "front")) {
                    draw(px, keys, phase, l, r, t, b, t + 2, look, 0, 0)
                }
            }
            maxOf(0, -minX, -minY, maxX - Sprite.GRID_W, maxY - Sprite.GRID_H)
        }
    }
}
