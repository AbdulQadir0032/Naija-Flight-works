package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.ai.CopilotService
import com.example.data.database.DroneDatabase
import com.example.data.models.ConnectionStatus
import com.example.data.models.FlightLogEntity
import com.example.data.models.FlightMode
import com.example.data.models.SavedMissionEntity
import com.example.data.models.SensorHealth
import com.example.data.models.TelemetryState
import com.example.data.models.Waypoint
import com.example.data.repository.DroneRepository
import com.example.data.service.DroneSimEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin

class DroneViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Core Services Setup
    private val database: DroneDatabase by lazy {
        Room.databaseBuilder(
            application,
            DroneDatabase::class.java,
            "sky_control_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: DroneRepository by lazy {
        DroneRepository(database.droneDao())
    }

    private val simEngine = DroneSimEngine(viewModelScope)
    private val copilotService = CopilotService()

    // 2. Telemetry and Sensor Observables
    val telemetryState: StateFlow<TelemetryState> = simEngine.telemetryState
    val sensorsHealth: StateFlow<List<SensorHealth>> = simEngine.sensorsHealth

    // 3. Room DB Observables (Flight Logs & Saved Plans)
    val flightLogs: StateFlow<List<FlightLogEntity>> = repository.allFlightLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedMissions: StateFlow<List<SavedMissionEntity>> = repository.allSavedMissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Interactive Route Planner Observables
    private val _plannedWaypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val plannedWaypoints: StateFlow<List<Waypoint>> = _plannedWaypoints.asStateFlow()

    private val _selectedWaypointId = MutableStateFlow<Int?>(null)
    val selectedWaypointId: StateFlow<Int?> = _selectedWaypointId.asStateFlow()

    // 5. AI Detection & Overlay States
    private val _isAiObjectTrackingActive = MutableStateFlow(false)
    val isAiObjectTrackingActive: StateFlow<Boolean> = _isAiObjectTrackingActive.asStateFlow()

    private val _aiDetectedObjects = MutableStateFlow<List<AiDetection>>(emptyList())
    val aiDetectedObjects: StateFlow<List<AiDetection>> = _aiDetectedObjects.asStateFlow()

    // 6. Gemini Copilot Advisories State
    private val _copilotAdvice = MutableStateFlow("Tap 'REQUEST BRIEFING' to query the Gemini AI Ground Assistant.")
    val copilotAdvice: StateFlow<String> = _copilotAdvice.asStateFlow()

    private val _isCopilotLoading = MutableStateFlow(false)
    val isCopilotLoading: StateFlow<Boolean> = _isCopilotLoading.asStateFlow()

    // 7. General Settings Configurations State
    val maxGeofenceMeters = MutableStateFlow(500f)
    val defaultRthAltitudeMeters = MutableStateFlow(40f)
    val isFpsOverlayEnabled = MutableStateFlow(true)
    val activeSsidSim = MutableStateFlow("SKYTAC_DATA_LINK_808")

    // Record list for dynamic graph indicators
    private val _altitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val altitudeHistory: StateFlow<List<Float>> = _altitudeHistory.asStateFlow()

    private val _voltageHistory = MutableStateFlow<List<Float>>(emptyList())
    val voltageHistory: StateFlow<List<Float>> = _voltageHistory.asStateFlow()

    init {
        // Automatically start updating graph lists
        viewModelScope.launch {
            telemetryState.collect { telemetry ->
                if (telemetry.connectionStatus == ConnectionStatus.CONNECTED) {
                    // Update Altitude Graph
                    _altitudeHistory.update { history ->
                        val next = history.toMutableList()
                        if (next.size > 40) next.removeAt(0)
                        next.add(telemetry.altitude)
                        next
                    }
                    // Update Voltage Graph
                    _voltageHistory.update { history ->
                        val next = history.toMutableList()
                        if (next.size > 40) next.removeAt(0)
                        next.add(telemetry.batteryVoltage)
                        next
                    }

                    // Keep active bounding boxes sliding to simulate object tracking dynamics
                    if (_isAiObjectTrackingActive.value) {
                        simulateAiDetections()
                    }
                }
            }
        }
    }

    // --- Ground link executions ---
    fun clickConnectToggle() {
        val state = telemetryState.value.connectionStatus
        if (state == ConnectionStatus.DISCONNECTED) {
            simEngine.connect()
        } else {
            // If disarming, log flight details into database
            if (telemetryState.value.flightTimeSeconds > 2) {
                saveFlightToDatabase()
            }
            simEngine.disconnect()
            _altitudeHistory.value = emptyList()
            _voltageHistory.value = emptyList()
        }
    }

    fun setFlightMode(mode: FlightMode) {
        simEngine.setFlightMode(mode)
    }

    fun triggerEmergencyDisarm() {
        simEngine.triggerEmergencyDisarm()
    }

    fun clearTelemetryLogs() {
        simEngine.clearLog()
    }

    // --- Interactive Planner controls ---
    fun addPlannedWaypoint(latitude: Double, longitude: Double) {
        val currentList = _plannedWaypoints.value
        val nextId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        val wp = Waypoint(
            id = nextId,
            latitude = latitude,
            longitude = longitude,
            altitude = 30f,
            speed = 6f,
            hoverTime = 3
        )
        _plannedWaypoints.value = currentList + wp
        _selectedWaypointId.value = wp.id
    }

    fun updateSelectedWaypointDetails(altitude: Float, speed: Float, hover: Int) {
        val selectedId = _selectedWaypointId.value ?: return
        _plannedWaypoints.update { list ->
            list.map { wp ->
                if (wp.id == selectedId) {
                    wp.copy(altitude = altitude, speed = speed, hoverTime = hover)
                } else wp
            }
        }
    }

    fun deleteWaypoint(id: Int) {
        _plannedWaypoints.update { list -> list.filter { it.id != id } }
        if (_selectedWaypointId.value == id) {
            _selectedWaypointId.value = null
        }
    }

    fun clearAllPlannedWaypoints() {
        _plannedWaypoints.value = emptyList()
        _selectedWaypointId.value = null
    }

    fun selectWaypoint(id: Int) {
        _selectedWaypointId.value = id
    }

    fun uploadAndLaunchPlan() {
        val waypoints = _plannedWaypoints.value
        if (waypoints.isEmpty()) return
        simEngine.loadAndStartMission(waypoints)
    }

    fun loadSelectedSavedMission(serialized: String) {
        if (serialized.isEmpty()) return
        val wps = mutableListOf<Waypoint>()
        try {
            val sections = serialized.split(";")
            var counter = 1
            for (sec in sections) {
                if (sec.isBlank()) continue
                val parts = sec.split(",")
                if (parts.size >= 5) {
                    wps.add(
                        Waypoint(
                            id = counter++,
                            latitude = parts[0].toDouble(),
                            longitude = parts[1].toDouble(),
                            altitude = parts[2].toFloat(),
                            speed = parts[3].toFloat(),
                            hoverTime = parts[4].toInt()
                        )
                    )
                }
            }
            _plannedWaypoints.value = wps
            if (wps.isNotEmpty()) _selectedWaypointId.value = wps.first().id
        } catch (e: Exception) {
            // failed to load
        }
    }

    fun saveCurrentPlanToDatabase(name: String, notes: String) {
        val waypoints = _plannedWaypoints.value
        if (waypoints.isEmpty()) return
        val stringBuilder = StringBuilder()
        for (wp in waypoints) {
            stringBuilder.append("${wp.latitude},${wp.longitude},${wp.altitude},${wp.speed},${wp.hoverTime};")
        }
        val entity = SavedMissionEntity(
            missionName = name,
            description = notes,
            waypointCount = waypoints.size,
            averageAltitude = waypoints.map { it.altitude }.average().toFloat(),
            projectedTimeSeconds = waypoints.size * 25, // rough estimate
            createdTimestamp = System.currentTimeMillis(),
            serializedWaypoints = stringBuilder.toString()
        )
        viewModelScope.launch {
            repository.saveMission(entity)
        }
    }

    fun deleteSavedMission(id: Long) {
        viewModelScope.launch {
            repository.deleteSavedMission(id)
        }
    }

    fun deleteFlightLog(id: Long) {
        viewModelScope.launch {
            repository.deleteFlightLog(id)
        }
    }

    // --- AI tracking simulations ---
    fun toggleAiObjectTracking() {
        _isAiObjectTrackingActive.update { active ->
            val next = !active
            if (!next) {
                _aiDetectedObjects.value = emptyList()
            }
            next
        }
    }

    private fun simulateAiDetections() {
        val width = 1000f
        val height = 600f
        // Add some noise to make bounding boxes slide gracefully, simulating optical tracking
        val timeSec = System.currentTimeMillis() / 1000.0
        val driftX = sin(timeSec).toFloat() * 15f
        val driftY = cos(timeSec).toFloat() * 15f

        _aiDetectedObjects.value = listOf(
            AiDetection(
                id = "AI_OBJ_01",
                label = "VEHICLE (HUMVEE)",
                confidence = 0.94f,
                left = 220f + driftX,
                top = 180f + driftY,
                right = 380f + driftX,
                bottom = 320f + driftY
            ),
            AiDetection(
                id = "AI_OBJ_02",
                label = "HUMAN (OPERATOR)",
                confidence = 0.88f,
                left = 650f - driftY,
                top = 260f + driftX,
                right = 720f - driftY,
                bottom = 440f + driftX
            )
        )
    }

    // --- Gemini GCS advisory calls ---
    fun fetchGeminiDiagnosticAdvisory() {
        val state = telemetryState.value
        if (state.connectionStatus != ConnectionStatus.CONNECTED) {
            _copilotAdvice.value = "⚠️ OFFLINE: Ground telemetry link is disconnected. Estabish a physical/RF drone connection first."
            return
        }

        val summary = """
            Drone ID: SKY-T-V4
            Current Altitude: ${state.altitude}m
            Dynamic Airspeed: ${state.airspeed} m/s
            Battery Pack Temp: ${state.cpuTemp}°C
            LiPo cell charge: ${state.batteryPercentage}% (${state.batteryVoltage}V)
            RF link metrics: Packet-loss ${"%.3f".format(state.packetLoss)}, Rtt latency ${state.latencyMs}ms, Strength ${state.signalStrength}%
            Nearest Obstacle: ${state.obstacleDistanceM}m
            Wind condition: ${state.windSpeedKph} KPH
        """.trimIndent()

        val activeWarnings = mutableListOf<String>()
        if (state.batteryPercentage < 25) activeWarnings.add("BATTERY CRITICALLY LOW")
        if (state.windSpeedKph > 16) activeWarnings.add("HIGH LEVEL TACTICAL TURBULENCE")
        if (state.obstacleDistanceM < 6) activeWarnings.add("LIDAR OBSTACLE PROXIMITY WARN")

        _isCopilotLoading.value = true
        _copilotAdvice.value = "QUERYING MISSION ASSISTANT..."

        viewModelScope.launch {
            val advice = copilotService.analyzeFlightDiagnostics(
                telemetrySummary = summary,
                activeMode = state.flightMode.name,
                activeWarnings = activeWarnings
            )
            _copilotAdvice.value = advice
            _isCopilotLoading.value = false
        }
    }

    // --- Helper to log flight completion ---
    private fun saveFlightToDatabase() {
        val state = telemetryState.value
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val dateString = format.format(Date())

        val coordinatesMock = StringBuilder().apply {
            // Write coordinates representing some flight circle around home
            val home = Pair(state.latitude, state.longitude)
            for (i in 0..15) {
                val angle = i * Math.PI / 8
                val latNoise = (cos(angle) * 0.0003)
                val lngNoise = (sin(angle) * 0.0003)
                append("${home.first + latNoise},${home.second + lngNoise};")
            }
        }.toString()

        val log = FlightLogEntity(
            droneId = "TACTICAL_DRONE_S4",
            timestamp = System.currentTimeMillis(),
            durationSeconds = state.flightTimeSeconds,
            maxAltitude = state.altitude + 5f,
            maxSpeed = state.groundSpeed + 1.2f,
            waypointCount = _plannedWaypoints.value.size,
            startLatitude = state.latitude,
            startLongitude = state.longitude,
            flightPathCoordinates = coordinatesMock,
            completionStatus = if (state.batteryPercentage < 15) "AUTO_RTH_LOW_BATTERY" else "COMPLETED_NORMAL",
            batteryDischargeValue = 25.2f - state.batteryVoltage,
            telemetryPacketCount = (state.flightTimeSeconds * 12).toLong(),
            analyticsSummary = "Flight debrief on $dateString. Dynamic power envelope successfully constrained. No abnormal hardware thermal trends detected down-link."
        )

        viewModelScope.launch {
            repository.saveFlightLog(log)
        }
    }
}

data class AiDetection(
    val id: String,
    val label: String,
    val confidence: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
