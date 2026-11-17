package com.example.airqualitymonitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AirQualityEntity::class], version = 1, exportSchema = false)
abstract class AirQualityDatabase : RoomDatabase() {

    abstract fun airQualityDao(): AirQualityDao

    companion object {
        @Volatile
        private var INSTANCE: AirQualityDatabase? = null

        fun getDatabase(context: Context): AirQualityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AirQualityDatabase::class.java,
                    "air_quality_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
