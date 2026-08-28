package com.aster.service.overlay

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Adaptive phone lift/height classifier, derived from the desktop IMU policy.
 *
 * It fuses high-pass acceleration and gyroscope activity, learns the vibration
 * floor of the current supported surface, confirms a pickup as a short handling
 * burst followed by a held phase, and optionally corroborates height with a
 * barometer. Raw samples never leave the process.
 */
class SmartLiftClassifier {
    enum class Edge { LIFTED, REFIRE, LANDED }

    companion object {
        private const val GRAVITY = 9.80665
        private const val HIGH_PASS_ALPHA = 0.85
        private const val ACTIVITY_ALPHA = 0.25
        private const val FLOOR_RISE_ALPHA = 0.035
        private const val FLOOR_FALL_ALPHA = 0.006
        private const val WARM_FLOOR_ALPHA = 0.08
        private const val CALIBRATION_MS = 5_000L
        private const val POST_SETTLE_CALIBRATION_MS = 1_500L

        private const val ACCEL_FLOOR_MIN_G = 0.004
        private const val ACCEL_FLOOR_MAX_G = 0.28
        private const val GYRO_FLOOR_MIN_DPS = 0.25
        private const val GYRO_FLOOR_MAX_DPS = 30.0
        private const val ONSET_FLOOR_MULT = 3.0
        private const val ONSET_ACCEL_MIN_G = 0.045
        private const val ONSET_GYRO_MIN_DPS = 3.5
        private const val ONSET_STREAK = 4
        private const val CONFIRM_QUIET_STREAK = 5
        private const val CANDIDATE_TIMEOUT_MS = 2_000L
        private const val STRONG_RATIO = 1.7
        private const val INPUT_STRONG_RATIO = 2.1

        private const val VIBRATING_ACCEL_G = 0.022
        private const val VIBRATING_GYRO_DPS = 1.7
        private const val HANDLING_RATIO = 1.9
        private const val LIFT_OFF_QUIET_RATIO = 0.58
        private const val LIFT_OFF_STREAK = 5

        private const val SETTLE_FLOOR_MULT = 1.65
        private const val SETTLE_ACCEL_MIN_G = 0.020
        private const val SETTLE_GYRO_MIN_DPS = 1.15
        private const val SETTLE_MS = 1_350L
        private const val MIN_AIRBORNE_MS = 650L
        private const val REFIRE_MS = 1_100L
        private const val HEIGHT_CONFIRM_METRES = 0.55
        private const val HEIGHT_LANDED_METRES = 0.28
        private const val NEW_SURFACE_MS = 8_000L
        private const val INTERACTION_WINDOW_MS = 1_200L
    }

    private var previousAccel: DoubleArray? = null
    private val highPass = DoubleArray(3)
    private var latestGyroDps = DoubleArray(3)
    private var accelActivity = 0.0
    private var gyroActivity = 0.0
    private var accelFloor = 0.012
    private var gyroFloor = 0.7

    private var calibrationUntilMs = 0L
    private var onsetStreak = 0
    private var candidateStartedAtMs = 0L
    private var confirmQuietStreak = 0
    private var liftOffQuietStreak = 0
    private var handlingMemory = 0
    private var airborne = false
    private var airborneSinceMs = 0L
    private var calmSinceMs = 0L
    private var lastRefireMs = 0L
    private var lastInteractionMs = Long.MIN_VALUE
    private var suppressedUntilMs = 0L

    private var pressureBaselineHpa: Double? = null
    private var pressureHpa: Double? = null
    private var candidateAccelFloor = 0.012
    private var candidateGyroFloor = 0.7
    private var hasGyroscope = true
    private var hasBarometer = true

    /** Sensor availability is explicit: cheaper phones still get a conservative
     *  accel burst→held fallback instead of silently losing the height reaction. */
    fun configureSensors(hasGyroscope: Boolean, hasBarometer: Boolean) {
        this.hasGyroscope = hasGyroscope
        this.hasBarometer = hasBarometer
        if (!hasBarometer) {
            pressureBaselineHpa = null
            pressureHpa = null
        }
    }

    fun reset() {
        previousAccel = null
        highPass.fill(0.0)
        latestGyroDps.fill(0.0)
        accelActivity = 0.0
        gyroActivity = 0.0
        accelFloor = 0.012
        gyroFloor = 0.7
        calibrationUntilMs = 0L
        onsetStreak = 0
        candidateStartedAtMs = 0L
        confirmQuietStreak = 0
        liftOffQuietStreak = 0
        handlingMemory = 0
        airborne = false
        airborneSinceMs = 0L
        calmSinceMs = 0L
        lastRefireMs = 0L
        lastInteractionMs = Long.MIN_VALUE
        suppressedUntilMs = 0L
        pressureBaselineHpa = null
        pressureHpa = null
        candidateAccelFloor = accelFloor
        candidateGyroFloor = gyroFloor
    }

