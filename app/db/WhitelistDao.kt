// FILE: WhitelistDao.kt
package com.ifs.stoppai.db

import androidx.room.*

@Dao
interface WhitelistDao {
    @Query("SELECT * FROM whitelist ORDER BY label ASC")
    suspend fun getAll(): List<WhitelistEntry>

    @Insert
    suspend fun insert(entry: WhitelistEntry): Long

    @Delete
    suspend fun delete(entry: WhitelistEntry)

    @Query("DELETE FROM whitelist WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM whitelist")
    fun getAllSync(): List<WhitelistEntry>
}
