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
}
