package com.jayra.app.ui.neko

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.jayra.app.ui.media.NativeVideoPlayer
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * NekoDetailScreen — detail standalone nekopoi: episode list + play native.
 */
@Composable
fun NekoDetailScreen(
    slug: String,
    onBack: () -> Unit,
    viewModel: NekoViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var playing by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(slug) { viewModel.loadDetail(slug) }
    LaunchedEffect(state.streamUrl) { state.streamUrl?.let { playing = it } }

    if (playing != null) {
        NativeVideoPlayer(videoUrl = playing!!, modifier = Modifier.fillMaxSize())
        // back overlay
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.statusBarsPadding().padding(8.dp).size(40.dp).clip(RoundedCornerShape(20.dp))
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)).clickable { playing = null },
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        Row(
            Modifier.fillMaxWidth().background(MediaColors.Bg).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MediaColors.TextPrimary)
            }
            Spacer(Modifier.size(4.dp))
            Text("Nekopoi", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
                color = MediaColors.TextPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        }
        MediaStateView(
            isLoading = state.isLoading,
            isEmpty = state.episodes.isEmpty(),
            error = state.error,
            onRetry = { viewModel.retry() },
            accent = MediaColors.Anime,
            emptyTitle = "Tidak ada episode",
            emptySubtitle = "Coba buka lain."
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(MediaDimens.ScreenPadding)) {
                item {
                    state.detail?.let { d ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            MediaPoster(d.image, d.title, accent = MediaColors.Anime, modifier = Modifier.size(80.dp, 110.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(d.title ?: "Tanpa Judul", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold,
                                fontSize = 18.sp, color = MediaColors.TextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                items(state.episodes) { ep ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MediaColors.SurfaceElevated)
                            .clickable { ep.link?.let { viewModel.loadStream(it) } }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = MediaColors.Anime, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(ep.title ?: "Episode", fontFamily = SFProFontFamily, fontWeight = FontWeight.Medium,
                            fontSize = 14.sp, color = MediaColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}