package com.reevan.reevzdrip

import com.reevan.reevzdrip.util.epochDayOf
import com.reevan.reevzdrip.util.startOfDay
import com.reevan.reevzdrip.util.startOfDayMillis
import com.reevan.reevzdrip.util.todayEpochDay
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Epoch-day conversion is new logic — Reevz Mealz stores millis instead — and it sits underneath
 * every history query the app will make. An off-by-one here means an outfit planned for today
 * shows up as already worn, which is invisible until it matters.
 */
class DatesTest {

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(year, month, day, hour, minute)
        return calendar.timeInMillis
    }

    @Test
    fun `epoch day round trips back to the same calendar date`() {
        val original = millisAt(2026, Calendar.SEPTEMBER, 25)
        val day = epochDayOf(original)
        val back = startOfDayMillis(day)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = back
        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH))
        assertEquals(25, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `every hour of a day maps to the same epoch day`() {
        // The boundary hours are where a naive millis-divided-by-86400000 goes wrong: in a
        // positive-offset zone, local midnight is still the previous day in UTC.
        val day = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 25, hour = 12))
        for (hour in 0..23) {
            assertEquals(
                "hour $hour landed on a different day",
                day,
                epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 25, hour = hour)),
            )
        }
    }

    @Test
    fun `consecutive days differ by exactly one`() {
        val first = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 25))
        val second = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 26))
        assertEquals(1, second - first)
    }

    @Test
    fun `epoch day increases across a month and a year boundary`() {
        assertTrue(
            epochDayOf(millisAt(2026, Calendar.OCTOBER, 1)) >
                epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 30)),
        )
        assertTrue(
            epochDayOf(millisAt(2027, Calendar.JANUARY, 1)) >
                epochDayOf(millisAt(2026, Calendar.DECEMBER, 31)),
        )
    }

    @Test
    fun `a leap day is an ordinary day`() {
        val feb28 = epochDayOf(millisAt(2028, Calendar.FEBRUARY, 28))
        val feb29 = epochDayOf(millisAt(2028, Calendar.FEBRUARY, 29))
        val mar1 = epochDayOf(millisAt(2028, Calendar.MARCH, 1))
        assertEquals(1, feb29 - feb28)
        assertEquals(1, mar1 - feb29)
    }

    @Test
    fun `startOfDayMillis lands on local midnight`() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startOfDayMillis(epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 25)))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `startOfDay strips the time without moving the date`() {
        val noon = millisAt(2026, Calendar.SEPTEMBER, 25, hour = 13, minute = 45)
        assertEquals(startOfDayMillis(epochDayOf(noon)), startOfDay(noon))
    }

    @Test
    fun `today is derived from the clock passed in, not the real one`() {
        val fixed = millisAt(2026, Calendar.SEPTEMBER, 25)
        assertEquals(epochDayOf(fixed), todayEpochDay(fixed))
    }
}
