package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val customName: String,
    val species: String,
    val lastWatered: Long = System.currentTimeMillis(),
    val wateringIntervalDays: Int = 7,
    val sunlight: String = "Indirect Bright Light",
    val difficulty: String = "Easy",
    val healthStatus: String = "Healthy",
    val careInstructions: String = "",
    val imageUrl: String? = null,
    val location: String = "Indoor",
    val addedDate: Long = System.currentTimeMillis()
)
