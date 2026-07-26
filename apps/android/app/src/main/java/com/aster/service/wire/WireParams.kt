package com.aster.service.wire

import kotlinx.serialization.json.JsonElement

/**
 * Cross-repo parameter-name normalisation for the `device.execute` wire.
 *
 * The kernel (`aster-one`, `cortex-tools-screen`) emits **snake_case** params;
 * several companion handlers historically read **camelCase**. Because
 * [com.aster.data.model.Command.params] is an untyped `Map<String, JsonElement>`
 * and every handler does `params?.get("someKey") ?: default`, a key spelled the
 * other way was never an error — it was silently dropped, and the action ran with
 * a default. That cost us, concretely:
 *
 *  - `search_text` / `max_elements` / `window` on `observe` — narrowing inoperative,
 *    so every observe returned the full element budget.
 *  - `until_text` on `scroll` — scroll-to-find degraded to a single swipe.
 *  - `quiet_ms` on `wait_for_idle` — the tuned settle window fell back to the default.
 *  - `view_id` on `wait_for` — a HARD failure ("wait_for requires at least one
 *    target"), which is what the preloaded LinkedIn flow's wait steps are built from.
 *
 * Rather than rename ~20 call sites across two independently-released APKs (where a
 * rename lands on only one side until both ship), we normalise **once** at every
 * point a `Command` is constructed: each key is published under BOTH spellings, so
 * a handler resolves it whichever convention it reads. An explicitly-present key is
 * never overwritten, so a caller that supplies both wins on its own spelling.
 *
 * This is deliberately a compatibility shim, not the contract. The contract is the
 * checked-in wire manifest and its two-sided tests; this keeps old kernels and old
 * companions interoperating while that lands.
 *
 * Pure Kotlin (no Android types) so it is unit-testable headless.
 */
object WireParams {

    /**
     * Expand [params] so every key is reachable under both its snake_case and
     * camelCase spelling. Returns the input unchanged when there is nothing to add.
     */
    fun normalize(params: Map<String, JsonElement>?): Map<String, JsonElement>? {
        if (params.isNullOrEmpty()) return params
        var out: MutableMap<String, JsonElement>? = null
        for ((key, value) in params) {
            for (alias in aliasesOf(key)) {
                if (alias == key || params.containsKey(alias)) continue
                val target = out ?: LinkedHashMap(params).also { out = it }
                // `putIfAbsent` semantics: two source keys can alias onto the same
                // name (e.g. both `view_id` and `viewId` present is handled by the
                // containsKey guard above; this covers alias-vs-alias collisions).
                if (!target.containsKey(alias)) target[alias] = value
            }
        }
        return out ?: params
    }

    /**
     * The alternate spellings of [key]. A key with neither an underscore nor an
     * interior capital (e.g. `text`, `role`, `gone`) has none.
     */
    private fun aliasesOf(key: String): List<String> = when {
        key.contains('_') -> listOf(toCamelCase(key))
        key.any { it.isUpperCase() } -> listOf(toSnakeCase(key))
        else -> emptyList()
    }

    /** `search_text` → `searchText`. Repeated/trailing underscores are preserved as-is. */
    fun toCamelCase(key: String): String {
        if (!key.contains('_')) return key
        val sb = StringBuilder(key.length)
        var upperNext = false
        for (c in key) {
            if (c == '_') {
                // A leading or doubled underscore has no letter to fold into; keep
                // it so the alias stays distinguishable rather than colliding.
                if (sb.isEmpty() || upperNext) sb.append('_') else upperNext = true
            } else if (upperNext) {
                sb.append(c.uppercaseChar())
                upperNext = false
            } else {
                sb.append(c)
            }
        }
        if (upperNext) sb.append('_')
        return sb.toString()
    }

    /** `searchText` → `search_text`. Runs of capitals fold one-for-one. */
    fun toSnakeCase(key: String): String {
        val sb = StringBuilder(key.length + 4)
        for ((i, c) in key.withIndex()) {
            if (c.isUpperCase()) {
                if (i > 0) sb.append('_')
                sb.append(c.lowercaseChar())
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }
}
