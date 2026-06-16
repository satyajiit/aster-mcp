package com.aster.service.handlers

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure predicate for `wait_for` (SPEC §3.3). Decides whether an observe-shaped element
 * list satisfies a target spec, with `gone` inverting the sense.
 *
 * Field names follow the SPEC §3.1 observe element shape: text, viewId, role.
 * Matching semantics:
 *   - text:   case-insensitive SUBSTRING match against element.text
 *   - viewId: case-insensitive EXACT match against element.viewId
 *   - role:   case-insensitive EXACT match against element.role
 * Multiple supplied criteria must ALL match on the SAME element.
 *
 * No Android deps -> JVM-unit-testable without an accessibility tree.
 */
object ScreenWaitMatcher {

    /**
     * @param elements observe-shaped element array (JsonArray of JsonObject).
     * @param text     optional text substring target.
     * @param viewId   optional exact viewId target.
     * @param role     optional exact role target.
     * @param gone     when true, satisfied means the target is ABSENT.
     * @return true when the wait condition is satisfied.
     */
    fun isSatisfied(
        elements: JsonArray,
        text: String?,
        viewId: String?,
        role: String?,
        gone: Boolean
    ): Boolean {
        // A wait with no target is never a "present" match (avoid vacuous true).
        if (text == null && viewId == null && role == null) return false

        val present = elements.any { el -> el is JsonObject && elementMatches(el, text, viewId, role) }
        return if (gone) !present else present
    }

    private fun elementMatches(
        el: JsonObject,
        text: String?,
        viewId: String?,
        role: String?
    ): Boolean {
        if (text != null) {
            val elText = el["text"]?.jsonPrimitive?.contentOrNull ?: ""
            if (!elText.contains(text, ignoreCase = true)) return false
        }
        if (viewId != null) {
            val elViewId = el["viewId"]?.jsonPrimitive?.contentOrNull ?: ""
            if (!elViewId.equals(viewId, ignoreCase = true)) return false
        }
        if (role != null) {
            val elRole = el["role"]?.jsonPrimitive?.contentOrNull ?: ""
            if (!elRole.equals(role, ignoreCase = true)) return false
        }
        return true
    }
}
