package com.aster.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import com.aster.BuildConfig
import com.aster.ui.CompanionApprovalActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CompanionFaceOverlay — hosts OpenAlly's ambient companion FACE in a
 * `TYPE_APPLICATION_OVERLAY` window docked beside the camera cutout.
 *
 * WHY ASTER HOSTS IT: OpenAlly ships on Google Play and must hold no
 * draw-over-other-apps permission. Aster is sideloaded and already holds
 * SYSTEM_ALERT_WINDOW for its screen-control overlays, so the WINDOW lives here and
 * OpenAlly only streams face geometry over the existing IPC bridge
 * (`IAsterService.pushCompanionFrame`). OpenAlly remains the pose source of truth;
 * [CompanionFaceView] adds a small native liveness/reaction layer only when remote
 * frames go stale, so Android background throttling cannot leave a frozen face.
 *
 * FRAME PATH (the part that must not misbehave). Frames arrive on a Binder pool
 * thread. Two properties are load-bearing:
 *
 *  1. **The transaction buffer is released when `onTransact` returns**, and a
 *     process's `oneway` allocations are capped at HALF its ~1 MB binder mapping
 *     (~508 KB) — a budget Aster SHARES with `system_server`'s delivery of
 *     accessibility events into `AsterAccessibilityService`. A slow frame handler
 *     would therefore not merely stutter the face, it could starve the very
 *     screen-control capability Aster exists to provide. So [onFrame] does the JSON
 *     parse right here on the binder thread and returns — it never blocks on the
 *     main thread, and it never queues unboundedly.
 *  2. **Only the newest frame matters.** A face frame is a complete picture, not a
 *     delta, so if the main thread is behind, older frames are worthless. [pending]
 *     holds exactly one frame and a single drain is posted; a burst collapses to one
 *     repaint instead of N.
 *
 * The face is DECORATIVE. A bare tap performs nothing privileged — it routes to
 * [CompanionApprovalActivity], a normal, tap-jacking-protected activity, exactly as
 * it did when the window lived inside OpenAlly.
 */
