// FILE: StoppAiDatabase.kt
// SCOPO: Inizializzazione Room DB base
// DIPENDENZE: CallLogEntry.kt, CallLogDao.kt
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallLogEntry::class, AppSettingsEntity::class], version = 6, exportSchema = false)
abstract class StoppAiDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: StoppAiDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (`key` TEXT PRIMARY KEY NOT NULL, `value` TEXT NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN nota TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN ariaNote TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN callOutcome TEXT NOT NULL DEFAULT 'PASSATA'")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN displayName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN callDirection TEXT NOT NULL DEFAULT 'ENTRATA'")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN smsInviato INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN smsRisposta TEXT")
            }
        }

        fun getInstance(context: Context): StoppAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoppAiDatabase::class.java,
                    "stoppai_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
