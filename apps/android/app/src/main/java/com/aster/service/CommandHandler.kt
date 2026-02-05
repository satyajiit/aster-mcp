package com.aster.service

import com.aster.data.model.Command
import kotlinx.serialization.json.JsonElement

/**
 * Interface for handling MCP commands.
 */
interface CommandHandler {
    /**
     * Returns the list of action names this handler supports.
     */
    fun supportedActions(): List<String>

    /**
     * Handles a command and returns the result.
     * @param command The command to handle
     * @return CommandResult containing success/failure and optional data
     */
    suspend fun handle(command: Command): CommandResult
}

/**
 * Result of a command execution.
 */
data class CommandResult(
    val success: Boolean,
    val data: JsonElement? = null,
    val error: String? = null
) {
    companion object {
        fun success(data: JsonElement? = null) = CommandResult(success = true, data = data)
        fun failure(error: String) = CommandResult(success = false, error = error)
    }
}
