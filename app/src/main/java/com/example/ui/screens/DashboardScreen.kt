package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OnlinePrediction
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ConnectionStatus
import com.example.data.models.FlightMode
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceVariant
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateCyanContainer
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DroneViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DroneViewModel,
    onNavigateToFpv: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val telemetry by viewModel.telemetryState.collectAsState()
    val altHistory by viewModel.altitudeHistory.collectAsState()
    val voltHistory by viewModel.voltageHistory.collectAsState()
    val copilotAdvice by viewModel.copilotAdvice.collectAsState()
    val isCopilotLoading by viewModel.isCopilotLoading.collectAsState()

    var showEmergencyConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        bottomBar = {
            TacticalBottomNavBar(
                currentRoute = "dashboard",
                onNavigateToDashboard = {},
                onNavigateToFpv = onNavigateToFpv,
                onNavigateToPlanner = onNavigateToPlanner,
                onNavigateToDiagnostics = onNavigateToDiagnostics,
                onNavigateToHistory = onNavigateToHistory
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. TOP QUICK METRICS BAR (AVIONICS STATUS BANNER)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flight Mode Display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(TelemetryGreen, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODE: ${telemetry.flightMode}",
                            color = TelemetryGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("dashboard_flight_mode")
                        )
                    }

                    // Battery Summary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${telemetry.batteryPercentage}% (${"%.1f".format(telemetry.batteryVoltage)}V)",
                            color = if (telemetry.batteryPercentage < 25) AlertCrimson else Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = if (telemetry.batteryPercentage < 25) AlertCrimson else CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Sat Count GPS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = null,
                            tint = TelemetryGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SATS: ${telemetry.satCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. MAIN COCKPIT: ARTIFICIAL HORIZON & CORE GYRO PANEL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gyro Artificial Horizon
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AVIONICS GYROSCOPE HUD",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        // The Interactive Horizon Canvas
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AvionicsArtificialHorizon(
                                pitch = telemetry.pitch,
                                roll = telemetry.roll,
                                modifier = Modifier.size(120.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text("PITCH: ${"%.1f".format(telemetry.pitch)}°", color = CyberCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("ROLL: ${"%.1f".format(telemetry.roll)}°", color = CyberCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Core Telemetry numeric logs
                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .fillMaxHeight()
                        .border(1.dp, CyberCyan.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TELEMETRY", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        
                        TelemetryValueItem(label = "ALTITUDE", value = "${"%.1f".format(telemetry.altitude)} m")
                        TelemetryValueItem(label = "SPD(AIR)", value = "${"%.1f".format(telemetry.airspeed)} m/s")
                        TelemetryValueItem(label = "CLIMB", value = "${"%.1f".format(telemetry.verticalSpeed)} m/s")
                        TelemetryValueItem(label = "HEADING", value = "${telemetry.heading}° N")
                        TelemetryValueItem(label = "DIST.HOME", value = "${"%.0f".format(telemetry.distanceFromHome)} m")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. ROLLING OSCILLOSCOPE GRAPHS (ALTITUDE & BATTERY STATUS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Altitude Vector Canvas
                VectorOscilloscope(
                    title = "ALTITUDE TRACKER (m)",
                    history = altHistory,
                    peakValue = 60f,
                    lineColor = CyberCyan,
                    modifier = Modifier.weight(1f)
                )

                // Battery Voltage Vector Canvas
                VectorOscilloscope(
                    title = "CELL PACK VOLTAGE (V)",
                    history = voltHistory,
                    peakValue = 26f,
                    lineColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. GEMINI GCS CO-PILOT ASSISTANT (INTELLIGENT FLIGHT BRIEF)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SlateCyanContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OnlinePrediction,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GEMINI FLIGHT CO-PILOT",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.fetchGeminiDiagnosticAdvisory() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("request_brief_button"),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isCopilotLoading) "QUERYING..." else "REQUEST BRIEFING",
                                color = ObsidianBackground,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = copilotAdvice,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. MASTER CONTROLLER BUTTON PANEL (FLIGHT STATE SELECTIONS)
            Text(
                text = "AVIONICS FLIGHT CONTROLLER EXECUTION",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlightCommandPill(
                    label = "STABILIZE",
                    isActive = telemetry.flightMode == FlightMode.STABILIZE,
                    onClick = { viewModel.setFlightMode(FlightMode.STABILIZE) }
                )
                FlightCommandPill(
                    label = "ALT HOLD",
                    isActive = telemetry.flightMode == FlightMode.ALT_HOLD,
                    onClick = { viewModel.setFlightMode(FlightMode.ALT_HOLD) }
                )
                FlightCommandPill(
                    label = "MANUAL",
                    isActive = telemetry.flightMode == FlightMode.MANUAL,
                    onClick = { viewModel.setFlightMode(FlightMode.MANUAL) }
                )
                FlightCommandPill(
                    label = "AUTO RET.HOME",
                    isActive = telemetry.flightMode == FlightMode.RTH,
                    onClick = { viewModel.setFlightMode(FlightMode.RTH) }
                )
                FlightCommandPill(
                    label = "AUTO D-LAND",
                    isActive = telemetry.flightMode == FlightMode.LAND,
                    onClick = { viewModel.setFlightMode(FlightMode.LAND) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. EMERGENCY ARMED BUTTONS (EMERGENCY DISARM)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AlertCrimson.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassSurface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!showEmergencyConfirm) {
                        Button(
                            onClick = { showEmergencyConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("emergency_land_panic"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Emergency, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MASTER COCKPIT PANIC / ARMED TIMEOUT",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        // Confirm Emergency Dialog Inside Dashboard
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "⚠️ CONFIRM ENGINE KILL?",
                                color = AlertCrimson,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "DRONE WILL INSTANTLY DISARM MOTORS AND DROP VERTICALLY!",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.triggerEmergencyDisarm()
                                        showEmergencyConfirm = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("confirm_kill_btn")
                                ) {
                                    Text("CONFIRM KILL", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = { showEmergencyConfirm = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text("CANCEL", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun TelemetryValueItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FlightCommandPill(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isActive) CyberCyan.copy(alpha = 0.2f) else GlassSurface,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (isActive) CyberCyan else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("command_pill_${label.lowercase().replace(" ", "_")}")
    ) {
        Text(
            text = label,
            color = if (isActive) CyberCyan else Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

// ----------------------------------------------------
// VECTOR-BASED OSCILLOSCOPE GRAPH CANVAS
// ----------------------------------------------------
@Composable
fun VectorOscilloscope(
    title: String,
    history: List<Float>,
    peakValue: Float,
    lineColor: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .border(1.dp, lineColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = lineColor,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw reference grid lines
                    drawLine(color = Color.White.copy(alpha = 0.04f), start = Offset(0f, h * 0.25f), end = Offset(w, h * 0.25f))
                    drawLine(color = Color.White.copy(alpha = 0.04f), start = Offset(0f, h * 0.5f), end = Offset(w, h * 0.5f))
                    drawLine(color = Color.White.copy(alpha = 0.04f), start = Offset(0f, h * 0.75f), end = Offset(w, h * 0.75f))

                    if (history.size > 1) {
                        val path = Path()
                        val stepX = w / 40f // up to 40 historical points

                        history.forEachIndexed { i, value ->
                            // Normalize coordinate y relative to graph scale peak value
                            val normY = (h - (value / peakValue * h)).coerceIn(0f, h)
                            val coordinateX = i * stepX

                            if (i == 0) {
                                path.moveTo(coordinateX, normY)
                            } else {
                                path.lineTo(coordinateX, normY)
                            }
                        }

                        // Draw the glowing vector line sweep
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            Text(
                text = "REALTIME ACTIVE FEED",
                color = Color.Gray,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ----------------------------------------------------
// ARTIFICIAL HUD HORIZON METER
// ----------------------------------------------------
@Composable
fun AvionicsArtificialHorizon(
    pitch: Float,
    roll: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Clip the drawings in a perfect circle frame
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = r
        )

        // Draw sky sky-blue vs land brown split shifted based on pitch and rolled!
        withTransform({
            rotate(roll, center)
            translate(0f, pitch * 1.5f) // pitch determines vertical offset shift
        }) {
            // Sky area
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF003C5C), Color(0xFF0074B7))
                ),
                topLeft = Offset(-r * 2, -r * 2),
                size = androidx.compose.ui.geometry.Size(r * 4, r * 2)
            )

            // Ground area
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF5E3F27), Color(0xFF331E12))
                ),
                topLeft = Offset(-r * 2, 0f),
                size = androidx.compose.ui.geometry.Size(r * 4, r * 2)
            )

            // Horizontal line separating Sky and Land
            drawLine(
                color = Color.White,
                start = Offset(-r, 0f),
                end = Offset(r, 0f),
                strokeWidth = 3f
            )

            // Pitch ladder indicators (10°, 20° labels markings)
            for (p in listOf(-20, -10, 10, 20)) {
                val yOffset = -p * 1.5f
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(-r * 0.3f, yOffset),
                    end = Offset(r * 0.3f, yOffset),
                    strokeWidth = 1.5f
                )
            }
        }

        // Draw static aircraft reticle directly over the rotating HUD
        drawCircle(
            color = CyberCyan,
            radius = 3.dp.toPx()
        )
        drawLine(
            color = CyberCyan,
            start = Offset(center.x - r * 0.4f, center.y),
            end = Offset(center.x - r * 0.1f, center.y),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = CyberCyan,
            start = Offset(center.x + r * 0.1f, center.y),
            end = Offset(center.x + r * 0.4f, center.y),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = CyberCyan,
            start = Offset(center.x, center.y),
            end = Offset(center.x, center.y + 10.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        // Outermost outer ring frame border
        drawCircle(
            color = CyberCyan.copy(alpha = 0.6f),
            radius = r,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ----------------------------------------------------
// HIGH TECH BOTTOM TACTICAL NAVIGATION BAR
// ----------------------------------------------------
@Composable
fun TacticalBottomNavBar(
    currentRoute: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToFpv: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(label = "HUD", icon = Icons.Default.Flight, isActive = currentRoute == "dashboard", onClick = onNavigateToDashboard)
            NavItem(label = "FPV", icon = Icons.Default.Radar, isActive = currentRoute == "fpv", onClick = onNavigateToFpv)
            NavItem(label = "NAV", icon = Icons.Default.Terrain, isActive = currentRoute == "planner", onClick = onNavigateToPlanner)
            NavItem(label = "SYS", icon = Icons.Default.Sensors, isActive = currentRoute == "diagnostics", onClick = onNavigateToDiagnostics)
            NavItem(label = "LOGS", icon = Icons.Default.Info, isActive = currentRoute == "history", onClick = onNavigateToHistory)
        }
    }
}

@Composable
fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 10.dp)
            .testTag("nav_item_${label.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) CyberCyan else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isActive) CyberCyan else Color.Gray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
