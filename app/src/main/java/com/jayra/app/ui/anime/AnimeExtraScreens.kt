package com.jayra.app.ui.anime

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.AnimeDay
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaStateView
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * AnimeSchedule — jadwal rilis anime (sankavollerei /anime/schedule).
 */
@Composable
fun AnimeScheduleScreen(
    onBack: () -> Unit,
    onAnimeClick: (String) -> Unit,
    viewModel: AnimeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadSankaHome() }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        AnimeExtraTopBar("Jadwal", onBack)
        MediaStateView(
            isLoading = state.isLoading && state.schedule.isEmpty(),
            isEmpty = state.schedule.isEmpty(),
            error = state.error,
            onRetry = { viewModel.loadSankaHome() },
            accent = MediaColors.Anime,
            emptyTitle = "Jadwal kosong",
            emptySubtitle = "Coba muat ulang."
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(MediaDimens.ScreenPadding)) {
                state.schedule.forEach { day: AnimeDay ->
                    item {
                        Text(day.day ?: "", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, color = MediaColors.Anime, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(day.anime_list.orEmpty()) { a -> AnimePosterCard(a) { onAnimeClick(it) } }
                }
            }
        }
    }
}

/**
 * AnimeNekopoi — daftar dari /anime/nekopoi/latest-hentai.
 */
@Composable
fun AnimeNekopoiScreen(
    onBack: () -> Unit,
    onAnimeClick: (String) -> Unit,
    viewModel: AnimeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadNekopoi() }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        AnimeExtraTopBar("Nekopoi", onBack)
        MediaStateView(
            isLoading = state.isLoading && state.nkLatest.isEmpty(),
            isEmpty = state.nkLatest.isEmpty(),
            error = state.error,
            onRetry = { viewModel.loadNekopoi() },
            accent = MediaColors.Anime,
            emptyTitle = "Belum ada konten",
            emptySubtitle = "Coba muat ulang."
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(MediaDimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.nkLatest) { AnimeGridItem(it) { onAnimeClick(it) } }
            }
        }
    }
}

@Composable
private fun AnimeExtraTopBar(title: String, onBack: () -> Unit) {
    Row(
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