package com.example.data.service

import com.example.data.models.ConnectionStatus
import com.example.data.models.FlightMode
import com.example.data.models.HealthStatus
import com.example.data.models.SensorHealth
import com.example.data.models.TelemetryState
import com.example.data.models.Waypoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class DroneSimEngine(private val appScope: CoroutineScope) {

    private val _telemetryState = MutableStateFlow(TelemetryState())
    val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    private val _sensorsHealth = MutableStateFlow<List<SensorHealth>>(emptyList())
    val sensorsHealth: StateFlow<List<SensorHealth>> = _sensorsHealth.asStateFlow()

    private var simJob: Job? = null
    private var missionJob: Job? = null

    // Reference home coordinate (tactical coordinates in SF or default sandbox field)
    private var homeLat = 37.7749
    private var homeLng = -122.4194

    private var curLat = homeLat
    private var curLng = homeLng
    private var curAlt = 0f

    // List of active mission waypoints
    private val activeWaypoints = mutableListOf<Waypoint>()
    private var currentWaypointIndex = 0

    init {
        resetSensors()
    }

    private fun resetSensors() {
        _sensorsHealth.value = listOf(
            SensorHealth("GPS_NEO_M9N", HealthStatus.OPERATIONAL, 10f, 0.05f),
            SensorHealth("IMU_ICM_20602", HealthStatus.OPERATIONAL, 50f, 0.01f),
            SensorHealth("COMPASS_HMC5883L", HealthStatus.OPERATIONAL, 20f, 0.08f),
            SensorHealth("FMU_STM32F4", HealthStatus.OPERATIONAL, 100f, 0.02f),
            SensorHealth("LIDAR_TFMINI", HealthStatus.OPERATIONAL, 40f, 0.12f),
            SensorHealth("ESC_TELEMETRY", HealthStatus.OPERATIONAL, 25f, 0.15f),
            SensorHealth("FPV_H265_TX", HealthStatus.OPERATIONAL, 30f, 0.51f)
        )
    }

    fun setHomeLocation(lat: Double, lng: Double) {
        homeLat = lat
        homeLng = lng
        if (_telemetryState.value.connectionStatus == ConnectionStatus.DISCONNECTED) {
            curLat = lat
            curLng = lng
        }
    }

    fun connect() {
        if (_telemetryState.value.connectionStatus == ConnectionStatus.CONNECTED) return

        _telemetryState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING) }
        logMessage("SYS: Connecting to Flight Controller UART/UDP stream...")

        simJob = appScope.launch(Dispatchers.Default) {
            delay(1200) // connection link settling
            _telemetryState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    signalStrength = 98,
                    batteryVoltage = 25.2f, // Fully charged 6S LiPo (4.2V * 6)
                    batteryPercentage = 100,
                    batteryHealth = "EXCELLENT",
                    satCount = 14,
                    gpsStatus = "3D_FIX_GPS",
                    latitude = curLat,
                    longitude = curLng,
                    cpuTemp = 42.5f,
                    escTemp = 36.8f,
                    flightMode = FlightMode.STABILIZE
                )
            }
            logMessage("MAV: Connected to ArduPilot Hawk V6 (PX4 Compatible). Protocol standard MAVLink v2.0")
            logMessage("SYS: All hardware nodes reporting OPERATIONAL.")

            // Start cyclic simulation loop (high rate update - ~80ms intervals)
            var counter = 0
            while (true) {
                delay(80)
                counter++
                updateTelemetryStep(counter)
            }
        }
    }

    fun disconnect() {
        simJob?.cancel()
        simJob = null
        stopActiveMission()
        _telemetryState.update {
            TelemetryState(connectionStatus = ConnectionStatus.DISCONNECTED)
        }
        logMessage("SYS: Closed ground station control links. Link OFFLINE.")
    }

    fun setFlightMode(mode: FlightMode) {
        if (_telemetryState.value.connectionStatus != ConnectionStatus.CONNECTED) return
        _telemetryState.update { it.copy(flightMode = mode) }
        logMessage("CMD: Switched flight control mode to -> $mode")

        if (mode == FlightMode.LAND) {
            triggerLandingSequence()
        } else if (mode == FlightMode.RTH) {
            triggerReturnToHome()
        } else if (mode != FlightMode.AUTO_PORT) {
            stopActiveMission()
        }
    }

    fun triggerEmergencyDisarm() {
        if (_telemetryState.value.connectionStatus != ConnectionStatus.CONNECTED) return
        _telemetryState.update {
            it.copy(
                flightMode = FlightMode.LAND,
                isEmergencyActionActive = true,
                motorRpm = 0,
                airspeed = 0f,
                verticalSpeed = -15f // heavy drop
            )
        }
        logMessage("🔴 CRITICAL: EMERGENCY TERMINATION TRIPPED! DISARMING MOTORS DIRECTLY!")
    }

    fun clearLog() {
        _telemetryState.update { it.copy(messageLog = emptyList()) }
    }

    // Command specific action triggers
    fun loadAndStartMission(waypoints: List<Waypoint>) {
        if (_telemetryState.value.connectionStatus != ConnectionStatus.CONNECTED) return
        if (waypoints.isEmpty()) {
            logMessage("SYS-ERR: Cannot launch. Zero waypoints loaded.")
            return
        }

        activeWaypoints.clear()
        activeWaypoints.addAll(waypoints)
        currentWaypointIndex = 0

        _telemetryState.update { it.copy(flightMode = FlightMode.AUTO_PORT) }
        logMessage("AUTO: Pre-flight check successful... Mission Plan contains ${waypoints.size} waypoints.")
        logMessage("AUTO: Launching autonomous route execution.")

        missionJob?.cancel()
        missionJob = appScope.launch(Dispatchers.Default) {
            curLat = _telemetryState.value.latitude
            curLng = _telemetryState.value.longitude
            curAlt = _telemetryState.value.altitude

            for (i in waypoints.indices) {
                currentWaypointIndex = i
                val wp = waypoints[i]
                logMessage("AUTO: Navigating to Waypoint ${wp.id} (Alt: ${wp.altitude}m, Speed: ${wp.speed}m/s)")

                // Simulate motion towards waypoint
                val targetLat = wp.latitude
                val targetLng = wp.longitude
                val targetAlt = wp.altitude

                var distance = getDistanceMeters(curLat, curLng, targetLat, targetLng)
                while (distance > 2.0 && _telemetryState.value.flightMode == FlightMode.AUTO_PORT) {
                    val bearing = getBearingDegrees(curLat, curLng, targetLat, targetLng)
                    
                    // Tilt drone forward according to travel heading
                    val calculatedPitch = 15f + Random.nextFloat() * 2f // Tilt forward
                    val calculatedRoll = (Random.nextFloat() * 4f) - 2f // Slight stabilization tilt

                    // Move coordinate slightly
                    val speedScale = wp.speed * 0.1 // meters per step (100ms interval)
                    val dLat = (speedScale * cos(bearing * PI / 180.0)) / 111111.0
                    val dLng = (speedScale * sin(bearing * PI / 180.0)) / (111111.0 * cos(curLat * PI / 180.0))

                    curLat += dLat
                    curLng += dLng

                    // Climb or descend towards target altitude
                    if (curAlt < targetAlt) curAlt += 1.5f
                    else if (curAlt > targetAlt) curAlt -= 1.5f

                    delay(100)
                    distance = getDistanceMeters(curLat, curLng, targetLat, targetLng)
                }

                if (_telemetryState.value.flightMode != FlightMode.AUTO_PORT) break

                // Arrived at waypoint. Perform hover actions.
                logMessage("AUTO: Arrived at WP ${wp.id}. Commencing hover routine (${wp.hoverTime}s).")
                _telemetryState.update { it.copy(pitch = 0f, roll = 0f, airspeed = 0f) }
                delay(wp.hoverTime * 1000L)
            }

            if (_telemetryState.value.flightMode == FlightMode.AUTO_PORT) {
                logMessage("AUTO: All waypoint targets reached. Engaging Return-To-Home sequence.")
                triggerReturnToHome()
            }
        }
    }

    private fun stopActiveMission() {
        missionJob?.cancel()
        missionJob = null
    }

    private fun triggerLandingSequence() {
        stopActiveMission()
        logMessage("LAND: Auto-landing activated. Descending vertically...")
        missionJob = appScope.launch(Dispatchers.Default) {
            while (curAlt > 0.2f && _telemetryState.value.flightMode == FlightMode.LAND) {
                curAlt -= 0.8f
                if (curAlt < 0f) curAlt = 0f
                _telemetryState.update {
                    it.copy(
                        airspeed = 0.5f,
                        verticalSpeed = -1.2f,
                        pitch = 1f,
                        roll = -0.5f
                    )
                }
                delay(150)
            }
            if (_telemetryState.value.flightMode == FlightMode.LAND) {
                curAlt = 0f
                _telemetryState.update {
                    it.copy(
                        altitude = 0f,
                        airspeed = 0f,
                        verticalSpeed = 0f,
                        motorRpm = 0,
                        pitch = 0f,
                        roll = 0f
                    )
                }
                logMessage("LAND: Touchdown detected. Motors disarmed. Flight finished successfully.")
                setFlightMode(FlightMode.STABILIZE)
            }
        }
    }

    private fun triggerReturnToHome() {
        stopActiveMission()
        _telemetryState.update { it.copy(flightMode = FlightMode.RTH) }
        logMessage("RTH: Commencing Return-To-Home. Reversing path back to home coordinates.")
        missionJob = appScope.launch(Dispatchers.Default) {
            val startLat = curLat
            val startLng = curLng
            
            // Climb to safe return altitude (e.g. 40m) if below it
            if (curAlt < 40f) {
                logMessage("RTH: Climbing to safe transit altitude (40.0m)...")
                while (curAlt < 40f && _telemetryState.value.flightMode == FlightMode.RTH) {
                    curAlt += 1.8f
                    delay(100)
                }
            }

            var distance = getDistanceMeters(curLat, curLng, homeLat, homeLng)
            while (distance > 2.0 && _telemetryState.value.flightMode == FlightMode.RTH) {
                val bearing = getBearingDegrees(curLat, curLng, homeLat, homeLng)
                val calculatedPitch = 12f // speed tilt
                val speedScale = 8f * 0.1 // returning at solid 8m/s speed
                
                val dLat = (speedScale * cos(bearing * PI / 180.0)) / 111111.0
                val dLng = (speedScale * sin(bearing * PI / 180.0)) / (111111.0 * cos(curLat * PI / 180.0))

                curLat += dLat
                curLng += dLng

                delay(100)
                distance = getDistanceMeters(curLat, curLng, homeLat, homeLng)
            }

            if (_telemetryState.value.flightMode == FlightMode.RTH) {
                logMessage("RTH: Arrived in home corridor. Initiating auto-landing...")
                triggerLandingSequence()
            }
        }
    }

    private fun updateTelemetryStep(step: Int) {
        val state = _telemetryState.value
        if (state.connectionStatus != ConnectionStatus.CONNECTED) return

        // Basic physics / battery discharge logic
        // Discharging over time (fully loaded drone consumes battery)
        val currentFactor = if (state.flightMode == FlightMode.AUTO_PORT || state.flightMode == FlightMode.RTH) 4500 else 1800
        val isInMotion = state.flightMode != FlightMode.STABILIZE && curAlt > 0.5f

        val totalFlightTime = if (curAlt > 0.1f) state.flightTimeSeconds + 1 else state.flightTimeSeconds
        val percentDrop = if (step % 40 == 0 && curAlt > 0.1f) 1 else 0
        val nextPercentage = (state.batteryPercentage - percentDrop).coerceAtLeast(0)
        
        // Match LiPo voltage roughly to cell capacity curve
        // 6S fully charged: 25.2V. 0% capacity: ~20.0V
        val nextVoltage = 20.0f + (nextPercentage / 100f * 5.2f)

        // Low Battery safety alarm
        var isEmergencyTriggered = state.isEmergencyActionActive
        var nextMode = state.flightMode
        if (nextPercentage < 15 && nextPercentage > 0 && state.flightMode != FlightMode.LAND && state.flightMode != FlightMode.RTH && curAlt > 1f) {
            logMessage("🔴 WARN: BATTERY DESTRUCTIVE THRESHOLD! percentage: $nextPercentage%, Auto return-to-home engaged.")
            triggerReturnToHome()
            nextMode = FlightMode.RTH
        }

        // CPU / Module Temperature Fluctuations
        val windSpeedVal = 12f + (sin(step / 10.0) * 4f).toFloat() // active wind gust simulation
        val tempDiff = if (isInMotion) 0.1f else 0.2f
        val calculatedCpuTemp = (state.cpuTemp + (Random.nextFloat() * tempDiff - (tempDiff / 2.1f))).coerceIn(38f, 65f)
        val calculatedEscTemp = (state.escTemp + (Random.nextFloat() * 0.15f - 0.07f)).coerceIn(34f, 52f)

        // Simulated ESC Motor RPM telemetry
        val baseRpm = if (curAlt > 0.2f) 6200 else 0
        val variance = if (curAlt > 0.2f) Random.nextInt(150) - 75 else 0
        val activeRpm = baseRpm + variance + (if (state.flightMode == FlightMode.AUTO_PORT) 800 else 0)

        // GPS Satellite quality drops randomly or stays solid
        val satVariance = if (step % 50 == 0) Random.nextInt(3) - 1 else 0
        val calculatedSats = (state.satCount + satVariance).coerceIn(8, 19)

        // Telemetry radio statistics
        val calculatedPacketLoss = (0.002f + (Random.nextFloat() * 0.005f)).coerceIn(0f, 1f)
        val calculatedLatency = 20 + Random.nextInt(25) // 20-45ms updates latency
        val signalQual = if (state.distanceFromHome > 1500f) (98 - (state.distanceFromHome / 80).toInt()).coerceAtLeast(10) else 98

        // Pitch and Roll variations when flying
        var pitVal = state.pitch
        var rolVal = state.roll
        var yawVal = state.yaw

        if (state.flightMode == FlightMode.STABILIZE) {
            // Level out slowly
            pitVal = (pitVal * 0.8f) + ((Random.nextFloat() * 2f - 1f) * 0.2f)
            rolVal = (rolVal * 0.8f) + ((Random.nextFloat() * 2f - 1f) * 0.2f)
            // Idle hover altitude is 0 if drone hasn't taken off
            if (curAlt > 0.1f) {
                // Keep altitude stable
                val gustForce = sin(step / 3.0).toFloat() * 0.2f
                curAlt += gustForce
            }
        } else if (state.flightMode == FlightMode.MANUAL) {
            // Random controller inputs drift
            pitVal = pitVal + (Random.nextFloat() * 4f - 2f)
            rolVal = rolVal + (Random.nextFloat() * 4f - 2f)
        }

        yawVal = (yawVal + 0.1f) % 360f

        // Distance relative to launch point
        val distanceCalc = getDistanceMeters(curLat, curLng, homeLat, homeLng).toFloat()

        // Gyro check for abnormal high wind turbulence
        if (windSpeedVal > 18f && step % 40 == 0 && curAlt > 1f) {
            logMessage("⚠️ PILOT WARN: HIGHER TRANSIENT GUSTS DETECTED ($windSpeedVal KPH). Stabilizing flight envelope.")
        }

        // Dynamic LiDAR obstacle detection simulator:
        // Nearest physical obstacle decays as drone height drops or goes into a "landing footprint"
        val obstacleDistance = if (curAlt in 0.1f..5f) {
            curAlt // straight down to ground
        } else {
            (50f + sin(step / 8.0).toFloat() * 12f).coerceAtLeast(3f)
        }

        // Auto landing checking
        if (state.flightMode == FlightMode.LAND && curAlt < 0.1f) {
            curAlt = 0f
        }

        val air = if (curAlt > 0.1f) {
            if (state.flightMode == FlightMode.AUTO_PORT) 8.5f else 4.2f
        } else 0f

        val verticalSpeedClimb = if (isInMotion) {
            if (state.flightMode == FlightMode.LAND) -1.2f else if (state.flightMode == FlightMode.RTH) 0.5f else 1.2f
        } else 0f

        _telemetryState.update {
            it.copy(
                flightTimeSeconds = totalFlightTime,
                batteryVoltage = nextVoltage,
                batteryPercentage = nextPercentage,
                cpuTemp = calculatedCpuTemp,
                escTemp = calculatedEscTemp,
                motorRpm = activeRpm,
                satCount = calculatedSats,
                packetLoss = calculatedPacketLoss,
                latencyMs = calculatedLatency,
                signalStrength = signalQual,
                altitude = curAlt,
                latitude = curLat,
                longitude = curLng,
                distanceFromHome = distanceCalc,
                pitch = pitVal,
                roll = rolVal,
                yaw = yawVal,
                airspeed = air,
                groundSpeed = air * 1.1f, // simple wind boost
                verticalSpeed = verticalSpeedClimb,
                obstacleDistanceM = obstacleDistance,
                windSpeedKph = windSpeedVal,
                heading = yawVal.toInt(),
                flightMode = nextMode
            )
        }
    }

    private fun logMessage(msg: String) {
        _telemetryState.update {
            val nextList = it.messageLog.toMutableList()
            if (nextList.size > 80) nextList.removeAt(0)
            nextList.add("[${System.currentTimeMillis() % 1000000 / 1000}] $msg")
            it.copy(messageLog = nextList)
        }
    }

    // Helper utilities for calculating coordinate math
    private fun getDistanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLng = (lng2 - lng1) * PI / 180.0
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun getBearingDegrees(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLng = (lng2 - lng1) * PI / 180.0
        val lat1Rad = lat1 * PI / 180.0
        val lat2Rad = lat2 * PI / 180.0
        val y = sin(dLng) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLng)
        val bearing = atan2(y, x) * 180.0 / PI
        return (bearing + 360.0) % 360.0
    }
}

// Float custom range helper
fun ClosedRange<Int>.coerceAtIn(min: Int, max: Int): Int {
    return this.start.coerceAtLeast(min).coerceAtMost(max)
}
