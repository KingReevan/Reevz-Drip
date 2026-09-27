package com.reevan.reevzdrip.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dates in this app live in two representations, on purpose:
 *
 * - **Epoch day** (`Int`) is what gets **stored**. It is a plain calendar-date number with no time
 *   and no zone, so `day < today` is an honest comparison, plan entries sort correctly, and the
 *   history queries are trivial. A timestamp would force every one of those to worry about where
 *   midnight fell.
 * - **Epoch millis** (`Long`) is what the **calendar UI** works in, because week/month grid maths
 *   is what [Calendar] is good at.
 *
 * Conversion happens at the persistence boundary — [epochDayOf] going in, [startOfDayMillis]
 * coming out — and nowhere else.
 *
 * `java.time` would make all of this shorter, but it needs API 26 and this app supports minSdk 24.
 * Enabling core library desugaring would buy it; revisit when Plan (Phase 5) lands and the date
 * logic gets heavier. Sibling app Reevz Mealz stores millis instead and does the same maths with
 * [Calendar], so the helpers below are deliberately familiar.
 */

private const val DAY_MILLIS = 86_400_000L

private fun utcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

/** Local midnight of the day containing [millis]. */
fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * The epoch day of the **local** calendar date containing [millis].
 *
 * The local year/month/day is re-anchored in UTC before dividing, so the result is the calendar
 * date the user would read off a wall calendar — not "how many 24-hour blocks since 1970 in this
 * zone", which drifts across DST and changes meaning if the phone crosses a timezone.
 */
fun epochDayOf(millis: Long): Int {
    val local = Calendar.getInstance()
    local.timeInMillis = millis
    val utc = utcCalendar()
    utc.clear()
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    return (utc.timeInMillis / DAY_MILLIS).toInt()
}

/** Local midnight of [epochDay] — the inverse of [epochDayOf]. */
fun startOfDayMillis(epochDay: Int): Long {
    val utc = utcCalendar()
    utc.timeInMillis = epochDay * DAY_MILLIS
    val local = Calendar.getInstance()
    local.clear()
    local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    return local.timeInMillis
}

/**
 * Today as an epoch day.
 *
 * [now] is a parameter rather than a bare call to the clock so anything built on top of this
 * stays testable — the "is this day still plannable" rule in Phase 5 especially.
 */
fun todayEpochDay(now: Long = System.currentTimeMillis()): Int = epochDayOf(now)

/** Short date for a list row, e.g. "26 Aug". */
fun formatShortDate(epochDay: Int): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(startOfDayMillis(epochDay)))

/** Full date for a history entry, e.g. "Tue 26 Aug 2026". */
fun formatFullDate(epochDay: Int): String =
    SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()).format(Date(startOfDayMillis(epochDay)))

// --- Calendar helpers, all in epoch days ------------------------------------------------
//
// Ported from Reevz Mealz's `util/Dates.kt`, converted from millis to epoch days. The conversion
// is worth it: `addDays` becomes exact integer arithmetic instead of a Calendar round-trip that
// has to renormalise to midnight afterwards so a DST transition cannot leave it an hour short.
// Everything that genuinely needs a calendar — month lengths, where a week starts — still goes
// through [Calendar], because that is what it is good at.

private const val DAYS_IN_WEEK = 7

private fun calendarAt(epochDay: Int): Calendar =
    Calendar.getInstance().apply { timeInMillis = startOfDayMillis(epochDay) }

/** [days] later than [epochDay]. Exact: an epoch day is a count of days, so this is addition. */
fun addDays(epochDay: Int, days: Int): Int = epochDay + days

/** The first day of the month containing [epochDay]. */
fun startOfMonth(epochDay: Int): Int {
    val calendar = calendarAt(epochDay)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    return epochDayOf(calendar.timeInMillis)
}

/**
 * The first day of the month [months] away from the one containing [epochDay].
 *
 * Anchored to the first of the month before adding, so stepping forward from the 31st cannot land
 * on a month that has no 31st and silently skip one.
 */
fun addMonths(epochDay: Int, months: Int): Int {
    val calendar = calendarAt(startOfMonth(epochDay))
    calendar.add(Calendar.MONTH, months)
    return epochDayOf(calendar.timeInMillis)
}

/** The seven days of the week containing [epochDay], from the locale's first day of the week. */
fun weekDaysOf(epochDay: Int): List<Int> {
    val calendar = calendarAt(epochDay)
    val firstDayOfWeek = calendar.firstDayOfWeek
    while (calendar.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }
    val weekStart = epochDayOf(calendar.timeInMillis)
    return (0 until DAYS_IN_WEEK).map { weekStart + it }
}

/**
 * The month containing [epochDay] as calendar cells: leading and trailing nulls pad the grid so
 * every row holds exactly seven entries and real days sit under the right weekday column.
 */
fun monthGridOf(epochDay: Int): List<Int?> {
    val monthStart = startOfMonth(epochDay)
    val calendar = calendarAt(monthStart)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks =
        ((calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek) + DAYS_IN_WEEK) %
            DAYS_IN_WEEK

    val cells = ArrayList<Int?>(leadingBlanks + daysInMonth)
    repeat(leadingBlanks) { cells.add(null) }
    for (offset in 0 until daysInMonth) {
        cells.add(monthStart + offset)
    }
    while (cells.size % DAYS_IN_WEEK != 0) {
        cells.add(null)
    }
    return cells
}

/** Weekday column headings, ordered from the locale's first day of the week. */
fun weekdayHeadings(now: Long = System.currentTimeMillis()): List<String> =
    weekDaysOf(todayEpochDay(now)).map { formatWeekdayShort(it).take(1) }

/** Always-qualified month heading for the picker, e.g. "September 2026". */
fun formatMonthAndYear(epochDay: Int): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startOfDayMillis(epochDay)))

/** Abbreviated weekday, e.g. "Tue". */
fun formatWeekdayShort(epochDay: Int): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(startOfDayMillis(epochDay)))

/** Day of the month as a bare number, e.g. "26". */
fun formatDayOfMonth(epochDay: Int): String =
    SimpleDateFormat("d", Locale.getDefault()).format(Date(startOfDayMillis(epochDay)))

/**
 * A date the way you would say it out loud: "Today", "Tomorrow", "Yesterday", or the full date.
 *
 * The three near days carry their meaning without arithmetic — "Tomorrow" is the whole point of
 * this app, and making the user work out that Mon 28 Sept is tomorrow defeats it.
 */
fun formatRelativeDay(epochDay: Int, today: Int): String = when (epochDay - today) {
    0 -> "Today"
    1 -> "Tomorrow"
    -1 -> "Yesterday"
    else -> formatFullDate(epochDay)
}

/**
 * Milliseconds from [now] until the next local midnight.
 *
 * Home has to roll over to the new day while it is open — the requirement is that today's outfits
 * appear "when that day arrives", and an app you have to kill and reopen at midnight does not do
 * that. A screen that waits exactly this long and then re-reads the date is precise and costs
 * nothing, where polling every minute would burn wakeups all day to catch one instant.
 *
 * Never returns zero or less: a non-positive delay would spin. Clock changes and DST are handled
 * by asking [startOfDayMillis] for the next calendar day rather than adding 24 hours, so a 23- or
 * 25-hour day still lands on midnight.
 */
fun millisUntilNextMidnight(now: Long = System.currentTimeMillis()): Long {
    val nextMidnight = startOfDayMillis(epochDayOf(now) + 1)
    return (nextMidnight - now).coerceAtLeast(1L)
}
