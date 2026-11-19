package com.jayra.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.Song

private val IOSBlue = Color(0xFF007AFF)
private val IOSBlueDark = Color(0xFF0A84FF)
private val GroupedLight = Color(0xFFF2F2F7)
private val GroupedDark = Color(0xFF121212)
private val SecondaryLight = Color(0xFF8A8A8E)
private val SecondaryDark = Color(0xFF98989F)

@Composable
fun MusicPlayerScreen(
    onBack: () -> Unit,
    viewModel: MusicViewModel = viewModel()
) {
    BackHandler { onBack() }

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val isRepeat by viewModel.isRepeat.collectAsStateWithLifecycle()

    MusicPlayerContent(
        song = currentSong,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        isShuffle = isShuffle,
        isRepeat = isRepeat,
        onBack = onBack,
        onPlayPauseClick = viewModel::onPlayPauseClick,
        onSeekTo = viewModel::onSeekTo,
        onNext = viewModel::onNext,
        onPrev = viewModel::onPrev,
        onShuffleToggle = viewModel::onShuffleToggle,
        onRepeatToggle = viewModel::onRepeatToggle
    )
}

@Composable
fun MusicPlayerContent(
    song: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isShuffle: Boolean,
    isRepeat: Boolean,
    onBack: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) GroupedDark else GroupedLight
    val labelColor = if (isDark) Color.White else Color.Black
    val secondary = if (isDark) SecondaryDark else SecondaryLight
    val accent = if (isDark) IOSBlueDark else IOSBlue

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    val effectivePosition = if (isUserSeeking) seekPosition.toLong() else currentPosition
    val effectiveDuration = if (duration > 0L) duration else 1L

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) listOf(Color(0xFF1E2A38), GroupedDark)
                    else listOf(Color(0xFFDDE7F2), GroupedLight)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ===== TOP BAR =====
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Kembali",
                        tint = labelColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Now Playing",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(40.dp))
            }

            Spacer(Modifier.height(24.dp))

            // ===== ARTWORK =====
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (!song?.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song?.coverUrl)
                            .crossfade(200)
                            .build(),
                        contentDescription = song?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ===== TITLE =====
            Text(
                text = song?.title ?: "Tidak ada lagu",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song?.artist ?: "",
                fontSize = 15.sp,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!song?.album.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song?.album ?: "",
                    fontSize = 12.sp,
                    color = secondary.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(22.dp))

            // ===== SEEK BAR =====
            Slider(
                value = if (effectiveDuration > 0) {
                    effectivePosition.coerceIn(0, effectiveDuration).toFloat()
                } else 0f,
                onValueChange = {
                    isUserSeeking = true
                    seekPosition = it
                },
                onValueChangeFinished = {
                    onSeekTo(seekPosition.toLong())
                    isUserSeeking = false
                },
                valueRange = 0f..effectiveDuration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = secondary.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(effectivePosition), fontSize = 12.sp, color = secondary)
                Spacer(Modifier.weight(1f))
                Text(formatTime(duration), fontSize = 12.sp, color = secondary)
            }

            Spacer(Modifier.height(20.dp))

            // ===== CONTROLS =====
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) accent else secondary,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable(onClick = onShuffleToggle)
                )
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Sebelumnya",
                    tint = labelColor,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onPrev)
                )
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = onPlayPauseClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Berikutnya",
                    tint = labelColor,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onNext)
                )
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeat) accent else secondary,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable(onClick = onRepeatToggle)
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Preview 30 detik dari Apple Music",
                fontSize = 11.sp,
                color = secondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val total = millis / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
