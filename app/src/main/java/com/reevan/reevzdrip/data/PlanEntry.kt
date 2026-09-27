package com.reevan.reevzdrip.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * One outfit assigned to one day.
 *
 * **A plan entry whose day has passed *is* a wear record** — there is no separate history table
 * and no "did you actually wear this?" step. That is D4, the single most important modelling
 * decision in this app, and everything else here follows from it: past entries are immutable
 * (D7), and a combination or group that appears in one can no longer be destroyed (D9).
 *
 * Unique `(day, combinationId)`: assigning the same outfit twice to one day says nothing.
 * *Different* outfits on one day are expected — office then gym is the motivating case.
 *
 * `RESTRICT` on the combination for the same reason garments have it: an entry in the past is a
 * record of what you wore, and deleting the outfit would erase it. The app archives instead, so
 * nothing should ever hit this — it is the backstop that turns a logic bug into a loud failure.
 */
@Entity(
    tableName = "plan_entries",
    indices = [
        Index(value = ["day", "combinationId"], unique = true),
        Index("combinationId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Combination::class,
            parentColumns = ["id"],
            childColumns = ["combinationId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class PlanEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Epoch day. Comparisons against today are what make history derivable. */
    val day: Int,
    val combinationId: Long,
)

/**
 * Who will see an outfit on a day.
 *
 * At least one row per entry, which SQLite cannot express — "at least one child" is not a
 * constraint it has — so it is enforced where the entry is built instead. The requirement is
 * explicit: *"Every time an outfit is assigned, I have to also assign a 'Group'."*
 */
@Entity(
    tableName = "plan_entry_groups",
    primaryKeys = ["planEntryId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = PlanEntry::class,
            parentColumns = ["id"],
            childColumns = ["planEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PeopleGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("groupId")],
)
data class PlanEntryGroup(
    val planEntryId: Long,
    val groupId: Long,
)

/**
 * A plan entry with everything needed to show it: the outfit, its garments, and the groups.
 *
 * Nested relations — the combination arrives as [CombinationWithGarments], so a collage can be
 * drawn straight from it without a second round trip per entry.
 *
 * Neither the combination nor the groups are filtered by `archived`. That is the point of
 * archiving: a day that has already happened keeps showing exactly what it showed at the time,
 * whatever has since been retired from the wardrobe or the group list.
 */
data class PlanEntryDetails(
    @Embedded val entry: PlanEntry,
    @Relation(
        entity = Combination::class,
        parentColumn = "combinationId",
        entityColumn = "id",
    )
    val combination: CombinationWithGarments,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlanEntryGroup::class,
            parentColumn = "planEntryId",
            entityColumn = "groupId",
        ),
    )
    val groups: List<PeopleGroup>,
) {
    /** Group names in a stable order, for the one-line summary under an outfit. */
    val groupNames: List<String> get() = groups.map { it.name }.sortedBy { it.lowercase() }
}

/** An id with a count beside it — how many plan entries reference a combination, or a group. */
data class IdCount(val id: Long, val count: Int)

/**
 * One occasion a group has seen an outfit, used to build the repeat warning.
 *
 * Carries the group's name as well as its id so the warning can be phrased without a second
 * lookup — "Colleagues saw this on 3 Oct" rather than "group 2 saw this".
 */
data class Sighting(
    val groupId: Long,
    val groupName: String,
    val day: Int,
)

/**
 * One past day an outfit was worn, and who saw it — a row of the Combinations detail history.
 *
 * Carries no combination of its own: the screen already knows which outfit it is showing, and
 * fetching the garments again per row would be work thrown away.
 *
 * Groups are **not** filtered by `archived`. A group retired since that day still has to be named,
 * otherwise a past day would quietly lose the people it was about — which is the exact failure
 * archiving exists to prevent (D9).
 */
data class WearOccasion(
    @Embedded val entry: PlanEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlanEntryGroup::class,
            parentColumn = "planEntryId",
            entityColumn = "groupId",
        ),
    )
    val groups: List<PeopleGroup>,
) {
    /** Group names in a stable order, for the "who saw it" line. */
    val groupNames: List<String> get() = groups.map { it.name }.sortedBy { it.lowercase() }
}

/**
 * One past outfit a group saw, and when — a row of the Groups detail history.
 *
 * The mirror of [WearOccasion]: this one carries the outfit (with its garments, so the row can
 * show a collage) and not the groups, because the screen already knows which group it is showing.
 *
 * An archived outfit still appears, for the same reason an archived group does above.
 */
data class SeenOutfit(
    @Embedded val entry: PlanEntry,
    @Relation(
        entity = Combination::class,
        parentColumn = "combinationId",
        entityColumn = "id",
    )
    val combination: CombinationWithGarments,
)

/**
 * Names a group of people the way you would say it: "Colleagues", "Colleagues and Gym",
 * "Colleagues, Gym and Family".
 *
 * Pure, so the awkward cases — none, one, exactly two — are pinned by tests rather than noticed
 * in a screenshot. A bare `joinToString(", ")` reads like a database dump in a sentence that is
 * meant to be read as English.
 */
fun describeAudience(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names[0]
    2 -> "${names[0]} and ${names[1]}"
    else -> names.dropLast(1).joinToString(", ") + " and " + names.last()
}
