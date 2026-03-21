// FILE: StoppAiDatabase.kt
// SCOPO: Inizializzazione Room DB base
// DIPENDENZE: CallLogEntry.kt, CallLogDao.kt
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallLogEntry::class], version = 1, exportSchema = false)
abstract class StoppAiDatabase : RoomDatabase() {
    // Espone il DAO per i log
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: StoppAiDatabase? = null

        fun getDatabase(context: Context): StoppAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoppAiDatabase::class.java,
                    "stoppai_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
