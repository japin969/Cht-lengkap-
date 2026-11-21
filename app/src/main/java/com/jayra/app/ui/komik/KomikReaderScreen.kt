package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.jayra.app.ui.media.SafeCoverImage

/**
 * Reader Komik — daftar halaman vertikal dengan latar hitam.
 *
 * - Setiap halaman punya placeholder native (tidak blank / tidak crash).
 * - Progress bar tipis di atas menunjukkan halaman aktif.
 * - Tombol back selalu tersedia.
 */
@Composable
fun KomikReaderScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(slug) { viewModel.loadStream(slug) }

    // Catat riwayat baca.
    val pageCount = state.pages.size
    LaunchedEffect(pageCount) {
        if (pageCount > 0) {
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

    val current by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + 1).coerceAtMost(pageCount.coerceAtLeast(1))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MediaTopBar(
            title = state.streamTitle?.takeIf { it.isNotBlank() } ?: "Membaca",
            subtitle = if (pageCount > 0) "$current / $pageCount halaman" else null,
            onBack = onBack,
            containerColor = Color(0xFF101010)
        )

        // Progress bar tipis
        if (pageCount > 0) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF2A2A2A))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction = current.toFloat() / pageCount)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                state.detailLoading && state.pages.isEmpty() -> MediaLoading()

                state.detailError != null && state.pages.isEmpty() -> MediaErrorState(
                    message = state.detailError,
                    title = "Halaman tidak bisa dimuat",
                    onRetry = { viewModel.retryStream() }
                )

                state.pages.isEmpty() -> MediaEmptyState(
                    title = "Halaman kosong",
                    message = "Chapter ini tidak punya halaman yang bisa dibaca.",
                    icon = Icons.Rounded.Image,
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.retryStream() }
                )

                else -> LazyColumn(
                    state = listState,
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
                            SafeCoverImage(
                                url = page.url,
                                contentDescription = "Halaman ${page.page}",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth,
                                shape = RoundedCornerShape(0.dp)
                            )
                            // Nomor halaman di pojok
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${page.page ?: ""}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    item(key = "end-note") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "— Akhir chapter —",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}