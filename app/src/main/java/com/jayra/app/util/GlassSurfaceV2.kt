package com.jayra.app.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jayra.app.ui.theme.ButtonSpec
import com.jayra.app.ui.theme.GlassSurfaceSpec
import com.jayra.app.ui.theme.GradientStop

// ═══════════════════════════════════════════════════════════
// JAYRA — GlassSurfaceV2
// ═══════════════════════════════════════════════════════════
// Spec-driven glass rendering. Zero hardcoded colors.
// Zero Modifier.blur. All visual layers via drawWithContent
// (draw-phase only, safe on Android 7 / RAM 3GB).
//
// Overload 1: GlassSurfaceSpec + solidColor (surface)
// Overload 2: ButtonSpec (self-contained fill + sheen + border)
// ═══════════════════════════════════════════════════════════

private const val BORDER_STROKE_DP = 0.75f
private const val GLARE_ALPHA = 0.10f

/**
 * Overload for surfaces with separately-provided base color
 * (GlassSurfaceSpec has baseAlpha but no baseColor field).
 */
fun Modifier.glassSurfaceV2(
    spec: GlassSurfaceSpec,
    solidColor: Color,
    cornerRadius: Dp = 12.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val strokeWidth = BORDER_STROKE_DP.dp
    return this
        .clip(shape)
        .drawWithContent {
            val radiusPx = cornerRadius.toPx()
            // 1) Base fill (behind content)
            drawRoundRect(
                color = solidColor.copy(alpha = spec.baseAlpha),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                size = size
            )
            // 2) Content
            drawContent()
            // 3) Sheen (top -> fade)
            if (spec.sheenGradient.isNotEmpty()) {
                drawRoundRect(
                    brush = stopsBrush(spec.sheenGradient),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    size = size
                )
            }
            // 4) Glare (top-left radial highlight; null = skip)
            if (spec.glareRadiusRatio != null) {
                val glareRadius = size.minDimension * spec.glareRadiusRatio
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = GLARE_ALPHA), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = glareRadius
                    ),
                    radius = glareRadius,
                    center = Offset(0f, 0f)
                )
            }
            // 5) Border via drawRoundRect + Stroke (NOT .border() modifier)
            if (spec.borderGradient.isNotEmpty()) {
                val strokePx = strokeWidth.toPx()
                drawRoundRect(
                    brush = stopsBrush(spec.borderGradient),
                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokePx)
                )
            }
        }
}

/**
 * Overload for buttons (ButtonSpec is self-contained: fillColor + fillAlpha
 * + nullable sheenGradient + nullable borderGradient).
 */
fun Modifier.glassSurfaceV2(
    spec: ButtonSpec,
    cornerRadius: Dp = 12.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val strokeWidth = BORDER_STROKE_DP.dp
    return this
        .clip(shape)
        .drawWithContent {
            val radiusPx = cornerRadius.toPx()
            // 1) Fill (behind content)
            drawRoundRect(
                color = spec.fillColor.copy(alpha = spec.fillAlpha),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                size = size
            )
            // 2) Content (text)
            drawContent()
            // 3) Sheen (nullable)
            val sheen = spec.sheenGradient
            if (!sheen.isNullOrEmpty()) {
                drawRoundRect(
                    brush = stopsBrush(sheen),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    size = size
                )
            }
            // 4) Border via drawRoundRect + Stroke (nullable)
            val border = spec.borderGradient
            if (!border.isNullOrEmpty()) {
                val strokePx = strokeWidth.toPx()
                drawRoundRect(
                    brush = stopsBrush(border),
                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokePx)
                )
            }
        }
}

private fun stopsBrush(stops: List<GradientStop>): Brush {
    val arr = stops.map { it.position to it.color }.toTypedArray()
    return Brush.verticalGradient(*arr)
}
