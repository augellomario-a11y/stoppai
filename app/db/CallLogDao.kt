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

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE phoneNumber = :number")
    suspend fun getCountForNumber(number: String): Int

    @Query("SELECT * FROM call_log_entries WHERE phoneNumber = :number AND statusId = 1 LIMIT 1")
    suspend fun isNumberReliable(number: String): CallLogEntry?

    @Query("SELECT * FROM call_log_entries ORDER BY timestamp DESC LIMIT 100")
    suspend fun getAllCallsSync(): List<CallLogEntry>

    @Query("UPDATE call_log_entries SET displayName = :name WHERE id = :id")
    suspend fun updateDisplayName(id: Long, name: String)
}
