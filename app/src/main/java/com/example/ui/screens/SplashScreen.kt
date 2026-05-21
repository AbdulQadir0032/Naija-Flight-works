package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TelemetryGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToNext: () -> Unit) {
    val angle = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Rotate the scan sweep eternally
        angle.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(Unit) {
        // Mock loading bar
        progress.animateTo(
            targetValue = 100f,
            animationSpec = tween(2200, easing = LinearEasing)
        )
        delay(300)
        onNavigateToNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tactical Avionics HUD Scanner
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Reticle grid rings
                    drawCircle(
                        color = CyberCyan.copy(alpha = 0.2f),
                        radius = radius,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = CyberCyan.copy(alpha = 0.4f),
                        radius = radius * 0.7f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = CyberCyan.copy(alpha = 0.6f),
                        radius = radius * 0.4f,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Crosshair grid markings
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.3f),
                        start = Offset(0f, center.y),
                        end = Offset(size.width, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.3f),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Scanning sweeping line
                    val angleRad = Math.toRadians(angle.value.toDouble())
                    val scanEnd = Offset(
                        (center.x + radius * Math.cos(angleRad)).toFloat(),
                        (center.y + radius * Math.sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = CyberCyan,
                        start = center,
                        end = scanEnd,
                        strokeWidth = 2.dp.toPx()
                    )
                }

                Text(
                    text = "GCS V4.1",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SKYCONTROL COCKPIT",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "STATUS: INITIALIZING AVIONICS LINK",
                color = TelemetryGreen,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Subsystem check labels
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val prg = progress.value
                val itemStyle = TextStyle(prg)
                Text("IMU GYROSCOPE STABILITY: ${itemStyle(20f)}", color = itemStyle.color(20f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("MAVLINK SERIAL CORRIDOR: ${itemStyle(45f)}", color = itemStyle.color(45f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("LIDAR OBSTACLE RADAR: ${itemStyle(65f)}", color = itemStyle.color(65f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("FLIGHT CONTROLLER HEURISTICS: ${itemStyle(85f)}", color = itemStyle.color(85f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

private class TextStyle(val currentProgress: Float) {
    fun color(threshold: Float): Color = if (currentProgress >= threshold) TelemetryGreen else Color.Gray.copy(alpha = 0.5f)
    operator fun invoke(threshold: Float): String = if (currentProgress >= threshold) "OPERATIONAL" else "PENDING..."
}
