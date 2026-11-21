package com.jayra.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CornerRadius
import androidx.compose.ui.graphics.drawscope.Outline
import androidx.compose.ui.graphics.shape.Shape
import androidx.compose.ui.layout.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

fun squircle(radius: Int = 12) = RoundedCornerShape(
    topStart = (radius * 1.2f).dp,
    topEnd = (radius * 1.2f).dp,
    bottomStart = (radius * 1.2f).dp,
    bottomEnd = (radius * 1.2f).dp
)

class SquircleShape(radius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerPx = (radius * 1.2f).toPx(density)
        return Outline.RoundedRect(
            rect = Offset.Zero.toRect(size),
            cornerRadius = CornerRadius(cornerPx)
        )
    }
}
