package com.reevan.reevzdrip.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A garment plus how many outfits it belongs to.
 *
 * The count travels with the row rather than being fetched when needed, because the one place it
 * matters — the delete confirmation — has to be able to say *"this is in 2 outfits"* the instant
 * the button is tapped. A separate query there would mean either a dialog that appears blank for a
 * frame or a delete that asks the wrong question.
 */
data class GarmentWithUsage(
    @Embedded val garment: Garment,
    val combinationCount: Int,
)

@Dao
interface GarmentDao {

    /**
     * Every garment still in the wardrobe, newest first, with its outfit count.
     *
     * Newest-first rather than alphabetical because the motivating moment is "I just bought three
     * things and added them" — they should be the first thing you see, and the first things you
     * reach for when building an outfit. Name is the tiebreaker so the order is total and cannot
     * shuffle between reads.
     *
     * **Archived garments are excluded.** They have been deleted as far as the user is concerned;
     * they survive only so the outfits already built from them still render.
     */
    @Query(
        """
        SELECT g.*, COUNT(ci.combinationId) AS combinationCount
        FROM garments g
        LEFT JOIN combination_items ci ON ci.garmentId = g.id
        WHERE g.archived = 0
        GROUP BY g.id
        ORDER BY g.addedOn DESC, g.id DESC
        """,
    )
    fun observeAll(): Flow<List<GarmentWithUsage>>

    @Query("SELECT * FROM garments WHERE id = :id")
    suspend fun byId(id: Long): Garment?

    @Insert
    suspend fun insert(garment: Garment): Long

    @Update
    suspend fun update(garment: Garment)

    /**
     * Retires a garment that is used in an outfit, instead of deleting it.
     *
     * Its photo file is deliberately **not** deleted — the outfits it belongs to still have to
     * draw it.
     */
    @Query("UPDATE garments SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    /**
     * Really deletes a garment. Only safe when nothing references it — the `RESTRICT` foreign key
     * on `combination_items` will refuse otherwise, which is the intended backstop rather than a
     * case to handle.
     */
    @Delete
    suspend fun delete(garment: Garment)
}
