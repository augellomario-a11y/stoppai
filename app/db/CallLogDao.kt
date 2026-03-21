// FILE: CallLogDao.kt
// SCOPO: Definizione dei DAO (Accesso Dati) per i log
// DIPENDENZE: CallLogEntry.kt
// ULTIMA MODIFICA: 2026-03-20

package com.ifs.stoppai.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Insert
    suspend fun insertCallLog(callLog: CallLogEntry)

    @Query("SELECT * FROM call_log_entries ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CallLogEntry>>
}
