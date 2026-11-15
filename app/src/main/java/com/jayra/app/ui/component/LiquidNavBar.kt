package com.jayra.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.jayra.app.util.Performance

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
    val isLowRam = remember { Performance.isLowRam(context) }
    val isLegacy = remember { Performance.isLegacy() }
    val showShine = !isLowRam && !isLegacy

    val painters = jayraNavItems.map { rememberVectorPainter(it.icon, Color.White) }
    val textMeasurer = rememberTextMeasurer()
    val textStyleInactive = remember {
        TextStyle(color = Color.White.copy(0.58f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }

    val pillAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1.22f) }
    LaunchedEffect(selectedIndex) {
        pillAnim.animateTo(
            selectedIndex.toFloat(),
            spring(dampingRatio = 0.75f, stiffness = if (isLowRam) 500f else 320f)
        )
        scaleAnim.snapTo(1f)
        scaleAnim.animateTo(1.22f, spring(dampingRatio = 0.6f, stiffness = 400f))
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
        val navWidth = maxWidth
        val slotW = navWidth / 5f
        val pillW = slotW * 0.9f

        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF161622), Color(0xFF1C1C2C), Color(0xFF221E38), Color(0xFF12121E))
                    ),
                    size = size,
                    cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx())
                )
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.Transparent)),
                    size = Size(size.width, size.height * 0.5f)
                )
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.32f))),
                    size = size
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(0.4f), Color.Transparent)
                    ),
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
                val slotPx = size.width / 5f
                val iconSize = 28.dp.toPx()
                val gapPx = 3.dp.toPx()
                jayraNavItems.forEachIndexed { i, item ->
                    if (i == selectedIndex) return@forEachIndexed
                    val cx = slotPx * i + slotPx / 2f
                    with(painters[i]) {
                        translate(
                            left = cx - iconSize / 2f,
                            top = size.height / 2f - iconSize / 2f - 8.dp.toPx()
                        ) {
                            draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Color.White.copy(0.58f)))
                        }
                    }
                    val tl = textMeasurer.measure(item.label, textStyleInactive)
                    drawText(
                        tl,
                        topLeft = Offset(
                            cx - tl.size.width / 2f,
                            size.height / 2f + iconSize / 2f + gapPx - 8.dp.toPx()
                        )
                    )
                }
            }

            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(y = 18.dp)
                    .graphicsLayer { translationX = pillAnim.value * slotW.toPx() }
                    .width(pillW)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color.White.copy(0.18f), Color.White.copy(0.06f))))
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = pillAnim.value * slotW.toPx() }
            ) {
                Column(
                    Modifier.width(pillW).align(Alignment.CenterStart),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = jayraNavItems[selectedIndex].icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { scaleX = scaleAnim.value; scaleY = scaleAnim.value }
                    )
                    Spacer(Modifier.height(3.dp))
                    androidx.compose.material3.Text(
                        text = jayraNavItems[selectedIndex].label,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Row(Modifier.fillMaxSize()) {
                repeat(5) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(i) }
                    )
                }
            }
        }
    }
}
