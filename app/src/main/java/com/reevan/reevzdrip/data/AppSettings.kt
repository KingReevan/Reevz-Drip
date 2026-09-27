package com.reevan.reevzdrip.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/**
 * App-wide preferences. A single row, id [SINGLETON_ID].
 *
 * One row rather than a key/value table: there is one app with a handful of settings, and a
 * key/value store would trade compile-time field names for runtime string keys and nullable reads
 * in exchange for flexibility nothing here needs. New preferences are new columns — each one an
 * additive migration Room can generate.
 *
 * The row does not exist until something is changed; readers substitute the defaults below, so a
 * fresh install behaves exactly as if it had been written.
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
