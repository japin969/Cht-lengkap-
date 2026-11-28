package com.jayra.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.hub.HubCard
import com.jayra.app.ui.theme.SFProFontFamily

@Composable
fun VidHubScreen(
    onAnimeClick: () -> Unit,
    onDonghubClick: () -> Unit,
    onDonghuaClick: () -> Unit,
    onKomikClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bg = if (isDark)
        Brush.verticalGradient(listOf(Color(0xFF0A0A0F), Color(0xFF12121A)))
    else
        Brush.verticalGradient(listOf(Color(0xFFF5F5F7), Color(0xFFEDEDF0)))

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Hub",
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            color = if (isDark) Color.White else Color(0xFF1C1C1E)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Pilih fitur",
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF636366)
        )
        Spacer(Modifier.height(24.dp))

        val cards = listOf(
            Triple("Donghub", "Donghua subtitle Indonesia", Color(0xFFFF6B35)) to onDonghubClick,
            Triple("Donghua", "Animasi Tiongkok", Color(0xFF00B4D8)) to onDonghuaClick,
            Triple("Anime", "Anime subtitle Indonesia", Color(0xFF8B5CF6)) to onAnimeClick,
            Triple("Komik", "Segera hadir", Color(0xFF10B981)) to onKomikClick
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            itemsIndexed(cards, key = { index, _ -> index }) { index, (info, click) ->
                val (title, subtitle, color) = info
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.fadeIn(tween(300, delayMillis = index * 50)) +
                            androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = index * 50))
                ) {
                    HubCard(
                        title = title,
                        subtitle = subtitle,
                        accentColor = color,
                        icon = if (title == "Komik") Icons.Rounded.MenuBook else Icons.Rounded.PlayArrow,
                        enabled = title != "Komik",
                        onClick = click
                    )
                }
            }
        }
    }
}