package com.weighttracker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFFF97316),
    onSecondary = Color.White,
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFFFDBA74),
    onSecondary = Color(0xFF0F172A),
)

@Composable
fun WeightTrackerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
