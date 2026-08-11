package com.aster.service.input

/**
 * The outcome of one key press, as reported to the caller.
 *
 * @property ok whether the press was actually delivered. Nothing else in this
 *   type may be read as success — in particular a non-null [backend] only says
 *   which backend last had the press, not that it worked.
 * @property backend which mechanism delivered it, or — when [ok] is false — the
 *   one that owned and failed the press. `null` means no backend ever took
 *   ownership: every one of them declined, or there were none.
 * @property reason why it did not work, verbatim from the backend that failed
 *   or joined from the declines. `null` only when [ok] is true.
 */
data class InputResult(val ok: Boolean, val backend: InputBackendKind?, val reason: String?)

/**
 * Orders the key-press backends and picks the first that can serve a press.
 *
 * The fall-through rule is the whole contract, and it is asymmetric on purpose:
 *
 *  - [BackendOutcome.Declined] falls through. The backend did not act, so
 *    handing the same press to the next one cannot double-deliver it.
 *  - [BackendOutcome.Failed] stops. The backend owned the press and reached for
 *    it; it may have partially landed, and its reason is the real diagnosis. A
 *    retry here would risk a second delivery and would replace a precise error
 *    with a vaguer one.
 *  - An unavailable backend is skipped, which is a decline in all but name: it
 *    never saw the press.
 *
 * With a single backend the router is a thin pass-through — but a *correct* one:
 * the ordering, the asymmetry and the reporting are all already in place, so a
 * second backend is one element in the list this is constructed with and no
 * change here at all.
 */
class InputRouter(private val backends: List<InputBackend>) {

    /**
     * Deliver [keyCode] (an `android.view.KeyEvent.KEYCODE_*` value) through the
     * first backend that can express it.
     *
     * Never throws for an unserviceable key: an exhausted chain is an
     * [InputResult] with `ok = false` and every backend's reason joined, so the
     * caller can report *why* nothing could do it rather than inventing a cause.
     */
    suspend fun pressKeyCode(keyCode: Int): InputResult {
        if (backends.isEmpty()) {
            return InputResult(ok = false, backend = null, reason = "no input backend is configured")
        }

        val declines = mutableListOf<String>()
        for (backend in backends) {
            val name = backend.kind.name.lowercase()
            if (!backend.isAvailable()) {
                declines += "$name: not available on this device right now"
                continue
            }
            when (val outcome = backend.pressKeyCode(keyCode)) {
                is BackendOutcome.Ok -> return InputResult(ok = true, backend = backend.kind, reason = null)
                is BackendOutcome.Declined -> declines += "$name: ${outcome.why}"
                is BackendOutcome.Failed -> return InputResult(
                    ok = false,
                    backend = backend.kind,
                    reason = outcome.why
                )
            }
        }

        // Every backend declined. No backend owned the press, so `backend` stays
        // null and the reason carries all of them — that set IS the diagnosis.
        return InputResult(ok = false, backend = null, reason = declines.joinToString("; "))
    }
}
