package com.aster.service.input

/**
 * Which mechanism actually delivered a key press.
 *
 * An enum rather than a boolean because a second mechanism has to be ADDITIVE:
 * adding a constant here plus one class implementing [InputBackend] is the whole
 * change, and every caller that reports *which* backend served a press keeps
 * compiling. It is also what makes the report honest — "this worked" and "this
 * worked, via X" are different claims, and only the second one lets the owner
 * tell a genuine capability from a lucky path.
 */
enum class InputBackendKind {
    /**
     * The accessibility API this app already holds (`BIND_ACCESSIBILITY_SERVICE`):
     * global actions, `ACTION_IME_ENTER`, and node actions on the focused
     * editable field. It needs no extra permission and no second app, so it is
     * always first in the router's order.
     */
    ACCESSIBILITY,
}

/**
 * What one backend has to say about one key press.
 *
 * The three states are load-bearing, and the split between the two failure
 * states is the entire reason this type exists rather than a `Boolean`:
 *
 *  - [Ok] — the press was delivered.
 *  - [Declined] — "I structurally cannot express this keycode." The backend did
 *    **not act at all**; nothing was attempted, so nothing was half-done. This
 *    is the ONLY outcome the router may fall through on, because falling
 *    through means letting another backend attempt the same press — which is
 *    safe exactly when the first backend is known not to have touched anything.
 *  - [Failed] — "this press is mine and it did not work." The backend owns the
 *    keycode and reached for it, so it may have partially acted (focused a node,
 *    moved a selection), and its reason is a real diagnosis rather than a
 *    coverage gap. The router MUST stop here and surface [Failed.why]. Retrying
 *    a press a backend already attempted risks delivering it twice, and
 *    laundering it into a decline would bury the real error under a misleading
 *    "nothing could do this".
 *
 * Collapsing the two failures into `false` is precisely the defect this
 * replaces: `press_key` used to return a bare boolean and the handler had to
 * guess at the cause ("unknown key or input keyevent failed"), which named
 * neither of the two things that could actually have gone wrong.
 */
sealed interface BackendOutcome {

    /** Delivered. */
    object Ok : BackendOutcome

    /**
     * Not expressible by this backend; NOTHING was attempted, so another
     * backend may safely try the same press.
     *
     * @property why names the keycode and what this backend does reach, so a
     *   caller can tell a coverage gap from a breakage without reading source.
     */
    data class Declined(val why: String) : BackendOutcome

    /**
     * Attempted by this backend and it did not work. Terminal — the router does
     * NOT fall through, because the press may already have partially landed.
     *
     * @property why the real diagnosis, reported verbatim to the caller.
     */
    data class Failed(val why: String) : BackendOutcome
}

/**
 * One mechanism for delivering a key press to the device.
 *
 * Implementations answer for a *subset* of keycodes and must say so honestly:
 * anything outside that subset is a [BackendOutcome.Declined], never a
 * [BackendOutcome.Failed] and never a silent `false`.
 */
interface InputBackend {

    /** Which mechanism this is; carried into the result so callers can report it. */
    val kind: InputBackendKind

    /**
     * Whether this backend can be reached at all right now (service bound,
     * permission held, companion installed…). A `false` here is a property of
     * the *device state*, not of any particular keycode — the router skips the
     * backend entirely and moves on, exactly as it would for a decline.
     *
     * Cheap and side-effect free: the router calls it before every press.
     */
    fun isAvailable(): Boolean

    /**
     * Attempt to deliver [keyCode] (an `android.view.KeyEvent.KEYCODE_*` value).
     *
     * `suspend` because a real delivery can await the platform — a gesture
     * dispatch callback, a bound companion's IPC round trip — and blocking a
     * Binder thread for that is how this process starves the very accessibility
     * pipeline it depends on.
     */
    suspend fun pressKeyCode(keyCode: Int): BackendOutcome
}
