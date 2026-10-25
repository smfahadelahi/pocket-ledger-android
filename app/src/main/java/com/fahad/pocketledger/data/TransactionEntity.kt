package com.fahad.pocketledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val type: String,
    val category: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val currency: String = "KRW"
)