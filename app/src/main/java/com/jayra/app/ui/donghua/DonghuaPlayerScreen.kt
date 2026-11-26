package com.jayra.app.ui.donghua

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.NativeVideoPlayer
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * DonghuaPlayer — 100% NATIVE (ExoPlayer via NativeVideoPlayer). TANPA WebView.
 * Server embed (ok.ru/rumble) tidak bisa diputar native -> tampilkan error jelas
 * + Retry + Ganti Server (bukan hitam, bukan WebView).
 */
@Composable
fun DonghuaPlayerScreen(
    videoUrl: String,
    title: String = "Memutar Donghua",
    onBack: () -> Unit,
    viewModel: DonghuaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentUrl by remember(videoUrl) { mutableStateOf(videoUrl) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(videoUrl) { viewModel.loadStream(videoUrl) }
    LaunchedEffect(state.streamUrl) { state.streamUrl?.let { currentUrl = it } }

    val servers = state.servers.ifEmpty { listOf("Server 1" to videoUrl) }
    val isDirect = currentUrl.startsWith("http") &&
        (currentUrl.contains(".mp4") || currentUrl.contains(".m3u8") ||
            currentUrl.contains("googlevideo") || currentUrl.contains("commondatastorage"))

    LaunchedEffect(currentUrl) { failed = false }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.isLoading && state.streamUrl == null && !videoUrl.startsWith("http") -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MediaColors.Strem)
                }
            }
            isDirect -> {
                NativeVideoPlayer(
                    videoUrl = currentUrl,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                NativeError(
                    message = "Sumber embed tidak dapat diputar native. Pilih server lain (mp4/m3u8).",
                    servers = servers,
                    onRetry = { failed = false; currentUrl = videoUrl },
                    onPick = { currentUrl = it },
                    onBack = onBack
                )
            }
        }

        // Back button
        Box(
            Modifier.statusBarsPadding().padding(8.dp).size(40.dp).clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.4f)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun NativeError(
    message: String,
    servers: List<Pair<String, String>>,
    onRetry: () -> Unit,
    onPick: (String) -> Unit,
    onBack: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Refresh, null, tint = MediaColors.Strem, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Tidak bisa memutar", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(message, fontFamily = SFProFontFamily, fontSize = 13.sp, color = Color(0xFFB0B0B8),
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MediaColors.Strem).clickable { onRetry() }
                .padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Retry", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 14.sp)
            }
            if (servers.size > 1) {
                Spacer(Modifier.height(14.dp))
                servers.forEach { (label, url) ->
                    Text(label, fontFamily = SFProFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        color = MediaColors.Strem, modifier = Modifier.padding(vertical = 4.dp).clickable { onPick(url) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Kembali", fontFamily = SFProFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                color = Color.White, modifier = Modifier.clickable { onBack() })
        }
    }
}