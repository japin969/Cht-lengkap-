package com.jayra.app.ui.drama

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

@Composable
fun DramaDetailScreen(
    dramaUrl: String,
    viewModel: DramaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var playingUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dramaUrl) {
        viewModel.loadDetail(dramaUrl)
    }

    // Jika ada URL video yang dipilih, tampilkan Player Fullscreen
    if (playingUrl != null) {
        FullScreenVideoPlayer(
            videoUrl = playingUrl!!,
            onBack = { playingUrl = null }
        )
        return
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${state.error}", color = Color.Red)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Header Info Drama
            item {
                Row(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = state.detail?.cover,
                        contentDescription = null,
                        modifier = Modifier
                            .width(120.dp)
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = state.detail?.title ?: "Judul Drama",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.detail?.chapterCount ?: 0} Episode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.detail?.intro ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 5
                        )
                    }
                }
            }

            // List Episode
            item {
                Text(
                    text = "Daftar Episode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(state.episodes) { ep ->
                val isUnlocked = ep.unlock == true && ep.videoUrl != null
                ListItem(
                    headlineContent = { Text(ep.name ?: "Episode ${ep.index}") },
                    supportingContent = { Text(if (isUnlocked) "Tersedia" else "Terkunci") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    },
                    modifier = Modifier.clickable(enabled = isUnlocked) {
                        // Catatan: URL mp4 asli perlu di-fetch dari API /video, 
                        // tapi untuk sekarang kita pakai videoUrl sebagai dummy validasi
                        playingUrl = ep.mp4 ?: ep.videoUrl
                    }
                )
                Divider()
            }
        }
    }
}

@Composable
fun FullScreenVideoPlayer(videoUrl: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .aspectRatio(16f / 9f)
        )
        
        // Tombol Back
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Tutup", color = Color.White)
        }
    }
}
