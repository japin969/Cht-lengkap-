package com.jayra.app.ui.komik

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
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
import com.jayra.app.data.KomikItem
import com.jayra.app.ui.media.CoverImage
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaIconButton
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.SectionHeader

private val KomikAccent = Color(0xFFF97316)

/**
 * Home Komik — layout modern bergaya Manga Plus / Webtoon.
 *
 * App bar: judul + aksi Search / Bookmark / History.
 * Konten : hero (komik unggulan) → carousel "Populer" → grid "Terbaru".
 */
@Composable
fun KomikHomeScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (String) -> Unit,
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
        KomikHomeTopBar(
            onSearchClick = onSearchClick,
            onBookmarkClick = onBookmarkClick,
            onHistoryClick = onHistoryClick
        )

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.latest.isEmpty() -> MediaLoading()

                state.homeError != null && state.latest.isEmpty() -> MediaErrorState(
                    message = state.homeError,
                    onRetry = { viewModel.retry() }
                )

                state.latest.isEmpty() -> MediaEmptyState(
                    title = "Belum ada komik",
                    message = "Daftar komik sedang kosong. Coba muat ulang.",
                    icon = Icons.Rounded.MenuBook,
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.retry() }
                )

                else -> KomikHomeContent(
                    latest = state.latest,
                    popular = state.popular,
                    onKomikClick = onKomikClick,
                    onViewAllClick = onViewAllClick
                )
            }
        }
    }
}

@Composable
private fun KomikHomeTopBar(
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
                    text = "Komik",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Baca chapter terbaru setiap hari",
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
private fun KomikHomeContent(
    latest: List<KomikItem>,
    popular: List<KomikItem>,
    onKomikClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        latest.firstOrNull()?.let { hero ->
            item(key = "hero") { KomikHeroCard(hero) { onKomikClick(it) } }
        }

        if (popular.isNotEmpty()) {
            item(key = "populer-header") {
                SectionHeader(
                    title = "Populer",
                    actionLabel = "Lihat semua",
                    onAction = onViewAllClick
                )
            }
            item(key = "populer-row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(popular, key = { it.slug ?: it.title ?: "" }) { komik ->
                        KomikPosterCard(komik) { onKomikClick(it) }
                    }
                }
            }
        }

        item(key = "terbaru-header") {
            SectionHeader(
                title = "Terbaru",
                actionLabel = "Lihat semua",
                onAction = onViewAllClick
            )
        }
        item(key = "terbaru-grid") {
            KomikGrid(
                items = latest,
                onClick = onKomikClick
            )
        }
    }
}

/** Grid 3 kolom non-lazy (jumlah item dibatasi oleh pemanggil). */
@Composable
internal fun KomikGrid(
    items: List<KomikItem>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = items.chunked(3)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { komik ->
                    Box(Modifier.weight(1f)) {
                        KomikGridItem(komik) { onClick(it) }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KomikHeroCard(komik: KomikItem, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick(komik.slug ?: "") }
    ) {
        CoverImage(
            url = komik.cover,
            contentDescription = komik.title,
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
                        0.55f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.92f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Surface(shape = RoundedCornerShape(50), color = KomikAccent) {
                Text(
                    text = "PILIHAN",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = komik.title ?: "Tanpa judul",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!komik.updatedAt.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = komik.updatedAt ?: "",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun KomikPosterCard(komik: KomikItem, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(komik.slug ?: "") }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            CoverImage(
                url = komik.cover,
                contentDescription = komik.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(14.dp)
            )
            if (!komik.updatedAt.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = komik.updatedAt?.take(12) ?: "",
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
            text = komik.title ?: "Tanpa judul",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KomikGridItem(komik: KomikItem, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(komik.slug ?: "") }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            CoverImage(
                url = komik.cover,
                contentDescription = komik.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(12.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = komik.title ?: "Tanpa judul",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}