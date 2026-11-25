package com.jayra.app.ui.anime

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Movie

import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.jayra.app.data.AnimeDetailData
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.MediaLibraryStore

/**
 * Detail Anime — poster besar, judul, sinopsis, genre, rating, status,
 * jumlah episode, dan tombol Play dari episode pertama.
 */
@Composable
fun AnimeDetailScreen(
    animeUrl: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (String) -> Unit,
    onBack: () -> Unit = {},
    onAnimeClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val enrichment by viewModel.enrichment.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var bookmarked by remember(animeUrl) { mutableStateOf(false) }
    var synopsisExpanded by remember(animeUrl) { mutableStateOf(false) }

    LaunchedEffect(animeUrl) {
        viewModel.loadDetail(animeUrl)
        bookmarked = MediaLibraryStore.isBookmarked(context, "anime", animeUrl)
    }

    // Isi enrichment untuk baris "Anime Lainnya".
    LaunchedEffect(state.popular, state.recent) {
        (state.popular + state.recent).take(10).forEach {
            viewModel.ensureEnriched(it.link)
        }
    }

    com.jayra.app.ui.media.MediaGlassTheme {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    com.jayra.app.ui.media.MediaGlassColors.BackgroundGradient
                )
            )
    ) {
        MediaTopBar(
            title = state.detail?.title ?: "Detail Anime",
            onBack = onBack,
            actions = {
                MediaIconButton(
                    icon = if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (bookmarked) "Hapus bookmark" else "Simpan bookmark",
                    onClick = {
                        bookmarked = MediaLibraryStore.toggleBookmark(
                            context,
                            MediaLibraryStore.Entry(
                                kind = "anime",
                                id = animeUrl,
                                title = state.detail?.title ?: "Tanpa judul",
                                image = state.detail?.image,
                                subtitle = "Anime"
                            )
                        )
                    },
                    active = bookmarked
                )
            }
        )

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.detail == null -> AnimeLoading()

                state.detailError != null && state.detail == null -> AnimeErrorState(
                    message = state.detailError,
                    title = "Detail tidak bisa dimuat",
                    onRetry = { viewModel.retryDetail() }
                )

                state.detail == null -> AnimeEmptyState(
                    title = "Anime tidak ditemukan",
                    message = "Detail anime tidak tersedia.",
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.retryDetail() }
                )

                else -> AnimeDetailContent(
                    detail = state.detail!!,
                    episodes = state.episodes,
                    popular = state.popular,
                    enrichment = enrichment,
                    bookmarked = bookmarked,
                    synopsisExpanded = synopsisExpanded,
                    onToggleSynopsis = { synopsisExpanded = !synopsisExpanded },
                    onPlayEpisode = onPlayEpisode,
                    onAnimeClick = onAnimeClick,
                    onRetryEpisodes = { viewModel.retryDetail() },
                    onToggleBookmark = {
                        bookmarked = MediaLibraryStore.toggleBookmark(
                            context,
                            MediaLibraryStore.Entry(
                                kind = "anime",
                                id = animeUrl,
                                title = state.detail?.title ?: "Tanpa judul",
                                image = state.detail?.image,
                                subtitle = "Anime"
                            )
                        )
                    }
                )
            }
        }
    }
    }
}

