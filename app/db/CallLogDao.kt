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
    suspend fun insertCallLog(callLog: CallLogEntry): Long

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

    @Query("SELECT * FROM call_log_entries WHERE phoneNumber = :number ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEntryForNumber(number: String): CallLogEntry?

    @Query("UPDATE call_log_entries SET smsRisposta = :text WHERE id = :id")
    suspend fun updateSmsRisposta(id: Long, text: String)

    @Query("DELETE FROM call_log_entries WHERE id = :id")
    suspend fun deleteCall(id: Long)

    @Query("DELETE FROM call_log_entries")
    suspend fun deleteAllCalls()

    @Query("DELETE FROM call_log_entries WHERE timestamp >= :since")
    suspend fun deleteCallsSince(since: Long)
    @Query("SELECT * FROM call_log_entries WHERE SUBSTR(REPLACE(REPLACE(phoneNumber, '+39', ''), '+', ''), -10) = SUBSTR(REPLACE(REPLACE(:numero, '+39', ''), '+', ''), -10) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentByNumber(numero: String): CallLogEntry?

    @Query("SELECT * FROM call_log_entries WHERE id = :id")
    suspend fun getCallById(id: Long): CallLogEntry?

    // --- STATISTICHE PER SYNC (SA-123) ---
    @Query("SELECT COUNT(*) FROM call_log_entries WHERE displayName != '' AND callOutcome != 'PASSATA' AND callDirection = 'ENTRATA' AND timestamp > :since")
    suspend fun countConosciutiNonRispostiSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE displayName = '' AND callType = 'MOBILE_IT' AND callOutcome != 'PASSATA' AND callDirection = 'ENTRATA' AND timestamp > :since")
    suspend fun countSconosciutiMobileNonRispostiSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE displayName = '' AND callType = 'MOBILE_IT' AND smsInviato = 1 AND timestamp > :since")
    suspend fun countSconosciutiMobileSmsSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE displayName = '' AND callType = 'MOBILE_IT' AND callOutcome = 'DEVIATA' AND timestamp > :since")
    suspend fun countSconosciutiMobileSegrSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE displayName = '' AND callType = 'FISSO_IT' AND callOutcome != 'PASSATA' AND callDirection = 'ENTRATA' AND timestamp > :since")
    suspend fun countSconosciutiFissiNonRispostiSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE displayName = '' AND callType = 'FISSO_IT' AND callOutcome = 'DEVIATA' AND timestamp > :since")
    suspend fun countSconosciutiFissiSegrSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE (callType = 'NASCOSTO' OR phoneNumber = '' OR phoneNumber LIKE '%nascosto%') AND callOutcome != 'PASSATA' AND callDirection = 'ENTRATA' AND timestamp > :since")
    suspend fun countPrivatiNonRispostiSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM call_log_entries WHERE (callType = 'NASCOSTO' OR phoneNumber = '' OR phoneNumber LIKE '%nascosto%') AND callOutcome = 'DEVIATA' AND timestamp > :since")
    suspend fun countPrivatiSegrSince(since: Long): Int
}
