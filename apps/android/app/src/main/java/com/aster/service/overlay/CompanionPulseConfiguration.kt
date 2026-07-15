package com.aster.service.overlay

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Immutable, whitelist-only policy received from OpenAlly for Aster-owned
 * background reactions. Unknown/future lanes are ignored, never promoted to
 * permissions, and malformed payloads leave the last known-good policy intact.
 */
data class CompanionPulseConfiguration(
    val enabled: Boolean,
    val lanes: Set<String>,
) {
    fun allows(lane: String): Boolean = enabled && lane in lanes

    companion object {
        /** Fail closed across APK version skew until OpenAlly sends explicit policy. */
        val NONE = CompanionPulseConfiguration(false, emptySet())
    }
}

/** Only signals this process can currently render without the JS engine. */
val NATIVE_PULSE_LANES: Set<String> = setOf(
    "power",
    "lift",
    "shake",
    "foregroundApp",
    "interaction",
    "scroll",
    "typing",
    "notification",
)

fun parseCompanionPulseConfiguration(json: String): CompanionPulseConfiguration? = runCatching {
    val objectValue = Json.parseToJsonElement(json).jsonObject
    val enabled = objectValue["enabled"]?.jsonPrimitive?.booleanOrNull ?: return@runCatching null
    val lanes = objectValue["lanes"]?.jsonArray
        ?.mapNotNull { element ->
            runCatching { element.jsonPrimitive.content }.getOrNull()
                ?.takeIf(NATIVE_PULSE_LANES::contains)
        }
        ?.toSet()
        ?: return@runCatching null
    CompanionPulseConfiguration(enabled, lanes)
}.getOrNull()
