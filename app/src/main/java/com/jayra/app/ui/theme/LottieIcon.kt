package com.jayra.app.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun LottieIcon(
    assetName: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    playOnStart: Boolean = false
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/$assetName.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (playOnStart) LottieConstants.IterateForever else 1,
        restartOnPlay = false
    )
    LottieAnimation(
        composition = composition,
        progress = progress,
        modifier = modifier.size(size)
    )
}
