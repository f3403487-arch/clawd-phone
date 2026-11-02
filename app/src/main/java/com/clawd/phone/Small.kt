package com.clawd.phone

import android.annotation.SuppressLint
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.MotionEvent
import android.view.View

/** Вью, которое рисует кадр спрайта, повёрнутый так, чтобы ноги смотрели в сторону «низа». */
open class SpriteView(context: Context) : View(context) {
    var bitmap: Bitmap? = null
    var angle = 0f

    /** Подогнать кадр под размер окна (когда Clawd влетает в Clawd из Telegram и меняет размер). */
    var fit = false

    /** Дополнительное вращение в полёте. */
    var spin = 0f

    /** Где центр спрайта по вертикали (-1 — по центру окна; для пряток окно обрезано снизу). */
    var centerY = -1f

    override fun onDraw(canvas: Canvas) {
        val b = bitmap ?: return
        canvas.save()
        canvas.translate(width / 2f, if (centerY >= 0) centerY else height / 2f)
        canvas.rotate(angle + spin)
        if (fit) {
            val swap = Math.abs(angle) == 90f
            val bw = if (swap) b.height else b.width
            val bh = if (swap) b.width else b.height
            val s = minOf(width.toFloat() / bw, height.toFloat() / bh)
            canvas.scale(s, s)
        }
        canvas.drawBitmap(b, -b.width / 2f, -b.height / 2f, null)
        canvas.restore()
    }
}

/** Сам питомец: касания отдаёт сервису. */
@SuppressLint("ViewConstructor")
class PetView(context: Context, private val service: PetService) : SpriteView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent) = service.onPetTouch(event)
}

/** Уголок Clawd: кроватка, миска (с рыбкой, если сыт) и мячик. Тап — по зоне. */
@SuppressLint("ViewConstructor")
class HomeView(context: Context, private val onTap: (String) -> Unit) : View(context) {
    var pixel = 6
        set(value) {
            if (value != field) cache.clear()
            field = value
        }
    var full = false
    var ballHere = true
    private val cache = HashMap<String, Bitmap>()

    private fun bmp(kind: String) = cache.getOrPut(kind) { Sprite.patternBitmap(kind, pixel) }

    override fun onDraw(canvas: Canvas) {
        val p = pixel.toFloat()
        canvas.drawBitmap(bmp("bed"), 0f, 0f, null)
        canvas.drawBitmap(bmp(if (full) "bowl_full" else "bowl"), 18 * p, 3 * p, null)
        if (ballHere) canvas.drawBitmap(bmp("ball"), 28 * p, 0f, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val cell = event.x / pixel
            onTap(
                when {
                    cell < 17 -> "bed"
                    cell < 27 -> "bowl"
                    else -> "ball"
                },
            )
        }
        return true
    }
}

/** Поднимает питомца после перезагрузки, если он был включён. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs(context).enabled && Settings.canDrawOverlays(context)) PetService.start(context)
    }
}

/** Слушает уведомления: кто пишет и о чём (текст анализируется на телефоне, наружу — только если разрешить ИИ). */
class NotifyListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val service = PetService.instance ?: return
        val app = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            return
        }
        val isMessage = n.category == Notification.CATEGORY_MESSAGE
        val who = if (isMessage) {
            n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() && it.length <= 24 }
        } else null
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.take(400)
        service.onNotification(sbn.packageName, app, who, text, isMessage)
    }
}
