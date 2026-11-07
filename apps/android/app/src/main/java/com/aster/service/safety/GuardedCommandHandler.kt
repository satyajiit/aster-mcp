package com.aster.service.safety

import com.aster.data.model.Command
import com.aster.service.AsterAccessibilityService
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import com.aster.service.execution.ExecutionCoordinator

/**
 * Decorator that runs [PackagePolicyGuard] and [ExpectedPackageGuard] in front
 * of EVERY command handler.
 *
 * Why a decorator and not a call inside each mode's dispatch: there are three
 * dispatch paths ([com.aster.service.mode.IpcMode], `McpToolRegistry` and
 * [com.aster.service.mode.RemoteWsMode]) and each builds its own `Command`, so a
 * guard placed in one of them covers one transport. The guard used to live
 * inside `AccessibilityHandler.handle` — narrower still: it covered the
 * accessibility verbs and nothing else, so `launch_intent` (IntentHandler) could
 * open a denylisted app that `tap` was refused on. Wrapping the map at
 * construction is the single seam every transport already funnels through
 * (`commandHandlers[action]`), so there is exactly one place to get right. The
 * tracked v2 lane dispatches through the same map, so it is covered too.
 *
 * The decorator is transparent: [supportedActions] delegates, so the map built
 * from it is keyed identically and every `getAvailableTools()` / catalog read is
 * unchanged.
 *
 * [foreground] reads the live screen for [ExpectedPackageGuard]; a parameter
 * only so a JVM test can stand in for the accessibility service.
 */
class GuardedCommandHandler(
    private val delegate: CommandHandler,
    private val guard: PackagePolicyGuard,
    private val executions: ExecutionCoordinator? = null,
    private val foreground: () -> LiveForeground? = {
        AsterAccessibilityService.getInstance()?.liveForeground()
    },
) : CommandHandler {

    override fun supportedActions(): List<String> = delegate.supportedActions()

    override suspend fun handle(command: Command): CommandResult {
        val execution = executions
        return if (execution == null) handleGuarded(command)
        else execution.legacy(command.action) { handleGuarded(command) }
    }

    private suspend fun handleGuarded(command: Command): CommandResult {
        // The app-switch check first: when the screen moved to a banking app
        // since the kernel looked, "another app is in front, look again" names
        // what actually changed; the denylist refusal would only name the bank.
        ExpectedPackageGuard.refusal(command.params, foreground)
            ?.let { refusal -> return CommandResult.failure(refusal) }
        guard.checkAllowed(command.action, PackagePolicyGuard.targetPackageOf(command))
            ?.let { refusal -> return CommandResult.failure(refusal) }
        return delegate.handle(command)
    }
}
