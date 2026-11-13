package com.jayra.app.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.theme.*
import com.jayra.app.util.glassSurface
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState

// ═══════════════════════════════════════════════════════════
// JAYRA-UI v2.0 — iOS Auth Components (Liquid Glass Edition)
// ═══════════════════════════════════════════════════════════

@Composable
fun IOSPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "ios_button_scale"
    )
    val bgColor = if (enabled) IOSBlue else IOSGray3
    val glassState = rememberLiquidGlassState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .glassSurface(solidColor = bgColor, cornerRadius = 12.dp, state = glassState)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
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

@Composable
fun IOSTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = IOSBlue,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.clickable { onClick() }
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
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(solidColor = bg, cornerRadius = 12.dp, state = glassState),
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
    val dark = isSystemInDarkTheme()
    val labelColor = if (isError) IOSRed else if (dark) IOSSecondaryLabelDark else IOSSecondaryLabel
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword) {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.Visibility
                        else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
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

// Backward compatibility wrappers
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
