package com.jayra.app.ui.donghua

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
import com.jayra.app.data.DramaItem
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaStateView
import com.jayra.app.ui.media.SectionHeader
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * DonghuaHome — beda dari AnimeHome: hero banner donghua + badge 3D,
 * section "Episode Terbaru", "Populer", "Rekomendasi". Warna accent pink.
 */
@Composable
fun DonghuaHomeScreen(
    onDonghuaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSeeAll: () -> Unit,
    viewModel: DonghuaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Strem",
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
                Icon(Icons.Rounded.Search, "Cari", tint = MediaColors.Strem, modifier = Modifier.width(22.dp))
            }
        }

        MediaStateView(
            isLoading = state.isLoading && state.featured.isEmpty(),
            isEmpty = state.featured.isEmpty() && state.popular.isEmpty() && state.latest.isEmpty(),
            error = state.error,
            onRetry = { viewModel.retryHome() },
            accent = MediaColors.Strem,
            emptyTitle = "Belum ada donghua",
            emptySubtitle = "Daftar donghua sedang kosong. Coba muat ulang."
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                val hero = state.featured.firstOrNull()
                if (hero != null) item { DonghuaHeroBanner(hero) { onDonghuaClick(it) } }

                if (state.latest.isNotEmpty()) {
                    item { SectionHeader("Episode Terbaru", MediaColors.Strem, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)) {
                            items(state.latest) { DonghuaPosterCard(it) { onDonghuaClick(it) } }
                        }
                    }
                }
                if (state.popular.isNotEmpty()) {
                    item { SectionHeader("Populer", MediaColors.Strem, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)) {
                            items(state.popular) { DonghuaPosterCard(it) { onDonghuaClick(it) } }
                        }
                    }
                }
                if (state.featured.size > 1) {
                    item { SectionHeader("Rekomendasi", MediaColors.Strem, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)) {
                            items(state.featured.drop(1)) { DonghuaPosterCard(it) { onDonghuaClick(it) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonghuaHeroBanner(item: DramaItem, onClick: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = MediaDimens.ScreenPadding).height(230.dp)
            .clip(RoundedCornerShape(16.dp)).clickable { onClick(item.url ?: "") }
    ) {
        AsyncImage(model = item.cover, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)))))
        // Badge 3D
        Box(Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(6.dp)).background(MediaColors.Strem).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text("3D", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(item.title ?: "Donghua Pilihan", fontFamily = SFProFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text("Tonton sekarang", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MediaColors.Strem)
        }
    }
}