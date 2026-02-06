package com.aster.service.handlers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.util.Log
import com.aster.BuildConfig
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Handler for system overlay commands.
 * Displays floating WebView overlays on top of other apps.
 */
class OverlayHandler(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "OverlayHandler"
        private val overlayIdCounter = AtomicInteger(1)
        private val activeOverlays = ConcurrentHashMap<Int, OverlayInstance>()
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private data class OverlayInstance(
        val id: Int,
        val container: FrameLayout,
        val webView: WebView,
        val timeoutRunnable: Runnable? = null
    )

    override fun supportedActions() = listOf(
        "show_overlay",
        "hide_overlay",
        "hide_all_overlays",
        "list_overlays"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "show_overlay" -> showOverlay(command)
            "hide_overlay" -> hideOverlay(command)
            "hide_all_overlays" -> hideAllOverlays()
            "list_overlays" -> listOverlays()
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun showOverlay(command: Command): CommandResult {
        // Check overlay permission
        if (!Settings.canDrawOverlays(context)) {
            return CommandResult.failure("Overlay permission not granted. Please enable 'Display over other apps' in Settings.")
        }

        val url = command.params?.get("url")?.jsonPrimitive?.contentOrNull
        val html = command.params?.get("html")?.jsonPrimitive?.contentOrNull

        if (url == null && html == null) {
            return CommandResult.failure("Either 'url' or 'html' parameter is required")
        }

        // Parse optional parameters
        val width = command.params?.get("width")?.jsonPrimitive?.intOrNull
            ?: WindowManager.LayoutParams.MATCH_PARENT
        val height = command.params?.get("height")?.jsonPrimitive?.intOrNull ?: 400
        val x = command.params?.get("x")?.jsonPrimitive?.intOrNull ?: 0
        val y = command.params?.get("y")?.jsonPrimitive?.intOrNull ?: 100
        val gravity = parseGravity(command.params?.get("gravity")?.jsonPrimitive?.contentOrNull)
        val draggable = command.params?.get("draggable")?.jsonPrimitive?.booleanOrNull ?: true
        val transparent = command.params?.get("transparent")?.jsonPrimitive?.booleanOrNull ?: false
        val showCloseButton = command.params?.get("showCloseButton")?.jsonPrimitive?.booleanOrNull ?: true
        val timeout = command.params?.get("timeout")?.jsonPrimitive?.intOrNull

        val overlayId = overlayIdCounter.getAndIncrement()

        try {
            mainHandler.post {
                createOverlay(overlayId, url, html, width, height, x, y, gravity, draggable, transparent, showCloseButton, timeout)
            }

            return CommandResult.success(buildJsonObject {
                put("overlayId", overlayId)
                put("width", width)
                put("height", height)
                put("created", true)
            })
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to create overlay", e)
            return CommandResult.failure("Failed to create overlay: ${e.message}")
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun createOverlay(
        overlayId: Int,
        url: String?,
        html: String?,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        gravity: Int,
        draggable: Boolean,
        transparent: Boolean,
        showCloseButton: Boolean,
        timeout: Int?
    ) {
        val container = FrameLayout(context)
        val webView = WebView(context)

        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        webView.webViewClient = WebViewClient()

        if (transparent) {
            webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        } else {
            container.setBackgroundColor(android.graphics.Color.WHITE)
        }

        // Add WebView to container
        container.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Add close button if requested
        if (showCloseButton) {
            val closeButton = android.widget.TextView(context).apply {
                text = "\u2715"
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#99000000"))
                setPadding(24, 8, 24, 8)
                setOnClickListener { removeOverlay(overlayId) }
            }
            val closeParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            )
            container.addView(closeButton, closeParams)
        }

        // Window layout params
        val layoutParams = WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    (if (transparent) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0),
            if (transparent) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE
        ).apply {
            this.gravity = gravity
            this.x = x
            this.y = y
        }

        // Make draggable if requested
        if (draggable) {
            setupDragListener(container, layoutParams)
        }

        // Add to window
        windowManager.addView(container, layoutParams)

        // Auto-timeout
        val timeoutRunnable = if (timeout != null && timeout > 0) {
            Runnable { removeOverlay(overlayId) }.also {
                mainHandler.postDelayed(it, timeout * 1000L)
            }
        } else null

        // Store instance
        activeOverlays[overlayId] = OverlayInstance(overlayId, container, webView, timeoutRunnable)

        // Load content
        if (url != null) {
            webView.loadUrl(url)
        } else if (html != null) {
            webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "UTF-8",
                null
            )
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Overlay created: $overlayId")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun removeOverlay(overlayId: Int) {
        val instance = activeOverlays.remove(overlayId) ?: return
        mainHandler.post {
            try {
                instance.timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                instance.webView.destroy()
                windowManager.removeView(instance.container)
                if (BuildConfig.DEBUG) Log.d(TAG, "Overlay removed: $overlayId")
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Error removing overlay $overlayId", e)
            }
        }
    }

    private fun hideOverlay(command: Command): CommandResult {
        val overlayId = command.params?.get("overlayId")?.jsonPrimitive?.intOrNull
            ?: return CommandResult.failure("Missing 'overlayId' parameter")

        val instance = activeOverlays.remove(overlayId)
            ?: return CommandResult.failure("Overlay not found: $overlayId")

        mainHandler.post {
            try {
                instance.timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                instance.webView.destroy()
                windowManager.removeView(instance.container)
                if (BuildConfig.DEBUG) Log.d(TAG, "Overlay hidden: $overlayId")
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Error removing overlay", e)
            }
        }

        return CommandResult.success(buildJsonObject {
            put("overlayId", overlayId)
            put("hidden", true)
        })
    }

    private fun hideAllOverlays(): CommandResult {
        val ids = activeOverlays.keys.toList()
        var hiddenCount = 0

        mainHandler.post {
            ids.forEach { id ->
                activeOverlays.remove(id)?.let { instance ->
                    try {
                        instance.timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                        instance.webView.destroy()
                        windowManager.removeView(instance.container)
                        hiddenCount++
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Error removing overlay $id", e)
                    }
                }
            }
        }

        return CommandResult.success(buildJsonObject {
            put("hiddenCount", ids.size)
            put("hiddenIds", buildJsonArray { ids.forEach { add(it) } })
        })
    }

    private fun listOverlays(): CommandResult {
        val overlayList = buildJsonArray {
            activeOverlays.values.forEach { instance ->
                add(buildJsonObject {
                    put("id", instance.id)
                })
            }
        }

        return CommandResult.success(buildJsonObject {
            put("overlays", overlayList)
            put("count", activeOverlays.size)
        })
    }

    private fun parseGravity(gravityStr: String?): Int {
        return when (gravityStr?.lowercase()) {
            "top" -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            "bottom" -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            "left", "start" -> Gravity.START or Gravity.CENTER_VERTICAL
            "right", "end" -> Gravity.END or Gravity.CENTER_VERTICAL
            "center" -> Gravity.CENTER
            "top_left", "top_start" -> Gravity.TOP or Gravity.START
            "top_right", "top_end" -> Gravity.TOP or Gravity.END
            "bottom_left", "bottom_start" -> Gravity.BOTTOM or Gravity.START
            "bottom_right", "bottom_end" -> Gravity.BOTTOM or Gravity.END
            else -> Gravity.TOP or Gravity.START
        }
    }

    /**
     * Cleanup all overlays when handler is destroyed.
     */
    fun cleanup() {
        mainHandler.post {
            activeOverlays.values.forEach { instance ->
                try {
                    instance.timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                    instance.webView.destroy()
                    windowManager.removeView(instance.container)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Error cleaning up overlay", e)
                }
            }
            activeOverlays.clear()
        }
    }
}
