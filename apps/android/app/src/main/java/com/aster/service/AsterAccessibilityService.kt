package com.aster.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.aster.BuildConfig
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.aster.service.accessibility.ActionMapper
import com.aster.service.accessibility.AccessibilityPulseClassifier
import com.aster.service.accessibility.Bounds
import com.aster.service.accessibility.ElementState
import com.aster.service.accessibility.LabelAggregator
import com.aster.service.accessibility.NodeDescriptor
import com.aster.service.accessibility.ObserveResult
import com.aster.service.accessibility.ObservedElement
import com.aster.service.accessibility.RoleMapper
import com.aster.service.accessibility.ScreenContext
import com.aster.service.accessibility.ScreenObserver
import com.aster.service.accessibility.SnapshotCache
import com.aster.service.accessibility.WindowInfo
import com.aster.service.input.InputResult
import com.aster.service.input.InputRouter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
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
@AndroidEntryPoint
class AsterAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AsterAccessibility"

        // Singleton instance for access from handlers
        @Volatile
        private var instance: AsterAccessibilityService? = null

        fun getInstance(): AsterAccessibilityService? = instance

        fun isServiceEnabled(): Boolean = instance != null

        /** Installed by [AsterService] while an IPC session is alive. The
         * payload is already privacy-minimised by the classifier. */
        @Volatile
        internal var onCompanionPulseEvent: ((AccessibilityPulseClassifier.Pulse) -> Unit)? = null

        // ── scroll-to-find bounds ────────────────────────────────────────────
        // An infinite feed's child-bounds signature changes on every scroll, so
        // "no progress" is never reached there and the loop had no exit. These are
        // the real terminators; the signature check stays as the fast path for a
        // finite list that has hit its end.
        /** Default wall-clock budget for one scroll-to-find. */
        const val SCROLL_FIND_TIMEOUT_MS = 15_000L
        /** Floor/ceiling applied to a caller-supplied budget. The ceiling stays well
         *  under the kernel's 150s REMOTE_TIMEOUT so the IPC call is never orphaned. */
        const val SCROLL_FIND_MIN_TIMEOUT_MS = 1_000L
        const val SCROLL_FIND_MAX_TIMEOUT_MS = 60_000L
        /** Hard cap on scroll steps, independent of the clock. */
        const val MAX_SCROLL_FIND_STEPS = 25

        // Global action constants
        const val ACTION_BACK = "BACK"
        const val ACTION_HOME = "HOME"
        const val ACTION_RECENTS = "RECENTS"
        const val ACTION_NOTIFICATIONS = "NOTIFICATIONS"
        const val ACTION_QUICK_SETTINGS = "QUICK_SETTINGS"
        const val ACTION_POWER_DIALOG = "POWER_DIALOG"
        const val ACTION_LOCK_SCREEN = "LOCK_SCREEN"

        // IME action constants
        const val ACTION_ENTER = "ENTER"
        const val ACTION_SEARCH = "SEARCH"
        const val ACTION_DONE = "DONE"
        const val ACTION_GO = "GO"
        const val ACTION_NEXT = "NEXT"
        const val ACTION_PREVIOUS = "PREVIOUS"

        // Gesture types
        const val GESTURE_TAP = "TAP"
        const val GESTURE_SWIPE = "SWIPE"
        const val GESTURE_LONG_PRESS = "LONG_PRESS"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val companionPulseClassifier = AccessibilityPulseClassifier()

    /** On-device OCR engine (lazily creates the ML Kit recognizer; closed in onDestroy). */
    private val ocrEngine = OcrEngine()

    /** Quiescence + revision tracker fed by onAccessibilityEvent (SPEC §3.3). */
    val screenSyncTracker = ScreenSyncTracker()

    /**
     * Re-resolution descriptor cache for `observe`. Service-held singleton so a
     * snapshot survives between an observe and the ref-based actions P2 adds.
     * Stores POJO descriptors only — NEVER live AccessibilityNodeInfo.
     */
    val snapshotCache = SnapshotCache()

    /**
     * The ordered key-press backend chain (see [com.aster.service.input.InputRouter]).
     *
     * Field-injected via `@AndroidEntryPoint`: Hilt injects in the generated
     * `onCreate`, which the framework always runs before `onServiceConnected` and
     * therefore before any command can reach this class — the service is only
     * discoverable through [getInstance], which `onServiceConnected` sets.
     *
     * Single binding style, matching [com.aster.di.ModeModule]: the chain is
     * assembled by exactly one `@Provides` in [com.aster.di.InputModule]. No
     * backend carries an `@Inject` constructor (a second binding for the same
     * type is a duplicate-binding error) and nothing injects a bare
     * `List<InputBackend>` (Dagger has no binding for one).
     */
    @Inject
    lateinit var inputRouter: InputRouter

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
        // Hot path: fires very frequently on the main thread. Keep it allocation-free —
        // just stamp the change time + bump the revision. Do NOT walk the tree here and do
        // NOT recycle the event (the framework owns it). See SPEC §3.3.
        val type = event?.eventType ?: return
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            // Stamp WHICH surface changed, so `changed` can distinguish "my tap
            // did something" from "a video is playing somewhere on the device".
            screenSyncTracker.recordChange(
                System.currentTimeMillis(),
                screenSyncTracker.surfaceKey(event.packageName?.toString(), event.windowId),
            )
        }

        // System Pulse observes only allow-listed event metadata. This branch
        // never accesses event.text, contentDescription, or event.source.
        val pulseEvent = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            type == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED ||
            type == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        if (pulseEvent) onCompanionPulseEvent?.let { publish ->
            val dx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaX else 0
            val dy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaY else 0
            val input = AccessibilityPulseClassifier.Input(
                eventType = type,
                packageName = event.packageName?.toString(),
                scrollDeltaX = dx,
                scrollDeltaY = dy,
                fromIndex = event.fromIndex,
                toIndex = event.toIndex,
            )
            companionPulseClassifier.classify(input, System.currentTimeMillis()).forEach(publish)
        }
    }

    /**
     * Package name of the current foreground/active window, for `foreground_after`
     * verification (SPEC §3.4). Reads rootInActiveWindow and recycles it.
     */
    fun foregroundPackage(): String? {
        val root = rootInActiveWindow ?: return null
        return try {
            root.packageName?.toString()
        } finally {
            root.recycle()
        }
    }

    /** Current screen revision, for pre/post-act `changed` derivation (SPEC §3.4). */
    /**
     * Revision of the surface an action is about to touch — the foreground
     * package, narrowed to [windowId] when the caller knows which window its
     * element lives in.
     *
     * This is what `changed` is derived from. The global counter it replaced
     * ticked on ANY accessibility event anywhere, so on a screen with a live map
     * or an autoplay video it was always moving and `changed` was always true:
     * a tap that hit nothing was indistinguishable from one that worked.
     */
    @JvmOverloads
    fun screenRevision(windowId: Int? = null): Long =
        screenSyncTracker.revision(screenSyncTracker.surfaceKey(foregroundPackage(), windowId))

    /**
     * Identity fingerprint of the live window SET — window ids only, never their
     * content.
     *
     * The per-surface revision above cannot see the single most common effect of a
     * tap: navigation. Opening a new screen DESTROYS the window the element lived
     * in and creates another, so events land on the new window's key and the acted
     * surface's counter can never move again. With the foreground package
     * unchanged (an in-app screen change), `changed` came back FALSE for a tap that
     * had just navigated — the exact inversion of the signal, on the exact case the
     * signal exists for.
     *
     * Window IDENTITY is the right complement precisely because it ignores content:
     * a ticking clock, a live map and an autoplay video do not mint windows, so
     * this stays silent for the ambient churn that dimensioning was introduced to
     * suppress, while a new activity, dialog, IME or popup shows up immediately.
     *
     * Order-independent (XOR): the window list is not ordered by contract, and a
     * reshuffle is not a change.
     */
    fun windowsFingerprint(): Long {
        val windowList: List<AccessibilityWindowInfo> = try {
            windows
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "getWindows() threw", e)
            return 0L
        }
        var acc = 0L
        for (w in windowList) {
            // Mix so that {1,2} and {3} cannot collide on a plain XOR of ids.
            val id = w.id.toLong()
            acc = acc xor (id * -0x61c8864680b583ebL + 0x165667b19e3779f9L)
        }
        return acc
    }

    override fun onInterrupt() {
        if (BuildConfig.DEBUG) Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        companionPulseClassifier.reset()
        ocrEngine.close()
        if (BuildConfig.DEBUG) Log.d(TAG, "Accessibility service destroyed")
        super.onDestroy()
    }

    /**
     * Event-driven quiescence wait (SPEC §3.3). Resolves true once no
     * WINDOW_CONTENT_CHANGED / WINDOW_STATE_CHANGED event has fired for [quietMs], or
     * false at [timeoutMs]. Implemented as a SELF-RESCHEDULING main-looper Handler timer
     * that computes remaining-quiet from real ScreenSyncTracker timestamps — NOT a fixed
     * sleep. The delay shrinks/extends with real events (project policy: no timer band-aids).
     *
     * MUST be called off the main thread (handler entry runs on a Binder/IO thread via
     * IpcMode.runBlocking / Mcp / RemoteWs), so suspending here is safe and the main looper
     * stays free to fire the timer.
     */
    suspend fun waitForIdle(quietMs: Long = 500L, timeoutMs: Long = 5_000L): Boolean =
        suspendCancellableCoroutine { continuation ->
            val deadline = System.currentTimeMillis() + timeoutMs
            lateinit var tick: Runnable
            tick = Runnable {
                if (!continuation.isActive) return@Runnable
                val now = System.currentTimeMillis()
                val remainingQuiet = screenSyncTracker.remainingQuietMs(now, quietMs)
                if (remainingQuiet <= 0L) {
                    continuation.resume(true)            // quiet long enough -> idle
                    return@Runnable
                }
                if (now >= deadline) {
                    continuation.resume(false)           // timed out before quiescence
                    return@Runnable
                }
                // Reschedule for exactly the shorter of "remaining quiet" and "time to deadline".
                val nextDelay = minOf(remainingQuiet, deadline - now)
                mainHandler.postDelayed(tick, nextDelay)
            }
            continuation.invokeOnCancellation { mainHandler.removeCallbacks(tick) }
            // First check posts immediately; if already quiet it resolves on the first tick.
            mainHandler.post(tick)
        }

    /**
     * Post-act settle: wait for the action's effect to BEGIN, then for the screen
     * to go quiet.
     *
     * [waitForIdle] alone cannot do this. It answers "has the screen been quiet for
     * quietMs?", and immediately after a tap the honest answer is usually YES —
     * the app has not reacted yet, so the last recorded event is the one that
     * preceded the action. It therefore resolved on its first tick, at t≈0, and
     * every post-act reading (`changed`, `foreground_after`, and the observe the
     * next replay step takes) sampled the screen BEFORE the action had any effect.
     * Measured on a stock Settings row: the target activity resumes ~130 ms after
     * the tap; the settle was returning `true` before any of that.
     *
     * So: phase 1 waits for the first change AFTER the act, bounded by
     * [reactionMs]; phase 2 is the existing quiescence wait, spending whatever is
     * left of [timeoutMs]. A genuinely inert tap costs [reactionMs] and then
     * reports honestly — which is the point.
     *
     * Phase 1 watches the GLOBAL revision on purpose: the acted window is often
     * destroyed by what the action triggers, so a per-surface counter is exactly
     * the thing that cannot observe a navigation.
     *
     * @param preGlobalRevision [globalRevision] sampled BEFORE the act.
     */
    suspend fun waitForSettle(
        preGlobalRevision: Long,
        reactionMs: Long,
        quietMs: Long,
        timeoutMs: Long,
    ): Boolean {
        val started = System.currentTimeMillis()
        if (screenSyncTracker.revision() == preGlobalRevision) {
            withTimeoutOrNull(reactionMs.coerceAtMost(timeoutMs)) {
                while (screenSyncTracker.revision() == preGlobalRevision) {
                    screenSyncTracker.changes.first()
                }
            }
        }
        val remaining = timeoutMs - (System.currentTimeMillis() - started)
        if (remaining <= 0L) return screenSyncTracker.isIdle(System.currentTimeMillis(), quietMs)
        return waitForIdle(quietMs = quietMs, timeoutMs = remaining)
    }

    /** Monotonic count of every recorded change, anywhere — see [waitForSettle]. */
    fun globalRevision(): Long = screenSyncTracker.revision()

    /**
     * Event-nudged wait for an element to appear or disappear (SPEC §3.3).
     * Evaluates [com.aster.service.handlers.ScreenWaitMatcher] against a fresh observation:
     * once on entry, then on each ScreenSyncTracker change emission, then once more at the
     * timeout. No fixed poll interval.
     *
     * @param observe a callback that returns the current observe-shaped element array. Supplied
     *   by the caller (AccessibilityHandler) so this method does not depend on the P1 observe
     *   builder's internal types. Returns true if matched before [timeoutMs], else false.
     */
    suspend fun waitFor(
        text: String?,
        viewId: String?,
        role: String?,
        gone: Boolean,
        timeoutMs: Long,
        observe: () -> JsonArray
    ): Boolean {
        // Entry eval — resolve immediately if already satisfied (handles "already present"
        // and silent transitions where no further event will fire).
        if (com.aster.service.handlers.ScreenWaitMatcher.isSatisfied(observe(), text, viewId, role, gone)) {
            return true
        }
        // Event-nudged: re-eval on each tracker change, bounded by timeout. withTimeoutOrNull
        // returns null on timeout -> we do one final eval below before reporting false.
        val matched = withTimeoutOrNull(timeoutMs) {
            // Collect change nudges; on each, re-evaluate. Suspends between events — no poll loop.
            while (true) {
                screenSyncTracker.changes.first() // suspends until the next change emission
                if (com.aster.service.handlers.ScreenWaitMatcher.isSatisfied(observe(), text, viewId, role, gone)) {
                    return@withTimeoutOrNull true
                }
            }
            @Suppress("UNREACHABLE_CODE") false
        }
        if (matched == true) return true
        // Final eval at timeout (a change may have landed exactly as the window closed).
        return com.aster.service.handlers.ScreenWaitMatcher.isSatisfied(observe(), text, viewId, role, gone)
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
     * Window descriptor for SPEC §3.1 screen.windows[]: { id, type, title, focused }.
     */
    data class WindowDescriptor(
        val id: Int,
        val type: String,
        val title: String,
        val focused: Boolean
    )

    /**
     * Multi-window observation plumbing (SPEC §3.4). Enumerates getWindows() (requires
     * flagRetrieveInteractiveWindows), invokes [perWindow] with each window's root + window
     * id + descriptor so the caller (the P1 observe builder) can build window-tagged elements,
     * and returns the collected screen.windows[] descriptors. Falls back to rootInActiveWindow
     * when getWindows() is empty (flag not yet active after a config-cached re-enable).
     *
     * RECYCLE DISCIPLINE: recycles each window's root AccessibilityNodeInfo, each
     * AccessibilityWindowInfo, and the windows list. Missing a recycle starves the a11y node
     * pool over a long autonomous loop.
     *
     * @param perWindow receives (root, windowId, descriptor) for each window; called with a
     *   live root that is recycled immediately after the callback returns — the callback must
     *   NOT retain the root.
     */
    fun observeWindows(
        perWindow: (root: AccessibilityNodeInfo, windowId: Int, descriptor: WindowDescriptor) -> Unit
    ): List<WindowDescriptor> {
        val descriptors = mutableListOf<WindowDescriptor>()
        val windowList: List<AccessibilityWindowInfo> = try {
            windows  // AccessibilityService.getWindows()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "getWindows() threw", e)
            emptyList()
        }

        if (windowList.isEmpty()) {
            // Fallback: single active window (flag not yet in effect, or no interactive windows).
            val root = rootInActiveWindow
            if (root != null) {
                try {
                    val fallbackId = root.windowId
                    val descriptor = WindowDescriptor(
                        id = fallbackId,
                        type = "application",
                        title = root.packageName?.toString() ?: "",
                        focused = true
                    )
                    descriptors.add(descriptor)
                    perWindow(root, fallbackId, descriptor)
                } finally {
                    root.recycle()
                }
            }
            return descriptors
        }

        // App Automations /goal I3 (SPEC §I3): walk NON-application windows first
        // (system / navigation / decor / input_method / accessibility_overlay),
        // then the focused application window, then the rest. Paired with the
        // ScreenObserver's reserved system-window budget, this guarantees the
        // bottom-navigation/system window is never starved by a huge focused app
        // window — the model always sees its clickable tabs (the "Post" CTA).
        // Reordering the SAME list keeps the per-window root/window recycle
        // discipline intact (each entry is still recycled in the finally below).
        val orderedWindows = windowList.sortedBy { window ->
            when {
                windowTypeName(window.type) != "application" -> 0  // system / nav / IME / overlay first
                window.isFocused -> 1                              // then the focused app window
                else -> 2                                          // then any other app windows
            }
        }

        for (window in orderedWindows) {
            val root = window.root  // AccessibilityWindowInfo.getRoot()
            try {
                val descriptor = WindowDescriptor(
                    id = window.id,
                    type = windowTypeName(window.type),
                    title = window.title?.toString() ?: (root?.packageName?.toString() ?: ""),
                    focused = window.isFocused
                )
                descriptors.add(descriptor)
                if (root != null) {
                    perWindow(root, window.id, descriptor)
                }
            } finally {
                root?.recycle()
                window.recycle()
            }
        }
        return descriptors
    }

    /** Map AccessibilityWindowInfo.type ints to the SPEC §3.1 window.type strings. */
    private fun windowTypeName(type: Int): String = when (type) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> "application"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "input_method"
        AccessibilityWindowInfo.TYPE_SYSTEM -> "system"
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "accessibility_overlay"
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "split_screen_divider"
        else -> "unknown"
    }

    /**
     * Result of the synchronous a11y-only observation pass (the P1–P3 walk). Carries the
     * assembled a11y [ObserveResult] plus the sparsity signals P4's OCR merge consumes
     * (SPEC §3.4). Kept synchronous so the non-suspend `wait_for` matcher path can reuse it
     * without dragging the OCR/screenshot coroutine into the wait loop.
     */
    private class A11yObservation(
        val result: ObserveResult,
        val actionableCount: Int,
        val hasNonTrivialContent: Boolean,
    )

    /**
     * Build the a11y-only observation (SPEC §3.1, P1–P3): the multi-window walk, snapshot cache
     * population, screen context, and the source="a11y" [ObserveResult]. Synchronous — no OCR,
     * no screenshot. Returns null when there is no active window.
     */
    private fun buildA11yObservation(
        mode: String,
        searchText: String?,
        maxElements: Int,
    ): A11yObservation? {
        // ONE accumulating observer across all windows — repeated walk() calls keep the
        // SINGLE e<N> ref counter continuous (SPEC §7.1: one e<N> namespace), so
        // dialog/IME/overlay elements get unique refs in reading order. The element
        // BUDGET, however, is split per-window-type inside the observer (App
        // Automations /goal I3): application windows share `maxElements`, non-
        // application (system/nav/IME/overlay) windows share a separate reserve, so a
        // huge app window cannot starve the bottom-nav window. observeWindows walks the
        // non-application windows FIRST (see its ordering) to reinforce this.
        val observer = ScreenObserver(
            mode = mode,
            searchText = searchText?.takeIf { it.isNotEmpty() },
            maxElements = maxElements,
        )

        // P3 multi-window merge: walk every interactive window, tagging each element with its
        // window id (ScreenObserver records `window = windowId` per element). observeWindows
        // owns the root/window/list recycle discipline and falls back to rootInActiveWindow.
        // While a live root is in hand, capture the focused application window's REAL package
        // name for screen.foreground_package (SPEC §7.2 — the window title may be a human label,
        // not the package, so read it from root.packageName here, not from the descriptor.title).
        var lastWalk: ScreenObserver.Walk? = null
        var focusedAppPackage: String? = null
        var anyAppPackage: String? = null
        val descriptors = observeWindows { root, windowId, descriptor ->
            // I3: the descriptor.type selects the observer's budget bucket — only the
            // "application" type draws from maxElements; everything else draws the reserve.
            lastWalk = observer.walk(root, windowId, isApplication = descriptor.type == "application")
            if (descriptor.type == "application") {
                val pkg = root.packageName?.toString()
                if (pkg != null) {
                    if (anyAppPackage == null) anyAppPackage = pkg
                    if (descriptor.focused && focusedAppPackage == null) focusedAppPackage = pkg
                }
            }
        }

        val walk = lastWalk ?: return null

        // Allocate a snapshot and populate the ref → descriptor cache.
        val snapshotId = snapshotCache.newSnapshot()
        walk.descriptors.forEach { d -> snapshotCache.put(snapshotId, d.ref, d) }

        val foregroundPackage = focusedAppPackage ?: anyAppPackage ?: ""
        val screen = buildScreenContext(descriptors, foregroundPackage)

        val result = ObserveResult(
            screen = screen,
            elements = walk.elements,
            scrollables = walk.scrollables,
            source = "a11y",
            snapshotId = snapshotId,
            truncated = walk.truncated,
        )

        // SPEC §3.4 sparsity signals for the OCR merge: how many actionable elements the a11y
        // tree yielded, and whether the screen has non-trivial readable content (so a genuinely
        // blank screen is never treated as "sparse → OCR").
        val actionableCount = walk.elements.size
        val hasNonTrivialContent = walk.elements.any {
            it.text.isNotEmpty() || it.desc.isNotEmpty()
        }
        return A11yObservation(result, actionableCount, hasNonTrivialContent)
    }

    /**
     * Synchronous a11y-only `observe` payload (SPEC §3.1). Used by the event-nudged `wait_for`
     * matcher, which only matches against the a11y element model (text/viewId/role) and must not
     * pull OCR/screenshot work into its tight loop. The OCR-aware variant is [observe].
     */
    fun observeA11y(mode: String, searchText: String?, maxElements: Int): JsonElement {
        val a11y = buildA11yObservation(mode, searchText, maxElements)
            ?: return buildJsonObject { put("error", "No active window") }
        return a11y.result.toJson()
    }

    /**
     * Build the SPEC §3.1 `observe` payload: a compact, indexed, LLM-shaped view of the current
     * screen with stable e<N> refs backed by the snapshot cache, with P4 OCR fallback merged in
     * when the a11y tree is sparse (SPEC §3.4).
     *
     * @param mode "actionable" (default) | "text" | "full"
     * @param searchText narrows elements to those whose text/desc contains it
     * @param maxElements token-budget cap (truncated:true when exceeded)
     * @param ocr tri-state OCR gate: null = auto (run OCR only when the a11y tree is sparse),
     *   true = force OCR, false = never OCR (a11y only). When OCR runs, recognized blocks become
     *   `o<idx>` pseudo-elements (a distinct namespace, SPEC §7.1) appended AFTER the a11y `e<N>`
     *   elements; `source` becomes "ocr" (a11y empty) or "merged" (a11y sparse) per
     *   [ScreenObserveSupport.mergeOcr].
     */
    suspend fun observe(
        mode: String,
        searchText: String?,
        maxElements: Int,
        ocr: Boolean? = null,
    ): JsonElement {
        val a11y = buildA11yObservation(mode, searchText, maxElements)
            ?: return buildJsonObject { put("error", "No active window") }

        val a11ySparse = ScreenObserveSupport.isSparse(
            actionableCount = a11y.actionableCount,
            hasNonTrivialContent = a11y.hasNonTrivialContent,
        )

        // Auto (null) → OCR only when the a11y tree is sparse; explicit true/false overrides.
        val wantOcr = ocr ?: a11ySparse
        if (!wantOcr) {
            return a11y.result.toJson()
        }

        // runOcr() captures + recognizes a fresh screenshot and maps blocks back to REAL screen
        // pixels (1/scale). ocrBlocksToElements builds o<idx> pseudo-elements at scale=1.0 (the
        // blocks are already real-px). Fail-closed: runOcr() returns [] on any failure → merge
        // keeps a11y only.
        val ocrElements = ScreenObserveSupport.ocrBlocksToElements(runOcr(), scale = 1.0f)

        // a11y elements as JSON for the merge (OCR pseudo-elements are JsonObjects, so merge at
        // the JSON layer). The a11y e<N> refs stay backed by the snapshot cache; OCR o<idx> refs
        // have no descriptor and re-resolve only via center_tap of their cached bounds (SPEC §7.1).
        val a11yElementsJson = a11y.result.elements.map { it.toJson() }
        val merged = ScreenObserveSupport.mergeOcr(a11yElementsJson, ocrElements, a11ySparse)

        // Re-serialize the a11y result, replacing `elements` (a11y e<N> first, OCR o<idx>
        // appended) and `source` (a11y → ocr → merged). Everything else (screen, scrollables,
        // snapshot_id, truncated) is unchanged from the a11y pass.
        val base = a11y.result.toJson()
        return buildJsonObject {
            base.forEach { (k, v) ->
                when (k) {
                    "elements" -> put("elements", ScreenObserveSupport.elementsToJsonArray(merged))
                    "source" -> put("source", merged.source)
                    else -> put(k, v)
                }
            }
        }
    }

    /**
     * Extract the SPEC §3.1 screen context from the merged window descriptors (SPEC §3.4).
     * Real screen pixels (not the app-usable area) so bounds (from getBoundsInScreen, real px)
     * share one coordinate space.
     *
     * P3 multi-window: foreground_package is captured by the caller from the focused
     * application window's live root.packageName (SPEC §7.2); ime_visible = any
     * TYPE_INPUT_METHOD window present; windows[] = the full getWindows() merge. activity
     * stays "" (no clean unprivileged top-activity API; never null, SPEC §7.2).
     */
    private fun buildScreenContext(
        windowDescriptors: List<WindowDescriptor>,
        foregroundPackage: String,
    ): ScreenContext {
        // Read the display WITHOUT a visual context. An AccessibilityService is a
        // non-visual context, so WindowManager.getCurrentWindowMetrics() /
        // defaultDisplay and Context.getDisplay() throw "Tried to obtain display
        // from a Context not associated with one" on Android 11+ (API 30+) — which
        // aborted every `observe` here on modern devices. DisplayManager +
        // Display.getRealMetrics() is the context-free path to the real
        // (decor-inclusive) screen size, the coordinate space getBoundsInScreen
        // reports in. See [realDisplayMetrics] / [displayRotation].
        val metrics = realDisplayMetrics()
        val realBounds = Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        val rotation: Int = displayRotation()

        // ime_visible: an input-method window is present in the merge (SPEC §7.2).
        val imeVisible = windowDescriptors.any { it.type == "input_method" }

        return ScreenContext(
            width = realBounds.width(),
            height = realBounds.height(),
            density = metrics.density,
            rotation = rotation,
            foregroundPackage = foregroundPackage,
            // No clean unprivileged top-activity API → "" (never null, SPEC §7.2).
            activity = "",
            imeVisible = imeVisible,
            windows = windowDescriptors.map { d ->
                WindowInfo(
                    id = d.id,
                    type = d.type,
                    title = d.title,
                    focused = d.focused,
                )
            },
        )
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
     *
     * `suspend` because the IME branch below routes through
     * [com.aster.service.input.InputRouter], whose backends may await the
     * platform. The cascade terminates immediately: the only caller is
     * `AccessibilityHandler.globalAction`, which is reached solely from the
     * already-`suspend` `CommandHandler.handle`.
     */
    suspend fun performGlobalActionByName(actionName: String): Boolean {
        val upper = actionName.uppercase()

        // IME actions — dispatch to focused input field
        if (upper in listOf(ACTION_ENTER, ACTION_SEARCH, ACTION_DONE, ACTION_GO, ACTION_NEXT, ACTION_PREVIOUS)) {
            return performImeAction(upper)
        }

        val action = when (upper) {
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
     * Perform an IME action (Enter, Search, Done, Go, Next, Previous)
     * on the currently focused editable node.
     *
     * Routed through [inputRouter] — the same path `press_key` takes — so this
     * process has exactly ONE implementation of "deliver a key". The second
     * strategy this used to carry, `ProcessBuilder("input","keyevent",…)`, could
     * never succeed: `/system/bin/input` requires the signature-level
     * `INJECT_EVENTS`, which this app neither declares nor can obtain.
     *
     * **RECURSION INVARIANT.** The router's accessibility backend must reach the
     * IME action through [tryImeEnterAction], NEVER through this function. This
     * function delegates INTO the router, and [performGlobalActionByName]
     * delegates ENTER/SEARCH into this function, so a backend that called back
     * here would loop until the stack died. [tryImeEnterAction] is the extracted
     * primitive that exists precisely to break that cycle — it performs the one
     * `ACTION_IME_ENTER` attempt and returns.
     */
    private suspend fun performImeAction(actionName: String): Boolean {
        // SEARCH keeps its own keycode so a future backend can express the
        // distinction. Every other IME action is an ENTER at the wire level, and
        // ACTION_IME_ENTER already fires whichever action the focused field
        // advertises (search, go, done, next) — which is why all six names
        // resolved to the same accessibility attempt before this change too.
        val keyCode = if (actionName == ACTION_SEARCH) KeyEvent.KEYCODE_SEARCH else KeyEvent.KEYCODE_ENTER
        return inputRouter.pressKeyCode(keyCode).ok
    }

    /**
     * ONE attempt at `ACTION_IME_ENTER` on the focused editable node (API 30+).
     *
     * `internal` — not private — because the accessibility input backend lives in
     * [com.aster.service.input] and a class in another package cannot call a
     * private member. This is the precise widening that package needs: the
     * primitive, not the routing.
     *
     * It has NO fallback of its own, by design. Choosing a fallback is the
     * router's job, and a fallback here is exactly what would close the
     * recursion described on [performImeAction].
     */
    internal fun tryImeEnterAction(): FocusedNodeOutcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return FocusedNodeOutcome.UNSUPPORTED_API
        return performOnFocusedEditable(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
    }

    /**
     * Perform [actionId] on whichever editable node currently holds focus.
     *
     * `internal` for the same reason as [tryImeEnterAction]: it is what the input
     * backend needs to express CUT/COPY/PASTE as key presses. Node lifetime
     * (obtain/recycle) stays in this class, where every other node walk keeps it,
     * so the backend never handles a live `AccessibilityNodeInfo`.
     *
     * Never returns [FocusedNodeOutcome.UNSUPPORTED_API]: an API-level guard
     * belongs to the caller, which is the only side that knows which action id it
     * is asking for.
     */
    internal fun performOnFocusedEditable(actionId: Int): FocusedNodeOutcome {
        val rootNode = rootInActiveWindow ?: return FocusedNodeOutcome.NO_FOCUSED_FIELD
        try {
            val focusedNode = findFocusedEditableNode(rootNode) ?: return FocusedNodeOutcome.NO_FOCUSED_FIELD
            try {
                return if (focusedNode.performAction(actionId)) {
                    FocusedNodeOutcome.PERFORMED
                } else {
                    FocusedNodeOutcome.REFUSED
                }
            } finally {
                focusedNode.recycle()
            }
        } finally {
            rootNode.recycle()
        }
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

    /**
     * Pinch gesture (two simultaneous strokes). startGap/endGap in real px.
     * endGap > startGap → zoom in (diverge); endGap < startGap → zoom out (converge).
     */
    suspend fun pinchGesture(
        cx: Float, cy: Float, startGap: Float, endGap: Float, duration: Long = 300L
    ): Boolean {
        val (left, right) = pinchStrokePoints(cx, cy, startGap, endGap)
        val pathL = Path().apply { moveTo(left.startX, left.startY); lineTo(left.endX, left.endY) }
        val pathR = Path().apply { moveTo(right.startX, right.startY); lineTo(right.endX, right.endY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(pathL, 0, duration))
            .addStroke(GestureDescription.StrokeDescription(pathR, 0, duration))
            .build()
        return performGesture(gesture)
    }

    /**
     * Drag gesture (press-hold-move single stroke). Single stroke; the pointer lifts at the
     * end (a complete drag-and-drop press → move → release).
     */
    suspend fun dragGesture(
        startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 500L
    ): Boolean {
        val s = dragStrokePoints(startX, startY, endX, endY)
        val path = Path().apply { moveTo(s.startX, s.startY); lineTo(s.endX, s.endY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
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
    suspend fun clickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        try {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            if (nodes.isEmpty()) return false

            // Separate non-editable from editable nodes.
            // Editable nodes (input fields) contain the typed text — not the target.
            val targets = nodes.filter { !it.isEditable }
            val editables = nodes.filter { it.isEditable }

            // Strategy 1: Find a clickable non-editable node or clickable parent
            for (node in targets) {
                if (node.isClickable) {
                    val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    nodes.forEach { it.recycle() }
                    return result
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable && !parent.isEditable) {
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

            // Strategy 2: Try ACTION_CLICK on non-clickable nodes directly.
            // Some apps handle clicks at adapter level without setting isClickable.
            for (node in targets) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    nodes.forEach { it.recycle() }
                    return true
                }
            }

            // Strategy 3: Tap the center of the first non-editable node's bounds
            // using dispatchGesture — simulates an actual screen tap.
            for (node in targets) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                if (bounds.width() > 0 && bounds.height() > 0) {
                    val cx = bounds.centerX().toFloat()
                    val cy = bounds.centerY().toFloat()
                    nodes.forEach { it.recycle() }
                    val gesture = createTapGesture(cx, cy, 100)
                    return performGesture(gesture)
                }
            }

            // Strategy 4: Last resort — try editable nodes (original behavior)
            for (node in editables) {
                if (node.isClickable) {
                    val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    nodes.forEach { it.recycle() }
                    return result
                }
            }

            nodes.forEach { it.recycle() }
        } finally {
            rootNode.recycle()
        }

        return false
    }

    /**
     * In-memory screenshot result. [bitmap] is the (possibly downscaled) ARGB_8888 capture;
     * [realW]/[realH] are the ORIGINAL screen pixels; [scale] = screenshot_px / real_px
     * (1.0 when not downscaled). One capture feeds both OCR and annotation — never capture twice.
     * The caller owns [bitmap] and MUST recycle it.
     */
    data class ScreenshotCapture(
        val bitmap: Bitmap,
        val realW: Int,
        val realH: Int,
        val scale: Float
    )

    /**
     * Capture the screen once (Android 11+), downscaling to <=1280px wide, and return the bitmap
     * plus real dims + applied scale. Returns null if unsupported/failed. Caller recycles the bitmap.
     */
    suspend fun captureBitmap(): ScreenshotCapture? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Screenshot via Accessibility requires Android 11+")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val callback = object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    try {
                        val hwBitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace
                        )
                        if (hwBitmap == null) {
                            screenshot.hardwareBuffer.close()
                            if (continuation.isActive) continuation.resume(null)
                            return
                        }
                        var softwareBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        hwBitmap.recycle()
                        screenshot.hardwareBuffer.close()

                        val realW = softwareBitmap.width
                        val realH = softwareBitmap.height

                        // Downscale if wider than 1280px; capture the applied scale (do NOT discard).
                        val maxWidth = 1280
                        var scale = 1.0f
                        if (softwareBitmap.width > maxWidth) {
                            scale = maxWidth.toFloat() / softwareBitmap.width
                            val newHeight = (softwareBitmap.height * scale).toInt()
                            val scaled = Bitmap.createScaledBitmap(softwareBitmap, maxWidth, newHeight, true)
                            softwareBitmap.recycle()
                            softwareBitmap = scaled
                        }

                        if (continuation.isActive) {
                            continuation.resume(ScreenshotCapture(softwareBitmap, realW, realH, scale))
                        } else {
                            softwareBitmap.recycle()
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Screenshot capture failed", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Screenshot failed with error code: $errorCode")
                    if (continuation.isActive) continuation.resume(null)
                }
            }

            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                mainExecutor,
                callback
            )
        }
    }

    /** Result of writing a screenshot file: the file plus the real dims + scale (SPEC §3.1/§3.2). */
    data class ScreenshotFile(val file: File, val realW: Int, val realH: Int, val scale: Float)

    /**
     * Capture + (optionally annotate with Set-of-Marks) + write a JPEG. [marks] bounds are in REAL
     * screen pixels; they are multiplied by the screenshot `scale` to land on the downscaled JPEG.
     * Returns the file + real dims + scale, or null on failure.
     */
    suspend fun takeScreenshot(
        annotate: Boolean = false,
        marks: List<Mark> = emptyList()
    ): ScreenshotFile? {
        val capture = captureBitmap() ?: return null
        var bitmapToWrite = capture.bitmap
        try {
            if (annotate && marks.isNotEmpty()) {
                // Refs are real-px; multiply by scale to land on the (downscaled) screenshot.
                val scaledMarks = marks.map { m ->
                    Mark(
                        index = m.index,
                        bounds = Rect(
                            (m.bounds.left * capture.scale).toInt(),
                            (m.bounds.top * capture.scale).toInt(),
                            (m.bounds.right * capture.scale).toInt(),
                            (m.bounds.bottom * capture.scale).toInt()
                        )
                    )
                }
                val annotated = ScreenAnnotator.annotate(capture.bitmap, scaledMarks)
                capture.bitmap.recycle()
                bitmapToWrite = annotated
            }

            val mediaDir = getAsterMediaDir("screenshots")
            val file = File(mediaDir, "screenshot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmapToWrite.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            return ScreenshotFile(file, capture.realW, capture.realH, capture.scale)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Screenshot write failed", e)
            return null
        } finally {
            bitmapToWrite.recycle()
        }
    }

    /**
     * Run on-device OCR on a fresh capture and return blocks in REAL screen pixels.
     * Screenshot-px boxes are mapped back via 1/scale. Fail-closed: empty list on any failure.
     * Used by the P1 `observe` branch when the a11y tree is sparse (see ScreenObserveSupport).
     */
    suspend fun runOcr(): List<OcrBlock> {
        val capture = captureBitmap() ?: return emptyList()
        try {
            // Screenshot is already rotation-corrected by the platform capture; pass 0.
            val screenshotPxBlocks = ocrEngine.recognize(capture.bitmap, rotationDegrees = 0)
            if (capture.scale == 1.0f) return screenshotPxBlocks
            val inv = 1.0f / capture.scale
            return screenshotPxBlocks.map { b ->
                OcrBlock(
                    text = b.text,
                    bounds = Rect(
                        (b.bounds.left * inv).toInt(),
                        (b.bounds.top * inv).toInt(),
                        (b.bounds.right * inv).toInt(),
                        (b.bounds.bottom * inv).toInt()
                    ),
                    confidence = b.confidence
                )
            }
        } finally {
            capture.bitmap.recycle()
        }
    }

    private fun getAsterMediaDir(subDir: String): File {
        val dir = File(applicationContext.getExternalFilesDir(null), "aster_media/$subDir")
        if (!dir.exists()) dir.mkdirs()
        return dir
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

    // ------------------------------------------------------------------
    // P2 — ref-addressed actions
    // ------------------------------------------------------------------

    /**
     * The outcome of re-resolving a ref to a live node at action time.
     * The caller OWNS [node] and MUST recycle it after acting (ownership contract).
     * For a [ScreenActionResult.ResolvedBy.CENTER_TAP] result, [node] is null and the caller
     * acts on [bounds] (a raw center-tap of the cached bounds).
     */
    class ResolvedRef(
        val node: AccessibilityNodeInfo?,
        val resolvedBy: ScreenActionResult.ResolvedBy,
        val bounds: Rect
    )

    /**
     * Real screen metrics in true pixels + density (single source of truth for gesture dims).
     * Uses the real display bounds (matching the coordinate space `observe`/getBoundsInScreen
     * report in), NOT the app-usable area, so gesture coordinates and reported dims agree.
     */
    fun screenMetrics(): Triple<Int, Int, Float> {
        val m = realDisplayMetrics()
        return Triple(m.widthPixels, m.heightPixels, m.density)
    }

    /**
     * Real display metrics read WITHOUT a visual context.
     *
     * An AccessibilityService's context is non-visual, so `WindowManager`'s
     * `getCurrentWindowMetrics()` / `defaultDisplay` and `Context.getDisplay()`
     * throw `IllegalArgumentException: Tried to obtain display from a Context not
     * associated with one` on Android 11+ (API 30+). `DisplayManager.getDisplay()`
     * returns the default `Display` regardless of context, and the
     * deprecated-but-context-free `Display.getRealMetrics()` fills the true
     * (decor-inclusive) screen size — the same coordinate space
     * `observe`/`getBoundsInScreen` report in.
     */
    private fun realDisplayMetrics(): android.util.DisplayMetrics {
        val metrics = android.util.DisplayMetrics()
        val display = (getSystemService(DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
            .getDisplay(android.view.Display.DEFAULT_DISPLAY)
        if (display != null) {
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)
        } else {
            // Default display unexpectedly unavailable — fall back to the
            // always-populated, context-free resources metrics rather than
            // returning a degenerate 0×0 / density-0 object that downstream
            // coordinate math would treat as a valid zero-size screen.
            metrics.setTo(resources.displayMetrics)
        }
        return metrics
    }

    /** Default-display rotation, read without a visual context (see [realDisplayMetrics]). */
    private fun displayRotation(): Int =
        (getSystemService(DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
            .getDisplay(android.view.Display.DEFAULT_DISPLAY)
            ?.rotation ?: 0

    /**
     * Re-resolve a ref to a live node (SPEC §3.1), in priority order, VERIFYING the resolved
     * node still matches the cached descriptor before returning it (SPEC §5). Returns null if no
     * strategy yields a descriptor-matching node → caller emits a structured stale_ref.
     *
     * Strategies:
     *  1) findAccessibilityNodeInfosByViewId  → resolved_by "viewId"
     *  2) text + role match within the cached window → "text_role"
     *  3) nearest node to cached bounds center (BFS, min center-distance) → "nearest_bounds"
     *  4) raw center-tap of cached bounds (no node) → "center_tap"
     *
     * The returned node (1–3) is OWNED by the caller (recycle after acting).
     *
     * P1 seam: the cached descriptor comes from the service-held [snapshotCache]
     * (com.aster.service.accessibility.SnapshotCache.get(snapshotId, ref)).
     *
     * Latest-snapshot fallback: the kernel passes `snapshot_id` only optionally. When it is
     * null or empty, resolve against the most-recently-created snapshot
     * ([SnapshotCache.latestId]) so a ref-action that omits the id can still act. Only when
     * there is no snapshot at all (latestId == null) — or the ref is unknown in the chosen
     * snapshot — do we return null → stale_ref (fail closed). The descriptorMatches
     * verify-before-act gate below is untouched, so a genuinely stale/changed node still fails
     * closed.
     */
    fun resolveRef(
        ref: String,
        snapshotId: String?,
        allowCoordinateFallback: Boolean = false,
    ): ResolvedRef? {
        // P1 seam — re-read the cached POJO descriptor from the snapshot cache.
        // Fall back to the latest snapshot when no id was supplied; a missing snapshot
        // entirely (no observation yet) still yields null → stale_ref.
        val requested = snapshotId?.takeIf { it.isNotEmpty() }
        val effectiveSnapshotId = requested ?: snapshotCache.latestId() ?: return null
        val descriptor = snapshotCache.get(effectiveSnapshotId, ref) ?: return null
        val cachedBounds = Rect(
            descriptor.bounds.x,
            descriptor.bounds.y,
            descriptor.bounds.x + descriptor.bounds.w,
            descriptor.bounds.y + descriptor.bounds.h
        )

        val rootNode = rootInActiveWindow ?: run {
            // No tree at all. Only a caller that explicitly opted in may blind-tap the
            // cached rect; everyone else fails closed (see the CENTER_TAP note below).
            return if (allowCoordinateFallback &&
                cachedBounds.width() > 0 && cachedBounds.height() > 0
            ) {
                ResolvedRef(null, ScreenActionResult.ResolvedBy.CENTER_TAP, cachedBounds)
            } else {
                null
            }
        }

        try {
            // Strategy 1: viewId (mirrors clickByViewId).
            descriptor.viewId.takeIf { it.isNotEmpty() }?.let { viewId ->
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (nodes.isNotEmpty()) {
                    var matched: AccessibilityNodeInfo? = null
                    for (n in nodes) {
                        if (matched == null && nodeMatchesDescriptor(n, descriptor)) {
                            matched = n // keep; recycle the rest below
                        } else {
                            n.recycle()
                        }
                    }
                    if (matched != null) {
                        val b = Rect(); matched.getBoundsInScreen(b)
                        return ResolvedRef(matched, ScreenActionResult.ResolvedBy.VIEW_ID, b)
                    }
                }
            }

            // Strategy 2: text + role match within the cached window (mirrors clickByText).
            descriptor.text.takeIf { it.isNotEmpty() }?.let { text ->
                val nodes = rootNode.findAccessibilityNodeInfosByText(text)
                var matched: AccessibilityNodeInfo? = null
                for (n in nodes) {
                    // P1 stores windowId as a real Int (>= 0) or -1 sentinel; only constrain to
                    // the same window when a real id was captured.
                    val sameWindow = descriptor.windowId < 0 || n.windowId == descriptor.windowId
                    if (matched == null && sameWindow && nodeMatchesDescriptor(n, descriptor)) {
                        matched = n
                    } else {
                        n.recycle()
                    }
                }
                if (matched != null) {
                    val b = Rect(); matched.getBoundsInScreen(b)
                    return ResolvedRef(matched, ScreenActionResult.ResolvedBy.TEXT_ROLE, b)
                }
            }

            // Strategy 2b: the aggregated LABEL of an anonymous row.
            //
            // A list row owns no viewId and no text, so strategies 1 and 2 have
            // nothing to search on — yet the row is perfectly identifiable by the
            // title in its child TextView. Find that descendant by its text, then
            // climb to the nearest ancestor the descriptor gate accepts.
            //
            // This is the only strategy that survives SCROLLING: nearest-bounds
            // below assumes the row is still roughly where it was, which is false
            // the moment a feed moves. Replay scrolls constantly.
            if (descriptor.text.isEmpty()) {
                descriptor.label.takeIf { it.isNotEmpty() }?.let { label ->
                    val title = LabelAggregator.primary(label)
                    if (title.isNotEmpty()) {
                        val matched = findAncestorMatchingDescriptor(rootNode, title, descriptor)
                        if (matched != null) {
                            val b = Rect(); matched.getBoundsInScreen(b)
                            return ResolvedRef(matched, ScreenActionResult.ResolvedBy.TEXT_ROLE, b)
                        }
                    }
                }
            }

            // Strategy 3: nearest node to cached bounds center (BFS, min center-distance),
            // still gated by descriptorMatches so we never act on a wrong node.
            val nearest = findNearestMatchingNode(rootNode, descriptor, cachedBounds)
            if (nearest != null) {
                val b = Rect(); nearest.getBoundsInScreen(b)
                return ResolvedRef(nearest, ScreenActionResult.ResolvedBy.NEAREST_BOUNDS, b)
            }
        } finally {
            rootNode.recycle()
        }

        // Strategy 4: raw center-tap of cached bounds (no live node, no verify possible).
        //
        // OPT-IN ONLY. This branch is reached precisely when every verified strategy
        // has FAILED — the descriptor no longer matches anything on screen — so
        // taking it means acting blind on stale coordinates and, because `tapRef`
        // returns non-null, reporting `{ok:true, resolved_by:"center_tap"}`. That
        // made `tap`/`long_press` structurally incapable of returning `stale_ref`:
        // the fail-closed contract the rest of this file documents was dead code for
        // the two most common verbs, and a step whose target had genuinely moved
        // silently tapped whatever now occupies the old rectangle.
        //
        // Defaulting to false converts those into honest `stale_ref` failures, which
        // the kernel already knows how to escalate. A caller that truly wants a
        // coordinate gesture should ask for one (`screen_gesture`) or pass the flag.
        if (allowCoordinateFallback && cachedBounds.width() > 0 && cachedBounds.height() > 0) {
            return ResolvedRef(null, ScreenActionResult.ResolvedBy.CENTER_TAP, cachedBounds)
        }
        return null
    }

    /** Verify a live node against the cached descriptor via the pure predicate. */
    private fun nodeMatchesDescriptor(node: AccessibilityNodeInfo, d: NodeDescriptor): Boolean {
        return ScreenActionResult.descriptorMatches(
            cachedViewId = d.viewId, nodeViewId = node.viewIdResourceName,
            cachedText = d.text, nodeText = node.text?.toString(),
            cachedDesc = d.desc, nodeDesc = node.contentDescription?.toString(),
            cachedRole = d.role, nodeRole = roleOf(node),
            cachedClassName = d.className, nodeClassName = node.className?.toString(),
            cachedTestTag = d.testTag,
            nodeTestTag = runCatching {
                node.extras?.getString("androidx.compose.ui.semantics.testTag")
            }.getOrNull(),
            cachedLabel = d.label,
            // Lazy: the predicate only invokes this for a node that has already
            // agreed on id, text, desc and role, so the subtree walk runs a
            // handful of times per resolution rather than once per node.
            nodeLabel = { LabelAggregator.aggregate(node) }
        )
    }

    /**
     * Role derivation reusing P1's canonical [RoleMapper] so the verify-before-act role
     * comparison agrees with the role string cached in the descriptor (no parallel logic).
     */
    private fun roleOf(node: AccessibilityNodeInfo): String {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        return RoleMapper.roleOf(
            className = node.className?.toString() ?: "",
            isEditable = node.isEditable,
            isCheckable = node.isCheckable,
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            hasText = text.isNotEmpty() || desc.isNotEmpty(),
        )
    }

    /** BFS for the descriptor-matching node nearest the cached bounds center. */
    private fun findNearestMatchingNode(
        root: AccessibilityNodeInfo,
        descriptor: NodeDescriptor,
        cachedBounds: Rect
    ): AccessibilityNodeInfo? {
        val targetX = cachedBounds.centerX()
        val targetY = cachedBounds.centerY()
        var best: AccessibilityNodeInfo? = null
        var bestDist = Long.MAX_VALUE

        fun walk(node: AccessibilityNodeInfo) {
            if (nodeMatchesDescriptor(node, descriptor)) {
                val b = Rect(); node.getBoundsInScreen(b)
                val dx = (b.centerX() - targetX).toLong()
                val dy = (b.centerY() - targetY).toLong()
                val dist = dx * dx + dy * dy
                if (dist < bestDist) {
                    best?.recycle()
                    best = AccessibilityNodeInfo.obtain(node)
                    bestDist = dist
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try { walk(child) } finally { child.recycle() }
            }
        }
        walk(root)
        return best
    }

    /**
     * Find the actionable ancestor of a descendant that carries [title].
     *
     * The inverse of [LabelAggregator.aggregate]: aggregation walked DOWN from the
     * row to name it, this walks UP from the named child to find the row again.
     * Bounded by [LabelAggregator.LABEL_MAX_DEPTH] — the same depth aggregation was
     * allowed to descend — so it can never climb out to the window root and return
     * a container that merely happens to satisfy a vacuous descriptor.
     *
     * Every node obtained here is recycled except the one returned, which the
     * caller owns.
     */
    private fun findAncestorMatchingDescriptor(
        root: AccessibilityNodeInfo,
        title: String,
        descriptor: NodeDescriptor,
    ): AccessibilityNodeInfo? {
        val hits = root.findAccessibilityNodeInfosByText(title)
        var result: AccessibilityNodeInfo? = null
        for (hit in hits) {
            if (result != null) { hit.recycle(); continue }
            val sameWindow = descriptor.windowId < 0 || hit.windowId == descriptor.windowId
            if (!sameWindow) { hit.recycle(); continue }
            var cursor: AccessibilityNodeInfo? = hit.parent
            var depth = 0
            while (cursor != null && depth < LabelAggregator.LABEL_MAX_DEPTH) {
                if (nodeMatchesDescriptor(cursor, descriptor)) {
                    result = AccessibilityNodeInfo.obtain(cursor)
                    cursor.recycle()
                    cursor = null
                    break
                }
                val parent = cursor.parent
                cursor.recycle()
                cursor = parent
                depth++
            }
            cursor?.recycle()
            hit.recycle()
        }
        return result
    }

    /**
     * Tap a node by ref. ACTION_CLICK if the resolved node is clickable; otherwise a gesture
     * tap on the bounds center (covers center_tap and non-clickable adapter rows).
     * Returns the strategy that resolved, or null → stale_ref.
     */
    suspend fun tapRef(
        ref: String,
        snapshotId: String?,
        allowCoordinateFallback: Boolean = false,
    ): ScreenActionResult.ResolvedBy? {
        val resolved = resolveRef(ref, snapshotId, allowCoordinateFallback) ?: return null
        val node = resolved.node
        try {
            if (node != null && node.isClickable &&
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return resolved.resolvedBy
            }
            // Fallback: gesture tap on bounds center.
            val ok = performGesture(
                createTapGesture(resolved.bounds.centerX().toFloat(), resolved.bounds.centerY().toFloat(), 100)
            )
            return if (ok) resolved.resolvedBy else null
        } finally {
            node?.recycle()
        }
    }

    /**
     * Long-press a node by ref. ACTION_LONG_CLICK if supported; otherwise a long gesture on
     * the bounds center (duration coerced to ≥ 500ms).
     */
    suspend fun longPressRef(
        ref: String,
        snapshotId: String?,
        duration: Long,
        allowCoordinateFallback: Boolean = false,
    ): ScreenActionResult.ResolvedBy? {
        val resolved = resolveRef(ref, snapshotId, allowCoordinateFallback) ?: return null
        val node = resolved.node
        val dur = duration.coerceAtLeast(500L)
        try {
            if (node != null && node.isLongClickable &&
                node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                return resolved.resolvedBy
            }
            val ok = performGesture(
                createTapGesture(resolved.bounds.centerX().toFloat(), resolved.bounds.centerY().toFloat(), dur)
            )
            return if (ok) resolved.resolvedBy else null
        } finally {
            node?.recycle()
        }
    }

    /**
     * Set text on a node by ref — NOT limited to the focused field. Focus the node first
     * (if focusable), then ACTION_SET_TEXT (replace) or current+new (append, mirrors appendText).
     * If submit, reuse the IME-enter path (performImeAction("ENTER")).
     * @param mode "replace" (default) | "append"
     */
    suspend fun setTextRef(
        ref: String,
        snapshotId: String?,
        text: String,
        mode: String,
        submit: Boolean
    ): ScreenActionResult.ResolvedBy? {
        val resolved = resolveRef(ref, snapshotId) ?: return null
        val node = resolved.node ?: return null // center_tap cannot set text → treat as unresolved
        try {
            if (node.isFocusable) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            }
            val newText = if (mode.equals("append", ignoreCase = true)) {
                (node.text?.toString() ?: "") + text
            } else {
                text
            }
            val args = android.os.Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            val set = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (!set) return null
            if (submit) {
                performImeAction(ACTION_ENTER)
            }
            return resolved.resolvedBy
        } finally {
            node.recycle()
        }
    }

    /**
     * Set a switch/checkbox to [on]. Read node.isChecked, click ONLY if it differs (idempotent).
     * Returns Pair(resolvedBy, alreadyInState).
     */
    suspend fun setToggleRef(
        ref: String,
        snapshotId: String?,
        on: Boolean
    ): Pair<ScreenActionResult.ResolvedBy, Boolean>? {
        val resolved = resolveRef(ref, snapshotId) ?: return null
        val node = resolved.node ?: return null
        try {
            if (node.isChecked == on) {
                return resolved.resolvedBy to true // already in desired state, no click
            }
            val clicked = if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                true
            } else {
                performGesture(
                    createTapGesture(resolved.bounds.centerX().toFloat(), resolved.bounds.centerY().toFloat(), 100)
                )
            }
            return if (clicked) resolved.resolvedBy to false else null
        } finally {
            node.recycle()
        }
    }

    /**
     * Invoke any advertised a11y action on a node by ref (SPEC §3.2 perform).
     * Accepts the SAME action-name strings that observe emits in elements[].actions
     * (round-trip consistency, e.g. "click","long_click","expand","collapse","dismiss",
     * "scroll_forward","scroll_backward","select","focus","clear_focus","copy","paste","cut").
     */
    suspend fun performActionOnRef(
        ref: String,
        snapshotId: String?,
        actionName: String
    ): ScreenActionResult.ResolvedBy? {
        val resolved = resolveRef(ref, snapshotId) ?: return null
        val node = resolved.node ?: return null
        val actionId = actionNameToId(actionName) ?: return null
        try {
            return if (node.performAction(actionId)) resolved.resolvedBy else null
        } finally {
            node.recycle()
        }
    }

    /** Map an observe-emitted action name → AccessibilityNodeInfo action id. null = unknown. */
    private fun actionNameToId(name: String): Int? = when (name.lowercase()) {
        "click" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id
        "long_click", "long_press" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK.id
        "expand" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND.id
        "collapse" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE.id
        "dismiss" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS.id
        "scroll_forward" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id
        "scroll_backward" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id
        "select" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SELECT.id
        "clear_selection" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_SELECTION.id
        "focus" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_FOCUS.id
        "clear_focus" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_FOCUS.id
        "copy" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY.id
        "paste" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE.id
        "cut" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT.id
        else -> null
    }

    /**
     * Press a key by name, through [inputRouter] (SPEC §3.2: press_key uses "the path
     * IME-enter already uses" — which, after this change, is a path that can actually
     * succeed).
     *
     * This used to shell out to `input keyevent`, which could never work from this
     * process: `/system/bin/input` requires `INJECT_EVENTS`, a signature-level
     * permission held by `shell`/`system` that `com.aster` does not declare and cannot
     * be granted. Every call failed, and the handler reported it as "unknown key" —
     * blaming the caller's key name for a permission wall.
     *
     * Returns the router's [InputResult] rather than a Boolean so the caller can report
     * WHICH mechanism served the press and, on failure, the real reason: a key no
     * backend can express reads differently from one a backend owned and could not
     * deliver. No fixed sleeps — UI settle is P3's `wait_for_idle`.
     */
    suspend fun pressKey(key: String): InputResult {
        val resolved = keyNameToKeycode(key)
            ?: return InputResult(ok = false, backend = null, reason = "unknown key name '$key'")
        // keyNameToKeycode passes a bare numeric string through unchecked, so a caller
        // can hand us something that is digits but not an Int. Say so instead of
        // reporting it as a routing failure.
        val keyCode = resolved.toIntOrNull()
            ?: return InputResult(ok = false, backend = null, reason = "'$key' is not a valid Android keycode")
        return inputRouter.pressKeyCode(keyCode)
    }

    /**
     * Ref-addressed scroll (SPEC §3.2). If [ref] is null, auto-pick via findScrollableNode.
     * Vertical up/down → ACTION_SCROLL_BACKWARD/FORWARD. Horizontal or amount=halfpage/toEdge →
     * a swipe gesture across the scrollable's bounds (a11y scroll is direction-agnostic).
     * [untilText] = scroll-to-find: repeat until the text appears OR a scroll makes no progress
     * (progress = signature of the scrollable's child bounds; NO fixed iteration sleeps — project
     * policy bans timer band-aids).
     *
     * The search is BOUNDED three ways — a deadline, an iteration cap, and cooperative
     * cancellation. The no-progress signature alone is not a termination condition: on an
     * endlessly paginating feed (LinkedIn, X, YouTube — three of the preloaded flows target
     * exactly these) the signature changes on every scroll, so the loop had no exit at all.
     * It ran on a blocked Binder thread, so a runaway search also held a binder slot from a
     * pool this process shares with system_server's accessibility-event delivery — degrading
     * the very screen-control capability it depends on.
     *
     * @param timeoutMs absolute wall-clock budget for the whole scroll-to-find.
     * @return Pair(scrolled:Boolean, foundUntilText:Boolean?). foundUntilText is null when no
     *         untilText was requested.
     */
    suspend fun scrollRef(
        ref: String?,
        snapshotId: String?,
        direction: String,
        amount: String?,
        untilText: String?,
        timeoutMs: Long = SCROLL_FIND_TIMEOUT_MS,
    ): Pair<Boolean, Boolean?> {
        val axis = normalizeScrollDirection(direction) ?: return false to null
        val amt = normalizeScrollAmount(amount)

        // No untilText: a single scroll step.
        if (untilText == null) {
            val ok = scrollOnce(ref, snapshotId, axis, amt)
            return ok to null
        }

        // Scroll-to-find: loop until found, until no progress, or until a bound trips.
        if (treeContainsText(untilText)) return false to true
        val deadline = SystemClock.elapsedRealtime() +
            timeoutMs.coerceIn(SCROLL_FIND_MIN_TIMEOUT_MS, SCROLL_FIND_MAX_TIMEOUT_MS)
        var lastSignature = scrollableSignature(ref, snapshotId)
        var scrolls = 0
        while (true) {
            // Unwind promptly when the caller's IPC call is cancelled or times out.
            coroutineContext.ensureActive()
            if (scrolls >= MAX_SCROLL_FIND_STEPS) return true to false
            if (SystemClock.elapsedRealtime() >= deadline) return true to false
            val scrolled = scrollOnce(ref, snapshotId, axis, amt)
            if (!scrolled) return false to false
            scrolls++
            if (treeContainsText(untilText)) return true to true
            val sig = scrollableSignature(ref, snapshotId)
            if (sig == lastSignature) return true to false // exhausted: no movement
            lastSignature = sig
        }
    }

    /** One scroll step. a11y action for vertical page; swipe gesture otherwise. */
    private suspend fun scrollOnce(
        ref: String?,
        snapshotId: String?,
        axis: ScrollAxis,
        amount: ScrollAmount
    ): Boolean {
        val target = obtainScrollableNode(ref, snapshotId) ?: return false
        try {
            val bounds = Rect(); target.getBoundsInScreen(bounds)
            val verticalPage = (axis == ScrollAxis.VERTICAL_FORWARD || axis == ScrollAxis.VERTICAL_BACKWARD) &&
                amount == ScrollAmount.PAGE
            if (verticalPage) {
                val action = if (axis == ScrollAxis.VERTICAL_BACKWARD)
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                if (target.performAction(action)) return true
                // fall through to gesture if the node refused the action
            }
            return performGesture(scrollSwipeGesture(bounds, axis, amount))
        } finally {
            target.recycle()
        }
    }

    /** Resolve the scrollable node: a ref (if given) else the first scrollable in the tree. */
    private fun obtainScrollableNode(ref: String?, snapshotId: String?): AccessibilityNodeInfo? {
        if (ref != null) {
            val resolved = resolveRef(ref, snapshotId) ?: return null
            return resolved.node // may be null for center_tap — caller treats null as no-scroll
        }
        val root = rootInActiveWindow ?: return null
        try {
            return findScrollableNode(root)
        } finally {
            root.recycle()
        }
    }

    /** Build a swipe gesture across [bounds] for the given axis+amount. */
    private fun scrollSwipeGesture(bounds: Rect, axis: ScrollAxis, amount: ScrollAmount): GestureDescription {
        val fraction = when (amount) {
            ScrollAmount.HALF_PAGE -> 0.25f
            ScrollAmount.PAGE -> 0.4f
            ScrollAmount.TO_EDGE -> 0.45f
        }
        val cx = bounds.centerX().toFloat()
        val cy = bounds.centerY().toFloat()
        // To scroll content FORWARD (down/right), swipe the finger BACKWARD (up/left).
        return when (axis) {
            ScrollAxis.VERTICAL_FORWARD -> {
                val dy = bounds.height() * fraction
                createSwipeGesture(cx, cy + dy, cx, cy - dy, 300L)
            }
            ScrollAxis.VERTICAL_BACKWARD -> {
                val dy = bounds.height() * fraction
                createSwipeGesture(cx, cy - dy, cx, cy + dy, 300L)
            }
            ScrollAxis.HORIZONTAL_FORWARD -> {
                val dx = bounds.width() * fraction
                createSwipeGesture(cx + dx, cy, cx - dx, cy, 300L)
            }
            ScrollAxis.HORIZONTAL_BACKWARD -> {
                val dx = bounds.width() * fraction
                createSwipeGesture(cx - dx, cy, cx + dx, cy, 300L)
            }
        }
    }

    /** Does the active tree currently contain [text]? (event-free poll, for scroll-to-find). */
    private fun treeContainsText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        try {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            val found = nodes.isNotEmpty()
            nodes.forEach { it.recycle() }
            return found
        } finally {
            root.recycle()
        }
    }

    /**
     * Cheap progress signature of the scrollable's children bounds — used to detect "exhausted"
     * (no movement after a scroll) WITHOUT a fixed iteration count or sleep.
     */
    private fun scrollableSignature(ref: String?, snapshotId: String?): Int {
        val node = obtainScrollableNode(ref, snapshotId) ?: return 0
        try {
            var sig = 1
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val b = Rect(); child.getBoundsInScreen(b)
                    sig = sig * 31 + b.top
                    sig = sig * 31 + b.left
                    sig = sig * 31 + (child.text?.toString()?.hashCode() ?: 0)
                } finally {
                    child.recycle()
                }
            }
            return sig
        } finally {
            node.recycle()
        }
    }
}

