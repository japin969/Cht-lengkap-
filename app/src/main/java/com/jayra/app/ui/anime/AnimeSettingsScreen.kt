package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.AnimeKompiSettingsData
import com.jayra.app.ui.theme.JayraTheme

// ============================================================================
// COLOR CONSTANTS
// ============================================================================

private val NetflixBlack = Color(0xFF000000)
private val NetflixDarkGray = Color(0xFF181818)
private val NetflixCardGray = Color(0xFF262626)
private val NetflixWhite = Color(0xFFFFFFFF)
private val NetflixTextPrimary = Color(0xFFFFFFFF)
private val NetflixTextSecondary = Color(0xFFB3B3B3)
private val NetflixTextMetadata = Color(0xFF8C8C8C)
private val NetflixRed = Color(0xFFE50914)
private val NetflixDivider = Color(0xFF262626)

// ============================================================================
// SETTINGS SCREEN
// ============================================================================

@Composable
fun AnimeSettingsScreen(
    viewModel: AnimeViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.settingsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            SettingsHeader(onBackClick = onBackClick)

            // Settings List
            if (state.isLoading) {
                SettingsSkeletonLoading()
            } else if (state.error != null) {
                SettingsErrorState(error = state.error!!, onRetry = { viewModel.loadSettings() })
            } else {
                SettingsList(settings = state.settings)
            }
        }
    }
}

// ============================================================================
// SETTINGS HEADER
// ============================================================================

@Composable
fun SettingsHeader(onBackClick: () -> Unit) {
    Surface(
        color = NetflixBlack,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }
            Text(
                text = "Pengaturan Anime",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NetflixWhite,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

// ============================================================================
// SETTINGS LIST
// ============================================================================

@Composable
fun SettingsList(settings: AnimeKompiSettingsData?) {
    val settings = settings ?: AnimeKompiSettingsData()

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Video Quality
        item {
            SettingsSection(title = "Video")
        }
        item {
            SettingsRow(
                icon = Icons.Default.HighQuality,
                title = "Kualitas Default",
                subtitle = settings.availableQualities?.joinToString(", ") ?: "Auto, 1080p, 720p, 480p, 360p",
                value = settings.defaultQuality ?: "Auto",
                onClick = { /* TODO: Show quality picker */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Subtitle
        item {
            SettingsRow(
                icon = Icons.Default.Subtitles,
                title = "Subtitle Default",
                subtitle = "Bahasa subtitle otomatis",
                value = settings.defaultSubtitle ?: "id",
                onClick = { /* TODO: Show subtitle picker */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Audio
        item {
            SettingsRow(
                icon = Icons.Default.Videocam,
                title = "Audio Default",
                subtitle = "Sub atau Dub",
                value = settings.defaultAudio ?: "sub",
                onClick = { /* TODO: Show audio picker */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Playback
        item {
            SettingsSection(title = "Pemutaran")
        }
        item {
            SettingsToggle(
                icon = Icons.Default.PlayArrow,
                title = "Auto Next Episode",
                subtitle = "Otomatis memutar episode berikutnya",
                checked = settings.autoNext ?: true,
                onCheckedChange = { /* TODO */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            SettingsToggle(
                icon = Icons.Default.PlayArrow,
                title = "Auto Play",
                subtitle = "Otomatis memutar video saat dibuka",
                checked = settings.autoPlay ?: true,
                onCheckedChange = { /* TODO */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Danmaku
        item {
            SettingsToggle(
                icon = Icons.Default.ClosedCaption,
                title = "Danmaku / Komentar Berjalan",
                subtitle = "Tampilkan komentar di atas video",
                checked = settings.danmakuEnabled ?: false,
                onCheckedChange = { /* TODO */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Skip Intro/Outro
        item {
            SettingsToggle(
                icon = Icons.Default.Forward10,
                title = "Skip Intro Otomatis",
                subtitle = "Lewati opening secara otomatis",
                checked = settings.skipIntroEnabled ?: true,
                onCheckedChange = { /* TODO */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            SettingsToggle(
                icon = Icons.Default.Forward10,
                title = "Skip Outro Otomatis",
                subtitle = "Lewati ending secara otomatis",
                checked = settings.skipOutroEnabled ?: true,
                onCheckedChange = { /* TODO */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Download
        item {
            SettingsSection(title = "Unduhan")
        }
        item {
            SettingsRow(
                icon = Icons.Default.FileDownload,
                title = "Kualitas Unduhan",
                subtitle = "Kualitas video saat diunduh",
                value = settings.downloadQuality ?: "720p",
                onClick = { /* TODO: Show download quality picker */ }
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // About
        item {
            SettingsSection(title = "Tentang")
        }
        item {
            SettingsRow(
                icon = Icons.Default.Info,
                title = "Versi Aplikasi",
                subtitle = "1.0.0",
                value = "",
                onClick = {}
            )
        }
        item {
            androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            SettingsRow(
                icon = Icons.Default.Web,
                title = "Sumber Data API",
                subtitle = "sankavollerei.web.id",
                value = "",
                onClick = {}
            )
        }
    }
}

// ============================================================================
// SETTINGS COMPONENTS
// ============================================================================

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = NetflixTextMetadata,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
fun SettingsRow(
    icon: androidx.compose.material.icons.filled.ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        color = NetflixBlack,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
                Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                    Text(title, fontSize = 15.sp, color = NetflixWhite)
                    Text(subtitle, fontSize = 13.sp, color = NetflixTextMetadata)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotBlank()) {
                    Text(value, fontSize = 13.sp, color = NetflixTextSecondary)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun SettingsToggle(
    icon: androidx.compose.material.icons.filled.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = NetflixBlack,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
                Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                    Text(title, fontSize = 15.sp, color = NetflixWhite)
                    Text(subtitle, fontSize = 13.sp, color = NetflixTextMetadata)
                }
            }
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NetflixRed,
                    checkedTrackColor = NetflixRed.copy(alpha = 0.5f),
                    uncheckedThumbColor = NetflixTextMetadata,
                    uncheckedTrackColor = NetflixCardGray
                )
            )
        }
    }
}

// ============================================================================
// SKELETON & ERROR
// ============================================================================

@Composable
fun SettingsSkeletonLoading() {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        repeat(15) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp)
                        .background(NetflixCardGray)
                )
            }
            item {
                androidx.compose.material3.Divider(color = NetflixDivider, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun SettingsErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Gagal memuat pengaturan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
        Spacer(Modifier.height(6.dp))
        Text(error, fontSize = 13.sp, color = NetflixTextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) { Text("Coba Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}