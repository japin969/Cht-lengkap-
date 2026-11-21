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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.komik.KomikSearchField
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.MediaTopBar
import kotlinx.coroutines.delay

/**
 * Daftar Anime (grid penuh) — dipakai untuk "Lihat semua".
 * Menggabungkan semua sumber (sedang tayang + populer + selesai) dan
 * menyaring duplikat berdasarkan link.
 */
@Composable
fun AnimeListScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val all = remember(state.ongoing, state.popular, state.completed) {
        val merged = (state.ongoing + state.popular + state.completed)
        merged.distinctBy { it.link ?: it.title }
    }

    val items = remember(all, query) {
        if (query.isBlank()) all
        else all.filter { it.title?.contains(query, ignoreCase = true) == true }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(title = "Semua Anime", onBack = onBack)

        KomikSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Cari judul anime..."
        )

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && all.isEmpty() -> MediaLoading()

                state.homeError != null && all.isEmpty() -> MediaErrorState(
                    message = state.homeError,
                    onRetry = { viewModel.retry() }
                )

                all.isEmpty() -> MediaEmptyState(
                    title = "Belum ada anime",
                    message = "Daftar anime sedang kosong.",
                    icon = Icons.Rounded.Movie,
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.retry() }
                )

                items.isEmpty() -> MediaEmptyState(
                    title = "Tidak ditemukan",
                    message = "Tidak ada anime yang cocok dengan \"$query\".",
                    icon = Icons.Rounded.Search
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "${items.size} anime",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        AnimeGrid(items = items, onClick = onAnimeClick)
                    }
                }
            }
        }
    }
}

/** Grid 3 kolom untuk anime (non-lazy, dibatasi pemanggil). */
@Composable
fun AnimeGrid(
    items: List<AnimeSearchItem>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { anime ->
                    Box(Modifier.weight(1f)) {
                        AnimeGridItem(anime) { onClick(it) }
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AnimeGridItem(anime: AnimeSearchItem, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(anime.link ?: "") }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            com.jayra.app.ui.media.CoverImage(
                url = anime.image,
                contentDescription = anime.title,
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = anime.title ?: "Tanpa judul",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Pencarian Anime — memakai Kitsu search API.
 */
@Composable
fun AnimeSearchScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var searchTerm by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<AnimeSearchItem>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchTerm = ""
            hasSearched = false
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        delay(350)
        searchTerm = query.trim()
    }

    LaunchedEffect(searchTerm) {
        if (searchTerm.isBlank()) return@LaunchedEffect
        searching = true
        searchError = null
        val outcome = runCatching { viewModel.search(searchTerm) }
            .fold(
                onSuccess = { it to null },
                onFailure = { emptyList<AnimeSearchItem>() to it.localizedMessage }
            )
        results = outcome.first
        searchError = outcome.second
        searching = false
        hasSearched = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(title = "Cari Anime", onBack = onBack)

        KomikSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Ketik judul anime..."
        )

        Box(Modifier.fillMaxSize()) {
            when {
                searching -> MediaLoading()

                searchError != null && results.isEmpty() -> MediaErrorState(
                    message = searchError,
                    title = "Pencarian gagal"
                )

                hasSearched && results.isEmpty() -> MediaEmptyState(
                    title = "Tidak ditemukan",
                    message = "Tidak ada anime yang cocok dengan \"$searchTerm\".",
                    icon = Icons.Rounded.Search
                )

                hasSearched && results.isNotEmpty() -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "${results.size} hasil untuk \"$searchTerm\"",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item { AnimeGrid(items = results, onClick = onAnimeClick) }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "Pencarian populer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    item {
                        Text(
                            text = "Mulai mengetik judul anime untuk mencari.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (state.popular.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Rekomendasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        item { AnimeGrid(items = state.popular, onClick = onAnimeClick) }
                    }
                }
            }
        }
    }
}

/**
 * Bookmark Anime — daftar anime yang disimpan pengguna.
 */
