package com.aster.service.execution

import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class ExecutionProtocolTest {
    private fun envelope(payload: String): ExecutionSubmit = ExecutionSubmit(
        ExecutionRequest(2, "01ARZ3NDEKTSV4RRFFQ69G5FAV", "01ARZ3NDEKTSV4RRFFQ69G5FAW", null,
            payload, ExecutionProtocol.sha256(payload), 123),
        ExecutionTarget("phone", "adapter-original", "companion", "boot"),
    )

    @Test fun exactUtf8BytesAreHashedWithoutReserializingPayload() {
        val original = envelope("{\"params\": {\"text\":\"नमस्ते\"}, \"action\":\"set_text\"}")
        val (query, command) = ExecutionProtocol.decodeSubmit(ExecutionProtocol.json.encodeToString(original))
        assertEquals(original.request.payload_hash, query.request.payload_hash)
        assertEquals("set_text", command.action)
        val changed = original.copy(request = original.request.copy(payload_json = original.request.payload_json + " "))
        assertTrue(runCatching { ExecutionProtocol.decodeSubmit(ExecutionProtocol.json.encodeToString(changed)) }.isFailure)
    }

    @Test fun unknownEnvelopeAndPayloadFieldsCannotCreateAnotherAuthorityLane() {
        val original = envelope("{\"action\":\"tap\",\"params\":{},\"approved\":true}")
        assertTrue(runCatching { ExecutionProtocol.decodeSubmit(ExecutionProtocol.json.encodeToString(original)) }.isFailure)
        val valid = ExecutionProtocol.json.encodeToString(envelope("{\"action\":\"tap\",\"params\":{}}"))
        assertTrue(runCatching { ExecutionProtocol.decodeSubmit(valid.dropLast(1) + ",\"trusted\":true}") }.isFailure)
    }

    @Test fun onlyPackageLaunchAndAuditedActionsAreAdvertised() {
        fun allowed(body: String) = ExecutionProtocol.supported(ExecutionProtocol.decodeSubmit(
            ExecutionProtocol.json.encodeToString(envelope(body)),
        ).second)
        assertTrue(allowed("{\"action\":\"launch_intent\",\"params\":{\"package\":\"org.example\"}}"))
        assertFalse(allowed("{\"action\":\"launch_intent\",\"params\":{\"package\":\"org.example\",\"data\":\"secret\"}}"))
        assertFalse(allowed("{\"action\":\"execute_shell\",\"params\":{}}"))
        assertFalse(allowed("{\"action\":\"screen_approve\",\"params\":{}}"))
    }

    @Test fun byteBoundAppliesToDecodedUnicodePayload() {
        val original = envelope("{\"action\":\"set_text\",\"params\":{\"text\":\"" + "é".repeat(140_000) + "\"}}")
        assertTrue(runCatching { ExecutionProtocol.decodeSubmit(ExecutionProtocol.json.encodeToString(original)) }.isFailure)
    }

    @Test fun queryRequiresCompleteOriginalIdentityAndDoesNotAcceptPayload() {
        val request = envelope("{\"action\":\"tap\",\"params\":{}}")
        val query = ExecutionQuery(request.request.identity(), request.target)
        assertEquals(query, ExecutionProtocol.decodeQuery(ExecutionProtocol.json.encodeToString(query)))
        assertTrue(runCatching { ExecutionProtocol.decodeQuery(ExecutionProtocol.json.encodeToString(request)) }.isFailure)
        assertTrue(runCatching { ExecutionProtocol.validate(query.copy(request = query.request.copy(workspace_id = "other"))) }.isFailure)
    }
}
