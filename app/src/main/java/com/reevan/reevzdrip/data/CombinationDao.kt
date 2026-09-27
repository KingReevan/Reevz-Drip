package com.reevan.reevzdrip.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CombinationDao {

    /**
     * Every outfit with its garments, newest first.
     *
     * `@Transaction` is required, not optional decoration: Room runs the combination query and the
     * garment query separately, and without it a save landing between the two would produce an
     * outfit holding garments it no longer has.
     *
     * Archived garments are **not** filtered out here, which is the whole point of archiving — an
     * outfit keeps rendering exactly as it was built.
     */
    @Transaction
    @Query("SELECT * FROM combinations WHERE archived = 0 ORDER BY createdOn DESC, id DESC")
    fun observeAll(): Flow<List<CombinationWithGarments>>

    @Transaction
    @Query("SELECT * FROM combinations WHERE id = :id")
    fun observeById(id: Long): Flow<CombinationWithGarments?>

    @Insert
    suspend fun insertCombination(combination: Combination): Long

    @Update
    suspend fun updateCombination(combination: Combination)

    @Insert
    suspend fun insertItems(items: List<CombinationItem>)

    @Query("DELETE FROM combination_items WHERE combinationId = :combinationId")
    suspend fun clearItems(combinationId: Long)

    /**
     * Retires an outfit that has been worn, instead of deleting it. Its membership rows and its
     * plan entries stay exactly as they are — that is the history.
     */
    @Query("UPDATE combinations SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    /**
     * Really deletes an outfit; its membership rows cascade away with it. Only safe when no plan
     * entry references it — the `RESTRICT` foreign key on `plan_entries` refuses otherwise, which
     * is the intended backstop rather than a case to handle.
     */
    @Delete
    suspend fun deleteCombination(combination: Combination)

    /**
     * Creates an outfit and its membership rows as one unit.
     *
     * Transactional because a combination with no garments is not a thing this app has any way to
     * show or edit — a half-applied save would leave an empty card the user could not fix.
     */
    @Transaction
    suspend fun create(combination: Combination, garmentIds: List<Long>): Long {
        val id = insertCombination(combination)
        insertItems(garmentIds.map { CombinationItem(combinationId = id, garmentId = it) })
        return id
    }

    /**
     * Rewrites an existing outfit: its name, and the whole set of garments.
     *
     * Replacing the membership rows wholesale rather than diffing them is deliberate. The set is a
     * handful of rows, the table has no other columns to preserve, and a diff is three code paths
     * (added / removed / unchanged) where this is one.
     */
    @Transaction
    suspend fun replace(combination: Combination, garmentIds: List<Long>) {
        updateCombination(combination)
        clearItems(combination.id)
        insertItems(garmentIds.map { CombinationItem(combinationId = combination.id, garmentId = it) })
    }
}
