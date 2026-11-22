package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.KomikItem
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.CoverImage
import com.jayra.app.ui.media.MediaLoading
import kotlinx.coroutines.delay

/** Aksen warna untuk bagian komik. */
private val KomikAccent = Color(0xFFF97316)

/** Pilihan filter yang tersedia berdasarkan data Nexadev. */
private enum class KomikFilter(val label: String) {
    TERBARU("Terbaru"),
    A_Z("Judul A-Z"),
    Z_A("Judul Z-A"),
    RATING("Rating Tertinggi")
}

/**
 * Home Komik — modern, dengan Search + Filter yang benar-benar bekerja
 * memakai data dari Nexadev (`/komik` dan `/komik?q=`).
 *
 * Poster/cover TIDAK diubah dari versi sebelumnya.
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

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<KomikItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(KomikFilter.TERBARU) }

    // Debounce pencarian ke Nexadev.
    LaunchedEffect(query) {
        if (query.isBlank()) {
            hasSearched = false
            searching = false
            searchError = null
            results = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        searchError = null
        val outcome = runCatching { viewModel.search(query.trim()) }
            .fold(
                onSuccess = { it to null },
                onFailure = { emptyList<KomikItem>() to it.localizedMessage }
            )
        results = outcome.first
        searchError = outcome.second
        searching = false
        hasSearched = true
    }

    // Terapkan filter pada sumber data aktif.
    val source: List<KomikItem> = if (hasSearched) results else state.latest
    val filtered: List<KomikItem> = remember(source, selectedFilter) {
        when (selectedFilter) {
            KomikFilter.TERBARU -> source
            KomikFilter.A_Z -> source.sortedBy {
                it.title?.removePrefix("Komik ")?.lowercase().orEmpty()
            }
            KomikFilter.Z_A -> source.sortedByDescending {
                it.title?.removePrefix("Komik ")?.lowercase().orEmpty()
            }
            KomikFilter.RATING -> source.sortedByDescending {
                it.rating?.toDoubleOrNull() ?: 0.0
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        KomikTopBar(
                onBookmarkClick = onBookmarkClick,
                onHistoryClick = onHistoryClick
            )

            KomikSearchField(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" }
            )

            FilterRow(
                selected = selectedFilter,
                onSelect = { selectedFilter = it }
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    searching -> MediaLoading()

                    searchError != null && results.isEmpty() -> MediaErrorState(
                        message = searchError,
                        title = "Pencarian gagal",
                        onRetry = { query = query }
                    )

                    state.isLoading && state.latest.isEmpty() && !hasSearched -> MediaLoading()

                    state.error != null && state.latest.isEmpty() && !hasSearched -> {
                        MediaErrorState(
                            message = state.error,
                            title = "Gagal memuat komik",
                            onRetry = { viewModel.retry() }
                        )
                    }

                    state.latest.isEmpty() && !hasSearched -> MediaEmptyState(
                        title = "Belum ada komik",
                        message = "Daftar komik sedang kosong. Coba muat ulang.",
                        icon = Icons.Rounded.MenuBook,
                        actionLabel = "Coba Lagi",
                        onAction = { viewModel.retry() }
                    )

                    filtered.isEmpty() -> MediaEmptyState(
                        title = if (hasSearched) "Tidak ditemukan" else "Tidak ada hasil",
                        message = if (hasSearched)
                            "Tidak ada komik yang cocok dengan \"$query\"."
                        else "Coba ubah filter atau muat ulang.",
                        icon = Icons.Rounded.Search,
                        actionLabel = if (hasSearched) null else "Coba Lagi",
                        onAction = if (hasSearched) null else ({ viewModel.retry() })
                    )

                    else -> KomikContent(
                        latest = state.latest,
                        filtered = filtered,
                        hasSearched = hasSearched,
                        onKomikClick = onKomikClick,
                        onViewAllClick = onViewAllClick,
                        onLoadMore = { viewModel.loadPage(2) }
                    )
                }
            }
        }
}

@Composable
private fun KomikTopBar(
    onBookmarkClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 16.dp),
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
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onBookmarkClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onHistoryClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Riwayat",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Kotak pencarian komik. */
@Composable
fun KomikSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            )
            if (query.isEmpty()) {
                Text(
                    text = "Cari judul komik...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Baris filter yang bisa di-scroll horizontal. */
@Composable
private fun FilterRow(
    selected: KomikFilter,
    onSelect: (KomikFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KomikFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
                leadingIcon = if (selected == filter) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun KomikContent(
    latest: List<KomikItem>,
    filtered: List<KomikItem>,
    hasSearched: Boolean,
    onKomikClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onLoadMore: () -> Unit
) {
    if (hasSearched) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.slug ?: it.title ?: "" }) { komik ->
                KomikCard(komik) { onKomikClick(it) }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hero
        latest.firstOrNull()?.let { hero ->
            item(key = "hero") { KomikHero(hero) { onKomikClick(it) } }
        }

        // Terbaru grid
        item(key = "latest_header") {
            SectionHeader(
                title = "Komik Terbaru",
                subtitle = "${filtered.size} judul",
                actionLabel = "Lihat semua",
                onAction = onViewAllClick
            )
        }
        item(key = "latest_grid") {
            KomikGrid(filtered, onClick = onKomikClick)
        }
    }
}

/** Grid 3 kolom non-lazy (dipakai di dalam LazyColumn). */
@Composable
fun KomikGrid(items: List<KomikItem>, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { komik ->
                    Box(Modifier.weight(1f)) {
                        KomikCard(komik) { onClick(it) }
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Hero besar untuk komik pilihan. */
@Composable
private fun KomikHero(komik: KomikItem, onClick: (String) -> Unit) {
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
                text = komik.title?.removePrefix("Komik ") ?: "Tanpa judul",
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

/** Header section dengan subtitle + aksi. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** Kartu komik — TIDAK diubah dari versi sebelumnya. */
@Composable
fun KomikCard(komik: KomikItem, onClick: (String) -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(10.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clickable { onClick(komik.slug ?: "") }
    ) {
        AsyncImage(
            model = komik.cover,
            contentDescription = komik.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(5.dp))
        Text(
            komik.title ?: "",
            maxLines = 2,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111111),
            overflow = TextOverflow.Ellipsis
        )
        Text(
            komik.updatedAt ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF555555),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(5.dp))
    }
}