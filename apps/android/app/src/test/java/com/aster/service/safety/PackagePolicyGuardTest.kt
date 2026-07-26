package com.aster.service.safety

import com.aster.data.model.Command
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The companion's half of guarded autonomy.
 *
 * Two live defects motivate these: `updatePolicy` had **zero callers**, so an
 * owner allow-override recorded kernel-side never reached the process that
 * refuses the tap; and the guard was called only from `AccessibilityHandler`,
 * so `launch_intent` could open an app that `tap` was refused on.
 *
 * `checkAllowed` reads the live foreground package through the accessibility
 * service when the command does not name a target, and that service is a real
 * Android singleton — absent in a JVM unit test. So these exercise the two
 * halves that are decidable without it: the ACTION scoping (which verbs are
 * gated at all) and the explicit-target path (`launch_intent`), which never
 * touches the service.
 */
class PackagePolicyGuardTest {

    private fun launch(pkg: String?) = Command(
        type = "command",
        id = "t",
        action = "launch_intent",
        params = pkg?.let { mapOf("package" to JsonPrimitive(it)) } ?: emptyMap(),
    )

    private fun guardWith(allow: Set<String> = emptySet(), deny: Set<String> = emptySet()) =
        PackagePolicyGuard().apply { updatePolicy(allow, deny) }

    // ---- action scoping ----

    @Test
    fun `screen reads are never gated`() {
        val guard = PackagePolicyGuard()
        // No accessibility service exists in this JVM. A read must still pass —
        // if it did not, the obstruction classifier would go blind exactly when
        // a denylisted app is in front, which is when it is needed.
        for (read in listOf("observe", "take_screenshot", "wait_for", "wait_for_idle", "find_element")) {
            assertNull("`$read` must not be gated", guard.checkAllowed(read))
        }
    }

    @Test
    fun `the companion's own overlays are never gated`() {
        val guard = PackagePolicyGuard()
        // A hand-off banner exists to fire ON a payment screen. Gating it would
        // suppress it at precisely the moment it is the point.
        for (own in listOf("screen_prompt", "screen_approve", "screen_signin_wait", "screen_handoff")) {
            assertNull("`$own` renders the companion's own UI and must not be gated", guard.checkAllowed(own))
        }
    }

    @Test
    fun `the capability probe is never gated`() {
        // Otherwise the agent cannot distinguish "companion off" from "denied".
        assertNull(PackagePolicyGuard().checkAllowed("screen_capability"))
    }

    @Test
    fun `non-screen verbs are never gated`() {
        val guard = PackagePolicyGuard()
        for (other in listOf("get_battery", "list_packages", "send_sms", "get_clipboard", "screen_set_policy")) {
            assertNull("`$other` drives no app and must not be gated", guard.checkAllowed(other))
        }
    }

    @Test
    fun `every gated action drives another app`() {
        // A verb added to GATED_ACTIONS that only READS would silently start
        // failing closed whenever the foreground is unreadable.
        assertTrue("tap" in PackagePolicyGuard.GATED_ACTIONS)
        assertTrue("launch_intent" in PackagePolicyGuard.GATED_ACTIONS)
        assertTrue("observe" !in PackagePolicyGuard.GATED_ACTIONS)
        assertTrue("screen_handoff" !in PackagePolicyGuard.GATED_ACTIONS)
    }

    // ---- launch_intent is gated on its TARGET, not the foreground ----

    @Test
    fun `launch_intent names its own target package`() {
        assertEquals("com.x", PackagePolicyGuard.targetPackageOf(launch("com.x")))
        assertNull("a package-less custom intent falls back to the foreground", PackagePolicyGuard.targetPackageOf(launch(null)))
        assertNull("only launch_intent names a target", PackagePolicyGuard.targetPackageOf(launch("com.x").copy(action = "tap")))
    }

    @Test
    fun `launching a default-denied banking app is refused`() {
        val refusal = guardWith().checkAllowed("launch_intent", "com.phonepe.app")
        assertNotNull("a banking package must be refused by the bundled default list", refusal)
        assertTrue(refusal!!.contains("com.phonepe.app"))
    }

    @Test
    fun `an owner allow-override beats the bundled default deny`() {
        // THE defect this whole path exists to fix: before `updatePolicy` had a
        // caller, this returned a refusal no matter what the owner had set.
        assertNull(guardWith(allow = setOf("com.phonepe.app")).checkAllowed("launch_intent", "com.phonepe.app"))
    }

    @Test
    fun `an owner deny beats an allow for the same package`() {
        val guard = guardWith(allow = setOf("com.x"), deny = setOf("com.x"))
        assertNotNull("explicit deny wins", guard.checkAllowed("launch_intent", "com.x"))
    }

    @Test
    fun `an ordinary app is allowed`() {
        assertNull(guardWith().checkAllowed("launch_intent", "com.ubercab"))
    }

    @Test
    fun `a blank target fails closed`() {
        assertNotNull(guardWith().checkAllowed("launch_intent", "  "))
    }

    // ---- the push is a replace, not a merge ----

    @Test
    fun `a later push replaces the earlier policy wholesale`() {
        val guard = guardWith(allow = setOf("com.phonepe.app"))
        assertNull(guard.checkAllowed("launch_intent", "com.phonepe.app"))
        // The owner cleared the override. The kernel re-sends its complete set,
        // which no longer contains it — merging here would leave the allow
        // permanently stuck until the companion process restarted.
        guard.updatePolicy(emptySet(), emptySet())
        assertNotNull(guard.checkAllowed("launch_intent", "com.phonepe.app"))
    }

    @Test
    fun `an unsynced guard still enforces the bundled defaults`() {
        val guard = PackagePolicyGuard()
        assertTrue("a fresh process has never been pushed a policy", !guard.isSynced())
        assertNotNull(
            "the fail-safe set must bite even before the kernel has ever synced",
            guard.checkAllowed("launch_intent", "com.mybank.mobile"),
        )
    }

    @Test
    fun `snapshot reports what was pushed`() {
        val (allow, deny) = guardWith(allow = setOf("a"), deny = setOf("b", "c")).snapshot()
        assertEquals(setOf("a"), allow)
        assertEquals(setOf("b", "c"), deny)
    }

    // ---- PolicyHandler decodes what the kernel sends ----

    @Test
    fun `the policy handler decodes both arrays and drops blanks`() = kotlinx.coroutines.runBlocking {
        val guard = PackagePolicyGuard()
        val handler = com.aster.service.handlers.PolicyHandler(guard)
        val result = handler.handle(
            Command(
                type = "command",
                id = "t",
                action = "screen_set_policy",
                params = mapOf(
                    "allow" to JsonArray(listOf(JsonPrimitive("com.a"), JsonPrimitive("  "), JsonPrimitive(" com.b "))),
                    "deny" to JsonArray(listOf(JsonPrimitive("com.c"))),
                ),
            ),
        )
        assertTrue(result.success)
        val (allow, deny) = guard.snapshot()
        assertEquals(setOf("com.a", "com.b"), allow)
        assertEquals(setOf("com.c"), deny)
    }

    @Test
    fun `an omitted key means the empty set, not keep-the-old-one`() = kotlinx.coroutines.runBlocking {
        val guard = guardWith(allow = setOf("com.phonepe.app"))
        com.aster.service.handlers.PolicyHandler(guard).handle(
            Command(type = "command", id = "t", action = "screen_set_policy", params = emptyMap()),
        )
        assertEquals(emptySet<String>(), guard.snapshot().first)
    }
}
