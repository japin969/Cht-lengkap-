package com.jayra.app.ui.comic

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
import androidx.compose.runtime.LaunchedEffect
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
import com.jayra.app.data.KomikItem
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaStateView
import com.jayra.app.ui.media.SectionHeader
import com.jayra.app.ui.media.ShimmerBox
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * ComicHome — pola aplikasi komik terkenal (Webtoon/Tapas):
 * TopBar "Comic" + search icon, hero banner komik unggulan, lalu section
 * "Terbaru", "Populer", "Rating Tertinggi" berupa baris poster horizontal.
 */
@Composable
fun ComicHomeScreen(
    onComicClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSeeAll: () -> Unit,
    onSeeAllBacakomik: () -> Unit = {},
    onSeeAllMaid: () -> Unit = {},
    onBkClick: (String) -> Unit = {},
    onMaidClick: (String) -> Unit = {},
    viewModel: ComicViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadBacakomik(); viewModel.loadMaid() }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Comic",
                fontFamily = SFProFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MediaColors.TextPrimary,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            Box(
                Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MediaColors.SurfaceElevated)
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Search, "Cari", tint = MediaColors.Comic, modifier = Modifier.width(22.dp))
            }
        }

        MediaStateView(
            isLoading = state.isLoading && state.latest.isEmpty(),
            isEmpty = state.latest.isEmpty() && state.popular.isEmpty() && state.trending.isEmpty(),
            error = state.error,
            onRetry = { viewModel.retryHome() },
            accent = MediaColors.Comic,
            emptyTitle = "Belum ada komik",
            emptySubtitle = "Daftar komik sedang kosong. Coba muat ulang."
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Hero banner (komik pertama dari trending)
                val hero = state.trending.firstOrNull() ?: state.latest.firstOrNull()
                if (hero != null) {
                    item { ComicHeroBanner(hero) { onComicClick(it) } }
                }

                if (state.latest.isNotEmpty()) {
                    item { SectionHeader("Terbaru", MediaColors.Comic, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)
                        ) {
                            items(state.latest) { ComicPosterCard(it) { onComicClick(it) } }
                        }
                    }
                }
                if (state.popular.isNotEmpty()) {
                    item { SectionHeader("Populer", MediaColors.Comic, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)
                        ) {
                            items(state.popular) { ComicPosterCard(it) { onComicClick(it) } }
                        }
                    }
                }
                if (state.trending.isNotEmpty()) {
                    item { SectionHeader("Rating Tertinggi", MediaColors.Comic, onSeeAll = onSeeAll) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)
                        ) {
                            items(state.trending) { ComicPosterCard(it) { onComicClick(it) } }
                        }
                    }
                }

                if (state.bkLatest.isNotEmpty()) {
                    item { SectionHeader("BacaKomik Terbaru", MediaColors.Comic, onSeeAll = onSeeAllBacakomik) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)
                        ) {
                            items(state.bkLatest) { ComicPosterCard(it) { onBkClick(it) } }
                        }
                    }
                }
                if (state.maidLatest.isNotEmpty()) {
                    item { SectionHeader("Maid Collection", MediaColors.Comic, onSeeAll = onSeeAllMaid) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = MediaDimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing)
                        ) {
                            items(state.maidLatest) { ComicPosterCard(it) { onMaidClick(it) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicHeroBanner(item: KomikItem, onClick: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaDimens.ScreenPadding)
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick(item.slug ?: "") }
    ) {
        AsyncImage(
            model = item.cover,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                item.title ?: "Komik Pilihan",
                fontFamily = SFProFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Baca sekarang",
                fontFamily = SFProFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MediaColors.Comic
            )
        }
    }
}