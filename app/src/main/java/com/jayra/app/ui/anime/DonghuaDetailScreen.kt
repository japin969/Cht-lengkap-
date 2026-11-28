package com.jayra.app.ui.anime

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jayra.app.data.DonghuaDetail
import com.jayra.app.data.DonghuaEpisode
import com.jayra.app.data.DonghuaEpisodeItem
import com.jayra.app.data.DonghuaItem
import com.jayra.app.data.DonghuaSource

@Composable
fun DonghuaDetailScreen(
    source: DonghuaSource,
    slug: String,
    onBack: () -> Unit,
    onEpisodeClick: (DonghuaSource, String) -> Unit,
    viewModel: DonghuaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(source, slug) {
        viewModel.setSource(source, reloadHome = false)
        viewModel.loadDetail(slug)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = state.detail?.title ?: "Detail",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when {
            state.isDetailLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.detailError != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.detailError ?: "Gagal memuat detail",
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadDetail(slug) }) { Text("Coba Lagi") }
                }
            }

            state.detail != null -> {
                DonghuaDetailContent(
                    detail = state.detail!!,
                    onEpisodeClick = onEpisodeClick,
                    onItemClick = { item -> onEpisodeClick(item.source, item.slug) }
                )
            }

            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun DonghuaDetailContent(
    detail: DonghuaDetail,
    onEpisodeClick: (DonghuaSource, String) -> Unit,
    onItemClick: (DonghuaItem) -> Unit
) {
    var synopsisExpanded by remember(detail.slug) { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item("header") {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                DonghuaPosterImage(
                    url = detail.poster,
                    title = detail.title,
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    val meta = listOfNotNull(detail.status, detail.type, detail.released)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    detail.rating?.takeIf { it.isNotBlank() }?.let {
                        Text("Rating: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    detail.totalEpisodes?.takeIf { it.isNotBlank() }?.let {
                        Text("Total: $it episode", style = MaterialTheme.typography.bodySmall)
                    }
                    detail.studio?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item("genres") {
            if (detail.genres.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detail.genres, key = { it.slug + it.name }) { genre ->
                        AssistChip(onClick = {}, label = { Text(genre.name) })
                    }
                }
            }
        }

        item("synopsis") {
            if (!detail.synopsis.isNullOrBlank()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = detail.synopsis ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = { synopsisExpanded = !synopsisExpanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (synopsisExpanded) "Tutup" else "Baca Selengkapnya")
                    }
                }
            }
        }

        item("watch") {
            val first = detail.episodes.firstOrNull()
            Button(
                onClick = { first?.let { onEpisodeClick(it.source, it.slug) } },
                enabled = first != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (first == null) "Belum ada episode" else "Tonton Episode")
            }
        }

        if (detail.episodes.isNotEmpty()) {
            item("episodes-title") {
                DonghuaSectionTitle("Daftar Episode (${detail.episodes.size})")
            }
            items(detail.episodes, key = { "${it.source}-${it.slug}" }) { ep ->
                DonghuaEpisodeRow(ep) { onEpisodeClick(ep.source, ep.slug) }
            }
        }

        if (detail.recommendations.isNotEmpty()) {
            item("rec-title") { DonghuaSectionTitle("Rekomendasi") }
            item("rec-row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        detail.recommendations,
                        key = { "${it.source}-${it.slug}-${it.title}" }
                    ) { item ->
                        DonghuaPosterCard(item = item, onClick = onItemClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun DonghuaEpisodeRow(ep: DonghuaEpisodeItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = ep.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ep.releaseDate?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider()
}

// ============================================================
// EPISODE / PLAYER
// ============================================================

@Composable
fun DonghuaPlayerScreen(
    source: DonghuaSource,
    slug: String,
    onBack: () -> Unit,
    onNavigateEpisode: (DonghuaSource, String) -> Unit,
    viewModel: DonghuaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(source, slug) {
        viewModel.setSource(source, reloadHome = false)
        viewModel.loadEpisode(slug)
    }

    val episode = state.episode
    val playable = episode?.playableUrl.orEmpty()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = "Episode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        when {
            state.isEpisodeLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.episodeError != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.episodeError ?: "Gagal memuat episode",
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadEpisode(slug) }) { Text("Coba Lagi") }
                }
            }

            episode != null && playable.isNotBlank() -> {
                DonghuaNativePlayer(url = playable)
                DonghuaEpisodeInfo(
                    episode = episode,
                    onNavigateEpisode = onNavigateEpisode
                )
            }

            episode != null -> {
                DonghuaUnsupportedStreams(episode)
                DonghuaEpisodeInfo(
                    episode = episode,
                    onNavigateEpisode = onNavigateEpisode
                )
            }

            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun DonghuaEpisodeInfo(
    episode: DonghuaEpisode,
    onNavigateEpisode: (DonghuaSource, String) -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Text(
            text = episode.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { episode.prevSlug?.let { onNavigateEpisode(episode.source, it) } },
                enabled = episode.prevSlug != null,
                modifier = Modifier.weight(1f)
            ) { Text("Prev") }
            OutlinedButton(
                onClick = { episode.nextSlug?.let { onNavigateEpisode(episode.source, it) } },
                enabled = episode.nextSlug != null,
                modifier = Modifier.weight(1f)
            ) { Text("Next") }
        }
    }
}

/**
 * Player native (Media3/ExoPlayer). Tidak memakai WebView.
 * Mendukung file langsung .mp4 / .m3u8 yang dikirim API.
 */
@OptIn(UnstableApi::class)
@Composable
private fun DonghuaNativePlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    var isBuffering by remember(url) { mutableStateOf(true) }
    var playbackError by remember(url) { mutableStateOf<String?>(null) }
    var retryToken by remember(url) { mutableIntStateOf(0) }

    LaunchedEffect(url, retryToken) {
        playbackError = null
        isBuffering = true
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "Gagal memutar video"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playbackError == null && isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        playbackError?.let { message ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Video gagal diputar",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { retryToken++ }) { Text("Coba Lagi") }
            }
        }
    }
}

/**
 * Endpoint hanya mengirim halaman embed (ok.ru, dailymotion, dll),
 * bukan file media. Ditampilkan jelas, tanpa WebView.
 */
@Composable
private fun DonghuaUnsupportedStreams(episode: DonghuaEpisode) {
    Column(Modifier.padding(16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Stream belum bisa diputar di player native",
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (episode.servers.isEmpty()) {
                        "Endpoint tidak mengirim server video untuk episode ini."
                    } else {
                        "Endpoint hanya mengirim link embed (bukan file .mp4/.m3u8), " +
                            "dan aplikasi ini tidak memakai WebView."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (episode.servers.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Server tersedia:", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            episode.servers.forEach { server ->
                Text(
                    text = "• ${server.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
