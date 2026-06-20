package com.aster.service.overlay

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
 * Non-blocking "the run is waiting for you" banner shown ON the controlled app —
 * for two cases the automation engine drives it: a login / register wall
 * (`screen_signin_wait`) and a clean step-back hand-off such as payment
 * (`screen_handoff`, [show] with `kind = "payment"` / `"explicit_handoff"`). In
 * both the assistant has stepped back and the owner finishes IN PLACE, so the
 * banner stays on the controlled app and never yanks OpenAlly forward.
 *
 * Two layouts, by kind:
 *  - SIGN-IN wall: a non-focusable, non-touchable strip at the BOTTOM — the owner
 *    types into the login form behind it; it auto-clears as the run resumes.
 *  - HAND-OFF (payment / explicit): a touchable, dismissable card at the TOP, so
 *    it never covers the bottom pay/confirm button, with a "Got it" to dismiss.
 *
 * Unlike [InteractiveOverlayController] (a focusable scrim that BLOCKS the app),
 * neither layout steals focus. Unlike [ToolExecutionOverlay] there is no border /
 * STOP (the run is waiting, not driving) — and the handler tears that overlay down
 * before showing this, so they never stack. It self-dismisses after an idle window
 * with no refresh: a sign-in wall is re-fired every poll (re-arming the short
 * [IDLE_DISMISS_MS]); a hand-off is fired once, so it lingers for
 * [HANDOFF_DISMISS_MS] (or until "Got it").
 *
 * All view work is marshalled to the main thread and gated on
 * [Settings.canDrawOverlays]; without that permission this is a no-op (the run
 * still polls — the owner just doesn't get the on-screen prompt).
 */
class SignInWaitOverlay(private val context: Context) {
    companion object {
        private const val TAG = "SignInWaitOverlay"
        private const val ACCENT = 0xFF_2D_D4_BF.toInt()       // brand teal
        private const val BAR_BG = 0xCC_0B_0F_14.toInt()       // dark semi-transparent
        private const val TEXT_COLOR = 0xFF_EC_F0_F6.toInt()   // light text
        private const val SUBTLE_TEXT = 0x99_FF_FF_FF.toInt()  // 60% white

        /**
         * Auto-dismiss after this long without a refresh. The EA re-checks every
         * few seconds while a sign-in wall is up (re-firing `screen_signin_wait`),
         * which re-arms this; when the run resumes the banner clears on its own.
         */
        private const val IDLE_DISMISS_MS = 12_000L

        /**
         * A hand-off (payment / explicit) is fired ONCE — there is no poll to
         * re-arm it — so it lingers longer to give the owner time to read it and
         * act before it self-clears.
         */
        private const val HANDOFF_DISMISS_MS = 30_000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val dismissRunnable = Runnable { remove() }

    private var rootView: FrameLayout? = null
    private var titleView: TextView? = null
    private var attached = false
    /** Which layout the live view was built for — rebuild when it flips so a
     *  sign-in strip (bottom, non-touchable) and a hand-off card (top, touchable)
     *  never reuse each other's gravity / window flags. */
    private var builtForHandoff: Boolean? = null

    /** Show or refresh the banner. Safe to call repeatedly (re-arms the timer). */
    fun show(aiName: String?, kind: String?, message: String? = null) {
        mainHandler.post { showOnMain(aiName, kind, message) }
    }

    /** Tear the banner down immediately (e.g. on detach). */
    fun clear() {
        mainHandler.post { remove() }
    }

    /** A hand-off (payment / explicit) is the assistant stepping back so the owner
     *  finishes in place — rendered as a TOP dismissable card that stays clear of
     *  the bottom action button. A sign-in wall stays a bottom strip. */
    private fun isHandoffKind(kind: String?): Boolean =
        kind == "payment" || kind == "handoff" || kind == "explicit_handoff"

    private fun showOnMain(aiName: String?, kind: String?, message: String?) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission not granted; wait banner suppressed")
            return
        }
        val handoff = isHandoffKind(kind)
        // Rebuild if the layout mode flipped since the view was last built.
        if (rootView != null && builtForHandoff != handoff) remove()
        ensureView(handoff)
        // The acting assistant's user-chosen name (kernel-stamped). It has a name,
        // so the fallback is neutral — never a brand.
        val who = aiName ?: "your assistant"
        titleView?.text = bannerText(who, kind, message)
        if (!attached) {
            try {
                windowManager.addView(rootView, layoutParams(handoff))
                attached = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add wait banner", e)
            }
        }
        mainHandler.removeCallbacks(dismissRunnable)
        mainHandler.postDelayed(dismissRunnable, dismissMsFor(kind))
    }

    /**
     * The banner line for this wait kind. For a hand-off (payment / explicit) a
     * kernel-supplied [message] wins; sign-in / register use the built-in
     * "will resume" copy. [who] is the assistant's name.
     */
    private fun bannerText(who: String, kind: String?, message: String?): String = when (kind) {
        "register" ->
            "Please create your account to continue — $who will resume automatically."
        "payment" ->
            message?.takeIf { it.isNotBlank() }
                ?: "$who set this up — review and complete the payment to finish."
        "handoff", "explicit_handoff" ->
            message?.takeIf { it.isNotBlank() }
                ?: "$who handed this over to you to finish."
        else -> // login / null
            "Please sign in to continue — $who will resume automatically."
    }

    /** A sign-in wall is re-armed by the EA's polling (short window); a hand-off
     *  is fired once (no poll), so it lingers longer. */
    private fun dismissMsFor(kind: String?): Long = when (kind) {
        "payment", "handoff", "explicit_handoff" -> HANDOFF_DISMISS_MS
        else -> IDLE_DISMISS_MS
    }

    private fun ensureView(handoff: Boolean) {
        if (rootView != null) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val dot = View(context).apply {
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
        val title = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(TEXT_COLOR)
            maxLines = 2
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dp(10)
            }
        }
        titleView = title

        // Trailing element: a tappable "Got it" pill that dismisses a hand-off
        // card; for a sign-in strip, the quiet "powered by OpenAlly" mark.
        val trailing: View = if (handoff) {
            TextView(context).apply {
                text = "Got it"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(ACCENT)
                maxLines = 1
                includeFontPadding = false
                val h = dp(14); val v = dp(7)
                setPadding(h, v, h, v)
                background = GradientDrawable().apply {
                    cornerRadius = 16 * density
                    setStroke(dp(1), ACCENT)
                }
                setOnClickListener { remove() }
            }
        } else {
            TextView(context).apply {
                text = "powered by OpenAlly"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                setTextColor(SUBTLE_TEXT)
                maxLines = 1
                includeFontPadding = false
            }
        }

        val hPad = dp(16)
        val vPad = dp(12)
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(hPad, vPad, hPad, vPad)
            background = GradientDrawable().apply {
                cornerRadius = 22 * density
                setColor(BAR_BG)
            }
            addView(dot)
            addView(title)
            addView(trailing)
        }

        rootView = FrameLayout(context).apply {
            val side = dp(12)
            // A hand-off card hugs the TOP (clear of the bottom pay/confirm
            // button); a sign-in strip sits at the bottom.
            if (handoff) setPadding(side, dp(44), side, 0)
            else setPadding(side, 0, side, dp(24))
            addView(
                bar,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        builtForHandoff = handoff
    }

    private fun layoutParams(handoff: Boolean): WindowManager.LayoutParams {
        // A hand-off card is TOUCHABLE (so "Got it" works) and sits at the TOP, so
        // the bottom action button stays reachable (taps below the card fall
        // through to the app). A sign-in strip is non-touchable at the bottom so
        // the owner types into the form behind it.
        val flags = if (handoff) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (handoff) Gravity.TOP else Gravity.BOTTOM) or
                Gravity.START or Gravity.END
        }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun remove() {
        mainHandler.removeCallbacks(dismissRunnable)
        if (attached) {
            try {
                windowManager.removeView(rootView)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove wait banner", e)
            }
            attached = false
        }
        rootView = null
        titleView = null
        builtForHandoff = null
    }
}
