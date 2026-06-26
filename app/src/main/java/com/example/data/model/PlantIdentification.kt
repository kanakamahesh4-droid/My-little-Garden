package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identifications")
data class PlantIdentification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val detectedName: String,
    val scientificName: String,
    val confidence: Float,
    val description: String,
    val origin: String = "",
    val growthHabits: String = "",
    val careNeeds: String = "",
    val lightGuide: String,
    val waterGuide: String,
    val soilGuide: String,
    val petSafe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
