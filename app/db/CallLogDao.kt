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
    fun getAllCalls(): kotlinx.coroutines.flow.Flow<List<CallLogEntry>>

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE timestamp > :startOfDay")
    fun getCallsToday(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM call_log_entries")
    fun getTotalCalls(): Int

    @Query("UPDATE call_log_entries SET statusId = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)

    @Query("UPDATE call_log_entries SET nota = :nota WHERE id = :id")
    suspend fun updateNota(id: Long, nota: String)

    @Query("DELETE FROM call_log_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
