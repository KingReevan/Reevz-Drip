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
import com.reevan.reevzdrip.data.combinationOf
import com.reevan.reevzdrip.util.todayEpochDay
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
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && combinations.isEmpty()
    val wardrobeIsEmpty: Boolean get() = loaded && wardrobe.isEmpty()
}

class CombinationsViewModel(
    private val dao: CombinationDao,
    garmentDao: GarmentDao,
) : ViewModel() {

    val uiState: StateFlow<CombinationsUiState> =
        combine(
            dao.observeAll(),
            garmentDao.observeAll().map { rows -> rows.map { it.garment } },
        ) { combinations, wardrobe ->
            CombinationsUiState(
                combinations = combinations,
                wardrobe = wardrobe,
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
     * Deletes an outfit. Its membership rows go with it through `ON DELETE CASCADE`; the garments
     * themselves are untouched, because they belong to the wardrobe and not to this outfit.
     */
    fun delete(combination: Combination) {
        viewModelScope.launch { dao.deleteCombination(combination) }
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
                )
            }
        }
    }
}
