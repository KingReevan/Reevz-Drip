package com.reevan.reevzdrip.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    /**
     * Everything assigned to one day, with outfits and groups.
     *
     * `@Transaction` is required rather than decorative: Room runs the entry query and the
     * relation queries separately, and a save landing between them would produce an entry holding
     * groups it no longer has.
     */
    @Transaction
    @Query("SELECT * FROM plan_entries WHERE day = :day ORDER BY id ASC")
    fun observeDay(day: Int): Flow<List<PlanEntryDetails>>

    /**
     * Every day that has anything planned — the dots on the calendar.
     *
     * Returns bare days rather than entries: the picker only needs to know *whether* a day is
     * spoken for, and pulling every outfit for every visible month to decide that would be work
     * thrown away.
     */
    @Query("SELECT DISTINCT day FROM plan_entries")
    fun observePlannedDays(): Flow<List<Int>>

    /** How many plan entries use each combination. Drives archive-or-delete (D9). */
    @Query("SELECT combinationId AS id, COUNT(*) AS count FROM plan_entries GROUP BY combinationId")
    fun observeCombinationUsage(): Flow<List<IdCount>>

    /** How many plan entries each group is attached to. Drives archive-or-delete (D9). */
    @Query(
        "SELECT groupId AS id, COUNT(*) AS count FROM plan_entry_groups GROUP BY groupId",
    )
    fun observeGroupUsage(): Flow<List<IdCount>>

    /**
     * Every occasion any group has seen a given outfit — the raw material for the repeat warning.
     *
     * [excludingEntryId] drops the entry being edited, so changing the groups on an existing
     * assignment does not report that assignment as a clash with itself.
     */
    @Query(
        """
        SELECT peg.groupId AS groupId, g.name AS groupName, pe.day AS day
        FROM plan_entries pe
        JOIN plan_entry_groups peg ON peg.planEntryId = pe.id
        JOIN groups g ON g.id = peg.groupId
        WHERE pe.combinationId = :combinationId AND pe.id != :excludingEntryId
        """,
    )
    suspend fun sightingsOf(combinationId: Long, excludingEntryId: Long = -1L): List<Sighting>

    /** The entries already on a day, to stop the same outfit being assigned to it twice. */
    @Query("SELECT combinationId FROM plan_entries WHERE day = :day AND id != :excludingEntryId")
    suspend fun combinationIdsOn(day: Int, excludingEntryId: Long = -1L): List<Long>

    /**
     * Every **past** day this outfit was worn, newest first, with who saw it.
     *
     * `day < :today` is what makes this history rather than a plan: a day still ahead is an
     * intention, and listing it here would claim people have seen something they have not. [today]
     * is a parameter rather than SQLite's own clock so the rule is testable and the app owns its
     * definition of "now" — see `util/todayFlow`, which keeps it current while the screen is open.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM plan_entries
        WHERE combinationId = :combinationId AND day < :today
        ORDER BY day DESC, id DESC
        """,
    )
    fun observeWearHistory(combinationId: Long, today: Int): Flow<List<WearOccasion>>

    /**
     * Every **past** outfit this group saw, newest first.
     *
     * The mirror of [observeWearHistory], and the other half of requirement 10 — opening a group
     * to check what they have already seen you in.
     */
    @Transaction
    @Query(
        """
        SELECT pe.* FROM plan_entries pe
        JOIN plan_entry_groups peg ON peg.planEntryId = pe.id
        WHERE peg.groupId = :groupId AND pe.day < :today
        ORDER BY pe.day DESC, pe.id DESC
        """,
    )
    fun observeGroupHistory(groupId: Long, today: Int): Flow<List<SeenOutfit>>

    @Insert
    suspend fun insertEntry(entry: PlanEntry): Long

    @Insert
    suspend fun insertEntryGroups(rows: List<PlanEntryGroup>)

    @Query("DELETE FROM plan_entry_groups WHERE planEntryId = :entryId")
    suspend fun clearEntryGroups(entryId: Long)

    @Query("UPDATE plan_entries SET combinationId = :combinationId WHERE id = :entryId")
    suspend fun setCombination(entryId: Long, combinationId: Long)

    /** Deleting an entry cascades to its group rows. */
    @Delete
    suspend fun deleteEntry(entry: PlanEntry)

    /**
     * Assigns an outfit to a day, with the groups who will see it, as one unit.
     *
     * Transactional because an entry with no groups is a state the requirements forbid and no
     * screen can repair — a half-applied save would leave an outfit on a day with nobody
     * attached, and no way to tell it apart from one the user meant to leave that way.
     */
    @Transaction
    suspend fun assign(entry: PlanEntry, groupIds: List<Long>): Long {
        val id = insertEntry(entry)
        insertEntryGroups(groupIds.map { PlanEntryGroup(planEntryId = id, groupId = it) })
        return id
    }

    /** Rewrites an existing assignment: which outfit, and who is seeing it. */
    @Transaction
    suspend fun reassign(entryId: Long, combinationId: Long, groupIds: List<Long>) {
        setCombination(entryId, combinationId)
        clearEntryGroups(entryId)
        insertEntryGroups(groupIds.map { PlanEntryGroup(planEntryId = entryId, groupId = it) })
    }
}
