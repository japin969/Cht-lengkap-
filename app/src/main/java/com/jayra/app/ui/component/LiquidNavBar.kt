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
import androidx.compose.foundation.background // TAMBAHAN 1
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.draw
import androidx.compose.ui.graphics.drawscope.translate // TAMBAHAN 2
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity // TAMBAHAN 3
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText // TAMBAHAN 4
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    val density = LocalDensity.current // FIX 5: untuk toPx di BoxWithConstraints
    val isLowRam = remember { Performance.isLowRam(context) }
    val isLegacy = remember { Performance.isLegacy() }
    val isFlagship = remember { Performance.isFlagship(context) }
    val showShine = !isLowRam && !isLegacy

    // FIX 6: API rememberVectorPainter versi ini tidak terima tint di parameter ke-2
    val painters = jayraNavItems.map { rememberVectorPainter(it.icon) }
    val textMeasurer = rememberTextMeasurer()
    val textStyleInactive = remember {
        TextStyle(color = Color.White.copy(0.58f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }

    val pillAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1.22f) }
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
        scaleAnim.animateTo(1.22f, spring(dampingRatio = 0.55f, stiffness = 420f))
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

    var predictiveIndex by remember { mutableStateOf<Int?>(null) }

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
        val realPillX = with(density) { pillAnim.value * slotW.toPx() } // FIX 5

        Box(Modifier.fillMaxSize()) {
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
                val slotPx = size.width / 5f
                val iconSize = 28.dp.toPx()
                val gapPx = 3.dp.toPx()
                jayraNavItems.forEachIndexed { i, item ->
                    if (i == selectedIndex) return@forEachIndexed
                    val cx = slotPx * i + slotPx / 2f
                    val painter = painters[i]
                    val painterSize = painter.intrinsicSize
                    val cy = size.height / 2f - 8.dp.toPx()
                    
                    if (painterSize.width > 0f && painterSize.height > 0f) {
                        val scaleFactor = iconSize / painterSize.width
                        scale(
                            scaleX = scaleFactor,
                            scaleY = scaleFactor,
                            pivot = Offset(cx, cy)
                        ) {
                            translate(
                                left = cx - painterSize.width / 2f,
                                top = cy - painterSize.height / 2f
                            ) {
                                painter.draw(
                                    drawScope = this,
                                    alpha = 1f,
                                    colorFilter = ColorFilter.tint(Color.White.copy(0.58f))
                                )
                            }
                        }
                    }
                    
                    val tl = textMeasurer.measure(item.label, textStyleInactive)
                    drawText(
                        textLayoutResult = tl,
                        topLeft = Offset(
                            cx - tl.size.width / 2f,
                            size.height / 2f + iconSize / 2f + gapPx - 8.dp.toPx()
                        )
                    )
                }
            }

            // PILL - Liquid Glass
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

            // IKON + LABEL AKTIF
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
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { scaleX = scaleAnim.value; scaleY = scaleAnim.value }
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

            // BUTTON AREA - Predictive + No Ripple + No Delay
            Row(Modifier.fillMaxSize()) {
                repeat(5) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(i) {
                                detectTapGestures(
                                    onPress = {
                                        predictiveIndex = i
                                        tryAwaitRelease()
                                        predictiveIndex = null
                                    },
                                    onTap = {
                                        if (selectedIndex != i) onSelect(i)
                                    }
                                )
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { if (selectedIndex != i) onSelect(i) }
                    )
                }
            }
        }
    }
}
