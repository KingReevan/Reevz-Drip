package com.reevan.reevzdrip.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reevan.reevzdrip.data.Garment

/**
 * How many garment tiles a collage shows, and how many are left over.
 *
 * Pure and separate from the drawing so the awkward cases — one garment, exactly four, a dozen —
 * can be pinned down in unit tests rather than discovered by scrolling.
 */
data class CollagePlan(val visible: Int, val overflow: Int)

/** Most tiles a collage draws before it starts counting the rest. */
const val MAX_COLLAGE_TILES = 4

/**
 * Decides the split between tiles shown and tiles summarised as "+N".
 *
 * Up to [MAX_COLLAGE_TILES] garments each get a tile. Beyond that, the last tile becomes the
 * counter, so five garments show three photos and "+2" rather than four photos and a hidden
 * remainder — a card that silently drops garments is worse than one that admits it.
 */
fun collagePlan(garmentCount: Int, maxTiles: Int = MAX_COLLAGE_TILES): CollagePlan {
    val count = garmentCount.coerceAtLeast(0)
    if (count <= maxTiles) return CollagePlan(visible = count, overflow = 0)
    val visible = maxTiles - 1
    return CollagePlan(visible = visible, overflow = count - visible)
}

/**
 * An outfit drawn as a photo collage of the garments in it.
 *
 * The layout changes with the number of tiles rather than always being a 2x2 grid, because a
 * two-garment outfit rendered into a quarter-filled square reads as broken rather than as minimal:
 *
 * - **1** — one photo, filling the square.
 * - **2** — two halves, side by side.
 * - **3** — one full-height tile on the left, two stacked on the right. Combined with the
 *   type ordering, the largest tile is the top, which is what identifies an outfit fastest.
 * - **4 or more** — a 2x2 grid, the last cell counting any remainder.
 *
 * [garments] is expected in display order already — `CombinationWithGarments.ordered`.
 */
@Composable
fun CombinationCollage(
    garments: List<Garment>,
    modifier: Modifier = Modifier,
    spacing: Dp = 2.dp,
    tileIcon: Dp = 20.dp,
) {
    val plan = collagePlan(garments.size)
    val tiles = garments.take(plan.visible)

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when (tiles.size) {
            0 -> Text(
                text = "Empty outfit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            1 -> Tile(tiles[0], tileIcon, Modifier.fillMaxSize())

            2 -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                tiles.forEach { Tile(it, tileIcon, Modifier.weight(1f).fillMaxHeight()) }
            }

            3 -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                Tile(tiles[0], tileIcon, Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    Tile(tiles[1], tileIcon, Modifier.weight(1f).fillMaxWidth())
                    Tile(tiles[2], tileIcon, Modifier.weight(1f).fillMaxWidth())
                }
            }

            else -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    Tile(tiles[0], tileIcon, Modifier.weight(1f).fillMaxHeight())
                    Tile(tiles[1], tileIcon, Modifier.weight(1f).fillMaxHeight())
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    Tile(tiles[2], tileIcon, Modifier.weight(1f).fillMaxHeight())
                    if (plan.overflow > 0) {
                        OverflowTile(plan.overflow, Modifier.weight(1f).fillMaxHeight())
                    } else {
                        Tile(tiles[3], tileIcon, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(garment: Garment, iconSize: Dp, modifier: Modifier) {
    GarmentImage(
        photoName = garment.photoName,
        // The card names the outfit; reading out every garment in the collage as well would make
        // a screen reader recite the whole wardrobe to get past one card.
        contentDescription = null,
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        placeholderIcon = iconSize,
    )
}

/** The "+N" cell standing in for the garments that did not get a tile. */
@Composable
private fun OverflowTile(count: Int, modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$count",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
