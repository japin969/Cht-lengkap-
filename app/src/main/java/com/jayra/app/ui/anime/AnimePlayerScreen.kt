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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.MediaItem
import androidx.media3.exoplayer.Player
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
private val NetflixControlBg = Color(0xCC000000)
private val NetflixButtonSecondaryBg = Color(0xFF2A2A2A)
private val NetflixTextSecondary = Color(0xFFB3B3B3)
private val NetflixCardGray = Color(0xFF262626)
private val NetflixDarkGray = Color(0xFF181818)

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
    onNextEpisode: (String) -> Unit,
    onPrevEpisode: (String) -> Unit,
    onMirrorChange: (String) -> Unit,
    onShowComments: () -> Unit,
    onShowReactions: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember { createExoPlayer(context) }
    var playerJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // Player state
    val isControllerVisible by remember { mutableStateOf(true) }
    val isBuffering by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    val playbackSpeed by remember { mutableStateOf(1.0f) }
    val selectedQuality by remember { mutableStateOf("Auto") }
    val selectedAudio by remember { mutableStateOf(0) }
    val selectedSubtitle by remember { mutableStateOf(-1) }
    val showSettings by remember { mutableStateOf(false) }
    val showQuality by remember { mutableStateOf(false) }
    val showSpeed by remember { mutableStateOf(false) }
    val showAudioSubs by remember { mutableStateOf(false) }
    val showMirrorSheet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
        playerJob?.cancel()
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
            playerJob?.cancel()
            hideControlsJob.cancel()
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .clickable { isControllerVisible = !isControllerVisible }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { _, _ -> },
                    onDragEnd = { },
                    onDragCancel = { }
                )
            }
    ) {
        // Player View - Use AndroidView with SurfaceView for ExoPlayer
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    // ExoPlayer will use this SurfaceView via player.setVideoSurfaceView()
                    // For simplicity, we'll use a placeholder here
                    // In production, you'd use ExoPlayer's PlayerView or setVideoSurfaceView
                }
            },
            update = { surfaceView ->
                // Set the surface view for ExoPlayer
                player.setVideoSurfaceView(surfaceView)
            }
        )

        // Error overlay
        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NetflixBlack)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(msg, fontSize = 16.sp, color = NetflixWhite, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { errorMessage = null; episodeData.videoUrl?.let { preparePlayer(player, it) } }) {
                        Text("Coba Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top controls
        if (isControllerVisible) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(NetflixBlack.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, "Back", NetflixWhite, Modifier.size(24.dp).padding(8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(episodeData.title ?: "Episode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
                            Text("Ep ${episodeData.episode}", fontSize = 14.sp, color = NetflixTextSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { showMirrorSheet = true }) { Icon(Icons.Default.Cast, "Mirrors", NetflixWhite, Modifier.size(24.dp).padding(8.dp)) }
                            IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Settings", NetflixWhite, Modifier.size(24.dp).padding(8.dp)) }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, NetflixBlack.copy(alpha = 0.8f))
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(formatTime(currentPosition), fontSize = 12.sp, color = NetflixWhite)
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                                onValueChange = { progress ->
                                    val newPos = (progress * duration.toFloat()).toLong()
                                    player.seekTo(newPos)
                                    currentPosition = newPos
                                },
                                colors = SliderDefaults.colors(thumbColor = NetflixRed, activeTrackColor = NetflixRed, inactiveTrackColor = NetflixProgressTrack)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(formatTime(duration), fontSize = 12.sp, color = NetflixWhite)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Main controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Prev, Rewind, Play/Pause, Forward, Next
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onPrevEpisode(episodeData.prevEpisodeSlug ?: "") }) { Icon(Icons.Default.SkipPrevious, "Previous", NetflixWhite, Modifier.size(32.dp)) }
                                IconButton(onClick = { player.seekTo((currentPosition - 10000).coerceAtLeast(0)) }) { Icon(Icons.Default.FastRewind, "Rewind 10s", NetflixWhite, Modifier.size(32.dp)) }
                                Button(onClick = { if (player.isPlaying) player.pause() else player.playWhenReady = true }, colors = ButtonDefaults.buttonColors(NetflixRed, NetflixWhite), shape = RoundedCornerShape(28.dp)) {
                                    Icon(if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, NetflixWhite, Modifier.size(24.dp).padding(8.dp))
                                }
                                IconButton(onClick = { player.seekTo((currentPosition + 10000).coerceAtMost(duration)) }) { Icon(Icons.Default.FastForward, "Forward 10s", NetflixWhite, Modifier.size(32.dp)) }
                                IconButton(onClick = { onNextEpisode(episodeData.nextEpisodeSlug ?: "") }) { Icon(Icons.Default.SkipNext, "Next", NetflixWhite, Modifier.size(32.dp)) }
                            }

                            // Right: Speed, Quality, Audio, Subs, Fullscreen
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showSpeed = !showSpeed }) { Icon(Icons.Default.Speed, "Speed", NetflixWhite, Modifier.size(24.dp).padding(8.dp)) }
                                IconButton(onClick = { showQuality = !showQuality }) { Text("${selectedQuality}p", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.padding(8.dp).background(NetflixButtonSecondaryBg, RoundedCornerShape(4.dp))) }
                                IconButton(onClick = { showAudioSubs = !showAudioSubs }) { Icon(Icons.Default.Subtitles, "Audio/Subs", NetflixWhite, Modifier.size(24.dp).padding(8.dp)) }
                                IconButton(onClick = { /* Fullscreen */ }) { Icon(Icons.Default.Fullscreen, "Fullscreen", NetflixWhite, Modifier.size(24.dp).padding(8.dp)) }
                            }
                        }
                    }
                }
            }
        }

        // Settings overlay
        if (showSettings) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showSettings = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(NetflixDarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.padding(bottom = 16.dp))
                    Text("Playback Speed", fontSize = 14.sp, color = NetflixTextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            Text(text = "${speed}x", fontSize = 14.sp, fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal, color = if (playbackSpeed == speed) NetflixRed else NetflixWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).background(if (playbackSpeed == speed) NetflixRed.copy(alpha = 0.1f) else NetflixCardGray, RoundedCornerShape(20.dp)).clickable { playbackSpeed = speed; player.playbackParameters = player.playbackParameters.withSpeed(speed) })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Quality", fontSize = 14.sp, color = NetflixTextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Auto", "1080", "720", "480", "360").forEach { q ->
                            Text(text = q, fontSize = 14.sp, fontWeight = if (selectedQuality == q) FontWeight.Bold else FontWeight.Normal, color = if (selectedQuality == q) NetflixRed else NetflixWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).background(if (selectedQuality == q) NetflixRed.copy(alpha = 0.1f) else NetflixCardGray, RoundedCornerShape(20.dp)).clickable { selectedQuality = q })
                        }
                    }
                }
            }
        }

        // Mirror sheet
        if (showMirrorSheet) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showMirrorSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(NetflixDarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pilih Mirror", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
                        IconButton(onClick = { showMirrorSheet = false }) { Icon(Icons.Default.Close, "Close", NetflixWhite, Modifier.size(24.dp)) }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mirrors) { mirror ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).background(NetflixCardGray, RoundedCornerShape(8.dp)).clickable { onMirrorChange(mirror.url) }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(mirror.name, fontSize = 16.sp, color = NetflixWhite, modifier = Modifier.padding(16.dp).weight(1f))
                                Text(mirror.type, fontSize = 12.sp, color = NetflixTextSecondary, modifier = Modifier.padding(end = 16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Speed overlay
        if (showSpeed) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showSpeed = false }) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            Text(text = "${speed}x", fontSize = 14.sp, fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal, color = if (playbackSpeed == speed) NetflixRed else NetflixWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).background(if (playbackSpeed == speed) NetflixRed.copy(alpha = 0.1f) else NetflixCardGray, RoundedCornerShape(20.dp)).clickable { playbackSpeed = speed; player.playbackParameters = player.playbackParameters.withSpeed(speed); showSpeed = false })
                        }
                    }
                }
            }
        }

        // Quality overlay
        if (showQuality) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showQuality = false }) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Auto", "1080", "720", "480", "360").forEach { q ->
                            Text(text = q, fontSize = 14.sp, fontWeight = if (selectedQuality == q) FontWeight.Bold else FontWeight.Normal, color = if (selectedQuality == q) NetflixRed else NetflixWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).background(if (selectedQuality == q) NetflixRed.copy(alpha = 0.1f) else NetflixCardGray, RoundedCornerShape(20.dp)).clickable { selectedQuality = q; showQuality = false })
                        }
                    }
                }
            }
        }

        // Audio/Subs overlay
        if (showAudioSubs) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showAudioSubs = false }) {
                Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)) {
                    Text("Audio", fontSize = 14.sp, color = NetflixTextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Indonesian", "Japanese", "English").forEachIndexed { idx, lang ->
                            Text(text = lang, fontSize = 14.sp, fontWeight = if (selectedAudio == idx) FontWeight.Bold else FontWeight.Normal, color = if (selectedAudio == idx) NetflixRed else NetflixWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).background(if (selectedAudio == idx) NetflixRed.copy(alpha = 0.1f) else NetflixCardGray, RoundedCornerShape(20.dp)).clickable { selectedAudio = idx; showAudioSubs = false })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Subtitles", fontSize = 14.sp, color = NetflixTextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Off", fontSize = 14.sp, fontWeight = if (selectedSubtitle == -1) FontWeight.Bold else FontWeight.Normal, color = if (selectedSubtitle == -1) NetflixRed else NetflixWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).background(if (selectedSubtitle == -1) NetflixRed.copy(alpha = 0.1f) else NetflixCardGray, RoundedCornerShape(20.dp)).clickable { selectedSubtitle = -1; showAudioSubs = false })
                        danmaku.forEach { sub ->
                            Text(text = sub.lang ?: "Unknown", fontSize = 14.sp, color = NetflixWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).background(NetflixCardGray, RoundedCornerShape(20.dp)).clickable { selectedSubtitle = danmaku.indexOf(sub); showAudioSubs = false })
                        }
                    }
                }
            }
        }

        // Next episode card
        if (showNextEpisodeCard) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable { }) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Episode Berikutnya", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
                    Spacer(Modifier.height(8.dp))
                    Text("Memulai dalam ${nextEpisodeCountdown}s", fontSize = 14.sp, color = NetflixTextSecondary)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { showNextEpisodeCard = false }, colors = ButtonDefaults.buttonColors(NetflixButtonSecondaryBg, NetflixWhite), shape = RoundedCornerShape(8.dp)) { Text("Batal", fontSize = 14.sp) }
                        Button(onClick = { episodeData.nextEpisodeSlug?.let { onNextEpisode(it); showNextEpisodeCard = false } }, colors = ButtonDefaults.buttonColors(NetflixRed, NetflixWhite), shape = RoundedCornerShape(8.dp)) { Text("Tonton Sekarang", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

// ============================================================================
// HELPER FUNCTIONS
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

private fun formatTime(millis: Long): String {
    if (millis == Long.MAX_VALUE || millis <= 0) return "--:--"
    val minutes = (millis / 1000 / 60) % 60
    val seconds = (millis / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}