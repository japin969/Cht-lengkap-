package com.jayra.app.ui.anime

import android.content.Context
import android.view.ViewGroup
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

@Composable
fun AnimeDetailScreen(
    animeUrl: String,
    viewModel: AnimeViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    onPlayEpisode: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(animeUrl) { viewModel.loadDetail(animeUrl) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Detail Anime",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        when {
            state.isDetailLoading && state.detail == null -> {
                AnimeDetailSkeleton()
            }

            state.detailError != null && state.detail == null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Gagal memuat detail anime",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.detailError ?: "Koneksi bermasalah",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.retryDetail(animeUrl) }) { Text("Coba Lagi") }
                }
            }

            else -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Row(Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = state.detail?.image,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    state.detail?.title ?: "",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Rating: ${state.detail?.rating ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                Text("Total: ${state.detail?.totalEpisodes ?: 0} Episode", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Text(state.detail?.synopsis ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 6)
                                
                                // Genres
                                val genres = state.detail?.genres
                                if (genres != null && genres.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Genre: ${genres.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                // Info map
                                val info = state.detail?.info
                                if (info != null && info.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Column {
                                        info.forEach { (key, value) ->
                                            Text("$key: $value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Text("Daftar Episode", Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
                    items(state.episodes) { ep ->
                        ListItem(
                            headlineContent = { Text(ep.title ?: "Episode ${ep.episode}") },
                            leadingContent = { Icon(Icons.Rounded.PlayArrow, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ep.episodeUrl?.let { onPlayEpisode(it) } }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider()
                    }
                    if (state.episodes.isEmpty()) {
                        item {
                            Text(
                                text = "Daftar episode belum tersedia. Info anime tetap bisa dilihat di atas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(
                                onClick = { viewModel.retryDetail(animeUrl) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) { Text("Coba Lagi") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeDetailSkeleton() {
    val transition = rememberInfiniteTransition(label = "anime_detail_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anime_detail_skeleton_alpha"
    )
    val bone = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f)

    Row(Modifier.padding(16.dp)) {
        Box(
            Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bone)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            repeat(5) {
                Box(
                    Modifier
                        .fillMaxWidth(if (it % 2 == 0) 0.9f else 0.65f)
                        .height(14.dp)
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bone)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ==================== EXOPLAYER PLAYER SCREEN ====================
@Composable
fun AnimePlayerScreen(videoUrl: String) {
    val context = LocalContext.current
    val playerView = remember { 
        PlayerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        } 
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    // Attach player to PlayerView
    DisposableEffect(exoPlayer, playerView) {
        playerView.player = exoPlayer
        onDispose {
            playerView.player = null
            exoPlayer.release()
        }
    }

    // Handle lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> exoPlayer.playWhenReady = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> exoPlayer.playWhenReady = false
                Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize()
    )
}