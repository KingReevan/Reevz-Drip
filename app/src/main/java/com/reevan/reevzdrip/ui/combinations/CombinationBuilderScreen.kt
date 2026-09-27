package com.reevan.reevzdrip.ui.combinations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reevan.reevzdrip.R
import com.reevan.reevzdrip.data.Garment
import com.reevan.reevzdrip.ui.common.EmptyState
import com.reevan.reevzdrip.ui.common.GarmentImage

/**
 * Building an outfit: browse the wardrobe, tap what goes together, save.
 *
 * A full screen rather than a bottom sheet. The requirement describes this as "having a look at
 * the clothing items in the Wardrobe and picking the clothes" — that is a browsing task, and it
 * needs the whole screen to be one. A sheet would show two rows of garments over a dimmed
 * background, which is the opposite of having a look.
 *
 * Selection is shown two ways on purpose: the tapped card is outlined with a tick, and a running
 * count sits in the save bar. The card state answers "did that tap register", the count answers
 * "have I finished", and neither substitutes for the other.
 */
@Composable
fun CombinationBuilderScreen(
    wardrobe: List<Garment>,
    initialName: String?,
    initialSelection: List<Long>,
    isEditing: Boolean,
    onCancel: () -> Unit,
    onSave: (name: String?, garmentIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(initialName.orEmpty()) }
    // A list rather than a Set so the save bar's count and the saved order are stable; the
    // collage re-sorts by type anyway, so this order is only ever about predictable UI.
    val selected = rememberSaveable(saver = longListSaver) {
        mutableStateListOf<Long>().apply { addAll(initialSelection) }
    }

    val canSave = selected.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        if (wardrobe.isEmpty()) {
            EmptyState(
                headline = "No clothes to pick from",
                hint = "Add a few things to your Wardrobe first, then come back and group them " +
                    "into an outfit.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    )
                }

                items(wardrobe, key = { it.id }) { garment ->
                    PickableGarment(
                        garment = garment,
                        selected = garment.id in selected,
                        onToggle = {
                            if (garment.id in selected) {
                                selected.remove(garment.id)
                            } else {
                                selected.add(garment.id)
                            }
                        },
                    )
                }
            }
        }

        // Three columns is right here, against the Wardrobe's two: this is a picking task, not a
        // recognising one, and seeing more of the wardrobe at once matters more than seeing each
        // garment large.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selected.isEmpty()) {
                        "Pick the clothes for this outfit"
                    } else {
                        "${selected.size} selected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(
                    onClick = { onSave(name, selected.toList()) },
                    enabled = canSave,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(if (isEditing) "Save" else "Create")
                }
            }
        }
    }
}

/** One wardrobe garment in the picker, with its selected state. */
@Composable
private fun PickableGarment(
    garment: Garment,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                    } else {
                        Modifier
                    },
                )
                // The whole tile is the target, so the tap area matches what the eye reads as
                // one thing — comfortably past 48dp at this cell size. `toggleable` rather than
                // `clickable` so a screen reader announces it as a checkbox that is on or off,
                // which is what it actually is.
                .toggleable(
                    value = selected,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ),
        ) {
            GarmentImage(
                photoName = garment.photoName,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                placeholderIcon = 22.dp,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = garment.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (garment.archived) {
            // Only ever shown while editing an outfit that already holds this garment; it says
            // why something is here that is no longer in the wardrobe.
            Text(
                text = "Removed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
            )
        }
    }
}

/**
 * Saver for the selected-ids list.
 *
 * `mutableStateListOf` is not saveable on its own, and losing the selection to the phone flipping
 * to dark at sunset mid-build would mean starting the outfit again (D15).
 */
private val longListSaver = androidx.compose.runtime.saveable.listSaver<
    androidx.compose.runtime.snapshots.SnapshotStateList<Long>,
    Long,
    >(
    save = { it.toList() },
    restore = { saved -> mutableStateListOf<Long>().apply { addAll(saved) } },
)
