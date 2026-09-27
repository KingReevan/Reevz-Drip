package com.reevan.reevzdrip.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reevan.reevzdrip.data.PeopleGroup
import com.reevan.reevzdrip.ui.common.CombinationCollage
import com.reevan.reevzdrip.util.formatFullDate

/**
 * One group: its name, and — from Phase 7 — everything it has seen you wear.
 *
 * That history is the reason the Groups section exists at all — requirement 10 is about opening a
 * group and checking what it has already seen, so you know whether an outfit is safe to repeat.
 *
 * Only **past** days appear: a day still ahead is an intention, not something they have seen.
 */
@Composable
fun GroupDetailScreen(
    group: PeopleGroup,
    history: SeenHistoryState,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = group.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Added ${formatFullDate(group.createdOn)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onRename) { Text("Rename") }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text(
            text = "What they've seen",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        when {
            // One frame at most; "seen nothing" before the answer arrives would be a lie.
            !history.loaded -> Unit

            history.isEmpty -> Text(
                text = "They haven't seen you in anything yet. Plan a day with this group, and " +
                    "once it has passed the outfit shows up here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            else -> history.outfits.forEach { seen ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CombinationCollage(
                        garments = seen.combination.ordered,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        tileIcon = 11.dp,
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = seen.combination.combination.name
                                ?: "${seen.combination.garments.size} pieces",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = formatFullDate(seen.entry.day),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
