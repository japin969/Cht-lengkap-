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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.jayra.app.data.DramaEpisode

/**
 * Detail Drama — Bilibili-style. Tombol TONTON SELALU ADA.
 * Tap episode -> resolve stream (m3u8) -> Player. Tidak ada dummy.
 */
@Composable
fun DramaDetailScreen(
    slug: String,
    source: String,
    viewModel: DramaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    // episode yang sedang dipilih untuk di-resolve stream-nya
    var selectedEp by remember { mutableStateOf<DramaEpisode?>(null) }
    // URL media langsung yang sudah siap diputar
    var playingUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(slug, source) {
        viewModel.loadDetail(slug, source)
    }

    // Resolve stream saat episode dipilih
    LaunchedEffect(selectedEp) {
        val ep = selectedEp
        if (ep != null) {
            playingUrl = null
            viewModel.loadStreams(ep.slug ?: "", source)
        }
    }

    // Saat streams siap, ambil URL pertama yang bisa diputar
    LaunchedEffect(state.streams) {
        val first = state.streams.firstOrNull()
        if (first != null) playingUrl = first.url
    }

    // Player fullscreen bila ada URL media
    if (playingUrl != null) {
        FullScreenVideoPlayer(
            videoUrl = playingUrl!!,
            onBack = {
                playingUrl = null
                selectedEp = null
                viewModel.clearStreams()
            }
        )
        return
    }

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        state.detail == null && state.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.error}", color = Color.Red)
            }
        }
        else -> {
            val d = state.detail
            LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                item {
                    Row(Modifier.padding(16.dp)) {
                        AsyncImage(
                            model = d?.item?.poster ?: d?.item?.cover,
                            contentDescription = null,
                            modifier = Modifier.width(120.dp).height(180.dp)
                                .clip(RoundedCornerShape(10.dp)).background(Color.Black),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                d?.item?.title ?: "Judul Drama",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${state.episodes.size} Episode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                d?.item?.intro ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(14.dp))
                            // TONTON SELALU ADA — putar episode pertama
                            Button(
                                onClick = {
                                    selectedEp = state.episodes.firstOrNull()
                                },
                                enabled = state.episodes.isNotEmpty()
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Tonton")
                            }
                        }
                    }
                }

                if (state.isResolving) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Menyiapkan video...")
                        }
                    }
                }

                if (state.streams.isEmpty() && state.error != null && selectedEp != null) {
                    item {
                        Text(
                            state.error!!,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                item {
                    Text(
                        "Daftar Episode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(state.episodes) { ep ->
                    ListItem(
                        headlineContent = { Text(ep.name ?: "Episode ${ep.index}") },
                        supportingContent = { Text(if (ep.slug != null) "Tersedia" else "Terkunci") },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (ep.slug != null) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        },
                        modifier = Modifier.clickable(enabled = ep.slug != null) {
                            selectedEp = ep
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun FullScreenVideoPlayer(videoUrl: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Text("Tutup", color = Color.White)
        }
    }
}