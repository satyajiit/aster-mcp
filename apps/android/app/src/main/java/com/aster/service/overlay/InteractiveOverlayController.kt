package com.aster.service.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.aster.service.overlay.InteractiveOverlayModel.InteractivePrompt
import com.aster.ui.InteractivePromptActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Automations /goal R-C — the blocking interactive-overlay round-trip.
 *
 * The kernel's `screen_prompt` / `screen_approve` tools dispatch a synchronous
 * `device.execute`; [com.aster.service.handlers.InteractiveOverlayHandler] calls
 * [awaitChoice], which shows a focusable scrim overlay (or, when draw-overlays
 * is not granted, the [InteractivePromptActivity] fallback) and **suspends**
 * until the owner acts — so the kernel host-call stays parked until a choice
 * comes back. The chosen JSON is returned verbatim to the EA.
 *
 * Exactly one prompt is in flight at a time. The kill switch
 * ([com.aster.service.safety.KillSwitchController.stop]) calls [cancelInFlight],
 * which resolves the parked prompt as cancel/reject and tears the surface down
 * immediately so the run aborts within one action.
 */
@Singleton
class InteractiveOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "InteractiveOverlay"

        /** Intent extra carrying the prompt epoch to the fallback Activity, so a
         *  stale Activity (whose prompt already resolved) finishes itself. */
        const val EXTRA_EPOCH = "com.aster.interactive.epoch"

        private const val SCRIM = 0xCC_05_07_0A.toInt()       // dark scrim
        private const val CARD_BG = 0xFF_12_17_1E.toInt()     // card surface
        private const val ACCENT = 0xFF_2D_D4_BF.toInt()      // Aster teal
        private const val TEXT_COLOR = 0xFF_EC_F0_F6.toInt()  // light text
        private const val SUBTLE_TEXT = 0xB3_FF_FF_FF.toInt() // 70% white
        private const val FAINT_TEXT = 0x80_FF_FF_FF.toInt()  // 50% white (meta)
        private const val FIELD_BG = 0xFF_1E_25_2F.toInt()    // input surface
        private const val OPTION_BG = 0xFF_18_1F_29.toInt()   // draft card surface
        private const val CARD_BORDER = 0x1A_FF_FF_FF.toInt() // 10% white hairline
        private const val DIVIDER = 0x14_FF_FF_FF.toInt()     // 8% white rule
        private const val REJECT_RED = 0xFF_EF_44_44.toInt()
        private const val REJECT_BORDER = 0x33_EF_44_44.toInt() // 20% red outline
        private const val ON_ACCENT = 0xFF_04_22_1F.toInt()   // deep teal-ink on teal
        private const val CARD_MAX_WIDTH_DP = 460
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val lock = Any()
    /** The single in-flight prompt's result sink. Non-null while one is showing. */
    @Volatile private var inFlight: CompletableDeferred<JsonObject>? = null

    /** Live scrim view (touched on the main thread only). */
    private var overlayView: View? = null

    /**
     * The prompt the [InteractivePromptActivity] fallback should render, and the
     * epoch identifying THIS prompt. The epoch lets a stale Activity (a late
     * `onCreate`, or one whose prompt already resolved) detect it is no longer
     * the live prompt and finish itself, and prevents one Activity's `onDestroy`
     * from clobbering a newer prompt's finisher (review F4/F6).
     */
    @Volatile var pendingPrompt: InteractivePrompt? = null
        private set
    @Volatile var liveEpoch: Long = 0
        private set
    private var epochSeq: Long = 0

    /** The live fallback Activity's finisher, tagged with its prompt epoch. */
    @Volatile private var activityFinisher: (() -> Unit)? = null
    @Volatile private var finisherEpoch: Long = -1

    /**
     * Show [prompt] and suspend until the owner acts, the kill switch fires, or
     * the prompt times out. Returns the choice JSON the kernel passes back to
     * the EA. Throws [IllegalStateException] if another prompt is already up.
     */
    suspend fun awaitChoice(prompt: InteractivePrompt): JsonObject {
        val deferred = CompletableDeferred<JsonObject>()
        val epoch = synchronized(lock) {
            check(inFlight == null) { "a prompt is already in flight" }
            inFlight = deferred
            pendingPrompt = prompt
            liveEpoch = ++epochSeq
            liveEpoch
        }
        try {
            present(prompt, epoch)
            return withTimeoutOrNull(prompt.timeoutMs) { deferred.await() }
                ?: prompt.timeoutResult()
        } finally {
            teardownUi(epoch)
            synchronized(lock) {
                inFlight = null
                if (liveEpoch == epoch) pendingPrompt = null
            }
        }
    }

    /** Complete the in-flight prompt with [result] (idempotent). Called from the UI. */
    fun deliver(result: JsonObject) {
        inFlight?.complete(result)
    }

    /**
     * Resolve any in-flight prompt as cancelled/rejected and tear the surface
     * down now. Wired into the kill switch's STOP teardown.
     */
    fun cancelInFlight(reason: String) {
        Log.w(TAG, "Interactive prompt cancelled: $reason")
        // cancelResult is type-correct per prompt (chooser cancel / approval
        // reject); deliver() targets the same in-flight deferred. teardownUi is
        // driven by awaitChoice's finally once the deferred resolves; we also
        // finish any fallback Activity immediately.
        val (prompt, epoch) = synchronized(lock) { pendingPrompt to liveEpoch }
        prompt?.let { deliver(it.cancelResult()) }
        teardownUi(epoch)
    }

    /**
     * Registered by a live fallback Activity (passing the [epoch] it captured at
     * `onCreate`) so teardown can finish it. If the prompt already resolved
     * (`inFlight == null`) or this is a stale epoch, the Activity is finished
     * immediately instead.
     */
    fun registerActivityFinisher(epoch: Long, finisher: () -> Unit) {
        val finishNow = synchronized(lock) {
            if (inFlight == null || epoch != liveEpoch) {
                true
            } else {
                activityFinisher = finisher
                finisherEpoch = epoch
                false
            }
        }
        if (finishNow) finisher()
    }

    /** Cleared by the fallback Activity in `onDestroy`, but only if it still owns
     *  the slot (a newer prompt's finisher must not be clobbered). */
    fun clearActivityFinisher(epoch: Long) {
        synchronized(lock) {
            if (finisherEpoch == epoch) {
                activityFinisher = null
                finisherEpoch = -1
            }
        }
    }

    // ── presentation ────────────────────────────────────────────────────────

    private fun present(prompt: InteractivePrompt, epoch: Long) {
        if (Settings.canDrawOverlays(context)) {
            mainHandler.post { showOverlay(prompt) }
        } else {
            // Fallback surface: a transparent Activity that renders the same
            // prompt and bridges the result back through this singleton. The
            // epoch rides in the Intent so a stale Activity self-finishes.
            try {
                context.startActivity(
                    Intent(context, InteractivePromptActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(EXTRA_EPOCH, epoch)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch fallback prompt activity", e)
                deliver(prompt.cancelResult())
            }
        }
    }

    private fun teardownUi(epoch: Long) {
        val finisher = synchronized(lock) {
            if (finisherEpoch == epoch) {
                val f = activityFinisher
                activityFinisher = null
                finisherEpoch = -1
                f
            } else {
                null
            }
        }
        mainHandler.post {
            overlayView?.let { v ->
                try {
                    windowManager.removeView(v)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove overlay", e)
                }
            }
            overlayView = null
            finisher?.invoke()
        }
    }

    private fun showOverlay(prompt: InteractivePrompt) {
        if (overlayView != null) return
        val root = buildScrim(prompt)
        try {
            windowManager.addView(root, scrimLayoutParams())
            overlayView = root
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add overlay", e)
            deliver(prompt.cancelResult())
        }
    }

    private fun scrimLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            // Focusable (NO FLAG_NOT_FOCUSABLE) so the text field + buttons work
            // and BACK is captured; LAYOUT_IN_SCREEN to cover the status bar.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    /** Build the scrim + centered card for [prompt] (classic views, main thread). */
    private fun buildScrim(prompt: InteractivePrompt): View {
        val card = buildCard(prompt)
        // Cap the card width so it reads as a centered dialog on large/landscape
        // screens instead of stretching edge-to-edge.
        val cardW = minOf(
            context.resources.displayMetrics.widthPixels - dp(32),
            dp(CARD_MAX_WIDTH_DP),
        )
        return FrameLayout(context).apply {
            setBackgroundColor(SCRIM)
            isFocusableInTouchMode = true
            // Deliberately NO tap-to-cancel on the scrim background: an
            // accidental outside tap (e.g. dismissing the keyboard) must not
            // reject an approval and discard the owner's edited draft (review
            // F5). Cancel is explicit — the Reject button or hardware BACK —
            // matching the Compose fallback (which also has no tap-to-dismiss).
            // Consume background taps so they don't fall through to the app.
            isClickable = true
            // Hardware BACK cancels.
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    deliver(prompt.cancelResult())
                    true
                } else {
                    false
                }
            }
            addView(
                card,
                FrameLayout.LayoutParams(
                    cardW,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.CENTER
                    val m = dp(16)
                    setMargins(m, m, m, m)
                },
            )
            requestFocus()
        }
    }

    private fun buildCard(prompt: InteractivePrompt): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true // consume taps so they don't bubble to the scrim
            val p = dp(18)
            setPadding(p, p, p, p)
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(CARD_BG)
                setStroke(dp(1), CARD_BORDER)
            }
        }
        // Branded header: avatar + AI name + the action context line.
        card.addView(brandHeader(prompt.aiName, headerSubtitle(prompt)))
        card.addView(spacer(dp(14)))
        card.addView(divider())
        card.addView(spacer(dp(14)))
        when (prompt) {
            is InteractivePrompt.Chooser -> buildChooser(card, prompt)
            is InteractivePrompt.Approval -> buildApproval(card, prompt)
        }
        return card
    }

    /** The contextual line under the AI name — the EA-supplied prompt/title. */
    private fun headerSubtitle(prompt: InteractivePrompt): String = when (prompt) {
        is InteractivePrompt.Approval -> prompt.title
        is InteractivePrompt.Chooser -> prompt.prompt
    }

    private fun buildChooser(card: LinearLayout, prompt: InteractivePrompt.Chooser) {
        val field: EditText? = prompt.textInput?.let { spec ->
            spec.label?.let {
                card.addView(label(it, 12f, SUBTLE_TEXT, bold = false))
                card.addView(spacer(dp(6)))
            }
            editField(spec.hint, spec.initial).also {
                card.addView(it)
                card.addView(spacer(dp(14)))
            }
        }
        prompt.options.forEach { opt ->
            card.addView(
                actionButton(opt.label, opt.hint, accent = opt.id == prompt.default) {
                    deliver(InteractiveOverlayModel.chooserResult(opt.id, field?.text?.toString()?.takeIf { it.isNotEmpty() }))
                },
            )
            card.addView(spacer(dp(8)))
        }
    }

    private fun buildApproval(card: LinearLayout, prompt: InteractivePrompt.Approval) {
        card.addView(label("Choose a draft to publish", 14f, TEXT_COLOR, bold = true))
        card.addView(spacer(dp(12)))

        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        prompt.variants.forEachIndexed { i, variant ->
            list.addView(variantCard(i + 1, variant, prompt.editable))
            list.addView(spacer(dp(10)))
        }
        // Scroll the variant list, capped so a tall set of drafts never pushes
        // the card off-screen on small devices.
        val maxListH = (context.resources.displayMetrics.heightPixels * 0.52).toInt()
        card.addView(
            CappedScrollView(context, maxListH).apply { addView(list) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        card.addView(spacer(dp(8)))
        // Footer: a quiet edit hint on the left, the Reject affordance on the right.
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        footer.addView(
            label(
                if (prompt.editable) "Tap a draft to edit" else "Pick a draft",
                12f,
                FAINT_TEXT,
                bold = false,
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        footer.addView(rejectButton { deliver(prompt.cancelResult()) })
        card.addView(footer)
    }

    /** One numbered draft card: eyebrow + (editable) body + char-count/Use-this row. */
    private fun variantCard(n: Int, variant: InteractiveOverlayModel.DraftVariant, editable: Boolean): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(OPTION_BG)
                setStroke(dp(1), CARD_BORDER)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        container.addView(label("OPTION $n", 11f, ACCENT, bold = true).apply { letterSpacing = 0.08f })
        container.addView(spacer(dp(6)))
        val body: EditText? = if (editable) {
            editFieldMultiline(variant.text).also { container.addView(it) }
        } else {
            container.addView(
                label(variant.text, 14f, TEXT_COLOR, bold = false).apply {
                    setLineSpacing(dp(2).toFloat(), 1f)
                },
            )
            null
        }
        container.addView(spacer(dp(10)))
        // Meta row: live character count + the teal "Use this" pill.
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val count = label("${variant.text.length} chars", 11f, FAINT_TEXT, bold = false).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        body?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                count.text = "${s?.length ?: 0} chars"
            }
        })
        row.addView(count)
        row.addView(
            usePill {
                deliver(
                    InteractiveOverlayModel.approvalResult(
                        decision = "approve",
                        selectedId = variant.id,
                        text = body?.text?.toString() ?: variant.text,
                    ),
                )
            },
        )
        container.addView(row)
        return container
    }

    // ── view helpers ─────────────────────────────────────────────────────────

    private fun dp(v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()

    private fun spacer(height: Int): View =
        View(context).apply { layoutParams = LinearLayout.LayoutParams(1, height) }

    private fun hSpacer(width: Int): View =
        View(context).apply { layoutParams = LinearLayout.LayoutParams(width, 1) }

    private fun divider(): View =
        View(context).apply {
            setBackgroundColor(DIVIDER)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        }

    /** Circular teal avatar carrying the AI's initial. */
    private fun avatar(aiName: String): View {
        val size = dp(40)
        val initial = aiName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A"
        return TextView(context).apply {
            text = initial
            gravity = Gravity.CENTER
            setTextColor(ON_ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ACCENT)
            }
            layoutParams = LinearLayout.LayoutParams(size, size)
        }
    }

    /** Avatar + AI name (primary) + the contextual subtitle (secondary). */
    private fun brandHeader(aiName: String, subtitle: String): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(avatar(aiName))
        row.addView(hSpacer(dp(12)))
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(label(aiName, 16f, TEXT_COLOR, bold = true))
        if (subtitle.isNotBlank()) {
            col.addView(spacer(dp(2)))
            col.addView(label(subtitle, 13f, SUBTLE_TEXT, bold = false))
        }
        row.addView(col)
        return row
    }

    private fun label(text: String, sizeSp: Float, color: Int, bold: Boolean): TextView =
        TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            includeFontPadding = false
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun editField(hint: String?, initial: String?): EditText =
        EditText(context).apply {
            setTextColor(TEXT_COLOR)
            setHintTextColor(SUBTLE_TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            hint?.let { this.hint = it }
            initial?.let { setText(it) }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(FIELD_BG)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

    /** Borderless multi-line editor used inside a draft card (the card supplies
     *  the surface, so this stays transparent and flush). */
    private fun editFieldMultiline(initial: String): EditText =
        EditText(context).apply {
            setTextColor(TEXT_COLOR)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setText(initial)
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(0, 0, 0, 0)
            background = null
            isSingleLine = false
            maxLines = 8
            setHorizontallyScrolling(false)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

    /** The teal "Use this ›" pill that approves a draft. */
    private fun usePill(onClick: () -> Unit): View {
        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(16), dp(9), dp(14), dp(9))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(ACCENT)
            }
            setOnClickListener { onClick() }
        }
        pill.addView(label("Use this", 13f, ON_ACCENT, bold = true))
        pill.addView(hSpacer(dp(5)))
        pill.addView(label("›", 15f, ON_ACCENT, bold = true))
        return pill
    }

    /** A quiet, outlined Reject affordance for the approval footer. */
    private fun rejectButton(onClick: () -> Unit): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), REJECT_BORDER)
            }
            setOnClickListener { onClick() }
            addView(label("Reject", 13f, REJECT_RED, bold = true))
        }

    private fun actionButton(
        text: String,
        hint: String?,
        accent: Boolean,
        danger: Boolean = false,
        onClick: () -> Unit,
    ): View {
        val fill = when {
            danger -> REJECT_RED
            accent -> ACCENT
            else -> FIELD_BG
        }
        // Deep-teal ink on the teal accent; white on the red; light on neutral.
        val fg = when {
            danger -> Color.WHITE
            accent -> ON_ACCENT
            else -> TEXT_COLOR
        }
        val hintColor = if (accent) 0xB3_04_22_1F.toInt() else if (danger) 0xCC_FF_FF_FF.toInt() else SUBTLE_TEXT
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(fill)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            setOnClickListener { onClick() }
        }
        column.addView(label(text, 15f, fg, bold = true).apply {
            gravity = Gravity.CENTER
        })
        if (hint != null) {
            column.addView(label(hint, 11f, hintColor, bold = false).apply {
                gravity = Gravity.CENTER
            })
        }
        return column
    }

    /** A [ScrollView] whose height never exceeds [maxH] — keeps a tall list of
     *  drafts from pushing the card past the screen on small devices. */
    private class CappedScrollView(context: Context, private val maxH: Int) : ScrollView(context) {
        override fun onMeasure(widthSpec: Int, heightSpec: Int) {
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST))
        }
    }
}
