package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceVariant
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.WarningAmber
import com.example.data.models.ConnectionStatus
import com.example.ui.viewmodel.DroneViewModel

@Composable
fun FpvScreen(
    viewModel: DroneViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val telemetry by viewModel.telemetryState.collectAsState()
    val isAiActive by viewModel.isAiObjectTrackingActive.collectAsState()
    val aiDetections by viewModel.aiDetectedObjects.collectAsState()

    var isRecording by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "RecPulsing")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        bottomBar = {
            TacticalBottomNavBar(
                currentRoute = "fpv",
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToFpv = {},
                onNavigateToPlanner = onNavigateToPlanner,
                onNavigateToDiagnostics = onNavigateToDiagnostics,
                onNavigateToHistory = onNavigateToHistory
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // FPV VIEWPORT LAYER - SIMULATED CORRIDOR VIDEO OR CAMERAX PREVIEW
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Vector HUD Camera Feed Simulator
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2, h / 2)

                    // Draw military crosshair grid
                    val gridAlpha = 0.15f
                    drawLine(color = CyberCyan.copy(alpha = gridAlpha), start = Offset(0f, h * 0.2f), end = Offset(w, h * 0.2f))
                    drawLine(color = CyberCyan.copy(alpha = gridAlpha), start = Offset(0f, h * 0.4f), end = Offset(w, h * 0.4f))
                    drawLine(color = CyberCyan.copy(alpha = gridAlpha), start = Offset(0f, h * 0.6f), end = Offset(w, h * 0.6f))
                    drawLine(color = CyberCyan.copy(alpha = gridAlpha), start = Offset(0f, h * 0.8f), end = Offset(w, h * 0.8f))

                    drawLine(color = CyberCyan.copy(alpha = gridAlpha), start = Offset(w * 0.2f, 0f), end = Offset(w * 0.2f, h))
                    drawLine(color = CyberCyan.copy(alpha = gridAlpha), start = Offset(w * 0.4f, 0f), end = Offset(w * 0.4f, h))
                    drawLine(color = CyberCyan.copy(alpha = gridAlpha), start = Offset(w * 0.6f, 0f), end = Offset(w * 0.6f, h))
                    drawLine(color = CyberCyan.copy(alpha = gridAlpha), start = Offset(w * 0.8f, 0f), end = Offset(w * 0.8f, h))

                    // Diagnostic center crosshair ring
                    drawCircle(
                        color = CyberCyan.copy(alpha = 0.3f),
                        radius = 45.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawLine(color = CyberCyan.copy(alpha = 0.5f), start = Offset(center.x - 15.dp.toPx(), center.y), end = Offset(center.x + 15.dp.toPx(), center.y))
                    drawLine(color = CyberCyan.copy(alpha = 0.5f), start = Offset(center.x, center.y - 15.dp.toPx()), end = Offset(center.x, center.y + 15.dp.toPx()))

                    // Outer tactical bounding frames
                    drawRect(
                        color = CyberCyan.copy(alpha = 0.15f),
                        topLeft = Offset(30.dp.toPx(), 30.dp.toPx()),
                        size = Size(w - 60.dp.toPx(), h - 60.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Moving drone scan lines representing gimbal calibration
                    val scanningY = (h * 0.3f) + (telemetry.pitch * 3f)
                    drawLine(
                        color = TelemetryGreen.copy(alpha = 0.35f),
                        start = Offset(40.dp.toPx(), scanningY),
                        end = Offset(w - 40.dp.toPx(), scanningY),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // AI CLASSIFICATION BOUNDING BOXES OVERLAY
                if (isAiActive && telemetry.connectionStatus == ConnectionStatus.CONNECTED) {
                    aiDetections.forEach { obj ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Translate normalized mockup boxes to viewport pixels safely
                                val boxLeft = obj.left
                                val boxTop = obj.top
                                val boxWidth = obj.right - obj.left
                                val boxHeight = obj.bottom - obj.top

                                // Draw bounding box frame (neon operational color)
                                drawRect(
                                    color = TelemetryGreen,
                                    topLeft = Offset(boxLeft, boxTop),
                                    size = Size(boxWidth, boxHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Draw corner bracket enhancements
                                val bracketSize = 12.dp.toPx()
                                drawLine(color = TelemetryGreen, start = Offset(boxLeft, boxTop), end = Offset(boxLeft + bracketSize, boxTop), strokeWidth = 4f)
                                drawLine(color = TelemetryGreen, start = Offset(boxLeft, boxTop), end = Offset(boxLeft, boxTop + bracketSize), strokeWidth = 4f)

                                drawLine(color = TelemetryGreen, start = Offset(boxLeft + boxWidth, boxTop), end = Offset(boxLeft + boxWidth - bracketSize, boxTop), strokeWidth = 4f)
                                drawLine(color = TelemetryGreen, start = Offset(boxLeft + boxWidth, boxTop), end = Offset(boxLeft + boxWidth, boxTop + bracketSize), strokeWidth = 4f)

                                drawLine(color = TelemetryGreen, start = Offset(boxLeft, boxTop + boxHeight), end = Offset(boxLeft + bracketSize, boxTop + boxHeight), strokeWidth = 4f)
                                drawLine(color = TelemetryGreen, start = Offset(boxLeft, boxTop + boxHeight), end = Offset(boxLeft, boxTop + boxHeight - bracketSize), strokeWidth = 4f)

                                drawLine(color = TelemetryGreen, start = Offset(boxLeft + boxWidth, boxTop + boxHeight), end = Offset(boxLeft + boxWidth - bracketSize, boxTop + boxHeight), strokeWidth = 4f)
                                drawLine(color = TelemetryGreen, start = Offset(boxLeft + boxWidth, boxTop + boxHeight), end = Offset(boxLeft + boxWidth, boxTop + boxHeight - bracketSize), strokeWidth = 4f)
                            }

                            // Dynamic box info tag
                            Column(
                                modifier = Modifier
                                    .padding(start = obj.left.dp + 4.dp, top = obj.top.dp - 18.dp)
                                    .background(TelemetryGreen, RoundedCornerShape(2.dp))
                                    .padding(vertical = 1.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "${obj.label} ${"%.0f".format(obj.confidence * 100)}%",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 2. CORNER OVERLAYS: TRANSMISSION TELEMETRY HUD
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Top Left: Recording Status
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecording) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = AlertCrimson,
                                modifier = Modifier
                                    .size(12.dp)
                                    .alpha(recAlpha)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REC 1080p",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "FEED LIVE h.265 (RTSP)",
                                color = TelemetryGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Top Right: FPS and Resolution metadata metrics
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("FPS: 60.2", color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        Text("3440 x 1440 ENCODED", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
                        Text("LATENCY: ${telemetry.latencyMs}ms", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
                    }

                    // Bottom Left: Yaw, compass & gimbal pitch state
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("GIMBAL STATE:", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("ROLL: ${"%.1f".format(telemetry.roll)}°", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("PITCH: ${"%.1f".format(telemetry.pitch)}°", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("YAW: ${"%.1f".format(telemetry.yaw)}°", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    // Bottom Right: GPS Coords
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("TACTICAL GPS LOC", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("LAT: ${"%.6f".format(telemetry.latitude)}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("LNG: ${"%.6f".format(telemetry.longitude)}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("ALT REL: ${"%.1f".format(telemetry.altitude)}m", color = TelemetryGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    // Center Top: YOLOv8 AI Warning Badge
                    AnimatedVisibility(
                        visible = isAiActive && telemetry.obstacleDistanceM < 15f && telemetry.connectionStatus == ConnectionStatus.CONNECTED,
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .background(AlertCrimson.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .border(1.dp, AlertCrimson, RoundedCornerShape(6.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AlertCrimson, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI ALERT: GROUND OBSTACLE RANGE LIMIT",
                                color = AlertCrimson,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // CONTROLLER PANELS ON LHS & RHS FLANK SIDES FOR ROTATION ADJUSTMENTS
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Panel: Dynamic Controls
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleAiObjectTracking() },
                        modifier = Modifier
                            .background(
                                color = if (isAiActive) TelemetryGreen.copy(alpha = 0.2f) else GlassSurface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isAiActive) TelemetryGreen else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("fpv_toggle_ai_tracking")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle YOLOv8 AI",
                            tint = if (isAiActive) TelemetryGreen else Color.White
                        )
                    }

                    IconButton(
                        onClick = { isRecording = !isRecording },
                        modifier = Modifier
                            .background(
                                color = if (isRecording) AlertCrimson.copy(alpha = 0.2f) else GlassSurface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isRecording) AlertCrimson else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("fpv_toggle_recording")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Videocam else Icons.Default.CameraAlt,
                            contentDescription = "FPV Record",
                            tint = if (isRecording) AlertCrimson else Color.White
                        )
                    }
                }
            }
        }
    }
}
