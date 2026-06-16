package com.aster.service.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class RoleMapperTest {

    @Test
    fun button_from_button_classname() {
        assertEquals(
            "button",
            RoleMapper.roleOf(
                className = "android.widget.Button",
                isEditable = false, isCheckable = false, isClickable = true,
                isLongClickable = false, isScrollable = false, hasText = true,
            ),
        )
    }

    @Test
    fun edittext_from_editable_flag_wins_over_classname() {
        // Editable is the strongest signal — even a custom className resolves to edittext.
        assertEquals(
            "edittext",
            RoleMapper.roleOf(
                className = "com.acme.FancyField",
                isEditable = true, isCheckable = false, isClickable = true,
                isLongClickable = false, isScrollable = false, hasText = false,
            ),
        )
    }

    @Test
    fun switch_from_switch_classname() {
        assertEquals(
            "switch",
            RoleMapper.roleOf(
                className = "android.widget.Switch",
                isEditable = false, isCheckable = true, isClickable = true,
                isLongClickable = false, isScrollable = false, hasText = false,
            ),
        )
    }

    @Test
    fun materialswitch_from_classname() {
        assertEquals(
            "switch",
            RoleMapper.roleOf(
                className = "com.google.android.material.materialswitch.MaterialSwitch",
                isEditable = false, isCheckable = true, isClickable = true,
                isLongClickable = false, isScrollable = false, hasText = false,
            ),
        )
    }

    @Test
    fun checkbox_from_checkable_without_switch_classname() {
        assertEquals(
            "checkbox",
            RoleMapper.roleOf(
                className = "android.widget.CheckBox",
                isEditable = false, isCheckable = true, isClickable = true,
                isLongClickable = false, isScrollable = false, hasText = true,
            ),
        )
    }

    @Test
    fun radio_from_radiobutton_classname() {
        assertEquals(
            "radio",
            RoleMapper.roleOf(
                className = "android.widget.RadioButton",
                isEditable = false, isCheckable = true, isClickable = true,
                isLongClickable = false, isScrollable = false, hasText = true,
            ),
        )
    }

    @Test
    fun image_from_imageview_no_text() {
        assertEquals(
            "image",
            RoleMapper.roleOf(
                className = "android.widget.ImageView",
                isEditable = false, isCheckable = false, isClickable = false,
                isLongClickable = false, isScrollable = false, hasText = false,
            ),
        )
    }

    @Test
    fun scrollview_from_scrollable_flag() {
        assertEquals(
            "scrollview",
            RoleMapper.roleOf(
                className = "androidx.recyclerview.widget.RecyclerView",
                isEditable = false, isCheckable = false, isClickable = false,
                isLongClickable = false, isScrollable = true, hasText = false,
            ),
        )
    }

    @Test
    fun listitem_from_clickable_with_text_in_list() {
        // Clickable + text + not a known widget → listitem-style actionable row.
        assertEquals(
            "listitem",
            RoleMapper.roleOf(
                className = "android.view.ViewGroup",
                isEditable = false, isCheckable = false, isClickable = true,
                isLongClickable = false, isScrollable = false, hasText = true,
            ),
        )
    }

    @Test
    fun text_from_textview_no_interaction() {
        assertEquals(
            "text",
            RoleMapper.roleOf(
                className = "android.widget.TextView",
                isEditable = false, isCheckable = false, isClickable = false,
                isLongClickable = false, isScrollable = false, hasText = true,
            ),
        )
    }

    @Test
    fun other_when_nothing_matches() {
        assertEquals(
            "other",
            RoleMapper.roleOf(
                className = "android.view.View",
                isEditable = false, isCheckable = false, isClickable = false,
                isLongClickable = false, isScrollable = false, hasText = false,
            ),
        )
    }
}
