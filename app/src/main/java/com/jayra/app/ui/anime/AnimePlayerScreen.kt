package com.jayra.app.ui.anime

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.jayra.app.data.AnimeKompiEpisodeData
import com.jayra.app.data.AnimeKompiMirror
import com.jayra.app.data.AnimeKompiSettingsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// ============================================================================
// COLOR CONSTANTS - NETFLIX PLAYER STYLE
// ============================================================================

private val NetflixBlack = Color(0xFF000000)
private val NetflixWhite = Color(0xFFFFFFFF)
private val NetflixRed = Color(0xFFE50914)
private val NetflixProgressTrack = Color(0xFF4D4D4D)
private val NetflixControlBg = Color(0xCC000000) // 80% black
private val NetflixButtonSecondaryBg = Color(0xFF2A2A2A)
private val NetflixTextSecondary = Color(0xFFB3B3B3)

// ============================================================================
// PLAYER SCREEN
// ============================================================================

@Composable
fun AnimePlayerScreen(
    episodeData: AnimeKompiEpisodeData,
    mirrors: List<AnimeKompiMirror>,
    danmaku: List<com.jayra.app.data.AnimeKompiDanmaku>,
    settings: AnimeKompiSettingsData?,
    viewModel: AnimeViewModel = viewModel(),
    onBackClick: () -> Unit,
    onNextEpisode: (String) -> Unit, // episode slug
    onPrevEpisode: (String) -> Unit,
    onMirrorChange: (String) -> Unit, // mirror URL
    onShowComments: () -> Unit,
    onShowReactions: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember { createExoPlayer(context) }
    val playerJob = remember { Job() }
    val scope = rememberCoroutineScope()

    // Player state
    val isControllerVisible by remember { mutableStateOf(true) }
    val isBuffering by remember { mutableStateOf(false) }
    val currentPosition by remember { mutableStateOf(0L) }
    val duration by remember { mutableStateOf(0L) }
    val playbackSpeed by remember { mutableStateOf(1.0f) }
    val selectedQuality by remember { mutableStateOf("Auto") }
    val selectedAudio by remember { mutableStateOf(0) }
    val selectedSubtitle by remember { mutableStateOf(-1) } // -1 = off
    val showSettings by remember { mutableStateOf(false) }
    val showQuality by remember { mutableStateOf(false) }
    val showSpeed by remember { mutableStateOf(false) }
    val showAudioSubs by remember { mutableStateOf(false) }
    val showMirrorSheet by remember { mutableStateOf(false) }
    val errorMessage by remember { mutableStateOf<String?>(null) }
    val showNextEpisodeCard by remember { mutableStateOf(false) }
    val nextEpisodeCountdown by remember { mutableStateOf(5) }

    // Initialize player
    LaunchedEffect(episodeData.videoUrl) {
        if (episodeData.videoUrl?.isNotBlank() == true) {
            preparePlayer(player, episodeData.videoUrl!!)
        } else {
            errorMessage = "Video URL tidak tersedia"
        }
    }

    // Update position/duration
    LaunchedEffect(player) {
        playerJob.cancel()
        playerJob = scope.launch {
            while (true) {
                currentPosition = player.currentPosition
                duration = player.duration
                delay(200)
            }
        }
    }

    // Auto-hide controls after 3 seconds
    val hideControlsJob = remember { Job() }
    LaunchedEffect(isControllerVisible) {
        if (isControllerVisible) {
            hideControlsJob.cancel()
            hideControlsJob = scope.launch {
                delay(3000)
                if (!showSettings && !showQuality && !showSpeed && !showAudioSubs && !showMirrorSheet) {
                    isControllerVisible = false
                }
            }
        }
    }

    // Cleanup
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            playerJob.cancel()
            hideControlsJob.cancel()
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // EXO PLAYER VIEW
        AndroidPlayerView(
            player = player,
            modifier = Modifier.fillMaxSize(),
            danmaku = danmaku,
            isBuffering = isBuffering
        )

        // ERROR OVERLAY
        if (errorMessage != null) {
            PlayerErrorOverlay(
                message = errorMessage!!,
                mirrors = mirrors,
                onRetry = { preparePlayer(player, episodeData.videoUrl!!) },
                onMirrorClick = { url -> onMirrorChange(url) },
                onBackClick = onBackClick
            )
        }

        // CONTROLS OVERLAY
        if (isControllerVisible || showSettings || showQuality || showSpeed || showAudioSubs || showMirrorSheet) {
            PlayerControlsOverlay(
                isVisible = isControllerVisible,
                onTap = { isControllerVisible = !isControllerVisible },
                onBackClick = onBackClick,
                episodeTitle = "${episodeData.animeTitle ?: ""} - ${episodeData.title ?: "Episode ${episodeData.episode}"}",
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { pos -> player.seekTo(pos) },
                isPlaying = player.playWhenReady,
                onPlayPause = { player.playWhenReady = !player.playWhenReady },
                onSkipPrevious = { onPrevEpisode(episodeData.prevEpisode?.slug ?: "") },
                onSkipNext = { onNextEpisode(episodeData.nextEpisode?.slug ?: "") },
                onRewind10 = { player.seekTo(max(0, player.currentPosition - 10000)) },
                onForward10 = { player.seekTo(min(player.duration, player.currentPosition + 10000)) },
                onSettingsClick = { showSettings = true },
                onCastClick = { /* TODO */ },
                playbackSpeed = playbackSpeed,
                onSpeedChange = { speed ->
                    playbackSpeed = speed
                    player.playbackParameters = player.playbackParameters.withSpeed(speed)
                    showSpeed = false
                },
                onQualityChange = { quality ->
                    selectedQuality = quality
                    // TODO: Implement quality switching via mirrors
                    showQuality = false
                },
                availableQualities = getAvailableQualities(mirrors),
                selectedAudio = selectedAudio,
                onAudioChange = { index ->
                    selectedAudio = index
                    // TODO: Switch audio track
                    showAudioSubs = false
                },
                audioTracks = episodeData.audio?.map { it.label ?: it.lang ?: "Audio" } ?: listOf("Sub"),
                selectedSubtitle = selectedSubtitle,
                onSubtitleChange = { index ->
                    selectedSubtitle = index
                    // TODO: Switch subtitle track
                    showAudioSubs = false
                },
                subtitleTracks = listOf("Off") + (episodeData.subtitles?.map { it.label ?: it.lang ?: "Subtitle" } ?: emptyList()),
                showSettings = showSettings,
                onSettingsDismiss = { showSettings = false },
                showQuality = showQuality,
                onQualityDismiss = { showQuality = false },
                showSpeed = showSpeed,
                onSpeedDismiss = { showSpeed = false },
                showAudioSubs = showAudioSubs,
                onAudioSubsDismiss = { showAudioSubs = false },
                showMirrorSheet = showMirrorSheet,
                onMirrorSheetDismiss = { showMirrorSheet = false },
                mirrors = mirrors,
                onMirrorSelect = { url ->
                    showMirrorSheet = false
                    onMirrorChange(url)
                },
                nextEpisode = episodeData.nextEpisode,
                onNextEpisodeClick = { ep ->
                    showNextEpisodeCard = false
                    onNextEpisode(ep.slug ?: "")
                },
                onNextEpisodeDismiss = { showNextEpisodeCard = false },
                showNextEpisodeCard = showNextEpisodeCard,
                nextEpisodeCountdown = nextEpisodeCountdown,
                skipIntro = episodeData.intro,
                skipOutro = episodeData.outro,
                onSkipIntro = { episodeData.intro?.end?.let { player.seekTo(it.toLong() * 1000) } },
                onSkipOutro = { episodeData.outro?.start?.let { player.seekTo(it.toLong() * 1000) } },
                onCommentsClick = onShowComments,
                onReactionsClick = onShowReactions,
            )
        }

        // NEXT EPISODE CARD (shows 10s before end)
        if (showNextEpisodeCard && episodeData.nextEpisode != null) {
            NextEpisodeCard(
                nextEpisode = episodeData.nextEpisode!!,
                countdown = nextEpisodeCountdown,
                onPlay = { ep -> onNextEpisode(ep.slug ?: "") },
                onDismiss = { showNextEpisodeCard = false }
            )
        }
    }

    // Start countdown for next episode when near end
    LaunchedEffect(currentPosition, duration) {
        if (duration > 0 && episodeData.nextEpisode != null) {
            val remaining = duration - currentPosition
            if (remaining > 0 && remaining <= 10000 && !showNextEpisodeCard) { // 10 seconds before end
                showNextEpisodeCard = true
                // Countdown
                for (i in 5 downTo 1) {
                    delay(1000)
                    nextEpisodeCountdown = i
                }
                // Auto play next
                if (showNextEpisodeCard) {
                    onNextEpisode(episodeData.nextEpisode!!.slug ?: "")
                }
            }
        }
    }
}

