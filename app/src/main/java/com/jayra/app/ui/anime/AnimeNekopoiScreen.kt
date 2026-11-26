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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaPoster
import com.jayra.app.ui.media.MediaStateView
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * AnimeNekopoiScreen — layar/card Nekopoi sendiri (bukan campur anime).
 * Menampilkan latest + recommended dari NekopoiApi.
 */
@Composable
fun AnimeNekopoiScreen(
    onBack: () -> Unit,
    onNekoClick: (String) -> Unit,
    viewModel: AnimeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadNeko() }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        Row(
            Modifier.fillMaxWidth().background(MediaColors.Bg).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MediaColors.TextPrimary)
            }
            Spacer(Modifier.size(4.dp))
            Text("Nekopoi", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                color = MediaColors.TextPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        }

        MediaStateView(
            isLoading = state.isLoading && state.nekoLatest.isEmpty(),
            isEmpty = state.nekoLatest.isEmpty() && state.nekoRecommended.isEmpty(),
            error = state.error,
            onRetry = { viewModel.loadNeko() },
            accent = MediaColors.Anime,
            emptyTitle = "Belum ada konten",
            emptySubtitle = "Coba muat ulang."
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(MediaDimens.ScreenPadding)) {
                if (state.nekoRecommended.isNotEmpty()) {
                    item {
                        Text("Rekomendasi", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, color = MediaColors.Anime, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(state.nekoRecommended) { NekoCard(it) { onNekoClick(it) } }
                }
                if (state.nekoLatest.isNotEmpty()) {
                    item {
                        Text("Latest", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, color = MediaColors.Anime, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(state.nekoLatest) { NekoCard(it) { onNekoClick(it) } }
                }
            }
        }
    }
}

@Composable
private fun NekoCard(item: AnimeSearchItem, onClick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MediaColors.SurfaceElevated)
            .clickable { onClick(item.link ?: "") }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaPoster(item.image, item.title, accent = MediaColors.Anime, modifier = Modifier.size(64.dp, 88.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title ?: "Tanpa Judul", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = MediaColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("Nekopoi", fontFamily = SFProFontFamily, fontSize = 12.sp, color = MediaColors.Anime)
        }
    }
}