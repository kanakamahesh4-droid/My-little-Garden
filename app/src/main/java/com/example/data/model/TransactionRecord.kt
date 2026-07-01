package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_records")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val amount: Float,
    val type: String, // "PLAY_BILLING" or "UPI"
    val duration: String, // "DAILY", "WEEKLY", "YEARLY"
    val tokenOrTxnId: String,
    val status: String // "SUCCESS", "PENDING", "FAILED"
)
