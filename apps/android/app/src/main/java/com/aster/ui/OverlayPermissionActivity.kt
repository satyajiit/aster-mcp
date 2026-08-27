package com.aster.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast

/**
 * OverlayPermissionActivity — the door another app knocks on to get ASTER granted
 * "display over other apps".
 *
 * WHY IT EXISTS: an app can only ever request this permission for ITSELF, so a
 * caller cannot ask on Aster's behalf; it launches this exported activity instead
 * and Aster asks for its own grant, from its own foreground.
 *
 * IT CURRENTLY HAS NO EXTERNAL CALLER. It was written for OpenAlly's ambient
 * companion face — the one Aster drew over other apps — and that face has been
 * removed from both sides. It stays because the permission it grants is what
 * Aster's OWN overlays need (the tool-execution border, the interactive prompt,
 * the sign-in wait card), and because a shipped OpenAlly still fires
 * `com.aster.action.REQUEST_OVERLAY_PERMISSION`: deleting this would turn that
 * into an ActivityNotFoundException instead of a screen that grants something
 * Aster genuinely uses.
 *
 * HONEST ABOUT THE PLATFORM: since Android 11, `ACTION_MANAGE_OVERLAY_PERMISSION`
 * ignores any `package:` data and always opens the TOP-LEVEL "Display over other
 * apps" list, so the user picks Aster from that list rather than landing on its page.
 * The `package:` datum is still supplied because it does deep-link on Android 10 and
 * below. We do not pretend otherwise and we do not fake a grant.
 */
class OverlayPermissionActivity : Activity() {

    companion object {
        private const val TAG = "OverlayPermissionAct"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Settings.canDrawOverlays(this)) {
            setResult(RESULT_OK)
            finish()
            return
        }

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        try {
            Toast.makeText(this, com.aster.R.string.companion_overlay_permission_hint, Toast.LENGTH_LONG).show()
            startActivity(intent)
            setResult(RESULT_OK)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open the draw-over-other-apps settings screen", e)
            setResult(RESULT_CANCELED)
        }
        finish()
    }
}
