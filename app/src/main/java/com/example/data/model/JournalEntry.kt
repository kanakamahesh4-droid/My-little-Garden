package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val growthStatus: String,
    val note: String,
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
