package com.jayra.app.ui.media

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * NativeVideoPlayer — 100% NATIVE (ExoPlayer + PlayerView via AndroidView).
 * - useController = false (kontrol kustom native di overlay).
 * - Player dari VideoPlayerManager (singleton), TIDAK buat ExoPlayer di Composable.
 * - Dispose benar (pause saat keluar), error asli dari manager, prefetch via prepare.
 * - Overlay Color.Black 0.3f (NO blur / RenderEffect).
 */
@Composable
fun NativeVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    showControls: Boolean = true
) {
    val context = LocalContext.current
    val player = remember { VideoPlayerManager.get(context) }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            VideoPlayerManager.setVideo(context, videoUrl, loop)
            isBuffering = true
            err = null
        }
    }

    // Listener state (buffering/ready/error)
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                isPlaying = player.playWhenReady && playbackState == Player.STATE_READY
                if (playbackState == Player.STATE_READY) err = null
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                err = error.localizedMessage ?: "Gagal memutar"
                isBuffering = false
            }
        }
        player.addListener(l)
        onDispose {
            player.removeListener(l)
            VideoPlayerManager.pause()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setKeepContentOnPlayerReset(true)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { view -> view.player = player },
            modifier = Modifier.fillMaxSize()
        )

        // Scrim hitam 0.3f (NO blur) saat kontrol tampil
        if (showControls) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }

        // Loading
        if (isBuffering && err == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Error asli (bukan hitam diam)
        err?.let { msg ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Tidak bisa memutar", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(msg, fontFamily = SFProFontFamily, fontSize = 13.sp, color = Color(0xFFB0B0B8))
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).clickable {
                        VideoPlayerManager.setVideo(context, videoUrl, loop)
                        err = null; isBuffering = true
                    }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Retry", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        }

        // Kontrol native (play/pause) — pojok kanan bawah
        if (showControls && err == null) {
            Row(
                Modifier.align(Alignment.BottomEnd).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.4f))
                        .clickable {
                            if (isPlaying) { player.pause(); isPlaying = false } else { player.play(); isPlaying = true }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause",
                        tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}