package com.student.esp32securelock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A237E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC5CAE9),
    onPrimaryContainer = Color(0xFF0D1453),
    secondary = Color(0xFF00695C),
    onSecondary = Color.White,
    error = Color(0xFFB00020),
    onError = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    onPrimary = Color(0xFF0D1453),
    primaryContainer = Color(0xFF283593),
    onPrimaryContainer = Color(0xFFE8EAF6),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00352D),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0)
)

@Composable
fun ESP32SecureLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = scheme, content = content)
}
