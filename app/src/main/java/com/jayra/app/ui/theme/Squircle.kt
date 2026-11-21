package com.jayra.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

fun squircle(radius: Int = 12) = RoundedCornerShape(
    topStart = (radius * 1.2f).dp,
    topEnd = (radius * 1.2f).dp,
    bottomStart = (radius * 1.2f).dp,
    bottomEnd = (radius * 1.2f).dp
)

class SquircleShape(radius: Dp) : RoundedCornerShape(
    topStart = radius * 1.2f,
    topEnd = radius * 1.2f,
    bottomStart = radius * 1.2f,
    bottomEnd = radius * 1.2f
)
