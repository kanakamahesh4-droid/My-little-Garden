package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Plant
import com.example.data.model.Diagnosis
import com.example.data.model.PlantIdentification
import com.example.data.model.JournalEntry
import com.example.data.dao.GardenDao

@Database(
    entities = [Plant::class, Diagnosis::class, PlantIdentification::class, JournalEntry::class],
    version = 3,
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
