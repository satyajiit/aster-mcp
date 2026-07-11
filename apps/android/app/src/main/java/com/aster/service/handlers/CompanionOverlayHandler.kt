package com.aster.service.handlers

import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import com.aster.service.overlay.CompanionFaceOverlay
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * CompanionOverlayHandler — the lifecycle verbs for OpenAlly's ambient companion
 * face, which Aster hosts in an over-other-apps window
 * ([CompanionFaceOverlay]). OpenAlly holds no draw-over-other-apps permission of its
 * own, so it drives the window from here.
 *
 * Only the LIFECYCLE rides this handler. The face FRAMES do not: `executeCommand`
 * takes a `runBlocking` and writes two audit rows per call, which is fine a few times
 * a minute and ruinous at frame rate. Frames go over the dedicated `oneway`
 * `IAsterService.pushCompanionFrame` lane straight into [CompanionFaceOverlay.onFrame].
 *
 * Missing permission is an ANSWER, not an error (the `NowPlayingHandler` precedent):
 * `companion_overlay_status` reports `canDrawOverlays: false` and
 * `companion_overlay_show` returns `shown: false` with a reason, so OpenAlly can route
 * the user to grant it to ASTER rather than guessing or half-attaching a window.
 */
class CompanionOverlayHandler(
    private val overlay: CompanionFaceOverlay,
) : CommandHandler {

    override fun supportedActions() = listOf(
        "companion_overlay_status",
        "companion_overlay_show",
        "companion_overlay_hide",
        "companion_overlay_recompute",
    )

    override suspend fun handle(command: Command): CommandResult = when (command.action) {
        "companion_overlay_status" -> CommandResult.success(
            buildJsonObject {
                put("canDrawOverlays", overlay.canDrawOverlays())
                put("attached", overlay.isAttached())
            },
        )

        "companion_overlay_show" -> {
            val shown = overlay.show()
            CommandResult.success(
                buildJsonObject {
                    put("shown", shown)
                    if (!shown) put("reason", "overlay_permission_denied")
                },
            )
        }

        "companion_overlay_hide" -> {
            overlay.hide()
            CommandResult.success(buildJsonObject { put("attached", false) })
        }

        "companion_overlay_recompute" -> {
            overlay.recompute()
            CommandResult.success(buildJsonObject { put("recomputed", true) })
        }

        else -> CommandResult.failure("Unknown action: ${command.action}")
    }
}