/**
 * Result of a node action aimed at the currently focused editable field
 * ([AsterAccessibilityService.performOnFocusedEditable] and
 * [AsterAccessibilityService.tryImeEnterAction]).
 *
 * Four states, because the caller must be able to tell "not expressible here, and
 * nothing was attempted" ([UNSUPPORTED_API], [NO_FOCUSED_FIELD]) from "attempted,
 * and the node said no" ([REFUSED]). That distinction is what decides whether
 * another input backend may safely try the same press — collapsing it to a
 * Boolean is how a permission wall came to be reported as an unknown key name.
 */
enum class FocusedNodeOutcome {
    /** The action id does not exist on this Android version. Nothing attempted. */
    UNSUPPORTED_API,

    /** No editable node holds focus, so there was no target. Nothing attempted. */
    NO_FOCUSED_FIELD,

    /** The focused node performed the action. */
    PERFORMED,

    /** The focused node was asked and declined — it does not advertise the action. */
    REFUSED,
}

// ----------------------------------------------------------------------
// P2 — top-level pure helpers (testable without the Android runtime)
// ----------------------------------------------------------------------

/**
 * Map a key name → Android keycode string. Case-insensitive; a bare numeric string
 * passes through. null = unknown (caller fails honestly, no guess).
 * Values are stable Android KeyEvent constants.
 *
 * Name resolution ONLY. Whether a resolved keycode can actually be delivered is the
 * input router's question, not this table's — a name here is not a promise that the
 * press will work, and [AsterAccessibilityService.pressKey] reports the difference.
 */
