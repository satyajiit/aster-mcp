package com.aster.service.accessibility

import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

/**
 * Privacy boundary for Companion System Pulse accessibility reactions.
 *
 * The classifier accepts only framework event metadata and emits a tiny,
 * allow-listed semantic payload. It has no field for text, content description,
 * notification content, or a node reference, which makes accidental content
 * capture impossible at this seam. Repeated hot-path events are rate-limited.
 */
internal class AccessibilityPulseClassifier {
    data class Input(
        val eventType: Int,
        val packageName: String?,
        val scrollDeltaX: Int = 0,
        val scrollDeltaY: Int = 0,
        val fromIndex: Int = -1,
        val toIndex: Int = -1,
    )

    data class Pulse(val kind: String, val values: Map<String, Any>)

    companion object {
        private const val INTERACTION_COOLDOWN_MS = 350L
        private const val SCROLL_COOLDOWN_MS = 700L
        private const val TYPING_COOLDOWN_MS = 1_500L
        private const val MAX_PACKAGE_LENGTH = 180
    }

    private var foregroundPackage: String? = null
    private var lastInteractionAt = Long.MIN_VALUE
    private var lastScrollAt = Long.MIN_VALUE
    private var lastTypingAt = Long.MIN_VALUE

    fun reset() {
        foregroundPackage = null
        lastInteractionAt = Long.MIN_VALUE
        lastScrollAt = Long.MIN_VALUE
        lastTypingAt = Long.MIN_VALUE
    }

    fun classify(input: Input, nowMs: Long): List<Pulse> {
        val out = ArrayList<Pulse>(2)
        val packageName = sanitizePackage(input.packageName)

        if (input.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && packageName != null) {
            if (packageName != foregroundPackage) {
                val initial = foregroundPackage == null
                foregroundPackage = packageName
                out += Pulse(
                    "foregroundApp",
                    mapOf("packageName" to packageName, "initial" to initial),
                )
            }
        }

        when (input.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                if (elapsed(nowMs, lastInteractionAt) >= INTERACTION_COOLDOWN_MS) {
                    lastInteractionAt = nowMs
                    out += Pulse(
                        "interaction",
                        mapOf(
                            "action" to if (
                                input.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
                            ) "longPress" else "tap",
                        ),
                    )
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (elapsed(nowMs, lastScrollAt) >= SCROLL_COOLDOWN_MS) {
                    direction(input)?.let { direction ->
                        lastScrollAt = nowMs
                        out += Pulse("scroll", mapOf("direction" to direction))
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                if (elapsed(nowMs, lastTypingAt) >= TYPING_COOLDOWN_MS) {
                    lastTypingAt = nowMs
                    out += Pulse("typing", mapOf("active" to true))
                }
            }
        }
        return out
    }

    private fun sanitizePackage(value: String?): String? = value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.take(MAX_PACKAGE_LENGTH)

    private fun direction(input: Input): String? {
        val dx = input.scrollDeltaX
        val dy = input.scrollDeltaY
        if (dx != 0 || dy != 0) {
            return if (abs(dy) >= abs(dx)) {
                if (dy > 0) "down" else "up"
            } else {
                if (dx > 0) "right" else "left"
            }
        }
        if (input.fromIndex >= 0 && input.toIndex >= 0 && input.fromIndex != input.toIndex) {
            return if (input.toIndex > input.fromIndex) "down" else "up"
        }
        return null
    }

    private fun elapsed(nowMs: Long, previousMs: Long): Long =
        if (previousMs == Long.MIN_VALUE) Long.MAX_VALUE else nowMs - previousMs
}
