package com.aster.service.wire

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Companion-side enforcement of the `device.execute` wire contract.
 *
 * The canonical declaration lives in `aster-one` at
 * `rust/cortex-kernel/crates/cortex-tools-screen/wire-manifest.json`; a byte copy
 * is checked in here as a test resource. The kernel side
 * (`cortex-tools-screen/src/wire_contract.rs`) asserts it never puts a param on
 * the wire that this file does not declare. **This** side asserts the mirror: that
 * every declared param is actually read by the handler that owns the action.
 *
 * That is the check that would have caught the whole class of defect we shipped:
 * the transport is an untyped map and every handler does `params?.get("x") ?: default`,
 * so a param the companion does not read is not an error — it is silently dropped
 * and the action runs with a default. `screen_gesture` was entirely dead,
 * scroll-to-find degraded to one swipe, and `wait_for {view_id}` hard-failed on
 * every call, all invisibly.
 *
 * The check is a source scan rather than a runtime probe because a handler's
 * accepted params are only observable by reading it: there is no schema to
 * introspect. It is exact about WHICH file must contain the read, so a param
 * migrating between handlers still trips it.
 */
class WireContractTest {

    /** Must match `CONTRACT_VERSION` in `cortex-tools-screen/src/wire_contract.rs`. */
    private val contractVersion = "1"

    /**
     * Which source file(s) own each action's params. A param counts as "read" if it
     * appears in any of its action's files, under either casing convention (both
     * sides widen keys on the wire — see [WireParams]).
     */
    private val actionSources = mapOf(
        "observe" to listOf("handlers/AccessibilityHandler.kt"),
        "take_screenshot" to listOf("handlers/AccessibilityHandler.kt"),
        "tap" to listOf("handlers/AccessibilityHandler.kt"),
        "long_press" to listOf("handlers/AccessibilityHandler.kt"),
        "set_text" to listOf("handlers/AccessibilityHandler.kt"),
        "set_toggle" to listOf("handlers/AccessibilityHandler.kt"),
        "perform" to listOf("handlers/AccessibilityHandler.kt"),
        "scroll" to listOf("handlers/AccessibilityHandler.kt"),
        "input_gesture" to listOf("handlers/AccessibilityHandler.kt"),
        "press_key" to listOf("handlers/AccessibilityHandler.kt"),
        "global_action" to listOf("handlers/AccessibilityHandler.kt"),
        "wait_for" to listOf("handlers/AccessibilityHandler.kt"),
        "wait_for_idle" to listOf("handlers/AccessibilityHandler.kt"),
        "launch_intent" to listOf("handlers/IntentHandler.kt"),
        "screen_prompt" to listOf(
            "handlers/InteractiveOverlayHandler.kt",
            "overlay/InteractiveOverlayModel.kt",
        ),
        "screen_approve" to listOf(
            "handlers/InteractiveOverlayHandler.kt",
            "overlay/InteractiveOverlayModel.kt",
        ),
        "screen_capability" to listOf("handlers/CapabilityHandler.kt"),
        "screen_signin_wait" to listOf("handlers/SignInWaitHandler.kt"),
        "screen_handoff" to listOf("handlers/SignInWaitHandler.kt"),
        "screen_set_policy" to listOf("handlers/PolicyHandler.kt"),
    )

    /**
     * Params every screen action carries as a kernel→companion side-channel rather
     * than as handler input: they are read once in [com.aster.service.mode.IpcMode]
     * for the P7 audit log, not by the action's own handler.
     */
    private val sideChannelParams = setOf("target_text", "risk", "approval", "ai_name")

    // kotlinx.serialization, NOT org.json: `org.json` is an unmocked Android stub in
    // the JVM unit-test classpath and every call throws "not mocked".
    private fun manifest(): JsonObject {
        val stream = javaClass.classLoader!!.getResourceAsStream("wire-manifest.json")
            ?: error(
                "wire-manifest.json missing from test resources. Copy it from " +
                    "aster-one/rust/cortex-kernel/crates/cortex-tools-screen/wire-manifest.json",
            )
        return Json.parseToJsonElement(stream.bufferedReader().readText()).jsonObject
    }

    private fun actions(): JsonObject = manifest().getValue("actions").jsonObject

    private fun stringList(obj: JsonObject, key: String): List<String> =
        obj[key]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

    /** Gradle runs unit tests with the module dir as CWD. */
    private fun serviceSource(relative: String): String {
        val f = File("src/main/java/com/aster/service/$relative")
        assertTrue("missing source file: ${f.absolutePath}", f.exists())
        return f.readText()
    }

    /** `search_text` → `searchText`. */
    private fun toCamel(key: String): String = WireParams.toCamelCase(key)

    @Test
    fun `contract version matches the kernel pin`() {
        assertEquals(
            "wire-manifest.json contract_version must match the pin in this test AND in " +
                "cortex-tools-screen/src/wire_contract.rs — bumping it is meant to require " +
                "touching both repositories",
            contractVersion,
            manifest()["contract_version"]?.jsonPrimitive?.contentOrNull,
        )
    }

    @Test
    fun `every manifest action is mapped to a source file`() {
        val unmapped = actions().keys - actionSources.keys
        assertTrue(
            "these actions are declared in wire-manifest.json but this test does not know " +
                "which handler owns them: $unmapped",
            unmapped.isEmpty(),
        )
    }

