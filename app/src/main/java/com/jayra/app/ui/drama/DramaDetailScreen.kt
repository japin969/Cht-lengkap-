package com.jayra.app.ui.drama

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.jayra.app.data.DramaItem
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.media.MediaGlassColors
import com.jayra.app.ui.media.MediaGlassTheme
import com.jayra.app.ui.media.MediaTheme
import com.jayra.app.ui.media.jellyClick

private val DramaAccent = Color(0xFFFF375F)

/**
 * Detail Drama — iOS 18 Glass, LEBAR & LEGA. Poster + info + daftar episode.
 * Klik episode -> resolve mp4 -> player (in-place). Klik drama apapun masuk
 * sini (id diambil dari item). Back: Play -> Detail -> List (native nav stack).
 */
@Composable
fun DramaDetailScreen(
    dramaUrl: String,
    viewModel: DramaViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var bookmarked by remember(dramaUrl) { mutableStateOf(false) }
    var synopsisExpanded by remember(dramaUrl) { mutableStateOf(false) }

    // Drama terpilih untuk diputar (in-place overlay).
    var playingEp by remember(dramaUrl) { mutableStateOf<String?>(null) }

    LaunchedEffect(dramaUrl) {
        viewModel.loadDetail(dramaUrl)
        bookmarked = MediaLibraryStore.isBookmarked(context, "drama", dramaUrl)
    }

    // Kalau sedang memutar episode -> tampilkan player full screen.
    val playing = playingEp
    if (playing != null) {
        DramaPlayerOverlay(
            viewModel = viewModel,
            dramaId = dramaUrl,
            ep = playing,
            title = state.detail?.title,
            onBack = { playingEp = null }
        )
        return
    }

    MediaGlassTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(MediaGlassColors.BackgroundGradient))
        ) {
            DramaDetailTopBar(
                title = state.detail?.title,
                bookmarked = bookmarked,
                onBack = onBack,
                onToggleBookmark = {
                    bookmarked = MediaLibraryStore.toggleBookmark(
                        context,
                        MediaLibraryStore.Entry(
                            kind = "drama",
                            id = dramaUrl,
                            title = state.detail?.title ?: "Tanpa judul",
                            image = state.detail?.bestCover,
                            subtitle = "Drama"
                        )
                    )
                }
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    state.detailLoading && state.detail == null -> DramaLoading()

                    state.detailError != null && state.detail == null -> DramaError(
                        message = state.detailError,
                        onRetry = { viewModel.retryDetail() }
                    )

                    state.detail == null -> DramaEmpty(onRetry = { viewModel.retryDetail() })

                    else -> DramaDetailContent(
                        detail = state.detail!!,
                        episodes = state.episodes,
                        offline = state.detailOffline,
                        synopsisExpanded = synopsisExpanded,
                        onToggleSynopsis = { synopsisExpanded = !synopsisExpanded },
                        onPlayEpisode = { ep -> playingEp = ep }
                    )
                }
            }
        }
    }
}

@Composable
private fun DramaDetailTopBar(
    title: String?,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPaddingCompat()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", tint = MediaGlassColors.OnSurface)
        }
        Text(
            text = title ?: "Detail Drama",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MediaGlassColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onToggleBookmark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (bookmarked) DramaAccent else MediaGlassColors.OnSurface
            )
        }
    }
}

@Composable
private fun Modifier.statusBarsPaddingCompat(): Modifier =
    this.then(statusBarsPadding())

@Composable
private fun DramaDetailContent(
    detail: DramaItem,
    episodes: List<com.jayra.app.data.DramaEpisode>,
    offline: Boolean,
    synopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    onPlayEpisode: (String) -> Unit
) {
    val synopsis = detail.bestSynopsis
    val synopsisText = synopsis?.takeIf { it.isNotBlank() }
    val firstEp = episodes.firstOrNull()?.epSlug

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // ── Hero ────────────────────────────────────────────────
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MediaGlassColors.Surface)
                        .border(1.dp, MediaGlassColors.GlassBorder, RoundedCornerShape(24.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AsyncImage(
                        model = detail.bestCover,
                        contentDescription = detail.title,
                        modifier = Modifier
                            .width(120.dp)
                            .height(170.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MediaGlassColors.SurfaceVariant),
                        contentScale = ContentScale.Crop,
                        imageLoader = com.jayra.app.ui.media.LocalMediaImageLoader.current
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = detail.title ?: "Tanpa judul",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MediaGlassColors.OnSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DramaChip(detail.epsCount?.let { "$it Episode" } ?: "Drama")
                            DramaChip(if (detail.completed) "Tamat" else "Ongoing")
                        }
                        if (firstEp != null) {
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onPlayEpisode(firstEp) },
                                colors = ButtonDefaults.buttonColors(containerColor = DramaAccent),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mulai Nonton")
                            }
                        }
                    }
                }
            }
        }

        if (offline) {
            item(key = "offline") {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x33FF9500)
                ) {
                    Text(
                        "Sedang offline — menampilkan data lama.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFB340),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // ── Sinopsis ────────────────────────────────────────────
        if (synopsisText != null) {
            item(key = "synopsis") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Sinopsis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediaGlassColors.OnSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = synopsisText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediaGlassColors.OnSurfaceVariant,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (synopsisText.length > 160) {
                        TextButton(onClick = onToggleSynopsis) {
                            Text(
                                if (synopsisExpanded) "Tutup" else "Selengkapnya",
                                style = MaterialTheme.typography.labelMedium,
                                color = DramaAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ── Episode ─────────────────────────────────────────────
        item(key = "ep_header") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daftar Episode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediaGlassColors.OnSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${episodes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MediaGlassColors.OnSurfaceVariant
                )
            }
        }

        if (episodes.isEmpty()) {
            item(key = "ep_empty") {
                Box(
                    Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Episode belum tersedia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediaGlassColors.OnSurfaceVariant
                    )
                }
            }
        } else {
            items(episodes, key = { it.num ?: it.title ?: "" }) { ep ->
                DramaEpisodeRow(
                    ep = ep,
                    onClick = { ep.epSlug?.let { onPlayEpisode(it) } }
                )
            }
        }
    }
}

@Composable
private fun DramaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MediaGlassColors.SurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MediaGlassColors.OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun DramaEpisodeRow(
    ep: com.jayra.app.data.DramaEpisode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = ep.epSlug != null, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(50))
                .background(DramaAccent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = DramaAccent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = ep.label?.takeIf { it.isNotBlank() } ?: ep.num?.let { "Episode $it" } ?: "Episode",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MediaGlassColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (ep.locked == true) {
                Text(
                    "Terkunci",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediaGlassColors.OnSurfaceVariant
                )
            }
        }
    }
}