// ============================================================================
// EXO PLAYER CREATION
// ============================================================================

private fun createExoPlayer(context: Context): ExoPlayer {
    return ExoPlayer.Builder(context).build()
}

private fun preparePlayer(player: ExoPlayer, videoUrl: String) {
    val mediaItem = MediaItem.fromUri(videoUrl)
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
}

// ============================================================================
// ANDROID PLAYER VIEW (ExoPlayer PlayerView)
// ============================================================================

@Composable
fun AndroidPlayerView(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    danmaku: List<com.jayra.app.data.AnimeKompiDanmaku>,
    isBuffering: Boolean
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                // Disable default controller
                controllerHideOnTouch = false
                controllerShowTimeoutMs = 0
            }
        },
        update = { view ->
            view.player = player
        },
        modifier = modifier
    )
}

// ============================================================================
// PLAYER CONTROLS OVERLAY
// ============================================================================

@Composable
fun PlayerControlsOverlay(
    isVisible: Boolean,
    onTap: () -> Unit,
    onBackClick: () -> Unit,
    episodeTitle: String,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onSettingsClick: () -> Unit,
    onCastClick: () -> Unit,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onQualityChange: (String) -> Unit,
    availableQualities: List<String>,
    selectedAudio: Int,
    onAudioChange: (Int) -> Unit,
    audioTracks: List<String>,
    selectedSubtitle: Int,
    onSubtitleChange: (Int) -> Unit,
    subtitleTracks: List<String>,
    showSettings: Boolean,
    onSettingsDismiss: () -> Unit,
    showQuality: Boolean,
    onQualityDismiss: () -> Unit,
    showSpeed: Boolean,
    onSpeedDismiss: () -> Unit,
    showAudioSubs: Boolean,
    onAudioSubsDismiss: () -> Unit,
    showMirrorSheet: Boolean,
    onMirrorSheetDismiss: () -> Unit,
    mirrors: List<AnimeKompiMirror>,
    onMirrorSelect: (String) -> Unit,
    nextEpisode: com.jayra.app.data.AnimeKompiNextEpisode?,
    onNextEpisodeClick: (com.jayra.app.data.AnimeKompiNextEpisode) -> Unit,
    onNextEpisodeDismiss: () -> Unit,
    showNextEpisodeCard: Boolean,
    nextEpisodeCountdown: Int,
    skipIntro: com.jayra.app.data.AnimeKompiSkipSegment?,
    skipOutro: com.jayra.app.data.AnimeKompiSkipSegment?,
    onSkipIntro: () -> Unit,
    onSkipOutro: () -> Unit,
    onCommentsClick: () -> Unit,
    onReactionsClick: () -> Unit,
) {
    // Main controls container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isVisible) Color.Transparent else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { offset ->
                        // Double tap left/right for seek
                        // TODO: Determine left/right based on offset
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR
            if (isVisible || showSettings || showQuality || showSpeed || showAudioSubs) {
                TopPlayerBar(
                    onBackClick = onBackClick,
                    episodeTitle = episodeTitle,
                    onSettingsClick = onSettingsClick,
                    onCastClick = onCastClick
                )
            }

            // CENTER CONTROLS (show when tapped)
            if (isVisible) {
                CenterPlayerControls(
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause,
                    onRewind10 = onRewind10,
                    onForward10 = onForward10
                )
            }

            // BOTTOM CONTROLS
            if (isVisible || showSettings || showQuality || showSpeed || showAudioSubs) {
                BottomPlayerControls(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = onSeek,
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    onRewind10 = onRewind10,
                    onForward10 = onForward10,
                    playbackSpeed = playbackSpeed,
                    onSpeedChange = onSpeedChange,
                    onQualityChange = onQualityChange,
                    availableQualities = availableQualities,
                    selectedAudio = selectedAudio,
                    onAudioChange = onAudioChange,
                    audioTracks = audioTracks,
                    selectedSubtitle = selectedSubtitle,
                    onSubtitleChange = onSubtitleChange,
                    subtitleTracks = subtitleTracks,
                    showSettings = showSettings,
                    onSettingsDismiss = onSettingsDismiss,
                    showQuality = showQuality,
                    onQualityDismiss = onQualityDismiss,
                    showSpeed = showSpeed,
                    onSpeedDismiss = onSpeedDismiss,
                    showAudioSubs = showAudioSubs,
                    onAudioSubsDismiss = onAudioSubsDismiss,
                    showMirrorSheet = showMirrorSheet,
                    onMirrorSheetDismiss = onMirrorSheetDismiss,
                    mirrors = mirrors,
                    onMirrorSelect = onMirrorSelect,
                    skipIntro = skipIntro,
                    onSkipIntro = onSkipIntro,
                    skipOutro = skipOutro,
                    onSkipOutro = onSkipOutro,
                    onCommentsClick = onCommentsClick,
                    onReactionsClick = onReactionsClick,
                )
            }
        }
    }
}

