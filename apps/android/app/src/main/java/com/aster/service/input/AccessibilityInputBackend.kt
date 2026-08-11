package com.aster.service.input

import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aster.service.AsterAccessibilityService
import com.aster.service.FocusedNodeOutcome

/**
 * Key presses expressed through the accessibility API — and ONLY those the API
 * genuinely expresses.
 *
 * This exists because the path it replaces could never work: `press_key` and the
 * IME-enter fallback both shelled out to `/system/bin/input keyevent`, which
 * requires `INJECT_EVENTS` — a signature-level permission held by `shell` and
 * `system`. This app neither declares it nor can obtain it, so every one of
 * those presses failed, and reported the failure as "unknown key or input
 * keyevent failed" — a message that blamed the caller's key name for a
 * permission wall.
 *
 * What the accessibility API actually reaches, and nothing more:
 *  - **BACK / HOME / APP_SWITCH** — `performGlobalAction`, via the service's
 *    existing name mapping.
 *  - **ENTER / SEARCH** — `ACTION_IME_ENTER` on the focused editable field
 *    (API 30+). This is *better* than a raw ENTER keycode: it fires whichever
 *    action the field advertises (search, go, done, next).
 *  - **CUT / COPY / PASTE** — the corresponding node actions on the focused
 *    editable field. The same actions the ref-addressed `perform` verb already
 *    serves; here they are addressed by focus instead of by ref, which is the
 *    only sensible reading of a bare keycode press.
 *
 * Every other keycode is [BackendOutcome.Declined] naming the keycode. There is
 * no accessibility expression for a TAB, a DEL, or a DPAD press that does not
 * amount to guessing, and guessing is what produced the misleading error above.
 */
class AccessibilityInputBackend : InputBackend {

    override val kind: InputBackendKind = InputBackendKind.ACCESSIBILITY

    /**
     * The accessibility service is bound. It is the sole route to every action
     * below, so its absence disables the whole backend rather than any one key.
     */
    override fun isAvailable(): Boolean = AsterAccessibilityService.getInstance() != null

    override suspend fun pressKeyCode(keyCode: Int): BackendOutcome {
        // Classify FIRST, before touching the service. The question "can this
        // mechanism express this keycode?" is structural — it has the same answer
        // whether or not the service happens to be bound right now — and
        // answering it first is what lets the decline name the keycode instead of
        // reporting an unrelated "service not connected".
        val route = routeFor(keyCode)
            ?: return BackendOutcome.Declined(
                "keycode $keyCode is not expressible through the accessibility API, which reaches " +
                    "only BACK/HOME/APP_SWITCH, the focused field's IME action (ENTER/SEARCH), " +
                    "and CUT/COPY/PASTE on a focused editable field"
            )

        // Declined, not Failed: an unbound service is a property of the device,
        // not of this press. Nothing was attempted, so another backend may try.
        val service = AsterAccessibilityService.getInstance()
            ?: return BackendOutcome.Declined(
                "the accessibility service is not connected (enable it in " +
                    "Settings > Accessibility > Aster by OpenAlly)"
            )

        return when (route) {
            // RECURSION INVARIANT (1 of 2): these three names are NOT IME action
            // names, so `performGlobalActionByName` takes its `performGlobalAction`
            // branch and never reaches `performImeAction` — which is the function
            // that delegates back into this router. The IME route below is the one
            // that would close that cycle, and it deliberately calls the extracted
            // primitive instead. See `AsterAccessibilityService.performImeAction`.
            AccessibilityKeyRoute.GLOBAL_BACK ->
                globalOutcome(service.performGlobalActionByName(AsterAccessibilityService.ACTION_BACK), "BACK")

            AccessibilityKeyRoute.GLOBAL_HOME ->
                globalOutcome(service.performGlobalActionByName(AsterAccessibilityService.ACTION_HOME), "HOME")

            AccessibilityKeyRoute.GLOBAL_RECENTS ->
                globalOutcome(service.performGlobalActionByName(AsterAccessibilityService.ACTION_RECENTS), "APP_SWITCH")

            // RECURSION INVARIANT (2 of 2): `tryImeEnterAction`, NEVER
            // `performImeAction`. The latter routes INTO this router, so calling it
            // from here would loop until the stack died. The primitive performs the
            // one ACTION_IME_ENTER attempt and returns.
            AccessibilityKeyRoute.IME_ENTER -> when (service.tryImeEnterAction()) {
                FocusedNodeOutcome.UNSUPPORTED_API -> BackendOutcome.Declined(
                    "ACTION_IME_ENTER needs Android 11 (API 30); this device is older"
                )
                FocusedNodeOutcome.NO_FOCUSED_FIELD -> BackendOutcome.Declined(
                    "no editable field has focus, so there is no IME action to fire"
                )
                FocusedNodeOutcome.PERFORMED -> BackendOutcome.Ok
                FocusedNodeOutcome.REFUSED -> BackendOutcome.Failed(
                    "the focused field refused ACTION_IME_ENTER"
                )
            }

            // Same action ids the ref-addressed `perform` verb already maps to
            // ("copy"/"paste"/"cut" in AsterAccessibilityService.actionNameToId);
            // only the addressing differs — focus here, ref there.
            AccessibilityKeyRoute.NODE_COPY ->
                nodeOutcome(service, "copy", AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY.id)

            AccessibilityKeyRoute.NODE_PASTE ->
                nodeOutcome(service, "paste", AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE.id)

            AccessibilityKeyRoute.NODE_CUT ->
                nodeOutcome(service, "cut", AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT.id)
        }
    }

