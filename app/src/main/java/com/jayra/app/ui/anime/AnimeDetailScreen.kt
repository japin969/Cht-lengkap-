package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import coil.compose.LocalImageLoader
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.ui.media.*

@Composable
fun AnimeDetailScreen(
    animeUrl: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    // ImageLoader khusus anime (tidak mengubah konfigurasi global app).
    val mediaLoader = remember(context) { createMediaImageLoader(context) }

    LaunchedEffect(animeUrl) { viewModel.loadDetail(animeUrl) }

    androidx.compose.runtime.CompositionLocalProvider(LocalImageLoader provides mediaLoader) {
        if (state.isLoading && state.detail == null) {
            MediaLoading()
        } else if (state.error != null && state.detail == null) {
            MediaErrorState(
                message = state.error,
                title = "Detail tidak bisa dimuat",
                onRetry = { viewModel.loadDetail(animeUrl) }
            )
        } else if (state.detail == null) {
            MediaEmptyState(
                title = "Anime tidak ditemukan",
                message = "Detail anime tidak tersedia.",
                icon = Icons.Rounded.Movie,
                actionLabel = "Coba Lagi",
                onAction = { viewModel.loadDetail(animeUrl) }
            )
        } else {
            val detail = state.detail ?: return@CompositionLocalProvider
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // ── Hero: cover + judul + meta ─────────────────────
                item(key = "hero") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                    ) {
                        MediaImage(
                            url = detail.image,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            contentScale = ContentScale.Crop,
                            shape = RoundedCornerShape(0.dp),
                            fallbackIcon = Icons.Rounded.Movie
                        )
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.15f to Color.Transparent,
                                        1f to MaterialTheme.colorScheme.background
                                    )
                                )
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            MediaImage(
                                url = detail.image,
                                contentDescription = detail.title,
                                modifier = Modifier
                                    .width(112.dp)
                                    .height(164.dp),
                                contentScale = ContentScale.Crop,
                                shape = RoundedCornerShape(12.dp),
                                fallbackIcon = Icons.Rounded.Movie
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Spacer(Modifier.height(28.dp))
                                Text(
                                    text = detail.title ?: "Tanpa judul",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AnimeRatingChip(detail.rating)
                                    AnimeInfoChip(
                                        detail.totalEpisodes?.let { "$it eps" } ?: "TV"
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Info ───────────────────────────────────────────
                val info = detail.info
                if (!info.isNullOrEmpty()) {
                    item(key = "info") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            info["Status"]?.let { AnimeInfoChip(it) }
                            info["Tipe"]?.let { AnimeInfoChip(it) }
                            info["Mulai"]?.let { AnimeInfoChip(it.take(10)) }
                        }
                    }
                }

                // ── Sinopsis ───────────────────────────────────────
                val synopsis = detail.synopsis?.takeIf { it.isNotBlank() }
                if (!synopsis.isNullOrBlank()) {
                    item(key = "synopsis") {
                        var expanded by remember(animeUrl) { mutableStateOf(false) }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Sinopsis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = synopsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expanded) Int.MAX_VALUE else 5,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (synopsis.length > 160) {
                                TextButton(onClick = { expanded = !expanded }) {
                                    Text(
                                        if (expanded) "Tutup" else "Selengkapnya",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        )
                    }
                }

                // ── Daftar episode ─────────────────────────────────
                item(key = "episode_header") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Daftar Episode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${state.episodes.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.episodes.isEmpty()) {
                    item(key = "episode_empty") {
                        MediaEmptyState(
                            title = "Episode belum tersedia",
                            message = "Daftar episode untuk anime ini belum bisa dimuat.",
                            icon = Icons.Rounded.Movie,
                            actionLabel = "Coba Lagi",
                            onAction = { viewModel.loadDetail(animeUrl) },
                            modifier = Modifier.height(240.dp)
                        )
                    }
                } else {
                    items(
                        items = state.episodes,
                        key = { it.episodeUrl ?: it.episode ?: it.title ?: "" }
                    ) { ep ->
                        AnimeEpisodeRow(
                            episode = ep,
                            posterUrl = detail.image,
                            onClick = { ep.episodeUrl?.let { onPlayEpisode(it) } }
                        )
                    }
                }
            }
        }
    }
}

