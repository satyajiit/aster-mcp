package com.aster.service.overlay

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/** Native reactions used when the OpenAlly JS render clock is background-suspended. */
enum class CompanionReaction {
    CHARGE,
    UNPLUG,
    LOW_BATTERY,
    SHAKE,
    LIFT,
    LAND,
    BOOP,
    SCROLL,
    TYPING,
    PING,
    CURIOUS,
}

data class CompanionMotionFrame(
    val dx: Float = 0f,
    val dy: Float = 0f,
    val rotationDeg: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val eyeScaleY: Float = 1f,
    val mouthScaleX: Float = 1f,
    val mouthScaleY: Float = 1f,
    val mouthRotationDeg: Float = 0f,
    val autonomous: Boolean = false,
    val decoration: CompanionReaction? = null,
)

/**
 * Small deterministic motion engine owned by Aster's process.
 *
 * Remote geometry remains the source of pose/shape truth. This layer activates only
 * after that stream goes quiet or for a native system reaction, adding squash/stretch,
 * blink, gaze, bounce and decoration. Thus minimizing OpenAlly no longer freezes a
 * screenshot, while foreground gestures remain pixel-identical to the shared rig.
 */
class CompanionNativeAnimator {
    companion object {
        const val REMOTE_FRESH_MS = 420L
    }

    private var reaction: CompanionReaction? = null
    private var reactionStartedAt = 0L
    private var reactionEndsAt = 0L
    private var speaking = false
    private var speechEnergy = 0.52f
    private var speechViseme = CompanionViseme.NEUTRAL
    private var speechExpiresAt = 0L
    private var speechStartedAt = 0L
    private var speechReducedMotion = false

    fun react(next: CompanionReaction, nowMs: Long, holdMs: Long = durationOf(next)) {
        reaction = next
        reactionStartedAt = nowMs
        reactionEndsAt = if (next == CompanionReaction.LIFT) Long.MAX_VALUE else nowMs + holdMs
    }

    fun land(nowMs: Long) {
        react(CompanionReaction.LAND, nowMs)
    }

    fun clear() {
        clearReaction()
        clearSpeech()
    }

    fun setSpeaking(
        active: Boolean,
        nowMs: Long,
        energy: Float = 0.52f,
        viseme: CompanionViseme = CompanionViseme.NEUTRAL,
        expiresAtMs: Long = Long.MAX_VALUE,
        reducedMotion: Boolean = false,
    ) {
        if (!active || expiresAtMs <= nowMs) {
            clearSpeech()
            return
        }
        if (!speaking) speechStartedAt = nowMs
        speaking = true
        // Smooth token discontinuities without hiding punctuation/closed-lip cues.
        speechEnergy = (speechEnergy * 0.28f + energy.coerceIn(0f, 1f) * 0.72f)
        speechViseme = viseme
        speechExpiresAt = expiresAtMs
        speechReducedMotion = reducedMotion
    }

    private fun clearReaction() {
        reaction = null
        reactionStartedAt = 0L
        reactionEndsAt = 0L
    }

    private fun clearSpeech() {
        speaking = false
        speechEnergy = 0.52f
        speechViseme = CompanionViseme.NEUTRAL
        speechExpiresAt = 0L
        speechStartedAt = 0L
        speechReducedMotion = false
    }

    fun sample(nowMs: Long, lastRemoteFrameAtMs: Long): CompanionMotionFrame {
        if (reactionEndsAt != Long.MAX_VALUE && nowMs >= reactionEndsAt) clearReaction()
        if (speaking && nowMs >= speechExpiresAt) clearSpeech()
        val active = reaction
        if (active != null) return reactionFrame(active, nowMs)
        if (nowMs - lastRemoteFrameAtMs <= REMOTE_FRESH_MS) return CompanionMotionFrame()
        if (speaking) return speakingFrame(nowMs)
        return ambientFrame(nowMs)
    }

