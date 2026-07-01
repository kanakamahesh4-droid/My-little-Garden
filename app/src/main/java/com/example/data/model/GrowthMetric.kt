package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "growth_metrics")
data class GrowthMetric(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val heightCm: Float,
    val leafCount: Int,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
