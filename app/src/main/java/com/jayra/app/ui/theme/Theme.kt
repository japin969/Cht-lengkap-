package com.jayra.app.ui.theme

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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val THEME_TRANSITION_DURATION_MS = 300

private val JayraDarkColorScheme = darkColorScheme(
    primary = JayraBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = JayraDarkTopBar,
    onPrimaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF0F1721),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF17212B),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = JayraSearchDark,
    onSurfaceVariant = JayraSubtext,
    outline = JayraSeparatorDark,
    error = JayraDangerRed
)

private val JayraLightColorScheme = lightColorScheme(
    primary = JayraBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = JayraLightTopBar,
    onPrimaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = JayraSearchLight,
    onSurfaceVariant = JayraSubtext,
    outline = JayraSeparatorLight,
    error = JayraDangerRed
)

@Composable
private fun animateJayraColorScheme(targetScheme: ColorScheme): ColorScheme {
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
fun JayraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColorOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) JayraDarkColorScheme else JayraLightColorScheme
    val targetScheme = if (primaryColorOverride != null) {
        baseScheme.copy(primary = primaryColorOverride)
    } else {
        baseScheme
    }
    val animatedColorScheme = animateJayraColorScheme(targetScheme)

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = SFProFontFamily)
        ) {
            content()
        }
    }
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    JayraTheme(darkTheme = darkTheme, content = content)
}

@Composable
private fun JayraThemePreviewCard(modeLabel: String) {
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

@Preview(name = "JayraTheme - Light", showBackground = true)
@Composable
private fun JayraThemeLightPreview() {
    JayraTheme(darkTheme = false) {
        JayraThemePreviewCard(modeLabel = "Jayra Light Theme")
    }
}

@Preview(name = "JayraTheme - Dark", showBackground = true)
@Composable
private fun JayraThemeDarkPreview() {
    JayraTheme(darkTheme = true) {
        JayraThemePreviewCard(modeLabel = "Jayra Dark Theme")
    }
}
