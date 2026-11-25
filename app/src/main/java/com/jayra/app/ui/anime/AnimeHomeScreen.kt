package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import com.jayra.app.ui.media.jellyClick
import com.jayra.app.ui.media.MediaGlassTheme
import com.jayra.app.ui.media.MediaGlassColors
import com.jayra.app.ui.media.MediaFilterChip
import com.jayra.app.ui.media.normalizeKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.AnimeSearchItem

private val AnimeAccent = Color(0xFF22C55E)

/**
 * Home Anime — modern dengan beberapa section terpisah.
 *
 * Section: Hero → Sedang Tayang → Populer → Selesai → Rekomendasi.
 * Setiap kartu menampilkan poster, judul, genre, rating, dan status.
 */
@Composable
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val enrichment by viewModel.enrichment.collectAsStateWithLifecycle()

    MediaGlassTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(MediaGlassColors.BackgroundGradient))
        ) {
            AnimeHomeTopBar(onSearchClick = onSearchClick)

            // Banner sumber: menunjukkan API mana yang aktif.
            val activeSources = state.sources.filter { it.ok }.map { it.source }
            if (activeSources.isNotEmpty()) {
                SourceBanner(sources = activeSources)
            }

            Box(Modifier.fillMaxSize()) {
                val empty = state.ongoing.isEmpty() &&
                    state.popular.isEmpty() &&
                    state.completed.isEmpty() &&
                    state.recent.isEmpty()

                when {
                    state.isLoading && empty -> AnimeLoading()

                    state.homeError != null && empty -> AnimeErrorState(
                        message = state.homeError,
                        title = "Gagal memuat anime",
                        onRetry = { viewModel.retry() }
                    )

                    empty -> AnimeEmptyState(
                        title = "Belum ada anime",
                        message = "Daftar anime sedang kosong. Coba muat ulang.",
                        actionLabel = "Coba Lagi",
                        onAction = { viewModel.retry() }
                    )

                    else -> AnimeHomeContent(
                        ongoing = state.ongoing,
                        popular = state.popular,
                        completed = state.completed,
                        recent = state.recent,
                        topRated = state.topRated,
                        recommendations = state.recommendations,
                        genres = state.genres,
                        enrichment = enrichment,
                        onAnimeClick = onAnimeClick,
                        onViewAllClick = onViewAllClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeHomeTopBar(onSearchClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPaddingCompat()
                .height(60.dp)
                .padding(horizontal = 16.dp),
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
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun Modifier.statusBarsPaddingCompat(): Modifier =
    this.then(statusBarsPadding())

@Composable
private fun AnimeHomeContent(
    ongoing: List<AnimeSearchItem>,
    popular: List<AnimeSearchItem>,
    completed: List<AnimeSearchItem>,
    recent: List<AnimeSearchItem>,
    topRated: List<AnimeSearchItem>,
    recommendations: List<AnimeSearchItem>,
    genres: List<String>,
    enrichment: Map<String, AnimeEnrichment>,
    onAnimeClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    // Judul hero tidak diulang di section lain. Kita lacak SEMUA judul yang
    // sudah tampil (hero + section sebelumnya) supaya tidak ada judul kembar
    // di Home (duplikat antar-section).
    val hero = ongoing.firstOrNull() ?: popular.firstOrNull()
    val seen = linkedSetOf<String>()
    hero?.let { seen.add(normalizeKey(it.title)) }

    fun unique(list: List<AnimeSearchItem>): List<AnimeSearchItem> =
        list.filter { seen.add(normalizeKey(it.title)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        hero?.let { h ->
            item(key = "hero") {
                AnimeHero(
                    anime = h,
                    enrichment = enrichment[h.link],
                    onClick = { onAnimeClick(h.link ?: "") }
                )
            }
        }

        // ── Sedang Tayang ─────────────────────────────────────
        val ongoingList = unique(ongoing)
        if (ongoingList.isNotEmpty()) {
            item(key = "ongoing_head") {
                AnimeSectionHeader(
                    title = "Sedang Tayang",
                    subtitle = "Episode baru setiap minggu",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "ongoing_row") {
                AnimeRow(ongoingList, enrichment, onAnimeClick)
            }
        }

        // ── Populer ───────────────────────────────────────────
        val popularList = unique(popular)
        if (popularList.isNotEmpty()) {
            item(key = "popular_head") {
                AnimeSectionHeader(
                    title = "Populer",
                    subtitle = "Paling banyak ditonton",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "popular_row") {
                AnimeRow(popularList, enrichment, onAnimeClick)
            }
        }

        // ── Terbaru / Update ──────────────────────────────────
        val recentList = unique(recent)
        if (recentList.isNotEmpty()) {
            item(key = "recent_head") {
                AnimeSectionHeader(
                    title = "Terbaru",
                    subtitle = "Baru diupdate",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "recent_row") {
                AnimeRow(recentList, enrichment, onAnimeClick)
            }
        }

        // ── Top Rated ─────────────────────────────────────────
        val topRatedList = unique(topRated)
        if (topRatedList.isNotEmpty()) {
            item(key = "toprated_head") {
                AnimeSectionHeader(
                    title = "Top Rated",
                    subtitle = "Rating tertinggi",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "toprated_row") {
                AnimeRow(topRatedList, enrichment, onAnimeClick)
            }
        }

        // ── Rekomendasi ───────────────────────────────────────
        val recList = unique(recommendations)
        if (recList.isNotEmpty()) {
            item(key = "rec_head") {
                AnimeSectionHeader(
                    title = "Rekomendasi",
                    subtitle = "Pilihan untukmu",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "rec_row") {
                AnimeRow(recList, enrichment, onAnimeClick)
            }
        }

        // ── Selesai ───────────────────────────────────────────
        val completedList = unique(completed)
        if (completedList.isNotEmpty()) {
            item(key = "completed_head") {
                AnimeSectionHeader(
                    title = "Selesai",
                    subtitle = "Sudah tamat, siap maraton",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "completed_row") {
                AnimeRow(completedList, enrichment, onAnimeClick)
            }
        }

        // ── Genre (chip, wrap tanpa scroll horizontal) ──────────
        if (genres.isNotEmpty()) {
            item(key = "genre_head") {
                AnimeSectionHeader(
                    title = "Genre",
                    subtitle = "Jelajahi berdasarkan genre"
                )
            }
            item(key = "genre_row") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.chunked(3).forEach { rowGenres ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowGenres.forEach { g ->
                                MediaFilterChip(
                                    label = g,
                                    selected = false,
                                    onClick = onViewAllClick
                                )
                            }
                            repeat(3 - rowGenres.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeRow(
    items: List<AnimeSearchItem>,
    enrichment: Map<String, AnimeEnrichment>,
    onClick: (String) -> Unit
) {
    // Grid 2 kolom BESAR (lega) — SCROLL VERTICAL saja, tanpa swipe.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { anime ->
                    Box(Modifier.weight(1f)) {
                        AnimePosterCard(
                            anime = anime,
                            enrichment = enrichment[anime.link],
                            onClick = { onClick(anime.link ?: "") }
                        )
                    }
                }
                repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Hero besar untuk anime unggulan. */
@Composable
private fun AnimeHero(
    anime: AnimeSearchItem,
    enrichment: AnimeEnrichment?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(230.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AnimePoster(
            url = anime.image,
            contentDescription = anime.title,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(20.dp),
            icon = Icons.Rounded.Movie
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.1f to Color.Transparent,
                        0.5f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.94f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(58.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Putar",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                enrichment?.rating?.let { r ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", r),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
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
            Spacer(Modifier.height(5.dp))
            Text(
                text = buildString {
                    append(anime.latestEpisode ?: "Episode terbaru")
                    val g = enrichment?.genres?.take(2)
                    if (!g.isNullOrEmpty()) append("  •  ${g.joinToString(", ")}")
                },
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Kartu poster anime dengan poster, judul, genre, rating, dan status.
 */
@Composable
fun AnimePosterCard(
    anime: AnimeSearchItem,
    onClick: () -> Unit,
    enrichment: AnimeEnrichment? = null,
    showStatus: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(com.jayra.app.ui.media.MediaTheme.CardRadius))
            .background(com.jayra.app.ui.media.MediaGlassColors.Surface)
            .border(1.dp, com.jayra.app.ui.media.MediaGlassColors.GlassBorder, RoundedCornerShape(com.jayra.app.ui.media.MediaTheme.CardRadius))
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(com.jayra.app.ui.media.MediaTheme.CardRadius),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .jellyClick(onClick = onClick, pressedScale = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(com.jayra.app.ui.media.MediaTheme.CardRadius))
                .background(com.jayra.app.ui.media.MediaGlassColors.Surface)
        ) {
            AnimePoster(
                url = anime.image,
                contentDescription = anime.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(com.jayra.app.ui.media.MediaTheme.CardRadius)
            )
            // Badge rating sebagai kaca di atas poster.
            enrichment?.rating?.let { r ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.22f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", r),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = anime.title ?: "Tanpa judul",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = com.jayra.app.ui.media.MediaGlassColors.OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 3.dp)
        )
        val genres = enrichment?.genres.orEmpty()
        if (genres.isNotEmpty()) {
            Text(
                text = genres.take(2).joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                color = com.jayra.app.ui.media.MediaGlassColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
        }
        if (showStatus) {
            Text(
                text = anime.latestEpisode ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = com.jayra.app.ui.media.MediaGlassColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
    }
}
/**
 * Banner kecil yang menunjukkan sumber API yang sedang aktif.
 * Membuat jelas bahwa kedua sumber (Nexadev + Sankavollerei) dipakai.
 */
@Composable
private fun SourceBanner(sources: List<String>) {
    val label = sources.joinToString(" + ") { src ->
        if (src.contains("nexadev", true)) "Nexadev" else "Sankavollerei"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF22C55E))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Sumber: $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
