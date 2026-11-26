package com.jayra.app.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * MediaVideoPlayer — wrapper tipis di atas NativeVideoPlayer (100% native).
 * Dibiarkan untuk kompatibilitas; semua player memakai VideoPlayerManager (singleton).
 * TIDAK ada ExoPlayer yang dibuat di Composable, TIDAK ada WebView.
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
    NativeVideoPlayer(
        videoUrl = videoUrl,
        modifier = modifier
    )
}