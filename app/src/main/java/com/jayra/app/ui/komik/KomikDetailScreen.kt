package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.media.CoverImage
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaIconButton
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.MediaTopBar
import com.jayra.app.ui.media.MetaChip

/**
 * Detail Komik — cover besar, judul, meta, sinopsis yang bisa diperluas,
 * dan daftar chapter yang bisa diklik.
 */
@Composable
fun KomikDetailScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onChapterClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var bookmarked by remember(slug) { mutableStateOf(false) }
    var synopsisExpanded by remember(slug) { mutableStateOf(false) }

    LaunchedEffect(slug) {
        viewModel.loadDetail(slug)
        bookmarked = MediaLibraryStore.isBookmarked(context, "komik", slug)
    }

    // Catat riwayat saat detail berhasil dimuat.
    LaunchedEffect(state.detail?.title) {
        val detail = state.detail ?: return@LaunchedEffect
        if (detail.success == true) {
            MediaLibraryStore.recordHistory(
                context,
                MediaLibraryStore.Entry(
                    kind = "komik",
                    id = slug,
                    title = detail.title ?: "Tanpa judul",
                    image = detail.cover,
                    subtitle = "Komik"
                )
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(
            title = state.detail?.title ?: "Detail Komik",
            onBack = onBack,
            actions = {
                MediaIconButton(
                    icon = if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (bookmarked) "Hapus bookmark" else "Simpan bookmark",
                    onClick = {
                        val nowSaved = MediaLibraryStore.toggleBookmark(
                            context,
                            MediaLibraryStore.Entry(
                                kind = "komik",
                                id = slug,
                                title = state.detail?.title ?: "Tanpa judul",
                                image = state.detail?.cover,
                                subtitle = "Komik"
                            )
                        )
                        bookmarked = nowSaved
                    },
                    active = bookmarked
                )
            }
        )

        Box(Modifier.fillMaxSize()) {
            when {
                state.detailLoading && state.detail == null -> MediaLoading()

                state.detailError != null && state.detail == null -> MediaErrorState(
                    message = state.detailError,
                    title = "Detail tidak bisa dimuat",
                    onRetry = { viewModel.retryDetail() }
                )

                state.detail == null -> MediaEmptyState(
                    title = "Komik tidak ditemukan",
                    message = "Detail komik tidak tersedia.",
                    icon = Icons.Rounded.MenuBook,
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.retryDetail() }
                )

                else -> KomikDetailContent(
                    state = state,
                    slug = slug,
                    synopsisExpanded = synopsisExpanded,
                    onToggleSynopsis = { synopsisExpanded = !synopsisExpanded },
                    onChapterClick = onChapterClick
                )
            }
        }
    }
}

@Composable
private fun KomikDetailContent(
    state: KomikUiState,
    slug: String,
    synopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    onChapterClick: (String) -> Unit
) {
    val detail = state.detail ?: return
    val synopsis = detail.synopsis?.takeIf { it.isNotBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // ── Hero: cover + judul + meta ─────────────────────────
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                // Cover blur-ish backdrop
                CoverImage(
                    url = detail.cover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(0.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.2f to MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CoverImage(
                        url = detail.cover,
                        contentDescription = detail.title,
                        modifier = Modifier
                            .width(110.dp)
                            .height(160.dp),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = detail.title ?: "Tanpa judul",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetaChip(
                                text = "${state.chapters.size} Chapter",
                                icon = Icons.Rounded.MenuBook
                            )
                            if (state.chapters.isNotEmpty()) {
                                val latest = state.chapters.firstOrNull()
                                    ?.title?.substringAfter("Chapter ")?.substringBefore(" ")
                                if (!latest.isNullOrBlank()) {
                                    MetaChip(
                                        text = "Ch. $latest",
                                        icon = Icons.Rounded.Star
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Sinopsis ───────────────────────────────────────────
        if (!synopsis.isNullOrBlank()) {
            item(key = "synopsis") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Sinopsis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (synopsis.length > 180) {
                        TextButton(onClick = onToggleSynopsis) {
                            Text(
                                text = if (synopsisExpanded) "Tutup" else "Selengkapnya",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            }
        }

        // ── Daftar chapter ─────────────────────────────────────
        item(key = "chapter-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Daftar Chapter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${state.chapters.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.chapters.isEmpty()) {
            item(key = "chapter-empty") {
                MediaEmptyState(
                    title = "Belum ada chapter",
                    message = "Chapter untuk komik ini belum tersedia.",
                    icon = Icons.Rounded.MenuBook,
                    modifier = Modifier.height(260.dp)
                )
            }
        } else {
            items(
                items = state.chapters,
                key = { it.slug ?: it.title ?: "" }
            ) { chapter ->
                KomikChapterRow(
                    chapter = chapter,
                    index = state.chapters.indexOf(chapter),
                    onClick = { onChapterClick(chapter.slug ?: "") }
                )
            }
        }
    }
}

@Composable
private fun KomikChapterRow(
    chapter: KomikChapter,
    index: Int,
    onClick: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nomor urut dalam kotak
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = chapter.title?.takeIf { it.isNotBlank() } ?: "Chapter",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Baca chapter",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    }
}