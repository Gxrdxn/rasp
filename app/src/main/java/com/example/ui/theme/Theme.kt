package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RaspiRed,
    onPrimary = Color.White,
    primaryContainer = RaspiRedDark,
    onPrimaryContainer = Color.White,
    secondary = TelemetryCyan,
    onSecondary = Color.Black,
    secondaryContainer = TelemetryCyanDark,
    onSecondaryContainer = Color.White,
    tertiary = EmeraldGreen,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = CriticalRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = RaspiRed,
    onPrimary = Color.White,
    secondary = TelemetryCyanDark,
    onSecondary = Color.White,
    tertiary = EmeraldGreen,
    onTertiary = Color.Black,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = CriticalRed,
    onError = Color.White
)

@Composable
fun RaspiDashTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode per prompt requirement
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias for template
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    RaspiDashTheme(darkTheme = darkTheme, content = content)
}
