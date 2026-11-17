package com.example.airqualitymonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "air_quality_history")
data class AirQualityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTime: String,
    val aqi: Int,
    val category: String,
    val location: String,
    val pm25: Double = 0.0,
    val pm10: Double = 0.0,
    val carbonMonoxide: Double = 0.0
)