@Singleton
class CompanionFaceOverlay @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "CompanionFaceOverlay"

        /** Responsive ambient pill bounds. The painter uses the same 200×96 crop as
         *  the desktop notch, so the Android surface reads as a notch rather than a
         *  square floating panel. */
        private const val MIN_PILL_WIDTH_DP = 168
        private const val MAX_PILL_WIDTH_DP = 240
        private const val PILL_SIDE_MARGIN_DP = 8
        private const val CUTOUT_SIDE_ROOM_DP = 96
        private const val CUTOUT_HANG_DP = 44

        /** No cutout reported → an honest top-centre dock below the status bar.
         *  The cutout is never guessed. */
        private const val FALLBACK_TOP_DP = 8
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private val displayManager: DisplayManager by lazy {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private var container: FrameLayout? = null
    private var faceView: CompanionFaceView? = null
    private var params: WindowManager.LayoutParams? = null
    private val pulseMonitor by lazy { CompanionSystemPulseMonitor(context, ::onSystemPulse) }

    private data class PowerSnapshot(
        val charging: Boolean,
        val external: Boolean,
        val percent: Int,
    )

    private var lastPower: PowerSnapshot? = null
    private var lastBatteryBucket = -1
    private var airborne = false
    @Volatile
    private var pulseConfiguration = CompanionPulseConfiguration.NONE
    /** Main-thread attribution for a currently visible native moment. */
    private var nativeMomentLane: String? = null
    private var dispatchingPulseLane: String? = null
    /** Last OpenAlly-arbitrated readout, preserved beneath short native moments. */
    private var remoteStatus: CompanionStatusModel? = null

    /** Read from the binder thread on every frame; written on the main thread. */
    @Volatile
    private var attached = false

    /** The newest undrawn frame (older ones are dropped — see the class doc). */
    private val pending = AtomicReference<CompanionFaceModel?>(null)
    private val drainScheduled = AtomicBoolean(false)

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            mainHandler.post {
                applyDisplayPowerState()
                recomputeGeometry()
            }
        }
    }

    // ── state ────────────────────────────────────────────────────────────────

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun isAttached(): Boolean = attached

    // ── lifecycle ────────────────────────────────────────────────────────────

    /**
     * Attach the face window. Returns false — honestly, without side effects — when
     * Aster does not hold draw-over-other-apps; the caller (OpenAlly) then routes the
     * user to [com.aster.ui.OverlayPermissionActivity] to grant it to ASTER, which is
     * the only app that can ask for it.
     */
    suspend fun show(): Boolean {
        if (!canDrawOverlays()) {
            Log.w(TAG, "show() refused — Aster does not hold draw-over-other-apps")
            return false
        }
        if (attached) return true
        // `show` is called by a suspendable command handler. Await the real addView
        // result instead of optimistically reporting success while a main-thread post
        // is still pending; otherwise OpenAlly can send (and permanently dedupe) its
        // initial frame before this window exists.
        return withContext(Dispatchers.Main.immediate) { attachWindow() }
    }

    fun hide() {
        runOnMain { detachWindow() }
    }

    /** Recompute the cutout placement now (e.g. OpenAlly observed a rotation/fold).
     *  The window also recomputes autonomously on display change. */
    fun recompute() {
        runOnMain { recomputeGeometry() }
    }

    // ── the frame lane (binder pool thread) ──────────────────────────────────

    /**
     * Ingest one serialized `buildGeometry` frame. Called on a Binder pool thread —
     * see the class doc for why the parse happens here and why only the newest frame
     * survives. Returns immediately; a malformed frame is dropped (the last good
     * frame stays on screen).
     */
    fun onFrame(frame: ByteArray) {
        if (!attached) return
        val model = parseCompanionFace(String(frame, Charsets.UTF_8))
        if (model == null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "dropped a malformed companion frame")
            return
        }
        pending.set(model)
        // Coalesce: at most ONE drain in flight, always carrying the newest frame.
        if (drainScheduled.compareAndSet(false, true)) {
            mainHandler.post {
                drainScheduled.set(false)
                val next = pending.getAndSet(null) ?: return@post
                faceView?.setFrame(next)
            }
        }
    }

    /** Low-rate structured readout lane. Parsing stays on the Binder pool thread;
     *  malformed payloads are dropped and a wire `null` clears the status. */
    fun onStatus(payload: ByteArray) {
        if (!attached) return
        val parsed = parseCompanionStatus(String(payload, Charsets.UTF_8), SystemClock.elapsedRealtime())
        if (!parsed.valid) {
            if (BuildConfig.DEBUG) Log.w(TAG, "dropped a malformed companion status")
            return
        }
        runOnMain {
            remoteStatus = parsed.status
            // Native system moments have the same priority role as the shared
            // arbiter's `system` channel. Keep them visible for their bounded TTL,
            // but continuously remember the underlying Pomodoro/music readout.
            if (nativeMomentLane == null) faceView?.setStatus(parsed.status)
        }
    }

    /** Apply the last known-good System Pulse policy even while no window is up. */
    fun onConfiguration(payload: ByteArray) {
        val parsed = parseCompanionPulseConfiguration(String(payload, Charsets.UTF_8))
        if (parsed == null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "dropped a malformed companion configuration")
            return
        }
        pulseConfiguration = parsed
        runOnMain { applyPulseConfiguration(parsed) }
    }

    /** Native/background system moment. Safe from Binder, sensor, and main threads. */
    fun onSystemPulse(kind: String, values: Map<String, Any>) {
        if (!attached) return
        runOnMain { applySystemPulse(kind, values) }
    }

    // ── window ───────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun attachWindow(): Boolean {
        if (attached) return true

        val face = CompanionFaceView(context).apply {
            onStatusVisibilityChanged = {
                mainHandler.post {
                    if (faceView?.hasStatus() != true && faceView?.hasStatusModel() != true) {
                        val nativeExpired = nativeMomentLane != null
                        nativeMomentLane = null
                        if (nativeExpired) {
                            remoteStatus?.let { faceView?.setStatus(it) }
                        } else {
                            // A remote side readout reached its safety expiry.
                            remoteStatus = null
                        }
                    }
                    recomputeGeometry()
                }
            }
        }
        faceView = face

        val root = FrameLayout(context).apply {
            addView(
                face,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            // Tap-jacking hygiene: drop touches delivered while another window
            // obscures this one, so nothing under the face can drive it.
            filterTouchesWhenObscured = true
            setOnClickListener { openApproval() }
            setOnTouchListener(faceTouchListener())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // On API 28–29 `rootWindowInsets` is commonly null immediately after
                // addView. Re-run placement when the platform delivers the real cutout
                // instead of leaving the one-time fallback parked below the status bar.
                setOnApplyWindowInsetsListener { _, insets ->
                    mainHandler.post { recomputeGeometry() }
                    insets
                }
            }
        }
        container = root

        val lp = buildLayoutParams()
        params = lp
        try {
            windowManager.addView(root, lp)
            attached = true
            recomputeGeometry()
            applyDisplayPowerState()
            displayManager.registerDisplayListener(displayListener, mainHandler)
            applyPulseConfiguration(pulseConfiguration)
            pulseMonitor.start(displayActive())
            Log.i(TAG, "companion face attached")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add companion face window", e)
            container = null
            faceView = null
            params = null
            return false
        }
    }

    private fun detachWindow() {
        if (!attached) return
        attached = false
        pending.set(null)
        pulseMonitor.stop()
        lastPower = null
        lastBatteryBucket = -1
        airborne = false
        nativeMomentLane = null
        remoteStatus = null
        runCatching { displayManager.unregisterDisplayListener(displayListener) }
        container?.let { root -> runCatching { windowManager.removeView(root) } }
        container = null
        faceView = null
        params = null
        Log.i(TAG, "companion face detached")
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val lp = WindowManager.LayoutParams(
            dp(MIN_PILL_WIDTH_DP),
            dp(MIN_PILL_WIDTH_DP * CompanionOverlayGeometry.CROP_HEIGHT / CompanionOverlayGeometry.CROP_WIDTH),
            overlayType(),
            // Non-focusable (never steals focus), non-touch-modal + LAYOUT_NO_LIMITS
            // so taps outside the small face pass straight through to the app beneath
            // (this is also what keeps Android 12+ untrusted-touch blocking from ever
            // firing), hardware-accelerated.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        // Render into the cutout region so the face can dock right beside the hole.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return lp
    }

    private fun overlayType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    // ── punch-hole geometry ──────────────────────────────────────────────────

    /**
     * Centre the ambient pill ON the camera cutout and hang it below the sensor area,
     * matching the desktop notch's silhouette. When the platform reports NO cutout,
     * fall back to an honest top-centre dock below the status bar — the hole is never
     * guessed.
     */
    private fun recomputeGeometry() {
        val root = container ?: return
        val lp = params ?: return

        val screenW = displayWidth()
        val cutout = preferredCutoutRect(root, screenW)?.let {
            CutoutBounds(it.left, it.top, it.right, it.bottom)
        }
        val geometry = CompanionOverlayGeometry.compute(
            screenWidthPx = screenW,
            density = context.resources.displayMetrics.density,
            cutout = cutout,
            statusBarHeightPx = statusBarHeight(root),
            minWidthDp = MIN_PILL_WIDTH_DP,
            maxWidthDp = MAX_PILL_WIDTH_DP,
            sideMarginDp = PILL_SIDE_MARGIN_DP,
            cutoutSideRoomDp = CUTOUT_SIDE_ROOM_DP,
            cutoutHangDp = CUTOUT_HANG_DP,
            fallbackTopDp = FALLBACK_TOP_DP,
        )
        val baseCentre = geometry.x + geometry.width / 2
        val statusWidth = if (faceView?.hasStatus() == true) {
            minOf(screenW - dp(PILL_SIDE_MARGIN_DP * 2), dp(336))
        } else {
            geometry.width
        }
        val resolvedX = (baseCentre - statusWidth / 2).coerceIn(
            dp(PILL_SIDE_MARGIN_DP),
            (screenW - statusWidth - dp(PILL_SIDE_MARGIN_DP)).coerceAtLeast(dp(PILL_SIDE_MARGIN_DP)),
        )
        if (
            lp.width == statusWidth &&
            lp.height == geometry.height &&
            lp.x == resolvedX &&
            lp.y == geometry.y
        ) {
            faceView?.bottomCornerRadiusPx = geometry.bottomCornerRadius
            return
        }
        faceView?.bottomCornerRadiusPx = geometry.bottomCornerRadius
        lp.width = statusWidth
        lp.height = geometry.height
        lp.x = resolvedX
        lp.y = geometry.y

        runCatching { windowManager.updateViewLayout(root, lp) }
            .onFailure { Log.w(TAG, "Failed to reposition companion face", it) }
    }

    private fun preferredCutoutRect(root: View, screenWidth: Int): Rect? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cutout = windowManager.currentWindowMetrics.windowInsets.displayCutout ?: return null
            return preferredTopCutout(cutout.boundingRects, screenWidth)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = root.rootWindowInsets?.displayCutout ?: return null
            return preferredTopCutout(cutout.boundingRects, screenWidth)
        }
        return null
    }

    /** Prefer a top-edge sensor and, when an OEM reports several cutouts, the one
     *  nearest the display centre. `boundingRects.first()` is not an ordering API. */
    private fun preferredTopCutout(rects: List<Rect>, screenWidth: Int): Rect? =
        rects
            .filter { !it.isEmpty && it.top <= 0 }
            .minByOrNull { kotlin.math.abs(it.centerX() - screenWidth / 2) }

    private fun displayWidth(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return windowManager.currentWindowMetrics.bounds.width()
        }
        @Suppress("DEPRECATION")
        return context.resources.displayMetrics.widthPixels
    }

    private fun statusBarHeight(root: View): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
        } else {
            @Suppress("DEPRECATION")
            root.rootWindowInsets?.stableInsetTop ?: dp(24)
        }

    /** Stop repainting while the display is off/dozing (battery); the view repaints
     *  its last frame on resume. */
    private fun applyDisplayPowerState() {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        val paused = display.state == Display.STATE_OFF ||
            display.state == Display.STATE_DOZE ||
            display.state == Display.STATE_DOZE_SUSPEND
        faceView?.paused = paused
        pulseMonitor.setDisplayActive(!paused)
    }

    private fun displayActive(): Boolean {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return true
        return display.state != Display.STATE_OFF &&
            display.state != Display.STATE_DOZE &&
            display.state != Display.STATE_DOZE_SUSPEND
    }

    private fun applySystemPulse(kind: String, values: Map<String, Any>) {
        if (kind == "interaction" || kind == "scroll" || kind == "typing") {
            // User handling is also a classifier guard: do not call a deliberate
            // phone movement a height scare merely because that reaction is muted.
            pulseMonitor.noteInteraction()
        }
        val lane = when (kind) {
            "motion" -> "lift"
            else -> kind
        }
        val configuration = pulseConfiguration
        val now = SystemClock.elapsedRealtime()
        dispatchingPulseLane = lane
        try {
            when (kind) {
            "power" -> applyPowerPulse(values, now, configuration.allows("power"))
            "motion" -> {
                if (!configuration.allows("lift")) return
                val moving = values["moving"] as? Boolean ?: return
                if (moving) {
                    if (!airborne) {
                        airborne = true
                        showNativeMoment(
                            CompanionStatusModel("side", "Too high!", "Hold on", "move", "#fb7185", null, null),
                            CompanionReaction.LIFT,
                            0L,
                            now,
                        )
                    }
                } else if (airborne) {
                    airborne = false
                    showNativeMoment(
                        CompanionStatusModel("side", "Solid ground", "Phew", "move", "#34d399", null, null),
                        CompanionReaction.LAND,
                        4_400L,
                        now,
                    )
                }
            }
            "shake" -> if (configuration.allows("shake")) showNativeMoment(
                CompanionStatusModel("side", "Whoa there!", "Easy on the shake", "vibrate", "#fb7185", null, null),
                CompanionReaction.SHAKE,
                3_200L,
                now,
            )
            "foregroundApp" -> {
                if (!configuration.allows("foregroundApp")) return
                if (values["initial"] == true) return
                val pkg = values["packageName"]?.toString() ?: return
                showNativeMoment(
                    CompanionStatusModel("side", "New scene", packageHint(pkg), "app", "#a78bfa", null, null),
                    CompanionReaction.CURIOUS,
                    3_000L,
                    now,
                )
            }
            "interaction" -> {
                if (!configuration.allows("interaction")) return
                val longPress = values["action"] == "longPress"
                showNativeMoment(
                    CompanionStatusModel("side", if (longPress) "Holding on" else "Boop!", if (longPress) "Still with you" else "I felt that", "touch", "#fb7185", null, null),
                    CompanionReaction.BOOP,
                    2_300L,
                    now,
                )
            }
            "scroll" -> {
                if (!configuration.allows("scroll")) return
                val direction = values["direction"]?.toString()?.take(12) ?: "down"
                showNativeMoment(
                    CompanionStatusModel("side", if (direction == "up" || direction == "left") "Wheee!" else "Down we go", "${direction.replaceFirstChar(Char::uppercase)}ward scroll", "scroll", "#22d3ee", null, null),
                    CompanionReaction.SCROLL,
                    2_100L,
                    now,
                )
            }
            "typing" -> {
                if (!configuration.allows("typing")) return
                if (values["active"] != true) return
                showNativeMoment(
                    CompanionStatusModel("side", "Thinking with you", "Keys are moving", "typing", "#60a5fa", null, null),
                    CompanionReaction.TYPING,
                    2_800L,
                    now,
                )
            }
            "notification" -> {
                if (!configuration.allows("notification")) return
                val pkg = values["packageName"]?.toString() ?: return
                showNativeMoment(
                    CompanionStatusModel("side", "New ping", packageHint(pkg), "notification", "#fbbf24", null, null),
                    CompanionReaction.PING,
                    3_200L,
                    now,
                )
            }
        }
        } finally {
            dispatchingPulseLane = null
        }
    }

    private fun applyPowerPulse(values: Map<String, Any>, now: Long, reactionsEnabled: Boolean) {
        val next = PowerSnapshot(
            charging = values["charging"] as? Boolean ?: return,
            external = values["external"] as? Boolean ?: return,
            percent = (values["percent"] as? Number)?.toInt()?.coerceIn(0, 100) ?: return,
        )
        val previous = lastPower
        lastPower = next
        val bucket = next.percent / 5
        if (values["initial"] == true || previous == null) {
            lastBatteryBucket = bucket
            return
        }
        if (!reactionsEnabled) {
            // Keep the baseline current while muted so re-enabling never surfaces a
            // transition that happened while the user explicitly had this lane off.
            lastBatteryBucket = bucket
            return
        }
        when {
            next.external && !previous.external -> showNativeMoment(
                CompanionStatusModel("side", "Charging", "${next.percent}%", "bolt", "#34d399", next.percent / 100f, null),
                CompanionReaction.CHARGE,
                5_000L,
                now,
            )
            !next.external && previous.external -> showNativeMoment(
                CompanionStatusModel("side", "On battery", "${next.percent}%", "battery", "#fbbf24", next.percent / 100f, null),
                CompanionReaction.UNPLUG,
                5_000L,
                now,
            )
            next.percent == 100 && previous.percent < 100 -> showNativeMoment(
                CompanionStatusModel("side", "Fully charged", "100%", "bolt", "#34d399", 1f, null),
                CompanionReaction.CHARGE,
                5_500L,
                now,
            )
            previous.percent > 10 && next.percent <= 10 -> showNativeMoment(
                CompanionStatusModel("side", "Battery critical", "${next.percent}% left", "battery", "#fb7185", next.percent / 100f, null),
                CompanionReaction.LOW_BATTERY,
                7_500L,
                now,
            )
            previous.percent > 20 && next.percent <= 20 -> showNativeMoment(
                CompanionStatusModel("side", "Battery low", "${next.percent}% left", "battery", "#fbbf24", next.percent / 100f, null),
                CompanionReaction.LOW_BATTERY,
                6_500L,
                now,
            )
            bucket != lastBatteryBucket -> showNativeMoment(
                CompanionStatusModel("side", if (next.charging) "Charging" else "Battery", "${next.percent}%", if (next.charging) "bolt" else "battery", if (next.charging) "#34d399" else "#60a5fa", next.percent / 100f, null),
                if (next.charging) CompanionReaction.CHARGE else CompanionReaction.CURIOUS,
                4_200L,
                now,
            )
        }
        lastBatteryBucket = bucket
    }

    private fun showNativeMoment(
        status: CompanionStatusModel,
        reaction: CompanionReaction,
        ttlMs: Long,
        now: Long,
    ) {
        nativeMomentLane = dispatchingPulseLane
        faceView?.setStatus(status.copy(expiresAtMs = if (ttlMs == 0L) 0L else now + ttlMs))
        faceView?.react(reaction, now)
    }

    private fun applyPulseConfiguration(configuration: CompanionPulseConfiguration) {
        pulseMonitor.configure(
            liftEnabled = configuration.allows("lift"),
            shakeEnabled = configuration.allows("shake"),
        )
        val activeLane = nativeMomentLane
        if (activeLane != null && !configuration.allows(activeLane)) {
            nativeMomentLane = null
            faceView?.setStatus(remoteStatus)
        }
        if (airborne && !configuration.allows("lift")) {
            airborne = false
            faceView?.react(CompanionReaction.LAND)
        }
    }

    private fun packageHint(packageName: String): String =
        packageName.split('.').lastOrNull().orEmpty().replace('_', ' ').replace('-', ' ').take(28)

    // ── interaction ──────────────────────────────────────────────────────────

    /** The face is draggable and tappable. A bare TAP does nothing privileged: it
     *  opens [CompanionApprovalActivity], never an action from the overlay window
     *  itself. Touches arriving while the window is obscured are dropped. */
    private fun faceTouchListener(): View.OnTouchListener {
        val touchSlop = (8 * context.resources.displayMetrics.density).toInt()
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        return View.OnTouchListener { view, ev ->
            if ((ev.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0) return@OnTouchListener false
            val lp = params ?: return@OnTouchListener false
            val root = container ?: return@OnTouchListener false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    startX = lp.x
                    startY = lp.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - downX).toInt()
                    val dy = (ev.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) dragged = true
                    if (dragged) {
                        lp.x = startX + dx
                        lp.y = startY + dy
                        runCatching { windowManager.updateViewLayout(root, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun openApproval() {
        runCatching {
            context.startActivity(
                Intent(context, CompanionApprovalActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { Log.w(TAG, "Failed to open the companion approval card", it) }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
