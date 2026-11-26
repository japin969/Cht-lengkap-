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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.DramaEpisode
import com.jayra.app.data.MediaEntry
import com.jayra.app.data.MediaStore
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaStateView
import com.jayra.app.ui.theme.SFProFontFamily
import kotlinx.coroutines.launch

/**
 * DonghuaDetail — beda dari AnimeDetail: badge 3D/2D, "Episode Terbaru",
 * info donghua. Tab swipe: "Episode" / "Info".
 */
@Composable
fun DonghuaDetailScreen(
    dramaUrl: String,
    onBack: () -> Unit,
    onPlayEpisode: (String) -> Unit,
    viewModel: DonghuaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var bookmarked by remember { mutableStateOf(MediaStore.isBookmarked(context, "strem", dramaUrl)) }

    LaunchedEffect(dramaUrl) { viewModel.loadDetail(dramaUrl) }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        DetailTopBar(onBack, bookmarked) {
            state.detail?.let { d ->
                MediaStore.toggleBookmark(context, "strem", MediaEntry(id = dramaUrl, title = d.title ?: "Donghua", cover = d.cover ?: ""))
                bookmarked = MediaStore.isBookmarked(context, "strem", dramaUrl)
            }
        }

        MediaStateView(
            isLoading = state.isLoading && state.detail == null,
            isEmpty = state.detail == null,
            error = state.error,
            onRetry = { viewModel.retryDetail(dramaUrl) },
            accent = MediaColors.Strem,
            emptyTitle = "Donghua tidak ditemukan",
            emptySubtitle = "Coba buka donghua lain."
        ) {
            val detail = state.detail ?: return@MediaStateView
            val pagerState = rememberPagerState(pageCount = { 2 })
            val scope = rememberCoroutineScope()

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Box(Modifier.fillMaxWidth().height(260.dp)) {
                        AsyncImage(model = detail.cover, contentDescription = detail.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MediaColors.Bg))))
                        Box(Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(6.dp)).background(MediaColors.Strem).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("3D", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = MediaDimens.ScreenPadding)) {
                        Text(detail.title ?: "Tanpa Judul", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MediaColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Text("${detail.chapterCount ?: state.episodes.size} Episode", fontFamily = SFProFontFamily, fontSize = 13.sp, color = MediaColors.TextSecondary)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DonghuaPlayButton(MediaColors.Strem) { state.episodes.firstOrNull()?.videoUrl?.let(onPlayEpisode) }
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (bookmarked) MediaColors.Strem else MediaColors.SurfaceElevated)
                                    .clickable {
                                        state.detail?.let { d ->
                                            MediaStore.toggleBookmark(context, "strem", MediaEntry(id = dramaUrl, title = d.title ?: "Donghua", cover = d.cover ?: ""))
                                            bookmarked = MediaStore.isBookmarked(context, "strem", dramaUrl)
                                        }
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Simpan", tint = if (bookmarked) Color.Black else MediaColors.TextPrimary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = MediaDimens.ScreenPadding)) {
                        listOf("Episode", "Info").forEachIndexed { index, title ->
                            val selected = pagerState.currentPage == index
                            Column(Modifier.weight(1f).clickable { scope.launch { pagerState.animateScrollToPage(index) } }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(title, fontFamily = SFProFontFamily, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp, color = if (selected) MediaColors.Strem else MediaColors.TextTertiary)
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.width(28.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(if (selected) MediaColors.Strem else Color.Transparent))
                            }
                        }
                    }
                }
                item {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(520.dp)) { page ->
                        when (page) {
                            0 -> LazyColumn(Modifier.fillMaxSize()) {
                                items(state.episodes) { ep -> EpisodeRow(ep) { onPlayEpisode(ep.videoUrl ?: dramaUrl) } }
                                if (state.episodes.isEmpty()) item {
                                    Text("Belum ada episode", fontFamily = SFProFontFamily, color = MediaColors.TextTertiary, modifier = Modifier.padding(16.dp))
                                }
                            }
                            1 -> Column(Modifier.fillMaxSize().padding(MediaDimens.ScreenPadding)) {
                                Text("Synopsis", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MediaColors.TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                Text(detail.intro?.ifBlank { "Belum ada deskripsi." } ?: "Belum ada deskripsi.", fontFamily = SFProFontFamily, fontSize = 14.sp, color = MediaColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit, bookmarked: Boolean, onBookmark: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(MediaColors.Bg).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MediaColors.TextPrimary)
        }
        Text("Detail", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = MediaColors.TextPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBookmark() }, contentAlignment = Alignment.Center) {
            Icon(if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Simpan", tint = if (bookmarked) MediaColors.Strem else MediaColors.TextPrimary)
        }
    }
}

@Composable
private fun DonghuaPlayButton(accent: Color, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(accent).clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("Tonton", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
    }
}

@Composable
private fun EpisodeRow(ep: DramaEpisode, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = MediaDimens.ScreenPadding, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MediaColors.Strem.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = MediaColors.Strem, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ep.name ?: "Episode ${ep.index}", fontFamily = SFProFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MediaColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Episode ${ep.index ?: ""}", fontFamily = SFProFontFamily, fontSize = 12.sp, color = MediaColors.TextTertiary)
        }
    }
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(MediaColors.Border))
}