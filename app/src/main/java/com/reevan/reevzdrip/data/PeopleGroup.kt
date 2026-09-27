package com.reevan.reevzdrip.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.reevan.reevzdrip.util.capitalizeWords

/**
 * A named set of people — "Colleagues", "College Friends".
 *
 * Called `PeopleGroup` rather than `Group` because `Group` collides with names in Compose and the
 * Android framework often enough that every file using it would need an import alias.
 *
 * A group has no members and no structure; it is a label for "the people who will see this". The
 * requirements never ask who is in a group, only what a group has seen, so modelling individual
 * people would be inventing work the app never uses.
 */
@Entity(
    tableName = "groups",
    indices = [Index(value = ["name"], unique = true)],
)
data class PeopleGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** Epoch day the group was created. Orders the list, newest first. */
    val createdOn: Int,
    /**
     * Retired, but still attached to the days it has already seen.
     *
     * Nothing can set this yet: a group can only have "seen" something once plan entries exist in
     * Phase 5. The column is here from the start anyway, because D9 has already decided the rule
     * and adding it now costs nothing — it rides the migration that creates the table, where
     * adding it later would need a schema bump of its own for a single boolean. The same
     * reasoning put `photoName` on `garments` one phase before anything used it.
     *
     * **Until Phase 5, groups are hard-deleted**, which is correct precisely because nothing can
     * reference one. See the Phase 5 checklist.
     */
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
)

/**
 * Builds a [PeopleGroup] from raw form input, applying the storage rules in one place — same job
 * as [garmentOf] and [combinationOf].
 *
 * The name is trimmed and capitalised on the way in, so "colleagues" and "Colleagues" cannot end
 * up as two rows that look identical in the list.
 */
fun peopleGroupOf(
    id: Long = 0L,
    name: String,
    createdOn: Int,
    archived: Boolean = false,
): PeopleGroup = PeopleGroup(
    id = id,
    name = capitalizeWords(name.trim()),
    createdOn = createdOn,
    archived = archived,
)

/**
 * Whether [name] would duplicate a group that already exists.
 *
 * Compared **case-insensitively**, which the database index is not: SQLite's unique index is a
 * byte comparison, so it would happily store "Colleagues" alongside "COLLEAGUES". The index is the
 * backstop that turns a logic bug into a loud failure; this is what actually stops the user
 * creating a near-duplicate they then have to tell apart by eye.
 *
 * [excludingId] is the group being edited, so renaming a group to the case it already has is not
 * reported as a clash with itself.
 */
fun isDuplicateGroupName(
    name: String,
    existing: List<PeopleGroup>,
    excludingId: Long? = null,
): Boolean {
    val candidate = name.trim()
    if (candidate.isEmpty()) return false
    return existing.any { it.id != excludingId && it.name.equals(candidate, ignoreCase = true) }
}
