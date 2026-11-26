package com.jayra.app.ui.media

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * NativeVideoPlayer — 100% NATIVE (ExoPlayer + PlayerView via AndroidView).
 * - useController = false (kontrol kustom di overlay, bukan PlayerView controller).
 * - Ambil player dari VideoPlayerManager (singleton), TIDAK buat ExoPlayer di Composable.
 * - Dispose benar: pause + release saat keluar.
 *
 * @param videoUrl URL video (mp4/m3u8). Jika blank -> tidak set video.
 * @param controlsOverlay overlay kontrol kustom (play/pause/seek) di atas video.
 */
@Composable
fun NativeVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    controlsOverlay: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val player = remember { VideoPlayerManager.get(context) }

    // Set video saat URL berubah (player dari manager, bukan dibuat di sini)
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            VideoPlayerManager.setVideo(context, videoUrl, loop)
        }
    }

    // Pause saat background, resume saat kembali
    DisposableEffect(Unit) {
        onDispose {
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

        // Overlay kontrol kustom (di atas video, tanpa blur)
        controlsOverlay()
    }
}