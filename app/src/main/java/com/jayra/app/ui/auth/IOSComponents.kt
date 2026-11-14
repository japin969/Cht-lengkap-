package com.jayra.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.util.GlassTier
import com.jayra.app.util.glassSurface
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState

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
 * iOS 26 wallpaper backdrop — 1 vertical gradient + 1 radial orb.
 * HIGH/MEDIUM tier: ambient drift (deferred read, zero recomposition).
 * LOW/OFF tier: static (no drift), for 3GB RAM no-lag guarantee.
 */
@Composable
fun IOSAuthBackdrop(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val tier = remember(GlassEffectPrefs.enabledState.value) {
        GlassEffectPrefs.resolveTier()
    }
    val animateOrb = tier == GlassTier.HIGH || tier == GlassTier.MEDIUM

    val gradientTop = if (dark) Color(0xFF0A0A14) else Color(0xFFE8F0FC)
    val gradientBottom = if (dark) Color(0xFF000000) else Color(0xFFF2F2F7)
    val orbColor = if (dark) {
        Color(0xFF0A84FF).copy(alpha = 0.16f)
    } else {
        Color(0xFF007AFF).copy(alpha = 0.10f)
    }

    // Transition always runs; value only read inside graphicsLayer when animateOrb.
    val ambient = rememberInfiniteTransition(label = "auth_ambient")
    val orbDxState = ambient.animateFloat(
        initialValue = 0f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_dx"
    )
    val orbDyState = ambient.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_dy"
    )

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(gradientTop, gradientBottom)))
    ) {
        Box(
            Modifier
                .size(500.dp)
                .align(Alignment.TopEnd)
                .offset(x = 250.dp, y = (-250).dp)
                .graphicsLayer {
                    if (animateOrb) {
                        translationX = orbDxState.value
                        translationY = orbDyState.value
                    }
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(orbColor, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
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
fun IOSWordmark(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    // Deferred read pattern — scale/alpha only applied in draw phase.
    val scaleState = animateFloatAsState(
        targetValue = if (appeared) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "wordmark_scale"
    )
    val alphaState = animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "wordmark_alpha"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scaleState.value
                scaleY = scaleState.value
                alpha = alphaState.value
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors = listOf(IOSBlue, IOSIndigo)))
            .iosLiquidGlass(18.dp, dark),
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

/**
 * Dual-mode spinner dispatcher.
 *  - LOW / OFF tier  → IOSSpinnerCanvas (Mode 1: graphicsLayer, zero-alloc per frame)
 *  - MEDIUM / HIGH   → IOSSpinnerLottie (Mode 2: Lottie asset loading-v4.json)
 * Contract unchanged: IOSPrimaryButton tidak tahu mode mana yang aktif.
 */
@Composable
private fun IOSSpinner(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    val tier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
    if (tier == GlassTier.LOW || tier == GlassTier.OFF) {
        IOSSpinnerCanvas(modifier, color, size, strokeWidth)
    } else {
        IOSSpinnerLottie(modifier, size)
    }
}

/** Mode 1 — Canvas spinner. Draw-only, no per-frame recomposition. */
@Composable
private fun IOSSpinnerCanvas(
    modifier: Modifier,
    color: Color,
    size: Dp,
    strokeWidth: Dp
) {
    val transition = rememberInfiniteTransition(label = "ios_spinner")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ios_spinner_angle"
    )
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = angle.value }
    ) {
        val strokePx = strokeWidth.toPx()
        val radius = (this.size.minDimension - strokePx) / 2f
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = radius,
            style = Stroke(width = strokePx)
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(strokePx / 2f, strokePx / 2f),
            size = Size(this.size.width - strokePx, this.size.height - strokePx),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

/** Mode 2 — Lottie spinner. Fallback ke Canvas jika asset gagal load (anti blank-screen). */
@Composable
private fun IOSSpinnerLottie(modifier: Modifier, size: Dp) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("loading-v4.json")
    )
    val comp = composition
    if (comp != null) {
        val progress by animateLottieCompositionAsState(
            composition = comp,
            iterations = LottieConstants.IterateForever
        )
        LottieAnimation(
            composition = comp,
            progress = { progress },
            modifier = modifier.size(size)
        )
    } else {
        IOSSpinnerCanvas(modifier, Color.White, size, 2.dp)
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
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.95f else 1f,
        animationSpec = JellySpring,
        label = "ios_button_scale"
    )
    val bgColor = if (enabled) IOSBlue else if (dark) IOSGray3Dark else IOSGray3
    val glassState = rememberLiquidGlassState()
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(shape)
            .glassSurface(solidColor = bgColor, cornerRadius = 12.dp, state = glassState)
            .iosLiquidGlass(12.dp, dark)
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
                IOSSpinner(
                    color = Color.White,
                    size = 20.dp,
                    strokeWidth = 2.dp
                )
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
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.35f else 1f,
        animationSpec = SmoothSpring,
        label = "ios_text_button_alpha"
    )

    Text(
        text = text,
        color = IOSBlue,
        fontSize = 15.sp,
        fontWeight = fontWeight,
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
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
    val bg = if (dark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemBackground
    val glassState = rememberLiquidGlassState()
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .glassSurface(solidColor = bg, cornerRadius = 12.dp, state = glassState)
            .iosLiquidGlass(12.dp, dark),
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val view = LocalView.current
    val dark = isSystemInDarkTheme()

    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> IOSRed
            isFocused -> IOSBlue
            dark -> IOSSecondaryLabelDark
            else -> IOSSecondaryLabel
        },
        animationSpec = tween(200),
        label = "ios_field_label_color"
    )
    val textColor = if (dark) IOSLabelDark else IOSLabel
    val placeholderColor = if (dark) IOSGray2Dark else IOSGray2
    val iconTint = if (dark) IOSGray2Dark else IOSGray2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            letterSpacing = 0.4.sp
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    cursorBrush = SolidColor(IOSBlue),
                    keyboardOptions = keyboardOptions,
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
                            tint = iconTint,
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
            .background(if (dark) IOSSeparatorDark else IOSSeparatorOpaque)
    )
}

@Composable
fun IOSErrorMessage(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IOSRed.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = IOSRed,
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
