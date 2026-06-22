package com.aster.service.handlers

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Base64
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * Host-directory reader for the OpenAlly **App Builder "host-dir"** feature.
 *
 * The OpenAlly kernel serves a `kind=dir` app surface from an owner-approved
 * external folder by proxying each GET to this companion over `device.execute`
 * → action **`files.read`**, params `{ host_dir, path }`, and expects the reply
 * `{ found: Boolean, content_base64: String }` (the exact shape the kernel's
 * `HostFileReply` decodes). `files.list` is the optional companion sibling.
 *
 * `host_dir` is the owner-approved folder handle: either a persisted SAF **tree
 * URI** (`content://…/tree/…`, as minted by the openally.ai app's folder picker)
 * or a plain filesystem path. `path` is the request path RELATIVE to that folder
 * (e.g. `index.html`, `css/site.css`) — already normalised by the kernel.
 *
 * ## Why a filesystem read (not the SAF ContentResolver) is the primary path
 *
 * A SAF tree URI granted to the **openally.ai** app cannot be read by this
 * **separate** `com.aster` process — `content://` permissions are per-app and
 * don't cross the process boundary. But this companion holds
 * `MANAGE_EXTERNAL_STORAGE` (All-Files-Access), so it maps the tree URI's
 * document id (`primary:Foo/Bar`) to a real path
 * (`/storage/emulated/0/Foo/Bar`) and reads it with `java.io.File`. That works
 * regardless of which app ran the picker. A SAF `ContentResolver` read is kept
 * as a **fallback** for the case this companion ever holds its own grant (a
 * future companion-side picker) or a non-external-storage provider.
 *
 * ## Path-traversal safety
 *
 * `path` is resolved strictly within the granted folder: every read canonicalises
 * the join and rejects anything that escapes the base (a `..` segment, an
 * absolute path, a symlink out of the tree) → reported as "not found". The
 * companion is authoritative for this bound (the kernel forwards the raw path).
 */
class HostDirHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf("files.read", "files.list")

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "files.read" -> filesRead(command)
            "files.list" -> filesList(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    // ── files.read ─────────────────────────────────────────────────────────

    private fun filesRead(command: Command): CommandResult {
        val hostDir = command.params?.get("host_dir")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'host_dir' parameter")
        val rel = command.params?.get("path")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'path' parameter")

        // A storage-access denial seen on either path is reported only if BOTH
        // paths then fail to produce the file (so the SAF fallback still runs).
        var accessDenied: String? = null

        // PRIMARY — filesystem read via All-Files-Access (handles a content://
        // external-storage tree URI by mapping it to a path, or a raw path). The
        // size is checked from metadata BEFORE reading, so an oversized file can
        // never OOM the read (the sibling FileSystemHandler does the same).
        try {
            val baseDir = resolveBaseDir(hostDir)
            if (baseDir != null) {
                val target = safeJoin(baseDir, rel)
                if (target != null && target.isFile && target.canRead()) {
                    if (target.length() > MAX_FILE_BYTES) {
                        return CommandResult.failure(
                            "File too large (max $MAX_FILE_BYTES bytes): ${target.length()}"
                        )
                    }
                    return bytesReply(target.readBytes())
                }
            }
        } catch (e: SecurityException) {
            accessDenied = e.message // let the SAF fallback try before giving up
        } catch (e: Exception) {
            // Fall through to the SAF fallback before giving up.
        }

        // FALLBACK — SAF ContentResolver (only succeeds if this app holds a
        // persisted grant for the tree). The stream is byte-capped as it reads.
        if (hostDir.startsWith("content://")) {
            try {
                val bytes = safRead(Uri.parse(hostDir), rel)
                if (bytes != null) return bytesReply(bytes)
            } catch (e: SecurityException) {
                accessDenied = accessDenied ?: e.message
            } catch (e: Exception) {
                // No grant / unreadable → not found.
            }
        }

        return if (accessDenied != null) {
            CommandResult.failure(
                "Storage access denied (grant All-Files-Access or re-pick the folder): $accessDenied"
            )
        } else {
            notFound()
        }
    }

    /** A successful read, size-capped, as `{ found:true, content_base64 }`. */
    private fun bytesReply(bytes: ByteArray): CommandResult {
        if (bytes.size > MAX_FILE_BYTES) {
            return CommandResult.failure("File too large (max $MAX_FILE_BYTES bytes): ${bytes.size}")
        }
        return CommandResult.success(buildJsonObject {
            put("found", true)
            put("content_base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
        })
    }

    /** The "absent" reply the kernel maps to a 404 for the served request. */
    private fun notFound(): CommandResult = CommandResult.success(buildJsonObject {
        put("found", false)
        put("content_base64", "")
    })

    // ── files.list ─────────────────────────────────────────────────────────

    private fun filesList(command: Command): CommandResult {
        val hostDir = command.params?.get("host_dir")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'host_dir' parameter")
        // `path` defaults to the folder root for a listing.
        val rel = command.params?.get("path")?.jsonPrimitive?.contentOrNull ?: ""

        try {
            val baseDir = resolveBaseDir(hostDir)
            if (baseDir != null) {
                val dir = if (rel.isBlank()) baseDir.canonicalFile else safeJoin(baseDir, rel)
                if (dir != null && dir.isDirectory && dir.canRead()) {
                    val children = dir.listFiles() ?: emptyArray()
                    val entries = buildJsonArray {
                        children.sortedBy { it.name.lowercase() }.forEach { f ->
                            addJsonObject {
                                put("name", f.name)
                                put("isDirectory", f.isDirectory)
                                put("size", f.length())
                            }
                        }
                    }
                    return CommandResult.success(buildJsonObject {
                        put("found", true)
                        put("entries", entries)
                    })
                }
            }
        } catch (e: SecurityException) {
            return CommandResult.failure("Storage access denied (grant All-Files-Access): ${e.message}")
        } catch (e: Exception) {
            // Fall through.
        }

        return CommandResult.success(buildJsonObject {
            put("found", false)
            put("entries", buildJsonArray { })
        })
    }

    // ── Resolution helpers ─────────────────────────────────────────────────

    /**
     * Map a `host_dir` handle to a real base directory on disk, or `null` when
     * it isn't a path we can resolve via All-Files-Access (e.g. a cloud
     * provider tree URI — the SAF fallback handles those when a grant exists).
     */
    private fun resolveBaseDir(hostDir: String): File? {
        if (hostDir.startsWith("/")) return File(hostDir)
        if (hostDir.startsWith("content://")) {
            return try {
                treeDocIdToFile(DocumentsContract.getTreeDocumentId(Uri.parse(hostDir)))
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    /**
     * Map a SAF tree document id (`primary:Foo/Bar`, `1A2B-3C4D:Media`) to its
     * filesystem location. `primary` is the device's main external storage; a
     * volume serial maps under `/storage/<serial>` (SD card / USB).
     */
    private fun treeDocIdToFile(docId: String): File? {
        val parts = docId.split(":", limit = 2)
        if (parts.size != 2) return null
        val (volume, relative) = parts
        return if (volume == "primary") {
            File(Environment.getExternalStorageDirectory(), relative)
        } else {
            File("/storage/$volume", relative)
        }
    }

    /**
     * Join `rel` onto `base` and return the canonical target ONLY if it stays
     * within `base` — otherwise `null` (traversal / absolute / symlink escape).
     */
    private fun safeJoin(base: File, rel: String): File? {
        // Reject obvious traversal up front; the canonical check below is the
        // real guard (covers symlinks too).
        val segments = rel.split('/', '\\').filter { it.isNotEmpty() && it != "." }
        if (segments.any { it == ".." }) return null
        return try {
            val baseCanon = base.canonicalPath
            val target = File(base, segments.joinToString(File.separator)).canonicalFile
            val targetCanon = target.path
            if (targetCanon == baseCanon || targetCanon.startsWith(baseCanon + File.separator)) {
                target
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * SAF ContentResolver read (fallback): walk `rel` from the tree root via
     * `DocumentsContract`, looking each segment up among its parent's children
     * (so it can never escape the tree), then stream the bytes. Returns `null`
     * when any segment is missing; throws when the tree isn't readable (no
     * grant) — the caller treats that as "not found".
     */
    private fun safRead(treeUri: Uri, rel: String): ByteArray? {
        val segments = rel.split('/', '\\').filter { it.isNotEmpty() && it != "." }
        if (segments.any { it == ".." } || segments.isEmpty()) return null
        var docId = DocumentsContract.getTreeDocumentId(treeUri)
        for (segment in segments) {
            docId = findChild(treeUri, docId, segment) ?: return null
        }
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return context.contentResolver.openInputStream(docUri)?.use { readCapped(it) }
    }

    /**
     * Read a stream into memory, bounded at [MAX_FILE_BYTES]. Returns `null` once
     * the cap is exceeded (treated as "not found") so a huge document can never
     * OOM the service. Uses `InputStream.read(ByteArray)` so it works on every
     * supported API level (unlike `readNBytes`, which is API 33+).
     */
    private fun readCapped(stream: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = stream.read(chunk)
            if (n < 0) break
            total += n
            if (total > MAX_FILE_BYTES) return null
            out.write(chunk, 0, n)
        }
        return out.toByteArray()
    }

    /** Find a child document id by display name under `parentDocId`. */
    private fun findChild(treeUri: Uri, parentDocId: String, name: String): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIdx) == name) return cursor.getString(idIdx)
            }
        }
        return null
    }

    companion object {
        // Served folders are static web assets; bound a single read well within
        // the IPC transaction ceiling (the Binder path to the kernel is the real
        // practical limit on large files).
        private const val MAX_FILE_BYTES = 8 * 1024 * 1024
    }
}
