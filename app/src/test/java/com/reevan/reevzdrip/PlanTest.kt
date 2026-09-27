package com.reevan.reevzdrip

import com.reevan.reevzdrip.data.Sighting
import com.reevan.reevzdrip.ui.plan.PlanLock
import com.reevan.reevzdrip.ui.plan.isPlannable
import com.reevan.reevzdrip.ui.plan.planLock
import com.reevan.reevzdrip.ui.plan.repeatWarnings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plan lock and the repeat warning — the two rules that decide whether this app can be
 * trusted. Both are invisible when wrong: an off-by-one in the lock silently makes today
 * uneditable on the one morning you need to change it, and a wrong warning either cries wolf or
 * lets you walk into work in last Tuesday's shirt.
 */
class PlanTest {

    private val today = 20_723 // 2026-09-27

    // --- planLock ------------------------------------------------------------------------

    @Test
    fun `today is open, because the requirement says you can plan the current day`() {
        // This is exactly where Reevz Mealz differs: Mealz locks a day once it has begun, since
        // its Today screen owns the actuals. Drip must not, and porting Mealz's rule unchanged
        // would have broken the day the user most often needs to fix.
        assertEquals(PlanLock.OPEN, planLock(today, today))
        assertTrue(isPlannable(today, today))
    }

    @Test
    fun `future days are open`() {
        assertEquals(PlanLock.OPEN, planLock(today + 1, today))
        assertEquals(PlanLock.OPEN, planLock(today + 365, today))
    }

    @Test
    fun `yesterday and earlier are locked`() {
        assertEquals(PlanLock.PASSED, planLock(today - 1, today))
        assertEquals(PlanLock.PASSED, planLock(today - 400, today))
        assertFalse(isPlannable(today - 1, today))
    }

    @Test
    fun `the boundary is exactly midnight, not a rolling day`() {
        // Days are compared, not instants, so the cutoff is the calendar date flipping.
        assertTrue(isPlannable(today, today))
        assertFalse(isPlannable(today - 1, today))
    }

    // --- repeatWarnings ------------------------------------------------------------------

    private fun sighting(groupId: Long, name: String, day: Int) = Sighting(groupId, name, day)

    @Test
    fun `an outfit nobody has seen produces no warning`() {
        assertTrue(repeatWarnings(emptyList(), setOf(1L), today + 2, today).isEmpty())
    }

    @Test
    fun `a group that saw it before is warned about, in the past tense`() {
        val warnings = repeatWarnings(
            sightings = listOf(sighting(1, "Colleagues", today - 10)),
            chosenGroupIds = setOf(1L),
            targetDay = today + 2,
            today = today,
        )
        assertEquals(1, warnings.size)
        assertEquals("Colleagues", warnings[0].groupName)
        assertEquals(today - 10, warnings[0].day)
        assertTrue(warnings[0].inPast)
    }

    @Test
    fun `a clash still in the future is flagged as future, not past`() {
        // Different tense, different meaning: a future clash is one you can still go and fix.
        val warnings = repeatWarnings(
            sightings = listOf(sighting(1, "Colleagues", today + 9)),
            chosenGroupIds = setOf(1L),
            targetDay = today + 2,
            today = today,
        )
        assertEquals(1, warnings.size)
        assertFalse(warnings[0].inPast)
    }

    @Test
    fun `groups that were not chosen are ignored`() {
        val warnings = repeatWarnings(
            sightings = listOf(
                sighting(1, "Colleagues", today - 5),
                sighting(2, "College Friends", today - 5),
            ),
            chosenGroupIds = setOf(2L),
            targetDay = today + 2,
            today = today,
        )
        assertEquals(listOf("College Friends"), warnings.map { it.groupName })
    }

    @Test
    fun `a group that has seen it many times is warned about once`() {
        // A wall of lines is noise; the nearest occasion is the point.
        val warnings = repeatWarnings(
            sightings = listOf(
                sighting(1, "Colleagues", today - 90),
                sighting(1, "Colleagues", today - 30),
                sighting(1, "Colleagues", today - 4),
            ),
            chosenGroupIds = setOf(1L),
            targetDay = today,
            today = today,
        )
        assertEquals(1, warnings.size)
        assertEquals(today - 4, warnings[0].day)
    }

    @Test
    fun `the target day itself is never reported as a clash`() {
        // The entry being edited sits on the target day; an outfit cannot clash with itself.
        val warnings = repeatWarnings(
            sightings = listOf(sighting(1, "Colleagues", today + 3)),
            chosenGroupIds = setOf(1L),
            targetDay = today + 3,
            today = today,
        )
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `warnings are ordered nearest to the target day first`() {
        val warnings = repeatWarnings(
            sightings = listOf(
                sighting(1, "Colleagues", today - 100),
                sighting(2, "College Friends", today - 2),
                sighting(3, "Gym", today - 20),
            ),
            chosenGroupIds = setOf(1L, 2L, 3L),
            targetDay = today,
            today = today,
        )
        assertEquals(listOf("College Friends", "Gym", "Colleagues"), warnings.map { it.groupName })
    }

    @Test
    fun `one warning per group even when several groups are chosen`() {
        val warnings = repeatWarnings(
            sightings = listOf(
                sighting(1, "Colleagues", today - 3),
                sighting(1, "Colleagues", today - 8),
                sighting(2, "Gym", today - 5),
                sighting(2, "Gym", today - 9),
            ),
            chosenGroupIds = setOf(1L, 2L),
            targetDay = today,
            today = today,
        )
        assertEquals(2, warnings.size)
        assertEquals(setOf("Colleagues", "Gym"), warnings.map { it.groupName }.toSet())
    }

    @Test
    fun `choosing no groups asks nothing of the history`() {
        val warnings = repeatWarnings(
            sightings = listOf(sighting(1, "Colleagues", today - 3)),
            chosenGroupIds = emptySet(),
            targetDay = today,
            today = today,
        )
        assertTrue(warnings.isEmpty())
    }
}
