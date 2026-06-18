package com.aster.service.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.aster.R
import com.aster.data.local.db.ToolCallLogger
import com.aster.data.local.db.ToolEvent
import com.aster.service.safety.KillSwitchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "AI is controlling your screen" overlay for on-device screen-control runs.
 *
 * While a screen-control tool is active this paints a full-screen teal BORDER
 * around the display plus a BOTTOM footer strip carrying the live step text,
 * the "Aster" / "powered by OpenAlly" branding and the single STOP affordance.
 *
 * Two separate windows are used so taps pass THROUGH the border to the target
 * app (the AI must keep driving it): the border is [FLAG_NOT_TOUCHABLE] and
 * the footer is touchable only so its STOP button works. Both windows are
 * added/updated/removed on the main thread, gated by [Settings.canDrawOverlays];
 * when draw-overlays is not granted the persistent STOP notification owned by
 * [KillSwitchController] is the fallback (kept intact).
 */
@Singleton
class ToolExecutionOverlay @Inject constructor(
    private val toolCallLogger: ToolCallLogger,
    private val killSwitchController: KillSwitchController
) {
    companion object {
        private const val TAG = "ToolExecOverlay"

        // Colors
        private const val ACCENT = 0xFF_2D_D4_BF.toInt()        // Aster teal
        private const val FOOTER_BG = 0xCC_0B_0F_14.toInt()     // dark semi-transparent
        private const val TEXT_COLOR = 0xFF_EC_F0_F6.toInt()    // light text
        private const val SUBTLE_TEXT = 0x99_FF_FF_FF.toInt()   // 60% white
        private const val STOP_RED = 0xFF_EF_44_44.toInt()      // STOP button

        // Border stroke pulse bounds (alpha applied to ACCENT).
        private const val PULSE_MIN_ALPHA = 0x66
        private const val PULSE_MAX_ALPHA = 0xFF
        private const val PULSE_DURATION_MS = 1100L

        /** Border stroke width (dp). Deliberately bold so "the AI is driving" reads
         *  at a glance over a busy app, not as a hairline the owner can miss. */
        private const val STROKE_DP = 6

        /**
         * Auto-dismiss the overlay after this much device inactivity. There is no
         * companion-side "run finished" signal (the companion only sees discrete
         * `device.execute` actions), so the overlay is torn down once no tool
         * event has fired for this long. Kept generous so the model's "think" gap
         * between batches doesn't flicker the border off mid-run.
         */
        private const val IDLE_DISMISS_MS = 15_000L

        /** Screen-control actions that drive the kill-switch + STOP affordance. */
        private val SCREEN_CONTROL_ACTIONS = setOf(
            "tap", "set_text", "long_press", "set_toggle", "perform", "scroll",
            "input_gesture", "press_key", "global_action", "input_text",
            "click_by_text", "click_by_view_id", "launch_intent"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { removeOverlay() }
    private var windowManager: WindowManager? = null
    private var context: Context? = null
    private var collectJob: Job? = null

    // Border window (full-screen, NOT_TOUCHABLE so taps pass through).
    private var borderView: FrameLayout? = null
    private var borderDrawable: GradientDrawable? = null
    private var borderAttached = false
    private var pulseAnimator: ValueAnimator? = null

    // Footer window (touchable — hosts the live step text + STOP). [footerView]
    // is a transparent full-width wrapper; the rounded bar lives inside it so we
    // can inset it from the screen edges.
    private var footerView: FrameLayout? = null
    private var stepView: TextView? = null
    private var footerAttached = false

    fun attach(context: Context, scope: CoroutineScope) {
        this.context = context
        this.windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        collectJob = toolCallLogger.toolEvents
            .onEach { event -> mainHandler.post { handleEvent(event) } }
            .launchIn(scope)

        Log.d(TAG, "Attached")
    }

    fun detach() {
        collectJob?.cancel()
        collectJob = null
        mainHandler.post { removeOverlay() }
        windowManager = null
        context = null
        Log.d(TAG, "Detached")
    }

    /**
     * Screen Control /goal P7 — immediately tear down the active border + footer
     * overlay WITHOUT cancelling the event collector (unlike [detach]). Invoked
     * by the kill switch on STOP so the activity overlay clears right away; the
     * collector keeps running so a later run still surfaces normally.
     */
    fun clearActive() {
        mainHandler.post { removeOverlay() }
    }

    private fun handleEvent(event: ToolEvent) {
        // Any device activity keeps the overlay alive; the run is treated as
        // finished only after IDLE_DISMISS_MS of silence (no "run ended" signal
        // exists companion-side). Observe/wait events aren't screen-control but
        // still count as "the run is active", so they reset the timer too.
        rearmDismissIfShown()
        when (event) {
            is ToolEvent.Started -> {
                if (!isScreenControl(event.toolName)) return
                show(friendly(event.toolName, event.target))
                // Persistent STOP notification — the reliable fallback when
                // draw-overlays is not granted (P7).
                killSwitchController.showControlActive(event.target)
            }
            is ToolEvent.Completed -> {
                // Keep the overlay up between steps; it's torn down on STOP
                // (clearActive), detach, or the idle timeout. While it's up, sync
                // the footer text to whatever just ran (incl. observe/wait).
                if (footerAttached) {
                    stepView?.text = friendly(event.toolName, event.target)
                }
            }
        }
    }

    /** Reset the idle auto-dismiss timer when the overlay is currently shown. */
    private fun rearmDismissIfShown() {
        if (!borderAttached && !footerAttached) return
        mainHandler.removeCallbacks(dismissRunnable)
        mainHandler.postDelayed(dismissRunnable, IDLE_DISMISS_MS)
    }

    private fun isScreenControl(action: String) = action in SCREEN_CONTROL_ACTIONS

    /** Map a raw toolName (+ optional target) to a human-friendly verbose step. */
    private fun friendly(action: String, target: String?): String = when (action) {
        "tap", "long_press", "click_by_text", "click_by_view_id", "perform" ->
            "Tapping ${target ?: "element"}"
        "set_text", "input_text" -> "Typing…"
        "launch_intent" -> "Opening ${target ?: "app"}"
        "observe", "screenshot", "wait_for", "wait_for_idle", "wait_for_text" ->
            "Looking at the screen…"
        "scroll" -> "Scrolling…"
        "global_action" -> "Navigating…"
        "set_toggle" -> "Toggling ${target ?: "setting"}"
        "press_key", "input_gesture" -> "Navigating…"
        else -> titleCase(action)
    }

    private fun titleCase(action: String): String =
        action.split('_').joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    private fun show(step: String) {
        val ctx = context ?: return
        if (!Settings.canDrawOverlays(ctx)) {
            // No overlay permission — the persistent STOP notification
            // (KillSwitchController) is the reachable fallback.
            Log.w(TAG, "Overlay permission not granted, relying on STOP notification")
            return
        }

        ensureBorder(ctx)
        if (!borderAttached) {
            try {
                windowManager?.addView(borderView, borderLayoutParams())
                borderAttached = true
                startPulse()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add border overlay view", e)
            }
        }

        ensureFooter(ctx)
        stepView?.text = step
        if (!footerAttached) {
            try {
                windowManager?.addView(footerView, footerLayoutParams())
                footerAttached = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add footer overlay view", e)
            }
        }

        // Arm / re-arm the idle auto-dismiss now that the overlay is up.
        mainHandler.removeCallbacks(dismissRunnable)
        mainHandler.postDelayed(dismissRunnable, IDLE_DISMISS_MS)
    }

    // ---- Border ------------------------------------------------------------

    private fun ensureBorder(ctx: Context) {
        if (borderView != null) return
        val density = ctx.resources.displayMetrics.density
        val stroke = (STROKE_DP * density).toInt()
        val drawable = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = 24 * density
            setStroke(stroke, ACCENT)
        }
        borderDrawable = drawable
        borderView = FrameLayout(ctx).apply {
            background = drawable
        }
    }

    private fun borderLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            // NOT_TOUCHABLE so every tap passes through to the target app.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    /** Simple main-thread pulse on the border stroke alpha. */
    private fun startPulse() {
        if (pulseAnimator != null) return
        pulseAnimator = ValueAnimator.ofInt(PULSE_MAX_ALPHA, PULSE_MIN_ALPHA).apply {
            duration = PULSE_DURATION_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { anim ->
                val alpha = anim.animatedValue as Int
                val color = (alpha shl 24) or (ACCENT and 0x00FFFFFF)
                val density = context?.resources?.displayMetrics?.density ?: 3f
                borderDrawable?.setStroke((STROKE_DP * density).toInt(), color)
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    // ---- Footer ------------------------------------------------------------

    private fun ensureFooter(ctx: Context) {
        if (footerView != null) return
        val density = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val font = try {
            ResourcesCompat.getFont(ctx, R.font.instrument_sans_medium)
        } catch (_: Exception) { null }
        val fontBold = try {
            ResourcesCompat.getFont(ctx, R.font.instrument_sans_bold)
        } catch (_: Exception) { null }

        // Leading dot/glyph in ACCENT.
        val dot = View(ctx).apply {
            val size = dp(8)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(10)
                gravity = Gravity.CENTER_VERTICAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ACCENT)
            }
        }

        // Live verbose step text (stretches).
        stepView = TextView(ctx).apply {
            text = "Aster is working…"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(TEXT_COLOR)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            includeFontPadding = false
            font?.let { typeface = it }
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dp(10)
            }
        }

        // Trailing brand block: "Aster" over "powered by OpenAlly".
        val nameView = TextView(ctx).apply {
            text = "Aster"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(TEXT_COLOR)
            maxLines = 1
            includeFontPadding = false
            fontBold?.let { typeface = it }
        }
        val poweredView = TextView(ctx).apply {
            text = "powered by OpenAlly"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTextColor(SUBTLE_TEXT)
            maxLines = 1
            includeFontPadding = false
            font?.let { typeface = it }
        }
        val brandBlock = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dp(12)
            }
            addView(nameView)
            addView(poweredView)
        }

        // The single STOP affordance.
        val stop = TextView(ctx).apply {
            text = "STOP"
            setTextColor(0xFF_FF_FF_FF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            fontBold?.let { typeface = it }
            val h = dp(16); val v = dp(8)
            setPadding(h, v, h, v)
            includeFontPadding = false
            background = GradientDrawable().apply {
                cornerRadius = 18 * density
                setColor(STOP_RED)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            setOnClickListener { killSwitchController.stop() }
        }

        // Rounded dark bar holding the content.
        val hPad = dp(16); val vPad = dp(12)
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(hPad, vPad, hPad, vPad)
            background = GradientDrawable().apply {
                cornerRadius = 22 * density
                setColor(FOOTER_BG)
            }
            addView(dot)
            addView(stepView)
            addView(brandBlock)
            addView(stop)
        }

        // Transparent full-width wrapper (the window root) — insets the bar from
        // the screen edges so it reads as a floating strip.
        footerView = FrameLayout(ctx).apply {
            val side = dp(12)
            setPadding(side, 0, side, dp(24))
            addView(
                bar,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun footerLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            // Touchable (NO FLAG_NOT_TOUCHABLE) so the STOP button receives taps.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START or Gravity.END
        }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    // ---- Teardown ----------------------------------------------------------

    private fun removeOverlay() {
        mainHandler.removeCallbacks(dismissRunnable)
        stopPulse()
        if (borderAttached) {
            try {
                windowManager?.removeView(borderView)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove border overlay view", e)
            }
            borderAttached = false
        }
        if (footerAttached) {
            try {
                windowManager?.removeView(footerView)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove footer overlay view", e)
            }
            footerAttached = false
        }
        borderView = null
        borderDrawable = null
        footerView = null
        stepView = null
        // Control session ended — drop the persistent STOP notification (P7).
        killSwitchController.hide()
    }
}
