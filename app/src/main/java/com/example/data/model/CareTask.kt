package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "care_tasks")
data class CareTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int = 0,
    val plantName: String,
    val taskType: String, // "Watering", "Fertilizing", "Sunlight/Rotation", "Vitamins", "Medicine/Pesticides", "Pruning"
    val scheduledDate: Long,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val completedDate: Long? = null
)
