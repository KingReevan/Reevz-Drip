package com.reevan.reevzdrip.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzdrip.data.DripDatabase
import com.reevan.reevzdrip.data.PlanDao
import com.reevan.reevzdrip.data.PlanEntryDetails
import com.reevan.reevzdrip.util.todayFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val today: Int = 0,
    val entries: List<PlanEntryDetails> = emptyList(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && entries.isEmpty()
}

/**
 * Home: what you are wearing today, and who will see it. Read-only, and nothing else.
 *
 * Today rolls over while the screen is open — see [todayFlow], which is deliberately the opposite
 * of `PlanViewModel`'s read-once approach (D24 vs D25).
 *
 * **This is the screen the app exists for.** Everything else — the wardrobe, the outfits, the
 * groups, the planning — is machinery for making this one correct, so it stays deliberately
 * featureless: no editing, no navigation, no prompts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(planDao: PlanDao) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        todayFlow()
            .flatMapLatest { day ->
                planDao.observeDay(day).map { entries ->
                    HomeUiState(today = day, entries = entries, loaded = true)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HomeUiState(),
            )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                HomeViewModel(DripDatabase.getInstance(application).planDao())
            }
        }
    }
}
