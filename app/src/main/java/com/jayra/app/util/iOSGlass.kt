package com.jayra.app.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.iosGlass(
    cornerRadius: Int = 20,
    tint: Color = Color.White.copy(alpha = 0.15f),
    elevation: Int = 8
): Modifier = this
    .shadow(elevation.dp, RoundedCornerShape(cornerRadius.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        Brush.verticalGradient(
            colors = listOf(
                tint.copy(alpha = tint.alpha + 0.1f),
                tint
            )
        )
    )
    .border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.25f),
        shape = RoundedCornerShape(cornerRadius.dp)
    )
