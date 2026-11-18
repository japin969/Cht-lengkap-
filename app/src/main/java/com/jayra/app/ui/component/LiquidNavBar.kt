package com.jayra.app.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.R

data class JayraNavItem(
    val label: String,
    @androidx.annotation.DrawableRes val iconRes: Int
)

val jayraNavItems = listOf(
    JayraNavItem("Chat", R.drawable.ic_chat),
    JayraNavItem("Calls", R.drawable.ic_panggilan),
    JayraNavItem("Music", R.drawable.ic_music),
    JayraNavItem("Hub", R.drawable.ic_hub),
    JayraNavItem("Settings", R.drawable.ic_setting)
)

/**
 * Bottom nav "liquid glass" TANPA BLUR.
 *
 * Efek kaca dibentuk murni dari:
 *  - background semi transparan (putih 80% / hitam 70%)
 *  - gradient highlight putih 20% → 5% (sheen kaca)
 *  - border 1dp putih 26%
 *  - shadow tipis 3dp (bukan 8–12dp, biar tetap ringan di HP kentang)
 *
 * Animasi: hanya scale 0.95 → 1.0 + fade, 160ms FastOutSlowInEasing.
 * Tidak ada VideoView, RenderEffect, atau blur library — sekali render, nol GPU cost.
 */
@Composable
fun LiquidNavBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isFolded: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(22.dp)

    val glassBase = if (isDark) Color(0xB3000000) else Color(0xCCFFFFFF)
    val sheen = if (isDark) {
        Brush.verticalGradient(
            listOf(Color(0x2BFFFFFF), Color(0x0DFFFFFF), Color.Transparent)
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0x33FFFFFF), Color(0x14FFFFFF), Color(0x0AFFFFFF))
        )
    }
    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x42FFFFFF)
    val fallbackBorder = if (isDark) Color(0x14FFFFFF) else Color(0x1A000000)
    val activeColor = if (isDark) Color(0xFF30D158) else Color(0xFF007AFF)
    val inactiveColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF8A8A8E)

    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 3.dp, shape = shape, clip = false)
            .clip(shape)
            .background(glassBase)
            .drawBehind {
                drawRect(brush = sheen)
                // inner highlight atas (kilau kaca)
                drawLine(
                    color = Color.White.copy(alpha = if (isDark) 0.10f else 0.45f),
                    start = Offset(0f, 1f),
                    end = Offset(size.width, 1f),
                    strokeWidth = 1.5f
                )
            }
            .border(width = 1.dp, color = if (borderColor.alpha > 0f) borderColor else fallbackBorder, shape = shape)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            jayraNavItems.forEachIndexed { i, item ->
                val selected = i == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                val pillShape = remember { RoundedCornerShape(16.dp) }
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.95f,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                    label = "nav-scale-$i"
                )

                Column(
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(pillShape)
                        .background(if (selected) activeColor.copy(alpha = if (isDark) 0.18f else 0.12f) else Color.Transparent)
                        .clickable(
                            interactionSource = interaction,
                            indication = LocalIndication.current
                        ) {
                            if (selectedIndex != i) onSelect(i)
                        }
                        .scale(scale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        tint = if (selected) activeColor else inactiveColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = if (selected) activeColor else inactiveColor,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = com.jayra.app.ui.theme.SFProFontFamily,
                        lineHeight = 10.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    // Dot kecil penanda tab aktif (hilang total saat tidak aktif)
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(if (selected) activeColor else Color.Transparent)
                    )
                }
            }
        }
    }
}
