package com.aster.service.safety

import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult

/**
 * Decorator that runs [PackagePolicyGuard] in front of EVERY command handler.
 *
 * Why a decorator and not a call inside each mode's dispatch: there are three
 * dispatch paths ([com.aster.service.mode.IpcMode], `McpToolRegistry` and
 * [com.aster.service.mode.RemoteWsMode]) and each builds its own `Command`, so a
 * guard placed in one of them covers one transport. The guard used to live
 * inside `AccessibilityHandler.handle` — narrower still: it covered the
 * accessibility verbs and nothing else, so `launch_intent` (IntentHandler) could
 * open a denylisted app that `tap` was refused on. Wrapping the map at
 * construction is the single seam every transport already funnels through
 * (`commandHandlers[action]`), so there is exactly one place to get right.
 *
 * The decorator is transparent: [supportedActions] delegates, so the map built
 * from it is keyed identically and every `getAvailableTools()` / catalog read is
 * unchanged.
 */
class GuardedCommandHandler(
    private val delegate: CommandHandler,
    private val guard: PackagePolicyGuard,
) : CommandHandler {

    override fun supportedActions(): List<String> = delegate.supportedActions()

    override suspend fun handle(command: Command): CommandResult {
        guard.checkAllowed(command.action, PackagePolicyGuard.targetPackageOf(command))
            ?.let { refusal -> return CommandResult.failure(refusal) }
        return delegate.handle(command)
    }
}
