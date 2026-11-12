package com.jayra.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.Color(0x1AFFFFFF)
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme
import java.util.Locale

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
    val gradientBackground = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xFF1E3A5F), Color(0xFF4A2E6B))
        } else {
            listOf(Color(0xFF3B5B78), Color(0xFF1E2833))
        }
    )

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }

    val effectivePosition = if (isUserSeeking) seekPosition.toLong() else currentPosition
    val effectiveDuration = if (duration > 0L) duration else 210000L // 3:30 default fallback

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 2.1 TopAppBar (Tinggi 56dp, background transparan)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("player_back_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Tutup Pemutar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Sedang Diputar",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Menu Lainnya",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
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

        Spacer(modifier = Modifier.height(24.dp))

        // 2.2 Cover Album (300dp, RoundedCornerShape 16dp, center, Shadow 8dp)
        Box(
            modifier = Modifier
                .size(300.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1AFFFFFF)),
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
                    tint = JayraSubtext,
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2.3 Info Lagu
        Text(
            text = song?.title ?: "Tidak ada lagu dipilih",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = song?.artist ?: "Pilih lagu untuk memulai",
            fontSize = 17.sp,
            color = JayraSubtext,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2.4 Progress Bar
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
                activeTrackColor = JayraBlue,
                inactiveTrackColor = JayraSubtext.copy(alpha = 0.5f)
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
                color = JayraSubtext
            )
            Text(
                text = formatTime(effectiveDuration),
                fontSize = 12.sp,
                color = JayraSubtext
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2.5 Kontrol Player (Row center, spacing 24dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Shuffle 24dp JayraSubtext
            IconButton(
                onClick = onShuffleToggle,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Acak",
                    tint = if (isShuffle) JayraBlue else JayraSubtext,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tombol Prev 32dp Putih
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
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tombol Play/Pause 64dp JayraBlue (circle background #FFFFFF alpha 0.2)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                        tint = JayraBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tombol Next 32dp Putih
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
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tombol Repeat 24dp JayraSubtext
            IconButton(
                onClick = onRepeatToggle,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = "Ulangi",
                    tint = if (isRepeat) JayraBlue else JayraSubtext,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Preview(name = "Music Player Light", showBackground = true)
@Composable
private fun MusicPlayerScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            MusicPlayerContent(
                song = Song(
                    id = "1",
                    title = "Starboy",
                    artist = "The Weeknd, Daft Punk",
                    album = "Starboy",
                    duration = "3:50"
                ),
                isPlaying = true,
                currentPosition = 84000L,
                duration = 230000L,
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

@Preview(name = "Music Player Dark", showBackground = true)
@Composable
private fun MusicPlayerScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            MusicPlayerContent(
                song = Song(
                    id = "2",
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    album = "After Hours",
                    duration = "3:20"
                ),
                isPlaying = false,
                currentPosition = 0L,
                duration = 200000L,
                isShuffle = true,
                isRepeat = false,
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
