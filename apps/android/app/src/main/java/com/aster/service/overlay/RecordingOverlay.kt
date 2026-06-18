package com.aster.service.overlay

import android.animation.ValueAnimator
import android.content.Context
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

/**
 * On-device "you are recording" overlay for the companion live recorder (the
 * manual record flow). While recording, the owner is inside the TARGET app — not
 * the OpenAlly app — so a floating strip is the only place to show progress and a
 * reachable **Finish**. It shows a pulsing REC dot, the live captured-step count,
 * and a Finish button.
 *
 * Finish does NOT stop recording locally — it invokes [onFinish] (the service
 * marks the buffer finish-requested), and the RN record screen's status poll
 * turns that into the real kernel-driven `automation:record-stop` (the single
 * drain path). So the overlay's Finish and the in-app Finish converge.
 *
 * Mirrors [ToolExecutionOverlay]'s window handling: `TYPE_APPLICATION_OVERLAY`,
 * gated on [Settings.canDrawOverlays], every window op posted to the main thread
 * (record-start/stop run on a Binder/IO thread; capture runs on the a11y thread).
 */
class RecordingOverlay {
    companion object {
        private const val TAG = "RecordingOverlay"
        private const val ACCENT = 0xFF_2D_D4_BF.toInt()    // Aster teal (Finish)
        private const val REC_RED = 0xFF_EF_44_44.toInt()   // recording dot
        private const val BAR_BG = 0xCC_0B_0F_14.toInt()    // dark strip
        private const val TEXT_COLOR = 0xFF_EC_F0_F6.toInt()
        private const val INK = 0xFF_0B_0F_14.toInt()       // Finish label (on teal)
        private const val PULSE_MS = 900L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var root: FrameLayout? = null
    private var countView: TextView? = null
    private var finishView: TextView? = null
    private var attached = false
    private var onFinish: (() -> Unit)? = null
    private var pulse: ValueAnimator? = null

    /** Show the strip (idempotent). Safe to call from any thread. */
    fun show(ctx: Context, onFinish: () -> Unit) {
        val app = ctx.applicationContext
        this.onFinish = onFinish
        mainHandler.post { addView(app) }
    }

    /** Update the live captured-step count. Safe from any thread. */
    fun setCount(n: Int) {
        mainHandler.post { countView?.text = countLabel(n) }
    }

    /** Tear the strip down. Safe from any thread. */
    fun hide() {
        mainHandler.post { removeView() }
    }

    private fun countLabel(n: Int): String =
        if (n == 1) "Recording · 1 step" else "Recording · $n steps"

    private fun addView(ctx: Context) {
        if (attached) return
        if (!Settings.canDrawOverlays(ctx)) {
            Log.w(TAG, "Overlay permission not granted — recording has no on-screen indicator")
            return
        }
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val density = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val dot = View(ctx).apply {
            val s = dp(10)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                marginEnd = dp(10)
                gravity = Gravity.CENTER_VERTICAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(REC_RED)
            }
        }

        val count = TextView(ctx).apply {
            text = countLabel(0)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(TEXT_COLOR)
            maxLines = 1
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dp(10)
            }
        }
        countView = count

        val finish = TextView(ctx).apply {
            text = "FINISH"
            setTextColor(INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            val h = dp(16); val v = dp(8)
            setPadding(h, v, h, v)
            includeFontPadding = false
            background = GradientDrawable().apply {
                cornerRadius = 18 * density
                setColor(ACCENT)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER_VERTICAL }
            setOnClickListener {
                text = "FINISHING…"
                isClickable = false
                onFinish?.invoke()
            }
        }
        finishView = finish

        val hPad = dp(16); val vPad = dp(12)
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(hPad, vPad, hPad, vPad)
            background = GradientDrawable().apply {
                cornerRadius = 22 * density
                setColor(BAR_BG)
            }
            addView(dot)
            addView(count)
            addView(finish)
        }

        val wrapper = FrameLayout(ctx).apply {
            val side = dp(12)
            setPadding(side, 0, side, dp(24))
            addView(
                bar,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        root = wrapper

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            // Touchable (no FLAG_NOT_TOUCHABLE) so Finish receives taps.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM or Gravity.START or Gravity.END }

        try {
            wm.addView(wrapper, lp)
            attached = true
            startPulse(dot)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add recording overlay", e)
        }
    }

    private fun startPulse(view: View) {
        pulse?.cancel()
        pulse = ValueAnimator.ofFloat(1f, 0.3f).apply {
            duration = PULSE_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { view.alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun removeView() {
        pulse?.cancel()
        pulse = null
        if (attached) {
            try {
                windowManager?.removeView(root)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove recording overlay", e)
            }
            attached = false
        }
        root = null
        countView = null
        finishView = null
        onFinish = null
    }
}
