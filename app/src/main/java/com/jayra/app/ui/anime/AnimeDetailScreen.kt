package com.jayra.app.ui.anime

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.media.CoverImage
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaIconButton
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.MediaTopBar
import com.jayra.app.ui.media.MetaChip

/**
 * Detail Anime — hero, sinopsis, info, dan daftar episode.
 *
 * Tidak ada WebView: pemutaran video memakai ExoPlayer native (lihat
 * [AnimePlayerScreen]).
 */
@Composable
fun AnimeDetailScreen(
    animeUrl: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var bookmarked by remember(animeUrl) { mutableStateOf(false) }
    var synopsisExpanded by remember(animeUrl) { mutableStateOf(false) }

    LaunchedEffect(animeUrl) {
        viewModel.loadDetail(animeUrl)
        bookmarked = MediaLibraryStore.isBookmarked(context, "anime", animeUrl)
    }

    // Catat riwayat saat detail berhasil dimuat.
    LaunchedEffect(state.detail?.title) {
        val detail = state.detail ?: return@LaunchedEffect
        if (detail.title != null) {
            MediaLibraryStore.recordHistory(
                context,
                MediaLibraryStore.Entry(
                    kind = "anime",
                    id = animeUrl,
                    title = detail.title ?: "Tanpa judul",
                    image = detail.image,
                    subtitle = "Anime"
                )
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(
            title = state.detail?.title ?: "Detail Anime",
            onBack = onBack,
            actions = {
                MediaIconButton(
                    icon = if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (bookmarked) "Hapus bookmark" else "Simpan bookmark",
                    onClick = {
                        val nowSaved = MediaLibraryStore.toggleBookmark(
                            context,
                            MediaLibraryStore.Entry(
                                kind = "anime",
                                id = animeUrl,
                                title = state.detail?.title ?: "Tanpa judul",
                                image = state.detail?.image,
                                subtitle = "Anime"
                            )
                        )
                        bookmarked = nowSaved
                    },
                    active = bookmarked
                )
            }
        )

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.detail == null -> MediaLoading()

                state.detailError != null && state.detail == null -> MediaErrorState(
                    message = state.detailError,
                    title = "Detail tidak bisa dimuat",
                    onRetry = { viewModel.retryDetail() }
                )

                state.detail == null -> MediaEmptyState(
                    title = "Anime tidak ditemukan",
                    message = "Detail anime tidak tersedia.",
                    icon = Icons.Rounded.Movie,
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.retryDetail() }
                )

                else -> AnimeDetailContent(
                    state = state,
                    synopsisExpanded = synopsisExpanded,
                    onToggleSynopsis = { synopsisExpanded = !synopsisExpanded },
                    onPlayEpisode = onPlayEpisode,
                    onRetryEpisodes = { viewModel.retryEpisodes() }
                )
            }
        }
    }
}

@Composable
private fun AnimeDetailContent(
    state: AnimeUiState,
    synopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    onPlayEpisode: (String) -> Unit,
    onRetryEpisodes: () -> Unit
) {
    val detail = state.detail ?: return
    val synopsis = detail.synopsis?.takeIf { it.isNotBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // ── Hero ───────────────────────────────────────────────
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                CoverImage(
                    url = detail.image,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(0.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.2f to MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CoverImage(
                        url = detail.image,
                        contentDescription = detail.title,
                        modifier = Modifier
                            .width(110.dp)
                            .height(160.dp),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = detail.title ?: "Tanpa judul",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AnimeRatingChip(detail.rating)
                            MetaChip(
                                text = detail.totalEpisodes?.let { "$it eps" } ?: "TV"
                            )
                        }
                    }
                }
            }
        }

        // ── Info chips ─────────────────────────────────────────
        val info = detail.info
        if (!info.isNullOrEmpty()) {
            item(key = "info") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info["Status"]?.let { MetaChip(text = it) }
                    info["Tipe"]?.let { MetaChip(text = it) }
                    info["Mulai"]?.let { MetaChip(text = it.take(10)) }
                }
            }
        }

        // ── Sinopsis ───────────────────────────────────────────
        if (!synopsis.isNullOrBlank()) {
            item(key = "synopsis") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (synopsis.length > 180) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onToggleSynopsis)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (synopsisExpanded) "Tutup" else "Selengkapnya",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(4.dp))
                            androidx.compose.material3.Icon(
                                imageVector = if (synopsisExpanded)
                                    Icons.Rounded.ExpandLess
                                else
                                    Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            }
        }

        // ── Daftar episode ─────────────────────────────────────
        item(key = "episode-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
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

        when {
            state.episodesLoading && state.episodes.isEmpty() -> item(key = "ep-loading") {
                MediaLoading(modifier = Modifier.height(200.dp))
            }

            state.episodes.isEmpty() -> item(key = "ep-empty") {
                MediaEmptyState(
                    title = "Episode belum tersedia",
                    message = "Daftar episode untuk anime ini belum bisa dimuat.",
                    icon = Icons.Rounded.Movie,
                    actionLabel = "Coba Lagi",
                    onAction = onRetryEpisodes,
                    modifier = Modifier.height(240.dp)
                )
            }

            else -> items(
                items = state.episodes,
                key = { it.episodeUrl ?: it.episode ?: it.title ?: "" }
            ) { ep ->
                AnimeEpisodeRow(
                    episode = ep,
                    onClick = { onPlayEpisode(ep.episodeUrl ?: "") }
                )
            }
        }
    }
}

@Composable
private fun AnimeEpisodeRow(episode: AnimeEpisode, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = episode.title?.takeIf { it.isNotBlank() }
                        ?: episode.episode?.let { "Episode $it" }
                        ?: "Episode",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (episode.episode != null) "Episode ${episode.episode}" else "Putar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    }
}

/**
 * Pemutar video native memakai ExoPlayer.
 * Menggantikan implementasi WebView sebelumnya. Bila [videoUrl] kosong,
 * ditampilkan pesan bahwa sumber video belum tersedia — tidak crash.
 */
@Composable
fun AnimePlayerScreen(
    videoUrl: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember(videoUrl) {
        if (videoUrl.isBlank()) null
        else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MediaTopBar(
            title = "Memutar",
            onBack = onBack,
            containerColor = Color(0xFF101010)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val player = exoPlayer
            if (player != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Sumber video belum tersedia",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sumber streaming tidak menyediakan tautan untuk episode ini.",
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}