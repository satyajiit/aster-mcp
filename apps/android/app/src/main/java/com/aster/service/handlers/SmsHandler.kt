package com.aster.service.handlers

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

/**
 * Handler for SMS-related commands.
 * Reads SMS messages from the device.
 */
class SmsHandler(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val SMS_CONTENT_URI = "content://sms"
        private const val SMS_INBOX_URI = "content://sms/inbox"
        private const val SMS_SENT_URI = "content://sms/sent"
        private const val SMS_CONVERSATIONS_URI = "content://sms/conversations"
    }

    override fun supportedActions() = listOf(
        "read_sms",
        "send_sms",
        "count_sms"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "read_sms" -> readSms(command)
            "send_sms" -> sendSms(command)
            "count_sms" -> countSms(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun readSms(command: Command): CommandResult {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return CommandResult.failure("SMS permission not granted")
        }

        val limit = command.params?.get("limit")?.jsonPrimitive?.intOrNull ?: 50
        val offset = command.params?.get("offset")?.jsonPrimitive?.intOrNull ?: 0
        val threadId = command.params?.get("threadId")?.jsonPrimitive?.contentOrNull
        val type = command.params?.get("type")?.jsonPrimitive?.contentOrNull // "inbox", "sent", "all"
        // Half-open date window [sinceDate, untilDate) in epoch ms. `untilDate`
        // (exclusive upper bound) is what the OpenAlly kernel's SMS-analyser
        // reverse-cursor pagination needs: it walks `untilDate` down to the
        // oldest date of each page, so without it the window can't close and
        // pages skip/duplicate.
        val sinceDate = command.params?.get("sinceDate")?.jsonPrimitive?.longOrNull
        val untilDate = command.params?.get("untilDate")?.jsonPrimitive?.longOrNull

        val uri = when {
            threadId != null -> Uri.parse("$SMS_CONTENT_URI").buildUpon()
                .appendQueryParameter("thread_id", threadId)
                .build()
            type == "inbox" -> Uri.parse(SMS_INBOX_URI)
            type == "sent" -> Uri.parse(SMS_SENT_URI)
            else -> Uri.parse(SMS_CONTENT_URI)
        }

        return try {
            // Fetch limit+1 rows to detect whether more pages exist
            val messages = querySms(uri, limit + 1, offset, threadId, sinceDate, untilDate)
            val hasMore = messages.size > limit
            val page = if (hasMore) JsonArray(messages.take(limit)) else messages
            CommandResult.success(buildJsonObject {
                put("messages", page)
                put("count", page.size)
                put("offset", offset)
                put("hasMore", hasMore)
            })
        } catch (e: Exception) {
            CommandResult.failure("Failed to read SMS: ${e.message}")
        }
    }

    private fun sendSms(command: Command): CommandResult {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.SEND_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return CommandResult.failure("SEND_SMS permission not granted")
        }

        val number = command.params?.get("number")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'number' parameter")
        val message = command.params?.get("message")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'message' parameter")

        // Clean the phone number
        val cleanNumber = number.replace(Regex("[^0-9+*#]"), "")
        if (cleanNumber.isEmpty()) {
            return CommandResult.failure("Invalid phone number")
        }

        return try {
            @Suppress("DEPRECATION")
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            // Handle long messages by splitting into parts
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(cleanNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
            }
            CommandResult.success(buildJsonObject {
                put("sent", true)
                put("number", cleanNumber)
                put("message", message)
                put("parts", parts.size)
            })
        } catch (e: Exception) {
            CommandResult.failure("Failed to send SMS: ${e.message}")
        }
    }

    private fun querySms(
        uri: Uri,
        limit: Int,
        offset: Int,
        threadId: String?,
        sinceDate: Long? = null,
        untilDate: Long? = null
    ): JsonArray {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.SEEN,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.PERSON
        )

        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (threadId != null) {
            conditions.add("${Telephony.Sms.THREAD_ID} = ?")
            args.add(threadId)
        }
        if (sinceDate != null) {
            conditions.add("${Telephony.Sms.DATE} >= ?")
            args.add(sinceDate.toString())
        }
        if (untilDate != null) {
            // Exclusive upper bound — half-open window [sinceDate, untilDate).
            conditions.add("${Telephony.Sms.DATE} < ?")
            args.add(untilDate.toString())
        }
        val selection = if (conditions.isNotEmpty()) conditions.joinToString(" AND ") else null
        val selectionArgs = if (args.isNotEmpty()) args.toTypedArray() else null

        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $limit OFFSET $offset"

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        return buildJsonArray {
            cursor?.use { c ->
                val idIndex = c.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
                val dateSentIndex = c.getColumnIndex(Telephony.Sms.DATE_SENT)
                val typeIndex = c.getColumnIndex(Telephony.Sms.TYPE)
                val readIndex = c.getColumnIndex(Telephony.Sms.READ)
                val seenIndex = c.getColumnIndex(Telephony.Sms.SEEN)
                val threadIdIndex = c.getColumnIndex(Telephony.Sms.THREAD_ID)

                while (c.moveToNext()) {
                    add(buildJsonObject {
                        if (idIndex >= 0) put("id", c.getLong(idIndex))
                        if (addressIndex >= 0) put("address", c.getString(addressIndex) ?: "")
                        if (bodyIndex >= 0) put("body", c.getString(bodyIndex) ?: "")
                        if (dateIndex >= 0) put("date", c.getLong(dateIndex))
                        if (dateSentIndex >= 0) put("dateSent", c.getLong(dateSentIndex))
                        if (typeIndex >= 0) {
                            val typeInt = c.getInt(typeIndex)
                            put("type", smsTypeToString(typeInt))
                            put("typeCode", typeInt)
                        }
                        if (readIndex >= 0) put("read", c.getInt(readIndex) == 1)
                        if (seenIndex >= 0) put("seen", c.getInt(seenIndex) == 1)
                        if (threadIdIndex >= 0) put("threadId", c.getLong(threadIdIndex))
                    })
                }
            }
        }
    }

    /**
     * Count messages in a half-open date window [sinceDate, untilDate),
     * without transferring bodies (projects only `_id`). Mirrors the
     * `read_sms` filter contract so the OpenAlly kernel's `count_range`
     * host import returns a consistent total for the same window.
     */
    private fun countSms(command: Command): CommandResult {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return CommandResult.failure("SMS permission not granted")
        }

        val threadId = command.params?.get("threadId")?.jsonPrimitive?.contentOrNull
        val type = command.params?.get("type")?.jsonPrimitive?.contentOrNull
        val sinceDate = command.params?.get("sinceDate")?.jsonPrimitive?.longOrNull
        val untilDate = command.params?.get("untilDate")?.jsonPrimitive?.longOrNull

        val uri = when {
            threadId != null -> Uri.parse(SMS_CONTENT_URI).buildUpon()
                .appendQueryParameter("thread_id", threadId)
                .build()
            type == "inbox" -> Uri.parse(SMS_INBOX_URI)
            type == "sent" -> Uri.parse(SMS_SENT_URI)
            else -> Uri.parse(SMS_CONTENT_URI)
        }

        return try {
            val count = countSmsRows(uri, threadId, sinceDate, untilDate)
            CommandResult.success(buildJsonObject {
                put("count", count)
            })
        } catch (e: Exception) {
            CommandResult.failure("Failed to count SMS: ${e.message}")
        }
    }

    private fun countSmsRows(
        uri: Uri,
        threadId: String?,
        sinceDate: Long?,
        untilDate: Long?
    ): Long {
        val projection = arrayOf(Telephony.Sms._ID)
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (threadId != null) {
            conditions.add("${Telephony.Sms.THREAD_ID} = ?")
            args.add(threadId)
        }
        if (sinceDate != null) {
            conditions.add("${Telephony.Sms.DATE} >= ?")
            args.add(sinceDate.toString())
        }
        if (untilDate != null) {
            conditions.add("${Telephony.Sms.DATE} < ?")
            args.add(untilDate.toString())
        }
        val selection = if (conditions.isNotEmpty()) conditions.joinToString(" AND ") else null
        val selectionArgs = if (args.isNotEmpty()) args.toTypedArray() else null

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )
        return cursor?.use { it.count.toLong() } ?: 0L
    }

    private fun smsTypeToString(type: Int): String {
        return when (type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> "inbox"
            Telephony.Sms.MESSAGE_TYPE_SENT -> "sent"
            Telephony.Sms.MESSAGE_TYPE_DRAFT -> "draft"
            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "outbox"
            Telephony.Sms.MESSAGE_TYPE_FAILED -> "failed"
            Telephony.Sms.MESSAGE_TYPE_QUEUED -> "queued"
            else -> "unknown"
        }
    }
}
