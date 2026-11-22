package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.LocalImageLoader
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaImage
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.createMediaImageLoader

@Composable
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    // ImageLoader khusus anime (tidak mengubah konfigurasi global app).
    val mediaLoader = remember(context) { createMediaImageLoader(context) }

    androidx.compose.runtime.CompositionLocalProvider(LocalImageLoader provides mediaLoader) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
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

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    if (state.ongoing.isNotEmpty()) {
                        item(key = "ongoing_header") {
                            AnimeSectionHeader(
                                title = "Sedang Tayang",
                                subtitle = "Episode terbaru"
                            )
                        }
                        item(key = "ongoing_row") {
                            AnimeRow(state.ongoing) { onAnimeClick(it) }
                        }
                    }
                    if (state.popular.isNotEmpty()) {
                        item(key = "popular_header") {
                            AnimeSectionHeader(
                                title = "Populer",
                                subtitle = "Paling banyak ditonton"
                            )
                        }
                        item(key = "popular_row") {
                            AnimeRow(state.popular) { onAnimeClick(it) }
                        }
                    }
                    if (state.completed.isNotEmpty()) {
                        item(key = "completed_header") {
                            AnimeSectionHeader(
                                title = "Selesai",
                                subtitle = "Sudah tamat"
                            )
                        }
                        item(key = "completed_row") {
                            AnimeRow(state.completed) { onAnimeClick(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeSectionHeader(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnimeRow(items: List<AnimeSearchItem>, onClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.link ?: it.title ?: "" }) { anime ->
            AnimePosterCard(anime) { onClick(it) }
        }
    }
}

/**
 * Kartu poster anime.
 * Gambar memakai MediaImage (Coil) dengan placeholder + fallback + retry antar host CDN.
 */
@Composable
fun AnimePosterCard(anime: AnimeSearchItem, onClick: (String) -> Unit) {
    Column(
        Modifier
            .width(124.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clickable { onClick(anime.link ?: "") }
    ) {
        MediaImage(
            url = anime.image,
            contentDescription = anime.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f),
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(12.dp),
            fallbackIcon = Icons.Rounded.Movie,
            iconSize = 30.dp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = anime.title ?: "Tanpa judul",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            color = Color(0xFF111111),
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        if (!anime.latestEpisode.isNullOrBlank()) {
            Text(
                text = anime.latestEpisode ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF555555),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}