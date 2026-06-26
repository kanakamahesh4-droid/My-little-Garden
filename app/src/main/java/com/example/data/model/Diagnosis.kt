package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnoses")
data class Diagnosis(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantName: String,
    val diseaseName: String,
    val confidence: Float,
    val symptoms: String,
    val treatmentSteps: String, // String with lines or separated values
    val homeRemedies: String,   // String with lines or separated values
    val prevention: String,     // Prevention tips
    val timestamp: Long = System.currentTimeMillis()
)
