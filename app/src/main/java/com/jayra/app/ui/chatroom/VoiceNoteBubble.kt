package com.jayra.app.ui.chatroom

import android.media.MediaPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.model.VoiceTrimmerStrings
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraBubbleInDark
import com.jayra.app.ui.theme.JayraBubbleInLight
import com.jayra.app.ui.theme.JayraBubbleOutDark
import com.jayra.app.ui.theme.JayraBubbleOutLight
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.util.AudioWaveTrimmer
import java.io.File
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun VoiceNoteBubble(
    messageId: String,
    durationMillis: Long,
    waveform: List<Float>,
    filePath: String? = null,
    strings: VoiceTrimmerStrings = VoiceTrimmerStrings(),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val safeDuration = durationMillis.coerceAtLeast(1_000L)
    val bars = remember(waveform, messageId) {
        if (waveform.isNotEmpty()) {
            waveform.take(32)
        } else {
            List(28) { idx ->
                (0.22f + 0.72f * abs(sin((idx + 1) * 0.49))).toFloat().coerceIn(0.18f, 1.0f)
            }
        }
    }

    var isPlaying by remember(messageId) { mutableStateOf(false) }
    var progressFraction by remember(messageId) { mutableFloatStateOf(0f) }
    var elapsedMillis by remember(messageId) { mutableLongStateOf(0L) }
    var mediaPlayerRef by remember(messageId) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(messageId) {
        onDispose {
            try {
                mediaPlayerRef?.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayerRef?.release()
            } catch (_: Exception) {
            }
            mediaPlayerRef = null
        }
    }

    LaunchedEffect(isPlaying, filePath, safeDuration) {
        if (!isPlaying) return@LaunchedEffect

        val player = mediaPlayerRef
        if (player != null) {
            while (isPlaying) {
                val active = try {
                    player.isPlaying
                } catch (_: Exception) {
                    false
                }
                if (!active) {
                    isPlaying = false
                    progressFraction = 0f
                    elapsedMillis = 0L
                    break
                }
                val pos = try {
                    player.currentPosition.toLong()
                } catch (_: Exception) {
                    0L
                }
                val dur = try {
                    player.duration.toLong().coerceAtLeast(safeDuration)
                } catch (_: Exception) {
                    safeDuration
                }
                elapsedMillis = pos.coerceAtMost(dur)
                progressFraction = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                delay(50L)
            }
        } else {
            val startOffset = (progressFraction * safeDuration).toLong()
            var currentMs = startOffset
            while (isPlaying && currentMs < safeDuration) {
                delay(50L)
                currentMs = (currentMs + 50L).coerceAtMost(safeDuration)
                elapsedMillis = currentMs
                progressFraction = (currentMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            }
            if (currentMs >= safeDuration) {
                isPlaying = false
                progressFraction = 0f
                elapsedMillis = 0L
            }
        }
    }

    val buttonBg by animateColorAsState(
        targetValue = if (isPlaying) JayraBlue.copy(alpha = 0.88f) else JayraBlue,
        animationSpec = tween(durationMillis = 200),
        label = "voice_play_btn_bg"
    )

    val unplayedBarColor = if (isDark) {
        Color(0xFFFFFFFF).copy(alpha = 0.24f)
    } else {
        JayraSubtext.copy(alpha = 0.36f)
    }

    Row(
        modifier = modifier
            .widthIn(min = 210.dp, max = 272.dp)
            .padding(vertical = 4.dp)
            .testTag("voice_note_bubble_$messageId"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable {
                    if (isPlaying) {
                        try {
                            mediaPlayerRef?.pause()
                        } catch (_: Exception) {
                        }
                        isPlaying = false
                    } else {
                        val localFile = filePath?.let { File(it) }
                        if (localFile != null && localFile.exists() && localFile.length() > 44L) {
                            try {
                                mediaPlayerRef?.release()
                                val mp = MediaPlayer().apply {
                                    setDataSource(localFile.absolutePath)
                                    prepare()
                                    setOnCompletionListener {
                                        isPlaying = false
                                        progressFraction = 0f
                                        elapsedMillis = 0L
                                    }
                                    start()
                                }
                                mediaPlayerRef = mp
                            } catch (_: Exception) {
                                mediaPlayerRef = null
                            }
                        }
                        isPlaying = true
                    }
                }
                .padding(3.dp)
                .testTag("voice_note_play_button_$messageId"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(buttonBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) strings.previewPauseLabel else strings.previewPlayLabel,
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val count = bars.size.coerceAtLeast(1)
                val gapPx = 2.5.dp.toPx()
                val totalGap = gapPx * (count - 1)
                val barWidth = ((size.width - totalGap) / count).coerceAtLeast(2.dp.toPx())
                val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                bars.forEachIndexed { index, amplitude ->
                    val fraction = (index + 0.5f) / count.toFloat()
                    val isPlayed = fraction <= progressFraction
                    val barHeight = (size.height * amplitude.coerceIn(0.16f, 1.0f))
                        .coerceAtLeast(4.dp.toPx())
                    val topY = (size.height - barHeight) / 2f
                    val leftX = index * (barWidth + gapPx)

                    drawRoundRect(
                        color = if (isPlayed) JayraBlue else unplayedBarColor,
                        topLeft = Offset(leftX, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shownMillis = if (isPlaying || elapsedMillis > 0L) elapsedMillis else safeDuration
                Text(
                    text = AudioWaveTrimmer.formatCompactDurationMillis(shownMillis),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = JayraBlue
                )
                Text(
                    text = strings.voiceBubbleLabel,
                    fontSize = 11.sp,
                    color = JayraSubtext
                )
            }
        }
    }
}

@Preview(name = "Voice Note Bubble Light", showBackground = true)
@Composable
private fun VoiceNoteBubbleLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface(color = JayraLightChatBg) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JayraBubbleOutLight)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                VoiceNoteBubble(
                    messageId = "vn_light_1",
                    durationMillis = 6_400L,
                    waveform = listOf(
                        0.25f, 0.42f, 0.78f, 0.92f, 0.55f, 0.34f, 0.68f,
                        0.85f, 0.96f, 0.60f, 0.30f, 0.48f, 0.74f, 0.52f,
                        0.38f, 0.66f, 0.88f, 0.44f, 0.28f, 0.62f, 0.79f, 0.36f
                    )
                )
            }
        }
    }
}

@Preview(name = "Voice Note Bubble Dark", showBackground = true)
@Composable
private fun VoiceNoteBubbleDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface(color = JayraDarkChatBg) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JayraBubbleInDark)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                VoiceNoteBubble(
                    messageId = "vn_dark_1",
                    durationMillis = 4_800L,
                    waveform = listOf(
                        0.30f, 0.56f, 0.84f, 0.45f, 0.67f, 0.91f, 0.73f,
                        0.39f, 0.52f, 0.88f, 0.95f, 0.61f, 0.33f, 0.49f,
                        0.76f, 0.58f, 0.41f, 0.69f, 0.82f, 0.47f, 0.31f, 0.55f
                    )
                )
            }
        }
    }
}
