package com.jayra.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.util.JayraHaptics
import com.jayra.app.util.JayraSmooth
import com.jayra.app.util.Performance
import kotlin.math.abs

data class JayraNavItem(val label: String, val icon: ImageVector)

val jayraNavItems = listOf(
    JayraNavItem("Chat", Icons.Outlined.ChatBubbleOutline),
    JayraNavItem("Calls", Icons.Outlined.Call),
    JayraNavItem("Music", Icons.Outlined.MusicNote),
    JayraNavItem("Hub", Icons.Outlined.GridView),
    JayraNavItem("Settings", Icons.Outlined.Settings)
)

@Composable
fun LiquidNavBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isFolded: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val isLowRam = remember { Performance.isLowRam(context) }
    val isLegacy = remember { Performance.isLegacy() }
    val isFlagship = remember { Performance.isFlagship(context) }
    val showShine = !isLowRam && !isLegacy

    val pillAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1.18f) }
    val stretchAnim = remember { Animatable(1f) }
    var lastIndex by remember { mutableStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        val distance = abs(selectedIndex - lastIndex).coerceAtLeast(1)
        pillAnim.animateTo(
            selectedIndex.toFloat(),
            spring(dampingRatio = 0.72f, stiffness = if (isLowRam) 520f else 340f)
        )
        JayraHaptics.tick(context, isFlagship)
        scaleAnim.snapTo(1f)
        scaleAnim.animateTo(1.18f, spring(dampingRatio = 0.55f, stiffness = 420f))
        if (distance > 1 && isFlagship) {
            stretchAnim.snapTo(1.08f)
            stretchAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f))
        }
        lastIndex = selectedIndex
    }

    val foldAnim = remember { Animatable(0f) }
    LaunchedEffect(isFolded) {
        foldAnim.animateTo(
            if (isFolded) 1f else 0f,
            spring(dampingRatio = 0.8f, stiffness = 300f)
        )
    }

    val shineAnim = if (showShine) rememberInfiniteTransition(label = "shine") else null
    val shineX = shineAnim?.animateFloat(
        -0.8f, 1.3f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "shineX"
    )?.value ?: 0f

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 14.dp)
            .graphicsLayer {
                val f = foldAnim.value
                scaleX = 1f - f * 0.03f
                scaleY = 1f - f * 0.03f
            }
    ) {
        val slotW = maxWidth / 5f
        val pillW = slotW * 0.9f
        val slotWPx = with(density) { slotW.toPx() }
        val realPillX = pillAnim.value * slotWPx

        Box(Modifier.fillMaxSize()) {
            // ============ CANVAS BACKGROUND (nol icon, nol text) ============
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = JayraSmooth.bgBrush,
                    size = size,
                    cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx())
                )
                drawRect(brush = JayraSmooth.topShineBrush, size = Size(size.width, size.height * 0.5f))
                drawRect(brush = JayraSmooth.bottomShadeBrush, size = size)
                drawLine(
                    brush = JayraSmooth.topLineBrush,
                    start = Offset(size.width * 0.12f, 0.5f),
                    end = Offset(size.width * 0.88f, 0.5f),
                    strokeWidth = 1f
                )
                if (showShine) {
                    var x = shineX * size.width
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(0.22f), Color.Transparent),
                            start = Offset(x - 80, 0f), end = Offset(x + 80, 0f)
                        ),
                        topLeft = Offset(x - 80, 0f),
                        size = Size(160f, size.height)
                    )
                    val shineX2 = ((shineX + 0.15f) % 2.1f) - 0.8f
                    x = shineX2 * size.width
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(0.13f), Color.Transparent),
                            start = Offset(x - 80, 0f), end = Offset(x + 80, 0f)
                        ),
                        topLeft = Offset(x - 80, 0f),
                        size = Size(160f, size.height)
                    )
                }
            }

            // ============ INACTIVE ITEMS (composable, nol Canvas draw) ============
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                jayraNavItems.forEachIndexed { i, item ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { if (selectedIndex != i) onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (i != selectedIndex) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = Color.White.copy(0.58f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = item.label,
                                    color = Color.White.copy(0.58f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // ============ PILL (Liquid Glass) ============
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(y = 18.dp)
                    .graphicsLayer {
                        translationX = realPillX
                        scaleX = stretchAnim.value
                        scaleY = 2f - stretchAnim.value
                    }
                    .width(pillW)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(JayraSmooth.pillBrush)
            )

            // ============ ACTIVE ICON (translationX + scale + rotation) ============
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = realPillX
                        scaleX = stretchAnim.value
                        scaleY = 2f - stretchAnim.value
                    }
            ) {
                Column(
                    Modifier.width(pillW).align(Alignment.CenterStart),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = jayraNavItems[selectedIndex].icon,
                        contentDescription = jayraNavItems[selectedIndex].label,
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = scaleAnim.value
                                scaleY = scaleAnim.value
                                rotationZ = when (jayraNavItems[selectedIndex].label) {
                                    "Settings" -> 90f
                                    "Music" -> -6f
                                    else -> 0f
                                }
                            }
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = jayraNavItems[selectedIndex].label,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
