package com.jayra.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext

@Composable
fun InfoProfileHeader(
    name: String,
    subtitle: String,
    avatarUrl: String,
    onAvatarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val headerBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val nameColor = Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(headerBg)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 120dp Avatar
        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(JayraBlue)
                .then(
                    if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = nameColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            fontSize = 15.sp,
            color = JayraSubtext,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun InfoActionGrid(
    action1Icon: ImageVector,
    action1Label: String,
    onAction1Click: () -> Unit,
    action2Icon: ImageVector,
    action2Label: String,
    onAction2Click: () -> Unit,
    action3Icon: ImageVector,
    action3Label: String,
    onAction3Click: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionItem(
            icon = action1Icon,
            label = action1Label,
            onClick = onAction1Click,
            modifier = Modifier.weight(1f)
        )
        ActionItem(
            icon = action2Icon,
            label = action2Label,
            onClick = onAction2Click,
            modifier = Modifier.weight(1f)
        )
        ActionItem(
            icon = action3Icon,
            label = action3Label,
            onClick = onAction3Click,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = JayraBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = JayraBlue
        )
    }
}

@Composable
fun InfoSettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    valueText: String? = null,
    onClick: () -> Unit = {},
    showChevron: Boolean = true,
    showSeparator: Boolean = true,
    titleColor: Color? = null,
    testTag: String = ""
) {
    val isDark = isSystemInDarkTheme()
    val defaultTitleColor = if (isDark) Color.White else Color.Black
    val separatorColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                color = titleColor ?: defaultTitleColor,
                modifier = Modifier.weight(1f)
            )
            if (valueText != null) {
                Text(
                    text = valueText,
                    fontSize = 17.sp,
                    color = JayraSubtext
                )
                if (showChevron) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = JayraSubtext,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (showSeparator) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp)
                    .height(0.5.dp)
                    .background(separatorColor)
            )
        }
    }
}

@Composable
fun InfoSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showSeparator: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color.Black
    val separatorColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = JayraBlue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                )
            )
        }
        if (showSeparator) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp)
                    .height(0.5.dp)
                    .background(separatorColor)
            )
        }
    }
}

@Composable
fun ShimmerCardList(cardCount: Int = 5) {
    val transition = rememberInfiniteTransition(label = "shimmer_info")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val isDark = isSystemInDarkTheme()
    val shimmerColors = if (isDark) {
        listOf(Color(0xFF232E3C), Color(0xFF2E3D4F), Color(0xFF232E3C))
    } else {
        listOf(Color(0xFFE4E7EB), Color(0xFFF3F5F7), Color(0xFFE4E7EB))
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 200f, y = translateAnim - 200f),
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(cardCount) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
    }
}
