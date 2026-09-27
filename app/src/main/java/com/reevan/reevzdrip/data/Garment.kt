package com.reevan.reevzdrip.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reevan.reevzdrip.util.capitalizeWords

/**
 * The four kinds of clothing the app knows about.
 *
 * [ACCESSORY] is the deliberate catch-all — watch, chain, hat, belt, anything that is not one of
 * the other three. The user asked for exactly this split, so resist growing it: a longer list
 * means a longer type picker on the one form that has to stay quick.
 */
enum class GarmentType(val label: String) {
    TOP("Top"),
    PANT("Pant"),
    SHOES("Shoes"),
    ACCESSORY("Accessory"),
}

/**
 * A single item of clothing — the atom the whole app is built from. Combinations group these;
 * plans assign those combinations to days.
 *
 * [photoName] is a **filename**, not a path, and the directory is resolved at read time. An
 * absolute path would break the moment Android relocates the app's data directory, which it does
 * on restore-to-a-new-device. The image bytes themselves never go in the database — see
 * `docs/DATA_MODEL.md`.
 *
 * Null [photoName] is a perfectly ordinary state, not a missing value to be backfilled: the user
 * asked for photos to be optional, so every surface that shows a garment has to render without
 * one.
 */
@Entity(tableName = "garments")
data class Garment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: GarmentType,
    /** Filename inside `filesDir/garments/`, or null when no photo was provided. */
    val photoName: String? = null,
    /** Epoch day the garment was added. Used only for a stable "newest first" ordering. */
    val addedOn: Int,
    /**
     * Retired from the wardrobe, but still part of the outfits it already belongs to.
     *
     * This is what "delete" means for a garment that is used in a combination. Hard-deleting it
     * would silently rewrite an outfit the user may already have worn in front of people, and the
     * wear history is the thing this whole app exists to keep — so a referenced garment is
     * archived instead, and only an unreferenced one is really deleted. See D9 in
     * `docs/DECISIONS.md`.
     *
     * Archived garments are excluded from the Wardrobe grid and from the combination builder, and
     * included everywhere a combination is rendered. Their photo file is **kept** — the outfit
     * still has to draw them.
     *
     * `defaultValue` is what lets Room generate the v1 → v2 `@AutoMigration` for a NOT NULL
     * column; without it the migration cannot be derived and has to be hand-written.
     */
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
)

/**
 * Builds a [Garment] from raw form input, applying the rules every entry point must obey.
 *
 * There is one entry point today — the Wardrobe editor — and there will be more once the
 * combination builder can create a garment inline. Keeping the rules here rather than in the
 * screen means they cannot drift apart:
 *
 * - **The name is trimmed and capitalised on the way in.** The keyboard's `Words` capitalisation
 *   is only a hint to the IME; pasted text, swipe input and `adb shell input text` all slip past
 *   it, so this pass is what actually guarantees the stored value.
 * - **A blank photo name is stored as null**, so "no photo" has exactly one representation rather
 *   than two that every `if` has to remember to check.
 */
fun garmentOf(
    id: Long = 0L,
    name: String,
    type: GarmentType,
    photoName: String? = null,
    addedOn: Int,
    archived: Boolean = false,
): Garment = Garment(
    id = id,
    name = capitalizeWords(name.trim()),
    type = type,
    photoName = photoName?.trim()?.ifBlank { null },
    addedOn = addedOn,
    archived = archived,
)
