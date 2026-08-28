package com.aster.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartLiftClassifierTest {
    private fun quiet(
        classifier: SmartLiftClassifier,
        fromMs: Long,
        samples: Int,
        pressure: Double? = null,
    ): Pair<Long, List<SmartLiftClassifier.Edge>> {
        var now = fromMs
        val edges = mutableListOf<SmartLiftClassifier.Edge>()
        repeat(samples) { i ->
            pressure?.let(classifier::pushPressure)
            classifier.pushGyroscope(0.002, -0.003, 0.001)
            val wobble = if (i % 2 == 0) 0.012 else -0.012
            classifier.pushAccelerometer(wobble, -wobble, 9.80665, now)?.let(edges::add)
            now += 40L
        }
        return now to edges
    }

    private fun calibrated(pressure: Double? = null): Pair<SmartLiftClassifier, Long> {
        val classifier = SmartLiftClassifier()
        val (now, edges) = quiet(classifier, 1_000L, 160, pressure)
        assertTrue(edges.isEmpty())
        return classifier to now
    }

    private fun pickup(classifier: SmartLiftClassifier, fromMs: Long): Pair<Long, List<SmartLiftClassifier.Edge>> {
        var now = fromMs
        val edges = mutableListOf<SmartLiftClassifier.Edge>()
        // Coherent handling burst: translation + rotation, then a held phase.
        repeat(9) { i ->
            val sign = if (i % 2 == 0) 1.0 else -1.0
            classifier.pushGyroscope(0.28 * sign, -0.19 * sign, 0.11)
            classifier.pushAccelerometer(3.4 * sign, -2.2 * sign, 9.80665 + 1.7 * sign, now)?.let(edges::add)
            now += 40L
        }
        repeat(24) { i ->
            val tremor = if (i % 2 == 0) 0.045 else -0.045
            classifier.pushGyroscope(0.012, -0.009, 0.006)
            classifier.pushAccelerometer(tremor, -tremor, 9.80665, now)?.let(edges::add)
            now += 40L
        }
        return now to edges
    }

    @Test
    fun `supported phone calibrates silently and remains grounded`() {
        val (classifier, now) = calibrated()
        val (_, edges) = quiet(classifier, now, 200)
        assertTrue(edges.isEmpty())
        assertFalse(classifier.isAirborne())
        assertTrue(classifier.learnedAccelFloor() > 0.0)
        assertTrue(classifier.learnedGyroFloor() > 0.0)
    }

    @Test
    fun `pickup requires accel gyro corroboration then held phase`() {
        val (classifier, now) = calibrated()
        val (_, edges) = pickup(classifier, now)
        assertEquals(1, edges.count { it == SmartLiftClassifier.Edge.LIFTED })
        assertTrue(classifier.isAirborne())
    }

    @Test
    fun `accelerometer bump without rotation is not a lift`() {
        val (classifier, start) = calibrated()
        var now = start
        repeat(12) { i ->
            val sign = if (i % 2 == 0) 1.0 else -1.0
            classifier.pushGyroscope(0.001, 0.001, 0.001)
            assertNull(classifier.pushAccelerometer(4.2 * sign, 0.0, 9.80665, now))
            now += 40L
        }
        val (_, edges) = quiet(classifier, now, 80)
        assertTrue(edges.none { it == SmartLiftClassifier.Edge.LIFTED })
    }

    @Test
    fun `devices without gyroscope use conservative burst held fallback`() {
        val classifier = SmartLiftClassifier().apply {
            configureSensors(hasGyroscope = false, hasBarometer = false)
        }
        var now = 1_000L
        repeat(160) {
            classifier.pushAccelerometer(0.0, 0.0, 9.80665, now)
            now += 40L
        }
        val edges = mutableListOf<SmartLiftClassifier.Edge>()
        repeat(12) { i ->
            val sign = if (i % 2 == 0) 1.0 else -1.0
            classifier.pushAccelerometer(5.2 * sign, -2.4 * sign, 9.80665, now)?.let(edges::add)
            now += 40L
        }
        repeat(24) {
            classifier.pushAccelerometer(0.02, -0.02, 9.80665, now)?.let(edges::add)
            now += 40L
        }

        assertEquals(1, edges.count { it == SmartLiftClassifier.Edge.LIFTED })
    }

    @Test
    fun `barometer measures meaningful height and landing waits for return`() {
        val (classifier, start) = calibrated(1_013.25)
        var now = start
        // ~0.8 m above the calibrated support.
        classifier.pushPressure(1_013.15)
        val pickup = pickup(classifier, now)
        now = pickup.first
        assertTrue(pickup.second.contains(SmartLiftClassifier.Edge.LIFTED))
        assertTrue(classifier.relativeHeightMetres() > 0.55)

        val held = quiet(classifier, now, 50, 1_013.15)
        now = held.first
        assertTrue(held.second.none { it == SmartLiftClassifier.Edge.LANDED })

        val landed = quiet(classifier, now, 50, 1_013.25)
        assertTrue(landed.second.contains(SmartLiftClassifier.Edge.LANDED))
        assertFalse(classifier.isAirborne())
    }

    @Test
    fun `deliberate shake suppression cannot become afraid`() {
        val (classifier, start) = calibrated()
        classifier.noteShake(start)
        var now = start
        repeat(10) { i ->
            val sign = if (i % 2 == 0) 1.0 else -1.0
            classifier.pushGyroscope(0.4 * sign, -0.3 * sign, 0.2)
            assertNull(classifier.pushAccelerometer(8.0 * sign, 4.0 * sign, 20.0 * sign, now))
            now += 50L
        }
        val (_, edges) = quiet(classifier, now, 30)
        assertTrue(edges.none { it == SmartLiftClassifier.Edge.LIFTED })
    }
}
