package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ConnectionStatus
import com.example.data.models.FlightMode
import com.example.data.models.Waypoint
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceVariant
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DroneViewModel

@Composable
fun MissionPlannerScreen(
    viewModel: DroneViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToFpv: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val telemetry by viewModel.telemetryState.collectAsState()
    val waypoints by viewModel.plannedWaypoints.collectAsState()
    val selectedWpId by viewModel.selectedWaypointId.collectAsState()
    val savedPlans by viewModel.savedMissions.collectAsState()

    var showSaveDetailsDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("SCAN_ALPHA_1") }
    var saveNotes by remember { mutableStateOf("Autopilot grid mapping area 4") }

    val selectedWp = waypoints.find { it.id == selectedWpId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        bottomBar = {
            TacticalBottomNavBar(
                currentRoute = "planner",
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToFpv = onNavigateToFpv,
                onNavigateToPlanner = {},
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
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. TACTICAL GRID VECTOR PLANE (CANVAS INTERFACES)
            Text(
                text = "TACTICAL NAV DESK GRID",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(GlassSurface)
                    .clipRadius(12)
                    .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Scale tap pixels to mock degrees coordinates around center lat/lng
                            // Center: (w/2, h/2) maps to (37.7749, -122.4194)
                            val latRange = 0.005
                            val lngRange = 0.005
                            val normX = (offset.x / size.width) - 0.5
                            val normY = 0.5 - (offset.y / size.height) // positive up

                            val calculatedLng = -122.4194 + (normX * lngRange)
                            val calculatedLat = 37.7749 + (normY * latRange)

                            viewModel.addPlannedWaypoint(calculatedLat, calculatedLng)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2, h / 2)

                    // Draw reference scale concentric target rings (50m, 100m steps)
                    drawCircle(color = CyberCyan.copy(alpha = 0.05f), radius = w * 0.15f, style = Stroke(width = 1f))
                    drawCircle(color = CyberCyan.copy(alpha = 0.08f), radius = w * 0.3f, style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                    drawCircle(color = CyberCyan.copy(alpha = 0.12f), radius = w * 0.45f, style = Stroke(width = 1f))

                    // Tactical Radar Sweeping Crosshairs
                    drawLine(color = CyberCyan.copy(alpha = 0.15f), start = Offset(0f, h / 2), end = Offset(w, h / 2))
                    drawLine(color = CyberCyan.copy(alpha = 0.15f), start = Offset(w / 2, 0f), end = Offset(w / 2, h))

                    // Plot and connect waypoints
                    var priorPoint: Offset? = null
                    waypoints.forEachIndexed { idx, wp ->
                        // Translate coordinate bounds back to canvas coordinates
                        val normLng = (wp.longitude - (-122.4194)) / 0.005
                        val normLat = (wp.latitude - 37.7749) / 0.005
                        val x = (center.x + (normLng * w)).toFloat()
                        val y = (center.y - (normLat * h)).toFloat()
                        val currentPoint = Offset(x, y)

                        // Connecting transits line
                        if (priorPoint != null) {
                            drawLine(
                                color = CyberCyan.copy(alpha = 0.8f),
                                start = priorPoint!!,
                                end = currentPoint,
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        }

                        // Waypoint point node
                        val isSelected = wp.id == selectedWpId
                        drawCircle(
                            color = if (isSelected) WarningAmber else CyberCyan,
                            radius = if (isSelected) 8.dp.toPx() else 6.dp.toPx()
                        )
                        drawCircle(
                            color = if (isSelected) WarningAmber.copy(alpha = 0.3f) else CyberCyan.copy(alpha = 0.3f),
                            radius = if (isSelected) 14.dp.toPx() else 10.dp.toPx()
                        )

                        priorPoint = currentPoint
                    }

                    // Plot active drone location
                    if (telemetry.connectionStatus == ConnectionStatus.CONNECTED) {
                        val normLng = (telemetry.longitude - (-122.4194)) / 0.005
                        val normLat = (telemetry.latitude - 37.7749) / 0.005
                        val droneX = (center.x + (normLng * w)).toFloat()
                        val droneY = (center.y - (normLat * h)).toFloat()
                        
                        // Icon node for telemetry drone target
                        drawCircle(color = TelemetryGreen, radius = 9.dp.toPx())
                        drawCircle(color = TelemetryGreen.copy(alpha = 0.3f), radius = 16.dp.toPx(), style = Stroke(width = 2f))
                    }
                }

                // HUD overlay message
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text("GRID SPAN: 500m x 500m", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("TAP GRID TO SEQUENCE WAYPOINTS", color = CyberCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. SELECTED WAYPOINT PARAMETER CONTROL PANEL
            if (selectedWp != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarningAmber.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WAYPOINT NODE #${selectedWp.id} PARAMETERS",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { viewModel.deleteWaypoint(selectedWp.id) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("delete_wp_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertCrimson, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cruise Altitude slider (meters)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ALTITUDE: ${selectedWp.altitude.toInt()}m", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(90.dp))
                            Slider(
                                value = selectedWp.altitude,
                                onValueChange = { viewModel.updateSelectedWaypointDetails(it, selectedWp.speed, selectedWp.hoverTime) },
                                valueRange = 10f..100f,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wp_alt_slider"),
                                colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = WarningAmber)
                            )
                        }

                        // Transit velocity slider (m/s)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("VELOCITY: ${selectedWp.speed.toInt()} m/s", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(90.dp))
                            Slider(
                                value = selectedWp.speed,
                                onValueChange = { viewModel.updateSelectedWaypointDetails(selectedWp.altitude, it, selectedWp.hoverTime) },
                                valueRange = 2f..15f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = WarningAmber)
                            )
                        }
                    }
                }
            } else {
                // Hint display if empty list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(GlassSurface, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "NO NODE IN FOCUS. TAP GRAPHICS DESK ABOVE TO GENERATE ROUTE.",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. MASTER COMMAND UPLOADER ACTIONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.clearAllPlannedWaypoints() },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("clear_waypoints_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CLEAR ALL", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { showSaveDetailsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE M-PLAN", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Button(
                    onClick = { viewModel.uploadAndLaunchPlan() },
                    colors = ButtonDefaults.buttonColors(containerColor = TelemetryGreen),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp)
                        .testTag("upload_launch_plan_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ObsidianBackground, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LAUNCH AUTO", color = ObsidianBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save details inline field editor mockup (since dialog blocks easily, inline is brilliant!)
            if (showSaveDetailsDialog) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("SAVE WAYPOINT PLAN", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.saveCurrentPlanToDatabase(saveName, saveNotes)
                                    showSaveDetailsDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TelemetryGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .testTag("submit_save_mission_plan")
                            ) {
                                Text("COMMIT PLAN", color = ObsidianBackground, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showSaveDetailsDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                            ) {
                                Text("CANCEL", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. LOAD COGNITIVE PLANS SAVED IN DATABASE
            Text(
                text = "SAVED AD-HOC MISSION RECORDS",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (savedPlans.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("DATABASE EMPTY: NO SAVED WAYPOINT PLANS FOUND", color = Color.White.copy(alpha = 0.2f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedPlans) { plan ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GlassSurface, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.loadSelectedSavedMission(plan.serializedWaypoints) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(plan.missionName, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text("Nodes: ${plan.waypointCount} | Mean Height: ${plan.averageAltitude.toInt()}m", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = "Load", tint = CyberCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteSavedMission(plan.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Save", tint = AlertCrimson.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension to clip custom elements with round corners safely in Compose Canvas/Box environments
fun Modifier.clipRadius(radiusDp: Int) = this.then(
    Modifier.background(Color.Transparent, RoundedCornerShape(radiusDp.dp))
)
