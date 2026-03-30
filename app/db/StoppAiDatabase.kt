// FILE: StoppAiDatabase.kt
// SCOPO: Inizializzazione Room DB base
// DIPENDENZE: CallLogEntry.kt, CallLogDao.kt
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallLogEntry::class, AppSettingsEntity::class, AriaMessaggio::class], version = 7, exportSchema = false)
abstract class StoppAiDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun ariaMessaggioDao(): AriaMessaggioDao

    companion object {
        @Volatile
        private var INSTANCE: StoppAiDatabase? = null

        fun getInstance(context: Context): StoppAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoppAiDatabase::class.java,
                    "stoppai_database"
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
