package com.reevan.reevzdrip.ui.groups

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzdrip.R
import com.reevan.reevzdrip.data.PeopleGroup
import com.reevan.reevzdrip.ui.common.EmptyState

/** List, or one group's detail. Same shape as Combinations — an id plus a mode, not a NavHost. */
private enum class Mode { LIST, DETAIL }

/**
 * The Groups section: the people who see your outfits.
 *
 * A plain list rather than the card grids used for clothes and outfits. A group is a word; there
 * is nothing to look at, and laying words out in a photo grid would be dressing up an address
 * book as a gallery.
 */
@Composable
fun GroupsScreen(
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableStateOf(Mode.LIST) }
    var activeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Derived from live state, so a group renamed or deleted underneath this screen cannot leave
    // a stale copy on show.
    val active = activeId?.let { id -> state.groups.firstOrNull { it.id == id } }
    val editing = editingId?.let { id -> state.groups.firstOrNull { it.id == id } }
    val pendingDelete = pendingDeleteId?.let { id -> state.groups.firstOrNull { it.id == id } }

    // Keep the ViewModel's notion of the open group in step with the screen's, from one place.
    LaunchedEffect(mode, activeId) {
        viewModel.openDetail(activeId.takeIf { mode == Mode.DETAIL })
    }

    // A group deleted from its own detail screen leaves nothing to detail.
    if (mode == Mode.DETAIL && state.loaded && active == null) {
        mode = Mode.LIST
        activeId = null
    }

    BackHandler(enabled = mode != Mode.LIST) {
        mode = Mode.LIST
        activeId = null
    }

    when (mode) {
        Mode.DETAIL -> active?.let { group ->
            GroupDetailScreen(
                group = group,
                history = history,
                onRename = {
                    editingId = group.id
                    sheetOpen = true
                },
                onDelete = { pendingDeleteId = group.id },
                modifier = modifier,
            )
        }

        Mode.LIST -> Box(modifier = modifier.fillMaxSize()) {
            when {
                !state.loaded -> Unit // One frame at most; an empty state here would flash.

                state.isEmpty -> EmptyState(
                    headline = "No groups yet",
                    hint = "Add the groups of people who see you — colleagues, college friends. " +
                        "Planning an outfit asks who'll be there, and that's how the app knows " +
                        "what not to repeat.",
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        // Clears the FAB, so the last row is never trapped underneath it.
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.groups, key = { it.id }) { group ->
                        GroupRow(
                            group = group,
                            onClick = {
                                activeId = group.id
                                mode = Mode.DETAIL
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
                    contentDescription = "New group",
                )
            }
        }
    }

    if (sheetOpen) {
        val current = editing
        GroupEditorSheet(
            key = current?.id,
            existing = current,
            allGroups = state.groups,
            onDismiss = { sheetOpen = false },
            onSave = { name ->
                viewModel.save(id = current?.id, name = name, existing = current)
                sheetOpen = false
            },
        )
    }

    pendingDelete?.let { group ->
        val seenCount = state.planUsage[group.id] ?: 0
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete ${group.name}?") },
            text = {
                Text(
                    if (seenCount > 0) {
                        val outfits = if (seenCount == 1) "1 planned outfit" else
                            "$seenCount planned outfits"
                        "This removes the group. It stays on the $outfits it's attached to, so " +
                            "your history of who saw what does not change."
                    } else {
                        "This removes the group. It cannot be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(group, seenCount)
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

@Composable
private fun GroupRow(
    group: PeopleGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_groups),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}
