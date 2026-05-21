package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.data.models.FlightLogEntity
import com.example.data.models.SavedMissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DroneDao {
    // --- Flight Logs ---
    @Query("SELECT * FROM flight_logs ORDER BY timestamp DESC")
    fun getAllFlightLogs(): Flow<List<FlightLogEntity>>

    @Query("SELECT * FROM flight_logs WHERE id = :id")
    suspend fun getFlightLogById(id: Long): FlightLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlightLog(log: FlightLogEntity): Long

    @Query("DELETE FROM flight_logs WHERE id = :id")
    suspend fun deleteFlightLogById(id: Long)

    @Query("DELETE FROM flight_logs")
    suspend fun clearAllFlightLogs()

    // --- Saved Missions ---
    @Query("SELECT * FROM saved_missions ORDER BY createdTimestamp DESC")
    fun getAllSavedMissions(): Flow<List<SavedMissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedMission(mission: SavedMissionEntity): Long

    @Query("DELETE FROM saved_missions WHERE id = :id")
    suspend fun deleteSavedMissionById(id: Long)
}

@Database(entities = [FlightLogEntity::class, SavedMissionEntity::class], version = 1, exportSchema = false)
abstract class DroneDatabase : RoomDatabase() {
    abstract fun droneDao(): DroneDao
}
