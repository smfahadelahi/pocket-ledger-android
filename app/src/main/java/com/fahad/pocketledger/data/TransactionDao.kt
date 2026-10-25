package com.fahad.pocketledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>
    @Insert suspend fun insert(item: TransactionEntity)
    @Delete suspend fun delete(item: TransactionEntity)
}