package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Plant
import com.example.data.model.Diagnosis
import com.example.data.model.PlantIdentification
import com.example.data.model.JournalEntry
import com.example.data.model.CareTask
import com.example.data.model.WikiPlantRecord
import com.example.data.dao.GardenDao

import com.example.data.model.GrowthMetric

@Database(
    entities = [
        Plant::class,
        Diagnosis::class,
        PlantIdentification::class,
        JournalEntry::class,
        CareTask::class,
        WikiPlantRecord::class,
        GrowthMetric::class,
        com.example.data.model.SearchRecord::class,
        com.example.data.model.UserProfile::class,
        com.example.data.model.TransactionRecord::class
    ],
    version = 8,
    exportSchema = false
)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun gardenDao(): GardenDao

    companion object {
        @Volatile
        private var INSTANCE: GardenDatabase? = null

        fun getDatabase(context: Context): GardenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GardenDatabase::class.java,
                    "garden_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