// ============================================================================
// TOP PLAYER BAR
// ============================================================================

@Composable
fun TopPlayerBar(
    onBackClick: () -> Unit,
    episodeTitle: String,
    onSettingsClick: () -> Unit,
    onCastClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(NetflixControlBg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NetflixWhite, modifier = Modifier.size(24.dp))
        }

        Text(
            text = episodeTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = NetflixWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onCastClick) {
                Icon(Icons.Default.Cast, contentDescription = "Cast", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ============================================================================
// CENTER PLAYER CONTROLS
// ============================================================================

@Composable
fun CenterPlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rewind 10s
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(44.dp)
                .clickable { onRewind10() }
                .background(NetflixControlBg, RoundedCornerShape(22.dp))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = NetflixWhite, modifier = Modifier.size(24.dp))
                Text("10s", fontSize = 10.sp, color = NetflixWhite)
            }
        }

        // Play/Pause
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(64.dp)
                .clickable { onPlayPause() }
                .background(NetflixWhite, RoundedCornerShape(32.dp))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = NetflixBlack,
                modifier = Modifier.size(32.dp).align(Alignment.Center)
            )
        }

        // Forward 10s
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(44.dp)
                .clickable { onForward10() }
                .background(NetflixControlBg, RoundedCornerShape(22.dp))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = NetflixWhite, modifier = Modifier.size(24.dp))
                Text("10s", fontSize = 10.sp, color = NetflixWhite)
            }
        }
    }
}

