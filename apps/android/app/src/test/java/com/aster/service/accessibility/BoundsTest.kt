package com.aster.service.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class BoundsTest {

    @Test
    fun bounds_fromLTRB_matches_spec_example() {
        // SPEC §3.1 example: x=980,y=2180,w=96,h=96,cx=1028,cy=2228
        val b = Bounds.fromLTRB(980, 2180, 1076, 2276)
        assertEquals(980, b.x)
        assertEquals(2180, b.y)
        assertEquals(96, b.w)
        assertEquals(96, b.h)
        assertEquals(1028, b.cx)
        assertEquals(2228, b.cy)
    }

    @Test
    fun descriptor_bounds_fromLTRB_agrees_with_bounds() {
        val b = Bounds.fromLTRB(0, 0, 200, 100)
        val d = DescriptorBounds.fromLTRB(0, 0, 200, 100)
        assertEquals(b.x, d.x)
        assertEquals(b.y, d.y)
        assertEquals(b.w, d.w)
        assertEquals(b.h, d.h)
        assertEquals(b.cx, d.cx)
        assertEquals(b.cy, d.cy)
    }

    @Test
    fun zero_area_bounds_are_safe() {
        val b = Bounds.fromLTRB(50, 50, 50, 50)
        assertEquals(0, b.w)
        assertEquals(0, b.h)
        assertEquals(50, b.cx)
        assertEquals(50, b.cy)
    }
}
