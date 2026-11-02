package com.clawd.phone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import java.util.concurrent.Executors

/** Видимый элемент интерфейса другого приложения. */
class UiNode(
    val rect: Rect,
    val text: String?,
    val clickable: Boolean,
    val editable: Boolean,
    val password: Boolean,
    val id: String? = null,
)

/**
 * «Глаза» Clawd: через спецвозможности узнаёт, какое приложение открыто, где кнопки
 * (на них можно стоять и их можно «съесть»), какой текст на экране и где клавиатура.
 * Всё обрабатывается на телефоне; наружу текст уходит только если включить это для ИИ.
 */
class ClawdAccess : AccessibilityService() {

    companion object {
        var instance: ClawdAccess? = null
            private set

        fun enabled(context: Context): Boolean {
            val me = ComponentName(context, ClawdAccess::class.java).flattenToString()
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return list.split(':').any { it.equals(me, ignoreCase = true) }
        }

        private const val MAX_NODES = 700
    }

    private val worker = Executors.newSingleThreadExecutor()
    var currentPkg = ""
        private set
    private var homePkg: String? = null
    private var imePkgs = emptySet<String>()
    private var cache: List<UiNode>? = null
    private var cacheAt = 0L

    override fun onServiceConnected() {
        instance = this
        homePkg = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0,
        )?.activityInfo?.packageName
        imePkgs = getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }.toSet()
        PetService.instance?.onAccessChanged()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        PetService.instance?.onAccessChanged()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        worker.shutdown()
        super.onDestroy()
    }

    override fun onInterrupt() {}

    fun isHome(pkg: String) = pkg == homePkg

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        val pet = PetService.instance
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                cache = null
                if (pkg != currentPkg && pkg != "com.android.systemui" && pkg !in imePkgs && pkg != "android") {
                    currentPkg = pkg
                    pet?.onAppChanged(pkg)
                } else pet?.onScreenChanged(false)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                cache = null
                pet?.onScreenChanged(true)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                cache = null
                pet?.onScreenChanged(false)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                if (e.isPassword) return
                val text = e.text?.joinToString("")?.toString() ?: return
                pet?.onTyped(text)
            }
        }
    }

    /** Видимые элементы активного окна (кэш на 400 мс — события сыплются пачками). */
    fun nodes(): List<UiNode> {
        val now = SystemClock.uptimeMillis()
        cache?.let { if (now - cacheAt < 400) return it }
        val out = ArrayList<UiNode>()
        val root = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: return out.also { cache = it; cacheAt = now }
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var seen = 0
        while (queue.isNotEmpty() && seen < MAX_NODES) {
            val n = queue.removeFirst()
            seen++
            try {
                if (n.isVisibleToUser) {
                    val r = Rect()
                    n.getBoundsInScreen(r)
                    if (r.width() > 0 && r.height() > 0) {
                        val text = (n.text ?: n.contentDescription)?.toString()?.trim()?.take(120)?.ifEmpty { null }
                        val id = n.viewIdResourceName?.substringAfter('/')?.take(40)
                        out.add(UiNode(r, if (n.isPassword) null else text, n.isClickable, n.isEditable, n.isPassword, id))
                    }
                    for (i in 0 until n.childCount) n.getChild(i)?.let { queue.add(it) }
                }
            } catch (_: Exception) {
            }
        }
        cache = out
        cacheAt = now
        return out
    }

    /** Верхний край клавиатуры, если она открыта. */
    fun keyboardTop(): Int? = try {
        windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }?.let { w ->
            val r = Rect()
            w.getBoundsInScreen(r)
            r.top.takeIf { r.height() > 0 }
        }
    } catch (_: Exception) {
        null
    }

    /** Текст, который сейчас виден на экране, одной строкой. */
    fun screenText(limit: Int): String {
        val seen = LinkedHashSet<String>()
        for (n in nodes()) {
            val t = n.text ?: continue
            if (n.password || t.length < 2) continue
            seen.add(t.replace(Regex("\\s+"), " "))
        }
        val sb = StringBuilder()
        for (t in seen) {
            if (sb.length + t.length + 3 > limit) break
            if (sb.isNotEmpty()) sb.append(" · ")
            sb.append(t)
        }
        return sb.toString()
    }

    fun visibleTexts(): Set<String> = nodes().mapNotNull { if (it.password) null else it.text }.toSet()

    /** Вставить текст в поле ввода, на котором сейчас фокус. */
    fun insertText(text: String): Boolean = try {
        val f = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (f == null || !f.isEditable) false else {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
    } catch (_: Exception) {
        false
    }

    // ---------- «руки»: действия на телефоне ----------

    fun global(action: Int) = try {
        performGlobalAction(action)
    } catch (_: Exception) {
        false
    }

    /** Элементы для агента: с текстом или нажимаемые. */
    fun elements(limit: Int): List<UiNode> = nodes()
        .filter { (it.text != null || it.clickable || it.editable) && it.rect.width() >= 8 && it.rect.height() >= 8 }
        .distinctBy { "${it.text}|${it.rect}" }
        .take(limit)

    private fun roots(): List<AccessibilityNodeInfo> {
        val out = ArrayList<AccessibilityNodeInfo>()
        try {
            rootInActiveWindow?.let { out.add(it) }
        } catch (_: Exception) {
        }
        try {
            for (w in windows) {
                if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION || w.type == AccessibilityWindowInfo.TYPE_SYSTEM) {
                    w.root?.let { out.add(it) }
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun findAll(limit: Int = 1500, pred: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
        val found = ArrayList<AccessibilityNodeInfo>()
        for (root in roots()) {
            val q = ArrayDeque<AccessibilityNodeInfo>()
            q.add(root)
            var seen = 0
            while (q.isNotEmpty() && seen < limit) {
                val n = q.removeFirst()
                seen++
                try {
                    if (!n.isVisibleToUser) continue
                    if (pred(n)) found.add(n)
                    for (i in 0 until n.childCount) n.getChild(i)?.let { q.add(it) }
                } catch (_: Exception) {
                }
            }
        }
        return found
    }

    private fun label(n: AccessibilityNodeInfo) = (n.text ?: n.contentDescription)?.toString()?.trim()?.lowercase()

    private fun clickNode(n: AccessibilityNodeInfo): Boolean {
        var cur: AccessibilityNodeInfo? = n
        var depth = 0
        while (cur != null && depth < 8) {
            if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            cur = cur.parent
            depth++
        }
        val r = Rect()
        n.getBoundsInScreen(r)
        return tap(r.exactCenterX(), r.exactCenterY())
    }

    /** Нажать на элемент с таким текстом (сначала точное совпадение, потом «содержит»). */
    fun clickText(text: String): Boolean {
        val t = text.trim().trim('«', '»', '"').lowercase()
        if (t.isEmpty()) return false
        val exact = findAll { label(it) == t }
        val node = exact.firstOrNull() ?: findAll { label(it)?.contains(t) == true }.minByOrNull { label(it)?.length ?: 999 }
        return node != null && clickNode(node)
    }

    fun clickRegex(re: Regex): Boolean {
        val node = findAll { n -> label(n)?.let { re.containsMatchIn(it) } == true }.firstOrNull() ?: return false
        return clickNode(node)
    }

    /** Нажать на элемент по его прямоугольнику (агент выбирает по номеру из списка). */
    fun clickRect(r: Rect): Boolean {
        val node = findAll { n ->
            val b = Rect()
            n.getBoundsInScreen(b)
            b == r
        }.lastOrNull()
        return if (node != null) clickNode(node) else tap(r.exactCenterX(), r.exactCenterY())
    }

    /** Где на экране элемент с такой подписью (так находим Clawd из плагина в Telegram). */
    fun findByDesc(desc: String): Rect? {
        val n = findAll(2500) { it.contentDescription?.toString() == desc }.firstOrNull() ?: return null
        val r = Rect()
        n.getBoundsInScreen(r)
        return r.takeIf { it.width() > 0 && it.height() > 0 }
    }

    fun tap(x: Float, y: Float) = gesture(Path().apply { moveTo(x, y) }, 60)

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float) = gesture(
        Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        },
        300,
    )

    private fun gesture(p: Path, dur: Long): Boolean = try {
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, dur)).build(), null, null)
    } catch (_: Exception) {
        false
    }

    /** Прокрутить самый большой прокручиваемый список; если такого нет — свайпнуть. */
    fun scroll(forward: Boolean): Boolean {
        val node = findAll(800) { it.isScrollable }.maxByOrNull {
            val b = Rect()
            it.getBoundsInScreen(b)
            b.width() * b.height()
        }
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        if (node != null && node.performAction(action)) return true
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        return if (forward) swipe(x, dm.heightPixels * 0.75f, x, dm.heightPixels * 0.3f)
        else swipe(x, dm.heightPixels * 0.3f, x, dm.heightPixels * 0.75f)
    }

    /** Снимок экрана (Android 11+), уже в обычном программном Bitmap. */
    fun screenshot(done: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT < 30) {
            done(null)
            return
        }
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY, worker,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        val hb = result.hardwareBuffer
                        val bmp = try {
                            Bitmap.wrapHardwareBuffer(hb, result.colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                        } catch (_: Exception) {
                            null
                        } finally {
                            hb.close()
                        }
                        done(bmp)
                    }

                    override fun onFailure(errorCode: Int) = done(null)
                },
            )
        } catch (_: Exception) {
            done(null)
        }
    }
}
