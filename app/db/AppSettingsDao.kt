// FILE: AppSettingsDao.kt
// SCOPO: Interfaccia accesso dati per le impostazioni app
// DIPENDENZE: AppSettingsEntity.kt
// ULTIMA MODIFICA: 2026-03-22

package com.ifs.stoppai.db

import androidx.room.*

@Dao
interface AppSettingsDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setValue(setting: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    fun clearAll()
}
