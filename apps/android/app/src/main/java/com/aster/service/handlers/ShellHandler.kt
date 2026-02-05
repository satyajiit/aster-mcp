package com.aster.service.handlers

import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader

class ShellHandler : CommandHandler {

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 30_000L
        private const val MAX_OUTPUT_SIZE = 1024 * 1024 // 1MB
    }

    override fun supportedActions() = listOf(
        "execute_shell"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "execute_shell" -> executeShell(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private suspend fun executeShell(command: Command): CommandResult = withContext(Dispatchers.IO) {
        val shellCommand = command.params?.get("command")?.jsonPrimitive?.contentOrNull
            ?: return@withContext CommandResult.failure("Missing 'command' parameter")

        val timeoutMs = command.params?.get("timeout")?.jsonPrimitive?.longOrNull ?: DEFAULT_TIMEOUT_MS

        try {
            withTimeout(timeoutMs) {
                val process = ProcessBuilder("sh", "-c", shellCommand)
                    .redirectErrorStream(true)
                    .start()

                val output = StringBuilder()
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (output.length < MAX_OUTPUT_SIZE) {
                        output.appendLine(line)
                    }
                }

                val exitCode = process.waitFor()
                reader.close()

                val truncated = output.length >= MAX_OUTPUT_SIZE
                val outputStr = if (truncated) {
                    output.substring(0, MAX_OUTPUT_SIZE) + "\n... (output truncated)"
                } else {
                    output.toString().trimEnd()
                }

                val data = buildJsonObject {
                    put("command", shellCommand)
                    put("exitCode", exitCode)
                    put("output", outputStr)
                    put("success", exitCode == 0)
                    put("truncated", truncated)
                }

                if (exitCode == 0) {
                    CommandResult.success(data)
                } else {
                    CommandResult(
                        success = false,
                        data = data,
                        error = "Command exited with code $exitCode"
                    )
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CommandResult.failure("Command timed out after ${timeoutMs}ms")
        } catch (e: Exception) {
            CommandResult.failure("Shell execution failed: ${e.message}")
        }
    }
}
