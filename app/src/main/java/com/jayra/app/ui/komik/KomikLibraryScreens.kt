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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.KomikItem
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.media.CoverImage
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.MediaTopBar

/**
 * Daftar Komik (grid penuh) — dipakai untuk "Lihat semua".
 * Mendukung pencarian lokal di header dan filter sederhana.
 */
@Composable
fun KomikListScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val items = remember(state.latest, query) {
        val all = state.latest
        if (query.isBlank()) all
        else all.filter { it.title?.contains(query, ignoreCase = true) == true }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(title = "Semua Komik", onBack = onBack)

        KomikSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Cari judul komik..."
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
                    message = "Daftar komik sedang kosong.",
                    icon = Icons.Rounded.MenuBook,
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.retry() }
                )

                items.isEmpty() -> MediaEmptyState(
                    title = "Tidak ditemukan",
                    message = "Tidak ada komik yang cocok dengan \"$query\".",
                    icon = Icons.Rounded.Search
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "count") {
                        Text(
                            text = "${items.size} komik",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    item(key = "grid") {
                        KomikGrid(items = items, onClick = onKomikClick)
                    }
                }
            }
        }
    }
}

/** Kolom pencarian reusable untuk halaman komik. */
@Composable
fun KomikSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Cari...",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text("Hapus", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Bookmark Komik — daftar komik yang disimpan pengguna.
 * Sumber data lokal (MediaLibraryStore), tidak butuh API.
 */
@Composable
fun KomikBookmarkScreen(
    onKomikClick: (String) -> Unit,
    onBack: () -> Unit = {},
    onBrowseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(MediaLibraryStore.bookmarks(context)) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        entries = MediaLibraryStore.bookmarks(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(
            title = "Bookmark",
            subtitle = "${entries.size} komik disimpan",
            onBack = onBack
        )

        Box(Modifier.fillMaxSize()) {
            if (entries.isEmpty()) {
                MediaEmptyState(
                    title = "Belum ada bookmark",
                    message = "Simpan komik favoritmu dengan menekan ikon bookmark di halaman detail.",
                    icon = Icons.Rounded.Bookmark,
                    actionLabel = "Jelajahi Komik",
                    onAction = onBrowseClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries, key = { it.kind + it.id }) { entry ->
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
}

/**
 * History Komik — riwayat baca.
 */
@Composable
fun KomikHistoryScreen(
    onKomikClick: (String) -> Unit,
    onBack: () -> Unit = {},
    onBrowseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(MediaLibraryStore.history(context, "komik")) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        entries = MediaLibraryStore.history(context, "komik")
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(
            title = "Riwayat Baca",
            subtitle = "${entries.size} komik",
            onBack = onBack,
            actions = {
                if (entries.isNotEmpty()) {
                    TextButton(onClick = {
                        MediaLibraryStore.clearHistory(context)
                        refresh++
                    }) { Text("Hapus Semua") }
                }
            }
        )

        Box(Modifier.fillMaxSize()) {
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries, key = { it.kind + it.id }) { entry ->
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
}

/**
 * Satu baris entri library (bookmark/history).
 * Dipakai bersama oleh halaman Komik & Anime.
 */
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
        CoverImage(
            url = entry.image,
            contentDescription = entry.title,
            modifier = Modifier
                .width(52.dp)
                .height(72.dp),
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title.ifBlank { "Tanpa judul" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!entry.subtitle.isNullOrBlank() || !entry.extra.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOfNotNull(
                        entry.subtitle?.takeIf { it.isNotBlank() },
                        entry.extra?.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRemove)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}