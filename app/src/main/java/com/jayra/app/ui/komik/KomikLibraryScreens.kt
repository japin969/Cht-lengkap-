package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.KomikItem
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaLoading
import kotlinx.coroutines.delay

/**
 * Halaman pendukung Komik: List, Search, Bookmark, History.
 * Semua data dari Nexadev (`/komik`, `/komik?q=`).
 */

@Composable
private fun KomikSubPageScaffold(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
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
                }
            }
            Box(Modifier.fillMaxSize()) { content() }
        }
}

/** Daftar semua komik (grid) + pencarian. */
@Composable
fun KomikListScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<KomikItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

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

    val list = if (hasSearched) results else state.latest

    KomikSubPageScaffold(
        title = "Semua Komik",
        subtitle = "${list.size} judul",
        onBack = onBack
    ) {
        Column(Modifier.fillMaxSize()) {
            KomikSearchField(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" }
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
                    state.error != null && state.latest.isEmpty() && !hasSearched ->
                        MediaErrorState(
                            message = state.error,
                            title = "Gagal memuat komik",
                            onRetry = { viewModel.retry() }
                        )
                    list.isEmpty() -> MediaEmptyState(
                        title = if (hasSearched) "Tidak ditemukan" else "Belum ada komik",
                        message = if (hasSearched)
                            "Tidak ada komik yang cocok dengan \"$query\"."
                        else "Daftar komik sedang kosong.",
                        icon = Icons.Rounded.MenuBook,
                        actionLabel = if (hasSearched) null else "Coba Lagi",
                        onAction = if (hasSearched) null else ({ viewModel.retry() })
                    )
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item { KomikGrid(items = list, onClick = onKomikClick) }
                    }
                }
            }
        }
    }
}

/** Pencarian komik. */
@Composable
fun KomikSearchScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<KomikItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var history by remember { mutableStateOf(MediaLibraryStore.searchHistory(context, "komik")) }

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
        // Baru di sini history disimpan (pencarian benar-benar dijalankan).
        MediaLibraryStore.recordSearch(context, "komik", query.trim())
        history = MediaLibraryStore.searchHistory(context, "komik")
    }

    KomikSubPageScaffold(title = "Cari Komik", onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            KomikSearchField(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" }
            )
            Box(Modifier.fillMaxSize()) {
                when {
                    searching -> MediaLoading()
                    searchError != null && results.isEmpty() -> MediaErrorState(
                        message = searchError,
                        title = "Pencarian gagal",
                        onRetry = { query = query }
                    )
                    hasSearched && results.isEmpty() -> MediaEmptyState(
                        title = "Tidak ditemukan",
                        message = "Tidak ada komik yang cocok dengan \"$query\".",
                        icon = Icons.Rounded.Search
                    )
                    hasSearched -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Text(
                                text = "${results.size} hasil untuk \"$query\"",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        item { KomikGrid(items = results, onClick = onKomikClick) }
                    }
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (history.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pencarian Terakhir",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 16.dp)
                                    )
                                    TextButton(onClick = {
                                        MediaLibraryStore.clearSearchHistory(context, "komik")
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
                                            MediaLibraryStore.recordSearch(context, "komik", h)
                                            history = MediaLibraryStore.searchHistory(context, "komik")
                                        }
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = h,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(onClick = {
                                        MediaLibraryStore.removeSearch(context, "komik", h)
                                        history = MediaLibraryStore.searchHistory(context, "komik")
                                    }) { Text("Hapus", style = MaterialTheme.typography.labelMedium) }
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                        item {
                            SectionHeader(
                                title = "Rekomendasi",
                                subtitle = "Paling banyak diikuti"
                            )
                        }
                        if (state.latest.isNotEmpty()) {
                            item { KomikGrid(items = state.latest, onClick = onKomikClick) }
                        }
                    }
                }
            }
        }
    }
}

/** Bookmark komik (data lokal). */
@Composable
fun KomikBookmarkScreen(
    onKomikClick: (String) -> Unit,
    onBack: () -> Unit = {},
    onBrowseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var entries by remember {
        mutableStateOf(MediaLibraryStore.bookmarks(context).filter { it.kind == "komik" })
    }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        entries = MediaLibraryStore.bookmarks(context).filter { it.kind == "komik" }
    }

    KomikSubPageScaffold(
        title = "Bookmark",
        subtitle = "${entries.size} komik",
        onBack = onBack
    ) {
        if (entries.isEmpty()) {
            MediaEmptyState(
                title = "Belum ada bookmark",
                message = "Simpan komik favoritmu dari halaman detail.",
                icon = Icons.Rounded.Bookmark,
                actionLabel = "Jelajahi Komik",
                onAction = onBrowseClick
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    LibraryEntryRow(
                        entry = entry,
                        onClick = { onKomikClick(entry.id) },
                        onRemove = {
                            MediaLibraryStore.removeBookmark(context, entry.kind, entry.id)
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

/** Riwayat baca komik. */
@Composable
fun KomikHistoryScreen(
    onKomikClick: (String) -> Unit,
    onBack: () -> Unit = {},
    onBrowseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var entries by remember {
        mutableStateOf(MediaLibraryStore.history(context).filter { it.kind == "komik" })
    }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        entries = MediaLibraryStore.history(context).filter { it.kind == "komik" }
    }

    KomikSubPageScaffold(
        title = "Riwayat Baca",
        subtitle = "${entries.size} komik",
        onBack = onBack
    ) {
        if (entries.isEmpty()) {
            MediaEmptyState(
                title = "Belum ada riwayat",
                message = "Komik yang kamu baca akan muncul di sini.",
                icon = Icons.Rounded.History,
                actionLabel = "Jelajahi Komik",
                                onAction = onBrowseClick
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    LibraryEntryRow(
                        entry = entry,
                        onClick = { onKomikClick(entry.id) },
                        onRemove = {
                            MediaLibraryStore.removeHistory(context, entry.kind, entry.id)
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

/** Satu baris entri bookmark/history. */
@Composable
fun LibraryEntryRow(
    entry: MediaLibraryStore.Entry,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = entry.image,
            contentDescription = entry.title,
            modifier = Modifier
                .width(52.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title.ifBlank { "Tanpa judul" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF111111)
            )
            if (!entry.subtitle.isNullOrBlank() || !entry.extra.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOfNotNull(
                        entry.subtitle?.takeIf { it.isNotBlank() },
                        entry.extra?.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF555555),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onRemove != null) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}