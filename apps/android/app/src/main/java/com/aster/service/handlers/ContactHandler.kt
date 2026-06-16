package com.aster.service.handlers

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

class ContactHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf(
        "search_contacts",
        "list_contacts_full",
        "delete_contacts"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "search_contacts" -> searchContacts(command)
            "list_contacts_full" -> listContactsFull(command)
            "delete_contacts" -> deleteContacts(command)
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

    /**
     * Paged full read of the device address book for the kernel's
     * `contacts.list` host-call (device-contacts index sync). Walks
     * `ContactsContract.Contacts` ordered by `_ID` ASC, using a numeric
     * `cursor` (the next `_ID` to start at) so the kernel can resume across
     * bounded pages without re-reading. Returns each contact's id, display
     * name, ALL numbers + emails, and account type. Caps `limit` to 500 so
     * one call can never block the companion past an ANR window.
     */
    private fun listContactsFull(command: Command): CommandResult {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return CommandResult.failure("Contacts permission not granted. Please enable READ_CONTACTS permission.")
        }

        val cursorId = command.params?.get("cursor")?.jsonPrimitive?.longOrNull ?: 0L
        val limit = (command.params?.get("limit")?.jsonPrimitive?.intOrNull ?: 200)
            .coerceIn(1, 500)

        return try {
            val contacts = mutableListOf<JsonObject>()
            var lastId = cursorId
            var fetched = 0

            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
            )
            // Fetch limit+1 to detect whether more pages exist.
            val sortOrder = "${ContactsContract.Contacts._ID} ASC LIMIT ${limit + 1}"

            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                "${ContactsContract.Contacts._ID} > ?",
                arrayOf(cursorId.toString()),
                sortOrder
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val updatedIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
                while (c.moveToNext() && fetched < limit) {
                    val contactId = c.getLong(idIdx)
                    val displayName = c.getString(nameIdx) ?: "Unknown"
                    val lastUpdated = c.getLong(updatedIdx)
                    val phones = getPhoneNumbers(contactId.toString())
                    val emails = getEmails(contactId.toString())
                    val accountType = accountTypeFor(contactId.toString())
                    contacts.add(buildJsonObject {
                        put("contact_id", contactId.toString())
                        put("display_name", displayName)
                        put("phones", buildJsonArray { phones.forEach { add(it) } })
                        put("emails", buildJsonArray { emails.forEach { add(it) } })
                        put("account_type", accountType)
                        put("last_updated", lastUpdated)
                    })
                    lastId = contactId
                    fetched++
                }
                // After consuming `limit` rows, if the cursor can still advance
                // there is at least one more row → another page exists.
                val hasMore = c.moveToNext()
                val data = buildJsonObject {
                    put("contacts", buildJsonArray { contacts.forEach { add(it) } })
                    put("next_cursor", lastId)
                    put("has_more", hasMore)
                }
                return CommandResult.success(data)
            }

            // No cursor (null query result) — empty address book.
            CommandResult.success(buildJsonObject {
                put("contacts", buildJsonArray { })
                put("next_cursor", cursorId)
                put("has_more", false)
            })
        } catch (e: SecurityException) {
            CommandResult.failure("Contacts permission not granted. Please enable READ_CONTACTS permission.")
        } catch (e: Exception) {
            CommandResult.failure("Failed to list contacts: ${e.message}")
        }
    }

    /** Look up the account type of a contact's first raw-contact row. */
    private fun accountTypeFor(contactId: String): String {
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { c ->
            if (c.moveToNext()) {
                return c.getString(0) ?: ""
            }
        }
        return ""
    }

    /**
     * Delete one or more contacts by their `Contacts._ID`. For each id we
     * delete every `RawContacts` row whose `CONTACT_ID` matches (a contact is
     * an aggregate of raw contacts), which removes the contact device-wide.
     * Requires `WRITE_CONTACTS`. Returns the count deleted plus a per-id
     * failure list so a partial batch surfaces honestly. This is the
     * companion leg behind the kernel's owner-confirmed `contacts_delete`
     * (the kernel re-validates and only fires this on an owner Confirm in P7).
     */
    private fun deleteContacts(command: Command): CommandResult {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return CommandResult.failure("Contacts write permission not granted. Please enable WRITE_CONTACTS permission.")
        }

        val ids = command.params?.get("ids")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: return CommandResult.failure("Provide 'ids' (array of contact ids) to delete")

        if (ids.isEmpty()) {
            return CommandResult.failure("'ids' must not be empty")
        }

        var deleted = 0
        val failed = mutableListOf<JsonObject>()

        for (id in ids) {
            try {
                val rows = context.contentResolver.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                    arrayOf(id)
                )
                if (rows > 0) {
                    deleted++
                } else {
                    failed.add(buildJsonObject {
                        put("id", id)
                        put("reason", "not_found")
                    })
                }
            } catch (e: SecurityException) {
                failed.add(buildJsonObject {
                    put("id", id)
                    put("reason", "permission_denied")
                })
            } catch (e: Exception) {
                failed.add(buildJsonObject {
                    put("id", id)
                    put("reason", e.message ?: "unknown")
                })
            }
        }

        return CommandResult.success(buildJsonObject {
            put("deleted", deleted)
            put("failed", buildJsonArray { failed.forEach { add(it) } })
        })
    }
}
