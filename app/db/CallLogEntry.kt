// FILE: CallLogEntry.kt
// SCOPO: Entity DB per la cronologia chiamate con campi CRM
// DIPENDENZE: Nessuna
// ULTIMA MODIFICA: 2026-03-23

package com.ifs.stoppai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_log_entries")
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val callType: String,
    val timestamp: Long,
    val statusId: Int = 0, // 0=DA TRATTARE (Rosso), 1=ATTENDIBILE (Verde), 2=SPAM (Grigio), 3=NOTE (Blu)
    val nota: String = "",
    val ariaNote: String = "",
    val callOutcome: String = "PASSATA", 
    val callDirection: String = "ENTRATA", // ENTRATA / USCITA
    val displayName: String = ""
)

data class CallLogCrmItem(
    val entry: CallLogEntry,
    val count: Int
)
