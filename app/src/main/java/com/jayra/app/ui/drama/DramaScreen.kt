package com.jayra.app.ui.drama

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
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.DramaItem
import com.jayra.app.ui.media.MediaGlassColors
import com.jayra.app.ui.media.MediaGlassTheme
import com.jayra.app.ui.media.MediaTheme
import com.jayra.app.ui.media.jellyClick

private val DramaAccent = Color(0xFFFF375F)

/**
 * Home Drama — iOS 18 Glass, LEBAR & LEGA (2 kolom besar), beberapa section:
 * Trending, For You, Hot Rank, Recommended. Semua dari cache-first (anti-limit).
 */
@Composable
fun DramaScreen(
    viewModel: DramaViewModel = viewModel(),
    onDramaClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MediaGlassTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(MediaGlassColors.BackgroundGradient))
        ) {
            DramaTopBar()

            Box(Modifier.fillMaxSize()) {
                val hasAny = state.sections.values.any { it.isNotEmpty() }
                when {
                    state.isLoading && !hasAny -> MediaLoading()
                    state.homeError != null && !hasAny -> MediaError(
                        message = state.homeError,
                        onRetry = { viewModel.retry() }
                    )
                    !hasAny -> MediaEmpty(onRetry = { viewModel.retry() })
                    else -> DramaHomeContent(
                        sections = state.sections,
                        offline = state.offline,
                        onDramaClick = onDramaClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DramaTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPaddingCompat()
            .height(60.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Drama",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MediaGlassColors.OnSurface,
                maxLines = 1
            )
            Text(
                text = "Tontonan pendek, episode baru tiap hari",
                style = MaterialTheme.typography.labelMedium,
                color = MediaGlassColors.OnSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun Modifier.statusBarsPaddingCompat(): Modifier =
    this.then(statusBarsPadding())

@Composable
private fun DramaHomeContent(
    sections: Map<String, List<DramaItem>>,
    offline: Boolean,
    onDramaClick: (String) -> Unit
) {
    // Hero = item pertama Trending (atau section pertama yang ada).
    val hero = sections["Trending"]?.firstOrNull()
        ?: sections.values.firstOrNull()?.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        hero?.let { h ->
            item(key = "hero") {
                DramaHero(
                    drama = h,
                    onClick = { onDramaClick(h.stableId ?: "") }
                )
            }
        }

        // Section: Trending, For You, Hot Rank, Recommended (skip yang kosong).
        sections.forEach { (title, items) ->
            val list = items.filter { it.stableId != hero?.stableId || title != "Trending" }
            if (list.isNotEmpty()) {
                item(key = "sec_$title") {
                    DramaSectionHeader(
                        title = title,
                        subtitle = "${list.size} judul"
                    )
                }
                item(key = "grid_$title") {
                    DramaGrid(items = list, onClick = onDramaClick)
                }
            }
        }

        if (offline) {
            item(key = "offline") {
                OfflineNote()
            }
        }
    }
}

@Composable
private fun DramaHero(drama: DramaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MediaGlassColors.SurfaceVariant)
            .jellyClick(onClick = onClick)
    ) {
        AsyncImage(
            model = drama.bestCover,
            contentDescription = drama.title,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            imageLoader = com.jayra.app.ui.media.LocalMediaImageLoader.current
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.1f to Color.Transparent,
                        0.55f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.94f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Putar",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Surface(shape = RoundedCornerShape(50), color = DramaAccent) {
                Text(
                    text = if (drama.completed) "TAMAT" else "ONGOING",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = drama.title ?: "Tanpa judul",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = buildString {
                    drama.epsCount?.let { append("$it Episode") }
                },
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/** Grid 2 kolom besar (lega) — scroll vertical. */
@Composable
private fun DramaGrid(items: List<DramaItem>, onClick: (String) -> Unit) {
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
                rowItems.forEach { drama ->
                    Box(Modifier.weight(1f)) {
                        DramaCard(drama) { onClick(it) }
                    }
                }
                repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Kartu poster drama — glass, sudut membulat besar, fill cell. */
@Composable
fun DramaCard(drama: DramaItem, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MediaTheme.CardRadius))
            .background(MediaGlassColors.Surface)
            .border(1.dp, MediaGlassColors.GlassBorder, RoundedCornerShape(MediaTheme.CardRadius))
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(MediaTheme.CardRadius),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .jellyClick(onClick = { onClick(drama.stableId ?: "") }, pressedScale = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(MediaTheme.CardRadius))
                .background(MediaGlassColors.SurfaceVariant)
        ) {
            AsyncImage(
                model = drama.bestCover,
                contentDescription = drama.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                imageLoader = com.jayra.app.ui.media.LocalMediaImageLoader.current
            )
            // Badge episode di atas poster (kaca).
            drama.epsCount?.let { n ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color.White.copy(alpha = 0.22f)
                    )
                ) {
                    Text(
                        text = "$n eps",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = drama.title ?: "Tanpa judul",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MediaGlassColors.OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = if (drama.completed) "Tamat" else "Ongoing",
            style = MaterialTheme.typography.labelSmall,
            color = MediaGlassColors.OnSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun DramaSectionHeader(title: String, subtitle: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MediaGlassColors.OnSurface,
            maxLines = 1
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MediaGlassColors.OnSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OfflineNote() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x33FF9500)
    ) {
        Text(
            text = "Sedang offline — menampilkan data lama.",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFFFB340),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// ── State helpers ─────────────────────────────────────────────────────

@Composable
private fun MediaLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MediaGlassColors.Accent)
    }
}

@Composable
private fun MediaError(message: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Movie, null, tint = MediaGlassColors.OnSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Gagal memuat drama", style = MaterialTheme.typography.titleMedium, color = MediaGlassColors.OnSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message ?: "Koneksi bermasalah.", style = MaterialTheme.typography.bodyMedium, color = MediaGlassColors.OnSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
private fun MediaEmpty(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Movie, null, tint = MediaGlassColors.OnSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Belum ada drama", style = MaterialTheme.typography.titleMedium, color = MediaGlassColors.OnSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Daftar drama sedang kosong. Coba muat ulang.", style = MaterialTheme.typography.bodyMedium, color = MediaGlassColors.OnSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Coba Lagi") }
    }
}