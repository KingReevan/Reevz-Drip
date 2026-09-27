package com.reevan.reevzdrip.ui.wardrobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzdrip.R
import com.reevan.reevzdrip.data.Garment
import com.reevan.reevzdrip.data.GarmentType
import com.reevan.reevzdrip.data.GarmentWithUsage
import com.reevan.reevzdrip.ui.common.EmptyState

/**
 * The Wardrobe: every garment as a card grid, plus full CRUD.
 *
 * Two columns rather than three. Three fits more on screen, but the card is how you recognise a
 * garment, and at three-across on a phone the photo slot is small enough that a navy shirt and a
 * black one stop being distinguishable — which defeats the point.
 */
@Composable
fun WardrobeScreen(
    modifier: Modifier = Modifier,
    viewModel: WardrobeViewModel = viewModel(factory = WardrobeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Ids rather than Garment objects, and rememberSaveable rather than remember, for two reasons:
    // a configuration change (the phone flipping to dark at sunset, say) must not close a
    // half-filled form, and holding an id means the sheet always reflects the row as it is now
    // rather than a snapshot taken when it opened.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    val editing: Garment? =
        editingId?.let { id -> state.all.firstOrNull { it.garment.id == id }?.garment }
    val pendingDelete: GarmentWithUsage? =
        pendingDeleteId?.let { id -> state.all.firstOrNull { it.garment.id == id } }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            !state.loaded -> Unit // One frame at most; an empty state here would flash.

            state.isEmpty -> EmptyState(
                headline = "Your wardrobe is empty",
                hint = "Add the clothes you own, and you can start building outfits from them.",
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    // Clears the FAB, so the last row is never trapped underneath it.
                    bottom = 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    TypeFilterRow(
                        selected = state.filter,
                        onSelect = viewModel::setFilter,
                    )
                }

                if (state.filteredToNothing) {
                    item(
                        span = {
                            androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)
                        },
                    ) {
                        Text(
                            text = "Nothing of this type yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 32.dp),
                        )
                    }
                }

                items(state.visible, key = { it.garment.id }) { row ->
                    GarmentCard(
                        garment = row.garment,
                        onClick = {
                            editingId = row.garment.id
                            sheetOpen = true
                        },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingId = null
                sheetOpen = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Add clothing",
            )
        }
    }

    if (sheetOpen) {
        val current = editing
        GarmentEditorSheet(
            // Keyed so that reopening the sheet on a different garment rebuilds the form state
            // instead of showing the previous garment's name.
            key = current?.id,
            existing = current,
            onDismiss = { sheetOpen = false },
            onSave = { name, type, photo ->
                viewModel.save(
                    id = current?.id,
                    name = name,
                    type = type,
                    existing = current,
                    photoEdit = photo,
                )
                sheetOpen = false
            },
            onDelete = current?.let {
                {
                    sheetOpen = false
                    pendingDeleteId = it.id
                }
            },
        )
    }

    // Deleting a garment is not recoverable and there is no undo, so it asks first — and it says
    // which of the two things will actually happen, because they are meaningfully different.
    pendingDelete?.let { row ->
        val usedCount = row.combinationCount
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete ${row.garment.name}?") },
            text = {
                Text(
                    if (usedCount > 0) {
                        val outfits = if (usedCount == 1) "1 outfit" else "$usedCount outfits"
                        "This removes it from your wardrobe. It stays in the $outfits it is " +
                            "already part of, so your history of what you wore does not change."
                    } else {
                        "This removes it from your wardrobe. It cannot be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(row.garment, usedCount)
                        pendingDeleteId = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            },
        )
    }
}

/** Type filter. Tapping the active chip clears it, so there is no separate "All" to maintain. */
@Composable
private fun TypeFilterRow(
    selected: GarmentType?,
    onSelect: (GarmentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GarmentType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.label) },
            )
        }
    }
}
