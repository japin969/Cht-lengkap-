package com.jayra.app.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jayra.app.R

@Composable
fun IOSLottieToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.toggle))
    val targetProgress = if (checked) 1f else 0f
    LottieAnimation(
        composition = composition,
        progress = { targetProgress },
        modifier = modifier
            .size(44.dp)
            .clickable { onCheckedChange(!checked) }
    )
}
