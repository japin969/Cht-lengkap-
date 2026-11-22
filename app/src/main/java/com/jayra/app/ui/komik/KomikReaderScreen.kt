package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.MediaTopBar
import com.jayra.app.ui.media.NetworkImage

/**
 * Reader Komik — daftar halaman vertikal.
 *
 * - Setiap halaman punya placeholder native (tidak blank).
 * - Top bar dengan tombol back.
 * - Progress halaman.
 */
@Composable
fun KomikReaderScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(slug) { viewModel.loadStream(slug) }

    // Catat riwayat baca saat halaman berhasil dimuat.
    LaunchedEffect(state.pages.size) {
        if (state.pages.isNotEmpty()) {
            MediaLibraryStore.recordHistory(
                context,
                MediaLibraryStore.Entry(
                    kind = "komik",
                    id = slug,
                    title = state.streamTitle ?: "Chapter",
                    subtitle = "Komik",
                    extra = "Chapter terakhir"
                )
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0D))
    ) {
        MediaTopBar(
            title = state.streamTitle?.takeIf { it.isNotBlank() } ?: "Membaca",
            subtitle = if (state.pages.isNotEmpty()) "${state.pages.size} halaman" else null,
            onBack = onBack
        )

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.pages.isEmpty() -> MediaLoading()

                state.error != null && state.pages.isEmpty() -> MediaErrorState(
                    message = state.error,
                    title = "Halaman tidak bisa dimuat",
                    onRetry = { viewModel.retryStream() }
                )

                state.pages.isEmpty() -> MediaEmptyState(
                    title = "Halaman kosong",
                    message = "Chapter ini tidak punya halaman yang bisa dibaca.",
                    icon = Icons.Rounded.Image
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = state.pages,
                        key = { it.page ?: it.url ?: "" }
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            NetworkImage(
                                url = page.url,
                                contentDescription = "Halaman ${page.page}",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth,
                                shape = RoundedCornerShape(0.dp),
                                fallbackIcon = Icons.Rounded.Image
                            )
                        }
                    }
                }
            }
        }
    }
}