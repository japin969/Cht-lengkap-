package com.jayra.app.ui.comic

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.KomikPage
import com.jayra.app.data.MediaEntry
import com.jayra.app.data.MediaStore
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * ComicReader — pola aplikasi komik terkenal (Webtoon vertical + Tapas paged):
 *  - mode vertical (webtoon) & paged (satu halaman)
 *  - tap untuk toggle kontrol
 *  - retry per-gambar jika 1 halaman gagal (yang lain tetap jalan)
 *  - progress tersimpan ke History
 */
enum class ComicReaderMode { DEFAULT, BK, MAID }

@Composable
fun ComicReaderScreen(
    slug: String,
    onBack: () -> Unit,
    viewModel: ComicViewModel = viewModel(),
    mode: ComicReaderMode = ComicReaderMode.DEFAULT
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var readerMode by remember { mutableStateOf(ReaderMode.VERTICAL) }

    LaunchedEffect(slug, mode) {
        when (mode) {
            ComicReaderMode.BK -> viewModel.loadBkChapter(slug)
            ComicReaderMode.MAID -> viewModel.loadMaidChapter(slug)
            ComicReaderMode.DEFAULT -> viewModel.loadStream(slug)
        }
    }

    // Pulihkan brightness saat keluar
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val original = activity?.window?.attributes?.screenBrightness
        onDispose {
            activity?.window?.attributes = activity.window.attributes.apply {
                screenBrightness = original ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.isLoading && state.pages.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MediaColors.Comic)
                }
            }
            state.pages.isEmpty() && state.error != null -> {
                ReaderError(state.error ?: "Gagal memuat", onRetry = { viewModel.retryStream(slug) }, onBack = onBack)
            }
            else -> {
                if (readerMode == ReaderMode.VERTICAL) {
                    VerticalReader(state.pages) { idx ->
                        val total = state.pages.size
                        val progress = if (total > 0) (idx + 1).toFloat() / total else 0f
                        MediaStore.pushHistory(
                            context, "comic",
                            MediaEntry(
                                id = slug,
                                title = state.streamTitle ?: "Komik",
                                progress = progress,
                                progressLabel = "Hal. ${idx + 1}/$total"
                            )
                        )
                    }
                } else {
                    PagedReader(state.pages)
                }
            }
        }

        // Tap area untuk toggle kontrol (di bawah kontrol)
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { showControls = !showControls }) }
        )

        // Kontrol atas (paling atas, menerima tap back/mode)
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            ReaderTopBar(
                title = state.streamTitle ?: "Baca",
                mode = readerMode,
                onToggleMode = { readerMode = if (readerMode == ReaderMode.VERTICAL) ReaderMode.PAGED else ReaderMode.VERTICAL },
                onBack = onBack
            )
        }
    }
}

private enum class ReaderMode { VERTICAL, PAGED }

@Composable
private fun ReaderTopBar(title: String, mode: ReaderMode, onToggleMode: () -> Unit, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
        }
        Text(title, fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White,
            maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        Box(Modifier.size(40.dp).clickable { onToggleMode() }, contentAlignment = Alignment.Center) {
            Icon(if (mode == ReaderMode.VERTICAL) Icons.Default.GridView else Icons.Default.AutoStories, "Mode", tint = Color.White)
        }
    }
}

@Composable
private fun VerticalReader(pages: List<KomikPage>, onProgress: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState.firstVisibleItemIndex, pages.size) { onProgress(listState.firstVisibleItemIndex) }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        items(pages) { page -> ReaderPage(page) }
    }
}

@Composable
private fun PagedReader(pages: List<KomikPage>) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            ReaderPage(pages[index], fill = true)
        }
    }
}

/** Satu halaman dengan retry per-gambar. */
@Composable
private fun ReaderPage(page: KomikPage, fill: Boolean = false) {
    var failed by remember(page.url) { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .then(if (fill) Modifier.fillMaxSize() else Modifier.aspectRatio(0.65f))
            .background(MediaColors.Surface)
    ) {
        if (failed) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text("Halaman gagal dimuat", fontFamily = SFProFontFamily, color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MediaColors.Comic).clickable { failed = false }.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    Text("Retry", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 13.sp)
                }
            }
        } else {
            AsyncImage(
                model = page.url,
                contentDescription = "Halaman ${page.page}",
                contentScale = if (fill) ContentScale.Fit else ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize(),
                onError = { failed = true }
            )
        }
    }
}

@Composable
private fun ReaderError(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Gagal memuat chapter", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(message, fontFamily = SFProFontFamily, fontSize = 13.sp, color = Color(0xFFB0B0B8))
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MediaColors.Comic).clickable { onRetry() }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Coba Lagi", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 14.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(MediaColors.SurfaceHigh).clickable { onBack() }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Kembali", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}