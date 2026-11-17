package com.example.airqualitymonitor.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AirQualityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: AirQualityEntity): Long

    @Query("SELECT * FROM air_quality_history ORDER BY id DESC")
    suspend fun getAllReadings(): List<AirQualityEntity>

    @Delete
    suspend fun delete(reading: AirQualityEntity)

    @Query("DELETE FROM air_quality_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
