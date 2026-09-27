package com.reevan.reevzdrip.ui.combinations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reevan.reevzdrip.data.CombinationWithGarments
import com.reevan.reevzdrip.ui.common.CombinationCollage

/**
 * One outfit in the list: its collage, and a line of text under it.
 *
 * The label falls back to a garment count when the outfit has no name, rather than leaving the row
 * blank. An unnamed outfit is the normal case — the collage is what you recognise it by — but an
 * empty line under a card reads as a rendering bug.
 */
@Composable
fun CombinationCard(
    entry: CombinationWithGarments,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        CombinationCollage(
            garments = entry.ordered,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = entry.combination.name ?: pieceLabel(entry.garments.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.combination.name != null) {
                Text(
                    text = pieceLabel(entry.garments.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** "1 piece" / "4 pieces" — the fallback label for an unnamed outfit. */
fun pieceLabel(count: Int): String = if (count == 1) "1 piece" else "$count pieces"
