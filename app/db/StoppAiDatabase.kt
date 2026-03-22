// FILE: StoppAiDatabase.kt
// SCOPO: Inizializzazione Room DB base
// DIPENDENZE: CallLogEntry.kt, CallLogDao.kt
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallLogEntry::class, AppSettingsEntity::class], version = 2, exportSchema = false)
abstract class StoppAiDatabase : RoomDatabase() {
    // Espone il DAO per i log
    abstract fun callLogDao(): CallLogDao
    
    // Espone il DAO per le impostazioni
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: StoppAiDatabase? = null

        // Definizione della migrazione per aggiungere la tabella app_settings
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (`key` TEXT PRIMARY KEY NOT NULL, `value` TEXT NOT NULL)")
            }
        }

        fun getInstance(context: android.content.Context): StoppAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoppAiDatabase::class.java,
                    "stoppai_database"
                )
                .addMigrations(MIGRATION_1_2) // Aggiunta migration
                .allowMainThreadQueries() // Necessaria per la lettura settings sincrona nel Service
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
