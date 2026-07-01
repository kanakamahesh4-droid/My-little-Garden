package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "wiki_plant_records",
    indices = [Index(value = ["common_name", "scientific_name"], unique = true)]
)
data class WikiPlantRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "common_name") val commonName: String,
    @ColumnInfo(name = "scientific_name") val scientificName: String,
    val synonyms: String, // Stored as JSON array string
    @ColumnInfo(name = "care_plan") val carePlan: String, // Stored as JSON object string
    val diagnosis: String // Stored as JSON object string
)
