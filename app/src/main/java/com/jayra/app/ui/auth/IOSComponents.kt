package com.jayra.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.theme.*
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.util.GlassTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════
// JAYRA-UI v2.4 — iOS 26 "Auth Shared Kit"
// ═══════════════════════════════════════════════════════════
// v2.4 delta dari v2.3:
//  • IOSAuthBackdrop: ambient orb drift (deferred read, tier-gated)
//  • IOSWordmark: entrance scale + fade (plays once on mount)
//  • IOSGroupedTextField: password icon crossfade 150ms
// ═══════════════════════════════════════════════════════════

private val JellySpring = spring<Float>(dampingRatio = 0.55f, stiffness = 450f)
private val SmoothSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 400f)

/**
 * iOS 18 Glass atmospheric backdrop (zero blur, 30fps-throttled GPU drift).
 * Uses drawWithCache so brushes allocate once and frames only update graphicsLayer.
 */
@Composable
fun IOSAuthBackdrop(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val tier = remember(GlassEffectPrefs.enabledState.value) {
        GlassEffectPrefs.resolveTier()
    }
    val glassEnabled = tier != GlassTier.OFF

    val topBg = if (dark) Color(0xFF0A0F1F) else Color(0xFFEEF4FF)
    val midBg = if (dark) Color(0xFF060A15) else Color(0xFFE3ECFD)
    val botBg = if (dark) Color(0xFF02040A) else Color(0xFFD6E2F9)

    val orbCool = if (dark) Color(0xFF0A84FF).copy(alpha = 0.28f) else Color(0xFF56C2FF).copy(alpha = 0.34f)
    val orbIndigo = if (dark) Color(0xFF5E5CE6).copy(alpha = 0.26f) else Color(0xFF6B66FF).copy(alpha = 0.24f)
    val orbWarm = if (dark) Color(0xFFFF9F0A).copy(alpha = 0.14f) else Color(0xFFFFB478).copy(alpha = 0.24f)

    val phaseState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(glassEnabled) {
        if (!glassEnabled) return@LaunchedEffect
        var lastFrameMs = 0L
        val twoPi = (2.0 * PI).toFloat()
        while (isActive) {
            withFrameMillis { frameMs ->
                if (lastFrameMs == 0L) lastFrameMs = frameMs
                val deltaMs = frameMs - lastFrameMs
                if (deltaMs >= 33L) {
                    lastFrameMs = frameMs
                    phaseState.floatValue = (phaseState.floatValue + deltaMs * 0.00034f) % twoPi
                }
            }
        }
    }

    key(dark) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithCache {
                    val bgBrush = Brush.verticalGradient(
                        0f to topBg,
                        0.55f to midBg,
                        1f to botBg
                    )
                    onDrawBehind {
                        drawRect(brush = bgBrush)
                    }
                }
        ) {
            if (glassEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val phase = phaseState.floatValue
                            val driftPx = 28f
                            translationX = sin(phase) * driftPx
                            translationY = sin(2f * phase) * (driftPx * 0.65f)
                        }
                        .drawWithCache {
                            val minDim = size.minDimension.coerceAtLeast(1f)
                            val c1 = Offset(size.width * 0.86f, size.height * 0.14f)
                            val r1 = minDim * 0.75f
                            val b1 = Brush.radialGradient(
                                colors = listOf(orbCool, Color.Transparent),
                                center = c1,
                                radius = r1
                            )

                            val c2 = Offset(size.width * 0.14f, size.height * 0.82f)
                            val r2 = minDim * 0.80f
                            val b2 = Brush.radialGradient(
                                colors = listOf(orbIndigo, Color.Transparent),
                                center = c2,
                                radius = r2
                            )

                            val c3 = Offset(size.width * 0.82f, size.height * 0.54f)
                            val r3 = minDim * 0.56f
                            val b3 = Brush.radialGradient(
                                colors = listOf(orbWarm, Color.Transparent),
                                center = c3,
                                radius = r3
                            )

                            onDrawBehind {
                                if (size.minDimension > 0f) {
                                    drawCircle(brush = b1, radius = r1, center = c1)
                                    drawCircle(brush = b2, radius = r2, center = c2)
                                    drawCircle(brush = b3, radius = r3, center = c3)
                                }
                            }
                        }
                )
            }
        }
    }
}

internal fun Modifier.iosLiquidGlass(
    cornerRadius: Dp,
    dark: Boolean
): Modifier {
    val specularAlpha = if (dark) 0.32f else 0.20f
    val borderTopAlpha = if (dark) 0.42f else 0.30f
    val borderBotAlpha = if (dark) 0.08f else 0.06f

    return this
        .drawWithContent {
            drawContent()
            val cornerPx = cornerRadius.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = specularAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 24.dp.toPx()
                ),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                size = size
            )
        }
        .border(
            width = 0.6.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderTopAlpha),
                    Color.White.copy(alpha = borderBotAlpha),
                    Color.White.copy(alpha = borderTopAlpha * 0.5f)
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}

