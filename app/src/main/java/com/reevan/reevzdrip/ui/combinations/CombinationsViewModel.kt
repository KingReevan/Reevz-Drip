package com.reevan.reevzdrip.ui.combinations

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzdrip.data.Combination
import com.reevan.reevzdrip.data.CombinationDao
import com.reevan.reevzdrip.data.CombinationWithGarments
import com.reevan.reevzdrip.data.DripDatabase
import com.reevan.reevzdrip.data.Garment
import com.reevan.reevzdrip.data.GarmentDao
import com.reevan.reevzdrip.data.PlanDao
import com.reevan.reevzdrip.data.WearOccasion
import com.reevan.reevzdrip.util.todayFlow
import com.reevan.reevzdrip.data.combinationOf
import com.reevan.reevzdrip.util.todayEpochDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CombinationsUiState(
    val combinations: List<CombinationWithGarments> = emptyList(),
    /**
     * The garments the builder can pick from — the live wardrobe, archived ones excluded.
     *
     * Read here rather than in a builder-specific ViewModel so that adding a garment in the
     * Wardrobe tab and then opening the builder shows it immediately, with no refresh step.
     */
    val wardrobe: List<Garment> = emptyList(),
    /** How many planned days each outfit appears on — drives archive-or-delete (D9). */
    val planUsage: Map<Long, Int> = emptyMap(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && combinations.isEmpty()
    val wardrobeIsEmpty: Boolean get() = loaded && wardrobe.isEmpty()
}

/** One outfit's wear history, for the detail screen. */
data class WearHistoryState(
    val occasions: List<WearOccasion> = emptyList(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && occasions.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class CombinationsViewModel(
    private val dao: CombinationDao,
    garmentDao: GarmentDao,
    private val planDao: PlanDao,
) : ViewModel() {

    val uiState: StateFlow<CombinationsUiState> =
        combine(
            dao.observeAll(),
            garmentDao.observeAll().map { rows -> rows.map { it.garment } },
            planDao.observeCombinationUsage(),
        ) { combinations, wardrobe, usage ->
            CombinationsUiState(
                combinations = combinations,
                wardrobe = wardrobe,
                planUsage = usage.associate { it.id to it.count },
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CombinationsUiState(),
        )

    /**
     * Creates the outfit when [id] is null, otherwise rewrites it in place.
     *
     * [garmentIds] is deduplicated before it reaches the database. The picker cannot select the
     * same garment twice, but the composite primary key on `combination_items` would make a
     * duplicate a crash rather than a no-op, and that is not a failure mode worth leaving to the
     * UI to prevent.
     */
    /**
     * The outfit whose detail screen is open, or null for the list.
     *
     * Held here rather than passed into a query from the screen so the history flow can switch
     * with it, and so it survives the screen being recomposed.
     */
    private val detailId = MutableStateFlow<Long?>(null)

    /**
     * The open outfit's wear history — every past day it was worn and who saw it.
     *
     * Recomputed when the date rolls over as well as when the outfit changes: an outfit planned
     * for *today* becomes history at midnight, and a detail screen left open overnight should say
     * so rather than keep insisting it has never been worn (D25).
     */
    val history: StateFlow<WearHistoryState> =
        combine(detailId, todayFlow()) { id, today -> id to today }
            .flatMapLatest { (id, today) ->
                if (id == null) {
                    flowOf(WearHistoryState(loaded = false))
                } else {
                    planDao.observeWearHistory(id, today).map {
                        WearHistoryState(occasions = it, loaded = true)
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = WearHistoryState(),
            )

    /** Tells the ViewModel which outfit's detail is open, so its history can be loaded. */
    fun openDetail(id: Long?) {
        detailId.value = id
    }

    fun save(id: Long?, name: String?, garmentIds: List<Long>, createdOn: Int? = null) {
        val ids = garmentIds.distinct()
        if (ids.isEmpty()) return // An outfit with nothing in it is not a thing to save.

        viewModelScope.launch {
            val combination = combinationOf(
                id = id ?: 0L,
                name = name,
                createdOn = createdOn ?: todayEpochDay(),
            )
            if (id == null) {
                dao.create(combination, ids)
            } else {
                dao.replace(combination, ids)
            }
        }
    }

    /**
     * Removes an outfit — archiving it if it has been planned for any day, deleting it otherwise.
     *
     * This is D9 arriving for combinations, and the reasoning is the one that has applied to
     * garments since Phase 3: a plan entry in the past *is* the record of what you wore (D4), so
     * destroying an outfit that appears in one would rewrite history that the app's whole purpose
     * depends on being true. An outfit never planned has no history to protect and goes for real.
     *
     * Either way the garments in it are untouched — they belong to the wardrobe, not to this
     * outfit. [plannedCount] comes from the loaded state, so no extra query is needed here.
     */
    fun delete(combination: Combination, plannedCount: Int) {
        viewModelScope.launch {
            if (plannedCount > 0) {
                dao.archive(combination.id)
            } else {
                dao.deleteCombination(combination)
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val database = DripDatabase.getInstance(application)
                CombinationsViewModel(
                    dao = database.combinationDao(),
                    garmentDao = database.garmentDao(),
                    planDao = database.planDao(),
                )
            }
        }
    }
}
