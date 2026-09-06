package com.aster.service.execution

import com.aster.data.model.Command
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.security.MessageDigest

/** Exact-byte, additive device execution lane. No transport identifier is authority. */
internal object ExecutionProtocol {
    const val VERSION = 2
    const val MAX_PAYLOAD_BYTES = 262_144
    const val MAX_RESULT_BYTES = 262_144
    const val MAX_RETAINED_RESULT_BYTES = 67_108_864L
    val json = Json { ignoreUnknownKeys = false; encodeDefaults = true }
    val supportedActions = setOf(
        "tap", "set_text", "long_press", "set_toggle", "perform", "scroll",
        "input_gesture", "press_key", "global_action", "input_text",
        "click_by_text", "click_by_view_id", "launch_intent",
        "screen_prompt", "screen_approve", "screen_signin_wait", "screen_handoff",
    )
    private val ulid = Regex("[0-7][0-9A-HJKMNP-TV-Z]{25}")
    private val digest = Regex("[0-9a-f]{64}")
    private fun bounded(value: String, max: Int) =
        value.isNotBlank() && value.toByteArray(Charsets.UTF_8).size <= max && value.none { it.isISOControl() }

    fun validate(query: ExecutionQuery) {
        val r = query.request
        require(r.protocol == VERSION && ulid.matches(r.request_id) && ulid.matches(r.workspace_id))
        require(r.created_at_ms > 0 && digest.matches(r.payload_hash))
        require(r.requested_device_id == null || bounded(r.requested_device_id, 256))
        require(bounded(query.target.device_id, 256) && bounded(query.target.adapter_incarnation, 128))
        require(bounded(query.target.companion_id, 128) && bounded(query.target.boot_id, 128))
    }

    fun decodeQuery(text: String): ExecutionQuery {
        require(text.toByteArray(Charsets.UTF_8).size <= 4096)
        return json.decodeFromString<ExecutionQuery>(text).also(::validate)
    }

    fun decodeSubmit(text: String): Pair<ExecutionQuery, Command> {
        // Escaping may enlarge the envelope; the decoded exact payload owns its
        // independent byte bound below. Neither hash nor body is reconstructed.
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES * 6 + 4096)
        val submit = json.decodeFromString<ExecutionSubmit>(text)
        val r = submit.request
        val query = ExecutionQuery(r.identity(), submit.target).also(::validate)
        require(r.payload_json.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES)
        require(sha256(r.payload_json) == r.payload_hash)
        val payload = json.decodeFromString<ExecutionPayload>(r.payload_json)
        require(bounded(payload.action, 128))
        return query to Command("command", r.request_id, payload.action, payload.params)
    }

    fun supported(command: Command): Boolean = command.action in supportedActions &&
        // Only package launch is audited here. An arbitrary intent may start
        // unrelated external work with no completion semantics for this lane.
        (command.action != "launch_intent" ||
            (command.params?.get("package") != null &&
                listOf("action", "data", "extras").none { command.params?.containsKey(it) == true }))

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

    fun encode(value: ExecutionObservation): String = json.encodeToString(value)
}

@Serializable
internal data class ExecutionIdentity(
    val protocol: Int,
    val request_id: String,
    val workspace_id: String,
    val requested_device_id: String?,
    val payload_hash: String,
    val created_at_ms: Long,
)

@Serializable
internal data class ExecutionRequest(
    val protocol: Int,
    val request_id: String,
    val workspace_id: String,
    val requested_device_id: String?,
    val payload_json: String,
    val payload_hash: String,
    val created_at_ms: Long,
) {
    fun identity() = ExecutionIdentity(protocol, request_id, workspace_id,
        requested_device_id, payload_hash, created_at_ms)
}

@Serializable
internal data class ExecutionTarget(
    val device_id: String,
    val adapter_incarnation: String,
    val companion_id: String,
    val boot_id: String,
)

@Serializable
internal data class ExecutionQuery(val request: ExecutionIdentity, val target: ExecutionTarget)

@Serializable
internal data class ExecutionSubmit(val request: ExecutionRequest, val target: ExecutionTarget)

@Serializable
internal data class ExecutionPayload(val action: String, val params: JsonObject)

@Serializable
internal data class ExecutionObservation(
    val protocol: Int = 2,
    val request_id: String,
    val workspace_id: String,
    val requested_device_id: String?,
    val payload_hash: String,
    val created_at_ms: Long,
    val target: ExecutionTarget,
    val state: String,
    val observed_at_ms: Long,
    val closed_at_ms: Long? = null,
    val result_json: String? = null,
    val result_hash: String? = null,
    val result_available: Boolean = false,
)

@Serializable
internal data class ExecutionResult(val success: Boolean, val data: JsonElement?, val error: String?)

@Serializable
internal data class ExecutionCapabilities(
    val protocol: Int = 2,
    val companion_id: String,
    val boot_id: String,
    val supported_actions: List<String> = ExecutionProtocol.supportedActions.sorted(),
    val max_payload_bytes: Int = ExecutionProtocol.MAX_PAYLOAD_BYTES,
    val max_result_bytes: Int = ExecutionProtocol.MAX_RESULT_BYTES,
    val max_active: Int = 1,
    val max_retained_result_bytes: Long = ExecutionProtocol.MAX_RETAINED_RESULT_BYTES,
    val unresolved: Boolean,
)