/** Chip kecil untuk info anime (rating / jumlah episode). */
@Composable
fun AnimeInfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

/** Chip rating bintang untuk anime. */
@Composable
fun AnimeRatingChip(rating: Double?) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = Color(0xFFFACC15),
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = rating?.let { String.format("%.1f", it) } ?: "-",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Baris episode bergaya YouTube: thumbnail + judul + nomor + durasi.
 * Bila Nexadev tidak menyediakan thumbnail per-episode, dipakai poster anime.
 */
@Composable
private fun AnimeEpisodeRow(
    episode: AnimeEpisode,
    posterUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .height(78.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            MediaImage(
                url = posterUrl,
                contentDescription = episode.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(10.dp),
                fallbackIcon = Icons.Rounded.Movie,
                iconSize = 22.dp
            )
            // Overlay tombol play
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            // Badge nomor episode
            episode.episode?.let { num ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = num,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = episode.title?.takeIf { it.isNotBlank() }
                    ?: episode.episode?.let { "Episode $it" }
                    ?: "Episode",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = episode.episode?.let { "Episode $it" } ?: "Putar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Pemutar video native (ExoPlayer) dengan kontrol modern:
 * play/pause, seekbar, posisi waktu, fullscreen, judul episode,
 * serta tombol episode sebelumnya / berikutnya.
 *
 * Tidak memakai WebView.
 */
@Composable
fun AnimePlayerScreen(
    videoUrl: String,
    title: String? = null,
    onBack: () -> Unit = {},
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    if (videoUrl.isBlank()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sumber video belum tersedia",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Update posisi & durasi secara berkala.
    LaunchedEffect(exoPlayer) {
        while (true) {
            position = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            isPlaying = exoPlayer.isPlaying
            kotlinx.coroutines.delay(500)
        }
    }

    // Sembunyikan kontrol otomatis setelah 3 detik.
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }

    BackHandler(enabled = true) { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .align(Alignment.TopCenter)
                ),
            update = { view ->
                // Klik area video untuk show/hide kontrol.
                view.setOnClickListener { showControls = !showControls }
            }
        )

        // Lapisan kontrol
        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.75f),
                            0.35f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            ) {
                // ── Bar atas: back + judul + fullscreen ──────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = title ?: "Memutar",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { isFullscreen = !isFullscreen },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFullscreen)
                                Icons.Rounded.FullscreenExit
                            else
                                Icons.Rounded.Fullscreen,
                            contentDescription = "Layar penuh",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // ── Tengah: prev / play-pause / next ─────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    PlayerRoundButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Episode sebelumnya",
                        enabled = hasPrevious,
                        onClick = {
                            exoPlayer.seekTo(0)
                            onPrevious()
                        }
                    )
                    Spacer(Modifier.width(28.dp))
                    Box(
                        Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                showControls = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying)
                                Icons.Rounded.Pause
                            else
                                Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.width(28.dp))
                    PlayerRoundButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Episode berikutnya",
                        enabled = hasNext,
                        onClick = {
                            exoPlayer.seekTo(0)
                            onNext()
                        }
                    )
                }

                // ── Bawah: seekbar + waktu ───────────────────────
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Slider(
                        value = if (duration > 0) position.toFloat() / duration else 0f,
                        onValueChange = { fraction ->
                            if (duration > 0) {
                                exoPlayer.seekTo((fraction * duration).toLong())
                                position = (fraction * duration).toLong()
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(position),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRoundButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (enabled) 0.16f else 0.06f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f)
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
