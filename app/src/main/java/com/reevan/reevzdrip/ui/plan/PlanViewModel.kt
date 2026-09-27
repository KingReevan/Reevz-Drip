package com.reevan.reevzdrip.ui.plan

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzdrip.data.CombinationDao
import com.reevan.reevzdrip.data.CombinationWithGarments
import com.reevan.reevzdrip.data.DripDatabase
import com.reevan.reevzdrip.data.GroupDao
import com.reevan.reevzdrip.data.PeopleGroup
import com.reevan.reevzdrip.data.PlanDao
import com.reevan.reevzdrip.data.PlanEntry
import com.reevan.reevzdrip.data.PlanEntryDetails
import com.reevan.reevzdrip.util.todayEpochDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanUiState(
    /** The day being looked at, as an epoch day. */
    val selectedDay: Int = 0,
    /** What is assigned to [selectedDay]. */
    val entries: List<PlanEntryDetails> = emptyList(),
    /** Days with anything planned — the dots on the calendar. */
    val plannedDays: Set<Int> = emptySet(),
    /** Outfits available to assign. Archived ones are already excluded by the DAO. */
    val combinations: List<CombinationWithGarments> = emptyList(),
    /** Groups available to attach. Archived ones are already excluded by the DAO. */
    val groups: List<PeopleGroup> = emptyList(),
    val today: Int = 0,
    val loaded: Boolean = false,
) {
    val lock: PlanLock get() = planLock(selectedDay, today)
    val canPlan: Boolean get() = lock.isOpen
    val isEmptyDay: Boolean get() = loaded && entries.isEmpty()

    /** True when the user has nothing to assign *with* — a different problem from an empty day. */
    val missingPrerequisites: Boolean
        get() = loaded && (combinations.isEmpty() || groups.isEmpty())
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(
    private val planDao: PlanDao,
    combinationDao: CombinationDao,
    groupDao: GroupDao,
) : ViewModel() {

    /**
     * Today is read once, when the ViewModel is built, and then held.
     *
     * Deliberate: re-reading the clock on every recomposition would make the lock flicker across
     * midnight while a screen is open, and a plan that silently becomes uneditable mid-edit is
     * worse than one that is a few hours stale. Phase 6's Home screen has the opposite
     * requirement — it must roll over — and will handle that itself.
     */
    private val today = todayEpochDay()

    private val selectedDay = MutableStateFlow(today)

    val uiState: StateFlow<PlanUiState> =
        combine(
            selectedDay,
            selectedDay.flatMapLatest { planDao.observeDay(it) },
            planDao.observePlannedDays(),
            combinationDao.observeAll(),
            groupDao.observeAll(),
        ) { day, entries, plannedDays, combinations, groups ->
            PlanUiState(
                selectedDay = day,
                entries = entries,
                plannedDays = plannedDays.toSet(),
                combinations = combinations,
                groups = groups,
                today = today,
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PlanUiState(selectedDay = today, today = today),
        )

    fun selectDay(day: Int) {
        selectedDay.value = day
    }

    /**
     * Assigns an outfit to a day, or rewrites an existing assignment.
     *
     * Three rules are enforced here rather than trusted to the screen, because each has a silent
     * failure mode: **at least one group** (the requirement is explicit, and an entry with none
     * would be invisible in history), **the day must still be open** (a past entry is a wear
     * record, D7), and **no duplicate outfit on a day** (the unique index would otherwise turn a
     * double-tap into a crash).
     */
    fun assign(entryId: Long?, day: Int, combinationId: Long, groupIds: List<Long>) {
        val ids = groupIds.distinct()
        if (ids.isEmpty()) return
        if (!isPlannable(day, today)) return

        viewModelScope.launch {
            val clash = planDao.combinationIdsOn(day, entryId ?: -1L).contains(combinationId)
            if (clash) return@launch

            if (entryId == null) {
                planDao.assign(PlanEntry(day = day, combinationId = combinationId), ids)
            } else {
                planDao.reassign(entryId, combinationId, ids)
            }
        }
    }

    /** Removes an assignment. Past days are refused for the same reason they cannot be edited. */
    fun remove(entry: PlanEntry) {
        if (!isPlannable(entry.day, today)) return
        viewModelScope.launch { planDao.deleteEntry(entry) }
    }

    /**
     * The repeat warning for a prospective assignment (D13).
     *
     * A suspend call made when the choice changes rather than a Flow: it asks a question about a
     * combination the user is *considering*, which no observable state knows about yet.
     */
    suspend fun warningsFor(
        combinationId: Long,
        groupIds: Set<Long>,
        targetDay: Int,
        excludingEntryId: Long?,
    ): List<RepeatWarning> {
        if (groupIds.isEmpty()) return emptyList()
        val sightings = planDao.sightingsOf(combinationId, excludingEntryId ?: -1L)
        return repeatWarnings(sightings, groupIds, targetDay, today)
    }

    /** Outfits already on [day], so the picker can grey out the ones that would clash. */
    suspend fun alreadyOn(day: Int, excludingEntryId: Long?): Set<Long> =
        planDao.combinationIdsOn(day, excludingEntryId ?: -1L).toSet()

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val database = DripDatabase.getInstance(application)
                PlanViewModel(
                    planDao = database.planDao(),
                    combinationDao = database.combinationDao(),
                    groupDao = database.groupDao(),
                )
            }
        }
    }
}
