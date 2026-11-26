package com.jayra.app.ui.drama

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.DramaRepository
import com.jayra.app.ui.video.NativeVideoPlayer

@Composable
fun DramaDetailScreen(
    dramaId: String,
    viewModel: DramaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var playingEpisode by remember { mutableStateOf<DramaRepository.DramaEpisode?>(null) }

    LaunchedEffect(dramaId) {
        viewModel.loadDetail(dramaId)
    }

    // Fullscreen Video Player Overlay
    if (playingEpisode != null) {
        val ep = playingEpisode!!
        val videoUrl = ep.mp4 ?: ep.videoUrl
        val hasVideo = videoUrl?.isNotBlank() == true

        if (hasVideo) {
            NativeVideoPlayer(
                videoUrl = videoUrl!!,
                headers = mapOf(
                    "Referer" to "https://www.tvmaze.com/",
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36"
                ),
                title = "${state.detail?.title ?: "Drama"} - ${ep.name ?: "Episode ${ep.index}"}",
                autoPlay = true,
                onError = { error ->
                    playingEpisode = null
                },
                onCompletion = {
                    playingEpisode = null
                }
            )
        } else {
            // No video URL available - show error and auto-dismiss
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material.Icon(
                        imageVector = Icons.Rounded.ErrorOutlined,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Video tidak tersedia", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Episode ini belum memiliki link streaming", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { playingEpisode = null },
                        colors = ButtonDefaults.buttonColors(containerColor = com.jayra.app.ui.theme.JayraBlue)
                    ) {
                        Text("Tutup", color = Color.White)
                    }
                }
            }
        }
    } else {
        // Detail Screen
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadDetail(dramaId) }) { Text("Coba Lagi") }
                }
            }
        } else {
            val detail = state.detail
            val episodes = state.episodes

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Header Info Drama
                item {
                    Row(modifier = Modifier.padding(16.dp)) {
                        AsyncImage(
                            model = detail?.cover,
                            contentDescription = null,
                            modifier = Modifier
                                .width(120.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            placeholder = placeholder(),
                            error = errorPlaceholder()
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = detail?.title ?: "Judul Drama",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${detail?.chapterCount ?: 0} Episode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = detail?.intro ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 5
                            )
                        }
                    }
                }

                // List Episode
                item {
                    Text(
                        text = "Daftar Episode (${episodes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(episodes) { ep ->
                    val hasVideo = ep.mp4?.isNotBlank() == true || ep.videoUrl?.isNotBlank() == true
                    ListItem(
                        headlineContent = { Text(ep.name ?: "Episode ${ep.index}") },
                        supportingContent = { Text(if (hasVideo) "Tersedia" else "Belum tersedia") },
                        leadingContent = {
                            androidx.compose.material.Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (hasVideo) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasVideo) {
                                if (hasVideo) playingEpisode = ep
                            }
                            .padding(vertical = 4.dp)
                    )
                    Divider()
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════

@Composable
private fun placeholder() = Box(
    Modifier
        .width(120.dp)
        .height(180.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
) {
    androidx.compose.material.Icon(
        imageVector = Icons.Rounded.Movie,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun errorPlaceholder() = Box(
    Modifier
        .width(120.dp)
        .height(180.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.errorContainer)
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material.Icon(
            imageVector = Icons.Rounded.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.height(4.dp))
        Text("Gagal muat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

import androidx.compose.foundation.layout.Arrangement