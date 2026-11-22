package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaIconButton
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.MediaTopBar
import com.jayra.app.ui.media.MediaIconButton
import com.jayra.app.ui.media.NetworkImage

/**
 * Detail Komik — cover, judul, sinopsis expandable, daftar chapter,
 * tombol bookmark, dan tombol back.
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

    com.jayra.app.ui.media.MediaGlassTheme {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    com.jayra.app.ui.media.MediaGlassColors.BackgroundGradient
                )
            )
    ) {
        MediaTopBar(
            title = state.detail?.title ?: "Detail Komik",
            onBack = onBack,
            actions = {
                MediaIconButton(
                    icon = if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (bookmarked) "Hapus bookmark" else "Simpan bookmark",
                    onClick = {
                        bookmarked = MediaLibraryStore.toggleBookmark(
                            context,
                            MediaLibraryStore.Entry(
                                kind = "komik",
                                id = slug,
                                title = state.detail?.title ?: "Tanpa judul",
                                image = state.detail?.cover,
                                subtitle = "Komik"
                            )
                        )
                    },
                    active = bookmarked
                )
            }
        )

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.detail == null -> MediaLoading()

                state.error != null && state.detail == null -> MediaErrorState(
                    message = state.error,
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
                    synopsisExpanded = synopsisExpanded,
                    onToggleSynopsis = { synopsisExpanded = !synopsisExpanded },
                    onChapterClick = onChapterClick
                )
            }
        }
    }
    }
}

@Composable
private fun KomikDetailContent(
    state: KomikUiState,
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
        // ── Hero ───────────────────────────────────────────────
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                NetworkImage(
                    url = detail.cover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(0.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.15f to Color.Transparent,
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
                    NetworkImage(
                        url = detail.cover,
                        contentDescription = detail.title,
                        modifier = Modifier
                            .width(112.dp)
                            .height(164.dp),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Spacer(Modifier.height(26.dp))
                        Text(
                            text = detail.title ?: "Tanpa judul",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            com.jayra.app.ui.media.MediaChip(
                                text = "${state.chapters.size} Chapter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            detail.rating?.let { r ->
                                com.jayra.app.ui.media.MediaChip(
                                    text = r,
                                    tint = Color(0xFFFACC15)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Sinopsis ───────────────────────────────────────────
        val synopsisText = synopsis
        if (!synopsisText.isNullOrBlank()) {
            item(key = "synopsis") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Sinopsis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = synopsisText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (synopsisText.length > 160) {
                        TextButton(onClick = onToggleSynopsis) {
                            Text(
                                if (synopsisExpanded) "Tutup" else "Selengkapnya",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            }
        }

        // ── Chapter ────────────────────────────────────────────
        item(key = "ch_header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.MenuBook,
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
            item(key = "ch_empty") {
                MediaEmptyState(
                    title = "Belum ada chapter",
                    message = "Chapter untuk komik ini belum tersedia.",
                    icon = Icons.Rounded.MenuBook,
                    modifier = Modifier.height(220.dp)
                )
            }
        } else {
            items(
                items = state.chapters,
                key = { it.slug ?: it.title ?: "" }
            ) { ch ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChapterClick(ch.slug ?: "") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = ch.title?.takeIf { it.isNotBlank() } ?: "Chapter",
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
    }
}