package com.stonecode.mapsroutepicker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4285F4),
    secondary = Color(0xFF34A853),
    tertiary = Color(0xFFFBBC04)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4285F4),
    secondary = Color(0xFF34A853),
    tertiary = Color(0xFFFBBC04)
)

@Composable
fun MapsRoutePickerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
