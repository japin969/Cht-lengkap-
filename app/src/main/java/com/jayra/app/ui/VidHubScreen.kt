package com.jayra.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.anime.AnimeHomeScreen
import com.jayra.app.ui.drama.DramaScreen
import com.jayra.app.ui.komik.KomikHomeScreen
import com.jayra.app.ui.theme.IOSBlue
import com.jayra.app.ui.theme.IOSBlueDark
import com.jayra.app.ui.theme.IOSGray6Dark
import com.jayra.app.ui.theme.IOSLabel
import com.jayra.app.ui.theme.IOSLabelDark
import com.jayra.app.ui.theme.IOSSecondaryLabel
import com.jayra.app.ui.theme.IOSSecondaryLabelDark
import com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark
import com.jayra.app.ui.theme.IOSSecondarySystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackgroundDark
import com.jayra.app.ui.theme.LiquidGlassSpecV2

@Composable
fun VidHubScreen(
    onDramaClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit,
    onKomikClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember { listOf("Drama", "Anime", "Komik") }

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val segBg = if (isDark) IOSGray6Dark else Color(0xFFE5E5EA)
    val pillBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue
    val borderColor = if (isDark) LiquidGlassSpecV2.DarkBorderTop else Color(0x14000000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "VidHub",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
        }

        // iOS Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(segBg)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val active = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) pillBg else Color.Transparent)
                        .then(
                            if (active) Modifier.border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) accentBlue else subColor
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> DramaScreen(onDramaClick = onDramaClick)
                1 -> AnimeHomeScreen(onAnimeClick = onAnimeClick)
                2 -> KomikHomeScreen(onKomikClick = onKomikClick)
            }
        }
    }
}