    /**
     * A global action is wholly owned by this backend: the system either performs
     * it or refuses it, and no other mechanism available to this app would fare
     * better. So a refusal is [BackendOutcome.Failed], never a decline.
     */
    private fun globalOutcome(performed: Boolean, name: String): BackendOutcome =
        if (performed) {
            BackendOutcome.Ok
        } else {
            BackendOutcome.Failed("the system refused the global action $name")
        }

    /**
     * Clipboard keys act on whatever editable field currently holds focus.
     *
     * "No focused field" is a decline (nothing was attempted); a field that
     * refuses the action — typically because it advertises no selection to
     * copy — is a failure, because this backend did reach for it.
     */
    private fun nodeOutcome(
        service: AsterAccessibilityService,
        verb: String,
        actionId: Int
    ): BackendOutcome = when (service.performOnFocusedEditable(actionId)) {
        FocusedNodeOutcome.UNSUPPORTED_API -> BackendOutcome.Declined(
            "the $verb node action is unavailable on this Android version"
        )
        FocusedNodeOutcome.NO_FOCUSED_FIELD -> BackendOutcome.Declined(
            "no editable field has focus, so there is nothing to $verb"
        )
        FocusedNodeOutcome.PERFORMED -> BackendOutcome.Ok
        FocusedNodeOutcome.REFUSED -> BackendOutcome.Failed(
            "the focused field does not offer the $verb action (a selection may be required)"
        )
    }
}

/**
 * The accessibility expressions a keycode can map onto. Deliberately closed: a
 * keycode with no entry here has no honest accessibility equivalent.
 */
private enum class AccessibilityKeyRoute {
    GLOBAL_BACK,
    GLOBAL_HOME,
    GLOBAL_RECENTS,
    IME_ENTER,
    NODE_COPY,
    NODE_PASTE,
    NODE_CUT,
}

/**
 * Keycode → accessibility expression. `null` means "no honest equivalent".
 *
 * Pure and Android-free at run time: every `KEYCODE_*` here is a Java
 * compile-time constant, so this classifies without loading a framework class —
 * which is what lets a decline be asserted in a plain JVM unit test.
 */
private fun routeFor(keyCode: Int): AccessibilityKeyRoute? = when (keyCode) {
    KeyEvent.KEYCODE_BACK -> AccessibilityKeyRoute.GLOBAL_BACK
    KeyEvent.KEYCODE_HOME -> AccessibilityKeyRoute.GLOBAL_HOME
    KeyEvent.KEYCODE_APP_SWITCH -> AccessibilityKeyRoute.GLOBAL_RECENTS
    // SEARCH and NUMPAD_ENTER collapse onto the same expression because
    // ACTION_IME_ENTER fires whichever action the focused field advertises.
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_SEARCH -> AccessibilityKeyRoute.IME_ENTER
    KeyEvent.KEYCODE_COPY -> AccessibilityKeyRoute.NODE_COPY
    KeyEvent.KEYCODE_PASTE -> AccessibilityKeyRoute.NODE_PASTE
    KeyEvent.KEYCODE_CUT -> AccessibilityKeyRoute.NODE_CUT
    else -> null
}
