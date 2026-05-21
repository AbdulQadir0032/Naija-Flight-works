package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Real-time core telemetry state of the autonomous drone.
 */
data class TelemetryState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val signalStrength: Int = 0, // 0 - 100%
    val packetLoss: Float = 0f, // 0.0 - 1.0
    val latencyMs: Int = 0,
    val batteryVoltage: Float = 0f, // LiPo cell state e.g., 22.2V (6S)
    val batteryPercentage: Int = 0,
    val batteryHealth: String = "EXCELLENT",
    val cpuTemp: Float = 0f, // °C
    val escTemp: Float = 0f, // °C
    val motorRpm: Int = 0,
    val satCount: Int = 0,
    val gpsStatus: String = "NO_GPS",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Float = 0f, // meters relative to home
    val obstacleDistanceM: Float = 99.9f, // LiDAR distance to nearest obstacle
    val windSpeedKph: Float = 0f,
    val pitch: Float = 0f, // degrees -90 to +90
    val roll: Float = 0f,  // degrees -180 to +180
    val yaw: Float = 0f,   // degrees 0 to 360 (compass heading)
    val airspeed: Float = 0f, // m/s
    val groundSpeed: Float = 0f, // m/s
    val verticalSpeed: Float = 0f, // m/s (climb/descend rate)
    val heading: Int = 0, // 0-359 degrees
    val distanceFromHome: Float = 0f, // meters
    val flightMode: FlightMode = FlightMode.STABILIZE,
    val flightTimeSeconds: Int = 0,
    val messageLog: List<String> = emptyList(),
    val isEmergencyActionActive: Boolean = false,
    val isGeofenceBreached: Boolean = false
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

enum class FlightMode {
    MANUAL,      // Direct control
    STABILIZE,   // Gyro assistance
    ALT_HOLD,    // Altitude control active
    AUTO_PORT,   // Autonomous waypoint execution
    LAND,        // Immediate land sequence
    RTH          // Return to home sequence
}

/**
 * Interactive Waypoint representation.
 */
data class Waypoint(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val altitude: Float = 30f, // default waypoint altitude 30m
    val speed: Float = 5f,     // m/s
    val hoverTime: Int = 2     // seconds
)

/**
 * Diagnostics indicators for critical sensors.
 */
data class SensorHealth(
    val moduleName: String,
    val status: HealthStatus,
    val payloadRateHz: Float,
    val errorRatePercentage: Float
)

enum class HealthStatus {
    OPERATIONAL,
    WARNING,
    CRITICAL,
    OFFLINE
}

/**
 * Room Entity: Completed flight histories with coordinates, states, and summary logs.
 */
@Entity(tableName = "flight_logs")
data class FlightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val droneId: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val maxAltitude: Float,
    val maxSpeed: Float,
    val waypointCount: Int,
    val startLatitude: Double,
    val startLongitude: Double,
    val flightPathCoordinates: String, // String delimited coordinates: "lat,lng;lat,lng..."
    val completionStatus: String, // "SUCCESS", "EMERGENCY_LAND", "RTH_BATTERY"
    val batteryDischargeValue: Float, // start vs end voltage drop
    val telemetryPacketCount: Long,
    val analyticsSummary: String // Gemini-assisted AI Diagnostic Briefing
)

/**
 * Room Entity: Custom saved multi-waypoint flight plans.
 */
@Entity(tableName = "saved_missions")
data class SavedMissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionName: String,
    val description: String,
    val waypointCount: Int,
    val averageAltitude: Float,
    val projectedTimeSeconds: Int,
    val createdTimestamp: Long,
    val serializedWaypoints: String // delimited structure: "lat,lng,alt,speed,hover;..."
)
