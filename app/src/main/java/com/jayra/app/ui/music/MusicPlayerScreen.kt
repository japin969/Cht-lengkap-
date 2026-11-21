package com.jayra.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.Song
import com.jayra.app.ui.theme.IOSBlueDark
import com.jayra.app.ui.theme.IOSGreenDark
import com.jayra.app.ui.theme.IOSRedDark
import com.jayra.app.ui.theme.IOSSecondaryLabelDark
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.ui.theme.SquircleShape
import java.util.Locale

@Composable
fun MusicPlayerScreen(
    onBack: () -> Unit,
    viewModel: MusicViewModel = viewModel()
) {
    BackHandler { onBack() }

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val isRepeat by viewModel.isRepeat.collectAsStateWithLifecycle()

    MusicPlayerContent(
        song = currentSong,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        playbackError = playbackError,
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
        onRepeatToggle = viewModel::onRepeatToggle,
        onDismissError = viewModel::clearPlaybackError
    )
}

@Composable
fun MusicPlayerContent(
    song: Song?,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    playbackError: String? = null,
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
    onRepeatToggle: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0E1B2B), Color(0xFF151E30), Color(0xFF0A0A0C))
    )

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }

    val effectivePosition = if (isUserSeeking) seekPosition.toLong() else currentPosition
    val effectiveDuration = if (duration > 0L) duration else 180000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("player_back_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Tutup Pemutar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SEDANG DIPUTAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = IOSSecondaryLabelDark
                )
                if (song != null && song.source.isNotBlank()) {
                    val isYt = song.source == "youtube"
                    Text(
                        text = if (isYt) "YouJay Full Track" else "SpoJay Full Track",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isYt) IOSRedDark else IOSGreenDark
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Menu Lainnya",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Album: ${song?.album ?: "-"}") },
                        onClick = { showMenu = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .aspectRatio(1f)
                .shadow(elevation = 16.dp, shape = squircle(24))
                .clip(squircle(24))
                .background(Color(0x1FFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (song != null && song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = "Cover",
                    tint = IOSSecondaryLabelDark,
                    modifier = Modifier.size(96.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        if (!playbackError.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(IOSRedDark.copy(alpha = 0.22f))
                    .clickable { onDismissError() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = playbackError,
                    fontSize = 12.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Text(
            text = song?.title ?: "Tidak ada lagu dipilih",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = song?.artist ?: "Pilih lagu untuk memulai",
            fontSize = 16.sp,
            color = IOSSecondaryLabelDark,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        Slider(
            value = effectivePosition.toFloat().coerceIn(0f, effectiveDuration.toFloat()),
            onValueChange = {
                isUserSeeking = true
                seekPosition = it
            },
            onValueChangeFinished = {
                isUserSeeking = false
                onSeekTo(seekPosition.toLong())
            },
            valueRange = 0f..effectiveDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = IOSBlueDark,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_progress_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(effectivePosition),
                fontSize = 12.sp,
                color = IOSSecondaryLabelDark
            )
            Text(
                text = formatTime(effectiveDuration),
                fontSize = 12.sp,
                color = IOSSecondaryLabelDark
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffleToggle, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Acak",
                    tint = if (isShuffle) IOSBlueDark else IOSSecondaryLabelDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("player_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Sebelumnya",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = Color(0xFF0E1B2B)
                    )
                } else {
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = Color(0xFF0E1B2B),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Berikutnya",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(onClick = onRepeatToggle, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = "Ulangi",
                    tint = if (isRepeat) IOSBlueDark else IOSSecondaryLabelDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.4f))
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Preview(name = "Music Player Dark", showBackground = true)
@Composable
private fun MusicPlayerScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            MusicPlayerContent(
                song = Song(
                    id = "1",
                    title = "Karena Kamu",
                    artist = "Geisha",
                    album = "Meraih Bintang",
                    duration = "3:54",
                    source = "spotify"
                ),
                isPlaying = true,
                currentPosition = 84000L,
                duration = 234000L,
                isShuffle = false,
                isRepeat = true,
                onBack = {},
                onPlayPauseClick = {},
                onSeekTo = {},
                onNext = {},
                onPrev = {},
                onShuffleToggle = {},
                onRepeatToggle = {}
            )
        }
    }
}
