package com.reevan.reevzdrip.ui

import androidx.annotation.DrawableRes
import com.reevan.reevzdrip.R

/**
 * The app's top-level sections.
 *
 * There are six, not the five the requirements list — that list omits **Plan**, which the same
 * requirements ask for twice elsewhere. Read as an oversight, not as an instruction to fold Plan
 * into another section. (`docs/DECISIONS.md`, Q1.)
 *
 * Six does not fit a bottom bar, so [inBottomBar] splits them: the five that are part of the daily
 * loop stay in the bar — five being Material 3's documented maximum — and **Settings**, the one
 * section you touch about twice a year, moves to an icon in the top bar. Sibling app Reevz Mealz
 * hit the same wall and solved it the same way, with a pause menu instead of a single icon,
 * because it had three occasional sections rather than one.
 *
 * [tabLabel] is deliberately shorter than [title]; the full name shows in the top bar.
 */
enum class AppSection(
    val title: String,
    val tabLabel: String,
    @param:DrawableRes val icon: Int,
    val inBottomBar: Boolean = true,
) {
    /** Today's plan, read-only. The screen the app exists for. */
    HOME("Home", "Home", R.drawable.ic_home),

    PLAN("Plan", "Plan", R.drawable.ic_plan),

    /** Saved outfits — groupings of wardrobe garments. */
    COMBINATIONS("Combinations", "Outfits", R.drawable.ic_combinations),

    WARDROBE("Wardrobe", "Wardrobe", R.drawable.ic_wardrobe),

    /** Named sets of people, and what each has already seen. */
    GROUPS("Groups", "Groups", R.drawable.ic_groups),

    SETTINGS("Settings", "Settings", R.drawable.ic_settings, inBottomBar = false);

    companion object {
        /** The bottom bar's tabs, in order. */
        val tabs: List<AppSection> = entries.filter { it.inBottomBar }
    }
}
