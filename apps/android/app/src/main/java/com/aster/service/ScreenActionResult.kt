package com.aster.service

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Shared result + error builders for ref-addressed screen actions (SPEC §3.4).
 *
 * Every ref-action returns `{ ok, resolved_by, ... }`; an unresolved ref returns the
 * structured `stale_ref` failure so the agent re-observes and never acts on a wrong node
 * (SPEC §5: "fail closed with a stale_ref → re-observe, never act on a wrong node").
 *
 * This file owns ONLY the result/error shape and the verify-before-act predicate.
 * It does NOT declare NodeDescriptor / the ref-cache — those belong to P1's
 * com.aster.service.accessibility package (NodeDescriptor + SnapshotCache).
 */
object ScreenActionResult {

    /** SPEC §3.1 re-resolution strategy names (exact wire values). */
    enum class ResolvedBy(val wire: String) {
        VIEW_ID("viewId"),
        TEXT_ROLE("text_role"),
        NEAREST_BOUNDS("nearest_bounds"),
        CENTER_TAP("center_tap")
    }

    /** Success result carrying { ok:true, resolved_by, ...extra }. */
    fun ok(
        resolvedBy: ResolvedBy,
        extra: JsonObjectBuilder.() -> Unit = {}
    ): CommandResult {
        val data: JsonElement = buildJsonObject {
            put("ok", true)
            put("resolved_by", resolvedBy.wire)
            extra()
        }
        return CommandResult.success(data)
    }

    /**
     * Structured stale-ref failure. The error string is a JSON object the agent can parse
     * to decide to re-observe. We keep it in `error` (not `data`) so existing transports
     * surface it as a failure across IPC / MCP / WS without special-casing.
     */
    fun staleRef(ref: String, snapshotId: String?): CommandResult {
        val payload = buildJsonObject {
            put("code", "stale_ref")
            put("ref", ref)
            put("snapshot_id", snapshotId?.let { JsonPrimitive(it) } ?: JsonNull)
            put("hint", "re-observe")
        }
        return CommandResult.failure(Json.encodeToString(JsonElement.serializer(), payload))
    }

    /**
     * Verify-before-act predicate (SPEC §5: "Re-resolution must verify the resolved node still
     * matches the cached descriptor before acting"). Pure — takes plain fields, NOT an
     * AccessibilityNodeInfo, so it is unit-testable without the Android runtime.
     *
     * Rules:
     *  - If both sides carry a viewId, they MUST be equal (a differing viewId is a hard mismatch).
     *  - If they carry equal viewIds, that is a strong match (text may legitimately have changed).
     *  - Otherwise (no viewId anchor), require text equality AND role-or-className equality.
     *
     * NOTE: P1's NodeDescriptor stores absence as the empty string "" (non-null fields). The
     * cached* params therefore arrive as "" when absent; this predicate treats "" identically
     * to null via [takeIf]/null-coalescing so it is robust to either convention.
     */
    fun descriptorMatches(
        cachedViewId: String?, nodeViewId: String?,
        cachedText: String?, nodeText: String?,
        cachedRole: String?, nodeRole: String?,
        cachedClassName: String?, nodeClassName: String?
    ): Boolean {
        val cv = cachedViewId?.takeIf { it.isNotEmpty() }
        val nv = nodeViewId?.takeIf { it.isNotEmpty() }
        if (cv != null && nv != null) {
            return cv == nv
        }
        if (cv != null && nv == null) {
            // Cached had a viewId, resolved node lost it → not confidently the same node.
            return false
        }
        // No viewId anchor on the cached side: fall back to text + role/className identity.
        val textOk = (cachedText ?: "") == (nodeText ?: "")
        val cr = cachedRole?.takeIf { it.isNotEmpty() }
        val cc = cachedClassName?.takeIf { it.isNotEmpty() }
        val roleOk = (cr != null && cr == nodeRole) ||
            (cc != null && cc == nodeClassName)
        return textOk && roleOk
    }
}
