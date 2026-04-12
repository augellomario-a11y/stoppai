// FILE: StoppAiDatabase.kt
// SCOPO: Inizializzazione Room DB base
// DIPENDENZE: CallLogEntry.kt, CallLogDao.kt
// ULTIMA MODIFICA: 2026-03-24

package com.ifs.stoppai.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallLogEntry::class, AppSettingsEntity::class, AriaMessaggio::class, WhitelistEntry::class], version = 10, exportSchema = false)
abstract class StoppAiDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun ariaMessaggioDao(): AriaMessaggioDao
    abstract fun whitelistDao(): WhitelistDao

    companion object {
        @Volatile
        private var INSTANCE: StoppAiDatabase? = null

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE aria_messaggi ADD COLUMN callLogId INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE aria_messaggi ADD COLUMN wavFilename TEXT")
                db.execSQL("ALTER TABLE aria_messaggi ADD COLUMN spamVoto TEXT")
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS whitelist (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, label TEXT NOT NULL, pattern TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        fun getInstance(context: Context): StoppAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoppAiDatabase::class.java,
                    "stoppai_database"
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
