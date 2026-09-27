package com.reevan.reevzdrip.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reevan.reevzdrip.R
import com.reevan.reevzdrip.data.CombinationWithGarments
import com.reevan.reevzdrip.data.PeopleGroup
import com.reevan.reevzdrip.ui.common.CombinationCollage
import com.reevan.reevzdrip.ui.common.EmptyState
import com.reevan.reevzdrip.util.formatShortDate
import com.reevan.reevzdrip.util.formatRelativeDay

/**
 * Assigning one outfit to one day, with the people who will see it.
 *
 * Both halves of the requirement are on one screen because they are one decision: *"Every time an
 * outfit is assigned, I have to also assign a 'Group'."* Splitting them into two steps would let
 * the user finish the first and think they were done.
 *
 * Save stays disabled until an outfit **and** at least one group are chosen — the rule is
 * enforced in the ViewModel too, but a disabled button explains itself where a silently-refused
 * save does not.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignOutfitScreen(
    day: Int,
    today: Int,
    combinations: List<CombinationWithGarments>,
    groups: List<PeopleGroup>,
    initialCombinationId: Long?,
    initialGroupIds: List<Long>,
    /** Outfits already on this day — choosing one again would clash with the unique index. */
    unavailableCombinationIds: Set<Long>,
    warnings: List<RepeatWarning>,
    onSelectionChange: suspend (combinationId: Long?, groupIds: Set<Long>) -> Unit,
    onCancel: () -> Unit,
    onSave: (combinationId: Long, groupIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var combinationId by rememberSaveable { mutableStateOf(initialCombinationId) }
    val selectedGroups = rememberSaveable(saver = longListSaver) {
        mutableStateListOf<Long>().apply { addAll(initialGroupIds) }
    }

    // Tell the caller whenever the choice changes, so it can go and fetch the repeat warning.
    // Keyed on both halves: a warning depends on the outfit *and* who is seeing it.
    LaunchedEffect(combinationId, selectedGroups.toList()) {
        onSelectionChange(combinationId, selectedGroups.toSet())
    }

    val canSave = combinationId != null && selectedGroups.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        if (combinations.isEmpty() || groups.isEmpty()) {
            EmptyState(
                headline = if (combinations.isEmpty()) "No outfits to assign" else "No groups yet",
                hint = if (combinations.isEmpty()) {
                    "Build an outfit in Outfits first, then come back and plan a day around it."
                } else {
                    "Add a group in Groups first — every outfit you plan needs to say who'll see it."
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = formatRelativeDay(day, today),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionLabel("Outfit")
                }

                items(combinations, key = { it.combination.id }) { entry ->
                    val id = entry.combination.id
                    val unavailable = id in unavailableCombinationIds
                    OutfitTile(
                        entry = entry,
                        selected = combinationId == id,
                        enabled = !unavailable,
                        onSelect = { combinationId = if (combinationId == id) null else id },
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionLabel("Who'll see it")
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        groups.forEach { group ->
                            FilterChip(
                                selected = group.id in selectedGroups,
                                onClick = {
                                    if (group.id in selectedGroups) {
                                        selectedGroups.remove(group.id)
                                    } else {
                                        selectedGroups.add(group.id)
                                    }
                                },
                                label = { Text(group.name) },
                            )
                        }
                    }
                }

                if (warnings.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        RepeatWarningPanel(warnings)
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        combinationId == null -> "Pick an outfit"
                        selectedGroups.isEmpty() -> "Pick who'll see it"
                        else -> "${selectedGroups.size} group" +
                            if (selectedGroups.size == 1) "" else "s"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(
                    onClick = {
                        val chosen = combinationId ?: return@Button
                        onSave(chosen, selectedGroups.toList())
                    },
                    enabled = canSave,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/**
 * The non-blocking repeat warning (D13).
 *
 * Deliberately not a dialog and never blocking: sometimes you *want* to repeat an outfit, and the
 * app has no business refusing. It states the fact and gets out of the way.
 */
@Composable
private fun RepeatWarningPanel(warnings: List<RepeatWarning>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "They've seen this before",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            warnings.forEach { warning ->
                Text(
                    text = if (warning.inPast) {
                        "${warning.groupName} saw this on ${formatShortDate(warning.day)}"
                    } else {
                        "${warning.groupName} are also seeing this on " +
                            formatShortDate(warning.day)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** One outfit in the picker. Dimmed and disarmed when it is already on this day. */
@Composable
private fun OutfitTile(
    entry: CombinationWithGarments,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f)) {
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
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ),
        ) {
            CombinationCollage(
                garments = entry.ordered,
                modifier = Modifier.fillMaxSize(),
                tileIcon = 14.dp,
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
            text = entry.combination.name ?: "${entry.garments.size} pieces",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (!enabled) {
            Text(
                text = "Already on this day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
            )
        }
    }
}

/** `mutableStateListOf` is not saveable on its own; a sunset theme flip must not lose the pick. */
private val longListSaver = listSaver<SnapshotStateList<Long>, Long>(
    save = { it.toList() },
    restore = { saved -> mutableStateListOf<Long>().apply { addAll(saved) } },
)
