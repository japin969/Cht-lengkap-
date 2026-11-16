package com.jayra.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun InfinityButton(modifier: Modifier = Modifier) {
    // Anti-lag: freeze animation saat app background
    var isVisible by remember { mutableStateOf(true) }
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            isVisible = e == androidx.lifecycle.Lifecycle.Event.ON_RESUME
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val inf = rememberInfiniteTransition(label = "inf")
    val rawOffset by inf.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow"
    )
    val offset = if (isVisible) rawOffset else 0f
    val pulse by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val scale = if (isVisible) pulse else 1f
    Box(
        modifier.size(64.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {},
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(64.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
            val p = Path().apply {
                moveTo(size.width * .208f, size.height * .5f)
                cubicTo(size.width * .208f, size.height * .312f, size.width * .395f, size.height * .291f, size.width * .5f, size.height * .5f)
                cubicTo(size.width * .604f, size.height * .708f, size.width * .791f, size.height * .687f, size.width * .791f, size.height * .5f)
                cubicTo(size.width * .791f, size.height * .312f, size.width * .604f, size.height * .291f, size.width * .5f, size.height * .5f)
                cubicTo(size.width * .395f, size.height * .708f, size.width * .208f, size.height * .687f, size.width * .208f, size.height * .5f)
                close()
            }
            drawPath(p, Color.Black.copy(alpha = 0.25f), style = Stroke(6f, cap = StrokeCap.Round))
            drawPath(p, Color.Black, style = Stroke(6.5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 22f), offset)))
        }
    }
}
