package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceVariant
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DroneViewModel

@Composable
fun SettingsScreen(
    viewModel: DroneViewModel,
    onNavigateBack: () -> Unit
) {
    val maxGeofence by viewModel.maxGeofenceMeters.collectAsState()
    val rthAlt by viewModel.defaultRthAltitudeMeters.collectAsState()
    val isFpsActive by viewModel.isFpsOverlayEnabled.collectAsState()
    val ssidSim by viewModel.activeSsidSim.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_btn")
                ) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Back", tint = CyberCyan)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GCS CONFIGURATIONS",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. SAFETY & ENVELOPE CONFIGS
            Text(
                "SAFETY LIMITS & COMPLIANCE",
                color = WarningAmber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Maximum geofence distance
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("MAX GEOFENCE", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Automatic Return-To-Home trigger point", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${maxGeofence.toInt()}m", color = WarningAmber, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = maxGeofence,
                        onValueChange = { viewModel.maxGeofenceMeters.value = it },
                        valueRange = 100f..1500f,
                        colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = WarningAmber),
                        modifier = Modifier.testTag("settings_geofence_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Safe RTH Cruise altitude
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("RTH SAFE ALTITUDE", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Transit altura during auto recovery", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${rthAlt.toInt()}m", color = WarningAmber, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = rthAlt,
                        onValueChange = { viewModel.defaultRthAltitudeMeters.value = it },
                        valueRange = 25f..120f,
                        colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = WarningAmber),
                        modifier = Modifier.testTag("settings_rth_slider")
                    )
                }
            }

            // 2. RADIO SYSTEM SIMULATED
            Text(
                "RF TELEMETRY CONFIGURATION",
                color = CyberCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LIVE HUD OVERLAYS", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Active diagnostic FPS metrics counters", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                        Switch(
                            checked = isFpsActive,
                            onCheckedChange = { viewModel.isFpsOverlayEnabled.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("settings_fps_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SIMULATED SSID BIND", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Active Bind: $ssidSim", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                val ran = (100..999).random()
                                viewModel.activeSsidSim.value = "SKYTAC_DATA_LINK_$ran"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceVariant),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("RE-BIND CHANNEL", color = CyberCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer credits
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AEROSYSTEMS GCS HUB - VER 4.1.2026",
                    color = Color.Gray.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}
