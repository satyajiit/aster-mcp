package com.aster.service.handlers

import com.aster.service.mode.IpcMode
import com.aster.service.safety.PackagePolicyGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The action-name surface of [AccessibilityHandler], pinned as an exact SET.
 *
 * Why a set of literals and not a count: `supportedActions()` is what
 * `ModeModule.provideCommandHandlers` keys the handler map on, and every entry it
 * produces is wrapped in `GuardedCommandHandler`. So a name is simultaneously the
 * dispatch key AND the token `PackagePolicyGuard` gates on. Adding one silently
 * widens the guarded surface; removing one silently un-registers a verb. A count
 * catches neither — swap two names and it never moves.
 *
 * Making `press_key` honest deliberately added NO action name. That is what keeps
 * this list, the guard's gated set, and the kill switch's set aligned, and these
 * assertions are what say so out loud.
 */
class AccessibilityHandlerActionsTest {

    /**
     * The complete, intended surface. Eighteen names. Changing this list is a
     * deliberate act with three consequences (dispatch, guard, kill switch), so it
     * should require editing this literal too.
     */
    private val expected = setOf(
        "observe",
        "get_screen_hierarchy",
        "input_gesture",
        "global_action",
        "input_text",
        "take_screenshot",
        "find_element",
        "click_by_text",
        "click_by_view_id",
        "scroll",
        // ref-addressed actions (SPEC §3.2)
        "tap",
        "set_text",
        "long_press",
        "set_toggle",
        "perform",
        "press_key",
        // synchronization (SPEC §3.3)
        "wait_for_idle",
        "wait_for",
    )

    @Test
    fun `supportedActions is exactly the pinned set`() {
        assertEquals(expected, AccessibilityHandler().supportedActions().toSet())
    }

    @Test
    fun `supportedActions has no duplicate names`() {
        // A duplicate would be invisible in the map (last write wins) but would
        // make the count lie about the surface.
        val actions = AccessibilityHandler().supportedActions()
        assertEquals(actions.size, actions.toSet().size)
    }

    // ---- press_key stays inside both safety rails ----

    @Test
    fun `press_key is gated by the package policy guard`() {
        // Without this, a key press could drive a banking app that `tap` is
        // refused on — the guard's whole reason for existing.
        assertTrue("press_key" in PackagePolicyGuard.GATED_ACTIONS)
    }

    @Test
    fun `press_key is fast-rejected by the kill switch`() {
        // IpcMode aborts an in-flight control loop within one action, but only for
        // names it recognises as screen control.
        assertTrue("press_key" in IpcMode.SCREEN_CONTROL_ACTIONS)
    }

    @Test
    fun `every gated accessibility verb is also a kill-switch verb`() {
        // The two sets are independent lists of the same idea. Any accessibility
        // action the guard gates must also be abortable, or STOP would leave it
        // running. (The kill-switch set is the wider one: it additionally covers
        // the companion's own overlay verbs, which the guard deliberately excludes.)
        val gatedHere = AccessibilityHandler().supportedActions()
            .filter { it in PackagePolicyGuard.GATED_ACTIONS }
        assertTrue("expected the guard to gate several accessibility verbs", gatedHere.isNotEmpty())
        val notAbortable = gatedHere.filterNot { it in IpcMode.SCREEN_CONTROL_ACTIONS }
        assertEquals(emptyList<String>(), notAbortable)
    }
}
