package com.example.data.dao

import androidx.room.*
import com.example.data.model.Plant
import com.example.data.model.Diagnosis
import com.example.data.model.PlantIdentification
import com.example.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenDao {
    @Query("SELECT * FROM plants ORDER BY addedDate DESC")
    fun getAllPlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun getPlantById(id: Int): Flow<Plant?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant): Long

    @Update
    suspend fun updatePlant(plant: Plant)

    @Delete
    suspend fun deletePlant(plant: Plant)

    @Query("UPDATE plants SET lastWatered = :timestamp, healthStatus = 'Healthy' WHERE id = :id")
    suspend fun waterPlant(id: Int, timestamp: Long)

    @Query("SELECT * FROM diagnoses ORDER BY timestamp DESC")
    fun getAllDiagnoses(): Flow<List<Diagnosis>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosis(diagnosis: Diagnosis): Long

    @Query("SELECT * FROM identifications ORDER BY timestamp DESC")
    fun getAllIdentifications(): Flow<List<PlantIdentification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentification(identification: PlantIdentification): Long

    @Query("SELECT * FROM journal_entries WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun getJournalEntriesForPlant(plantId: Int): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry): Long

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntry)
}
