package com.jayra.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * Hub — pintu masuk ke tiga fitur media.
 * Hanya judul "HUB" (SF Pro) + 3 card: anime, comic, strem (huruf kecil).
 * TIDAK ada bottom nav. Klik card → masuk fitur. Back → Chat.
 * Pola: launcher/hub sederhana seperti aplikasi agregator media populer.
 */
@Composable
fun HubScreen(
    onAnimeClick: () -> Unit,
    onComicClick: () -> Unit,
    onStremClick: () -> Unit,
    onNekoClick: () -> Unit = {},
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0B12), Color(0xFF08080C))
                )
            )
    ) {
        // Header HUB + back
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MediaColors.TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "HUB",
                fontFamily = SFProFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = MediaColors.TextPrimary
            )
        }

        // 3 card
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HubCard(
                label = "anime",
                icon = Icons.Rounded.PlayCircle,
                accent = MediaColors.Anime,
                onClick = onAnimeClick
            )
            Spacer(Modifier.height(18.dp))
            HubCard(
                label = "comic",
                icon = Icons.Rounded.AutoStories,
                accent = MediaColors.Comic,
                onClick = onComicClick
            )
            Spacer(Modifier.height(18.dp))
            HubCard(
                label = "strem",
                icon = Icons.Rounded.Movie,
                accent = MediaColors.Strem,
                onClick = onStremClick
            )
            Spacer(Modifier.height(18.dp))
            HubCard(
                label = "neko",
                icon = Icons.Rounded.AutoStories,
                accent = MediaColors.Anime,
                onClick = onNekoClick
            )
        }
    }
}

@Composable
private fun HubCard(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.18f), MediaColors.Surface)
                )
            )
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = accent, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(18.dp))
        Text(
            label,
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MediaColors.TextPrimary
        )
        Spacer(Modifier.weight(1f))
        Text("›", fontFamily = SFProFontFamily, fontSize = 28.sp, color = accent)
    }
}