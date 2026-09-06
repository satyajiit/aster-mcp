package com.aster.service.execution

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.system.Os
import android.system.OsConstants
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.RandomAccessFile
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface ExecutionResultCipher {
    fun encrypt(bytes: ByteArray): ByteArray
    fun decrypt(bytes: ByteArray): ByteArray
}

private class AndroidExecutionResultCipher : ExecutionResultCipher {
    private val key: SecretKey by lazy {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "aster.execution.results.v2"
        (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore",
        ).run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    override fun encrypt(bytes: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
        init(Cipher.ENCRYPT_MODE, key)
        iv + doFinal(bytes)
    }
    override fun decrypt(bytes: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
        require(bytes.size >= 28)
        init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        doFinal(bytes, 12, bytes.size - 12)
    }
}

/** No request body is stored. Terminal identity/hash evidence is never pruned. */
internal class ExecutionJournal internal constructor(
    private val file: File,
    private val cipher: ExecutionResultCipher,
    val bootId: String = UUID.randomUUID().toString(),
    private val retainedResultBytes: Long = ExecutionProtocol.MAX_RETAINED_RESULT_BYTES,
    private val stabilize: (File) -> Unit = ::stabilizeFile,
) {
    constructor(context: Context) : this(
        File(context.noBackupFilesDir, "device-executions-v2.sqlite"), AndroidExecutionResultCipher(),
    )

    private val db: SQLiteDatabase
    private var poisoned = false
    val companionId: String

    init {
        file.parentFile!!.mkdirs()
        db = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            db.rawQuery("PRAGMA journal_mode=DELETE", null).use { it.moveToFirst() }
            db.execSQL("PRAGMA synchronous=FULL")
            db.execSQL("PRAGMA busy_timeout=5000")
            db.execSQL("CREATE TABLE IF NOT EXISTS execution_meta (key TEXT PRIMARY KEY,value TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS executions (id TEXT PRIMARY KEY,owner TEXT NOT NULL,query_json TEXT NOT NULL,state TEXT NOT NULL,closed_at_ms INTEGER,result_hash TEXT,result_cipher BLOB,result_bytes INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE TABLE IF NOT EXISTS legacy_flight (id TEXT PRIMARY KEY,boot_id TEXT NOT NULL)")
            companionId = transaction {
                val existing = db.rawQuery("SELECT value FROM execution_meta WHERE key='companion_id'", null).use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
                val id = existing ?: UUID.randomUUID().toString().also {
                    db.execSQL("INSERT INTO execution_meta(key,value) VALUES('companion_id',?)", arrayOf(it))
                }
                // An old boot may have dispatched. It cannot become closed merely
                // because its coroutine disappeared. Prepared rows never dispatched
                // and remain explicitly sealable, without relaunching a worker.
                db.execSQL("UPDATE executions SET state='uncertain' WHERE state='running'")
                id
            }
            stabilize(file)
        } catch (e: Exception) {
            poisoned = true
            db.close()
            throw e
        }
    }

    private fun <T> transaction(body: () -> T): T {
        check(!poisoned) { "Execution journal requires reopen after uncertain storage" }
        db.beginTransaction()
        try {
            val result = body()
            db.setTransactionSuccessful()
            return result
        } finally {
            try { db.endTransaction() } catch (e: Exception) { poisoned = true; throw e }
        }
    }

    private data class Row(val query: ExecutionQuery, val owner: String, val state: String,
        val closedAt: Long?, val hash: String?, val encrypted: ByteArray?)

    private fun row(query: ExecutionQuery, owner: String): Row? {
        check(!poisoned)
        require(query.target.companion_id == companionId) { "Different companion installation" }
        return db.rawQuery("SELECT query_json,owner,state,closed_at_ms,result_hash,result_cipher FROM executions WHERE id=?", arrayOf(query.request.request_id)).use {
            if (!it.moveToFirst()) return@use null
            val saved = ExecutionProtocol.json.decodeFromString<ExecutionQuery>(it.getString(0))
            ExecutionProtocol.validate(saved)
            require(saved == query && it.getString(1) == owner) { "Original execution identity differs" }
            val state = it.getString(2)
            check(state in setOf("prepared", "running", "uncertain", "no_dispatch", "closed"))
            if (state in setOf("closed", "no_dispatch")) check(!it.isNull(3) && it.getLong(3) > 0)
            else check(it.isNull(3) && it.isNull(4) && it.isNull(5))
            if (state == "closed") check(!it.isNull(4) && Regex("[0-9a-f]{64}").matches(it.getString(4)))
            if (state == "no_dispatch") check(it.isNull(4) && it.isNull(5))
            Row(saved, owner, state, if (it.isNull(3)) null else it.getLong(3),
                if (it.isNull(4)) null else it.getString(4), if (it.isNull(5)) null else it.getBlob(5))
        }
    }

    private fun unresolvedOnDb(): Boolean = db.rawQuery(
        "SELECT EXISTS(SELECT 1 FROM executions WHERE state IN ('prepared','running','uncertain')) OR EXISTS(SELECT 1 FROM legacy_flight)", null,
    ).use { it.moveToFirst(); it.getInt(0) != 0 }

    @Synchronized fun unresolved(): Boolean { check(!poisoned); return unresolvedOnDb() }

    private fun insert(query: ExecutionQuery, owner: String, state: String, closed: Long? = null) {
        db.execSQL("INSERT INTO executions(id,owner,query_json,state,closed_at_ms) VALUES(?,?,?,?,?)", arrayOf(
            query.request.request_id, owner, ExecutionProtocol.json.encodeToString(query), state, closed,
        ))
    }

