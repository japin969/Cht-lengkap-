package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.MediaLibraryStore

/**
 * Halaman pendukung Anime: List, Search, Bookmark.
 * Semua data dari Nexadev (`/animasu?...`).
 */

// ═══════════════════════════════════════════════════════════════════
// SEARCH FIELD bersama
// ═══════════════════════════════════════════════════════════════════

/** Kotak pencarian anime. */
@Composable
fun AnimeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Cari judul anime...",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(onClick = { onQueryChange("") }) {
                    Text("Hapus", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// FILTER ROW
// ═══════════════════════════════════════════════════════════════════

/** Baris filter: Status + Sort. */
@Composable
fun AnimeFilterRow(
    status: AnimeStatus?,
    sort: AnimeSort,
    onStatusChange: (AnimeStatus?) -> Unit,
    onSortChange: (AnimeSort) -> Unit,
    modifier: Modifier = Modifier
) {
    // Wrap ke baris berikutnya kalau tidak muat — TIDAK ada scroll horizontal.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimeFilterChip(
                label = "Semua",
                selected = status == null,
                onClick = { onStatusChange(null) }
            )
            AnimeFilterChip(
                label = "Ongoing",
                selected = status == AnimeStatus.ONGOING,
                onClick = { onStatusChange(AnimeStatus.ONGOING) }
            )
            AnimeFilterChip(
                label = "Completed",
                selected = status == AnimeStatus.COMPLETED,
                onClick = { onStatusChange(AnimeStatus.COMPLETED) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimeFilterChip(
                label = "Populer",
                selected = status == AnimeStatus.POPULAR,
                onClick = { onStatusChange(AnimeStatus.POPULAR) }
            )
            AnimeFilterChip(
                label = sort.label,
                selected = true,
                onClick = {
                    onSortChange(
                        when (sort) {
                            AnimeSort.TERBARU -> AnimeSort.TERPOPULER
                            AnimeSort.TERPOPULER -> AnimeSort.AZ
                            AnimeSort.AZ -> AnimeSort.TERBARU
                        }
                    )
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// LIST
// ═══════════════════════════════════════════════════════════════════

/**
 * Daftar semua anime dengan pencarian + filter status + sort.
 */
@Composable
fun AnimeListScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val enrichment by viewModel.enrichment.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AnimeSearchItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<AnimeStatus?>(null) }
    var sort by remember { mutableStateOf(AnimeSort.TERBARU) }

    // Debounce pencarian.
    LaunchedEffect(query) {
        if (query.isBlank()) {
            hasSearched = false
            searching = false
            searchError = null
            searchResults = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(350)
        searching = true
        searchError = null
        val outcome = runCatching { viewModel.search(query.trim()) }
            .fold(
                onSuccess = { it to null },
                onFailure = { emptyList<AnimeSearchItem>() to it.localizedMessage }
            )
        searchResults = outcome.first
        searchError = outcome.second
        searching = false
        hasSearched = true
    }

    // Sumber data mengikuti filter status.
    val source: List<AnimeSearchItem> = when {
        hasSearched -> searchResults
        status == AnimeStatus.ONGOING -> state.ongoing
        status == AnimeStatus.COMPLETED -> state.completed
        status == AnimeStatus.POPULAR -> state.popular
        else -> (state.ongoing + state.popular + state.completed + state.recent)
            .distinctBy { it.link ?: it.title }
    }

    val filtered = remember(source, sort) {
        when (sort) {
            AnimeSort.TERBARU -> source
            AnimeSort.TERPOPULER -> source.sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }
            AnimeSort.AZ -> source.sortedBy {
                it.title?.lowercase().orEmpty()
            }
        }
    }

    AnimeSubPage(
        title = "Semua Anime",
        subtitle = "${filtered.size} judul",
        onBack = onBack
    ) {
        Column(Modifier.fillMaxSize()) {
            AnimeSearchField(
                query = query,
                onQueryChange = { query = it }
            )
            AnimeFilterRow(
                status = status,
                sort = sort,
                onStatusChange = { status = it },
                onSortChange = { sort = it }
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    searching -> AnimeLoading()

                    searchError != null && searchResults.isEmpty() -> AnimeErrorState(
                        message = searchError,
                        title = "Pencarian gagal"
                    )

                    state.isLoading && state.popular.isEmpty() && !hasSearched -> AnimeLoading()

                    state.homeError != null && state.popular.isEmpty() && !hasSearched ->
                        AnimeErrorState(
                            message = state.homeError,
                            title = "Gagal memuat anime",
                            onRetry = { viewModel.retry() }
                        )

                    filtered.isEmpty() -> AnimeEmptyState(
                        title = if (hasSearched) "Tidak ditemukan" else "Belum ada anime",
                        message = if (hasSearched)
                            "Tidak ada anime yang cocok dengan \"$query\"."
                        else "Daftar anime sedang kosong.",
                        actionLabel = if (hasSearched) null else "Coba Lagi",
                        onAction = if (hasSearched) null else ({ viewModel.retry() })
                    )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filtered, key = { it.link ?: it.title ?: "" }) { anime ->
                            AnimePosterCard(
                                anime = anime,
                                enrichment = enrichment[anime.link],
                                showStatus = true,
                                onClick = { onAnimeClick(anime.link ?: "") }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SEARCH
// ═══════════════════════════════════════════════════════════════════

/** Pencarian anime dengan hasil langsung (tanpa enter). */
@Composable
fun AnimeSearchScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val enrichment by viewModel.enrichment.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AnimeSearchItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<AnimeStatus?>(null) }
    var sort by remember { mutableStateOf(AnimeSort.TERBARU) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var history by remember { mutableStateOf(MediaLibraryStore.searchHistory(context, "anime")) }

    // Debounce pencarian. Mengetik SAJA tidak menyimpan history.
    LaunchedEffect(query) {
        if (query.isBlank()) {
            hasSearched = false
            searching = false
            searchError = null
            results = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(350)
        searching = true
        searchError = null
        val outcome = runCatching { viewModel.search(query.trim()) }
            .fold(
                onSuccess = { it to null },
                onFailure = { emptyList<AnimeSearchItem>() to it.localizedMessage }
            )
        results = outcome.first
        searchError = outcome.second
        searching = false
        hasSearched = true
        // History HANYA disimpan setelah pencarian benar-benar dijalankan.
        MediaLibraryStore.recordSearch(context, "anime", query.trim())
        history = MediaLibraryStore.searchHistory(context, "anime")
    }

    // Filter status hanya berlaku bila datanya tersedia di sumber lokal.
    val filtered = remember(results, status, sort, hasSearched) {
        val byStatus = if (hasSearched || status == null) results
        else when (status) {
            AnimeStatus.ONGOING -> state.ongoing
            AnimeStatus.COMPLETED -> state.completed
            AnimeStatus.POPULAR -> state.popular
            null -> results
        }
        when (sort) {
            AnimeSort.TERBARU -> byStatus
            AnimeSort.TERPOPULER -> byStatus.sortedByDescending {
                it.rating?.toDoubleOrNull() ?: 0.0
            }
            AnimeSort.AZ -> byStatus.sortedBy { it.title?.lowercase().orEmpty() }
        }
    }

    AnimeSubPage(title = "Cari Anime", onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            AnimeSearchField(
                query = query,
                onQueryChange = { query = it }
            )
            AnimeFilterRow(
                status = status,
                sort = sort,
                onStatusChange = { status = it },
                onSortChange = { sort = it }
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    searching -> AnimeLoading()

                    searchError != null && results.isEmpty() -> AnimeErrorState(
                        message = searchError,
                        title = "Pencarian gagal"
                    )

                    hasSearched && filtered.isEmpty() -> AnimeEmptyState(
                        title = "Tidak ditemukan",
                        message = "Tidak ada anime yang cocok dengan \"$query\".",
                        actionLabel = null,
                        onAction = null
                    )

                    hasSearched -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filtered, key = { it.link ?: it.title ?: "" }) { anime ->
                            AnimePosterCard(
                                anime = anime,
                                enrichment = enrichment[anime.link],
                                onClick = { onAnimeClick(anime.link ?: "") }
                            )
                        }
                    }

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "hdr") {
                            Text(
                                text = "Rekomendasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        if (history.isNotEmpty()) {
                            item(key = "hist_hdr") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pencarian Terakhir",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = {
                                        MediaLibraryStore.clearSearchHistory(context, "anime")
                                        history = emptyList()
                                    }) { Text("Hapus Semua") }
                                }
                            }
                            items(history, key = { it }) { h ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            query = h
                                            // Klik history = aksi eksplisit, simpan lagi.
                                            MediaLibraryStore.recordSearch(context, "anime", h)
                                            history = MediaLibraryStore.searchHistory(context, "anime")
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = h,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(onClick = {
                                        MediaLibraryStore.removeSearch(context, "anime", h)
                                        history = MediaLibraryStore.searchHistory(context, "anime")
                                    }) { Text("Hapus", style = MaterialTheme.typography.labelMedium) }
                                }
                            }
                            item(key = "hist_gap") { Spacer(Modifier.height(8.dp)) }
                        }
                        items(filtered, key = { it.link ?: it.title ?: "" }) { anime ->
                            AnimePosterCard(
                                anime = anime,
                                enrichment = enrichment[anime.link],
                                onClick = {
                                    // Klik hasil = aksi eksplisit, simpan history.
                                    MediaLibraryStore.recordSearch(context, "anime", query)
                                    onAnimeClick(anime.link ?: "")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// BOOKMARK
// ═══════════════════════════════════════════════════════════════════

/** Bookmark anime (data lokal). */
@Composable
fun AnimeBookmarkScreen(
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit = {},
    onBrowseClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var entries by remember {
        mutableStateOf(
            com.jayra.app.data.MediaLibraryStore.bookmarks(context)
                .filter { it.kind == "anime" }
        )
    }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        entries = com.jayra.app.data.MediaLibraryStore.bookmarks(context)
            .filter { it.kind == "anime" }
    }

    AnimeSubPage(
        title = "Bookmark",
        subtitle = "${entries.size} anime",
        onBack = onBack
    ) {
        if (entries.isEmpty()) {
            AnimeEmptyState(
                title = "Belum ada bookmark",
                message = "Simpan anime favoritmu dari halaman detail.",
                actionLabel = "Jelajahi Anime",
                onAction = onBrowseClick
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    com.jayra.app.ui.komik.LibraryEntryRow(
                        entry = entry,
                        onClick = { onAnimeClick(entry.id) },
                        onRemove = {
                            com.jayra.app.data.MediaLibraryStore
                                .removeBookmark(context, entry.kind, entry.id)
                            refresh++
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SCAFFOLD bersama
// ═══════════════════════════════════════════════════════════════════

/** Kerangka halaman anime dengan app bar back. */
@Composable
fun AnimeSubPage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    com.jayra.app.ui.media.MediaGlassTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        com.jayra.app.ui.media.MediaGlassColors.BackgroundGradient
                    )
                )
        ) {
            MediaTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack
            )
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}