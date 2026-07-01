package com.example.data.repository

import com.example.data.dao.GardenDao
import com.example.data.model.Plant
import com.example.data.model.Diagnosis
import com.example.data.model.PlantIdentification
import com.example.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

class GardenRepository(private val gardenDao: GardenDao) {
    val allPlants: Flow<List<Plant>> = gardenDao.getAllPlants()
    val allDiagnoses: Flow<List<Diagnosis>> = gardenDao.getAllDiagnoses()
    val allIdentifications: Flow<List<PlantIdentification>> = gardenDao.getAllIdentifications()

    fun getPlantById(id: Int): Flow<Plant?> = gardenDao.getPlantById(id)

    suspend fun insertPlant(plant: Plant): Long = gardenDao.insertPlant(plant)

    suspend fun updatePlant(plant: Plant) = gardenDao.updatePlant(plant)

    suspend fun deletePlant(plant: Plant) = gardenDao.deletePlant(plant)

    suspend fun waterPlant(id: Int, timestamp: Long) = gardenDao.waterPlant(id, timestamp)

    suspend fun insertDiagnosis(diagnosis: Diagnosis): Long = gardenDao.insertDiagnosis(diagnosis)

    suspend fun insertIdentification(identification: PlantIdentification): Long = gardenDao.insertIdentification(identification)

    fun getJournalEntriesForPlant(plantId: Int): Flow<List<JournalEntry>> = gardenDao.getJournalEntriesForPlant(plantId)

    suspend fun insertJournalEntry(entry: JournalEntry): Long = gardenDao.insertJournalEntry(entry)

    suspend fun deleteJournalEntry(entry: JournalEntry) = gardenDao.deleteJournalEntry(entry)

    fun getGrowthMetricsForPlant(plantId: Int): Flow<List<com.example.data.model.GrowthMetric>> = gardenDao.getGrowthMetricsForPlant(plantId)

    suspend fun insertGrowthMetric(metric: com.example.data.model.GrowthMetric): Long = gardenDao.insertGrowthMetric(metric)

    suspend fun deleteGrowthMetric(metric: com.example.data.model.GrowthMetric) = gardenDao.deleteGrowthMetric(metric)

    val allCareTasks: Flow<List<com.example.data.model.CareTask>> = gardenDao.getAllCareTasks()

    suspend fun insertCareTask(task: com.example.data.model.CareTask): Long = gardenDao.insertCareTask(task)

    suspend fun updateCareTask(task: com.example.data.model.CareTask) = gardenDao.updateCareTask(task)

    suspend fun deleteCareTask(task: com.example.data.model.CareTask) = gardenDao.deleteCareTask(task)

    suspend fun insertWikiPlantRecord(record: com.example.data.model.WikiPlantRecord): Long = gardenDao.insertWikiPlantRecord(record)

    val userProfile: Flow<com.example.data.model.UserProfile?> = gardenDao.getUserProfile()
    suspend fun insertUserProfile(profile: com.example.data.model.UserProfile) = gardenDao.insertUserProfile(profile)

    val searchCount: Flow<Int> = gardenDao.getSearchCount()
    suspend fun getSearchCountDirect(): Int = gardenDao.getSearchCountDirect()
    suspend fun insertSearchRecord(record: com.example.data.model.SearchRecord): Long = gardenDao.insertSearchRecord(record)

    val allTransactions: Flow<List<com.example.data.model.TransactionRecord>> = gardenDao.getAllTransactions()
    suspend fun insertTransactionRecord(record: com.example.data.model.TransactionRecord): Long = gardenDao.insertTransactionRecord(record)
}
