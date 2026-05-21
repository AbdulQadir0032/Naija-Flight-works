package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ConnectionStatus
import com.example.data.models.HealthStatus
import com.example.data.models.SensorHealth
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DroneViewModel

@Composable
fun DiagnosticsScreen(
    viewModel: DroneViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToFpv: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val telemetry by viewModel.telemetryState.collectAsState()
    val sensors by viewModel.sensorsHealth.collectAsState()
    val consoleListState = rememberLazyListState()

    // Automatically scroll down console log feed when new messages land
    LaunchedEffect(telemetry.messageLog.size) {
        if (telemetry.messageLog.isNotEmpty()) {
            consoleListState.animateScrollToItem(telemetry.messageLog.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        bottomBar = {
            TacticalBottomNavBar(
                currentRoute = "diagnostics",
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToFpv = onNavigateToFpv,
                onNavigateToPlanner = onNavigateToPlanner,
                onNavigateToDiagnostics = {},
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
            Spacer(modifier = Modifier.height(14.dp))

            // 1. THERMAL SYSTEM LOG BOARD
            Text(
                text = "THERMAL DIAGNOSTICS NODE",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThermalIndicatorCell(
                    label = "CPU CORE TEMP",
                    value = "${"%.1f".format(telemetry.cpuTemp)} °C",
                    statusColor = if (telemetry.cpuTemp < 52f) TelemetryGreen else WarningAmber,
                    modifier = Modifier.weight(1f)
                )

                ThermalIndicatorCell(
                    label = "ESC POWER PACK",
                    value = "${"%.1f".format(telemetry.escTemp)} °C",
                    statusColor = if (telemetry.escTemp < 45f) TelemetryGreen else WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. DETAILED MODULE NODES (REAL TIME FREQUENCIES)
            Text(
                text = "INDIVIDUAL SENSOR REGISTRY (MAVLink)",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (telemetry.connectionStatus != ConnectionStatus.CONNECTED) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("UART CONTEXT OFFLINE: ESTABLISH GROUND CONNECTION", color = Color.White.copy(alpha = 0.2f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        items(sensors) { sensor ->
                            SensorRegistryItem(sensor)
                        }
                    }
                }

                // 3. REAL-TIME FLIGHT CONTROLLER MAVLINK SERIAL CONSOLE FEED
                Text(
                    text = "MAVLINK REALTIME SERIAL CONSOLE LINK",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COM_UART_STREAM // 115200bps", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                            IconButton(onClick = { viewModel.clearTelemetryLogs() }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            state = consoleListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("mavlink_console_logs")
                        ) {
                            if (telemetry.messageLog.isEmpty()) {
                                item {
                                    Text("[-] Console idle... awaiting telemetry sync downlink.", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            } else {
                                items(telemetry.messageLog) { logLine ->
                                    val isCrit = logLine.contains("CRITICAL") || logLine.contains("WARN")
                                    Text(
                                        text = logLine,
                                        color = if (isCrit) AlertCrimson else TelemetryGreen,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
fun ThermalIndicatorCell(
    label: String,
    value: String,
    statusColor: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.DeviceThermostat, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun SensorRegistryItem(sensor: SensorHealth) {
    val (statusText, statusColor) = when (sensor.status) {
        HealthStatus.OPERATIONAL -> "OK" to TelemetryGreen
        HealthStatus.WARNING -> "WARN" to WarningAmber
        HealthStatus.CRITICAL -> "ALERT" to AlertCrimson
        HealthStatus.OFFLINE -> "OFFLINE" to Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassSurface, RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(statusColor, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(sensor.moduleName, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("RATE: ${sensor.payloadRateHz.toInt()} Hz | INT.ERR: ${"%.2f".format(sensor.errorRatePercentage)}%", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Text(
            text = statusText,
            color = statusColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
