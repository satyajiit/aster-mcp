package com.aster.service.handlers

import android.content.ContentProviderOperation
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
        "delete_contacts",
        "delete_contacts_verified"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "search_contacts" -> searchContacts(command)
            "list_contacts_full" -> listContactsFull(command)
            "delete_contacts" -> deleteContacts(command)
            "delete_contacts_verified" -> deleteContactsVerified(command)
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
            //
            // The row cap rides on LIMIT_PARAM_KEY, NOT on a "… ASC LIMIT n"
            // sortOrder string. Appending LIMIT to sortOrder is a well-known
            // hack that ContactsProvider2 happens to accept on AOSP, but a
            // hardened provider running SQLiteQueryBuilder in strict mode
            // rejects a sortOrder containing SQL — which would surface here as
            // "Failed to list contacts: …" and abort the whole index sync on
            // exactly the vendor ROMs (MIUI / HyperOS among them) we cannot
            // test against. The query parameter is the sanctioned API.
            val pagedUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(
                    ContactsContract.LIMIT_PARAM_KEY,
                    (limit + 1).toString()
                )
                .build()
            val sortOrder = "${ContactsContract.Contacts._ID} ASC"

            context.contentResolver.query(
                pagedUri,
                projection,
                "${ContactsContract.Contacts._ID} > ?",
                arrayOf(cursorId.toString()),
                sortOrder
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val updatedIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
                // ★ `hasMore` is decided INSIDE the walk, not after it.
                //
                // The old shape was `while (c.moveToNext() && fetched < limit)`
                // followed by `val hasMore = c.moveToNext()`. Kotlin evaluates
                // `moveToNext()` first, so the check that ended the loop had
                // already consumed the limit+1 sentinel row; the trailing call
                // then asked for row limit+2, which LIMIT limit+1 guarantees
                // does not exist. `has_more` was therefore ALWAYS false, and the
                // kernel's sync — which stops on `!has_more` — would have
                // indexed only the first page (200 contacts) of any address
                // book, silently. Never observed in the field only because
                // nothing ever called the sync (ticket OA-2026-0207).
                var hasMore = false
                while (c.moveToNext()) {
                    if (fetched >= limit) {
                        // We already have a full page and the cursor just
                        // advanced onto the sentinel → at least one more row.
                        hasMore = true
                        break
                    }
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
     * failure list so a partial batch surfaces honestly.
     *
     * The MCP server's `aster_delete_contacts` (a human at the dashboard).
     * OpenAlly never calls this one: it uses [deleteContactsVerified], which
     * checks who each id is before deleting it.
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

    /**
     * `delete_contacts_verified` — the delete OpenAlly sends after its owner
     * tapped **Delete** on a card that listed every contact by name
     * (OA-2026-0207). Params: `ids`, `expect` (`[{id, display_name}]`, one
     * per id) and optional `keep` (`[{id, display_name}]`, the copies the card
     * said stay — if any of them is gone or renamed, nothing is deleted). Answers `{deleted_ids, failed: [{id, reason}], permission_needed}`
     * with `reason` from [ContactDeletePlanner]'s closed set.
     *
     * - **Permission.** Needs `WRITE_CONTACTS` (and `READ_CONTACTS` to check
     *   names first). A missing grant deletes nothing and says so with a typed
     *   `permission_needed` flag rather than a sentence, so OpenAlly can tell
     *   the owner to open Aster and allow Contacts.
     * - **Identity.** Each id is re-read (`LOOKUP_KEY`, display name) and
     *   deleted only if its name still matches what the owner approved, through
     *   `Contacts.getLookupUri(id, lookupKey)` — the provider's stable handle
     *   for a contact whose `_ID` may have changed.
     * - **Batch.** One `applyBatch` with a yield point after every contact: the
     *   Contacts Provider guide recommends batch mode, and a yield point makes
     *   each contact its own atomic unit ("all accesses between two yield points
     *   will either succeed or fail as a single unit") while keeping the
     *   provider's 500-operations-between-yield-points ceiling out of reach. If
     *   a chunk throws, it is retried one contact at a time so the report says
     *   exactly which ones went.
     *
     * Deleting a contact removes all its raw contacts; account sync adapters
     * (e.g. Google) then remove them server-side too — OpenAlly's card says so.
     * https://developer.android.com/identity/providers/contacts-provider
     */
    private fun deleteContactsVerified(command: Command): CommandResult {
        val params = command.params
            ?: return CommandResult.failure("Provide 'ids' and 'expect'")
        val ids = params["ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: return CommandResult.failure("Provide 'ids' (array of contact ids)")
        val expectByIdMap = params["expect"]?.jsonArray
            ?.let { namedIds(it) }
            ?.toMap()
            ?: return CommandResult.failure("Provide 'expect' (array of {id, display_name})")
        // The copies the owner was told are kept. Optional: a delete that is
        // not a duplicate clean-up keeps nobody.
        val keep = params["keep"]?.jsonArray?.let { namedIds(it) } ?: emptyList()
        if (ids.isEmpty()) return CommandResult.failure("'ids' must not be empty")
        if (ids.size > MAX_VERIFIED_DELETE || keep.size > MAX_VERIFIED_DELETE) {
            return CommandResult.failure("At most $MAX_VERIFIED_DELETE contacts per call")
        }
        // Every id must carry the name the owner approved. An id without one is
        // refused outright rather than deleted unchecked.
        val missingExpectation = ids.filter { it !in expectByIdMap }
        if (missingExpectation.isNotEmpty()) {
            return CommandResult.failure("Every id needs an 'expect' entry")
        }

        val canRead = granted(android.Manifest.permission.READ_CONTACTS)
        val canWrite = granted(android.Manifest.permission.WRITE_CONTACTS)
        if (!canRead || !canWrite) {
            return CommandResult.success(verifiedReply(
                deleted = emptyList(),
                failed = ids.distinct().map { it to ContactDeletePlanner.PERMISSION_NEEDED },
                permissionNeeded = true
            ))
        }

        return try {
            val expected = ids.distinct().map { it to expectByIdMap.getValue(it) }
            val lookupIds = (expected.map { it.first } + keep.map { it.first }).distinct()
            val plan = ContactDeletePlanner.plan(expected, currentContacts(lookupIds), keep)
            val deleted = mutableListOf<String>()
            val failed = plan.refused.map { it.id to it.reason }.toMutableList()
            var permissionNeeded = false
            for (chunk in plan.toDelete.chunked(DELETE_CHUNK)) {
                val outcome = applyDeletes(chunk)
                deleted += outcome.first
                failed += outcome.second
                if (outcome.second.any { it.second == ContactDeletePlanner.PERMISSION_NEEDED }) {
                    permissionNeeded = true
                }
            }
            CommandResult.success(verifiedReply(deleted, failed, permissionNeeded && deleted.isEmpty()))
        } catch (e: SecurityException) {
            CommandResult.success(verifiedReply(
                deleted = emptyList(),
                failed = ids.distinct().map { it to ContactDeletePlanner.PERMISSION_NEEDED },
                permissionNeeded = true
            ))
        } catch (e: Exception) {
            CommandResult.failure("Failed to delete contacts: ${e.message}")
        }
    }

    /** `[{id, display_name}]` → `(id, name)` pairs; malformed entries are dropped. */
    private fun namedIds(entries: kotlinx.serialization.json.JsonArray): List<Pair<String, String>> =
        entries.mapNotNull { entry ->
            val obj = entry as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            // Nested keys are not covered by WireParams' top-level aliasing.
            val name = (obj["display_name"] ?: obj["displayName"])
                ?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            id to name
        }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** `_ID → (LOOKUP_KEY, DISPLAY_NAME_PRIMARY)` for the ids that still exist. */
    private fun currentContacts(ids: List<String>): Map<String, ContactDeletePlanner.CurrentContact> {
        val out = mutableMapOf<String, ContactDeletePlanner.CurrentContact>()
        // At most 2 x MAX_VERIFIED_DELETE (targets plus kept copies), well below
        // the 999 bound-parameter cap of older Android SQLite builds.
        val placeholders = ids.joinToString(",") { "?" }
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            ),
            "${ContactsContract.Contacts._ID} IN ($placeholders)",
            ids.toTypedArray(),
            null
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val keyIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIdx = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (c.moveToNext()) {
                out[c.getLong(idIdx).toString()] = ContactDeletePlanner.CurrentContact(
                    lookupKey = c.getString(keyIdx),
                    displayName = c.getString(nameIdx)
                )
            }
        }
        return out
    }

    private fun lookupUri(p: ContactDeletePlanner.Planned): Uri =
        ContactsContract.Contacts.getLookupUri(p.id.toLong(), p.lookupKey)

    /**
     * Delete one chunk in a single batch; on any batch failure, fall back to
     * one-at-a-time so each contact gets its own answer.
     */
    private fun applyDeletes(
        chunk: List<ContactDeletePlanner.Planned>
    ): Pair<List<String>, List<Pair<String, String>>> {
        val deleted = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()
        try {
            val ops = ArrayList<ContentProviderOperation>(chunk.size)
            for (p in chunk) {
                ops += ContentProviderOperation.newDelete(lookupUri(p))
                    .withYieldAllowed(true)
                    .build()
            }
            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            chunk.forEachIndexed { i, p ->
                val count = results.getOrNull(i)?.count ?: 0
                if (count > 0) deleted += p.id else failed += p.id to ContactDeletePlanner.NOT_FOUND
            }
            return deleted to failed
        } catch (e: SecurityException) {
            return emptyList<String>() to chunk.map { it.id to ContactDeletePlanner.PERMISSION_NEEDED }
        } catch (e: Exception) {
            // Fall through to one-at-a-time. Contacts committed before a
            // yield point stay deleted; a second delete of them finds nothing
            // and reports `not_found`, which OpenAlly treats as gone.
        }
        for (p in chunk) {
            try {
                val rows = context.contentResolver.delete(lookupUri(p), null, null)
                if (rows > 0) deleted += p.id else failed += p.id to ContactDeletePlanner.NOT_FOUND
            } catch (e: SecurityException) {
                failed += p.id to ContactDeletePlanner.PERMISSION_NEEDED
            } catch (e: Exception) {
                failed += p.id to ContactDeletePlanner.FAILED
            }
        }
        return deleted to failed
    }

    private fun verifiedReply(
        deleted: List<String>,
        failed: List<Pair<String, String>>,
        permissionNeeded: Boolean
    ): JsonObject = buildJsonObject {
        put("deleted_ids", buildJsonArray { deleted.forEach { add(JsonPrimitive(it)) } })
        put("failed", buildJsonArray {
            failed.forEach { (id, reason) ->
                add(buildJsonObject {
                    put("id", id)
                    put("reason", reason)
                })
            }
        })
        put("permission_needed", permissionNeeded)
    }

    private companion object {
        /** OpenAlly stages at most 100 contacts per card. */
        const val MAX_VERIFIED_DELETE = 100
        /** Operations per `applyBatch`; far below the provider's 500 ceiling. */
        const val DELETE_CHUNK = 50
    }
}
