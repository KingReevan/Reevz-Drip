package com.reevan.reevzdrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzdrip.ui.ReevzDripApp
import com.reevan.reevzdrip.ui.settings.PreferencesViewModel
import com.reevan.reevzdrip.ui.theme.ReevzDripTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The same instance Settings uses — `viewModel()` resolves to the activity's store —
            // so changing the theme repaints straight away instead of on next launch.
            val preferences: PreferencesViewModel =
                viewModel(factory = PreferencesViewModel.Factory)
            val settings by preferences.settings.collectAsStateWithLifecycle()

            ReevzDripTheme(themeMode = settings.themeMode) {
                ReevzDripApp()
            }
        }
    }
}