@Composable
fun Modifier.iosGlassEntrance(delayMillis: Int = 0): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }
    val alphaState = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "glass_entrance_alpha"
    )
    val offsetYState = animateFloatAsState(
        targetValue = if (visible) 0f else 26f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "glass_entrance_y"
    )
    val scaleState = animateFloatAsState(
        targetValue = if (visible) 1f else 0.965f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "glass_entrance_scale"
    )
    return this.graphicsLayer {
        alpha = alphaState.value
        translationY = offsetYState.value
        scaleX = scaleState.value
        scaleY = scaleState.value
    }
}

@Composable
fun IOSGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val tier = remember(GlassEffectPrefs.enabledState.value) {
        GlassEffectPrefs.resolveTier()
    }
    val glassEnabled = tier != GlassTier.OFF

    val glassModifier = remember(dark, glassEnabled, cornerRadius) {
        val shape = RoundedCornerShape(cornerRadius)
        val topTint = when {
            !glassEnabled && dark -> Color(0xFF1C1C1E)
            !glassEnabled -> Color.White
            dark -> Color(0xFF1E283D).copy(alpha = 0.74f)
            else -> Color.White.copy(alpha = 0.78f)
        }
        val midTint = when {
            !glassEnabled && dark -> Color(0xFF1C1C1E)
            !glassEnabled -> Color.White
            dark -> Color(0xFF141B2B).copy(alpha = 0.64f)
            else -> Color(0xFFF5F8FF).copy(alpha = 0.64f)
        }
        val botTint = when {
            !glassEnabled && dark -> Color(0xFF1C1C1E)
            !glassEnabled -> Color.White
            dark -> Color(0xFF0E1422).copy(alpha = 0.58f)
            else -> Color(0xFFE9F0FF).copy(alpha = 0.56f)
        }
        val specularTop = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.62f)
        val causticCorner = if (dark) Color(0xFF5E5CE6).copy(alpha = 0.10f) else Color.White.copy(alpha = 0.34f)
        val rimStart = if (dark) Color.White.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.90f)
        val rimMid = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.24f)
        val rimEnd = if (dark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.45f)
        val shadowTint = if (dark) Color(0xFF000000).copy(alpha = 0.60f) else Color(0xFF3E5B99).copy(alpha = 0.18f)

        Modifier
            .then(
                if (glassEnabled) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = shadowTint,
                        spotColor = shadowTint
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .drawWithCache {
                val radiusPx = cornerRadius.toPx()
                val corner = CornerRadius(radiusPx, radiusPx)
                val bodyBrush = Brush.verticalGradient(
                    0f to topTint,
                    0.52f to midTint,
                    1f to botTint
                )
                val sheenBrush = Brush.verticalGradient(
                    colors = listOf(specularTop, Color.Transparent),
                    startY = 0f,
                    endY = 38.dp.toPx()
                )
                val causticRadius = (size.minDimension * 0.65f).coerceAtLeast(1f)
                val causticCenter = Offset(size.width * 0.92f, size.height * 0.96f)
                val causticBrush = Brush.radialGradient(
                    colors = listOf(causticCorner, Color.Transparent),
                    center = causticCenter,
                    radius = causticRadius
                )
                val rimStrokePx = 1.dp.toPx()
                val rimBrush = Brush.linearGradient(
                    0f to rimStart,
                    0.48f to rimMid,
                    1f to rimEnd
                )
                onDrawBehind {
                    drawRoundRect(brush = bodyBrush, cornerRadius = corner, size = size)
                    if (glassEnabled && size.minDimension > 0f) {
                        drawRoundRect(brush = sheenBrush, cornerRadius = corner, size = size)
                        drawCircle(brush = causticBrush, radius = causticRadius, center = causticCenter)
                        drawRoundRect(
                            brush = rimBrush,
                            topLeft = Offset(rimStrokePx / 2f, rimStrokePx / 2f),
                            size = Size(size.width - rimStrokePx, size.height - rimStrokePx),
                            cornerRadius = corner,
                            style = Stroke(width = rimStrokePx)
                        )
                    }
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(glassModifier)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun IOSWordmark(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val scaleState = animateFloatAsState(
        targetValue = if (appeared) 1f else 0.84f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 380f),
        label = "wordmark_scale"
    )
    val alphaState = animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "wordmark_alpha"
    )

    val tier = remember(GlassEffectPrefs.enabledState.value) {
        GlassEffectPrefs.resolveTier()
    }
    val glassEnabled = tier != GlassTier.OFF
    val tileModifier = remember(dark, glassEnabled) {
        val cornerDp = 22.dp
        val shape = RoundedCornerShape(cornerDp)
        val glowColor = IOSBlue.copy(alpha = if (dark) 0.42f else 0.30f)
        Modifier
            .then(
                if (glassEnabled) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = glowColor,
                        spotColor = glowColor
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .drawWithCache {
                val radiusPx = cornerDp.toPx()
                val corner = CornerRadius(radiusPx, radiusPx)
                val bgBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2896FF),
                        IOSBlue,
                        IOSIndigo
                    )
                )
                val topGloss = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.38f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.52f
                )
                val rimStrokePx = 1.dp.toPx()
                val rimBrush = Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.65f),
                    0.5f to Color.White.copy(alpha = 0.16f),
                    1f to Color.White.copy(alpha = 0.35f)
                )
                onDrawBehind {
                    drawRoundRect(brush = bgBrush, cornerRadius = corner, size = size)
                    if (glassEnabled && size.minDimension > 0f) {
                        drawRoundRect(brush = topGloss, cornerRadius = corner, size = size)
                        drawRoundRect(
                            brush = rimBrush,
                            topLeft = Offset(rimStrokePx / 2f, rimStrokePx / 2f),
                            size = Size(size.width - rimStrokePx, size.height - rimStrokePx),
                            cornerRadius = corner,
                            style = Stroke(width = rimStrokePx)
                        )
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .size(74.dp)
            .graphicsLayer {
                scaleX = scaleState.value
                scaleY = scaleState.value
                alpha = alphaState.value
            }
            .then(tileModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "J",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp
        )
    }
}

