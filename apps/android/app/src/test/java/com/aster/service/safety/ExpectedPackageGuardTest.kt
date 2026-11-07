package com.aster.service.safety

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The companion's half of "Act without asking" (agent autonomy §2.3): a tap the
 * kernel let skip its card runs only while the app it observed is still in
 * front. Every row below is one way the screen can have moved since then.
 */
class ExpectedPackageGuardTest {

    private val telegram = "org.telegram.messenger"
    private val zelle = "com.zellepay.zelle"

    private fun expecting(value: JsonElement) = mapOf("expected_package" to value)
    private fun expecting(pkg: String) = expecting(JsonPrimitive(pkg))

    private fun front(active: String?, vararg apps: Pair<String?, Boolean>) =
        LiveForeground(active, apps.map { (pkg, focused) -> LiveForeground.AppWindow(pkg, focused) })

    private fun refused(params: Map<String, JsonElement>?, live: LiveForeground?): Map<String, JsonElement> {
        val refusal = ExpectedPackageGuard.refusal(params) { live }
        assertNotNull("expected a refusal", refusal)
        return Json.parseToJsonElement(refusal!!).jsonObject
    }

    @Test
    fun `an action without the field is untouched and reads nothing`() {
        var reads = 0
        for (params in listOf(null, emptyMap(), mapOf("ref" to JsonPrimitive("e1")))) {
            assertNull(ExpectedPackageGuard.refusal(params) { reads++; null })
        }
        assertEquals("no window read without the field", 0, reads)
    }

    @Test
    fun `the observed app still in front runs`() {
        assertNull(ExpectedPackageGuard.refusal(expecting(telegram)) { front(telegram, telegram to true) })
        // A second, unfocused app (split screen, picture in picture) does not
        // change which app the tap lands in.
        assertNull(
            ExpectedPackageGuard.refusal(expecting(telegram)) {
                front(telegram, zelle to false, telegram to true)
            },
        )
        // No window list (the flag not yet in effect): the active root decides,
        // as it did for the observe the kernel decided on.
        assertNull(ExpectedPackageGuard.refusal(expecting(telegram)) { front(telegram) })
        // Either spelling of the key.
        assertNull(
            ExpectedPackageGuard.refusal(mapOf("expectedPackage" to JsonPrimitive(telegram))) {
                front(telegram, telegram to true)
            },
        )
    }

    @Test
    fun `another app in front is refused and named`() {
        val body = refused(expecting(telegram), front(zelle, zelle to true))
        assertEquals(ExpectedPackageGuard.CODE, body["code"]!!.jsonPrimitive.content)
        assertEquals(telegram, body["expected_package"]!!.jsonPrimitive.content)
        assertEquals(zelle, body["foreground_package"]!!.jsonPrimitive.content)
        assertEquals("re-observe", body["hint"]!!.jsonPrimitive.content)
        assertTrue(body["message"]!!.jsonPrimitive.content.contains(zelle))
    }

    @Test
    fun `a front the device cannot vouch for is refused`() {
        val cannotSay = listOf(
            // The notification shade is the active window over the app.
            front("com.android.systemui", telegram to true),
            // Split screen: the other half holds focus.
            front(telegram, telegram to false, zelle to true),
            // The active window's root cannot be read.
            front(null, telegram to true),
            // Windows listed but none focused.
            front(telegram, telegram to false),
            // A focused window whose root cannot be read.
            front(telegram, null to true),
            // Blank packages.
            front("  ", telegram to true),
        )
        for (live in cannotSay) {
            val body = refused(expecting(telegram), live)
            assertEquals("$live", ExpectedPackageGuard.CODE, body["code"]!!.jsonPrimitive.content)
            assertEquals("$live", JsonNull, body["foreground_package"])
        }
        // No accessibility service at all.
        assertEquals(JsonNull, refused(expecting(telegram), null)["foreground_package"])
    }

    @Test
    fun `an unreadable expected package is refused, never ignored`() {
        for (value in listOf(JsonPrimitive(""), JsonPrimitive("   "), JsonPrimitive(42), JsonNull)) {
            val body = refused(expecting(value), front(telegram, telegram to true))
            assertEquals("$value", ExpectedPackageGuard.CODE, body["code"]!!.jsonPrimitive.content)
            assertEquals("$value", JsonNull, body["expected_package"])
        }
    }
}
