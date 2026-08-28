package com.aster.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPulseConfigurationTest {
    @Test
    fun `accepts known lanes and ignores unknown future lanes`() {
        val parsed = parseCompanionPulseConfiguration(
            """{"enabled":true,"lanes":["power","lift","rootAccess"]}""",
        ) ?: error("configuration should parse")

        assertEquals(setOf("power", "lift"), parsed.lanes)
        assertTrue(parsed.allows("power"))
        assertFalse(parsed.allows("notification"))
        assertFalse(parsed.allows("rootAccess"))
    }

    @Test
    fun `master switch denies every lane`() {
        val parsed = parseCompanionPulseConfiguration(
            """{"enabled":false,"lanes":["power","shake"]}""",
        ) ?: error("configuration should parse")

        assertFalse(parsed.allows("power"))
        assertFalse(parsed.allows("shake"))
    }

    @Test
    fun `rejects malformed or incomplete configuration`() {
        assertNull(parseCompanionPulseConfiguration("{}"))
        assertNull(parseCompanionPulseConfiguration("not-json"))
        assertNull(parseCompanionPulseConfiguration("""{"enabled":true,"lanes":7}"""))
    }
}
