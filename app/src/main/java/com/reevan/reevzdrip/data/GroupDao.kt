package com.reevan.reevzdrip.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    /**
     * Every group still in use, newest first.
     *
     * Newest-first matches the Wardrobe and Combinations: the thing you just created is the thing
     * you are most likely to want next. Name is the tiebreaker so the order is total.
     *
     * Archived groups are excluded, for the same reason archived garments are — they have been
     * deleted as far as the user is concerned, and survive only so the history that mentions them
     * still reads correctly. Nothing can archive one until Phase 5.
     */
    @Query("SELECT * FROM groups WHERE archived = 0 ORDER BY createdOn DESC, id DESC")
    fun observeAll(): Flow<List<PeopleGroup>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun byId(id: Long): PeopleGroup?

    @Insert
    suspend fun insert(group: PeopleGroup): Long

    @Update
    suspend fun update(group: PeopleGroup)

    /** Retires a group that has seen something, instead of deleting it. Unused until Phase 5. */
    @Query("UPDATE groups SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Delete
    suspend fun delete(group: PeopleGroup)
}
