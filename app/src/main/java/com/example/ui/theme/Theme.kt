package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TelegramDarkColorScheme = darkColorScheme(
    primary = TelegramBlue,
    onPrimary = Color.White,
    primaryContainer = TelegramDarkTopBar,
    onPrimaryContainer = Color.White,
    background = Color(0xFF0F1721),
    onBackground = Color.White,
    surface = Color(0xFF17212B),
    onSurface = Color.White,
    surfaceVariant = TelegramSearchDark,
    onSurfaceVariant = TelegramSubtext,
    outline = TelegramSeparatorDark,
    error = TelegramDangerRed
)

private val TelegramLightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = Color.White,
    primaryContainer = TelegramLightTopBar,
    onPrimaryContainer = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = TelegramSearchLight,
    onSurfaceVariant = TelegramSubtext,
    outline = TelegramSeparatorLight,
    error = TelegramDangerRed
)

@Composable
fun TelegramTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TelegramDarkColorScheme else TelegramLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    TelegramTheme(darkTheme = darkTheme, content = content)
}