// ============================================================================
// BOTTOM PLAYER CONTROLS
// ============================================================================

@Composable
fun BottomPlayerControls(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onQualityChange: (String) -> Unit,
    availableQualities: List<String>,
    selectedAudio: Int,
    onAudioChange: (Int) -> Unit,
    audioTracks: List<String>,
    selectedSubtitle: Int,
    onSubtitleChange: (Int) -> Unit,
    subtitleTracks: List<String>,
    showSettings: Boolean,
    onSettingsDismiss: () -> Unit,
    showQuality: Boolean,
    onQualityDismiss: () -> Unit,
    showSpeed: Boolean,
    onSpeedDismiss: () -> Unit,
    showAudioSubs: Boolean,
    onAudioSubsDismiss: () -> Unit,
    showMirrorSheet: Boolean,
    onMirrorSheetDismiss: () -> Unit,
    mirrors: List<AnimeKompiMirror>,
    onMirrorSelect: (String) -> Unit,
    skipIntro: com.jayra.app.data.AnimeKompiSkipSegment?,
    onSkipIntro: () -> Unit,
    skipOutro: com.jayra.app.data.AnimeKompiSkipSegment?,
    onSkipOutro: () -> Unit,
    onCommentsClick: () -> Unit,
    onReactionsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(NetflixControlBg)
    ) {
        // Progress bar
        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .fillMaxWidth()
                .background(NetflixProgressTrack)
                .clip(RoundedCornerShape(2.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { },
                        onDrag = { change, dragAmount ->
                            // Seek on drag
                            val width = change.consumeAllChanges()
                            // TODO: Calculate seek position based on drag
                        },
                        onDragEnd = { }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width({ (this@Box.maxWidth * progress).toInt() })
                    .background(NetflixRed)
                    .clip(RoundedCornerShape(2.dp))
            )
        }

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), fontSize = 12.sp, color = NetflixWhite)
            Text("-${formatTime(max(0, duration - currentPosition))}", fontSize = 12.sp, color = NetflixTextSecondary)
        }

        // Control buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NetflixWhite,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Prev Episode
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Episode", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }

            // Next Episode
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Episode", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }

            // Audio & Subtitles
            IconButton(onClick = { /* showAudioSubs = true */ }) {
                Icon(Icons.Default.Subtitles, contentDescription = "Audio & Subs", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }
            Text("Audio & Subs", fontSize = 12.sp, color = NetflixWhite)

            // Speed
            IconButton(onClick = { /* showSpeed = true */ }) {
                Icon(Icons.Default.Speed, contentDescription = "Speed", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }
            Text("${playbackSpeed}x", fontSize = 12.sp, color = NetflixWhite)

            // Fullscreen
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ============================================================================
// SETTINGS BOTTOM SHEETS (Quality, Speed, Audio/Subs, Mirrors)
// ============================================================================

@Composable
fun QualityBottomSheet(
    qualities: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(NetflixDarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                Text("Video Quality", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.padding(bottom = 16.dp))
                qualities.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onSelect(quality) }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(quality, fontSize = 15.sp, color = NetflixWhite)
                        if (quality == selected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = NetflixRed, modifier = Modifier.size(24.dp))
                        }
                    }
                    androidx.compose.material3.Divider(color = NetflixDivider)
                }
            }
        },
        sheetAction = { onDismiss() }
    )
}

