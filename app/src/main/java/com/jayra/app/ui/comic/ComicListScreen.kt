package com.jayra.app.ui.comic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaStateView
import com.jayra.app.ui.media.PosterGridPlaceholder
import com.jayra.app.ui.theme.SFProFontFamily
import kotlinx.coroutines.launch

/**
 * ComicList — daftar semua komik / hasil filter genre. Grid 3 kolom,
 * infinite scroll, tombol scroll-to-top (pola aplikasi komik terkenal).
 */
@Composable
fun ComicListScreen(
    title: String = "Semua Komik",
    tagId: String? = null,
    onBack: () -> Unit,
    onComicClick: (String) -> Unit,
    viewModel: ComicViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(tagId) { viewModel.loadList(tagId = tagId, reset = true) }

    // Infinite scroll: saat mendekati akhir, minta lebih banyak
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - 4 && state.canLoadMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingMore) viewModel.loadMore()
    }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        ComicListTopBar(title, onBack)
        Box(Modifier.fillMaxSize()) {
            MediaStateView(
                isLoading = state.isLoading && state.results.isEmpty(),
                isEmpty = state.results.isEmpty(),
                error = state.error,
                onRetry = { viewModel.loadList(tagId = tagId, reset = true) },
                accent = MediaColors.Comic,
                emptyTitle = "Tidak ada komik",
                emptySubtitle = "Coba muat ulang atau pilih genre lain."
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(MediaDimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.results) { item -> ComicGridItem(item) { onComicClick(it) } }
                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MediaColors.Comic, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }

            // Scroll to top
            if (gridState.firstVisibleItemIndex > 6) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MediaColors.Comic)
                        .clickable { scope.launch { gridState.animateScrollToItem(0) } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "Ke atas", tint = androidx.compose.ui.graphics.Color.Black)
                }
            }
        }
    }
}

@Composable
private fun ComicListTopBar(title: String, onBack: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().background(MediaColors.Bg).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MediaColors.TextPrimary)
        }
        Spacer(Modifier.size(4.dp))
        Text(title, fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
            color = MediaColors.TextPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
    }
}