package com.aster.service.accessibility

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityPulseClassifierTest {
    @Test
    fun `foreground changes baseline first app and deduplicates package`() {
        val classifier = AccessibilityPulseClassifier()
        val first = classifier.classify(
            AccessibilityPulseClassifier.Input(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                "com.example.first",
            ),
            100,
        )
        assertEquals(true, first.single().values["initial"])

        assertTrue(
            classifier.classify(
                AccessibilityPulseClassifier.Input(
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    "com.example.first",
                ),
                200,
            ).isEmpty(),
        )

        val changed = classifier.classify(
            AccessibilityPulseClassifier.Input(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                "com.example.second",
            ),
            300,
        )
        assertEquals(false, changed.single().values["initial"])
    }

    @Test
    fun `scroll chooses dominant axis and rate limits event bursts`() {
        val classifier = AccessibilityPulseClassifier()
        val input = AccessibilityPulseClassifier.Input(
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            "com.example",
            scrollDeltaX = 2,
            scrollDeltaY = 14,
        )
        assertEquals("down", classifier.classify(input, 1_000).single().values["direction"])
        assertTrue(classifier.classify(input, 1_200).isEmpty())
        assertEquals("down", classifier.classify(input, 1_800).single().values["direction"])
    }

    @Test
    fun `payload shape cannot contain private UI or typed content`() {
        val classifier = AccessibilityPulseClassifier()
        val pulses = listOf(
            classifier.classify(
                AccessibilityPulseClassifier.Input(
                    AccessibilityEvent.TYPE_VIEW_CLICKED,
                    "com.example",
                ),
                100,
            ).single(),
            classifier.classify(
                AccessibilityPulseClassifier.Input(
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                    "com.example",
                ),
                200,
            ).single(),
        )
        val keys = pulses.flatMap { it.values.keys }
        assertFalse(keys.any { it in setOf("text", "content", "description", "title", "source") })
        assertTrue(keys.all { it in setOf("action", "active") })
    }
}
