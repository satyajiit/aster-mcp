package com.aster.service.safety

import com.aster.service.safety.ExpectedPackageGuard.Target
import com.aster.service.safety.ExpectedPackageGuard.TargetNode
import com.aster.service.safety.LiveForeground.Kind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
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
 * front, alone on the screen, and only as a click on the element itself. Every
 * row below is one way that can fail.
 */
class ExpectedPackageGuardTest {

    private val telegram = "org.telegram.messenger"
    private val zelle = "com.zellepay.zelle"
    private val own = "com.aster"

    private fun expecting(value: JsonElement, ref: Boolean = true): Map<String, JsonElement> =
        buildMap {
            put("expected_package", value)
            if (ref) put("ref", JsonPrimitive("e1"))
        }

    private fun expecting(pkg: String) = expecting(JsonPrimitive(pkg))

    private fun app(pkg: String?, focused: Boolean = false) = LiveForeground.Window(Kind.APPLICATION, pkg, focused)
    private fun other(pkg: String?) = LiveForeground.Window(Kind.OTHER, pkg, focused = false)
    private fun keyboard() = LiveForeground.Window(Kind.INPUT_METHOD, "com.google.android.inputmethod.latin", false)

    /** Telegram in front, with the system bars any real screen has. */
    private fun telegramScreen(vararg extra: LiveForeground.Window) = LiveForeground(
        telegram,
        listOf(other("com.android.systemui"), app(telegram, focused = true)) + extra,
        own,
    )

    private fun body(error: String?): JsonObject {
        assertNotNull("expected a refusal", error)
        return Json.parseToJsonElement(error!!).jsonObject
    }

    private fun refused(action: String, params: Map<String, JsonElement>, live: LiveForeground?) =
        body(ExpectedPackageGuard.refusal(action, params) { live })

    private fun JsonObject.code() = getValue("code").jsonPrimitive.content

    @Test
    fun `an action without the field is untouched and reads nothing`() {
        var reads = 0
        for (params in listOf(null, emptyMap(), mapOf("ref" to JsonPrimitive("e1")))) {
            for (action in listOf("tap", "input_gesture", "observe")) {
                assertNull(ExpectedPackageGuard.refusal(action, params) { reads++; null })
            }
        }
        assertEquals("no window read without the field", 0, reads)
    }

    @Test
    fun `the observed app alone in front runs`() {
        for (action in listOf("tap", "long_press", "set_toggle", "perform")) {
            assertNull(action, ExpectedPackageGuard.refusal(action, expecting(telegram)) { telegramScreen() })
        }
        // The keyboard, the companion's own overlays and the app's own extra
        // windows (a dialog, a popup) are not another app.
        assertNull(
            ExpectedPackageGuard.refusal("tap", expecting(telegram)) {
                telegramScreen(keyboard(), other(own), app(telegram))
            },
        )
        // Either spelling of the key.
        assertNull(
            ExpectedPackageGuard.refusal("tap", mapOf("expectedPackage" to JsonPrimitive(telegram), "ref" to JsonPrimitive("e1"))) {
                telegramScreen()
            },
        )
    }

    @Test
    fun `another app in front is refused and named`() {
        val zelleScreen = LiveForeground(zelle, listOf(app(zelle, focused = true)), own)
        val body = refused("tap", expecting(telegram), zelleScreen)
        assertEquals(ExpectedPackageGuard.MISMATCH, body.code())
        assertEquals(telegram, body.getValue("expected_package").jsonPrimitive.content)
        assertEquals(zelle, body.getValue("foreground_package").jsonPrimitive.content)
        assertEquals("re-observe", body.getValue("hint").jsonPrimitive.content)
        assertTrue(body.getValue("message").jsonPrimitive.content.contains(zelle))
    }

    @Test
    fun `a front the device cannot vouch for is refused`() {
        val cannotSay = listOf(
            // The notification shade is the active window over the app.
            LiveForeground("com.android.systemui", listOf(app(telegram, focused = true)), own),
            // Split screen: the other half holds focus.
            LiveForeground(telegram, listOf(app(telegram), app(zelle, focused = true)), own),
            // The active window's root cannot be read.
            LiveForeground(null, listOf(app(telegram, focused = true)), own),
            // Windows listed but none focused.
            LiveForeground(telegram, listOf(app(telegram)), own),
            // A focused window whose root cannot be read.
            LiveForeground(telegram, listOf(app(null, focused = true)), own),
            // No window list: nothing to confirm the front or rule out an overlay.
            LiveForeground(telegram, emptyList(), own),
            // Blank packages.
            LiveForeground("  ", listOf(app(telegram, focused = true)), own),
        )
        for (live in cannotSay) {
            val body = refused("tap", expecting(telegram), live)
            assertEquals("$live", ExpectedPackageGuard.MISMATCH, body.code())
            assertEquals("$live", JsonNull, body["foreground_package"])
        }
        // No accessibility service at all.
        assertEquals(JsonNull, refused("tap", expecting(telegram), null)["foreground_package"])
    }

