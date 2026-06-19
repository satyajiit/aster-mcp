package com.aster.service.handlers

import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import com.aster.service.overlay.InteractiveOverlayController
import com.aster.service.overlay.InteractiveOverlayModel

/**
 * App Automations /goal R-C — bridges the kernel's `screen_prompt` /
 * `screen_approve` `device.execute` actions to the blocking
 * [InteractiveOverlayController]. `handle()` suspends (the IPC dispatcher parks
 * its Binder thread in `runBlocking`) until the owner picks, then returns the
 * choice JSON under `data`. One prompt is in flight at a time; a second arriving
 * while one is up is refused.
 */
class InteractiveOverlayHandler(
    private val controller: InteractiveOverlayController,
) : CommandHandler {

    override fun supportedActions(): List<String> = listOf("screen_prompt", "screen_approve")

    override suspend fun handle(command: Command): CommandResult {
        val params = command.params ?: emptyMap()
        val prompt = try {
            when (command.action) {
                "screen_prompt" -> InteractiveOverlayModel.parsePrompt(params)
                "screen_approve" -> InteractiveOverlayModel.parseApproval(params)
                else -> return CommandResult.failure("Unsupported action: ${command.action}")
            }
        } catch (e: IllegalArgumentException) {
            return CommandResult.failure(e.message ?: "invalid prompt request")
        }

        return try {
            CommandResult.success(controller.awaitChoice(prompt))
        } catch (e: IllegalStateException) {
            // One prompt at a time — another is already on screen.
            CommandResult.failure(e.message ?: "a prompt is already in flight")
        }
    }
}