@Composable
fun AnimeBookmarkScreen(
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit = {},
    onBrowseClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var entries by remember { mutableStateOf(MediaLibraryStore.bookmarks(context)) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        entries = MediaLibraryStore.bookmarks(context).filter { it.kind == "anime" }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(
            title = "Bookmark",
            subtitle = "${entries.size} anime disimpan",
            onBack = onBack
        )

        Box(Modifier.fillMaxSize()) {
            if (entries.isEmpty()) {
                MediaEmptyState(
                    title = "Belum ada bookmark",
                    message = "Simpan anime favoritmu dengan menekan ikon bookmark di halaman detail.",
                    icon = Icons.Rounded.Bookmark,
                    actionLabel = "Jelajahi Anime",
                    onAction = onBrowseClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries, key = { it.kind + it.id }) { entry ->
                        com.jayra.app.ui.komik.LibraryEntryRow(
                            entry = entry,
                            onClick = { onAnimeClick(entry.id) },
                            onRemove = {
                                MediaLibraryStore.removeBookmark(context, entry.kind, entry.id)
                                refresh++
                            }
                        )
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Riwayat Tonton Anime — daftar anime yang pernah dibuka.
 */
@Composable
fun AnimeHistoryScreen(
    onAnimeClick: (String) -> Unit,
    onBack: () -> Unit = {},
    onBrowseClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var entries by remember { mutableStateOf(MediaLibraryStore.history(context, "anime")) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        entries = MediaLibraryStore.history(context, "anime")
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(
            title = "Riwayat Tonton",
            subtitle = "${entries.size} anime",
            onBack = onBack,
            actions = {
                if (entries.isNotEmpty()) {
                    TextButton(onClick = {
                        // Hapus hanya riwayat anime, bukan komik.
                        MediaLibraryStore.history(context, "anime").forEach {
                            MediaLibraryStore.removeHistory(context, it.kind, it.id)
                        }
                        refresh++
                    }) { Text("Hapus Semua") }
                }
            }
        )

        Box(Modifier.fillMaxSize()) {
            if (entries.isEmpty()) {
                MediaEmptyState(
                    title = "Belum ada riwayat",
                    message = "Anime yang kamu buka akan muncul di sini.",
                    icon = Icons.Rounded.History,
                    actionLabel = "Jelajahi Anime",
                    onAction = onBrowseClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries, key = { it.kind + it.id }) { entry ->
                        com.jayra.app.ui.komik.LibraryEntryRow(
                            entry = entry,
                            onClick = { onAnimeClick(entry.id) },
                            onRemove = {
                                MediaLibraryStore.removeHistory(context, entry.kind, entry.id)
                                refresh++
                            }
                        )
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detail Episode Anime.
 *
 * Menampilkan info episode + tombol putar yang membuka [AnimePlayerScreen]
 * (native ExoPlayer, tanpa WebView).
 */
@Composable
fun AnimeEpisodeDetailScreen(
    episodeId: String,
    episodeTitle: String?,
    episodeNumber: String?,
    onPlay: () -> Unit,
    onBack: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(
            title = episodeTitle?.takeIf { it.isNotBlank() }
                ?: episodeNumber?.let { "Episode $it" }
                ?: "Episode",
            subtitle = episodeNumber?.let { "Episode $it" },
            onBack = onBack
        )

        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Thumbnail area dengan tombol play
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onPlay),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = "Putar episode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Text(
                    text = episodeTitle?.takeIf { it.isNotBlank() }
                        ?: episodeNumber?.let { "Episode $it" }
                        ?: "Episode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (episodeNumber != null) {
                        com.jayra.app.ui.media.MetaChip(text = "Episode $episodeNumber")
                    }
                    com.jayra.app.ui.media.MetaChip(text = "Subtitle")
                }

                androidx.compose.material3.Button(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Putar Episode")
                }

                Text(
                    text = "Diputar dengan pemutar video bawaan aplikasi.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}