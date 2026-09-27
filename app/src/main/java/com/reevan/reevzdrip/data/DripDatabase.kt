package com.reevan.reevzdrip.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema history — **one line per version, every time.**
 * - v1: `garments`
 * - v2: added `combinations` and `combination_items`; added `garments.archived`
 * - v3: added `groups`
 * - v4: added `plan_entries` and `plan_entry_groups`; added `combinations.archived`
 * - v5: added `app_settings`
 *
 * Version bumps must add a migration here. Adding a table, or a column with a `defaultValue`, is
 * purely additive, so Room generates the migration from the exported schemas in `app/schemas/`.
 * Never use `fallbackToDestructiveMigration` — a wardrobe and its wear history are not
 * recoverable.
 *
 * The schema is now complete for the requirements as written.
 */
@Database(
    entities = [
        Garment::class,
        Combination::class,
        CombinationItem::class,
        PeopleGroup::class,
        PlanEntry::class,
        PlanEntryGroup::class,
        AppSettings::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        // Two new tables, plus garments.archived defaulting to 0. Every existing garment is
        // therefore un-archived, which is exactly what it was before the column existed.
        AutoMigration(from = 1, to = 2),
        // One new table. No existing row is read or rewritten, and an empty `groups` table means
        // nobody has been recorded as seeing anything — which is the truth.
        AutoMigration(from = 2, to = 3),
        // Two new tables, plus combinations.archived defaulting to 0 — so every outfit that
        // already exists is un-archived, which is what it was before the column existed.
        AutoMigration(from = 3, to = 4),
        // One new table. Its row does not exist until a setting is changed, and an absent row
        // means "all defaults" — which is what every previous version behaved as.
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class DripDatabase : RoomDatabase() {

    abstract fun garmentDao(): GarmentDao

    abstract fun combinationDao(): CombinationDao

    abstract fun groupDao(): GroupDao

    abstract fun planDao(): PlanDao

    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        private const val NAME = "reevz-drip.db"

        @Volatile
        private var instance: DripDatabase? = null

        fun getInstance(context: Context): DripDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DripDatabase::class.java,
                    NAME,
                )
                    // SQLite ignores foreign keys unless they are switched on per connection, and
                    // this app leans on them: ON DELETE RESTRICT is what stops a garment deletion
                    // silently rewriting an outfit somebody already saw, and ON DELETE CASCADE is
                    // what clears an outfit's membership rows with it. A constraint that is
                    // quietly ignored is worse than none, because the code around it assumes
                    // protection it does not have.
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys=ON")
                        }
                    })
                    .build()
                    .also { instance = it }
            }
    }
}