@Composable
fun IOSPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = JellySpring,
        label = "ios_button_scale"
    )
    val pressAlphaState = animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.86f else 1f,
        animationSpec = SmoothSpring,
        label = "ios_button_press_alpha"
    )
    val tier = remember(GlassEffectPrefs.enabledState.value) {
        GlassEffectPrefs.resolveTier()
    }
    val glassEnabled = tier != GlassTier.OFF
    val buttonModifier = remember(dark, glassEnabled, enabled) {
        val cornerDp = 16.dp
        val shape = RoundedCornerShape(cornerDp)
        val glowTint = IOSBlue.copy(alpha = if (dark) 0.40f else 0.28f)
        val fillStart = if (enabled) Color(0xFF1D8EFF) else IOSBlue.copy(alpha = 0.35f)
        val fillMid = if (enabled) IOSBlue else IOSBlue.copy(alpha = 0.30f)
        val fillEnd = if (enabled) Color(0xFF4A56E2) else IOSIndigo.copy(alpha = 0.30f)
        Modifier
            .then(
                if (glassEnabled && enabled) {
                    Modifier.shadow(10.dp, shape, clip = false, ambientColor = glowTint, spotColor = glowTint)
                } else Modifier
            )
            .clip(shape)
            .drawWithCache {
                val radiusPx = cornerDp.toPx()
                val corner = CornerRadius(radiusPx, radiusPx)
                val fillBrush = Brush.linearGradient(colors = listOf(fillStart, fillMid, fillEnd))
                val topSheen = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.55f
                )
                val strokePx = 1.dp.toPx()
                val rimBrush = Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.52f),
                    0.5f to Color.White.copy(alpha = 0.14f),
                    1f to Color.White.copy(alpha = 0.28f)
                )
                onDrawBehind {
                    drawRoundRect(brush = fillBrush, cornerRadius = corner, size = size)
                    if (glassEnabled && enabled && size.minDimension > 0f) {
                        drawRoundRect(brush = topSheen, cornerRadius = corner, size = size)
                        drawRoundRect(
                            brush = rimBrush,
                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                            size = Size(size.width - strokePx, size.height - strokePx),
                            cornerRadius = corner,
                            style = Stroke(width = strokePx)
                        )
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scaleState.value
                scaleY = scaleState.value
                alpha = (if (enabled) 1f else 0.55f) * pressAlphaState.value
            }
            .then(buttonModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "ios_button_state"
        ) { loading ->
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Memuat...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.2).sp
                    )
                }
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}

@Composable
fun IOSTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Medium
) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alphaState = animateFloatAsState(
        targetValue = if (isPressed) 0.40f else 1f,
        animationSpec = SmoothSpring,
        label = "ios_text_button_alpha"
    )
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = SmoothSpring,
        label = "ios_text_button_scale"
    )

    Text(
        text = text,
        color = if (dark) IOSBlueDark else IOSBlue,
        fontSize = 15.sp,
        fontWeight = fontWeight,
        modifier = modifier
            .graphicsLayer {
                alpha = alphaState.value
                scaleX = scaleState.value
                scaleY = scaleState.value
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            )
    )
}

