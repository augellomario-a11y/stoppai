// FILE: CallLogEntry.kt
// SCOPO: Entity DB per la cronologia chiamate
// DIPENDENZE: Nessuna
// ULTIMA MODIFICA: 2026-03-20

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
    val statusId: Int = 0, // 0=da trattare, 1=trattato, 2=attendibile, 3=spam, 4=possibile spam
    val nota: String = "",
    val ariaNote: String = "",
    val callOutcome: String = "PASSATA" // PASSATA / DEVIATA / MANCATA
)