@Composable
fun SpeedBottomSheet(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    ModalBottomSheetLayout(
        sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(NetflixDarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                Text("Playback Speed", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.padding(bottom = 16.dp))
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onSelect(speed) }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${speed}x", fontSize = 15.sp, color = NetflixWhite)
                        if (speed == currentSpeed) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = NetflixRed, modifier = Modifier.size(24.dp))
                        }
                    }
                    androidx.compose.material3.Divider(color = NetflixDivider)
                }
            }
        },
        sheetAction = { onDismiss() }
    )
}

@Composable
fun AudioSubtitlesBottomSheet(
    audioTracks: List<String>,
    selectedAudio: Int,
    onAudioSelect: (Int) -> Unit,
    subtitleTracks: List<String>,
    selectedSubtitle: Int,
    onSubtitleSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(NetflixDarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                // Audio section
                Text("Audio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.padding(bottom = 12.dp))
                audioTracks.forEachIndexed { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onAudioSelect(index) }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(track, fontSize = 15.sp, color = NetflixWhite)
                        if (index == selectedAudio) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = NetflixRed, modifier = Modifier.size(24.dp))
                        }
                    }
                    androidx.compose.material3.Divider(color = NetflixDivider)
                }

                Spacer(Modifier.height(16.dp))

                // Subtitles section
                Text("Subtitles", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.padding(bottom = 12.dp))
                subtitleTracks.forEachIndexed { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onSubtitleSelect(index) }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(track, fontSize = 15.sp, color = NetflixWhite)
                        if (index == selectedSubtitle) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = NetflixRed, modifier = Modifier.size(24.dp))
                        }
                    }
                    androidx.compose.material3.Divider(color = NetflixDivider)
                }
            }
        },
        sheetAction = { onDismiss() }
    )
}