@Composable
private fun AnimeDetailContent(
    detail: AnimeDetailData,
    episodes: List<AnimeEpisode>,
    popular: List<AnimeSearchItem>,
    enrichment: Map<String, AnimeEnrichment>,
    bookmarked: Boolean,
    synopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    onPlayEpisode: (String) -> Unit,
    onAnimeClick: (String) -> Unit,
    onToggleBookmark: () -> Unit,
    onRetryEpisodes: () -> Unit = {}
) {
    val synopsis = detail.synopsis?.takeIf { it.isNotBlank() }
    val firstEpisode = episodes.firstOrNull()?.episodeUrl

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // ── Hero ───────────────────────────────────────────────
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                AnimePoster(
                    url = detail.image,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(0.dp),
                    icon = Icons.Rounded.Movie
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.15f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AnimePoster(
                        url = detail.image,
                        contentDescription = detail.title,
                        modifier = Modifier
                            .width(112.dp)
                            .height(164.dp),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Spacer(Modifier.height(26.dp))
                        Text(
                            text = detail.title ?: "Tanpa judul",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AnimeRatingChip(detail.rating)
                            AnimeInfoChip(detail.totalEpisodes?.let { "$it eps" } ?: "TV")
                        }
                        // Tombol play episode pertama
                        if (firstEpisode != null) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { onPlayEpisode(firstEpisode) },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Mulai Nonton")
                            }
                        }
                    }
                }
            }
        }

        // ── Genre ──────────────────────────────────────────────
        val genres = detail.genres.orEmpty()
        if (genres.isNotEmpty()) {
            item(key = "genres") {
                // Wrap 3 per baris — tanpa scroll horizontal.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.chunked(3).forEach { rowGenres ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowGenres.forEach { g ->
                                AnimeInfoChip(g)
                            }
                            repeat(3 - rowGenres.size) { Spacer(Modifier.width(0.dp)) }
                        }
                    }
                }
            }
        }

        // ── Sinopsis ───────────────────────────────────────────
        if (!synopsis.isNullOrBlank()) {
            item(key = "synopsis") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Sinopsis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (synopsis.length > 160) {
                        TextButton(onClick = onToggleSynopsis) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (synopsisExpanded) "Tutup" else "Selengkapnya",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (synopsisExpanded)
                                        Icons.Rounded.ExpandLess
                                    else
                                        Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            }
        }

        // ── Episode ────────────────────────────────────────────
        item(key = "ep_header") {
            AnimeSectionHeader(
                title = "Episode",
                subtitle = "${episodes.size} episode",
                actionLabel = if (firstEpisode != null) "Putar" else null,
                onAction = firstEpisode?.let { ep -> { onPlayEpisode(ep) } }
            )
        }

        if (episodes.isEmpty()) {
            item(key = "ep_empty") {
                AnimeEmptyState(
                    title = "Episode belum tersedia",
                    message = "Daftar episode belum bisa dimuat dari semua sumber. Coba lagi.",
                    actionLabel = "Coba Lagi",
                    onAction = onRetryEpisodes,
                    modifier = Modifier.height(220.dp)
                )
            }
        } else {
            items(episodes, key = { it.episodeUrl ?: it.episode ?: it.title ?: "" }) { ep ->
                AnimeEpisodeRow(
                    episode = ep,
                    onClick = { ep.episodeUrl?.let { onPlayEpisode(it) } }
                )
            }
        }

        // ── Anime lainnya ──────────────────────────────────────
        if (popular.isNotEmpty()) {
            item(key = "rec_head") {
                AnimeSectionHeader(
                    title = "Anime Lainnya",
                    subtitle = "Rekomendasi untukmu"
                )
            }
            item(key = "rec_row") {
                AnimeGridRows(
                    items = popular,
                    enrichment = enrichment,
                    onClick = onAnimeClick
                )
            }
        }
    }
}

