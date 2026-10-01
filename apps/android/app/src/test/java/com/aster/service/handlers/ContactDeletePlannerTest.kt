package com.aster.service.handlers

import com.aster.service.handlers.ContactDeletePlanner.CurrentContact
import com.aster.service.handlers.ContactDeletePlanner.Planned
import com.aster.service.handlers.ContactDeletePlanner.Refused
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The identity check behind `delete_contacts_verified` (OA-2026-0207): a
 * contact is deleted only when the row behind its id still carries the name
 * the owner approved, and always through its lookup key.
 */
class ContactDeletePlannerTest {

    @Test
    fun matchingNamesAreClearedWithTheirLookupKeys() {
        val plan = ContactDeletePlanner.plan(
            expected = listOf("2" to "Ada", "3" to "Ada L"),
            current = mapOf(
                "2" to CurrentContact("lk-2", "Ada"),
                "3" to CurrentContact("lk-3", "Ada L")
            )
        )
        assertEquals(listOf(Planned("2", "lk-2"), Planned("3", "lk-3")), plan.toDelete)
        assertTrue(plan.refused.isEmpty())
    }

    @Test
    fun aRenumberedOrRenamedContactIsNeverDeleted() {
        // Android re-aggregated: id 3 now names somebody else.
        val plan = ContactDeletePlanner.plan(
            expected = listOf("3" to "Ada L"),
            current = mapOf("3" to CurrentContact("lk-x", "Bob"))
        )
        assertTrue(plan.toDelete.isEmpty())
        assertEquals(listOf(Refused("3", ContactDeletePlanner.CHANGED)), plan.refused)
    }

    @Test
    fun aMissingContactIsReportedNotFound() {
        val plan = ContactDeletePlanner.plan(listOf("9" to "Gone"), emptyMap())
        assertEquals(listOf(Refused("9", ContactDeletePlanner.NOT_FOUND)), plan.refused)
    }

    @Test
    fun aRowWithoutALookupKeyIsRefusedRatherThanDeletedById() {
        val plan = ContactDeletePlanner.plan(
            listOf("4" to "Ada"),
            mapOf("4" to CurrentContact(null, "Ada"))
        )
        assertEquals(listOf(Refused("4", ContactDeletePlanner.FAILED)), plan.refused)
    }

    @Test
    fun whitespaceAndTheNoNamePlaceholderStillMatch() {
        val plan = ContactDeletePlanner.plan(
            expected = listOf("5" to "Ada  Lovelace", "6" to "Unknown"),
            current = mapOf(
                "5" to CurrentContact("lk-5", " Ada Lovelace"),
                "6" to CurrentContact("lk-6", null)
            )
        )
        assertEquals(2, plan.toDelete.size)
    }

    @Test
    fun aRepeatedIdIsPlannedOnce() {
        val plan = ContactDeletePlanner.plan(
            listOf("2" to "Ada", "2" to "Ada"),
            mapOf("2" to CurrentContact("lk-2", "Ada"))
        )
        assertEquals(1, plan.toDelete.size)
    }
}
