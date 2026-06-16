package com.aster.service.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionMapperTest {

    @Test
    fun click() {
        assertEquals("click", ActionMapper.normalize(AccessibilityNodeInfo.ACTION_CLICK))
    }

    @Test
    fun long_click() {
        assertEquals("long_click", ActionMapper.normalize(AccessibilityNodeInfo.ACTION_LONG_CLICK))
    }

    @Test
    fun scroll_forward() {
        assertEquals(
            "scroll_forward",
            ActionMapper.normalize(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD),
        )
    }

    @Test
    fun scroll_backward() {
        assertEquals(
            "scroll_backward",
            ActionMapper.normalize(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD),
        )
    }

    @Test
    fun set_text() {
        assertEquals("set_text", ActionMapper.normalize(AccessibilityNodeInfo.ACTION_SET_TEXT))
    }

    @Test
    fun expand() {
        assertEquals("expand", ActionMapper.normalize(AccessibilityNodeInfo.ACTION_EXPAND))
    }

    @Test
    fun collapse() {
        assertEquals("collapse", ActionMapper.normalize(AccessibilityNodeInfo.ACTION_COLLAPSE))
    }

    @Test
    fun dismiss() {
        assertEquals("dismiss", ActionMapper.normalize(AccessibilityNodeInfo.ACTION_DISMISS))
    }

    @Test
    fun focus() {
        assertEquals("focus", ActionMapper.normalize(AccessibilityNodeInfo.ACTION_FOCUS))
    }

    @Test
    fun select() {
        assertEquals("select", ActionMapper.normalize(AccessibilityNodeInfo.ACTION_SELECT))
    }

    @Test
    fun unknown_action_id_returns_null() {
        assertNull(ActionMapper.normalize(0x7FFF_FFFF))
    }
}
