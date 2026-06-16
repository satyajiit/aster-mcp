package com.aster.service.handlers

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenWaitMatcherTest {

    // observe-shaped elements: { ref, role, text, viewId, ... }
    private val elements = buildJsonArray {
        add(buildJsonObject {
            put("ref", "e1"); put("role", "button")
            put("text", "Send Message"); put("viewId", "com.app:id/send")
        })
        add(buildJsonObject {
            put("ref", "e2"); put("role", "edittext")
            put("text", ""); put("viewId", "com.app:id/input")
        })
    }

    @Test
    fun text_substring_matches_present() {
        // "Send" is a substring of "Send Message"
        assertTrue(
            ScreenWaitMatcher.isSatisfied(elements, text = "Send", viewId = null, role = null, gone = false)
        )
    }

    @Test
    fun text_no_match_is_not_satisfied_when_present_expected() {
        assertFalse(
            ScreenWaitMatcher.isSatisfied(elements, text = "Checkout", viewId = null, role = null, gone = false)
        )
    }

    @Test
    fun viewId_exact_match() {
        assertTrue(
            ScreenWaitMatcher.isSatisfied(elements, text = null, viewId = "com.app:id/input", role = null, gone = false)
        )
        assertFalse(
            ScreenWaitMatcher.isSatisfied(elements, text = null, viewId = "com.app:id/nope", role = null, gone = false)
        )
    }

    @Test
    fun role_match() {
        assertTrue(
            ScreenWaitMatcher.isSatisfied(elements, text = null, viewId = null, role = "button", gone = false)
        )
        assertFalse(
            ScreenWaitMatcher.isSatisfied(elements, text = null, viewId = null, role = "checkbox", gone = false)
        )
    }

    @Test
    fun gone_inverts_present() {
        // "Send" IS present -> gone=true is NOT satisfied
        assertFalse(
            ScreenWaitMatcher.isSatisfied(elements, text = "Send", viewId = null, role = null, gone = true)
        )
        // "Checkout" is NOT present -> gone=true IS satisfied
        assertTrue(
            ScreenWaitMatcher.isSatisfied(elements, text = "Checkout", viewId = null, role = null, gone = true)
        )
    }

    @Test
    fun multiple_criteria_must_all_match_on_one_element() {
        // text "Send" + role "button" both on e1 -> present
        assertTrue(
            ScreenWaitMatcher.isSatisfied(elements, text = "Send", viewId = null, role = "button", gone = false)
        )
        // text "Send" on e1 but role "edittext" on e2 -> no single element has both
        assertFalse(
            ScreenWaitMatcher.isSatisfied(elements, text = "Send", viewId = null, role = "edittext", gone = false)
        )
    }

    @Test
    fun no_criteria_is_not_satisfied() {
        // A wait with no target can never resolve as "present"; guard against vacuous match.
        assertFalse(
            ScreenWaitMatcher.isSatisfied(elements, text = null, viewId = null, role = null, gone = false)
        )
    }
}
