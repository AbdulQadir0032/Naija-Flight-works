package com.example.data.repository

import com.example.data.database.DroneDao
import com.example.data.models.FlightLogEntity
import com.example.data.models.SavedMissionEntity
import kotlinx.coroutines.flow.Flow

class DroneRepository(private val droneDao: DroneDao) {

    val allFlightLogs: Flow<List<FlightLogEntity>> = droneDao.getAllFlightLogs()
    val allSavedMissions: Flow<List<SavedMissionEntity>> = droneDao.getAllSavedMissions()

    suspend fun getFlightLogById(id: Long): FlightLogEntity? {
        return droneDao.getFlightLogById(id)
    }

    suspend fun saveFlightLog(log: FlightLogEntity): Long {
        return droneDao.insertFlightLog(log)
    }

    suspend fun deleteFlightLog(id: Long) {
        return droneDao.deleteFlightLogById(id)
    }

    suspend fun clearAllFlightLogs() {
        return droneDao.clearAllFlightLogs()
    }

    suspend fun saveMission(mission: SavedMissionEntity): Long {
        return droneDao.insertSavedMission(mission)
    }

    suspend fun deleteSavedMission(id: Long) {
        return droneDao.deleteSavedMissionById(id)
    }
}
