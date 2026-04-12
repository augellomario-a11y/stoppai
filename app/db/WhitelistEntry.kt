// FILE: WhitelistEntry.kt
// SCOPO: Entity Room per white list numeri/prefissi consentiti
package com.ifs.stoppai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist")
data class WhitelistEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String,       // Es: "Malta", "Italia-Roma", "Corriere"
    val pattern: String,     // Es: "+356*", "+3906*", "+39066751384"
    val createdAt: Long = System.currentTimeMillis()
)
