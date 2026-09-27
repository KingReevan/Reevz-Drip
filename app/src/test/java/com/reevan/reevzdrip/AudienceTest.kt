package com.reevan.reevzdrip

import com.reevan.reevzdrip.data.describeAudience
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How the history names the people who saw an outfit.
 *
 * Small, but it is read as a sentence in both history views, and the three awkward cases — none,
 * one, exactly two — are exactly the ones a bare `joinToString` gets wrong in a way that looks
 * like a bug rather than a style choice.
 */
class AudienceTest {

    @Test
    fun `one group is just its name`() {
        assertEquals("Colleagues", describeAudience(listOf("Colleagues")))
    }

    @Test
    fun `two groups are joined with and, not a comma`() {
        assertEquals("Colleagues and Gym", describeAudience(listOf("Colleagues", "Gym")))
    }

    @Test
    fun `three or more use commas and a final and`() {
        assertEquals(
            "Colleagues, Gym and Family",
            describeAudience(listOf("Colleagues", "Gym", "Family")),
        )
        assertEquals(
            "A, B, C and D",
            describeAudience(listOf("A", "B", "C", "D")),
        )
    }

    @Test
    fun `no groups produces empty text rather than a stray separator`() {
        // Should never happen — an entry requires at least one group — but an "and" hanging off
        // the end of a blank line is the kind of thing that ships.
        assertEquals("", describeAudience(emptyList()))
    }

    @Test
    fun `the original order is preserved`() {
        // Callers sort before calling; this must not re-sort and fight them.
        assertEquals("Zoo and Alpha", describeAudience(listOf("Zoo", "Alpha")))
    }
}