@Composable
fun IOSFieldGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val tier = remember(GlassEffectPrefs.enabledState.value) {
        GlassEffectPrefs.resolveTier()
    }
    val glassEnabled = tier != GlassTier.OFF
    val wellModifier = remember(dark, glassEnabled) {
        val cornerDp = 16.dp
        val shape = RoundedCornerShape(cornerDp)
        val fillTop = when {
            !glassEnabled && dark -> IOSSecondarySystemBackgroundDark
            !glassEnabled -> IOSSecondarySystemBackground
            dark -> Color(0xFF232D44).copy(alpha = 0.60f)
            else -> Color.White.copy(alpha = 0.68f)
        }
        val fillBot = when {
            !glassEnabled && dark -> IOSSecondarySystemBackgroundDark
            !glassEnabled -> IOSSecondarySystemBackground
            dark -> Color(0xFF1A2234).copy(alpha = 0.50f)
            else -> Color(0xFFF6F9FF).copy(alpha = 0.54f)
        }
        val rimStart = if (dark) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.85f)
        val rimEnd = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.30f)
        Modifier
            .clip(shape)
            .drawWithCache {
                val radiusPx = cornerDp.toPx()
                val corner = CornerRadius(radiusPx, radiusPx)
                val fillBrush = Brush.verticalGradient(colors = listOf(fillTop, fillBot))
                val strokePx = 0.85.dp.toPx()
                val rimBrush = Brush.linearGradient(colors = listOf(rimStart, rimEnd))
                onDrawBehind {
                    drawRoundRect(brush = fillBrush, cornerRadius = corner, size = size)
                    if (glassEnabled && size.minDimension > 0f) {
                        drawRoundRect(
                            brush = rimBrush,
                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                            size = Size(size.width - strokePx, size.height - strokePx),
                            cornerRadius = corner,
                            style = Stroke(width = strokePx)
                        )
                    }
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(wellModifier),
        content = content
    )
}

@Composable
fun IOSGroupedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    prefix: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    val accent = if (dark) IOSBlueDark else IOSBlue

    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> if (dark) IOSRedDark else IOSRed
            isFocused -> accent
            dark -> IOSSecondaryLabelDark
            else -> IOSSecondaryLabel
        },
        animationSpec = tween(180),
        label = "ios_field_label_color"
    )
    val rowTint by animateColorAsState(
        targetValue = when {
            isError -> IOSRed.copy(alpha = if (dark) 0.10f else 0.06f)
            isFocused -> accent.copy(alpha = if (dark) 0.10f else 0.055f)
            else -> Color.Transparent
        },
        animationSpec = tween(180),
        label = "ios_field_row_tint"
    )
    val textColor = if (dark) IOSLabelDark else IOSLabel
    val placeholderColor = if (dark) IOSGray2Dark else IOSGray2
    val iconTint = if (dark) IOSGray2Dark else IOSGray2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowTint)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!prefix.isNullOrEmpty()) {
                Text(
                    text = prefix,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused || value.isNotEmpty()) accent else placeholderColor,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 17.sp,
                        color = placeholderColor
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = SFProFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = if (isPassword && !passwordVisible)
                        PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            if (isPassword) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        passwordVisible = !passwordVisible
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    AnimatedContent(
                        targetState = passwordVisible,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "password_icon_crossfade"
                    ) { visible ->
                        Icon(
                            imageVector = if (visible) Icons.Rounded.Visibility
                            else Icons.Rounded.VisibilityOff,
                            contentDescription = if (visible) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                            tint = if (visible) accent else iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IOSDivider(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(0.5.dp)
            .background(
                if (dark) Color.White.copy(alpha = 0.12f)
                else Color(0xFF3C3C43).copy(alpha = 0.14f)
            )
    )
}

@Composable
fun IOSErrorMessage(message: String, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val errColor = if (dark) IOSRedDark else IOSRed
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(errColor.copy(alpha = if (dark) 0.16f else 0.12f))
            .border(
                width = 0.8.dp,
                color = errColor.copy(alpha = 0.34f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = errColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            color = errColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Backward compat wrappers ──────────────────────────────

@Composable
fun IOSButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) = IOSPrimaryButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    isLoading = isLoading
)

@Deprecated("Use IOSGroupedTextField")
@Composable
fun IOSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isPassword: Boolean = false,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) = IOSGroupedTextField(
    label = placeholder,
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    placeholder = "",
    isPassword = isPassword,
    isError = isError,
    keyboardOptions = keyboardOptions
)
