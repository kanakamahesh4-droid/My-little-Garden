package com.example.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlantIdentificationDTO(
    @Json(name = "detectedName") val detectedName: String,
    @Json(name = "scientificName") val scientificName: String,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "description") val description: String,
    @Json(name = "origin") val origin: String,
    @Json(name = "growthHabits") val growthHabits: String,
    @Json(name = "careNeeds") val careNeeds: String,
    @Json(name = "lightGuide") val lightGuide: String,
    @Json(name = "waterGuide") val waterGuide: String,
    @Json(name = "soilGuide") val soilGuide: String,
    @Json(name = "petSafe") val petSafe: Boolean
)

@JsonClass(generateAdapter = true)
data class DiseaseDiagnosisDTO(
    @Json(name = "plantName") val plantName: String,
    @Json(name = "diseaseName") val diseaseName: String,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "symptoms") val symptoms: String,
    @Json(name = "treatmentSteps") val treatmentSteps: List<String>,
    @Json(name = "homeRemedies") val homeRemedies: List<String>,
    @Json(name = "prevention") val prevention: String
)

@JsonClass(generateAdapter = true)
data class PersonalizedCarePlanDTO(
    @Json(name = "plantName") val plantName: String,
    @Json(name = "wateringFrequency") val wateringFrequency: String,
    @Json(name = "wateringIntervalDays") val wateringIntervalDays: Int,
    @Json(name = "sunlightRequirements") val sunlightRequirements: String,
    @Json(name = "humidity") val humidity: String,
    @Json(name = "temperature") val temperature: String,
    @Json(name = "fertilizer") val fertilizer: String,
    @Json(name = "soilType") val soilType: String,
    @Json(name = "careTips") val careTips: List<String>
)
