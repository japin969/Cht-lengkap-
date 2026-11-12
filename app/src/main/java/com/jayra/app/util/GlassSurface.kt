package com.jayra.app.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState

/**
 * JAYRA-UI v2.0 — Liquid Glass sejati via library NadeemIqbal.
 * Fallback zero-alloc otomatis di Android < 12 / RAM rendah.
 * Efek kaca (edge sheen + tint) tetap ada di HP jadul, tanpa blur buffer.
 */
@Composable
fun Modifier.glassSurface(
    solidColor: Color,
    cornerRadius: Dp = 20.dp,
    state: LiquidGlassState = rememberLiquidGlassState()
): Modifier = this
    .shadow(
        elevation = 4.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = Color.Black.copy(alpha = 0.06f),
        spotColor = Color.Black.copy(alpha = 0.12f)
    )
    .clip(RoundedCornerShape(cornerRadius))
    .liquidGlass(state)
    .background(solidColor.copy(alpha = 0.60f))
    .border(0.75.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(cornerRadius))

@Deprecated(
    "Gunakan glassSurface(solidColor, cornerRadius, state)",
    replaceWith = ReplaceWith("glassSurface(solidColor, cornerRadius)")
)
@Composable
fun Modifier.glassSurface(
    solidColor: Color,
    tintAlpha: Float,
    blurRadius: Dp,
    cornerRadius: Dp = 0.dp
): Modifier = this.glassSurface(solidColor = solidColor, cornerRadius = cornerRadius)
