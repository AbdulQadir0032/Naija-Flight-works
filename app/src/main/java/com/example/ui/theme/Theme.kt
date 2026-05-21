package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    primaryContainer = SlateCyanContainer,
    secondary = WarningAmber,
    tertiary = AlertCrimson,
    background = ObsidianBackground,
    surface = GlassSurface,
    surfaceVariant = GlassSurfaceVariant,
    onPrimary = ObsidianBackground,
    onSecondary = ObsidianBackground,
    onTertiary = ObsidianBackground,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = DarkColorScheme // Default Ground Stations enforce dark cockpit backgrounds

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force HUD mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
