package com.jayra.app.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState

/**
 * JAYRA-UI v2.2 — Liquid Glass adaptif per device tier.
 *
 * HIGH   (API ≥31, RAM ≥6GB)   → real blur via liquidGlass
 * MEDIUM (API ≥31, RAM 4–6GB)  → real blur + tint lebih tebal
 * LOW    (API <31 / low-RAM / RAM <4GB)
 *                              → FAUX GLASS: gradient sheen + radial highlight
 *                                + border gradient. NOL blur, NOL RenderEffect.
 *                                Visual tetap iOS-grade, GPU cost nol.
 * OFF    (user toggle off)      → solid opaque
 *
 * Semua operasi LOW/MEDIUM draw-phase only. Aman di Android 7+ RAM 3GB.
 */
@Composable
fun Modifier.glassSurface(
    solidColor: Color,
    cornerRadius: Dp = 20.dp,
    state: LiquidGlassState = rememberLiquidGlassState()
): Modifier {
    val tier = remember(GlassEffectPrefs.enabledState.value) {
        GlassEffectPrefs.resolveTier()
    }
    val shape = RoundedCornerShape(cornerRadius)

    // Shadow berlapis — contact + key light (mirip iOS material)
    val base = this
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.05f),
            spotColor = Color.Black.copy(alpha = 0.12f)
        )
        .clip(shape)

    return when (tier) {
        // ── HIGH: real blur, tint tipis ──────────────────────────
        GlassTier.HIGH -> base
            .liquidGlass(state)
            .background(solidColor.copy(alpha = 0.60f))
            .border(0.75.dp, Color.White.copy(alpha = 0.28f), shape)

        // ── MEDIUM: real blur, tint sedang ───────────────────────
        GlassTier.MEDIUM -> base
            .liquidGlass(state)
            .background(solidColor.copy(alpha = 0.70f))
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = shape
            )

        // ── LOW: FAUX GLASS iOS-grade (zero blur, zero RenderEffect) ──
        GlassTier.LOW -> base
            // 1) base fill: 22% transparan → wallpaper tetap kelihatan samar
            .background(solidColor.copy(alpha = 0.78f))
            // 2) vertical sheen: specular khas iOS dari atas
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.02f),
                        Color.Transparent
                    )
                )
            )
            // 3) radial highlight top-left: "glass glare"
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(0f, 0f),
                    radius = 420f
                )
            )
            // 4) border gradient: edge refraction (bright top → dim bottom)
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.38f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            )

        // ── OFF: solid ───────────────────────────────────────────
        GlassTier.OFF -> base
            .background(solidColor)
    }
}

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