    /** Returns true only for this first durable prepared insertion. */
    @Synchronized fun prepare(query: ExecutionQuery, owner: String, supported: Boolean): Boolean = transaction {
        if (row(query, owner) != null) return@transaction false
        if (!supported || query.target.boot_id != bootId || unresolvedOnDb()) {
            insert(query, owner, "no_dispatch", System.currentTimeMillis())
            false
        } else {
            // Establish usable private-result encryption before admitting any
            // physical work. A broken/missing keystore is not a dispatch reason.
            cipher.encrypt(ByteArray(0)).fill(0)
            insert(query, owner, "prepared")
            true
        }
    }

    /** A seal racing the worker must win before this transition to prevent dispatch. */
    @Synchronized fun begin(query: ExecutionQuery, owner: String): Boolean = transaction {
        if (row(query, owner)?.state != "prepared" || query.target.boot_id != bootId) return@transaction false
        db.execSQL("UPDATE executions SET state='running' WHERE id=? AND state='prepared'", arrayOf(query.request.request_id))
        true
    }

    @Synchronized fun seal(query: ExecutionQuery, owner: String) = transaction {
        val before = row(query, owner)
        if (before == null) insert(query, owner, "no_dispatch", System.currentTimeMillis())
        else if (before.state == "prepared") db.execSQL(
            "UPDATE executions SET state='no_dispatch',closed_at_ms=? WHERE id=? AND state='prepared'",
            arrayOf(System.currentTimeMillis(), query.request.request_id),
        )
    }

    @Synchronized fun finish(query: ExecutionQuery, owner: String, resultJson: String) {
        val bytes = resultJson.toByteArray(Charsets.UTF_8)
        // An oversized original result can prove closure without retaining its
        // payload. Never replace it with a made-up successful/failed result.
        val encrypted = try {
            // The worker has already joined the handler and every child. A
            // keystore failure now affects result availability, not that proof.
            if (bytes.size <= ExecutionProtocol.MAX_RESULT_BYTES) {
                runCatching { cipher.encrypt(bytes) }.getOrNull()
            } else null
        } finally { bytes.fill(0) }
        val hash = ExecutionProtocol.sha256(resultJson)
        transaction {
            check(row(query, owner)?.state == "running")
            db.execSQL("UPDATE executions SET state='closed',closed_at_ms=?,result_hash=?,result_cipher=?,result_bytes=? WHERE id=? AND state='running'", arrayOf(
                System.currentTimeMillis(), hash, encrypted, encrypted?.size ?: 0, query.request.request_id,
            ))
            // Only private result bytes are compacted. Identity, terminal state,
            // timestamp and original result hash remain immutable indefinitely.
            var total = db.rawQuery("SELECT COALESCE(sum(result_bytes),0) FROM executions", null).use { it.moveToFirst(); it.getLong(0) }
            val old = db.rawQuery("SELECT id,result_bytes FROM executions WHERE result_cipher IS NOT NULL ORDER BY closed_at_ms,id", null).use {
                buildList { while (it.moveToNext()) add(it.getString(0) to it.getLong(1)) }
            }
            for ((id, size) in old) {
                if (total <= retainedResultBytes) break
                db.execSQL("UPDATE executions SET result_cipher=NULL,result_bytes=0 WHERE id=?", arrayOf(id))
                total -= size
            }
        }
    }

    @Synchronized fun uncertain(query: ExecutionQuery, owner: String) = transaction {
        if (row(query, owner)?.state == "running") db.execSQL(
            "UPDATE executions SET state='uncertain' WHERE id=? AND state='running'", arrayOf(query.request.request_id),
        )
    }

    @Synchronized fun observe(query: ExecutionQuery, owner: String): ExecutionObservation {
        val row = row(query, owner)
        val result = row?.encrypted?.let { bytes ->
            try {
                val plain = cipher.decrypt(bytes)
                try { plain.toString(Charsets.UTF_8).takeIf { ExecutionProtocol.sha256(it) == row.hash } }
                finally { plain.fill(0) }
            } catch (_: Exception) { null }
        }
        val r = query.request
        return ExecutionObservation(request_id = r.request_id, workspace_id = r.workspace_id,
            requested_device_id = r.requested_device_id, payload_hash = r.payload_hash,
            created_at_ms = r.created_at_ms, target = query.target,
            state = when (row?.state) { "prepared" -> "accepted"; null -> "uncertain"; else -> row.state },
            observed_at_ms = System.currentTimeMillis(), closed_at_ms = row?.closedAt,
            result_json = result, result_hash = row?.hash, result_available = result != null)
    }

    @Synchronized fun beginLegacy(): String? = transaction {
        if (unresolvedOnDb()) return@transaction null
        UUID.randomUUID().toString().also {
            db.execSQL("INSERT INTO legacy_flight(id,boot_id) VALUES(?,?)", arrayOf(it, bootId))
        }
    }

    @Synchronized fun finishLegacy(id: String) = transaction {
        db.execSQL("DELETE FROM legacy_flight WHERE id=? AND boot_id=?", arrayOf(id, bootId))
    }

    @Synchronized internal fun closeForTest() = db.close()

    companion object {
        private fun stabilizeFile(file: File) {
            RandomAccessFile(file, "rw").use { it.fd.sync() }
            val descriptor = Os.open(file.parentFile!!.absolutePath, OsConstants.O_RDONLY or OsConstants.O_DIRECTORY, 0)
            try { Os.fsync(descriptor) } finally { Os.close(descriptor) }
        }
    }
}
