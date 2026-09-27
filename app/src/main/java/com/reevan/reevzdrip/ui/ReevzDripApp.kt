package com.reevan.reevzdrip.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.reevan.reevzdrip.R
import com.reevan.reevzdrip.ui.combinations.CombinationsScreen
import com.reevan.reevzdrip.ui.groups.GroupsScreen
import com.reevan.reevzdrip.ui.home.HomeScreen
import com.reevan.reevzdrip.ui.settings.SettingsScreen
import com.reevan.reevzdrip.ui.plan.PlanScreen
import com.reevan.reevzdrip.ui.wardrobe.WardrobeScreen

/**
 * The app shell: top bar, bottom navigation, and section dispatch.
 *
 * Navigation is an [AppSection] in state and a `when` — no NavHost, no routes, no back stack
 * library. Sibling app Reevz Mealz works the same way, and with six flat sections and no deep
 * links there is nothing a navigation library would do here except add indirection.
 *
 * Back is handled explicitly: from Settings it returns to whichever tab you came from, and from
 * any tab other than Home it returns to Home. Only Home lets the system handle back and leave the
 * app, which matches where you expect "back" to bottom out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReevzDripApp() {
    // rememberSaveable, not remember: a configuration change must not bounce you back to Home.
    // The phone flips light/dark on its own at sunset, and that recreates the activity — with a
    // plain remember, whatever section you were on silently becomes Home mid-use.
    var section by rememberSaveable { mutableStateOf(AppSection.HOME) }
    // Kept separately so opening and closing Settings does not lose the tab underneath it.
    var tabBeneathSettings by rememberSaveable { mutableStateOf(AppSection.HOME) }

    val inSettings = section == AppSection.SETTINGS

    BackHandler(enabled = section != AppSection.HOME) {
        section = if (inSettings) tabBeneathSettings else AppSection.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(section.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (inSettings) {
                                section = tabBeneathSettings
                            } else {
                                tabBeneathSettings = section
                                section = AppSection.SETTINGS
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (inSettings) R.drawable.ic_close else R.drawable.ic_settings,
                            ),
                            contentDescription = if (inSettings) "Close settings" else "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppSection.tabs.forEach { tab ->
                    NavigationBarItem(
                        // While Settings is open no tab reads as selected — the header says
                        // Settings, and a highlighted tab would be naming a screen not on show.
                        selected = section == tab,
                        onClick = { section = tab },
                        icon = {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = null,
                            )
                        },
                        label = { Text(tab.tabLabel) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (section) {
            AppSection.HOME -> HomeScreen(modifier = contentModifier)

            AppSection.PLAN -> PlanScreen(modifier = contentModifier)

            AppSection.COMBINATIONS -> CombinationsScreen(modifier = contentModifier)

            AppSection.WARDROBE -> WardrobeScreen(modifier = contentModifier)

            AppSection.GROUPS -> GroupsScreen(modifier = contentModifier)

            AppSection.SETTINGS -> SettingsScreen(modifier = contentModifier)
        }
    }
}
