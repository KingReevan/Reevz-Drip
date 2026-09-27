package com.reevan.reevzdrip.ui.combinations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
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
import com.reevan.reevzdrip.ui.common.EmptyState

/**
 * Which of the section's three screens is on show.
 *
 * Kept as an id plus a mode rather than as nested navigation. The section has one list and two
 * leaves; a NavHost would add routes, a back stack and an argument-encoding scheme to express
 * something a `when` already says, and the app shell deliberately works the same way (see
 * `ReevzDripApp`).
 */
private enum class Mode { LIST, BUILDER, DETAIL }

/**
 * Garments an outfit contains that the wardrobe no longer offers — i.e. archived ones.
 *
 * Pure, so the "edit an outfit holding a deleted garment" case is pinned down by a test rather
 * than by remembering to archive something before opening the builder.
 */
internal fun pickableExtras(
    wardrobe: List<com.reevan.reevzdrip.data.Garment>,
    inOutfit: List<com.reevan.reevzdrip.data.Garment>,
): List<com.reevan.reevzdrip.data.Garment> {
    val known = wardrobe.mapTo(HashSet()) { it.id }
    return inOutfit.filter { it.id !in known }
}

/**
 * The Combinations section: every saved outfit, plus building and inspecting one.
 *
 * Its own [BackHandler] sits inside the shell's, so back from the builder or the detail view
 * returns to the list rather than jumping out to Home. Compose gives the innermost handler
 * priority, which is exactly the nesting the user expects.
 */
@Composable
fun CombinationsScreen(
    modifier: Modifier = Modifier,
    viewModel: CombinationsViewModel = viewModel(factory = CombinationsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableStateOf(Mode.LIST) }
    var activeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Derived from live state rather than held, so an outfit edited or deleted underneath this
    // screen cannot leave a stale copy on show.
    val active = activeId?.let { id -> state.combinations.firstOrNull { it.combination.id == id } }
    val pendingDelete =
        pendingDeleteId?.let { id -> state.combinations.firstOrNull { it.combination.id == id } }

    // An outfit deleted from its own detail screen leaves nothing to detail.
    if (mode == Mode.DETAIL && state.loaded && active == null) {
        mode = Mode.LIST
        activeId = null
    }

    // Back retraces the way in rather than always jumping to the list: leaving the builder while
    // editing returns to the outfit you were editing, which is where Cancel goes too.
    BackHandler(enabled = mode != Mode.LIST) {
        when (mode) {
            Mode.BUILDER -> mode = if (active != null) Mode.DETAIL else Mode.LIST
            Mode.DETAIL -> {
                mode = Mode.LIST
                activeId = null
            }

            Mode.LIST -> Unit
        }
    }

    when (mode) {
        Mode.BUILDER -> CombinationBuilderScreen(
            // An outfit being edited may contain garments that have since been archived. They are
            // not in the wardrobe any more, but they *are* in this outfit — leaving them out
            // would show "3 selected" above two ticked tiles, and give no way to take one out.
            // They are appended only for the outfit that already has them; a brand-new outfit
            // still only ever sees the live wardrobe.
            wardrobe = state.wardrobe + pickableExtras(state.wardrobe, active?.ordered.orEmpty()),
            initialName = active?.combination?.name,
            initialSelection = active?.ordered?.map { it.id }.orEmpty(),
            isEditing = active != null,
            onCancel = {
                mode = if (active != null) Mode.DETAIL else Mode.LIST
            },
            onSave = { name, garmentIds ->
                viewModel.save(
                    id = active?.combination?.id,
                    name = name,
                    garmentIds = garmentIds,
                    createdOn = active?.combination?.createdOn,
                )
                mode = if (active != null) Mode.DETAIL else Mode.LIST
            },
            modifier = modifier,
        )

        Mode.DETAIL -> active?.let { entry ->
            CombinationDetailScreen(
                entry = entry,
                onEdit = { mode = Mode.BUILDER },
                onDelete = { pendingDeleteId = entry.combination.id },
                modifier = modifier,
            )
        }

        Mode.LIST -> Box(modifier = modifier.fillMaxSize()) {
            when {
                !state.loaded -> Unit // One frame at most; an empty state here would flash.

                state.isEmpty -> EmptyState(
                    headline = "No outfits yet",
                    hint = if (state.wardrobeIsEmpty) {
                        "Add some clothes to your Wardrobe first, then group them into an outfit."
                    } else {
                        "Group the clothes in your Wardrobe into outfits you can plan days around."
                    },
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        // Clears the FAB, so the last row is never trapped underneath it.
                        bottom = 96.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.combinations, key = { it.combination.id }) { entry ->
                        CombinationCard(
                            entry = entry,
                            onClick = {
                                activeId = entry.combination.id
                                mode = Mode.DETAIL
                            },
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    activeId = null
                    mode = Mode.BUILDER
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "New outfit",
                )
            }
        }
    }

    // Deleting an outfit does not touch the garments in it — they belong to the wardrobe.
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete ${entry.combination.name ?: "this outfit"}?") },
            text = {
                Text(
                    "The clothes in it stay in your Wardrobe. Only the grouping is removed, and " +
                        "it cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(entry.combination)
                        pendingDeleteId = null
                        activeId = null
                        mode = Mode.LIST
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
