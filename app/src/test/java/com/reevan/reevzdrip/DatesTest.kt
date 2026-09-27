package com.reevan.reevzdrip

import com.reevan.reevzdrip.util.addDays
import com.reevan.reevzdrip.util.addMonths
import com.reevan.reevzdrip.util.epochDayOf
import com.reevan.reevzdrip.util.formatRelativeDay
import com.reevan.reevzdrip.util.millisUntilNextMidnight
import com.reevan.reevzdrip.util.monthGridOf
import com.reevan.reevzdrip.util.startOfMonth
import com.reevan.reevzdrip.util.weekDaysOf
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

    // --- calendar helpers (Phase 5) ------------------------------------------------------

    @Test
    fun `adding days is exact arithmetic`() {
        val day = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 27))
        assertEquals(day + 1, addDays(day, 1))
        assertEquals(day - 1, addDays(day, -1))
        assertEquals(day, addDays(day, 0))
    }

    @Test
    fun `adding days crosses a month boundary correctly`() {
        val sep30 = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 30))
        val oct1 = epochDayOf(millisAt(2026, Calendar.OCTOBER, 1))
        assertEquals(oct1, addDays(sep30, 1))
    }

    @Test
    fun `startOfMonth lands on the first`() {
        val mid = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 27))
        val first = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 1))
        assertEquals(first, startOfMonth(mid))
        assertEquals(first, startOfMonth(first))
    }

    @Test
    fun `addMonths never skips a month when the day does not exist in the next one`() {
        // Stepping forward from the 31st is the classic way to land two months on.
        val jan31 = epochDayOf(millisAt(2026, Calendar.JANUARY, 31))
        val feb1 = epochDayOf(millisAt(2026, Calendar.FEBRUARY, 1))
        assertEquals(feb1, addMonths(jan31, 1))
    }

    @Test
    fun `addMonths walks forwards and backwards symmetrically`() {
        val sep = startOfMonth(epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 27)))
        assertEquals(sep, addMonths(addMonths(sep, 5), -5))
        assertEquals(sep, addMonths(addMonths(sep, -13), 13))
    }

    @Test
    fun `a week is seven consecutive days containing the given day`() {
        val day = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 27))
        val week = weekDaysOf(day)
        assertEquals(7, week.size)
        assertEquals(week, week.sorted())
        week.zipWithNext().forEach { (a, b) -> assertEquals(1, b - a) }
        assertTrue("the week must contain its own day", day in week)
    }

    @Test
    fun `every day of a week returns that same week`() {
        val day = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 27))
        val week = weekDaysOf(day)
        week.forEach { assertEquals(week, weekDaysOf(it)) }
    }

    @Test
    fun `a month grid is whole weeks and holds every day of the month`() {
        val sep = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 15))
        val grid = monthGridOf(sep)
        assertEquals("grid must be whole rows of seven", 0, grid.size % 7)

        val days = grid.filterNotNull()
        assertEquals(30, days.size) // September
        assertEquals(startOfMonth(sep), days.first())
        days.zipWithNext().forEach { (a, b) -> assertEquals(1, b - a) }
    }

    @Test
    fun `a month grid pads a leap February to whole weeks`() {
        val feb = epochDayOf(millisAt(2028, Calendar.FEBRUARY, 10))
        val grid = monthGridOf(feb)
        assertEquals(0, grid.size % 7)
        assertEquals(29, grid.filterNotNull().size)
    }

    @Test
    fun `the near days are named rather than dated`() {
        val today = epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 27))
        assertEquals("Today", formatRelativeDay(today, today))
        assertEquals("Tomorrow", formatRelativeDay(today + 1, today))
        assertEquals("Yesterday", formatRelativeDay(today - 1, today))
        // Anything further out falls back to a real date rather than "in 2 days".
        assertTrue(formatRelativeDay(today + 2, today).contains("2026"))
    }

    // --- midnight rollover (Phase 6) -----------------------------------------------------

    @Test
    fun `the wait lands exactly on the next local midnight`() {
        val now = millisAt(2026, Calendar.SEPTEMBER, 27, hour = 13, minute = 45)
        val landsOn = now + millisUntilNextMidnight(now)
        assertEquals(startOfDayMillis(epochDayOf(now) + 1), landsOn)
    }

    @Test
    fun `waiting from any hour of a day lands on the same midnight`() {
        val expected = startOfDayMillis(epochDayOf(millisAt(2026, Calendar.SEPTEMBER, 27)) + 1)
        for (hour in 0..23) {
            val now = millisAt(2026, Calendar.SEPTEMBER, 27, hour = hour, minute = 30)
            assertEquals("hour $hour", expected, now + millisUntilNextMidnight(now))
        }
    }

    @Test
    fun `the wait is always positive, so the rollover loop cannot spin`() {
        // A zero or negative delay would busy-loop the Home screen for the whole day.
        for (hour in 0..23) {
            val now = millisAt(2026, Calendar.SEPTEMBER, 27, hour = hour, minute = 59)
            assertTrue("hour $hour", millisUntilNextMidnight(now) > 0)
        }
    }

    @Test
    fun `a moment before midnight waits a moment, not a day`() {
        val now = millisAt(2026, Calendar.SEPTEMBER, 27, hour = 23, minute = 59)
        val wait = millisUntilNextMidnight(now)
        assertTrue("waited ${wait}ms", wait in 1..(61L * 1000L))
    }

    @Test
    fun `the rollover crosses a month boundary`() {
        val now = millisAt(2026, Calendar.SEPTEMBER, 30, hour = 22)
        val nextDay = epochDayOf(now + millisUntilNextMidnight(now))
        assertEquals(epochDayOf(millisAt(2026, Calendar.OCTOBER, 1)), nextDay)
    }
}
