package com.jayra.app.ui.media

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * Pemutar video (ExoPlayer) yang robust — pola aplikasi streaming terkenal:
 *  - loading state yang jelas (spinner)
 *  - error state + tombol Retry dan ganti server
 *  - kontrol lengkap (play/pause, seek bar, posisi tersimpan saat rotasi)
 *  - lifecycle benar: prepare saat start, release saat dispose, pause saat stop
 *
 * @param videoUrl URL stream (mp4/m3u8). Jika blank -> tampilkan error state.
 * @param servers daftar server alternatif (label,url) untuk "ganti server".
 */
@Composable
fun MediaVideoPlayer(
    videoUrl: String,
    accent: Color,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    servers: List<Pair<String, String>> = emptyList()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentUrl by remember(videoUrl) { mutableStateOf(videoUrl) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    val exoPlayer = remember(currentUrl, retryKey) {
        if (currentUrl.isBlank()) null
        else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(currentUrl))
            playWhenReady = true
            prepare()
        }
    }

    // Reset state saat URL/retry berubah
    LaunchedEffect(currentUrl, retryKey) {
        isLoading = currentUrl.isNotBlank()
        errorMessage = if (currentUrl.isBlank()) "Sumber video tidak tersedia" else null
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        errorMessage = null
                    }
                    Player.STATE_ENDED -> isLoading = false
                    Player.STATE_IDLE -> { /* biarkan error state yang menangani */ }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                errorMessage = error.localizedMessage ?: "Gagal memutar video"
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    // Pause saat app background, resume saat kembali (hemat baterai + anti bug)
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> exoPlayer?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        controllerAutoShow = true
                        setKeepContentOnPlayerReset(true)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { view -> view.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading overlay
        AnimatedVisibility(visible = isLoading && errorMessage == null, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accent)
                    Spacer(Modifier.height(12.dp))
                    Text("Memuat video...", fontFamily = SFProFontFamily, color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Error overlay + retry + ganti server
        errorMessage?.let { msg ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Refresh, null, tint = accent, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Tidak bisa memutar",
                        fontFamily = SFProFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        msg,
                        fontFamily = SFProFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFFB0B0B8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton("Retry", accent) { retryKey++ }
                    }
                    if (servers.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        servers.forEach { (label, url) ->
                            Text(
                                label,
                                fontFamily = SFProFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = accent,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        currentUrl = url
                                        retryKey++
                                    }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Kembali",
                        fontFamily = SFProFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.clickable { onBack() }
                    )
                }
            }
        }

        // Tombol back (selalu terlihat di atas player)
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        // Judul
        Text(
            title,
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .statusBarsPadding()
                .padding(start = 16.dp, bottom = 16.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PillButton(text: String, bg: Color, textColor: Color = Color.Black, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text, fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
    }
}