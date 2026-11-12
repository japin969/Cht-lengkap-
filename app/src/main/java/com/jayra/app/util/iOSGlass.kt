package com.jayra.app.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Faux glass — versi simple untuk pemakaian cepat.
 * Specular highlight top-left, asymmetric shadow, hairline border.
 */
fun Modifier.iosGlass(
    cornerRadius: Int = 20,
    tint: Color = Color.White.copy(alpha = 0.15f),
    elevation: Int = 4,
    showSpecular: Boolean = true,
    showBorder: Boolean = true
): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    var m = this
        .shadow(
            elevation = elevation.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.06f),
            spotColor = Color.Black.copy(alpha = 0.12f)
        )
        .clip(shape)
        .background(tint)

    if (showSpecular) {
        m = m.background(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                center = Offset.Zero,
                radius = 600f
            )
        )
    }

    if (showBorder) {
        m = m.border(0.75.dp, Color.White.copy(alpha = 0.28f), shape)
    }

    return m
}