/** Grid 3 kolom (scroll vertical, tanpa swipe). */
@Composable
private fun AnimeGridRows(
    items: List<AnimeSearchItem>,
    enrichment: Map<String, AnimeEnrichment>,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { anime ->
                    Box(Modifier.weight(1f)) {
                        AnimePosterCard(
                            anime = anime,
                            enrichment = enrichment[anime.link],
                            onClick = { onClick(anime.link ?: "") }
                        )
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Baris episode dengan thumbnail, judul, dan nomor. */
@Composable
fun AnimeEpisodeRow(
    episode: AnimeEpisode,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .height(78.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AnimePoster(
                url = null,
                contentDescription = episode.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(10.dp),
                icon = Icons.Rounded.Movie,
                iconSize = 24.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            episode.episode?.let { num ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = num,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = episode.title?.takeIf { it.isNotBlank() }
                    ?: episode.episode?.let { "Episode $it" }
                    ?: "Episode",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = episode.episode?.let { "Episode $it" } ?: "Putar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Sedang diputar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Baris genre yang bisa di-scroll horizontal. */
// ═══════════════════════════════════════════════════════════════════
// PLAYER — YouTube-style, native ExoPlayer
// ═══════════════════════════════════════════════════════════════════

/**
 * Pemutar video native (ExoPlayer) dengan kontrol modern.
 *
 * PENTING: googlevideo menolak User-Agent selain okhttp, jadi
 * [AnimeViewModel.USER_AGENT] dipasang pada data source.
 *
 * Menampilkan: loading jelas, error + tombol coba lagi, play/pause,
 * seekbar + timestamp, fullscreen, prev/next episode, daftar episode,
 * aksi Like/Share/Save, dan rekomendasi.
 */
@Composable
fun AnimePlayerScreen(
    videoUrl: String,
    title: String? = null,
    subtitle: String? = null,
    onBack: () -> Unit = {},
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    episodes: List<AnimeEpisode> = emptyList(),
    activeEpisodeUrl: String? = null,
    recommendations: List<AnimeSearchItem> = emptyList(),
    enrichment: Map<String, AnimeEnrichment> = emptyMap(),
    onEpisodeClick: (String) -> Unit = {},
    onAnimeClick: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // ── ExoPlayer dengan User-Agent okhttp (wajib untuk googlevideo) ──
    val exoPlayer = remember(videoUrl) {
        if (videoUrl.isBlank()) null
        else runCatching {
            val isHls = videoUrl.lowercase().contains(".m3u8")
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
                context,
                androidx.media3.datasource.DefaultHttpDataSource.Factory()
                    .setUserAgent(AnimeViewModel.USER_AGENT)
                    .setAllowCrossProtocolRedirects(true)
            )
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
                .setUri(videoUrl)
                .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                .build()
            ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build().apply {
                    Log.d("JayraStream", "Anime ExoPlayer prepare: $videoUrl (hls=$isHls)")
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
        }.getOrNull()
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playError by remember { mutableStateOf<String?>(null) }
    var liked by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    // Fullscreen -> sembunyikan system bars (landscape immersive). Aman di preview.
    val view = LocalView.current
    DisposableEffect(isFullscreen) {
        val window = (context as? android.app.Activity)?.window
        val controller = if (window != null) androidx.core.view.WindowCompat.getInsetsController(window, view) else null
        if (controller != null) {
            if (isFullscreen) {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            runCatching { controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars()) }
        }
    }

    // Fullscreen -> landscape (seperti YouTube). Pakai LaunchedEffect (tanpa
    // cleanup race yang bikin balik ke portrait).
    val activity = LocalContext.current as? android.app.Activity
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen)
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // Pantau state player.
    LaunchedEffect(exoPlayer) {
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
                Log.d("JayraStream", "Anime player state: $name")
                when (playbackState) {
                    Player.STATE_READY -> playError = null
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        if (hasNext) onNext()
                    }
                    Player.STATE_IDLE -> {
                        // Instance idle biasanya karena error; biarkan
                        // onPlayerError yang mengisi pesan detailnya.
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                Log.d("JayraStream", "Anime video size: ${videoSize.width}x${videoSize.height}")
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playError = error.message ?: "Gagal memutar video"
                Log.e(
                    "JayraStream",
                    "Anime ExoPlayer error ${error.errorCodeName}(${error.errorCode}): ${error.message}",
                    error.cause
                )
            }
        }
        exoPlayer?.addListener(listener)
        try {
            // Ticker posisi
            while (true) {
                position = exoPlayer?.currentPosition ?: 0L
                duration = (exoPlayer?.duration ?: 0L).coerceAtLeast(0L)
                kotlinx.coroutines.delay(500)
            }
        } finally {
            exoPlayer?.removeListener(listener)
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            kotlinx.coroutines.delay(3500)
            showControls = false
        }
    }

    BackHandler(enabled = true) { onBack() }

    com.jayra.app.ui.media.MediaGlassTheme {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    com.jayra.app.ui.media.MediaGlassColors.BackgroundGradient
                )
            )
    ) {
        // ── Player 16:9 ────────────────────────────────────────
        item(key = "player") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                    .background(Color.Black)
            ) {
                if (videoUrl.isBlank()) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Sumber video belum tersedia",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            view.setOnClickListener { showControls = !showControls }
                        }
                    )
                }

                // Loading
                if (isBuffering && playError == null && videoUrl.isNotBlank()) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Memuat video...",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // Error
                if (playError != null) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = playError ?: "Gagal memutar video",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { /* reload */ }) { Text("Coba Lagi") }
                        }
                    }
                }

                // Kontrol
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControls && playError == null,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.75f),
                                    0.35f to Color.Transparent,
                                    0.65f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.88f)
                                )
                            )
                    ) {
                        // Atas: back + judul + fullscreen
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
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
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = title ?: "Memutar",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { isFullscreen = !isFullscreen },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen)
                                        Icons.Rounded.FullscreenExit
                                    else
                                        Icons.Rounded.Fullscreen,
                                    contentDescription = "Layar penuh",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Tengah: prev / play / next
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            RoundControl(
                                icon = Icons.Rounded.SkipPrevious,
                                enabled = hasPrevious,
                                onClick = onPrevious
                            )
                            Spacer(Modifier.width(26.dp))
                            Box(
                                Modifier
                                    .size(66.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable {
                                        if (isPlaying) exoPlayer?.pause()
                                        else exoPlayer?.play()
                                        showControls = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying)
                                        Icons.Rounded.Pause
                                    else
                                        Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.width(26.dp))
                            RoundControl(
                                icon = Icons.Rounded.SkipNext,
                                enabled = hasNext,
                                onClick = onNext
                            )
                        }

                        // Bawah: seekbar + waktu
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Slider(
                                value = if (duration > 0)
                                    (position.toFloat() / duration).coerceIn(0f, 1f)
                                else 0f,
                                onValueChange = { fraction ->
                                    if (duration > 0) {
                                        exoPlayer?.seekTo((fraction * duration).toLong())
                                        position = (fraction * duration).toLong()
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(position),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = formatTime(duration),
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Judul + aksi ───────────────────────────────────────
        item(key = "meta") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = title ?: "Memutar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill(
                        icon = Icons.Rounded.ThumbUp,
                        label = if (liked) "Disukai" else "Suka",
                        active = liked,
                        onClick = { liked = !liked }
                    )
                    ActionPill(
                        icon = Icons.Rounded.Share,
                        label = "Bagikan",
                        active = false,
                        onClick = {}
                    )
                    ActionPill(
                        icon = if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        label = if (saved) "Tersimpan" else "Simpan",
                        active = saved,
                        onClick = { saved = !saved }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        }

        // ── Daftar episode ─────────────────────────────────────
        item(key = "ep_head") {
            AnimeSectionHeader(
                title = "Episode",
                subtitle = "${episodes.size} episode"
            )
        }

        if (episodes.isEmpty()) {
            item(key = "ep_empty") {
                AnimeEmptyState(
                    title = "Episode belum tersedia",
                    message = "Daftar episode belum bisa dimuat.",
                    modifier = Modifier.height(200.dp)
                )
            }
        } else {
            items(episodes, key = { it.episodeUrl ?: it.episode ?: it.title ?: "" }) { ep ->
                AnimeEpisodeRow(
                    episode = ep,
                    isActive = ep.episodeUrl == activeEpisodeUrl,
                    onClick = { ep.episodeUrl?.let { onEpisodeClick(it) } }
                )
            }
        }

        // ── Anime lainnya ──────────────────────────────────────
        if (recommendations.isNotEmpty()) {
            item(key = "rec_head") {
                AnimeSectionHeader(
                    title = "Anime Lainnya",
                    subtitle = "Rekomendasi untukmu"
                )
            }
            item(key = "rec_row") {
                AnimeGridRows(
                    items = recommendations,
                    enrichment = enrichment,
                    onClick = onAnimeClick
                )
            }
        }
    }
    }
}

@Composable
private fun RoundControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (enabled) 0.16f else 0.06f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f)
        )
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

// ═══════════════════════════════════════════════════════════════════
// TOP BAR bersama
// ═══════════════════════════════════════════════════════════════════

/** App bar sederhana untuk halaman anime (back + judul + action). */
@Composable
fun MediaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            actions()
        }
    }
}

/** Tombol ikon bulat untuk action bar. */
@Composable
fun MediaIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}