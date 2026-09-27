package com.reevan.reevzdrip.ui.groups

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzdrip.data.DripDatabase
import com.reevan.reevzdrip.data.GroupDao
import com.reevan.reevzdrip.data.PeopleGroup
import com.reevan.reevzdrip.data.PlanDao
import com.reevan.reevzdrip.data.SeenOutfit
import com.reevan.reevzdrip.util.todayFlow
import com.reevan.reevzdrip.data.peopleGroupOf
import com.reevan.reevzdrip.util.todayEpochDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groups: List<PeopleGroup> = emptyList(),
    /** How many planned outfits each group is attached to — drives archive-or-delete (D9). */
    val planUsage: Map<Long, Int> = emptyMap(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && groups.isEmpty()
}

/** What one group has already seen, for the detail screen. */
data class SeenHistoryState(
    val outfits: List<SeenOutfit> = emptyList(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && outfits.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModel(
    private val dao: GroupDao,
    private val planDao: PlanDao,
) : ViewModel() {

    val uiState: StateFlow<GroupsUiState> =
        combine(dao.observeAll(), planDao.observeGroupUsage()) { groups, usage ->
            GroupsUiState(
                groups = groups,
                planUsage = usage.associate { it.id to it.count },
                loaded = true,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = GroupsUiState(),
            )

    /** The group whose detail screen is open, or null for the list. */
    private val detailId = MutableStateFlow<Long?>(null)

    /**
     * The open group's history — every past outfit they have seen, newest first.
     *
     * Recomputed when the date rolls over as well as when the group changes, so an outfit they are
     * seeing *today* joins the history at midnight without the screen needing to be reopened
     * (D25).
     */
    val history: StateFlow<SeenHistoryState> =
        combine(detailId, todayFlow()) { id, today -> id to today }
            .flatMapLatest { (id, today) ->
                if (id == null) {
                    flowOf(SeenHistoryState(loaded = false))
                } else {
                    planDao.observeGroupHistory(id, today).map {
                        SeenHistoryState(outfits = it, loaded = true)
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SeenHistoryState(),
            )

    /** Tells the ViewModel which group's detail is open, so its history can be loaded. */
    fun openDetail(id: Long?) {
        detailId.value = id
    }

    /**
     * Creates the group when [id] is null, otherwise renames it in place.
     *
     * `createdOn` is carried through an edit unchanged — renaming a group should not move it to
     * the top of the list.
     *
     * Duplicate names are rejected by the editor before they reach here, case-insensitively; the
     * unique index on `groups.name` is the backstop behind that, not the first line of defence.
     */
    fun save(id: Long?, name: String, existing: PeopleGroup? = null) {
        if (name.isBlank()) return

        val group = peopleGroupOf(
            id = id ?: 0L,
            name = name,
            createdOn = existing?.createdOn ?: todayEpochDay(),
            archived = existing?.archived ?: false,
        )
        viewModelScope.launch {
            if (id == null) dao.insert(group) else dao.update(group)
        }
    }

    /**
     * Removes a group — archiving it if it is attached to any planned outfit, deleting it
     * otherwise.
     *
     * D9, arriving for groups now that a plan entry can make one part of history. A group that has
     * seen something is *why* the history reads the way it does; destroying it would leave past
     * days unable to say who was there. A group that has seen nothing goes for real.
     */
    fun delete(group: PeopleGroup, seenCount: Int) {
        viewModelScope.launch {
            if (seenCount > 0) {
                dao.archive(group.id)
            } else {
                dao.delete(group)
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
                GroupsViewModel(
                    dao = database.groupDao(),
                    planDao = database.planDao(),
                )
            }
        }
    }
}
