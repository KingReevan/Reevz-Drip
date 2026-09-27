package com.reevan.reevzdrip.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.reevan.reevzdrip.util.capitalizeWords

/**
 * A saved outfit: nothing more than a *set of garments*.
 *
 * The requirements are explicit that this is "essentially just grouping of the individual clothing
 * items", so there are no structural rules here — no "must contain exactly one top". Whatever set
 * the user thinks goes together is a valid combination.
 *
 * [name] is optional. The requirements never ask to name an outfit, and the collage is how you
 * actually recognise one; but a wear history that reads "worn 3 Oct, 17 Oct" scans better with a
 * label, so there is somewhere to put one. Null means unnamed, and unnamed is an ordinary state.
 */
@Entity(tableName = "combinations")
data class Combination(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String? = null,
    /** Epoch day the outfit was put together. Orders the list, newest first. */
    val createdOn: Int,
)

/**
 * One garment's membership of one combination.
 *
 * The composite primary key means a garment can appear in an outfit at most once, which is the
 * only rule worth enforcing here — you cannot wear the same shirt twice at the same time.
 *
 * **There is no `position` column, deliberately.** An earlier draft of the data model had one for
 * collage order. Nothing in the requirements asks to reorder an outfit, insertion order is not
 * meaningful to look at, and a position column has to be renumbered every time a garment is added
 * or removed. [orderedForCollage] derives a better order from the garment types instead, so every
 * outfit reads top-to-bottom the same way and there is no extra state to keep correct.
 *
 * The two foreign keys are deliberately asymmetric:
 * - Deleting a **combination** cascades. Its membership rows mean nothing without it.
 * - Deleting a **garment** is `RESTRICT`, so the database refuses to silently empty an outfit the
 *   user may already have worn. Nothing should ever hit that restriction, because the app archives
 *   a referenced garment rather than deleting it (see `GarmentDao.archive`) — it is the backstop
 *   that turns a logic bug into a loud failure instead of quiet data loss.
 */
@Entity(
    tableName = "combination_items",
    primaryKeys = ["combinationId", "garmentId"],
    foreignKeys = [
        ForeignKey(
            entity = Combination::class,
            parentColumns = ["id"],
            childColumns = ["combinationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Garment::class,
            parentColumns = ["id"],
            childColumns = ["garmentId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("garmentId")],
)
data class CombinationItem(
    val combinationId: Long,
    val garmentId: Long,
)

/** A combination with the garments in it, as the UI needs it. */
data class CombinationWithGarments(
    @Embedded val combination: Combination,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = androidx.room.Junction(
            value = CombinationItem::class,
            parentColumn = "combinationId",
            entityColumn = "garmentId",
        ),
    )
    val garments: List<Garment>,
) {
    /** The garments in the order they should be shown. See [orderedForCollage]. */
    val ordered: List<Garment> get() = orderedForCollage(garments)
}

/**
 * Puts a combination's garments into a stable, meaningful order: by type — top, pant, shoes, then
 * accessories — and alphabetically within a type.
 *
 * Two reasons this is derived rather than stored:
 *
 * - **It reads like an outfit.** A collage ordered top-to-bottom is recognisable at a glance in a
 *   way that insertion order never is; "whatever I happened to tap first" is not information.
 * - **It cannot drift.** A stored order has to be renumbered on every edit, and a gap or a
 *   duplicate in those numbers is a bug that only shows up as a subtly shuffled collage.
 *
 * Relies on [GarmentType] being declared in the order clothes are worn, which it is. Name is the
 * tiebreaker so the order is total — without it, two accessories could swap places between reads
 * and the collage would flicker.
 */
fun orderedForCollage(garments: List<Garment>): List<Garment> =
    garments.sortedWith(compareBy({ it.type.ordinal }, { it.name.lowercase() }, { it.id }))

/**
 * Builds a [Combination] from raw form input.
 *
 * Same job as [garmentOf]: one place that owns the storage rules, so every entry point obeys them.
 * A blank name is stored as null rather than an empty string, so "unnamed" has exactly one
 * representation.
 */
fun combinationOf(
    id: Long = 0L,
    name: String?,
    createdOn: Int,
): Combination = Combination(
    id = id,
    name = name?.trim()?.ifBlank { null }?.let(::capitalizeWords),
    createdOn = createdOn,
)