// ── Player overlay (in-place) ──────────────────────────────────────────

@Composable
private fun DramaPlayerOverlay(
    viewModel: DramaViewModel,
    dramaId: String,
    ep: String,
    title: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(dramaId, ep) { viewModel.loadStream(ep) }

    val exoPlayer = remember(state.streamUrl) {
        val url = state.streamUrl
        if (url.isNullOrBlank()) null
        else runCatching {
            val isHls = url.lowercase().contains(".m3u8")
            val referer = runCatching { java.net.URI(url).let { "${it.scheme}://${it.host}/" } }.getOrNull()
            val dataSourceFactory = DefaultDataSource.Factory(
                context,
                DefaultHttpDataSource.Factory()
                    .setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                    )
                    .setAllowCrossProtocolRedirects(true)
                    .setDefaultRequestProperties(
                        if (referer != null) mapOf("Referer" to referer) else emptyMap()
                    )
            )
            // Decoder fallback + extension renderer: cegah layar hitam saat
            // codec HEVC/AV1 tidak didukung hardware device.
            val renderersFactory = DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setAllowVideoMixedMimeTypeAdaptiveness(true)
                        .setAllowVideoNonSeamlessAdaptiveness(true)
                )
            }
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(50_000, 50_000, 2_500, 5_000)
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                .build()
            ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build().apply {
                    Log.d("JayraStream", "ExoPlayer prepare: $url (hls=$isHls)")
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
        }.getOrNull()
    }
    DisposableEffect(exoPlayer) { onDispose { exoPlayer?.release() } }

    var playError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }

    // Log state player (IDLE/BUFFERING/READY/ENDED) + error asli ExoPlayer.
    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                val name = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "?"
                }
                Log.d("JayraStream", "Player state: $name")
                if (playbackState == Player.STATE_READY) playError = null
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                Log.d("JayraStream", "Video size: ${videoSize.width}x${videoSize.height}")
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playError = error.message ?: "Gagal memutar video"
                Log.e(
                    "JayraStream",
                    "ExoPlayer error ${error.errorCodeName}(${error.errorCode}): ${error.message}",
                    error.cause
                )
            }
        }
        player.addListener(listener)
        try {
            while (true) {
                kotlinx.coroutines.delay(500)
            }
        } finally {
            player.removeListener(listener)
        }
    }

    MediaGlassTheme {
        Column(
            Modifier.fillMaxSize().background(Color.Black)
        ) {
            // Top bar back + judul
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPaddingCompat()
                    .height(52.dp)
                    .padding(horizontal = 8.dp)
                    .background(Color.Black.copy(alpha = 0.6f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", tint = Color.White)
                }
                Text(
                    text = title ?: "Memutar",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
            }

            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.streamLoading && state.streamUrl == null -> {
                        CircularProgressIndicator(color = Color.White)
                    }
                    exoPlayer != null -> {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isBuffering && playError == null) {
                            Box(
                                Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                        if (playError != null) {
                            Box(
                                Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        playError ?: "Gagal memutar video",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.retryStream() }) { Text("Coba Lagi") }
                                }
                            }
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Movie, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                state.streamError ?: "Sumber video belum tersedia",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.retryStream() }) { Text("Coba Lagi") }
                        }
                    }
                }
            }

            // Info episode
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    "Memutar Episode",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (!title.isNullOrBlank()) {
                    Text(
                        title,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── State helpers ─────────────────────────────────────────────────────

@Composable
private fun DramaLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MediaGlassColors.Accent)
    }
}

@Composable
private fun DramaError(message: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Movie, null, tint = MediaGlassColors.OnSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Gagal memuat detail", style = MaterialTheme.typography.titleMedium, color = MediaGlassColors.OnSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message ?: "Koneksi bermasalah.", style = MaterialTheme.typography.bodyMedium, color = MediaGlassColors.OnSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
private fun DramaEmpty(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Movie, null, tint = MediaGlassColors.OnSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Drama tidak ditemukan", style = MaterialTheme.typography.titleMedium, color = MediaGlassColors.OnSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Detail drama tidak tersedia.", style = MaterialTheme.typography.bodyMedium, color = MediaGlassColors.OnSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Coba Lagi") }
    }
}