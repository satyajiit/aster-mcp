package com.aster.service.handlers

/**
 * Decides, per contact, whether a verified delete may go ahead — the pure half
 * of `delete_contacts_verified` (OpenAlly ticket OA-2026-0207).
 *
 * OpenAlly's owner approved a list of contacts BY NAME. Between that tap and
 * this call the address book can change, and Android can re-number a contact:
 * the Contacts Provider guide says a contact's `_ID` "may change … in response
 * to an aggregation or sync", while `CONTENT_LOOKUP_URI` + `LOOKUP_KEY` "will
 * still point to the contact row"
 * (https://developer.android.com/identity/providers/contacts-provider). So an
 * id alone is not proof of who it is. Each contact is deleted only when the
 * row behind its id still carries the name the owner saw, and it is deleted
 * through its lookup URI, never by raw id.
 *
 * Kept free of Android types so the decision is unit-tested on the JVM.
 */
object ContactDeletePlanner {

    /** What the provider holds for one contact id right now. */
    data class CurrentContact(val lookupKey: String?, val displayName: String?)

    /** One contact cleared to delete. */
    data class Planned(val id: String, val lookupKey: String)

    /** One contact refused, with a reason from the closed wire set. */
    data class Refused(val id: String, val reason: String)

    data class Plan(val toDelete: List<Planned>, val refused: List<Refused>)

    /** Reasons OpenAlly renders; anything else is reported as `failed`. */
    const val NOT_FOUND = "not_found"
    const val CHANGED = "changed"
    const val PERMISSION_NEEDED = "permission_needed"
    const val FAILED = "failed"

    /**
     * The name `list_contacts_full` reports for a contact with none, so the
     * expectation OpenAlly echoes back for such a contact still matches.
     */
    private const val NO_NAME = "Unknown"

    private fun normalise(name: String?): String =
        (name ?: NO_NAME).trim().replace(Regex("\\s+"), " ")

    /**
     * @param expected contact id → the display name the owner approved, in the
     *   order OpenAlly sent them (the report keeps that order).
     * @param current what the provider holds for those ids; a missing key means
     *   the contact no longer exists.
     */
    fun plan(
        expected: List<Pair<String, String>>,
        current: Map<String, CurrentContact>,
        keep: List<Pair<String, String>> = emptyList()
    ): Plan {
        val toDelete = mutableListOf<Planned>()
        val refused = mutableListOf<Refused>()
        val seen = mutableSetOf<String>()
        // The copy the owner was told stays must still be on the phone, under
        // the name they saw. OpenAlly checks its own index first, but that
        // index can lag the phone (a sync has not run since the kept copy was
        // deleted, or Android merged it into one of the targets). Deleting
        // "the duplicates" then would delete the person, so nothing goes.
        if (!keepIntact(keep, current)) {
            for ((id, _) in expected) {
                if (seen.add(id)) refused += Refused(id, CHANGED)
            }
            return Plan(toDelete, refused)
        }
        for ((id, name) in expected) {
            if (!seen.add(id)) continue
            val row = current[id]
            if (row == null) {
                refused += Refused(id, NOT_FOUND)
                continue
            }
            val lookupKey = row.lookupKey
            if (lookupKey.isNullOrEmpty()) {
                refused += Refused(id, FAILED)
            } else if (normalise(row.displayName) != normalise(name)) {
                refused += Refused(id, CHANGED)
            } else {
                toDelete += Planned(id, lookupKey)
            }
        }
        return Plan(toDelete, refused)
    }

    /**
     * Whether every kept contact still exists under the name the owner saw.
     * An empty list (a delete that is not a duplicate clean-up) is intact.
     */
    fun keepIntact(keep: List<Pair<String, String>>, current: Map<String, CurrentContact>): Boolean =
        keep.all { (id, name) ->
            val row = current[id]
            row != null && normalise(row.displayName) == normalise(name)
        }
}
