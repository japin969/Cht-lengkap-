package com.jayra.app.ui.anime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.NativeVideoPlayer

/**
 * AnimePlayer — 100% NATIVE (ExoPlayer via NativeVideoPlayer). TANPA WebView.
 * Player diambil dari VideoPlayerManager (singleton), tidak dibuat di Composable.
 */
@Composable
fun AnimePlayerScreen(
    videoUrl: String,
    title: String = "Memutar Anime",
    onBack: () -> Unit,
    viewModel: AnimeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(videoUrl) { viewModel.loadStream(videoUrl) }

    val stream = state.streamUrl
    if (stream.isNullOrBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MediaColors.Anime)
        }
    } else {
        NativeVideoPlayer(
            videoUrl = stream,
            modifier = Modifier.fillMaxSize()
        )
    }
}