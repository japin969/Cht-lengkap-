package com.jayra.app.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * JAYRA-UI v2.0 — Faux Glass for iOS 26 aesthetic.
 * Zero blur. Zero RenderEffect. Zero API-level branching.
 * Visual identik di semua Android 7-16.
 */
enum class GlassLevel { Subtle, Standard, Prominent }

@Composable
fun Modifier.glassSurface(
    solidColor: Color,
    level: GlassLevel = GlassLevel.Standard,
    cornerRadius: Dp = 20.dp,
    showBorder: Boolean = true,
    showShadow: Boolean = true
): Modifier {
    val enabled = GlassEffectPrefs.enabledState.value
    val shape: Shape = RoundedCornerShape(cornerRadius)

    val tintAlpha = when (level) {
        GlassLevel.Subtle -> 0.92f
        GlassLevel.Standard -> 0.80f
        GlassLevel.Prominent -> 0.68f
    }

    var base = this

    if (showShadow) {
        base = base.shadow(
            elevation = 6.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.06f),
            spotColor = Color.Black.copy(alpha = 0.12f)
        )
    }

    base = base.clip(shape)
        .background(solidColor.copy(alpha = if (enabled) tintAlpha else 1f))

    if (enabled) {
        base = base.background(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                center = Offset.Zero,
                radius = 700f
            )
        )
        base = base.background(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.05f))
            )
        )
    }

    if (showBorder && enabled) {
        base = base.border(
            width = 0.75.dp,
            color = Color.White.copy(alpha = 0.32f),
            shape = shape
        )
    }

    return base
}

/**
 * Backward-compat overload untuk caller lama (ChatRoomScreen, CreateGroupScreen).
 * Map parameter lama -> GlassLevel baru. Zero visual break, zero compile break.
 */
@Deprecated(
    message = "Gunakan glassSurface(solidColor, level, cornerRadius) — versi faux glass v2.",
    replaceWith = ReplaceWith("glassSurface(solidColor, level, cornerRadius)")
)
@Composable
fun Modifier.glassSurface(
    solidColor: Color,
    tintAlpha: Float,
    blurRadius: Dp,
    cornerRadius: Dp = 0.dp,
    overlayTop: Color = Color.White.copy(alpha = 0.08f),
    overlayBottom: Color = Color.Black.copy(alpha = 0.04f)
): Modifier {
    val mappedLevel = when {
        tintAlpha >= 0.88f -> GlassLevel.Subtle
        tintAlpha >= 0.75f -> GlassLevel.Standard
        else -> GlassLevel.Prominent
    }
    return this.glassSurface(
        solidColor = solidColor,
        level = mappedLevel,
        cornerRadius = cornerRadius
    )
}