@Composable
fun MirrorBottomSheet(
    mirrors: List<AnimeKompiMirror>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(NetflixDarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                Text("Ganti Mirror", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.padding(bottom = 16.dp))
                mirrors.forEach { mirror ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onSelect(mirror.url ?: "") }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                            Text(mirror.name ?: "Mirror", fontSize = 15.sp, color = NetflixWhite)
                            Text("${mirror.quality ?: ""} • ${mirror.type ?: ""}", fontSize = 12.sp, color = NetflixTextSecondary)
                        }
                    }
                    androidx.compose.material3.Divider(color = NetflixDivider)
                }
            }
        },
        sheetAction = { onDismiss() }
    )
}

// ============================================================================
// NEXT EPISODE CARD
// ============================================================================

@Composable
fun NextEpisodeCard(
    nextEpisode: com.jayra.app.data.AnimeKompiNextEpisode,
    countdown: Int,
    onPlay: (com.jayra.app.data.AnimeKompiNextEpisode) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(320.dp)
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NetflixWhite),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Next Episode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NetflixBlack)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = NetflixBlack, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Thumbnail placeholder
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(68.dp)
                            .background(NetflixCardGray)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(nextEpisode.title ?: "Episode ${nextEpisode.episode}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NetflixBlack, maxLines = 1)
                        Text("S1:E${nextEpisode.episode} • ${nextEpisode.title ?: ""}", fontSize = 11.sp, color = NetflixBlack.copy(alpha = 0.7f), maxLines = 1)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("Auto-play in $countdown", fontSize = 11.sp, color = NetflixBlack.copy(alpha = 0.7f))
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onPlay(nextEpisode) },
                        colors = ButtonDefaults.buttonColors(containerColor = NetflixBlack, contentColor = NetflixWhite),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Play", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// ERROR OVERLAY
// ============================================================================

@Composable
fun PlayerErrorOverlay(
    message: String,
    mirrors: List<AnimeKompiMirror>,
    onRetry: () -> Unit,
    onMirrorClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = NetflixWhite, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Gagal memutar video", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 12.sp, color = NetflixTextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NetflixWhite, contentColor = NetflixBlack), shape = RoundedCornerShape(4.dp)) {
                    Text("Coba Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (mirrors.isNotEmpty()) {
                    Button(onClick = { mirrors.first().url?.let(onMirrorClick) }, colors = ButtonDefaults.buttonColors(containerColor = NetflixButtonSecondaryBg, contentColor = NetflixWhite), shape = RoundedCornerShape(4.dp)) {
                        Text("Ganti Mirror", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = NetflixButtonSecondaryBg, contentColor = NetflixWhite), shape = RoundedCornerShape(4.dp)) {
                    Text("Kembali", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// SKIP INTRO/OUTRO BUTTONS
// ============================================================================

@Composable
fun SkipButtons(
    skipIntro: com.jayra.app.data.AnimeKompiSkipSegment?,
    onSkipIntro: () -> Unit,
    skipOutro: com.jayra.app.data.AnimeKompiSkipSegment?,
    onSkipOutro: () -> Unit,
    currentPosition: Long
) {
    val showSkipIntro = skipIntro != null && currentPosition >= (skipIntro.start ?: 0).toLong() * 1000 && currentPosition <= (skipIntro.end ?: 0).toLong() * 1000
    val showSkipOutro = skipOutro != null && currentPosition >= (skipOutro.start ?: 0).toLong() * 1000

    if (showSkipIntro) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Button(
                onClick = onSkipIntro,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetflixWhite.copy(alpha = 0.9f),
                    contentColor = NetflixBlack
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Skip Intro", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

private fun formatTime(ms: Long): String {
    if (ms < 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${"%02d".format(seconds)}"
}

private fun getAvailableQualities(mirrors: List<AnimeKompiMirror>): List<String> {
    val qualities = mirrors.mapNotNull { it.quality }.distinct().sortedDescending()
    return if (qualities.isNotEmpty()) listOf("Auto") + qualities else listOf("Auto")
}