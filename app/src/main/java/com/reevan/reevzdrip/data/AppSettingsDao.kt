package com.reevan.reevzdrip.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    /** Null until something has been changed; callers substitute [AppSettings] defaults. */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettings?>

    /** Read once, for the read-modify-write in a setter. */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettings?

    /** REPLACE on a fixed primary key is what makes this an upsert of the single row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettings)
}
