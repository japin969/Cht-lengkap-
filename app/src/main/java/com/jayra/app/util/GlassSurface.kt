package com.jayra.app.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Glass effect untuk iOS 26 — adaptive per device:
 * - Aktif + API 31+ → real blur + gradient overlay + hairline border
 * - Aktif + API < 31 → gradient (semu, tanpa blur)
 * - Nonaktif → solid color
 *
 * 0 lag untuk HP kentang: kalau blur bikin fps turun, user bisa matikan via Settings.
 */
@Composable
fun Modifier.glassSurface(
    solidColor: Color,
    tintAlpha: Float = 0.85f,
    blurRadius: Dp = 20.dp,
    cornerRadius: Dp = 0.dp,
    overlayTop: Color = Color.White.copy(alpha = 0.08f),
    overlayBottom: Color = Color.Black.copy(alpha = 0.04f)
): Modifier {
    val enabled = GlassEffectPrefs.enabledState.value
    val shape: Shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RoundedCornerShape(0.dp)
    var base = this
    if (enabled && GlassEffectPrefs.supportsRealBlur()) {
        // Android 12+ : real blur
        base = base.blur(radius = blurRadius)
    }
    return base
        .clip(shape)
        .background(solidColor.copy(alpha = if (enabled) tintAlpha else 1f))
        .background(
            Brush.verticalGradient(
                colors = if (enabled) listOf(overlayTop, Color.Transparent, overlayBottom)
                else listOf(Color.Transparent, Color.Transparent)
            )
        )
        .border(
            width = if (enabled) 0.5.dp else 0.dp,
            color = if (enabled) Color.White.copy(alpha = 0.25f) else Color.Transparent,
            shape = shape
        )
}
