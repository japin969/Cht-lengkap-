package com.jayra.app.ui.anime

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.ui.media.CoverImage
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaIconButton
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.SectionHeader

private val AnimeAccent = Color(0xFF22C55E)

/**
 * Home Anime — layout modern bergaya Crunchyroll / Bstation.
 *
 * App bar: judul + aksi Search / Bookmark / History.
 * Konten : hero (anime unggulan) → carousel "Sedang Tayang" → "Populer" → "Selesai".
 */
@Composable
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimeHomeTopBar(
            onSearchClick = onSearchClick,
            onBookmarkClick = onBookmarkClick,
            onHistoryClick = onHistoryClick
        )

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading &&
                    state.popular.isEmpty() &&
                    state.ongoing.isEmpty() &&
                    state.completed.isEmpty() -> MediaLoading()

                state.homeError != null &&
                    state.popular.isEmpty() &&
                    state.ongoing.isEmpty() &&
                    state.completed.isEmpty() -> MediaErrorState(
                    message = state.homeError,
                    onRetry = { viewModel.retry() }
                )

                state.popular.isEmpty() && state.ongoing.isEmpty() && state.completed.isEmpty() ->
                    MediaEmptyState(
                        title = "Belum ada anime",
                        message = "Daftar anime sedang kosong. Coba muat ulang.",
                        icon = Icons.Rounded.Movie,
                        actionLabel = "Coba Lagi",
                        onAction = { viewModel.retry() }
                    )

                else -> AnimeHomeContent(
                    ongoing = state.ongoing,
                    popular = state.popular,
                    completed = state.completed,
                    onAnimeClick = onAnimeClick,
                    onViewAllClick = onViewAllClick
                )
            }
        }
    }
}

@Composable
private fun AnimeHomeTopBar(
    onSearchClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Anime",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Episode terbaru, subtitle Indonesia",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            MediaIconButton(Icons.Rounded.Search, "Cari", onSearchClick)
            MediaIconButton(Icons.Rounded.BookmarkBorder, "Bookmark", onBookmarkClick)
            MediaIconButton(Icons.Rounded.History, "Riwayat", onHistoryClick)
        }
    }
}

@Composable
private fun AnimeHomeContent(
    ongoing: List<AnimeSearchItem>,
    popular: List<AnimeSearchItem>,
    completed: List<AnimeSearchItem>,
    onAnimeClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (ongoing.firstOrNull() ?: popular.firstOrNull())?.let { hero ->
            item(key = "hero") { AnimeHeroCard(hero) { onAnimeClick(it) } }
        }

        if (ongoing.isNotEmpty()) {
            item(key = "ongoing-header") {
                SectionHeader(
                    title = "Sedang Tayang",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "ongoing-row") {
                AnimeCarousel(items = ongoing, onClick = onAnimeClick)
            }
        }

        if (popular.isNotEmpty()) {
            item(key = "popular-header") {
                SectionHeader(
                    title = "Populer",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "popular-row") {
                AnimeCarousel(items = popular, onClick = onAnimeClick)
            }
        }

        if (completed.isNotEmpty()) {
            item(key = "completed-header") {
                SectionHeader(
                    title = "Selesai",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "completed-row") {
                AnimeCarousel(items = completed, onClick = onAnimeClick)
            }
        }
    }
}

@Composable
private fun AnimeCarousel(
    items: List<AnimeSearchItem>,
    onClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.link ?: it.title ?: "" }) { anime ->
            AnimePosterCard(anime) { onClick(it) }
        }
    }
}

@Composable
private fun AnimeHeroCard(anime: AnimeSearchItem, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick(anime.link ?: "") }
    ) {
        CoverImage(
            url = anime.image,
            contentDescription = anime.title,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(20.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.1f to Color.Transparent,
                        0.5f to Color.Black.copy(alpha = 0.4f),
                        1f to Color.Black.copy(alpha = 0.94f)
                    )
                )
        )
        // Tombol play di tengah
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Putar",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Surface(shape = RoundedCornerShape(50), color = AnimeAccent) {
                Text(
                    text = anime.type?.uppercase() ?: "ANIME",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = anime.title ?: "Tanpa judul",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!anime.latestEpisode.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = anime.latestEpisode ?: "",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AnimePosterCard(anime: AnimeSearchItem, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(anime.link ?: "") }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            CoverImage(
                url = anime.image,
                contentDescription = anime.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(14.dp)
            )
            if (!anime.latestEpisode.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = anime.latestEpisode?.take(12) ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = anime.title ?: "Tanpa judul",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Chip rating bintang untuk detail anime. */
@Composable
fun AnimeRatingChip(rating: Double?) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
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