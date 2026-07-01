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

    @Query("SELECT * FROM growth_metrics WHERE plantId = :plantId ORDER BY timestamp ASC")
    fun getGrowthMetricsForPlant(plantId: Int): Flow<List<com.example.data.model.GrowthMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrowthMetric(metric: com.example.data.model.GrowthMetric): Long

    @Delete
    suspend fun deleteGrowthMetric(metric: com.example.data.model.GrowthMetric)

    @Query("SELECT * FROM care_tasks ORDER BY scheduledDate ASC")
    fun getAllCareTasks(): Flow<List<com.example.data.model.CareTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCareTask(task: com.example.data.model.CareTask): Long

    @Update
    suspend fun updateCareTask(task: com.example.data.model.CareTask)

    @Delete
    suspend fun deleteCareTask(task: com.example.data.model.CareTask)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWikiPlantRecord(record: com.example.data.model.WikiPlantRecord): Long

    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getUserProfile(): Flow<com.example.data.model.UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: com.example.data.model.UserProfile)

    @Query("SELECT COUNT(*) FROM search_records")
    fun getSearchCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM search_records")
    suspend fun getSearchCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchRecord(record: com.example.data.model.SearchRecord): Long

    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<com.example.data.model.TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionRecord(record: com.example.data.model.TransactionRecord): Long
}