fun keyNameToKeycode(key: String): String? {
    val k = key.trim()
    if (k.isNotEmpty() && k.all { it.isDigit() }) return k
    return when (k.uppercase()) {
        "ENTER" -> "66"
        "BACK" -> "4"
        "DEL", "BACKSPACE" -> "67"            // KEYCODE_DEL
        "FORWARD_DEL", "DELETE" -> "112"      // KEYCODE_FORWARD_DEL
        "DPAD_UP", "UP" -> "19"
        "DPAD_DOWN", "DOWN" -> "20"
        "DPAD_LEFT", "LEFT" -> "21"
        "DPAD_RIGHT", "RIGHT" -> "22"
        "DPAD_CENTER", "CENTER" -> "23"
        "TAB" -> "61"
        "SPACE" -> "62"
        "SEARCH" -> "84"
        "PASTE" -> "279"
        "COPY" -> "278"
        "CUT" -> "277"
        "HOME" -> "3"
        "APP_SWITCH", "RECENTS" -> "187"
        "ESCAPE", "ESC" -> "111"
        "MENU" -> "82"
        "PAGE_UP" -> "92"
        "PAGE_DOWN" -> "93"
        "MOVE_HOME" -> "122"
        "MOVE_END" -> "123"
        else -> null
    }
}

