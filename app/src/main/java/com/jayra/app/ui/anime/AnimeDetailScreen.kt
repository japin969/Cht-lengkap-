package com.jayra.app.ui.anime

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Message
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.data.AnimeStreamServer
import com.jayra.app.data.NetworkClient
import com.jayra.app.ui.theme.IOSBlue
import com.jayra.app.ui.theme.IOSBlueDark
import com.jayra.app.ui.theme.IOSGray6
import com.jayra.app.ui.theme.IOSGray6Dark
import com.jayra.app.ui.theme.IOSLabel
import com.jayra.app.ui.theme.IOSLabelDark
import com.jayra.app.ui.theme.IOSOrange
import com.jayra.app.ui.theme.IOSRed
import com.jayra.app.ui.theme.IOSSecondaryLabel
import com.jayra.app.ui.theme.IOSSecondaryLabelDark
import com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark
import com.jayra.app.ui.theme.IOSSecondarySystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackgroundDark
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.ui.theme.SquircleShape
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AnimeDetailScreen(
    animeUrl: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val cardBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subLabelColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue
    val borderColor = if (isDark) LiquidGlassSpecV2.DarkBorderTop else Color(0x14000000)

    var activeEpisode by remember { mutableStateOf<AnimeEpisode?>(null) }
    var sortAscending by remember { mutableStateOf(false) }
    var expandSynopsis by remember { mutableStateOf(false) }

    LaunchedEffect(animeUrl) {
        viewModel.loadDetail(animeUrl)
    }

    if (activeEpisode != null) {
        BackHandler {
            activeEpisode = null
            viewModel.clearStream()
        }
        AnimeNativeEpisodePlayer(
            episodeTitle = activeEpisode?.title ?: "Episode ${activeEpisode?.episode.orEmpty()}",
            animeTitle = state.detail?.title.orEmpty(),
            episodes = state.episodes,
            activeEpisodeUrl = activeEpisode?.episodeUrl.orEmpty(),
            isLoading = state.isStreamLoading,
            error = state.streamError,
            streamUrl = state.streamUrl,
            servers = state.streamServers,
            onSelectServer = viewModel::selectStreamServer,
            onSelectEpisode = { ep ->
                ep.episodeUrl?.let { url ->
                    activeEpisode = ep
                    viewModel.loadStream(url)
                }
            },
            onRetry = { activeEpisode?.episodeUrl?.let { viewModel.loadStream(it) } },
            onBack = {
                activeEpisode = null
                viewModel.clearStream()
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when {
            state.isDetailLoading -> CircularProgressIndicator(
                color = accentBlue,
                modifier = Modifier.align(Alignment.Center)
            )
            state.detailError != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = IOSRed, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(state.detailError ?: "Gagal memuat detail", color = labelColor, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.loadDetail(animeUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            else -> {
                val detail = state.detail
                val orderedEpisodes = remember(state.episodes, sortAscending) {
                    if (sortAscending) state.episodes.reversed() else state.episodes
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(squircle(18))
                                .background(cardBg)
                                .border(0.75.dp, borderColor, squircle(18))
                                .padding(14.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(detail?.image).crossfade(true).build(),
                                contentDescription = detail?.title,
                                modifier = Modifier
                                    .width(116.dp)
                                    .height(168.dp)
                                    .clip(squircle(12))
                                    .background(if (isDark) IOSGray6Dark else IOSGray6),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail?.title ?: "Memuat...",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = labelColor,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    detail?.rating?.let { r ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(IOSOrange.copy(alpha = 0.16f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Rounded.Star, contentDescription = null, tint = IOSOrange, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(String.format(Locale.US, "%.2f", r), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IOSOrange)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accentBlue.copy(alpha = 0.14f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${detail?.displayEpisodeCount ?: state.episodes.size} Episode",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentBlue
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                listOfNotNull(
                                    detail?.info?.get("Status")?.let { "Status: $it" },
                                    detail?.info?.get("Studio")?.takeIf { it != "Unknown" }?.let { "Studio: $it" },
                                    detail?.info?.get("Durasi")?.takeIf { it != "Unknown" }?.let { "Durasi: $it" }
                                ).forEach { line ->
                                    Text(line, fontSize = 12.sp, color = subLabelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    val genres = detail?.genres.orEmpty()
                    if (genres.isNotEmpty()) {
                        item(key = "genres") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(genres, key = { it }) { g ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(cardBg)
                                            .border(0.75.dp, borderColor, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(g, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = labelColor)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "synopsis") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(squircle(16))
                                .background(cardBg)
                                .border(0.75.dp, borderColor, squircle(16))
                                .clickable { expandSynopsis = !expandSynopsis }
                                .padding(14.dp)
                        ) {
                            Text("Sinopsis", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = labelColor)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = detail?.displaySynopsis ?: "Memuat sinopsis...",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = subLabelColor,
                                maxLines = if (expandSynopsis) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (expandSynopsis) "Tampilkan lebih sedikit" else "Baca selengkapnya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentBlue
                            )
                        }
                    }

                    item(key = "ep_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daftar Episode (${state.episodes.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = labelColor)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cardBg)
                                    .border(0.75.dp, borderColor, RoundedCornerShape(10.dp))
                                    .clickable { sortAscending = !sortAscending }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.SwapVert, contentDescription = "Urutkan", tint = accentBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (sortAscending) "Ep 1 Dulu" else "Terbaru", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accentBlue)
                            }
                        }
                    }

                    if (orderedEpisodes.isEmpty()) {
                        item(key = "ep_empty") {
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(squircle(14)).background(cardBg).padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada daftar episode", color = subLabelColor, fontSize = 14.sp)
                            }
                        }
                    } else {
                        itemsIndexed(orderedEpisodes, key = { idx, ep -> "ep_${idx}_${ep.episodeUrl}" }) { _, ep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(squircle(14))
                                    .background(cardBg)
                                    .border(0.75.dp, borderColor, squircle(14))
                                    .clickable {
                                        ep.episodeUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                            activeEpisode = ep
                                            viewModel.loadStream(url)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(accentBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar", tint = accentBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title?.takeIf { it.isNotBlank() } ?: "Episode ${ep.episode.orEmpty()}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = labelColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    ep.date?.takeIf { it.isNotBlank() }?.let { d ->
                                        Text(d, fontSize = 12.sp, color = subLabelColor)
                                    }
                                }
                                Text("Tonton", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accentBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeNativeEpisodePlayer(
    episodeTitle: String,
    animeTitle: String,
    episodes: List<AnimeEpisode>,
    activeEpisodeUrl: String,
    isLoading: Boolean,
    error: String?,
    streamUrl: String?,
    servers: List<AnimeStreamServer>,
    onSelectServer: (String) -> Unit,
    onSelectEpisode: (AnimeEpisode) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }

    DisposableEffect(isFullscreen) {
        val activity = context.findActivity()
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .then(if (isFullscreen) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())
    ) {
        if (!isFullscreen) {
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(episodeTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (animeTitle.isNotBlank()) {
                        Text(animeTitle, fontSize = 12.sp, color = IOSSecondaryLabelDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Box(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize().background(Color.Black)
            } else {
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
            },
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = IOSBlueDark)
                error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(error, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = IOSBlueDark)) {
                        Text("Muat Ulang", color = Color.White)
                    }
                }
                !streamUrl.isNullOrBlank() -> {
                    if (isDirectMp4Stream(streamUrl)) {
                        JayraCustomExoPlayer(
                            videoUrl = streamUrl,
                            isFullscreen = isFullscreen,
                            onToggleFullscreen = { isFullscreen = !isFullscreen }
                        )
                    } else {
                        AnimePlayerScreen(videoUrl = streamUrl)
                    }
                }
            }
        }

        if (!isFullscreen) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (servers.isNotEmpty()) {
                    item(key = "quality_picker") {
                        Text("Kualitas & Stream", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(servers, key = { idx, srv -> "srv_${idx}_${srv.url}" }) { _, srv ->
                                val active = srv.url == streamUrl
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) IOSBlueDark else Color(0xFF1C1C1E))
                                        .border(0.75.dp, if (active) IOSBlueDark else Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                        .clickable { onSelectServer(srv.url) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    val label = if (!srv.quality.isNullOrBlank()) "${srv.name} • ${srv.quality}" else srv.name
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                if (episodes.isNotEmpty()) {
                    item(key = "quick_eps") {
                        Text("Episode Lainnya", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(episodes, key = { idx, ep -> "qep_${idx}_${ep.episodeUrl}" }) { _, ep ->
                                val active = ep.episodeUrl == activeEpisodeUrl
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) IOSBlueDark else Color(0xFF1C1C1E))
                                        .border(0.75.dp, if (active) IOSBlueDark else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                        .clickable { onSelectEpisode(ep) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = ep.title ?: "Ep ${ep.episode.orEmpty()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun JayraCustomExoPlayer(
    videoUrl: String,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var savedPosition by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(NetworkClient.BROWSER_UA)
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.blogger.com/"))
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
    }

    LaunchedEffect(videoUrl) {
        if (exoPlayer.currentPosition > 0L) {
            savedPosition = exoPlayer.currentPosition
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        if (savedPosition > 0L) {
            exoPlayer.seekTo(savedPosition)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY && exoPlayer.duration > 0L) {
                    totalDuration = exoPlayer.duration
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (true) {
            if (!isSeeking) {
                currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0L) totalDuration = exoPlayer.duration
            }
            delay(400L)
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500L)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            CircularProgressIndicator(
                color = IOSBlueDark,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x66000000), Color(0x33000000), Color(0xAA000000))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x55000000))
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    if (!isBuffering) {
                        IconButton(
                            onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(IOSBlueDark.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(60.dp))
                    }

                    IconButton(
                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x55000000))
                    ) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val safeDuration = totalDuration.coerceAtLeast(1L)
                    val shownPos = if (isSeeking) seekValue.toLong() else currentPos
                    Slider(
                        value = shownPos.toFloat().coerceIn(0f, safeDuration.toFloat()),
                        onValueChange = {
                            isSeeking = true
                            seekValue = it
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            exoPlayer.seekTo(seekValue.toLong())
                            currentPos = seekValue.toLong()
                        },
                        valueRange = 0f..safeDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = IOSBlueDark,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(26.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatVideoTime(shownPos)} / ${formatVideoTime(totalDuration)}",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = onToggleFullscreen, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = "Layar Penuh",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isDirectMp4Stream(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("googlevideo.com/videoplayback") ||
        lower.contains(".mp4") ||
        lower.contains(".m3u8") ||
        lower.contains(".mkv")
}

private fun formatVideoTime(millis: Long): String {
    val totalSec = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    else String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AnimePlayerScreen(videoUrl: String) {
    if (isDirectMp4Stream(videoUrl)) {
        var fs by remember { mutableStateOf(false) }
        JayraCustomExoPlayer(videoUrl = videoUrl, isFullscreen = fs, onToggleFullscreen = { fs = !fs })
        return
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(videoUrl) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
            webViewRef = null
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.userAgentString = NetworkClient.BROWSER_UA
                settings.setSupportMultipleWindows(false)
                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(v: WebView?, d: Boolean, u: Boolean, r: Message?): Boolean = false
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                        val u = r?.url?.toString()?.lowercase().orEmpty()
                        return listOf("ads", "doubleclick", "popads", "bet", "casino").any { u.contains(it) }
                    }
                }
                loadUrl(videoUrl)
                webViewRef = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