    @Test
    fun `another app's window anywhere on screen is refused`() {
        val overlays = listOf(
            // A chat head or any app drawing over other apps: a system window
            // under the drawing app's package.
            other("com.facebook.orca") to "com.facebook.orca",
            // Another app's window that does not hold focus (picture in picture).
            app("com.google.android.youtube") to "com.google.android.youtube",
            // Another accessibility service's overlay.
            LiveForeground.Window(Kind.OTHER, "com.example.reader", false) to "com.example.reader",
        )
        for ((window, pkg) in overlays) {
            val body = refused("tap", expecting(telegram), telegramScreen(window))
            assertEquals(pkg, ExpectedPackageGuard.OVERLAY, body.code())
            assertEquals(pkg, body.getValue("overlay_package").jsonPrimitive.content)
        }
        // A window nobody can name counts too.
        val unnamed = refused("tap", expecting(telegram), telegramScreen(other(null)))
        assertEquals(ExpectedPackageGuard.OVERLAY, unnamed.code())
        assertEquals(JsonNull, unnamed["overlay_package"])
    }

    @Test
    fun `a guarded action that could only be a gesture is refused before any read`() {
        var reads = 0
        val live = { reads++; telegramScreen() }
        // Every control verb that is not a node action.
        for (action in listOf("input_gesture", "scroll", "press_key", "global_action", "launch_intent", "click_by_text")) {
            val body = body(ExpectedPackageGuard.refusal(action, expecting(telegram), live))
            assertEquals(action, ExpectedPackageGuard.GESTURE, body.code())
        }
        // A tap or long press on coordinates, with no element.
        for (action in listOf("tap", "long_press")) {
            val body = body(ExpectedPackageGuard.refusal(action, expecting(JsonPrimitive(telegram), ref = false), live))
            assertEquals(action, ExpectedPackageGuard.GESTURE, body.code())
        }
        assertEquals(0, reads)
    }

    @Test
    fun `an unreadable expected package is refused, never ignored`() {
        for (value in listOf(JsonPrimitive(""), JsonPrimitive("   "), JsonPrimitive(42), JsonNull)) {
            val body = refused("tap", expecting(value), telegramScreen())
            assertEquals("$value", ExpectedPackageGuard.MISMATCH, body.code())
            assertEquals("$value", JsonNull, body["expected_package"])
        }
    }

    @Test
    fun `a guarded click lands on the element or its nearest clickable ancestor, never a gesture`() {
        val label = TargetNode(telegram, clickable = false, longClickable = false)
        val button = TargetNode(telegram, clickable = true, longClickable = false)
        val row = TargetNode(telegram, clickable = true, longClickable = true)

        assertEquals(Target.Act(0), ExpectedPackageGuard.target(listOf(button, row), telegram, longPress = false))
        assertEquals(Target.Act(1), ExpectedPackageGuard.target(listOf(label, button), telegram, longPress = false))
        assertEquals(Target.Act(2), ExpectedPackageGuard.target(listOf(label, button, row), telegram, longPress = true))

        // Nothing takes it, or no live node at all: a gesture is all that is left.
        for (chain in listOf(listOf(label, label), emptyList())) {
            val refused = ExpectedPackageGuard.target(chain, telegram, longPress = false)
            assertTrue("$chain", refused is Target.Refuse)
            assertEquals(ExpectedPackageGuard.GESTURE, body((refused as Target.Refuse).error).code())
        }
        assertTrue(ExpectedPackageGuard.target(listOf(button), telegram, longPress = true) is Target.Refuse)

        // The node found belongs to another app: the screen moved after the check.
        val elsewhere = ExpectedPackageGuard.target(listOf(TargetNode(zelle, true, false)), telegram, longPress = false)
        val moved = body((elsewhere as Target.Refuse).error)
        assertEquals(ExpectedPackageGuard.MISMATCH, moved.code())
        assertEquals(zelle, moved.getValue("foreground_package").jsonPrimitive.content)
    }

    @Test
    fun `the gesture refusal carries no package guess`() {
        val body = body(ExpectedPackageGuard.gesture(telegram))
        assertEquals(ExpectedPackageGuard.GESTURE, body.code())
        assertEquals(telegram, body.getValue("expected_package").jsonPrimitive.content)
        assertEquals(setOf("code", "expected_package", "hint", "message"), body.keys)
    }
}
