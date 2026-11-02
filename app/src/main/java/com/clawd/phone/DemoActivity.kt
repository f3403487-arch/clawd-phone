package com.clawd.phone

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/** Однотонный фон для съёмки роликов: ни обоев, ни значков, ни уведомлений в кадре. */
class DemoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("off", false)) {
            finish()
            return
        }
        setContentView(object : View(this) {
            val floor = Paint().apply { color = Color.parseColor("#0F1828") }
            override fun onDraw(c: Canvas) {
                c.drawColor(Color.parseColor("#1B2A42"))
                val d = resources.displayMetrics.density
                val top = height - 48 * d
                c.drawRect(0f, top, width.toFloat(), height.toFloat(), floor)
                var x = 0f
                while (x < width) { // пиксельная кромка пола
                    c.drawRect(x, top - 6 * d, x + 14 * d, top, floor)
                    x += 28 * d
                }
            }
        })
        if (android.os.Build.VERSION.SDK_INT >= 30) window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("off", false)) finish()
    }
}