    fun noteInteraction(nowMs: Long) {
        lastInteractionMs = nowMs
    }

    /** A deliberate shake is its own gesture, not evidence that the phone is high. */
    fun noteShake(nowMs: Long) {
        candidateStartedAtMs = 0L
        confirmQuietStreak = 0
        onsetStreak = 0
        suppressedUntilMs = nowMs + 900L
    }

    fun pushGyroscope(xRad: Double, yRad: Double, zRad: Double) {
        if (!xRad.isFinite() || !yRad.isFinite() || !zRad.isFinite()) return
        val toDegrees = 180.0 / PI
        latestGyroDps = doubleArrayOf(xRad * toDegrees, yRad * toDegrees, zRad * toDegrees)
    }

    fun pushPressure(hectopascals: Double) {
        if (!hectopascals.isFinite() || hectopascals !in 300.0..1_100.0) return
        pressureHpa = hectopascals
        if (pressureBaselineHpa == null) pressureBaselineHpa = hectopascals
    }

    fun pushAccelerometer(x: Double, y: Double, z: Double, nowMs: Long): Edge? {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        val current = doubleArrayOf(x / GRAVITY, y / GRAVITY, z / GRAVITY)
        val previous = previousAccel
        previousAccel = current
        if (previous == null) {
            calibrationUntilMs = nowMs + CALIBRATION_MS
            return null
        }
        for (axis in 0..2) {
            highPass[axis] = HIGH_PASS_ALPHA * (highPass[axis] + current[axis] - previous[axis])
        }
        accelActivity += ACTIVITY_ALPHA * (norm(highPass) - accelActivity)
        gyroActivity += ACTIVITY_ALPHA * (norm(latestGyroDps) - gyroActivity)

        if (nowMs < calibrationUntilMs) {
            learnFloor(warm = true)
            learnPressure()
            return null
        }
        if (nowMs < suppressedUntilMs) return null

        val accelOnset = maxOf(ONSET_FLOOR_MULT * accelFloor, ONSET_ACCEL_MIN_G)
        val gyroOnset = maxOf(ONSET_FLOOR_MULT * gyroFloor, ONSET_GYRO_MIN_DPS)
        val accelRatio = accelActivity / accelOnset
        val gyroRatio = gyroActivity / gyroOnset
        val corroborated = accelRatio >= 0.72 && gyroRatio >= 0.72
        val strong = maxOf(accelRatio, gyroRatio) >= STRONG_RATIO
        val recentInteraction = nowMs - lastInteractionMs <= INTERACTION_WINDOW_MS
        val onset = if (hasGyroscope) {
            corroborated && (strong || !recentInteraction || maxOf(accelRatio, gyroRatio) >= INPUT_STRONG_RATIO)
        } else {
            // No gyro: require a materially stronger translational burst. The same
            // quiet held phase is still mandatory before declaring a pickup.
            accelRatio >= if (recentInteraction) INPUT_STRONG_RATIO else STRONG_RATIO
        }

        if (!airborne) {
            val vibrating = accelFloor >= VIBRATING_ACCEL_G &&
                (!hasGyroscope || gyroFloor >= VIBRATING_GYRO_DPS)
            val handling = if (hasGyroscope) {
                (accelActivity / accelFloor >= HANDLING_RATIO && gyroActivity / gyroFloor >= 1.3) ||
                    (gyroActivity / gyroFloor >= HANDLING_RATIO && accelActivity / accelFloor >= 1.3)
            } else {
                accelActivity / accelFloor >= HANDLING_RATIO
            }
            handlingMemory = if (handling) 25 else (handlingMemory - 1).coerceAtLeast(0)

            if (vibrating) {
                val isolated = accelActivity <= accelFloor * LIFT_OFF_QUIET_RATIO &&
                    (!hasGyroscope || gyroActivity <= gyroFloor * LIFT_OFF_QUIET_RATIO) &&
                    handlingMemory > 0
                liftOffQuietStreak = if (isolated) liftOffQuietStreak + 1 else 0
                if (liftOffQuietStreak >= LIFT_OFF_STREAK) return startAirborne(nowMs)
            }

            if (candidateStartedAtMs != 0L) {
                val held = accelActivity <= accelOnset * 0.92 &&
                    (!hasGyroscope || gyroActivity <= gyroOnset * 0.92)
                confirmQuietStreak = if (held) confirmQuietStreak + 1 else 0
                val heightConfirmed = relativeHeightMetres() >= HEIGHT_CONFIRM_METRES
                if (confirmQuietStreak >= CONFIRM_QUIET_STREAK || (heightConfirmed && confirmQuietStreak >= 2)) {
                    return startAirborne(nowMs)
                }
                if (nowMs - candidateStartedAtMs >= CANDIDATE_TIMEOUT_MS) {
                    calibrationUntilMs = nowMs + CALIBRATION_MS / 2
                    candidateStartedAtMs = 0L
                    confirmQuietStreak = 0
                }
                return null
            }

            onsetStreak = if (onset) onsetStreak + 1 else 0
            if (onsetStreak >= ONSET_STREAK) {
                candidateStartedAtMs = nowMs
                onsetStreak = 0
                confirmQuietStreak = 0
                return null
            }
            if (!onset && !handling) {
                learnFloor(warm = false)
                learnPressure()
            }
            return null
        }

        candidateAccelFloor += 0.01 * (accelActivity - candidateAccelFloor)
        candidateGyroFloor += 0.01 * (gyroActivity - candidateGyroFloor)
        val accelCalm = accelActivity <= maxOf(SETTLE_FLOOR_MULT * accelFloor, SETTLE_ACCEL_MIN_G)
        val gyroCalm = gyroActivity <= maxOf(SETTLE_FLOOR_MULT * gyroFloor, SETTLE_GYRO_MIN_DPS)
        val calm = accelCalm && (!hasGyroscope || gyroCalm)
        calmSinceMs = when {
            !calm -> 0L
            calmSinceMs == 0L -> nowMs
            else -> calmSinceMs
        }
        val airborneFor = nowMs - airborneSinceMs
        val height = relativeHeightMetres()
        val heightAllowsLanding = !hasBarometer || pressureHpa == null || height <= HEIGHT_LANDED_METRES
        if (
            airborneFor >= MIN_AIRBORNE_MS && calmSinceMs != 0L &&
            nowMs - calmSinceMs >= SETTLE_MS && heightAllowsLanding
        ) {
            return settle(nowMs, rebase = false)
        }
        if (airborneFor >= NEW_SURFACE_MS && calmSinceMs != 0L && nowMs - calmSinceMs >= SETTLE_MS * 2) {
            return settle(nowMs, rebase = true)
        }
        if (nowMs - lastRefireMs >= REFIRE_MS) {
            lastRefireMs = nowMs
            return Edge.REFIRE
        }
        return null
    }

