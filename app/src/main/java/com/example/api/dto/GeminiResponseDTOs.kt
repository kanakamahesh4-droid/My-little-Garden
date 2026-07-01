package com.example.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WikiCarePlanDTO(
    @Json(name = "watering") val watering: String,
    @Json(name = "sunlight") val sunlight: String,
    @Json(name = "soil") val soil: String,
    @Json(name = "fertilizer") val fertilizer: String
)

@JsonClass(generateAdapter = true)
data class WikiDiagnosisDTO(
    @Json(name = "common_diseases") val commonDiseases: List<String>,
    @Json(name = "treatment_steps") val treatmentSteps: List<String>,
    @Json(name = "home_remedies") val homeRemedies: List<String>
)

@JsonClass(generateAdapter = true)
data class WikiRecordDTO(
    @Json(name = "common_name") val commonName: String,
    @Json(name = "scientific_name") val scientificName: String,
    @Json(name = "synonyms") val synonyms: List<String>,
    @Json(name = "care_plan") val carePlan: WikiCarePlanDTO,
    @Json(name = "diagnosis") val diagnosis: WikiDiagnosisDTO
)

@JsonClass(generateAdapter = true)
data class PlantIdentificationDTO(
    @Json(name = "detectedName") val detectedName: String,
    @Json(name = "scientificName") val scientificName: String,
    @Json(name = "familyName") val familyName: String? = null,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "description") val description: String,
    @Json(name = "origin") val origin: String,
    @Json(name = "growthHabits") val growthHabits: String,
    @Json(name = "careNeeds") val careNeeds: String,
    @Json(name = "lightGuide") val lightGuide: String,
    @Json(name = "waterGuide") val waterGuide: String,
    @Json(name = "soilGuide") val soilGuide: String,
    @Json(name = "petSafe") val petSafe: Boolean,
    @Json(name = "fertilizerGuide") val fertilizerGuide: String? = null,
    @Json(name = "remediesForGrowth") val remediesForGrowth: List<String>? = null,
    @Json(name = "referenceImages") val referenceImages: List<String>? = null,
    @Json(name = "hasIssue") val hasIssue: Boolean? = false,
    @Json(name = "diseaseName") val diseaseName: String? = null,
    @Json(name = "symptoms") val symptoms: String? = null,
    @Json(name = "treatmentSteps") val treatmentSteps: List<String>? = null,
    @Json(name = "homeRemedies") val homeRemedies: List<String>? = null,
    @Json(name = "prevention") val prevention: String? = null,
    @Json(name = "wikiRecord") val wikiRecord: WikiRecordDTO? = null
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

@JsonClass(generateAdapter = true)
data class DeficiencyAnalysisDTO(
    @Json(name = "plantCondition") val plantCondition: String,
    @Json(name = "lackingCategory") val lackingCategory: String,
    @Json(name = "mineralsAnalysis") val mineralsAnalysis: String,
    @Json(name = "sunlightAnalysis") val sunlightAnalysis: String,
    @Json(name = "waterAnalysis") val waterAnalysis: String,
    @Json(name = "vitaminsAnalysis") val vitaminsAnalysis: String,
    @Json(name = "medicineAnalysis") val medicineAnalysis: String,
    @Json(name = "remedialPlan") val remedialPlan: List<String>
)