    /** Native talking fallback over the last shared geometry pose. */
    private fun speakingFrame(nowMs: Long): CompanionMotionFrame {
        val elapsed = (nowMs - speechStartedAt).coerceAtLeast(0L) / 1_000f
        val fast = abs(sin(elapsed * PI.toFloat() * 2f * 5.4f))
        val texture = abs(sin(elapsed * PI.toFloat() * 2f * 2.15f + 0.9f))
        // Brief natural rests every few seconds. A live punctuation token also sends
        // REST, so foreground/background use the same closed-mouth semantics.
        val phrase = ((nowMs - speechStartedAt).coerceAtLeast(0L) % 3_200L) / 3_200f
        val phraseGate = when {
            phrase < 0.84f -> 1f
            phrase < 0.91f -> 1f - (phrase - 0.84f) / 0.07f
            else -> 0.08f + (phrase - 0.91f) / 0.09f * 0.92f
        }
        val articulation = (0.2f + fast * 0.58f + texture * 0.22f) * phraseGate
        val (shapeX, shapeY) = when (speechViseme) {
            CompanionViseme.REST -> 0.96f to 0.46f
            CompanionViseme.CLOSED -> 0.86f to 0.52f
            CompanionViseme.OPEN -> 0.91f to 1.16f
            CompanionViseme.WIDE -> 1.18f to 0.76f
            CompanionViseme.ROUND -> 0.76f to 1.02f
            CompanionViseme.TEETH -> 1.06f to 0.68f
            CompanionViseme.TONGUE -> 0.95f to 0.88f
            CompanionViseme.NEUTRAL -> 1f to 0.9f
        }
        val energy = 0.35f + speechEnergy * 0.65f
        val mouthY = (shapeY * (0.58f + articulation * 0.9f * energy)).coerceIn(0.34f, 1.9f)
        if (speechReducedMotion) {
            return CompanionMotionFrame(
                mouthScaleX = shapeX,
                mouthScaleY = mouthY,
                autonomous = true,
            )
        }
        return CompanionMotionFrame(
            dy = -0.35f + articulation * -0.7f,
            rotationDeg = sin(elapsed * PI.toFloat() * 2f * 0.72f) * 0.8f * energy,
            scaleX = 1f + articulation * 0.008f,
            scaleY = 1f - articulation * 0.006f,
            eyeScaleY = 0.92f + texture * 0.08f,
            mouthScaleX = shapeX * (1.04f - articulation * 0.08f),
            mouthScaleY = mouthY,
            mouthRotationDeg = sin(elapsed * PI.toFloat() * 2f * 1.1f) * 2.1f * energy,
            autonomous = true,
        )
    }

    private fun ambientFrame(nowMs: Long): CompanionMotionFrame {
        val seconds = nowMs / 1_000.0
        val breath = sin(seconds * PI * 0.72).toFloat()
        val longBeat = ((nowMs % 11_000L) / 11_000f)
        val curious = when {
            longBeat in 0.54f..0.67f -> (longBeat - 0.54f) / 0.13f
            longBeat in 0.67f..0.80f -> 1f - (longBeat - 0.67f) / 0.13f
            else -> 0f
        }
        val blinkPhase = nowMs % 4_600L
        val eyeScale = when (blinkPhase) {
            in 4_180L..4_245L -> 1f - (blinkPhase - 4_180L) / 65f * 0.88f
            in 4_246L..4_320L -> 0.12f + (blinkPhase - 4_246L) / 74f * 0.88f
            else -> 1f
        }
        return CompanionMotionFrame(
            dx = curious * 2.4f,
            dy = breath * 0.8f - curious * 0.7f,
            rotationDeg = curious * 2.8f,
            scaleX = 1f + breath * 0.006f,
            scaleY = 1f - breath * 0.006f,
            eyeScaleY = eyeScale.coerceIn(0.12f, 1f),
            mouthScaleY = 1f + breath * 0.025f,
            autonomous = true,
        )
    }

