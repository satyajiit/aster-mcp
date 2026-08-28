package com.aster.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/**
 * CompanionApprovalActivity — the tap-jacking-protected landing for anything reached
 * from OpenAlly's ambient companion face, which Aster hosts in an over-other-apps
 * window ([com.aster.service.overlay.CompanionFaceOverlay]).
 *
 * The face is strictly DECORATIVE: a bare tap on the overlay never performs a
 * privileged action from the overlay window itself. It routes HERE — a normal
 * activity that sets `filterTouchesWhenObscured` on its root AND independently drops
 * any `FLAG_WINDOW_IS_OBSCURED` touch, so a window layered over this one can never
 * drive a confirmation. Today it offers only the benign "Open OpenAlly" summon; it is
 * the surface any future consequential confirmation would render in, always behind
 * that obscured-touch floor.
 *
 * Built programmatically (no resource plumbing); the translucent theme comes from the
 * manifest, matching Aster's other overlay activities.
 */
class CompanionApprovalActivity : Activity() {

    companion object {
        private const val OPENALLY_PACKAGE = "openally.ai"

        private val CARD_BG = Color.parseColor("#111827")
        private val TITLE_INK = Color.parseColor("#ECF0F6")
        private val BODY_INK = Color.parseColor("#99FFFFFF")
        private val ACCENT = Color.parseColor("#2DD4BF")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = GradientDrawable().apply {
                cornerRadius = 20 * density
                setColor(CARD_BG)
            }
            // Tap-jacking floor: refuse touches while another window obscures this one.
            filterTouchesWhenObscured = true
        }

        val title = TextView(this).apply {
            text = getString(com.aster.R.string.companion_approval_title)
            setTextColor(TITLE_INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        val body = TextView(this).apply {
            text = getString(com.aster.R.string.companion_approval_body)
            setTextColor(BODY_INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(16))
        }
        val open = TextView(this).apply {
            text = getString(com.aster.R.string.companion_approval_open)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            val h = dp(20)
            val v = dp(10)
            setPadding(h, v, h, v)
            background = GradientDrawable().apply {
                cornerRadius = 18 * density
                setColor(ACCENT)
            }
            // Belt-and-suspenders: drop obscured touches on the action itself too.
            setOnTouchListener { view, ev ->
                if ((ev.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0) return@setOnTouchListener true
                if (ev.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                false
            }
            setOnClickListener { openOpenAlly() }
        }

        card.addView(title)
        card.addView(body)
        card.addView(open)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(
                card,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            setOnClickListener { finish() } // tap the scrim → dismiss
        }
        setContentView(root)
    }

    /** Summon OpenAlly. Aster holds SYSTEM_ALERT_WINDOW, which exempts it from the
     *  background-activity-launch restrictions — but this activity is foreground
     *  anyway by the time the button is pressed. */
    private fun openOpenAlly() {
        runCatching {
            packageManager.getLaunchIntentForPackage(OPENALLY_PACKAGE)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
        }
        finish()
    }
}