    internal fun isAirborne(): Boolean = airborne
    internal fun learnedAccelFloor(): Double = accelFloor
    internal fun learnedGyroFloor(): Double = gyroFloor
    internal fun relativeHeightMetres(): Double {
        val current = pressureHpa ?: return 0.0
        val baseline = pressureBaselineHpa ?: return 0.0
        return 44_330.0 * (1.0 - (current / baseline).pow(0.1903))
    }

    private fun startAirborne(nowMs: Long): Edge {
        airborne = true
        airborneSinceMs = nowMs
        lastRefireMs = nowMs
        calmSinceMs = 0L
        candidateStartedAtMs = 0L
        confirmQuietStreak = 0
        onsetStreak = 0
        candidateAccelFloor = accelActivity.coerceAtLeast(ACCEL_FLOOR_MIN_G)
        candidateGyroFloor = gyroActivity.coerceAtLeast(GYRO_FLOOR_MIN_DPS)
        return Edge.LIFTED
    }

    private fun settle(nowMs: Long, rebase: Boolean): Edge {
        airborne = false
        if (rebase) {
            accelFloor = candidateAccelFloor.coerceIn(ACCEL_FLOOR_MIN_G, ACCEL_FLOOR_MAX_G)
            gyroFloor = candidateGyroFloor.coerceIn(GYRO_FLOOR_MIN_DPS, GYRO_FLOOR_MAX_DPS)
            pressureBaselineHpa = pressureHpa
        }
        calibrationUntilMs = nowMs + POST_SETTLE_CALIBRATION_MS
        calmSinceMs = 0L
        liftOffQuietStreak = 0
        handlingMemory = 0
        return Edge.LANDED
    }

    private fun learnFloor(warm: Boolean) {
        val accelAlpha = if (warm) WARM_FLOOR_ALPHA else if (accelActivity > accelFloor) FLOOR_RISE_ALPHA else FLOOR_FALL_ALPHA
        val gyroAlpha = if (warm) WARM_FLOOR_ALPHA else if (gyroActivity > gyroFloor) FLOOR_RISE_ALPHA else FLOOR_FALL_ALPHA
        accelFloor = (accelFloor + accelAlpha * (accelActivity - accelFloor)).coerceIn(ACCEL_FLOOR_MIN_G, ACCEL_FLOOR_MAX_G)
        gyroFloor = (gyroFloor + gyroAlpha * (gyroActivity - gyroFloor)).coerceIn(GYRO_FLOOR_MIN_DPS, GYRO_FLOOR_MAX_DPS)
    }

    private fun learnPressure() {
        val current = pressureHpa ?: return
        val baseline = pressureBaselineHpa
        pressureBaselineHpa = if (baseline == null) current else baseline + 0.004 * (current - baseline)
    }

    private fun norm(v: DoubleArray): Double = sqrt(v.sumOf { it * it })
}