    @Test
    fun `every declared param is actually read by its handler`() {
        val unread = mutableListOf<String>()

        for ((action, element) in actions()) {
            val entry = element.jsonObject
            val notImplemented = stringList(entry, "not_implemented").toSet()
            val sources = actionSources[action] ?: continue
            val blob = sources.joinToString("\n") { serviceSource(it) }

            for (param in stringList(entry, "params")) {
                if (param in sideChannelParams || param in notImplemented) continue
                // Accept either spelling — both reach the handler after normalisation.
                val read = blob.contains("\"$param\"") || blob.contains("\"${toCamel(param)}\"")
                if (!read) unread += "$action.$param (looked in ${sources.joinToString()})"
            }
        }

        assertTrue(
            "these params are declared in the wire contract but NO companion handler reads " +
                "them — the kernel will send them and they will be silently dropped:\n  " +
                unread.joinToString("\n  "),
            unread.isEmpty(),
        )
    }

    /**
     * The specific regressions this contract exists to prevent, asserted by name so
     * a future refactor cannot quietly reintroduce them.
     */
    @Test
    fun `the params that were silently dropped in production are read`() {
        val handler = serviceSource("handlers/AccessibilityHandler.kt")
        // wait_for by view-id — hard-failed on every call; the LinkedIn flow's wait
        // steps are built from it.
        assertTrue("wait_for must read viewId", handler.contains("\"viewId\""))
        // input_gesture — the kernel sent `type`, the companion requires gestureType.
        assertTrue("input_gesture must read gestureType", handler.contains("\"gestureType\""))
        // scroll-to-find — degraded to a single swipe.
        assertTrue("scroll must read untilText", handler.contains("\"untilText\""))
        // observe narrowing — inoperative, so every observe returned the full budget.
        assertTrue("observe must read searchText", handler.contains("\"searchText\""))
        assertTrue("observe must read maxElements", handler.contains("\"maxElements\""))
        // wait_for_idle tuning.
        assertTrue("wait_for_idle must read quietMs", handler.contains("\"quietMs\""))
    }

    @Test
    fun `wait_for reports failure through ok so a timeout is not read as success`() {
        val handler = serviceSource("handlers/AccessibilityHandler.kt")
        assertTrue(
            "waitFor must emit `ok` mirroring `matched` — without it the kernel's " +
                "result_failed() sees a successful call and every verify.wait_for " +
                "post-condition passes on timeout",
            handler.contains("put(\"ok\", matched)"),
        )
    }

    @Test
    fun `the blind coordinate fallback is opt-in`() {
        val handler = serviceSource("handlers/AccessibilityHandler.kt")
        assertTrue(
            "tap/long_press must default allow_coordinate_fallback to false, or they can " +
                "never return stale_ref",
            handler.contains("\"allow_coordinate_fallback\"") &&
                handler.contains("?: false"),
        )
    }

    @Test
    fun `observe emits elements in reading order, not rank order`() {
        // Ranking decides WHAT survives the element budget; it must never decide
        // what order the kernel reads. This was an incidental nicety until
        // ordinal resolution shipped on the kernel side: a step can now declare
        // `ordinal: first | last | <n>` to pick among rows no primitive can tell
        // apart (a list of ride options differing only by a fare), and the
        // kernel indexes the matching peers IN THE ORDER THIS FILE SENDS THEM.
        // Emit rank order instead and "the first ride option" silently becomes
        // "whichever row scored highest" — a wrong tap that reports success,
        // across two independently-sideloaded APKs that can be on different
        // versions. The manifest declares the invariant; this pins the code.
        val observer = serviceSource("accessibility/ScreenObserver.kt")
        assertTrue(
            "ScreenObserver must re-sort the budget survivors back into traversal " +
                "order before emitting them (kept.sortBy { it.order }) — see " +
                "wire-manifest.json observe.result_invariants.elements_in_reading_order",
            observer.contains("kept.sortBy { it.order }"),
        )
        // And the refs must be assigned FROM that final order, not before it.
        val sortAt = observer.indexOf("kept.sortBy { it.order }")
        val refAt = observer.indexOf("val ref = ")
        assertTrue("could not locate the ref assignment", refAt > 0)
        assertTrue(
            "e<N> refs must be assigned after the reading-order sort, or the refs " +
                "and the element order disagree",
            sortAt in 1 until refAt,
        )
    }

    @Test
    fun `the observe result invariants are declared in the manifest`() {
        val invariants = actions().getValue("observe").jsonObject["result_invariants"]?.jsonObject
        assertTrue(
            "wire-manifest.json must declare observe.result_invariants — the kernel's " +
                "ordinal resolution depends on element ordering, and an undeclared " +
                "contract is one nobody can be held to",
            invariants?.get("elements_in_reading_order")?.jsonPrimitive?.contentOrNull == "true",
        )
    }

    @Test
    fun `a generic contentDescription does not suppress label aggregation`() {
        // Uber's ride chooser: eight ride options, each a clickable image
        // carrying desc="Vehicle" and nothing else. The words UberGo, the fare
        // and the ETA live in child TextViews that `actionable` mode drops, and
        // the gate used to be `text.isEmpty() && desc.isEmpty()` — so one
        // generic contentDescription switched off the aggregation that exists to
        // recover exactly those words. All eight rows reached the kernel as
        // byte-identical elements and "choose UberX" became unanswerable.
        //
        // A node's own `text` is its name; a contentDescription on an image or a
        // row container very often is not. Only `text` may suppress aggregation.
        val observer = serviceSource("accessibility/ScreenObserver.kt")
        assertTrue(
            "ScreenObserver must aggregate whenever the node has no text of its own, " +
                "regardless of contentDescription — a generic desc must not blind the " +
                "kernel to a row's actual words",
            observer.contains("if (text.isEmpty()) LabelAggregator.aggregate(node)"),
        )
        assertFalse(
            "the desc half of the old gate must be gone",
            observer.contains("text.isEmpty() && desc.isEmpty()"),
        )
    }
}
