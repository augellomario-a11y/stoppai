// FILE: StoppAiDatabase.kt
// SCOPO: Inizializzazione Room DB base
// DIPENDENZE: CallLogEntry.kt, CallLogDao.kt
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallLogEntry::class, AppSettingsEntity::class], version = 3, exportSchema = false)
abstract class StoppAiDatabase : RoomDatabase() {
    // Espone il DAO per i log
    abstract fun callLogDao(): CallLogDao
    
    // Espone il DAO per le impostazioni
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: StoppAiDatabase? = null

        // Migrazione v1->v2 (AppSettings)
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (`key` TEXT PRIMARY KEY NOT NULL, `value` TEXT NOT NULL)")
            }
        }

        // Migrazione v2->v3 (CRM Fields)
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Nota: statusId è già presente dalla v1, ignoriamo errore se già presente
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN nota TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN ariaNote TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE call_log_entries ADD COLUMN callOutcome TEXT NOT NULL DEFAULT 'PASSATA'")
            }
        }

        fun getInstance(context: android.content.Context): StoppAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoppAiDatabase::class.java,
                    "stoppai_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
