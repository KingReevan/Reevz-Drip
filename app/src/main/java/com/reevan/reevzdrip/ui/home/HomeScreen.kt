package com.reevan.reevzdrip.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzdrip.data.PlanEntryDetails
import com.reevan.reevzdrip.ui.common.CombinationCollage
import com.reevan.reevzdrip.ui.common.EmptyState

/**
 * Today's outfits, and the people who will see each. Read-only.
 *
 * The requirement is unusually specific about what is *not* here: *"The Home section is minimal and
 * will only show the outfits to wear for today and in front of which group. Nothing else."* So
 * there is no date header (the top bar already says Home), no edit affordance, no link through to
 * Plan, no counts. Resist adding any — the value of this screen is that opening the app in the
 * morning answers the question with no taps and nothing to read past.
 *
 * The collage is large because at 7am, recognising the outfit *is* the whole interaction.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            // One frame at most. An empty state here would flash "nothing planned" at someone
            // who does have something planned, which is the one lie this screen must not tell.
            !state.loaded -> Unit

            state.isEmpty -> EmptyState(
                headline = "Nothing planned for today",
                hint = "Plan a day in Plan, and what you're wearing shows up here when it arrives.",
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.entries, key = { it.entry.id }) { details ->
                    TodayOutfitCard(details)
                }
            }
        }
    }
}

/** One of today's outfits: the collage, what it is, and who is seeing it. */
@Composable
private fun TodayOutfitCard(details: PlanEntryDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        CombinationCollage(
            garments = details.combination.ordered,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            tileIcon = 26.dp,
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = details.combination.combination.name
                    ?: "${details.combination.garments.size} pieces",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = details.groupNames.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
