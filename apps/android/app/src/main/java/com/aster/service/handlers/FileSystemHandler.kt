package com.aster.service.handlers

import android.content.Context
import android.os.Environment
import android.util.Base64
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileSystemHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf(
        "list_files",
        "read_file",
        "write_file",
        "delete_file"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "list_files" -> listFiles(command)
            "read_file" -> readFile(command)
            "write_file" -> writeFile(command)
            "delete_file" -> deleteFile(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun listFiles(command: Command): CommandResult {
        val pathParam = command.params?.get("path")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'path' parameter")

        val path = resolvePath(pathParam)
        val directory = File(path)

        if (!directory.exists()) {
            return CommandResult.failure("Path does not exist: $path")
        }

        if (!directory.isDirectory) {
            return CommandResult.failure("Path is not a directory: $path")
        }

        if (!directory.canRead()) {
            return CommandResult.failure("Cannot read directory: $path")
        }

        val files = directory.listFiles() ?: emptyArray()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val fileList = buildJsonArray {
            files.sortedBy { it.name.lowercase() }.forEach { file ->
                addJsonObject {
                    put("name", file.name)
                    put("path", file.absolutePath)
                    put("isDirectory", file.isDirectory)
                    put("isFile", file.isFile)
                    put("size", file.length())
                    put("lastModified", dateFormat.format(Date(file.lastModified())))
                    put("canRead", file.canRead())
                    put("canWrite", file.canWrite())
                    put("isHidden", file.isHidden)

                    if (file.isFile) {
                        put("extension", file.extension)
                    }
                }
            }
        }

        val data = buildJsonObject {
            put("path", path)
            put("count", files.size)
            put("files", fileList)
        }

        return CommandResult.success(data)
    }

    private fun readFile(command: Command): CommandResult {
        val pathParam = command.params?.get("path")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'path' parameter")

        val path = resolvePath(pathParam)
        val file = File(path)

        if (!file.exists()) {
            return CommandResult.failure("File does not exist: $path")
        }

        if (!file.isFile) {
            return CommandResult.failure("Path is not a file: $path")
        }

        if (!file.canRead()) {
            return CommandResult.failure("Cannot read file: $path")
        }

        // Limit file size for safety (10MB)
        val maxSize = 10 * 1024 * 1024L
        if (file.length() > maxSize) {
            return CommandResult.failure("File too large (max 10MB): ${file.length()} bytes")
        }

        val isText = isTextFile(file)
        val content: String
        val encoding: String

        if (isText) {
            content = file.readText()
            encoding = "text"
        } else {
            content = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            encoding = "base64"
        }

        val data = buildJsonObject {
            put("path", path)
            put("name", file.name)
            put("size", file.length())
            put("encoding", encoding)
            put("content", content)
            put("mimeType", getMimeType(file))
        }

        return CommandResult.success(data)
    }

    private fun writeFile(command: Command): CommandResult {
        val pathParam = command.params?.get("path")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'path' parameter")

        val content = command.params?.get("content")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'content' parameter")

        val encoding = command.params?.get("encoding")?.jsonPrimitive?.contentOrNull ?: "text"

        val path = resolvePath(pathParam)
        val file = File(path)

        // Ensure parent directory exists
        file.parentFile?.mkdirs()

        try {
            when (encoding) {
                "text" -> file.writeText(content)
                "base64" -> file.writeBytes(Base64.decode(content, Base64.DEFAULT))
                else -> return CommandResult.failure("Unknown encoding: $encoding")
            }
        } catch (e: Exception) {
            return CommandResult.failure("Failed to write file: ${e.message}")
        }

        val data = buildJsonObject {
            put("path", path)
            put("size", file.length())
            put("success", true)
        }

        return CommandResult.success(data)
    }

    private fun deleteFile(command: Command): CommandResult {
        val pathParam = command.params?.get("path")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'path' parameter")

        val path = resolvePath(pathParam)
        val file = File(path)

        if (!file.exists()) {
            return CommandResult.failure("File does not exist: $path")
        }

        val deleted = if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }

        if (!deleted) {
            return CommandResult.failure("Failed to delete: $path")
        }

        val data = buildJsonObject {
            put("path", path)
            put("deleted", true)
        }

        return CommandResult.success(data)
    }

    private fun resolvePath(path: String): String {
        return when {
            path.startsWith("/") -> path
            path.startsWith("~") -> path.replaceFirst("~", Environment.getExternalStorageDirectory().absolutePath)
            path.startsWith("internal:") -> context.filesDir.absolutePath + "/" + path.removePrefix("internal:")
            path.startsWith("external:") -> (context.getExternalFilesDir(null)?.absolutePath ?: "") + "/" + path.removePrefix("external:")
            path.startsWith("cache:") -> context.cacheDir.absolutePath + "/" + path.removePrefix("cache:")
            else -> Environment.getExternalStorageDirectory().absolutePath + "/" + path
        }
    }

    private fun isTextFile(file: File): Boolean {
        val textExtensions = setOf(
            "txt", "json", "xml", "html", "htm", "css", "js", "ts",
            "md", "yaml", "yml", "ini", "conf", "cfg", "properties",
            "sh", "bash", "zsh", "log", "csv", "sql", "kt", "java",
            "py", "rb", "go", "rs", "c", "cpp", "h", "hpp"
        )
        return file.extension.lowercase() in textExtensions
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "txt" -> "text/plain"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }
}
