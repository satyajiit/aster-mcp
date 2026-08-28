package com.aster.service.overlay

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** A validated, bounded readout painted in the Android notch. */
data class CompanionStatusModel(
    val mode: String,
    val text: String,
    val detail: String?,
    val icon: String?,
    val hue: String?,
    val progress: Float?,
    val fx: String?,
    /** Monotonic deadline; 0 means persistent until explicitly replaced/cleared. */
    val expiresAtMs: Long = 0L,
    /** Wall-clock deadline for a native-maintained full countdown (Pomodoro). */
    val countdownEndsAtEpochMs: Long? = null,
    val countdownTotalSeconds: Int? = null,
    /** Delayed visibility preserves a short face-only Pomodoro interlude. */
    val visibleAfterEpochMs: Long? = null,
)

/** Distinguishes a valid wire `null` (clear) from malformed input (drop). */
data class CompanionStatusParseResult(
    val valid: Boolean,
    val status: CompanionStatusModel?,
)

private val SAFE_ICON = Regex("^[a-z][a-zA-Z0-9-]{0,31}$")
private val SAFE_HEX = Regex("^#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$")

/** Parse untrusted Binder JSON without Android framework calls, so it is JVM-testable. */
fun parseCompanionStatus(
    json: String,
    nowMs: Long,
    wallNowMs: Long = System.currentTimeMillis(),
): CompanionStatusParseResult = runCatching {
    val element = Json.parseToJsonElement(json)
    if (element.toString() == "null") return@runCatching CompanionStatusParseResult(true, null)
    val o = element.jsonObject
    val text = o["text"]?.jsonPrimitive?.content?.trim()?.take(80)
        ?: return@runCatching CompanionStatusParseResult(false, null)
    val wireMode = o.string("mode").takeIf { it == "full" } ?: "side"
    val progress = o["progress"]?.jsonPrimitive?.doubleOrNull
        ?.takeIf(Double::isFinite)
        ?.coerceIn(0.0, 1.0)
        ?.toFloat()
    val icon = o.string("icon")?.takeIf(SAFE_ICON::matches)
    val hue = o.string("hue")?.takeIf(SAFE_HEX::matches)
    val fx = o.string("fx")?.takeIf { it == "eq" || it == "spin" }
    val rawCountdownEnd = o["countdownEndsAtMs"]?.jsonPrimitive?.longOrNull
        ?.takeIf { it in (wallNowMs - 86_400_000L)..(wallNowMs + 86_400_000L) }
    val revealAt = o["countdownRevealAtMs"]?.jsonPrimitive?.longOrNull
        ?.takeIf {
            rawCountdownEnd != null &&
                it in (wallNowMs - 5_000L)..(wallNowMs + 60_000L) &&
                it <= rawCountdownEnd
        }
    val hiddenCountdown = text.isEmpty() && revealAt != null
    if (text.isEmpty() && !hiddenCountdown) {
        return@runCatching CompanionStatusParseResult(true, null)
    }
    val mode = if (hiddenCountdown) "full" else wireMode
    val countdownEnd = rawCountdownEnd?.takeIf { mode == "full" }
    val countdownTotal = o["countdownTotalSeconds"]?.jsonPrimitive?.longOrNull
        ?.takeIf { countdownEnd != null && it in 1L..86_400L }
        ?.toInt()
    if (hiddenCountdown && countdownTotal == null) {
        return@runCatching CompanionStatusParseResult(false, null)
    }
    // Side readouts are moments and must never freeze forever if the OpenAlly JS
    // clock is suspended immediately after pushing one. Full mode is a deliberate
    // takeover (Pomodoro) and persists until replaced/cleared.
    val expiry = if (mode == "side") nowMs + 8_000L else 0L
    CompanionStatusParseResult(
        valid = true,
        status = CompanionStatusModel(
            mode = mode,
            text = text,
            detail = o.string("detail")?.take(120),
            icon = icon,
            hue = hue,
            progress = progress,
            fx = fx,
            expiresAtMs = expiry,
            countdownEndsAtEpochMs = countdownEnd,
            countdownTotalSeconds = countdownTotal,
            visibleAfterEpochMs = revealAt,
        ),
    )
}.getOrElse { CompanionStatusParseResult(false, null) }

private fun JsonObject.string(key: String): String? =
    this[key]?.jsonPrimitive?.content?.trim()?.takeIf(String::isNotEmpty)
