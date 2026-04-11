// FILE: AriaMessaggio.kt
// SCOPO: Entity Room per la memorizzazione locale delle trascrizioni ARIA (SA-093)
// ULTIMA MODIFICA: 2026-03-30

package com.ifs.stoppai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rappresenta un messaggio trascritto dall'AI ARIA
 */
@Entity(tableName = "aria_messaggi")
data class AriaMessaggio(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val numero: String,
    val testo: String,
    val timestamp: Long,
    val callLogId: Long = 0L,
    val letto: Boolean = false,
    val stato: String = "DA_TRATTARE",
    val wavFilename: String? = null,
    val spamVoto: String? = null  // "spam" | "attendibile" | null
)
