package com.aster.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionFaceStateTest {
    @Test
    fun `parses one bounded text-free speech snapshot`() {
        val parsed = parseCompanionFaceState(
            """{"version":1,"session":"oa-session-1","sequence":7,"speech":[{"id":"chat:run-1","source":"stream","level":0.72,"viseme":"round","ttlMs":30000,"reducedMotion":false}]}""",
        )

        requireNotNull(parsed)
        assertEquals(7L, parsed.sequence)
        assertEquals(1, parsed.speech.size)
        assertEquals(CompanionViseme.ROUND, parsed.speech.single().viseme)
        assertEquals(CompanionSpeechSource.STREAM, parsed.speech.single().source)
    }

    @Test
    fun `empty snapshot is the explicit stop state`() {
        val parsed = parseCompanionFaceState(
            """{"version":1,"session":"oa-session-1","sequence":8,"speech":[]}""",
        )
        requireNotNull(parsed)
        assertTrue(parsed.speech.isEmpty())
    }

    @Test
    fun `rejects unknown fields enums duplicate ids and out of bounds values`() {
        assertNull(parseCompanionFaceState("""{"version":1,"session":"oa-session-1","sequence":1,"speech":[],"text":"secret"}"""))
        assertNull(parseCompanionFaceState("""{"version":4294967297,"session":"oa-session-1","sequence":1,"speech":[]}"""))
        assertNull(parseCompanionFaceState("""{"version":1,"session":"bad session","sequence":1,"speech":[]}"""))
        assertNull(
            parseCompanionFaceState(
                """{"version":1,"session":"oa-session-1","sequence":1,"speech":[{"id":"x","source":"stream","level":2,"viseme":"round","ttlMs":30000,"reducedMotion":false}]}""",
            ),
        )
        assertNull(
            parseCompanionFaceState(
                """{"version":1,"session":"oa-session-1","sequence":1,"speech":[{"id":"x","source":"stream","level":0.5,"viseme":"unknown","ttlMs":30000,"reducedMotion":false}]}""",
            ),
        )
        assertNull(
            parseCompanionFaceState(
                """{"version":1,"session":"oa-session-1","sequence":1,"speech":[{"id":"x","source":"stream","level":0.5,"viseme":"open","ttlMs":30000,"reducedMotion":false},{"id":"x","source":"audio","level":0.4,"viseme":"neutral","ttlMs":30000,"reducedMotion":false}]}""",
            ),
        )
    }

    @Test
    fun `concurrent aggregation never extends one source with another ttl`() {
        val snapshot = CompanionFaceStateSnapshot(
            version = 1,
            session = "oa-session-1",
            sequence = 1,
            speech = listOf(
                CompanionSpeechStream(
                    id = "loud",
                    source = CompanionSpeechSource.STREAM,
                    level = 0.9f,
                    viseme = CompanionViseme.OPEN,
                    ttlMs = 1_000L,
                    reducedMotion = false,
                ),
                CompanionSpeechStream(
                    id = "long",
                    source = CompanionSpeechSource.AUDIO,
                    level = 0.4f,
                    viseme = CompanionViseme.ROUND,
                    ttlMs = 5_000L,
                    reducedMotion = true,
                ),
            ),
        )

        val first = requireNotNull(aggregateCompanionSpeech(snapshot, 10_000L, 10_500L))
        assertEquals("loud", first.cue.id)
        assertEquals(11_000L, first.cueExpiresAtMs)
        assertEquals(11_000L, first.nextExpiryAtMs)
        assertTrue(first.reducedMotion)

        val second = requireNotNull(aggregateCompanionSpeech(snapshot, 10_000L, 11_001L))
        assertEquals("long", second.cue.id)
        assertEquals(15_000L, second.cueExpiresAtMs)
    }
}
