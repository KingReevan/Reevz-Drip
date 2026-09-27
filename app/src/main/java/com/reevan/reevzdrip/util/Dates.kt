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
