package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ConnectionStatus
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceVariant
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DroneViewModel

@Composable
fun ConnectionScreen(
    viewModel: DroneViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val telemetry by viewModel.telemetryState.collectAsState()
    val activeSsid by viewModel.activeSsidSim.collectAsState()

    var selectedConnectionMethod by remember { mutableStateOf(ConnectionMethod.WIFI) }

    // Pulsing alpha for active operations
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GROUND COMMAND STATION",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "COGNITIVE DIGITAL LINK PROTOCOL v2.0",
                        color = CyberCyan.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("connection_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "GCS Settings",
                        tint = CyberCyan
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Connection Link State Display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RADIO LINK STATUS",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Large dynamic status pill
                        val (statusText, statusColor) = when (telemetry.connectionStatus) {
                            ConnectionStatus.DISCONNECTED -> "LINK OFFLINE" to AlertCrimson
                            ConnectionStatus.CONNECTING -> "ESTABLISHING SYNC..." to WarningAmber
                            ConnectionStatus.CONNECTED -> "LINK OPERATIONAL" to TelemetryGreen
                            ConnectionStatus.RECONNECTING -> "SIGNAL LOST: RETRYING" to WarningAmber
                        }

                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .border(1.dp, statusColor, RoundedCornerShape(20.dp))
                                .padding(horizontal = 18.dp, vertical = 6.dp)
                                .alpha(if (telemetry.connectionStatus == ConnectionStatus.CONNECTING) pulsingAlpha else 1f)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("LATENCY", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("${if (telemetry.connectionStatus == ConnectionStatus.CONNECTED) telemetry.latencyMs else 0}ms", color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SIGNAL STRENGTH", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("${if (telemetry.connectionStatus == ConnectionStatus.CONNECTED) telemetry.signalStrength else 0}%", color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PACKET DROP", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = if (telemetry.connectionStatus == ConnectionStatus.CONNECTED) "%.3f".format(telemetry.packetLoss) else "0.000",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Selectable connection interfaces
                Text(
                    text = "SELECT RECEPTION MODE",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ConnectionMethodItem(
                        method = ConnectionMethod.WIFI,
                        title = "WiFi Smart TCP/IP Stream",
                        detail = "Dynamic SSID: $activeSsid | Port 14550",
                        icon = Icons.Default.Wifi,
                        isSelected = selectedConnectionMethod == ConnectionMethod.WIFI,
                        onClick = { selectedConnectionMethod = ConnectionMethod.WIFI }
                    )
                    ConnectionMethodItem(
                        method = ConnectionMethod.USB,
                        title = "USB Serial Baudrate (FTDI/CP210x)",
                        detail = "Baud Rate: 115200 bps | Raw UART Direct",
                        icon = Icons.Default.Cable,
                        isSelected = selectedConnectionMethod == ConnectionMethod.USB,
                        onClick = { selectedConnectionMethod = ConnectionMethod.USB }
                    )
                    ConnectionMethodItem(
                        method = ConnectionMethod.BLE,
                        title = "Bluetooth LE Telemetry Radio",
                        detail = "High speed low emission | 2.4GHz secure FHSS",
                        icon = Icons.Default.Bluetooth,
                        isSelected = selectedConnectionMethod == ConnectionMethod.BLE,
                        onClick = { selectedConnectionMethod = ConnectionMethod.BLE }
                    )
                    ConnectionMethodItem(
                        method = ConnectionMethod.RF_RADIO,
                        title = "RF Radio Transceiver (MAVLink)",
                        detail = "Secure encrypted UHF | Band: 915 MHz",
                        icon = Icons.Default.Radio,
                        isSelected = selectedConnectionMethod == ConnectionMethod.RF_RADIO,
                        onClick = { selectedConnectionMethod = ConnectionMethod.RF_RADIO }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Large action trigger button
                Button(
                    onClick = { viewModel.clickConnectToggle() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (telemetry.connectionStatus) {
                            ConnectionStatus.DISCONNECTED -> CyberCyan
                            ConnectionStatus.CONNECTING -> WarningAmber
                            ConnectionStatus.CONNECTED -> AlertCrimson
                            ConnectionStatus.RECONNECTING -> WarningAmber
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("connect_action_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val label = when (telemetry.connectionStatus) {
                        ConnectionStatus.DISCONNECTED -> "INITIALIZE MASTER LINK"
                        ConnectionStatus.CONNECTING -> "SYNCING DOWNLINK DATA..."
                        ConnectionStatus.CONNECTED -> "SECURE DISCONNECT"
                        ConnectionStatus.RECONNECTING -> "FORCE DISCHARGE LINK"
                    }
                    Text(
                        text = label,
                        color = ObsidianBackground,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Enter Flight Cockpit navigation
                AnimatedVisibility(visible = telemetry.connectionStatus == ConnectionStatus.CONNECTED) {
                    Button(
                        onClick = onNavigateToDashboard,
                        colors = ButtonDefaults.buttonColors(containerColor = TelemetryGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(1.dp, TelemetryGreen, RoundedCornerShape(12.dp))
                            .testTag("enter_cockpit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ENTER THE FLIGHT COCKPIT",
                                color = ObsidianBackground,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ObsidianBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

enum class ConnectionMethod {
    WIFI, USB, BLE, RF_RADIO
}

@Composable
fun ConnectionMethodItem(
    method: ConnectionMethod,
    title: String,
    detail: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .testTag("connection_item_${method.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GlassSurfaceVariant else GlassSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) CyberCyan else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detail,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}
