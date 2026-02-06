package com.aster.service.handlers

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

class ContactHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf("search_contacts")

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "search_contacts" -> searchContacts(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun searchContacts(command: Command): CommandResult {
        val name = command.params?.get("name")?.jsonPrimitive?.contentOrNull
        val number = command.params?.get("number")?.jsonPrimitive?.contentOrNull
        val limit = command.params?.get("limit")?.jsonPrimitive?.intOrNull ?: 20

        if (name == null && number == null) {
            return CommandResult.failure("Provide 'name' and/or 'number' to search")
        }

        return try {
            val contacts = mutableListOf<JsonObject>()

            if (name != null) {
                searchByName(name, limit, contacts)
            }

            if (number != null) {
                searchByNumber(number, limit - contacts.size, contacts)
            }

            val data = buildJsonObject {
                put("contacts", buildJsonArray { contacts.forEach { add(it) } })
                put("count", contacts.size)
            }

            CommandResult.success(data)
        } catch (e: SecurityException) {
            CommandResult.failure("Contacts permission not granted. Please enable READ_CONTACTS permission.")
        } catch (e: Exception) {
            CommandResult.failure("Failed to search contacts: ${e.message}")
        }
    }

    private fun searchByName(query: String, limit: Int, results: MutableList<JsonObject>) {
        if (limit <= 0) return

        val uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_FILTER_URI,
            Uri.encode(query)
        )

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        val seenIds = results.map { it["id"]?.jsonPrimitive?.content }.toMutableSet()

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                if (contactId in seenIds) continue
                seenIds.add(contactId)

                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: "Unknown"
                val hasPhone = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0

                val phones = if (hasPhone) getPhoneNumbers(contactId) else emptyList()
                val emails = getEmails(contactId)

                results.add(buildJsonObject {
                    put("id", contactId)
                    put("name", displayName)
                    put("phones", buildJsonArray { phones.forEach { add(it) } })
                    put("emails", buildJsonArray { emails.forEach { add(it) } })
                })
                count++
            }
        }
    }

    private fun searchByNumber(query: String, limit: Int, results: MutableList<JsonObject>) {
        if (limit <= 0) return

        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(query)
        )

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )

        val seenIds = results.map { it["id"]?.jsonPrimitive?.content }.toMutableSet()

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                if (contactId in seenIds) continue
                seenIds.add(contactId)

                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)) ?: "Unknown"

                val phones = getPhoneNumbers(contactId)
                val emails = getEmails(contactId)

                results.add(buildJsonObject {
                    put("id", contactId)
                    put("name", displayName)
                    put("phones", buildJsonArray { phones.forEach { add(it) } })
                    put("emails", buildJsonArray { emails.forEach { add(it) } })
                })
                count++
            }
        }
    }

    private fun getPhoneNumbers(contactId: String): List<JsonObject> {
        val phones = mutableListOf<JsonObject>()

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                val typeName = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    context.resources, type, ""
                ).toString()

                phones.add(buildJsonObject {
                    put("number", number)
                    put("type", typeName)
                })
            }
        }

        return phones
    }

    private fun getEmails(contactId: String): List<JsonObject> {
        val emails = mutableListOf<JsonObject>()

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE
            ),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val address = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE))
                val typeName = ContactsContract.CommonDataKinds.Email.getTypeLabel(
                    context.resources, type, ""
                ).toString()

                emails.add(buildJsonObject {
                    put("address", address)
                    put("type", typeName)
                })
            }
        }

        return emails
    }
}