/** Resolved scroll axis+direction for scrollRef (pure, testable). */
enum class ScrollAxis { VERTICAL_FORWARD, VERTICAL_BACKWARD, HORIZONTAL_FORWARD, HORIZONTAL_BACKWARD }

/** Scroll magnitude bucket (SPEC §3.2 amount: page|halfpage|toEdge). */
enum class ScrollAmount { PAGE, HALF_PAGE, TO_EDGE }

/** Normalize a direction string (up|down|left|right + legacy forward/backward) → axis. null = unknown. */
fun normalizeScrollDirection(direction: String): ScrollAxis? = when (direction.lowercase()) {
    "up", "backward" -> ScrollAxis.VERTICAL_BACKWARD
    "down", "forward" -> ScrollAxis.VERTICAL_FORWARD
    "left" -> ScrollAxis.HORIZONTAL_BACKWARD
    "right" -> ScrollAxis.HORIZONTAL_FORWARD
    else -> null
}

/** Normalize an amount string → bucket. Defaults to PAGE for null/unknown. */
fun normalizeScrollAmount(amount: String?): ScrollAmount = when (amount?.lowercase()) {
    "halfpage" -> ScrollAmount.HALF_PAGE
    "toedge" -> ScrollAmount.TO_EDGE
    else -> ScrollAmount.PAGE
}

/** A single stroke's endpoints (pure, no GestureDescription dependency). */
data class StrokePts(val startX: Float, val startY: Float, val endX: Float, val endY: Float)

/**
 * Two horizontal strokes for a pinch centred at (cx,cy). startGap/endGap = full distance
 * between the two fingers at start/end. endGap > startGap → diverge (zoom in);
 * endGap < startGap → converge (zoom out). Pure → unit-testable without GestureDescription.
 */
fun pinchStrokePoints(cx: Float, cy: Float, startGap: Float, endGap: Float): Pair<StrokePts, StrokePts> {
    val s = startGap / 2f
    val e = endGap / 2f
    val left = StrokePts(cx - s, cy, cx - e, cy)
    val right = StrokePts(cx + s, cy, cx + e, cy)
    return left to right
}

/** A drag is a single stroke from start to end. Pure. */
fun dragStrokePoints(startX: Float, startY: Float, endX: Float, endY: Float): StrokePts =
    StrokePts(startX, startY, endX, endY)
