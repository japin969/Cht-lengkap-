package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val THEME_TRANSITION_DURATION_MS = 300

private val TelegramDarkColorScheme = darkColorScheme(
    primary = TelegramBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = TelegramDarkTopBar,
    onPrimaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF0F1721),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF17212B),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = TelegramSearchDark,
    onSurfaceVariant = TelegramSubtext,
    outline = TelegramSeparatorDark,
    error = TelegramDangerRed
)

private val TelegramLightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = TelegramLightTopBar,
    onPrimaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = TelegramSearchLight,
    onSurfaceVariant = TelegramSubtext,
    outline = TelegramSeparatorLight,
    error = TelegramDangerRed
)

@Composable
private fun animateTelegramColorScheme(targetScheme: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = THEME_TRANSITION_DURATION_MS)

    val primary by animateColorAsState(targetScheme.primary, spec, label = "theme_primary")
    val onPrimary by animateColorAsState(targetScheme.onPrimary, spec, label = "theme_on_primary")
    val primaryContainer by animateColorAsState(
        targetScheme.primaryContainer,
        spec,
        label = "theme_primary_container"
    )
    val onPrimaryContainer by animateColorAsState(
        targetScheme.onPrimaryContainer,
        spec,
        label = "theme_on_primary_container"
    )
    val background by animateColorAsState(targetScheme.background, spec, label = "theme_background")
    val onBackground by animateColorAsState(
        targetScheme.onBackground,
        spec,
        label = "theme_on_background"
    )
    val surface by animateColorAsState(targetScheme.surface, spec, label = "theme_surface")
    val onSurface by animateColorAsState(targetScheme.onSurface, spec, label = "theme_on_surface")
    val surfaceVariant by animateColorAsState(
        targetScheme.surfaceVariant,
        spec,
        label = "theme_surface_variant"
    )
    val onSurfaceVariant by animateColorAsState(
        targetScheme.onSurfaceVariant,
        spec,
        label = "theme_on_surface_variant"
    )
    val outline by animateColorAsState(targetScheme.outline, spec, label = "theme_outline")
    val error by animateColorAsState(targetScheme.error, spec, label = "theme_error")

    return targetScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        error = error
    )
}

@Composable
fun TelegramTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColorOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) TelegramDarkColorScheme else TelegramLightColorScheme
    val targetScheme = if (primaryColorOverride != null) {
        baseScheme.copy(primary = primaryColorOverride)
    } else {
        baseScheme
    }
    val animatedColorScheme = animateTelegramColorScheme(targetScheme)

    MaterialTheme(
        colorScheme = animatedColorScheme,
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

@Composable
private fun TelegramThemePreviewCard(modeLabel: String) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = modeLabel,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transisi warna mulus 300ms aktif",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(name = "TelegramTheme - Light", showBackground = true)
@Composable
private fun TelegramThemeLightPreview() {
    TelegramTheme(darkTheme = false) {
        TelegramThemePreviewCard(modeLabel = "Jayra Light Theme")
    }
}

@Preview(name = "TelegramTheme - Dark", showBackground = true)
@Composable
private fun TelegramThemeDarkPreview() {
    TelegramTheme(darkTheme = true) {
        TelegramThemePreviewCard(modeLabel = "Jayra Dark Theme")
    }
}
