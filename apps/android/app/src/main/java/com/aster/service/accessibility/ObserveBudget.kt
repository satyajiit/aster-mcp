package com.aster.service.accessibility

/**
 * Two-bucket element budget for the multi-window `observe` walk (App Automations
 * /goal I3, SPEC §I3).
 *
 * The screen is walked window-by-window into one accumulating [ScreenObserver].
 * Without a split budget, a huge focused application window would exhaust the cap
 * and starve every later window — so the bottom-navigation/system window could
 * emit ZERO elements and the model would never see the "Post" tab.
 *
 * This splits the budget into two INDEPENDENT buckets:
 *   - [appCap]    — shared by all application windows;
 *   - [systemCap] — shared by all non-application windows (system / navigation /
 *                   decor / input_method / accessibility_overlay).
 *
 * A full application bucket can never gate a non-application window (and vice
 * versa) — the bottom-nav window always keeps its reserved slots. The buckets
 * only meter the budget; the `e<N>` ref namespace stays single in
 * [ScreenObserver] (one shared refCounter), so reordering windows never produces
 * colliding refs (SPEC §7.1).
 *
 * Pure (no Android types) so it is directly unit-testable.
 */
class ObserveBudget(
    private val appCap: Int,
    private val systemCap: Int,
) {
    private var appCount = 0
    private var systemCount = 0

    /** True once the bucket for this window-type has been exhausted. */
    fun isFull(isApplication: Boolean): Boolean =
        if (isApplication) appCount >= appCap else systemCount >= systemCap

    /**
     * Charge one kept element to its window-type bucket. Returns true if it was
     * accepted (room remained), false if the bucket was already full (the caller
     * should then mark the result truncated and stop this window's walk).
     */
    fun take(isApplication: Boolean): Boolean {
        if (isFull(isApplication)) return false
        if (isApplication) appCount++ else systemCount++
        return true
    }
}
