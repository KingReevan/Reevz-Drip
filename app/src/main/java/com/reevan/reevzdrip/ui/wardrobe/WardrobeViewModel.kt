package com.reevan.reevzdrip.ui.wardrobe

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzdrip.data.DripDatabase
import com.reevan.reevzdrip.data.Garment
import com.reevan.reevzdrip.data.GarmentDao
import com.reevan.reevzdrip.data.GarmentType
import com.reevan.reevzdrip.data.GarmentWithUsage
import com.reevan.reevzdrip.data.PhotoStore
import com.reevan.reevzdrip.data.garmentOf
import com.reevan.reevzdrip.util.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WardrobeUiState(
    /** Everything in the wardrobe, newest first — the unfiltered truth. */
    val all: List<GarmentWithUsage> = emptyList(),
    /** What the grid shows: [all] narrowed by [filter]. */
    val visible: List<GarmentWithUsage> = emptyList(),
    val filter: GarmentType? = null,
    /**
     * False until the database has answered once.
     *
     * Without this the empty state flashes on every launch before the first emission arrives,
     * which reads as "your wardrobe is gone" rather than "still loading".
     */
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && all.isEmpty()
    val filteredToNothing: Boolean get() = loaded && all.isNotEmpty() && visible.isEmpty()
}

/**
 * What the editor wants done with the garment's photo.
 *
 * Three states rather than a nullable Uri, because "leave it alone" and "remove it" are different
 * instructions that a null cannot tell apart — and getting them confused would silently delete the
 * photo of every garment whose name was edited.
 */
sealed interface PhotoEdit {
    /** Keep whatever photo the garment already has. */
    data object Unchanged : PhotoEdit

    /** Drop the photo; its file is deleted. */
    data object Removed : PhotoEdit

    /** Import [source] and use the result, deleting whatever was there before. */
    data class Replaced(val source: Uri) : PhotoEdit
}

class WardrobeViewModel(
    private val dao: GarmentDao,
    private val photos: PhotoStore,
) : ViewModel() {

    private val filter = MutableStateFlow<GarmentType?>(null)

    val uiState: StateFlow<WardrobeUiState> =
        combine(dao.observeAll(), filter) { garments, selected ->
            WardrobeUiState(
                all = garments,
                visible = if (selected == null) {
                    garments
                } else {
                    garments.filter { it.garment.type == selected }
                },
                filter = selected,
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = WardrobeUiState(),
        )

    /**
     * Narrows the grid to one garment type, or to everything when [type] is null.
     *
     * Re-selecting the active type also clears it, which is what a chip that looks like a toggle
     * should do. That is no longer the only way back to the whole wardrobe, though — the row has
     * an explicit "All", because a gesture nobody can see is not an affordance.
     */
    fun setFilter(type: GarmentType?) {
        filter.value = if (filter.value == type) null else type
    }

    /**
     * Creates the garment when [id] is null, otherwise updates it in place.
     *
     * The photo is imported here rather than when it was picked, so abandoning the editor cannot
     * leave an orphaned file behind — nothing is written until the user actually saves.
     *
     * The **old file is deleted only after the row has been written**, and only once the row no
     * longer refers to it. A file with no row is a harmless orphan; a row pointing at a file that
     * is gone is a visible hole in the user's wardrobe, so the ordering is deliberate.
     *
     * If an import fails the garment still saves, keeping whatever photo it had. Losing the new
     * photo is a small annoyance; losing the garment the user was trying to save is not.
     */
    fun save(
        id: Long?,
        name: String,
        type: GarmentType,
        existing: Garment?,
        photoEdit: PhotoEdit = PhotoEdit.Unchanged,
    ) {
        viewModelScope.launch {
            val previousPhoto = existing?.photoName

            val photoName = when (photoEdit) {
                PhotoEdit.Unchanged -> previousPhoto
                PhotoEdit.Removed -> null
                is PhotoEdit.Replaced -> photos.import(photoEdit.source) ?: previousPhoto
            }

            val garment = garmentOf(
                id = id ?: 0L,
                name = name,
                type = type,
                photoName = photoName,
                addedOn = existing?.addedOn ?: todayEpochDay(),
            )

            if (id == null) dao.insert(garment) else dao.update(garment)

            // Only now that no row refers to it.
            if (previousPhoto != null && previousPhoto != photoName) {
                photos.delete(previousPhoto)
            }
            photos.clearCameraCache()
        }
    }

    /**
     * Removes a garment from the wardrobe — by archiving it if any outfit uses it, and really
     * deleting it otherwise.
     *
     * This is D9 in `docs/DECISIONS.md`, and the asymmetry is the whole point. Hard-deleting a
     * garment that belongs to an outfit would silently rewrite something the user may already have
     * worn in front of people, and that wear history is what the app exists to keep. A garment
     * nothing references has no history to protect, so a mistyped entry added a minute ago still
     * disappears cleanly, photo and all.
     *
     * An archived garment **keeps its photo file**: the outfits it belongs to still have to draw
     * it. [usedInCombinations] comes from the row itself, so no extra query is needed here.
     */
    fun delete(garment: Garment, usedInCombinations: Int) {
        viewModelScope.launch {
            if (usedInCombinations > 0) {
                dao.archive(garment.id)
            } else {
                dao.delete(garment)
                photos.delete(garment.photoName)
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                WardrobeViewModel(
                    dao = DripDatabase.getInstance(application).garmentDao(),
                    photos = PhotoStore(application),
                )
            }
        }
    }
}
