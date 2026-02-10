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
        "send_sms"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "read_sms" -> readSms(command)
            "send_sms" -> sendSms(command)
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
        val threadId = command.params?.get("threadId")?.jsonPrimitive?.contentOrNull
        val type = command.params?.get("type")?.jsonPrimitive?.contentOrNull // "inbox", "sent", "all"

        val uri = when {
            threadId != null -> Uri.parse("$SMS_CONTENT_URI").buildUpon()
                .appendQueryParameter("thread_id", threadId)
                .build()
            type == "inbox" -> Uri.parse(SMS_INBOX_URI)
            type == "sent" -> Uri.parse(SMS_SENT_URI)
            else -> Uri.parse(SMS_CONTENT_URI)
        }

        return try {
            val messages = querySms(uri, limit, threadId)
            CommandResult.success(buildJsonObject {
                put("messages", messages)
                put("count", messages.size)
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

    private fun querySms(uri: Uri, limit: Int, threadId: String?): JsonArray {
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

        val selection = if (threadId != null) {
            "${Telephony.Sms.THREAD_ID} = ?"
        } else null

        val selectionArgs = if (threadId != null) {
            arrayOf(threadId)
        } else null

        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $limit"

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
