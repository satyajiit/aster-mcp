package com.aster.service.handlers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class ClipboardHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf(
        "get_clipboard",
        "set_clipboard"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "get_clipboard" -> getClipboard()
            "set_clipboard" -> setClipboard(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private suspend fun getClipboard(): CommandResult = withContext(Dispatchers.Main) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        if (!clipboardManager.hasPrimaryClip()) {
            return@withContext CommandResult.success(buildJsonObject {
                put("hasContent", false)
                put("content", JsonNull)
            })
        }

        val clip = clipboardManager.primaryClip
        val item = clip?.getItemAt(0)

        val text = item?.text?.toString()
        val uri = item?.uri?.toString()
        val htmlText = item?.htmlText

        val data = buildJsonObject {
            put("hasContent", true)
            put("description", clip?.description?.label?.toString())
            put("text", text)
            put("uri", uri)
            put("htmlText", htmlText)
            put("itemCount", clip?.itemCount ?: 0)
        }

        CommandResult.success(data)
    }

    private suspend fun setClipboard(command: Command): CommandResult = withContext(Dispatchers.Main) {
        val text = command.params?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return@withContext CommandResult.failure("Missing 'text' parameter")

        val label = command.params?.get("label")?.jsonPrimitive?.contentOrNull ?: "Aster"

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)

        val data = buildJsonObject {
            put("success", true)
            put("text", text)
        }

        CommandResult.success(data)
    }
}
