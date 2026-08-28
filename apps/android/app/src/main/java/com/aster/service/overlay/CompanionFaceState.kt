package com.aster.service.overlay

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Text-free mouth families accepted from OpenAlly's versioned state lane. */
enum class CompanionViseme {
    REST,
    CLOSED,
    OPEN,
    WIDE,
    ROUND,
    TEETH,
    TONGUE,
    NEUTRAL,
}

enum class CompanionSpeechSource { STREAM, AUDIO }

data class CompanionSpeechStream(
    val id: String,
    val source: CompanionSpeechSource,
    val level: Float,
    val viseme: CompanionViseme,
    val ttlMs: Long,
    val reducedMotion: Boolean,
)

data class CompanionFaceStateSnapshot(
    val version: Int,
    val session: String,
    val sequence: Long,
    val speech: List<CompanionSpeechStream>,
)

data class CompanionSpeechAggregate(
    val cue: CompanionSpeechStream,
    val cueExpiresAtMs: Long,
    val nextExpiryAtMs: Long,
    val reducedMotion: Boolean,
)

private const val SPEECH_PROTOCOL_VERSION = 1
private const val MAX_SPEECH_STREAMS = 8
private const val MAX_STREAM_ID_CHARS = 96
private const val MAX_SESSION_ID_CHARS = 64
private const val MIN_SPEECH_TTL_MS = 1_000L
private const val MAX_SPEECH_TTL_MS = 60_000L
private val STREAM_ID = Regex("^[A-Za-z0-9:._-]{1,$MAX_STREAM_ID_CHARS}$")
private val SESSION_ID = Regex("^[A-Za-z0-9:._-]{1,$MAX_SESSION_ID_CHARS}$")
private val ROOT_KEYS = setOf("version", "session", "sequence", "speech")
private val SPEECH_KEYS = setOf("id", "source", "level", "viseme", "ttlMs", "reducedMotion")

/**
 * Strictly parse one complete semantic state snapshot.
 *
 * This is an authenticated Binder lane but still an inter-process boundary: reject
 * unknown fields, duplicate IDs, invalid enum strings, non-finite values and every
 * size/time outside the protocol bounds. A rejected packet leaves the last good
 * state untouched.
 */
fun parseCompanionFaceState(json: String): CompanionFaceStateSnapshot? = runCatching {
    val root = Json.parseToJsonElement(json) as? JsonObject ?: error("state root must be object")
    require(root.keys == ROOT_KEYS)
    val versionLong = root.strictLong("version")
    require(versionLong == SPEECH_PROTOCOL_VERSION.toLong())
    val version = versionLong.toInt()
    val session = root.strictString("session")
    require(SESSION_ID.matches(session))
    val sequence = root.strictLong("sequence")
    require(sequence > 0L)
    val speechJson = root["speech"] as? JsonArray ?: error("speech must be array")
    require(speechJson.size <= MAX_SPEECH_STREAMS)

    val ids = HashSet<String>()
    val speech = ArrayList<CompanionSpeechStream>(speechJson.size)
    for (element in speechJson) {
        val item = element as? JsonObject ?: error("speech item must be object")
        require(item.keys == SPEECH_KEYS)
        val idValue = item["id"] as? JsonPrimitive ?: error("id must be string")
        require(idValue.isString)
        val id = idValue.content
        require(STREAM_ID.matches(id) && ids.add(id))
        val source = when (item.strictString("source")) {
            "stream" -> CompanionSpeechSource.STREAM
            "audio" -> CompanionSpeechSource.AUDIO
            else -> error("invalid speech source")
        }
        val level = item["level"]?.jsonPrimitive?.doubleOrNull ?: error("invalid level")
        require(level.isFinite() && level in 0.0..1.0)
        val viseme = when (item.strictString("viseme")) {
            "rest" -> CompanionViseme.REST
            "closed" -> CompanionViseme.CLOSED
            "open" -> CompanionViseme.OPEN
            "wide" -> CompanionViseme.WIDE
            "round" -> CompanionViseme.ROUND
            "teeth" -> CompanionViseme.TEETH
            "tongue" -> CompanionViseme.TONGUE
            "neutral" -> CompanionViseme.NEUTRAL
            else -> error("invalid viseme")
        }
        val ttlMs = item.strictLong("ttlMs")
        require(ttlMs in MIN_SPEECH_TTL_MS..MAX_SPEECH_TTL_MS)
        val reducedMotion = item["reducedMotion"]?.jsonPrimitive?.booleanOrNull
            ?: error("reducedMotion must be boolean")
        speech += CompanionSpeechStream(
            id = id,
            source = source,
            level = level.toFloat(),
            viseme = viseme,
            ttlMs = ttlMs,
            reducedMotion = reducedMotion,
        )
    }
    CompanionFaceStateSnapshot(version, session, sequence, speech)
}.getOrNull()

/** Select the current source without extending its cue using another source's TTL. */
fun aggregateCompanionSpeech(
    snapshot: CompanionFaceStateSnapshot,
    receivedAtMs: Long,
    nowMs: Long,
): CompanionSpeechAggregate? {
    val active = snapshot.speech.filter { receivedAtMs + it.ttlMs > nowMs }
    if (active.isEmpty()) return null
    val cue = active.maxBy { it.level }
    return CompanionSpeechAggregate(
        cue = cue,
        cueExpiresAtMs = receivedAtMs + cue.ttlMs,
        nextExpiryAtMs = active.minOf { receivedAtMs + it.ttlMs },
        reducedMotion = active.any { it.reducedMotion },
    )
}

private fun JsonObject.strictLong(key: String): Long {
    val primitive = this[key] as? JsonPrimitive ?: error("$key must be a number")
    require(!primitive.isString)
    return primitive.longOrNull ?: error("$key must be an integer")
}

private fun JsonObject.strictString(key: String): String {
    val primitive = this[key] as? JsonPrimitive ?: error("$key must be a string")
    require(primitive.isString)
    return primitive.content
}
