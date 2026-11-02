package com.clawd.phone

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt
import kotlin.random.Random

/** Вкладка «Гардероб»: живой Clawd в обновке, слоты, сетка вещей с превью. Всё сохраняется в w_<слот>. */
@SuppressLint("ViewConstructor")
class WardrobeView(context: Context, private val prefs: Prefs) : ScrollView(context) {
    private val t = TgTheme(context)
    private val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val count = TextView(context)
    private val tabs = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val styleRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private var slot = "head"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    init {
        setBackgroundColor(t.gray)
        addView(col)
        col.setPadding(dp(12), dp(8), dp(8), dp(24))
        col.addView(card().apply {
            addView(PreviewView(context, prefs), LinearLayout.LayoutParams(-1, dp(170)))
            addView(count.apply {
                textSize = 13f
                setTextColor(t.sub)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(10))
            })
        })
        col.addView(View(context), LinearLayout.LayoutParams(-1, dp(12)))
        col.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabs)
        })
        col.addView(View(context), LinearLayout.LayoutParams(-1, dp(10)))
        col.addView(styleRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        col.addView(grid)
        val buttons = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(button("🎲 Случайный образ", true) { randomOutfit() }, LinearLayout.LayoutParams(0, dp(52), 1f))
        buttons.addView(View(context), LinearLayout.LayoutParams(dp(8), 1))
        buttons.addView(button("Снять всё", false) { clearAll() }, LinearLayout.LayoutParams(0, dp(52), 1f))
        col.addView(buttons, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        col.addView(TextView(context).apply {
            text = "Одновременно — по одной вещи в каждом слоте. Закрытые открываются ачивками (вкладка «Настройки» → Достижения). " +
                "Зонт в дождь, шарф в мороз и ночной колпак Clawd надевает сам поверх выбранного."
            textSize = 13f
            setTextColor(t.sub)
            setLineSpacing(0f, 1.15f)
            setPadding(dp(8), dp(14), dp(8), 0)
        })
        refresh()
    }

    private fun card() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val d = resources.displayMetrics.density
        background = PixelDrawable(t.cell, t.divider, t.shadow, step = 3 * d, bw = 2 * d, shadowOff = 4 * d)
        setPadding(dp(2), dp(2), dp(6), dp(6))
    }

    private fun button(text: String, main: Boolean, onClick: () -> Unit) = TextView(context).apply {
        this.text = text
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(if (main) 0xFF1A0F0A.toInt() else t.accent)
        val d = resources.displayMetrics.density
        background = PixelDrawable(if (main) t.accent2 else t.cell2, if (main) null else t.divider, t.shadow,
            step = 3 * d, bw = 2 * d, shadowOff = 3 * d)
        setOnClickListener { onClick() }
    }

    fun refresh() {
        val open = Wardrobe.openKeys(context, prefs)
        val all = Wardrobe.items.values.filter { !it.auto }
        count.text = "Открыто ${all.count { it.key in open }} из ${all.size}"

        tabs.removeAllViews()
        for ((key, name, icon) in Wardrobe.slots) {
            val on = key == slot
            val worn = prefs.s(Wardrobe.pref(key)).isNotEmpty()
            tabs.addView(TextView(context).apply {
                text = "$icon $name" + if (worn) " •" else ""
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (on) 0xFF1A0F0A.toInt() else t.sub)
                setPadding(dp(14), dp(10), dp(14), dp(12))
                val d = resources.displayMetrics.density
                background = PixelDrawable(if (on) t.accent2 else t.cell, if (on) null else t.divider, null, step = 3 * d, bw = 2 * d)
                setOnClickListener {
                    slot = key
                    refresh()
                }
            }, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6) })
        }

        // во вкладке «Аура» — как она выглядит: свечение, частицы или всё вместе
        styleRow.removeAllViews()
        styleRow.visibility = if (slot == "aura") View.VISIBLE else View.GONE
        val style = prefs.i("aura_style")
        Wardrobe.AURA_STYLES.forEachIndexed { i, name ->
            val on = i == style
            val d = resources.displayMetrics.density
            styleRow.addView(TextView(context).apply {
                text = name
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (on) 0xFF1A0F0A.toInt() else t.sub)
                setPadding(dp(6), dp(10), dp(6), dp(10))
                background = PixelDrawable(if (on) t.accent2 else t.cell, if (on) null else t.divider, null, step = 3 * d, bw = 2 * d)
                setOnClickListener {
                    prefs.put("aura_style", i)
                    toast(name)
                    refresh()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { if (i < 2) marginEnd = dp(6) })
        }

        grid.removeAllViews()
        // сначала открытые, потом то, что откроется раньше
        val rank = { it: WItem -> if (it.key in open) -1 else (it.need as? Int) ?: 99 }
        val list = all.filter { it.slot == slot }.sortedBy(rank)
        val color = Sprite.colors[PetService.colorFor(prefs)]
        for (rowItems in list.chunked(3)) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (i in 0 until 3) {
                val it = rowItems.getOrNull(i)
                val lp = LinearLayout.LayoutParams(0, -2, 1f).apply { if (i < 2) marginEnd = dp(8) }
                row.addView(if (it == null) View(context) else cell(it, it.key in open, color, style), lp)
            }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun cell(it: WItem, open: Boolean, color: PetColors, style: Int): View {
        val on = prefs.s(Wardrobe.pref(it.slot)) == it.key
        val d = resources.displayMetrics.density
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(6), dp(4), dp(8))
            background = PixelDrawable(if (on) 0xFF2B1F19.toInt() else t.cell, if (on) t.accent2 else t.divider, null,
                step = 3 * d, bw = (if (on) 3 else 1) * d)
            addView(ItemPreview(context, it, color, open, style), LinearLayout.LayoutParams(-1, dp(72)))
            addView(TextView(context).apply {
                text = it.name
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (open) t.text else t.sub)
                maxLines = 2
            })
            if (!open) addView(TextView(context).apply {
                text = "🔒 " + Wardrobe.needText(it.need)
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(t.accent)
            })
            setOnClickListener { _ ->
                if (!open) {
                    toast("🔒 ${it.name}: ${Wardrobe.needText(it.need)}")
                    return@setOnClickListener
                }
                Wardrobe.wear(prefs, it.slot, if (on) "" else it.key)
                if (!on) toast(it.name)
                refresh()
            }
        }
    }

    private fun randomOutfit() {
        val open = Wardrobe.openKeys(context, prefs)
        for ((key, _, _) in Wardrobe.slots) {
            val opts = Wardrobe.items.values.filter { it.slot == key && !it.auto && it.key in open }
            // не всё сразу: каждый слот с шансом, чтобы образы были разные
            Wardrobe.wear(prefs, key, if (opts.isNotEmpty() && Random.nextFloat() < 0.65f) opts.random().key else "")
        }
        toast("Новый образ 🎲")
        refresh()
    }

    private fun clearAll() {
        for ((key, _, _) in Wardrobe.slots) Wardrobe.wear(prefs, key, "")
        toast("Снял всё")
        refresh()
    }

    private fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

