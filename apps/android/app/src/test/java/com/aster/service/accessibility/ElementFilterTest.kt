package com.aster.service.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementFilterTest {

    private fun facts(
        clickable: Boolean = false,
        editable: Boolean = false,
        checkable: Boolean = false,
        scrollable: Boolean = false,
        longClickable: Boolean = false,
        text: String = "",
        desc: String = "",
    ) = NodeFacts(
        clickable = clickable, editable = editable, checkable = checkable,
        scrollable = scrollable, longClickable = longClickable, text = text, desc = desc,
    )

    @Test
    fun actionable_keeps_clickable() {
        assertTrue(ElementFilter.keep(facts(clickable = true), ObserveMode.ACTIONABLE, null))
    }

    @Test
    fun actionable_keeps_editable() {
        assertTrue(ElementFilter.keep(facts(editable = true), ObserveMode.ACTIONABLE, null))
    }

    @Test
    fun actionable_keeps_checkable_and_scrollable() {
        assertTrue(ElementFilter.keep(facts(checkable = true), ObserveMode.ACTIONABLE, null))
        assertTrue(ElementFilter.keep(facts(scrollable = true), ObserveMode.ACTIONABLE, null))
    }

    @Test
    fun actionable_drops_plain_text() {
        assertFalse(ElementFilter.keep(facts(text = "hello"), ObserveMode.ACTIONABLE, null))
    }

    @Test
    fun text_mode_keeps_any_text_or_desc() {
        assertTrue(ElementFilter.keep(facts(text = "hello"), ObserveMode.TEXT, null))
        assertTrue(ElementFilter.keep(facts(desc = "icon"), ObserveMode.TEXT, null))
        assertFalse(ElementFilter.keep(facts(), ObserveMode.TEXT, null))
    }

    @Test
    fun full_mode_keeps_everything() {
        assertTrue(ElementFilter.keep(facts(), ObserveMode.FULL, null))
    }

    @Test
    fun searchText_narrows_within_mode() {
        // Matches text → kept; non-match → dropped, even though clickable.
        assertTrue(ElementFilter.keep(facts(clickable = true, text = "Send"), ObserveMode.ACTIONABLE, "send"))
        assertFalse(ElementFilter.keep(facts(clickable = true, text = "Cancel"), ObserveMode.ACTIONABLE, "send"))
    }

    @Test
    fun searchText_matches_desc_too() {
        assertTrue(ElementFilter.keep(facts(clickable = true, desc = "Send button"), ObserveMode.ACTIONABLE, "send"))
    }

    @Test
    fun default_cap_is_positive() {
        assertTrue(ElementFilter.MAX_ELEMENTS_DEFAULT > 0)
    }
}
