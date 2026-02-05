package com.aster.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aster.BuildConfig
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import android.util.Base64
import kotlin.coroutines.resume

/**
 * Accessibility Service for screen control and automation.
 *
 * Provides:
 * - Screen hierarchy capture (AccessibilityNodeInfo tree)
 * - Gesture dispatch (tap, swipe)
 * - Global actions (BACK, HOME, RECENTS, etc.)
 * - Text input into focused fields
 * - Screenshot capture (Android 11+)
 */
class AsterAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AsterAccessibility"

        // Singleton instance for access from handlers
        @Volatile
        private var instance: AsterAccessibilityService? = null

        fun getInstance(): AsterAccessibilityService? = instance

        fun isServiceEnabled(): Boolean = instance != null

        // Global action constants
        const val ACTION_BACK = "BACK"
        const val ACTION_HOME = "HOME"
        const val ACTION_RECENTS = "RECENTS"
        const val ACTION_NOTIFICATIONS = "NOTIFICATIONS"
        const val ACTION_QUICK_SETTINGS = "QUICK_SETTINGS"
        const val ACTION_POWER_DIALOG = "POWER_DIALOG"
        const val ACTION_LOCK_SCREEN = "LOCK_SCREEN"

        // Gesture types
        const val GESTURE_TAP = "TAP"
        const val GESTURE_SWIPE = "SWIPE"
        const val GESTURE_LONG_PRESS = "LONG_PRESS"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Guard against multiple onServiceConnected calls
        if (instance != null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Accessibility service already connected, ignoring duplicate")
            return
        }
        instance = this
        if (BuildConfig.DEBUG) Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We handle events on demand, not reactively for this use case
    }

    override fun onInterrupt() {
        if (BuildConfig.DEBUG) Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        if (BuildConfig.DEBUG) Log.d(TAG, "Accessibility service destroyed")
        super.onDestroy()
    }

    /**
     * Get the complete screen hierarchy as a JSON structure.
     */
    fun getScreenHierarchy(): JsonElement {
        val rootNode = rootInActiveWindow
        return if (rootNode != null) {
            try {
                nodeToJson(rootNode, 0)
            } finally {
                rootNode.recycle()
            }
        } else {
            buildJsonObject {
                put("error", "No active window")
            }
        }
    }

    /**
     * Convert an AccessibilityNodeInfo to JSON recursively.
     */
    private fun nodeToJson(node: AccessibilityNodeInfo, depth: Int): JsonObject {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)

        return buildJsonObject {
            // Basic info
            put("className", node.className?.toString() ?: "")
            put("packageName", node.packageName?.toString() ?: "")
            put("text", node.text?.toString() ?: "")
            put("contentDescription", node.contentDescription?.toString() ?: "")
            put("viewIdResourceName", node.viewIdResourceName ?: "")

            // State
            put("isClickable", node.isClickable)
            put("isLongClickable", node.isLongClickable)
            put("isScrollable", node.isScrollable)
            put("isCheckable", node.isCheckable)
            put("isChecked", node.isChecked)
            put("isEnabled", node.isEnabled)
            put("isFocusable", node.isFocusable)
            put("isFocused", node.isFocused)
            put("isSelected", node.isSelected)
            put("isEditable", node.isEditable)
            put("isPassword", node.isPassword)
            put("isVisibleToUser", node.isVisibleToUser)

            // Bounds
            putJsonObject("bounds") {
                put("left", bounds.left)
                put("top", bounds.top)
                put("right", bounds.right)
                put("bottom", bounds.bottom)
                put("centerX", bounds.centerX())
                put("centerY", bounds.centerY())
                put("width", bounds.width())
                put("height", bounds.height())
            }

            // Depth for hierarchy understanding
            put("depth", depth)

            // Actions available
            putJsonArray("actions") {
                node.actionList.forEach { action ->
                    add(buildJsonObject {
                        put("id", action.id)
                        put("label", action.label?.toString() ?: "")
                    })
                }
            }

            // Children
            val childCount = node.childCount
            if (childCount > 0) {
                putJsonArray("children") {
                    for (i in 0 until childCount) {
                        val child = node.getChild(i)
                        if (child != null) {
                            try {
                                add(nodeToJson(child, depth + 1))
                            } finally {
                                child.recycle()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Perform a global action.
     */
    fun performGlobalActionByName(actionName: String): Boolean {
        val action = when (actionName.uppercase()) {
            ACTION_BACK -> GLOBAL_ACTION_BACK
            ACTION_HOME -> GLOBAL_ACTION_HOME
            ACTION_RECENTS -> GLOBAL_ACTION_RECENTS
            ACTION_NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
            ACTION_QUICK_SETTINGS -> GLOBAL_ACTION_QUICK_SETTINGS
            ACTION_POWER_DIALOG -> GLOBAL_ACTION_POWER_DIALOG
            ACTION_LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    GLOBAL_ACTION_LOCK_SCREEN
                } else {
                    if (BuildConfig.DEBUG) Log.w(TAG, "Lock screen action requires Android P+")
                    return false
                }
            }
            else -> {
                if (BuildConfig.DEBUG) Log.w(TAG, "Unknown global action: $actionName")
                return false
            }
        }

        return performGlobalAction(action)
    }

    /**
     * Perform a tap gesture at coordinates.
     */
    suspend fun performTap(x: Float, y: Float, duration: Long = 100L): Boolean {
        return performGesture(createTapGesture(x, y, duration))
    }

    /**
     * Perform a long press gesture at coordinates.
     */
    suspend fun performLongPress(x: Float, y: Float, duration: Long = 1000L): Boolean {
        return performGesture(createTapGesture(x, y, duration))
    }

    /**
     * Perform a swipe gesture.
     */
    suspend fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = 300L
    ): Boolean {
        return performGesture(createSwipeGesture(startX, startY, endX, endY, duration))
    }

    /**
     * Perform a complex gesture with multiple points.
     */
    suspend fun performGestureWithPoints(
        points: List<Pair<Float, Float>>,
        duration: Long = 300L
    ): Boolean {
        if (points.size < 2) {
            return performTap(points[0].first, points[0].second)
        }

        val path = Path()
        path.moveTo(points[0].first, points[0].second)

        for (i in 1 until points.size) {
            path.lineTo(points[i].first, points[i].second)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        return performGesture(gesture)
    }

    private fun createTapGesture(x: Float, y: Float, duration: Long): GestureDescription {
        val path = Path()
        path.moveTo(x, y)

        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        return GestureDescription.Builder()
            .addStroke(stroke)
            .build()
    }

    private fun createSwipeGesture(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long
    ): GestureDescription {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        return GestureDescription.Builder()
            .addStroke(stroke)
            .build()
    }

    private suspend fun performGesture(gesture: GestureDescription): Boolean =
        suspendCancellableCoroutine { continuation ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            val dispatched = dispatchGesture(gesture, callback, mainHandler)
            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        }

    /**
     * Input text into the currently focused editable field.
     */
    fun inputText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        try {
            val focusedNode = findFocusedEditableNode(rootNode)
            if (focusedNode != null) {
                try {
                    // First try to set text directly
                    val arguments = android.os.Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                    return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                } finally {
                    focusedNode.recycle()
                }
            }
        } finally {
            rootNode.recycle()
        }

        return false
    }

    /**
     * Append text to the currently focused editable field.
     */
    fun appendText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        try {
            val focusedNode = findFocusedEditableNode(rootNode)
            if (focusedNode != null) {
                try {
                    val currentText = focusedNode.text?.toString() ?: ""
                    val newText = currentText + text
                    val arguments = android.os.Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        newText
                    )
                    return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                } finally {
                    focusedNode.recycle()
                }
            }
        } finally {
            rootNode.recycle()
        }

        return false
    }

    /**
     * Clear text in the currently focused editable field.
     */
    fun clearText(): Boolean {
        return inputText("")
    }

    private fun findFocusedEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // First try to find the input-focused node
        val focusedNode = node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && focusedNode.isEditable) {
            return focusedNode
        }
        focusedNode?.recycle()

        // Fallback: search for any focused editable node
        return findEditableNodeRecursive(node)
    }

    private fun findEditableNodeRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                val result = findEditableNodeRecursive(child)
                if (result != null) {
                    return result
                }
            } finally {
                child.recycle()
            }
        }

        return null
    }

    /**
     * Click on a node by its viewId.
     */
    fun clickByViewId(viewId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        try {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (nodes.isNotEmpty()) {
                val node = nodes[0]
                val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                nodes.forEach { it.recycle() }
                return result
            }
        } finally {
            rootNode.recycle()
        }

        return false
    }

    /**
     * Click on a node by its text content.
     */
    fun clickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        try {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            if (nodes.isNotEmpty()) {
                // Find a clickable node
                for (node in nodes) {
                    if (node.isClickable) {
                        val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        nodes.forEach { it.recycle() }
                        return result
                    }
                    // Try parent if node itself is not clickable
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            parent.recycle()
                            nodes.forEach { it.recycle() }
                            return result
                        }
                        val grandParent = parent.parent
                        parent.recycle()
                        parent = grandParent
                    }
                }
                nodes.forEach { it.recycle() }
            }
        } finally {
            rootNode.recycle()
        }

        return false
    }

    /**
     * Take a screenshot (Android 11+).
     * Returns base64 encoded PNG or null if not supported/failed.
     */
    suspend fun takeScreenshot(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Screenshot via Accessibility requires Android 11+")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val callback = object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    try {
                        val bitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace
                        )
                        if (bitmap != null) {
                            val outputStream = ByteArrayOutputStream()
                            // Create a software copy for compression
                            val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                            softwareBitmap.recycle()
                            bitmap.recycle()
                            screenshot.hardwareBuffer.close()

                            if (continuation.isActive) {
                                continuation.resume(base64)
                            }
                        } else {
                            screenshot.hardwareBuffer.close()
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Screenshot processing failed", e)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Screenshot failed with error code: $errorCode")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                mainExecutor,
                callback
            )
        }
    }

    /**
     * Scroll in a direction on the screen.
     */
    fun scroll(direction: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        try {
            val scrollableNode = findScrollableNode(rootNode)
            if (scrollableNode != null) {
                try {
                    val action = when (direction.uppercase()) {
                        "UP", "BACKWARD" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                        "DOWN", "FORWARD" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                        else -> return false
                    }
                    return scrollableNode.performAction(action)
                } finally {
                    scrollableNode.recycle()
                }
            }
        } finally {
            rootNode.recycle()
        }

        return false
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                val result = findScrollableNode(child)
                if (result != null) {
                    return result
                }
            } finally {
                child.recycle()
            }
        }

        return null
    }
}
