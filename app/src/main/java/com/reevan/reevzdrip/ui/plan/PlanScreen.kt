package com.reevan.reevzdrip.ui.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzdrip.R
import com.reevan.reevzdrip.data.PlanEntryDetails
import com.reevan.reevzdrip.ui.common.CombinationCollage
import com.reevan.reevzdrip.util.formatRelativeDay

private enum class Mode { DAY, ASSIGN }

/**
 * The Plan section: pick a day, then say what you are wearing and who will see it.
 *
 * The day picker stays at the top and the day's assignments sit under it, so changing day is one
 * tap from reading the result — the browsing motion the requirement describes ("a day of the
 * week. Or month. Or year.").
 *
 * **Past days are visible but not editable** (D7). They are shown rather than hidden because a
 * past day is the record of what you wore, and looking it up is half the point of the app.
 */
@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableStateOf(Mode.DAY) }
    var pickerMode by rememberSaveable { mutableStateOf(PickerMode.WEEK) }
    var anchorDay by rememberSaveable { mutableIntStateOf(0) }
    var editingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingRemoveId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Both are derived, not authored: the LaunchedEffects below recompute them from state that
    // *is* saveable, so a plain remember is correct here and a Saver would be ceremony.
    var warnings by remember { mutableStateOf(emptyList<RepeatWarning>()) }
    var unavailable by remember { mutableStateOf(emptySet<Long>()) }

    // The anchor starts on whatever day the ViewModel opened with, and is only steered by the
    // user's ‹ › afterwards. Initialised lazily because `today` is not known at first composition.
    LaunchedEffect(state.loaded) {
        if (state.loaded && anchorDay == 0) anchorDay = state.selectedDay
    }

    val editing = editingEntryId?.let { id -> state.entries.firstOrNull { it.entry.id == id } }
    val pendingRemove = pendingRemoveId?.let { id -> state.entries.firstOrNull { it.entry.id == id } }

    // Refresh which outfits are already on this day whenever the assign screen opens.
    LaunchedEffect(mode, state.selectedDay, editingEntryId) {
        if (mode == Mode.ASSIGN) {
            unavailable = viewModel.alreadyOn(state.selectedDay, editingEntryId)
        }
    }

    BackHandler(enabled = mode != Mode.DAY) {
        mode = Mode.DAY
        editingEntryId = null
        warnings = emptyList()
    }

    when (mode) {
        Mode.ASSIGN -> AssignOutfitScreen(
            day = state.selectedDay,
            today = state.today,
            combinations = state.combinations,
            groups = state.groups,
            initialCombinationId = editing?.combination?.combination?.id,
            initialGroupIds = editing?.groups?.map { it.id }.orEmpty(),
            unavailableCombinationIds = unavailable,
            warnings = warnings,
            onSelectionChange = { combinationId, groupIds ->
                warnings = if (combinationId == null) {
                    emptyList()
                } else {
                    viewModel.warningsFor(
                        combinationId = combinationId,
                        groupIds = groupIds,
                        targetDay = state.selectedDay,
                        excludingEntryId = editingEntryId,
                    )
                }
            },
            onCancel = {
                mode = Mode.DAY
                editingEntryId = null
                warnings = emptyList()
            },
            onSave = { combinationId, groupIds ->
                viewModel.assign(
                    entryId = editingEntryId,
                    day = state.selectedDay,
                    combinationId = combinationId,
                    groupIds = groupIds,
                )
                mode = Mode.DAY
                editingEntryId = null
                warnings = emptyList()
            },
            modifier = modifier,
        )

        Mode.DAY -> Column(modifier = modifier.fillMaxSize()) {
            DayPicker(
                mode = pickerMode,
                onModeChange = { pickerMode = it },
                selectedDay = state.selectedDay,
                onSelectDay = viewModel::selectDay,
                anchorDay = if (anchorDay == 0) state.selectedDay else anchorDay,
                onAnchorChange = { anchorDay = it },
                markedDays = state.plannedDays,
                today = state.today,
            )

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatRelativeDay(state.selectedDay, state.today),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (state.canPlan) {
                    OutlinedButton(
                        onClick = {
                            editingEntryId = null
                            mode = Mode.ASSIGN
                        },
                        enabled = !state.missingPrerequisites,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Outfit", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !state.loaded -> Unit

                    state.isEmptyDay -> DayEmptyState(
                        canPlan = state.canPlan,
                        missingPrerequisites = state.missingPrerequisites,
                        noCombinations = state.combinations.isEmpty(),
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.entries, key = { it.entry.id }) { details ->
                            PlanEntryCard(
                                details = details,
                                editable = state.canPlan,
                                onEdit = {
                                    editingEntryId = details.entry.id
                                    mode = Mode.ASSIGN
                                },
                                onRemove = { pendingRemoveId = details.entry.id },
                            )
                        }

                        if (!state.canPlan) {
                            item {
                                Text(
                                    text = "This day has passed, so what you wore stays as it is.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingRemove?.let { details ->
        AlertDialog(
            onDismissRequest = { pendingRemoveId = null },
            title = { Text("Remove this outfit from the day?") },
            text = {
                Text(
                    "The outfit itself stays in Outfits. Only the plan for " +
                        "${formatRelativeDay(details.entry.day, state.today).lowercase()} changes.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.remove(details.entry)
                        pendingRemoveId = null
                    },
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveId = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DayEmptyState(
    canPlan: Boolean,
    missingPrerequisites: Boolean,
    noCombinations: Boolean,
) {
    val hint = when {
        !canPlan -> "Nothing was planned for this day."
        missingPrerequisites && noCombinations ->
            "Build an outfit in Outfits first, then plan a day around it."
        missingPrerequisites -> "Add a group in Groups first — an outfit needs someone to see it."
        else -> "Add an outfit and say who'll see you in it."
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nothing planned",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** One outfit assigned to the day, with the groups who will see it. */
@Composable
private fun PlanEntryCard(
    details: PlanEntryDetails,
    editable: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CombinationCollage(
                garments = details.combination.ordered,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp)),
                tileIcon = 12.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = details.combination.combination.name
                        ?: "${details.combination.garments.size} pieces",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = details.groupNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (details.combination.combination.archived) {
                    Text(
                        text = "Outfit removed from Outfits",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        if (editable) {
            Row(modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)) {
                TextButton(onClick = onEdit) { Text("Change") }
                TextButton(onClick = onRemove) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