    private fun reactionFrame(active: CompanionReaction, nowMs: Long): CompanionMotionFrame {
        val elapsed = (nowMs - reactionStartedAt).coerceAtLeast(0L) / 1_000f
        val wave = { hz: Float -> sin(elapsed * PI.toFloat() * 2f * hz) }
        return when (active) {
            CompanionReaction.CHARGE -> {
                val bounce = abs(wave(2.2f)) * exp(-elapsed * 1.7f)
                CompanionMotionFrame(
                    dy = -4.5f * bounce,
                    rotationDeg = wave(1.7f) * 2.4f * exp(-elapsed * 1.4f),
                    scaleX = 1f + bounce * 0.05f,
                    scaleY = 1f - bounce * 0.035f,
                    eyeScaleY = 0.72f + bounce * 0.28f,
                    mouthScaleY = 1.18f,
                    autonomous = true,
                    decoration = active,
                )
            }
            CompanionReaction.UNPLUG -> CompanionMotionFrame(
                dx = wave(8f) * 2.8f * exp(-elapsed * 2.7f),
                rotationDeg = wave(6f) * 3.2f * exp(-elapsed * 2.2f),
                eyeScaleY = 1.22f,
                mouthScaleY = 1.35f,
                autonomous = true,
                decoration = active,
            )
            CompanionReaction.LOW_BATTERY -> CompanionMotionFrame(
                dy = 3.2f + wave(2.1f) * 0.8f,
                rotationDeg = -3.4f + wave(1.2f) * 1.2f,
                scaleX = 1.02f,
                scaleY = 0.96f,
                eyeScaleY = 0.48f,
                mouthScaleY = 0.72f,
                autonomous = true,
                decoration = active,
            )
            CompanionReaction.SHAKE -> CompanionMotionFrame(
                dx = wave(12f) * 4f * exp(-elapsed * 1.6f),
                dy = wave(8f) * 1.2f,
                rotationDeg = wave(10f) * 4.5f * exp(-elapsed * 1.3f),
                scaleX = 1.04f,
                scaleY = 0.96f,
                eyeScaleY = 1.25f,
                mouthScaleY = 1.45f,
                autonomous = true,
                decoration = active,
            )
            CompanionReaction.LIFT -> CompanionMotionFrame(
                dx = wave(13f) * 2f + wave(5f) * 0.8f,
                dy = -1.6f + abs(wave(3f)) * -0.8f,
                rotationDeg = wave(11f) * 2.5f,
                scaleX = 0.98f + abs(wave(6f)) * 0.025f,
                scaleY = 1.04f,
                eyeScaleY = 1.34f,
                mouthScaleY = 1.62f,
                autonomous = true,
                decoration = active,
            )
            CompanionReaction.LAND -> {
                val bounce = wave(3.2f) * exp(-elapsed * 2.4f)
                CompanionMotionFrame(
                    dy = abs(bounce) * 3.5f,
                    rotationDeg = bounce * 2.2f,
                    scaleX = 1f + abs(bounce) * 0.08f,
                    scaleY = 1f - abs(bounce) * 0.08f,
                    eyeScaleY = 0.62f,
                    mouthScaleY = 0.86f,
                    autonomous = true,
                    decoration = active,
                )
            }
            CompanionReaction.BOOP -> {
                val squash = abs(wave(3.8f)) * exp(-elapsed * 2.3f)
                CompanionMotionFrame(
                    dy = squash * 3f,
                    scaleX = 1f + squash * 0.13f,
                    scaleY = 1f - squash * 0.15f,
                    eyeScaleY = 0.34f + squash * 0.2f,
                    mouthScaleY = 0.7f,
                    autonomous = true,
                    decoration = active,
                )
            }
            CompanionReaction.SCROLL -> CompanionMotionFrame(
                dy = wave(2.8f) * 3f,
                rotationDeg = wave(1.4f) * 2.6f,
                scaleX = 1.02f,
                scaleY = 0.98f,
                eyeScaleY = 1.1f,
                autonomous = true,
                decoration = active,
            )
            CompanionReaction.TYPING -> CompanionMotionFrame(
                dx = wave(4.8f) * 1.1f,
                dy = abs(wave(4.8f)) * 1.3f,
                rotationDeg = -1.8f,
                scaleX = 1.015f,
                scaleY = 0.985f,
                eyeScaleY = 0.74f,
                mouthScaleY = 0.82f,
                autonomous = true,
                decoration = active,
            )
            CompanionReaction.PING -> {
                val hop = abs(wave(3.6f)) * exp(-elapsed * 2f)
                CompanionMotionFrame(
                    dy = -5f * hop,
                    rotationDeg = wave(2.5f) * 3.4f * exp(-elapsed * 1.8f),
                    scaleX = 1f + hop * 0.06f,
                    scaleY = 1f - hop * 0.04f,
                    eyeScaleY = 1.16f,
                    mouthScaleY = 1.25f,
                    autonomous = true,
                    decoration = active,
                )
            }
            CompanionReaction.CURIOUS -> CompanionMotionFrame(
                dx = 2.7f,
                dy = -1.2f,
                rotationDeg = 5f + wave(1.5f),
                scaleX = 1.015f,
                scaleY = 0.985f,
                eyeScaleY = 1.12f,
                mouthScaleY = 0.9f,
                autonomous = true,
                decoration = active,
            )
        }
    }

    private fun durationOf(reaction: CompanionReaction): Long = when (reaction) {
        CompanionReaction.LIFT -> Long.MAX_VALUE
        CompanionReaction.LOW_BATTERY -> 4_800L
        CompanionReaction.TYPING -> 2_800L
        CompanionReaction.CURIOUS -> 2_600L
        else -> 2_200L
    }
}
