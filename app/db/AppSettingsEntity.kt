// FILE: AppSettingsEntity.kt
// SCOPO: Definizione tabella chiave-valore per le impostazioni app
// DIPENDENZE: Room
// ULTIMA MODIFICA: 2026-03-22

package com.ifs.stoppai.db

import androidx.room.*

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
