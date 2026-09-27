package com.reevan.reevzdrip.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzdrip.data.AppSettings
import com.reevan.reevzdrip.data.AppSettingsDao
import com.reevan.reevzdrip.data.DripDatabase
import com.reevan.reevzdrip.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide preferences.
 *
 * Shared between `MainActivity`, which needs the theme before anything renders, and the Settings
 * screen that changes it — `viewModel()` resolves to the activity's store, so both get the same
 * instance and a theme change repaints immediately rather than on next launch.
 *
 * A missing row means "all defaults", so the very first run needs no seeding step.
 */
class PreferencesViewModel(private val dao: AppSettingsDao) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        dao.observe()
            .map { it ?: AppSettings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AppSettings(),
            )

    fun setThemeMode(mode: ThemeMode) {
        update { it.copy(themeMode = mode) }
    }

    /**
     * Read-modify-write against the stored row rather than against [settings].
     *
     * The StateFlow can be a frame behind, and it substitutes defaults for a row that does not
     * exist yet — writing that substitute back would overwrite any setting added later with its
     * default. Re-reading first keeps a setter honest about the columns it is not changing.
     */
    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            dao.upsert(transform(dao.get() ?: AppSettings()))
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                PreferencesViewModel(DripDatabase.getInstance(application).appSettingsDao())
            }
        }
    }
}
