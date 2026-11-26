package com.jayra.app.ui.video

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jayra.app.data.VideoPlayerManager
import com.jayra.app.ui.theme.JayraBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * NativeVideoPlayer - Compose wrapper around ExoPlayer (via VideoPlayerManager).
 *
 * Features:
 * 1. Uses singleton VideoPlayerManager (no new ExoPlayer per instance)
 * 2. Custom controller overlay (Compose-based, no PlayerView default controller)
 * 3. Hardware accelerated playback
 * 4. Proper lifecycle handling (pause on background, release on dispose)
 * 5. Error handling with retry
 * 6. Fullscreen support
 * 7. Prefetch support
 *
 * Usage:
 * NativeVideoPlayer(
 *     videoUrl = "https://example.com/video.m3u8",
 *     headers = mapOf("Referer" to "https://example.com/"),
 *     title = "Episode 1",
 *     onError = { message -> /* show error */ },
 *     onCompletion = { /* next episode */ }
 * )
 */
@Composable
fun NativeVideoPlayer(
    videoUrl: String,
    headers: Map<String, String> = emptyMap(),
    title: String? = null,
    autoPlay: Boolean = true,
    onError: ((String) -> Unit)? = null,
    onCompletion: (() -> Unit)? = null,
    onPrepared: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerManager = VideoPlayerManager
    val exoPlayer = remember { playerManager.player() }

    var isPrepared by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(false) }

    // Prepare player when videoUrl changes
    LaunchedEffect(videoUrl) {
        isError = false
        isPrepared = false
        isBuffering = true

        try {
            playerManager.prepare(videoUrl, headers)
            playerManager.play()
            if (autoPlay) {
                playerManager.play()
            }
            isPrepared = true
            isBuffering = false
            onPrepared?.invoke()
        } catch (e: Exception) {
            isError = true
            errorMessage = e.message ?: "Failed to prepare video"
            isBuffering = false
            onError?.invoke(errorMessage)
        }
    }

    // Observe player state
    LaunchedEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // UI updates via state observation
            }

            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                isError = true
                errorMessage = error.message ?: "Playback error"
                isBuffering = false
                onError?.invoke(errorMessage)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Handle play/pause
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // New media item
            }
        }

        exoPlayer.addListener(listener)
        DisposableEffect(Unit) {
            onDispose { exoPlayer.removeListener(listener) }
        }
    }

    // Update position/duration periodically
    LaunchedEffect(exoPlayer.playbackState) {
        while (true) {
            delay(500)
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                onCompletion?.invoke()
            }
        }
    }

    // Lifecycle handling
    val lifecycleOwner = LocalContext.current as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (exoPlayer.playWhenReady) {
                        exoPlayer.playWhenReady = false
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    // Keep player for reuse, just pause
                }
                Lifecycle.Event.ON_DESTROY -> {
                    // Don't release singleton, just clear media
                    playerManager.clear()
                }
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
    ) {
        // PlayerView (AndroidView)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We use custom Compose controller
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                (view as PlayerView).player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading / Buffering indicator
        if (isBuffering && !isPrepared) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = JayraBlue,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Error overlay
        if (isError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    androidx.compose.material.icons.rounded.ErrorOutlined
                        .icon(androidx.compose.material3.Icon, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))
                    Text(
                        text = "Gagal memutar video",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            isError = false
                            playerManager.prepare(videoUrl, headers)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = JayraBlue)
                    ) {
                        Text("Coba Lagi", color = Color.White)
                    }
                }
            }
        }

        // Custom Controls Overlay
        if (showControls && isPrepared && !isError) {
            VideoControls(
                title = title,
                currentPosition = currentPosition,
                duration = duration,
                isPlaying = exoPlayer.playWhenReady && exoPlayer.playbackState == Player.STATE_READY,
                onPlayPause = { playerManager.play() },
                onPause = { playerManager.pause() },
                onSeek = { pos -> playerManager.seekTo(pos) },
                onRewind10 = { playerManager.seekTo((currentPosition - 10_000).coerceAtLeast(0)) },
                onForward10 = { playerManager.seekTo((currentPosition + 10_000).coerceAtMost(duration)) },
                onFullscreenToggle = { isFullscreen = !isFullscreen },
                onClose = { playerManager.clear() },
                isFullscreen = isFullscreen
            )
        }

        // Tap to toggle controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls }
                    )
                }
        )
    }
}

// ═══════════════════════════════════════════
// VIDEO CONTROLS COMPOSABLE
// ═══════════════════════════════════════════

@Composable
fun VideoControls(
    title: String?,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onClose: () -> Unit,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(bottom = 48.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
        ) {
            // Title
            title?.let { t ->
                Text(
                    text = t,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
            }

            // Progress Bar
            if (duration > 0) {
                androidx.compose.material3.Slider(
                    value = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                    onValueChange = { fraction ->
                        onSeek((fraction * duration).toLong())
                    },
                    onValueChangeFinished = { onPlayPause() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = JayraBlue,
                        activeTrackColor = JayraBlue,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                // Time labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
                    Text(formatTime(duration), color = Color.White, fontSize = 12.sp)
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Close / Back
                IconButton(onClick = onClose) {
                    androidx.compose.material.Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center: Playback controls
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRewind10) {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Mundur 10 detik",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { if (isPlaying) onPause() else onPlayPause() }) {
                        androidx.compose.material.Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" : "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(onClick = onForward10) {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Maju 10 detik",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Right: Fullscreen
                IconButton(onClick = onFullscreenToggle) {
                    androidx.compose.material.Icon(
                        imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        contentDescription = if (isFullscreen) "Keluar fullscreen" : "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// ═══════════════════════════════════════════
// PLAYER VIEW FOR COMPOSE INTEROP
// ═══════════════════════════════════════════

/**
 * Custom PlayerView that works well with Compose.
 * Handles touch events properly for controller overlay.
 */
class ComposePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    init {
        useController = false
        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}