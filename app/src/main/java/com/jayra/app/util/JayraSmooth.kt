package com.jayra.app.util

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object JayraSmooth {
    // RAM DIET: gradient dibuat sekali, tidak pernah dibuat ulang
    val bgBrush = Brush.linearGradient(
        listOf(Color(0xFF161622), Color(0xFF1C1C2C), Color(0xFF221E38), Color(0xFF12121E))
    )
    val pillBrush = Brush.linearGradient(
        listOf(Color.White.copy(0.18f), Color.White.copy(0.06f))
    )
    val topShineBrush = Brush.verticalGradient(
        listOf(Color.White.copy(0.08f), Color.Transparent)
    )
    val bottomShadeBrush = Brush.verticalGradient(
        listOf(Color.Transparent, Color.Black.copy(0.32f))
    )
    val topLineBrush = Brush.horizontalGradient(
        listOf(Color.Transparent, Color.White.copy(0.4f), Color.Transparent)
    )
    fun clampDelta(v: Float): Float = v.coerceIn(-1.5f, 1.5f)
}