/** Картинка вещи: Clawd в ней (для ауры — Clawd в облаке частиц), закрытая — тёмная. */
@SuppressLint("ViewConstructor")
private class ItemPreview(
    context: Context, private val item: WItem, private val color: PetColors, private val open: Boolean, private val auraStyle: Int,
) : View(context) {
    private val dim = Paint().apply { alpha = 90 }

    override fun onDraw(canvas: Canvas) {
        val unit = maxOf(2, (3 * resources.displayMetrics.density).roundToInt())
        // «только частицы» — без свечения, «только свечение» — без частиц
        val acc = if (item.slot == "aura" && auraStyle == 1) null else item.key
        val bmp = Sprite.render(Pose(acc = acc), unit, color.body, color.eye)
        val s = minOf(1f, width.toFloat() / bmp.width, height.toFloat() / bmp.height)
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(s, s)
        val a = item.aura
        if (a != null && auraStyle != 2) {
            val spots = listOf(-1.1f to -0.6f, 1.0f to -0.8f, -0.9f to 0.3f, 1.1f to 0.2f, 0f to -1.1f, -1.3f to -0.2f)
            spots.forEachIndexed { i, (fx, fy) ->
                val c = if (a.colors.isNotEmpty()) a.colors[i % a.colors.size] else 0xFFFF5C7A.toInt()
                val glyph = if (a.shape == "heart") null else Sprite.auraGlyph(a.shape, a.text.getOrNull(i % maxOf(1, a.text.length)))
                val g = if (glyph == null) Sprite.patternBitmap("heart", maxOf(1, unit / 2))
                else Sprite.patternBitmap("aura|$glyph|$c", maxOf(1, unit * 2 / 3))
                canvas.drawBitmap(g, fx * bmp.width / 2.2f - g.width / 2f, fy * bmp.height / 2.2f - g.height / 2f, if (open) null else dim)
            }
        }
        canvas.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, if (open) null else dim)
        canvas.restore()
    }
}
