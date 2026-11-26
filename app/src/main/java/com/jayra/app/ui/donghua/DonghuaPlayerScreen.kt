package com.jayra.app.ui.donghua

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * DonghuaPlayer — server embed (ok.ru/rumble/dst) dibuka via WebView; mp4 langsung via ExoPlayer.
 * Loading jelas, error + retry + ganti server. Tidak hitam.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DonghuaPlayerScreen(
    videoUrl: String,
    title: String = "Memutar Donghua",
    onBack: () -> Unit,
    viewModel: DonghuaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentUrl by remember(videoUrl) { mutableStateOf(videoUrl) }
    var isLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(videoUrl) { viewModel.loadStream(videoUrl) }

    // Prioritas server dari state (jika ada), else pakai videoUrl.
    val servers = state.servers.ifEmpty { listOf("Server 1" to videoUrl) }
    val activeUrl = if (state.streamUrl != null) currentUrl else videoUrl
    val isEmbed = activeUrl.contains("ok.ru") || activeUrl.contains("rumble") ||
        activeUrl.contains("d.tube") || activeUrl.contains("odysee") || activeUrl.contains("embed")

    LaunchedEffect(activeUrl) { isLoading = true; failed = false }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (isEmbed) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                if (request?.isForMainFrame == true) { isLoading = false; failed = true }
                            }
                        }
                        loadUrl(activeUrl)
                    }
                },
                update = { it.loadUrl(activeUrl) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // mp4 langsung → ExoPlayer
            com.jayra.app.ui.media.MediaVideoPlayer(
                videoUrl = activeUrl,
                accent = MediaColors.Strem,
                title = title,
                onBack = onBack,
                servers = servers
            )
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MediaColors.Strem)
            }
        }

        if (failed) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Refresh, null, tint = MediaColors.Strem, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Tidak bisa memutar", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MediaColors.Strem).clickable { failed = false; isLoading = true; currentUrl = activeUrl }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            Text("Retry", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 14.sp)
                        }
                    }
                    if (servers.size > 1) {
                        Spacer(Modifier.height(14.dp))
                        servers.forEach { (label, url) ->
                            Text(label, fontFamily = SFProFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MediaColors.Strem,
                                modifier = Modifier.padding(vertical = 4.dp).clickable { currentUrl = url; failed = false; isLoading = true })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Kembali", fontFamily = SFProFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
                }
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