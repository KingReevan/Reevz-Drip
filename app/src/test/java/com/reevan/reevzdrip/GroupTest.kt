package com.reevan.reevzdrip

import com.reevan.reevzdrip.data.PeopleGroup
import com.reevan.reevzdrip.data.isDuplicateGroupName
import com.reevan.reevzdrip.data.peopleGroupOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupTest {

    private fun group(id: Long, name: String) = PeopleGroup(id = id, name = name, createdOn = 0)

    // --- peopleGroupOf -------------------------------------------------------------------

    @Test
    fun `a name is trimmed and capitalised`() {
        assertEquals("Colleagues", peopleGroupOf(name = "  colleagues ", createdOn = 0).name)
        assertEquals("College Friends", peopleGroupOf(name = "college friends", createdOn = 0).name)
    }

    @Test
    fun `existing capitalisation inside a word survives`() {
        assertEquals("NIT Friends", peopleGroupOf(name = "NIT friends", createdOn = 0).name)
    }

    @Test
    fun `a new group starts un-archived`() {
        assertFalse(peopleGroupOf(name = "Colleagues", createdOn = 0).archived)
    }

    // --- isDuplicateGroupName ------------------------------------------------------------

    @Test
    fun `an unused name is not a duplicate`() {
        val existing = listOf(group(1, "Colleagues"))
        assertFalse(isDuplicateGroupName("College Friends", existing))
    }

    @Test
    fun `duplicates are caught regardless of case`() {
        // The database's unique index is a byte comparison and would allow all of these, which is
        // why the check lives here: two groups that look identical in every list are unusable.
        val existing = listOf(group(1, "Colleagues"))
        assertTrue(isDuplicateGroupName("Colleagues", existing))
        assertTrue(isDuplicateGroupName("colleagues", existing))
        assertTrue(isDuplicateGroupName("COLLEAGUES", existing))
        assertTrue(isDuplicateGroupName("cOlLeAgUeS", existing))
    }

    @Test
    fun `surrounding whitespace does not smuggle a duplicate past the check`() {
        val existing = listOf(group(1, "Colleagues"))
        assertTrue(isDuplicateGroupName("  Colleagues  ", existing))
    }

    @Test
    fun `renaming a group does not clash with itself`() {
        // Opening the editor and saving without changing anything must not be blocked.
        val existing = listOf(group(1, "Colleagues"), group(2, "Gym"))
        assertFalse(isDuplicateGroupName("Colleagues", existing, excludingId = 1))
        assertFalse(isDuplicateGroupName("colleagues", existing, excludingId = 1))
    }

    @Test
    fun `renaming onto another group's name is still a duplicate`() {
        val existing = listOf(group(1, "Colleagues"), group(2, "Gym"))
        assertTrue(isDuplicateGroupName("Gym", existing, excludingId = 1))
    }

    @Test
    fun `a blank name is not reported as a duplicate`() {
        // Blank is rejected by the required-name rule; reporting it as a clash as well would show
        // the wrong error the moment the field is cleared.
        val existing = listOf(group(1, "Colleagues"))
        assertFalse(isDuplicateGroupName("", existing))
        assertFalse(isDuplicateGroupName("   ", existing))
    }

    @Test
    fun `the first group is never a duplicate`() {
        assertFalse(isDuplicateGroupName("Colleagues", emptyList()))
    }
}
