package com.aster.service.safety

import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The decorator is the one seam every transport and the tracked v2 lane
 * dispatch through, so it is where a mismatched `expected_package` must stop
 * the action: the handler that would tap is never reached.
 */
class GuardedCommandHandlerTest {

    private class Recording : CommandHandler {
        var calls = 0
        override fun supportedActions() = listOf("tap", "observe", "input_gesture")
        override suspend fun handle(command: Command): CommandResult {
            calls++
            return CommandResult.success()
        }
    }

    private fun window(pkg: String) = LiveForeground.Window(LiveForeground.Kind.APPLICATION, pkg, focused = true)

    private fun command(action: String, params: Map<String, JsonElement>) =
        Command(type = "command", id = "t", action = action, params = params)

    private val bypassedSend = mapOf(
        "ref" to JsonPrimitive("e1"),
        "snapshot_id" to JsonPrimitive("s1"),
        "risk_ack" to JsonPrimitive("agent_bypass"),
        "expected_package" to JsonPrimitive("org.telegram.messenger"),
    )

    @Test
    fun `a tap meant for one app never reaches the handler while another is in front`() = runBlocking<Unit> {
        val handler = Recording()
        val zelle = LiveForeground("com.zellepay.zelle", listOf(window("com.zellepay.zelle")), "com.aster")
        val guarded = GuardedCommandHandler(handler, PackagePolicyGuard(), foreground = { zelle })

        val result = guarded.handle(command("tap", bypassedSend))

        assertFalse(result.success)
        assertEquals(0, handler.calls)
        val body = Json.parseToJsonElement(result.error!!).jsonObject
        assertEquals(ExpectedPackageGuard.MISMATCH, body["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a guarded gesture never reaches the handler`() = runBlocking<Unit> {
        val handler = Recording()
        var reads = 0
        val guarded = GuardedCommandHandler(handler, PackagePolicyGuard(), foreground = { reads++; null })
        val swipe = mapOf(
            "gesture_type" to JsonPrimitive("swipe"),
            "expected_package" to JsonPrimitive("org.telegram.messenger"),
        )

        val result = guarded.handle(command("input_gesture", swipe))

        assertFalse(result.success)
        assertEquals(0, handler.calls)
        assertEquals(0, reads)
        val body = Json.parseToJsonElement(result.error!!).jsonObject
        assertEquals(ExpectedPackageGuard.GESTURE, body["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `no accessibility service refuses the tap`() = runBlocking<Unit> {
        val handler = Recording()
        val guarded = GuardedCommandHandler(handler, PackagePolicyGuard(), foreground = { null })

        val result = guarded.handle(command("tap", bypassedSend))

        assertFalse(result.success)
        assertEquals(0, handler.calls)
    }

    @Test
    fun `a matching front passes on, and no field means no window read`() = runBlocking<Unit> {
        // `observe` is not gated by the denylist, so it shows the expected-package
        // check passing through on its own; `tap` would also need the live
        // accessibility service the denylist reads, which a JVM test has none of.
        val handler = Recording()
        var reads = 0
        val telegram = LiveForeground("org.telegram.messenger", listOf(window("org.telegram.messenger")), "com.aster")
        val guarded = GuardedCommandHandler(handler, PackagePolicyGuard(), foreground = { reads++; telegram })

        assertTrue(guarded.handle(command("observe", bypassedSend)).success)
        assertEquals(1, reads)
        assertTrue(guarded.handle(command("observe", emptyMap())).success)
        assertEquals("an action without the field reads no window", 1, reads)
        assertEquals(2, handler.calls)
    }
}
