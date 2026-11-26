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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaStateView
import com.jayra.app.ui.media.SectionHeader
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * AnimeHome — pola aplikasi anime terkenal (Crunchyroll/Netflix):
 * TopBar "Anime" + search, hero banner anime unggulan, lalu baris
 * "Sedang Tayang", "Populer", "Selesai".
 */
@Composable
fun AnimeHomeScreen(
    onAnimeClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSeeAll: () -> Unit,
    viewModel: AnimeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Anime",
                fontFamily = SFProFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MediaColors.TextPrimary,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            Box(
                Modifier.width(40.dp).height(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(MediaColors.SurfaceElevated).clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Search, "Cari", tint = MediaColors.Anime, modifier = Modifier.width(22.dp))
            }
        }

        MediaStateView(
            isLoading = state.isLoading && state.popular.isEmpty(),
            isEmpty = state.popular.isEmpty() && state.ongoing.isEmpty() && state.completed.isEmpty(),
            error = state.error,
            onRetry = { viewModel.retryHome() },
            accent = MediaColors.Anime,
            emptyTitle = "Belum ada anime",
            emptySubtitle = "Daftar anime sedang kosong. Coba muat ulang."
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                val hero = state.ongoing.firstOrNull() ?: state.popular.firstOrNull()
                if (hero != null) item { AnimeHeroBanner(hero) { onAnimeClick(it) } }

                if (state.ongoing.isNotEmpty()) {
                    item { SectionHeader("Sedang Tayang", MediaColors.Anime, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)) {
                            items(state.ongoing) { AnimePosterCard(it) { onAnimeClick(it) } }
                        }
                    }
                }
                if (state.popular.isNotEmpty()) {
                    item { SectionHeader("Populer", MediaColors.Anime, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)) {
                            items(state.popular) { AnimePosterCard(it) { onAnimeClick(it) } }
                        }
                    }
                }
                if (state.completed.isNotEmpty()) {
                    item { SectionHeader("Selesai", MediaColors.Anime, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)) {
                            items(state.completed) { AnimePosterCard(it) { onAnimeClick(it) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeHeroBanner(item: AnimeSearchItem, onClick: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = MediaDimens.ScreenPadding).height(230.dp)
            .clip(RoundedCornerShape(16.dp)).clickable { onClick(item.link ?: "") }
    ) {
        AsyncImage(model = item.image, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(item.title ?: "Anime Pilihan", fontFamily = SFProFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text("Tonton sekarang", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MediaColors.Anime)
        }
    }
}