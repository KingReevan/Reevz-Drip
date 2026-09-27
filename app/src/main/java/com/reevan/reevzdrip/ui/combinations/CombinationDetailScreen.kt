package com.reevan.reevzdrip.ui.combinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.reevan.reevzdrip.data.CombinationWithGarments
import com.reevan.reevzdrip.data.describeAudience
import com.reevan.reevzdrip.util.formatFullDate
import com.reevan.reevzdrip.ui.common.CombinationCollage
import com.reevan.reevzdrip.ui.common.GarmentImage

/**
 * One outfit, full size: its collage, what is in it, and — from Phase 7 — where it has been.
 *
 * The wear history is the reason this screen exists at all — requirement 9 is about tapping an
 * outfit to see when you wore it and who saw it, so you know whether repeating it is safe.
 *
 * Only **past** days appear. A day still ahead is an intention, and listing it as history would
 * claim people have seen something they have not.
 */
@Composable
fun CombinationDetailScreen(
    entry: CombinationWithGarments,
    history: WearHistoryState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        CombinationCollage(
            garments = entry.ordered,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .padding(top = 0.dp),
            tileIcon = 28.dp,
        )

        Text(
            text = entry.combination.name ?: "Unnamed outfit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = pieceLabel(entry.garments.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text(
            text = "In this outfit",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        entry.ordered.forEach { garment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GarmentImage(
                    photoName = garment.photoName,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    placeholderIcon = 18.dp,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = garment.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (garment.archived) {
                            "${garment.type.label} · removed from wardrobe"
                        } else {
                            garment.type.label
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text(
            text = "Where you've worn it",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        when {
            // One frame at most. Saying "never worn" before the answer is in would be a lie about
            // the one thing this screen exists to be trusted on.
            !history.loaded -> Unit

            history.isEmpty -> Text(
                text = "You haven't worn this yet. Once a day you planned it for has passed, it " +
                    "shows up here with who saw it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            else -> history.occasions.forEach { occasion ->
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = formatFullDate(occasion.entry.day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = describeAudience(occasion.groupNames),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
