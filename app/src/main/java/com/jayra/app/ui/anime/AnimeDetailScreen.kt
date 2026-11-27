package com.jayra.app.ui.anime

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun AnimeDetailScreen(
    animeUrl: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(animeUrl) { viewModel.loadDetail(animeUrl) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${state.error}") }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = state.detail?.image,
                        contentDescription = null,
                        modifier = Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(state.detail?.title ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Rating: ${state.detail?.rating ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        Text("Total: ${state.detail?.totalEpisodes ?: 0} Episode", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(state.detail?.synopsis ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 6)
                    }
                }
            }
            item { Text("Daftar Episode", Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
            items(state.episodes) { ep ->
                ListItem(
                    headlineContent = { Text(ep.title ?: "Episode ${ep.episode}") },
                    trailingContent = {
                        Button(onClick = { ep.episodeUrl?.let { onPlayEpisode(it) } }) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Play")
                        }
                    }
                )
                Divider()
            }
        }
    }
}

@Composable
fun AnimePlayerScreen(videoUrl: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
