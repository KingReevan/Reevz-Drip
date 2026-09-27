package com.reevan.reevzdrip.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reevan.reevzdrip.util.addDays
import com.reevan.reevzdrip.util.addMonths
import com.reevan.reevzdrip.util.formatDayOfMonth
import com.reevan.reevzdrip.util.formatMonthAndYear
import com.reevan.reevzdrip.util.formatWeekdayShort
import com.reevan.reevzdrip.util.monthGridOf
import com.reevan.reevzdrip.util.weekDaysOf
import com.reevan.reevzdrip.util.weekdayHeadings

enum class PickerMode(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
}

/**
 * Day picker, as a one-week strip or a month grid.
 *
 * Ported from Reevz Mealz, converted from millis to epoch days — which removes a whole class of
 * bug, since "the next day" is now `+ 1` rather than a Calendar round-trip that has to be
 * renormalised to midnight afterwards.
 *
 * Both modes exist because the requirement asks to plan "for a day of the week. Or month. Or
 * year." The week strip is for the common case — tomorrow, this weekend — and the month grid for
 * reaching a date further out without pressing `›` fourteen times.
 *
 * [markedDays] are days worth a dot: here, days that already have something planned.
 *
 * A day the caller cannot use is **dimmed and disarmed, not hidden**: these are calendars, and a
 * calendar missing half its days is harder to read than one showing which days are out of reach.
 * `selectable(enabled = false)` is what actually stops the tap — greying the text alone would
 * leave a control that looks dead and still works.
 */
@Composable
fun DayPicker(
    mode: PickerMode,
    onModeChange: (PickerMode) -> Unit,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    anchorDay: Int,
    onAnchorChange: (Int) -> Unit,
    markedDays: Set<Int>,
    today: Int,
    modifier: Modifier = Modifier,
    isSelectable: (Int) -> Boolean = { true },
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(40.dp),
        ) {
            PickerMode.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = mode == entry,
                    onClick = { onModeChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PickerMode.entries.size,
                    ),
                    label = { Text(entry.label) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    onAnchorChange(
                        if (mode == PickerMode.WEEK) {
                            addDays(anchorDay, -DAYS_IN_WEEK)
                        } else {
                            addMonths(anchorDay, -1)
                        },
                    )
                },
            ) {
                Text("‹")
            }
            Text(
                text = formatMonthAndYear(anchorDay),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    onAnchorChange(
                        if (mode == PickerMode.WEEK) {
                            addDays(anchorDay, DAYS_IN_WEEK)
                        } else {
                            addMonths(anchorDay, 1)
                        },
                    )
                },
            ) {
                Text("›")
            }
        }

        when (mode) {
            PickerMode.WEEK -> WeekStrip(
                anchorDay = anchorDay,
                selectedDay = selectedDay,
                onSelectDay = onSelectDay,
                markedDays = markedDays,
                today = today,
                isSelectable = isSelectable,
            )

            PickerMode.MONTH -> MonthGrid(
                anchorDay = anchorDay,
                selectedDay = selectedDay,
                onSelectDay = onSelectDay,
                markedDays = markedDays,
                today = today,
                isSelectable = isSelectable,
            )
        }
    }
}

@Composable
private fun WeekStrip(
    anchorDay: Int,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    markedDays: Set<Int>,
    today: Int,
    isSelectable: (Int) -> Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        weekDaysOf(anchorDay).forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatWeekdayShort(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DayCell(
                    day = day,
                    selected = day == selectedDay,
                    isToday = day == today,
                    marked = day in markedDays,
                    enabled = isSelectable(day),
                    onClick = { onSelectDay(day) },
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    anchorDay: Int,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    markedDays: Set<Int>,
    today: Int,
    isSelectable: (Int) -> Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayHeadings().forEach { heading ->
                Text(
                    text = heading,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        monthGridOf(anchorDay).chunked(DAYS_IN_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                selected = day == selectedDay,
                                isToday = day == today,
                                marked = day in markedDays,
                                enabled = isSelectable(day),
                                onClick = { onSelectDay(day) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    selected: Boolean,
    isToday: Boolean,
    marked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            // The cell is drawn at CELL_SIZE, which is under the 48dp minimum touch target. This
            // reserves the full 48dp for the *tap* without growing the circle — a calendar of
            // 48dp discs is a lot of ink for information that reads better small.
            .minimumInteractiveComponentSize()
            .padding(2.dp)
            .size(CELL_SIZE)
            .clip(CircleShape)
            .selectable(selected = selected, enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDayOfMonth(day),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        // Dim, but still legible: outlineVariant would vanish on the surface.
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                Box(
                    modifier = Modifier
                        .size(PLAN_DOT_SIZE)
                        .clip(CircleShape),
                ) {
                    if (marked) {
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(PLAN_DOT_SIZE),
                            content = {},
                        )
                    }
                }
            }
        }
    }
}

private const val DAYS_IN_WEEK = 7
private val CELL_SIZE = 42.dp
private val PLAN_DOT_SIZE = 4.dp
