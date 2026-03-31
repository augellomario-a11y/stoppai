// FILE: AriaMessaggioDao.kt
// SCOPO: Interfaccia Accesso Dati (DAO) per le trascrizioni ARIA (SA-093)
// ULTIMA MODIFICA: 2026-03-30

package com.ifs.stoppai.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Accesso dati alla tabella delle trascrizioni AI
 */
@Dao
interface AriaMessaggioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserisci(msg: AriaMessaggio)

    @Query("SELECT * FROM aria_messaggi WHERE numero = :numero ORDER BY timestamp DESC")
    fun getPerNumero(numero: String): Flow<List<AriaMessaggio>>

    @Query("SELECT * FROM aria_messaggi WHERE callLogId = :callId ORDER BY timestamp DESC")
    fun getPerCallLogId(callId: Long): Flow<List<AriaMessaggio>>

    @Query("SELECT * FROM aria_messaggi ORDER BY timestamp DESC")
    fun getTutti(): Flow<List<AriaMessaggio>>

    @Query("SELECT COUNT(*) FROM aria_messaggi WHERE letto = 0")
    fun getNonLetti(): Flow<Int>

    @Update
    suspend fun aggiorna(msg: AriaMessaggio)

    @Delete
    suspend fun elimina(msg: AriaMessaggio)
}
