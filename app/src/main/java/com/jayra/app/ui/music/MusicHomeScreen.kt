package com.jayra.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
private val IOSGreen = Color(0xFF34C759)
private val IOSGreenDark = Color(0xFF30D158)
private val GroupedLight = Color(0xFFF2F2F7)
private val GroupedDark = Color(0xFF1C1C1E)
private val CardDark = Color(0xFF2C2C2E)
private val SecondaryLight = Color(0xFF8A8A8E)
private val SecondaryDark = Color(0xFF98989F)

@Composable
fun MusicHomeScreen(
    onBack: (() -> Unit)? = null,
    onSongClick: (Song) -> Unit,
    viewModel: MusicViewModel = viewModel()
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isPreparing by viewModel.isPreparing.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    MusicHomeContent(
        uiState = uiState,
        searchQuery = searchQuery,
        currentSong = currentSong,
        isPlaying = isPlaying,
        isPreparing = isPreparing,
        currentPosition = currentPosition,
        duration = duration,
        searchHistory = searchHistory,
        onHistoryClick = viewModel::onHistoryClick,
        onClearHistory = viewModel::onClearHistory,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSongClick = { song ->
            viewModel.onSongClick(song)
            onSongClick(song)
        },
        onPlayPauseClick = viewModel::onPlayPauseClick,
        onRetry = viewModel::onRetry,
        onBack = onBack
    )
}

@Composable
fun MusicHomeContent(
    uiState: MusicUiState,
    searchQuery: String,
    currentSong: Song?,
    isPlaying: Boolean,
    isPreparing: Boolean = false,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    searchHistory: List<String> = emptyList(),
    onHistoryClick: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onRetry: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) GroupedDark else GroupedLight
    val cardBg = if (isDark) CardDark else Color.White
    val labelColor = if (isDark) Color.White else Color.Black
    val secondary = if (isDark) SecondaryDark else SecondaryLight
    val accent = if (isDark) IOSBlueDark else IOSBlue
    val playAccent = if (isDark) IOSGreenDark else IOSGreen

    Box(
        Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ===== HEADER =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                            .testTag("music_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Kembali",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = "Musik",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    color = labelColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // ===== SEARCH BAR iOS =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE3E3E8))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = secondary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Box(Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Cari lagu, penyanyi...", fontSize = 16.sp, color = secondary)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_search_input"),
                        textStyle = TextStyle(fontSize = 16.sp, color = labelColor),
                        cursorBrush = SolidColor(accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false)
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus",
                        tint = secondary,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }

            // ===== HISTORY CHIPS =====
            if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pencarian terakhir", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = secondary)
                        Text(
                            "Hapus",
                            fontSize = 13.sp,
                            color = accent,
                            modifier = Modifier.clickable { onClearHistory() }
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchHistory, key = { it }) { q ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) CardDark else Color.White)
                                    .clickable { onHistoryClick(q) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(q, fontSize = 13.sp, color = labelColor, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // ===== CONTENT =====
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState) {
                    is MusicUiState.Loading -> MusicShimmerRows(isDark, cardBg)

                    is MusicUiState.Empty -> MusicPlaceholder(
                        title = if (searchQuery.isBlank()) "Cari lagu dulu yuk" else "Gak ketemu lagu",
                        message = if (searchQuery.isBlank())
                            "Ketik judul lagu atau nama penyanyi di kolom pencarian."
                        else "Coba kata kunci lain atau tekan coba lagi.",
                        accent = accent,
                        secondary = secondary,
                        showRetry = searchQuery.isNotBlank(),
                        onRetry = onRetry,
                        onSuggestion = onSearchQueryChange
                    )

                    is MusicUiState.Error -> MusicPlaceholder(
                        title = "Gagal mencari lagu",
                        message = uiState.message,
                        accent = accent,
                        secondary = secondary,
                        showRetry = true,
                        onRetry = onRetry,
                        onSuggestion = onSearchQueryChange
                    )

                    is MusicUiState.Success -> {
                        if (uiState.songs.isEmpty()) {
                            MusicPlaceholder(
                                title = if (searchQuery.isBlank()) "Cari lagu dulu yuk" else "Gak ketemu lagu",
                                message = if (searchQuery.isBlank())
                                    "Ketik judul lagu atau nama penyanyi di kolom pencarian."
                                else "Coba kata kunci lain atau tekan coba lagi.",
                                accent = accent,
                                secondary = secondary,
                                showRetry = searchQuery.isNotBlank(),
                                onRetry = onRetry,
                                onSuggestion = onSearchQueryChange
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 6.dp,
                                    bottom = if (currentSong != null) 84.dp else 16.dp
                                )
                            ) {
                                items(
                                    items = uiState.songs,
                                    key = { it.id },
                                    contentType = { "song_item" }
                                ) { song ->
                                    MusicSongRow(
                                        song = song,
                                        isCurrent = currentSong?.id == song.id,
                                        isPlaying = currentSong?.id == song.id && isPlaying,
                                        isPreparing = currentSong?.id == song.id && isPreparing,
                                        cardBg = cardBg,
                                        labelColor = labelColor,
                                        secondary = secondary,
                                        playAccent = playAccent,
                                        onClick = { onSongClick(song) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== MINI PLAYER (glass, NO blur) =====
        AnimatedVisibility(
            visible = currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (currentSong != null) {
                MusicMiniPlayer(
                    song = currentSong,
                    isPlaying = isPlaying,
                    isPreparing = isPreparing,
                    position = currentPosition,
                    duration = duration,
                    onClick = { onSongClick(currentSong) },
                    onPlayPauseClick = onPlayPauseClick
                )
            }
        }
    }
}

@Composable
private fun MusicSongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isPreparing: Boolean,
    cardBg: Color,
    labelColor: Color,
    secondary: Color,
    playAccent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("song_item_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.coverUrl)
                .crossfade(200)
                .build(),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(secondary.copy(alpha = 0.15f))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) playAccent else labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = song.duration.ifBlank { "Preview" },
            fontSize = 12.sp,
            color = secondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(playAccent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isPreparing -> Icons.Rounded.Refresh
                    isPlaying -> Icons.Rounded.Pause
                    else -> Icons.Rounded.PlayArrow
                },
                contentDescription = "Putar",
                tint = playAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MusicMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isPreparing: Boolean,
    position: Long,
    duration: Long,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val glassBase = if (isDark) Color(0xE61C1C1E) else Color(0xE6FFFFFF)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(glassBase)
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x33FFFFFF) else Color(0x42FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Progress tipis di atas (Apple Music style)
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(if (isDark) IOSGreenDark else IOSGreen)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = if (isDark) 0.04f else 0.35f), Color.Transparent)
                    )
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.coverUrl)
                    .crossfade(150)
                    .build(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    song.artist,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDark) SecondaryDark else SecondaryLight
                )
            }
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isPreparing -> Icons.Rounded.Refresh
                        isPlaying -> Icons.Rounded.Pause
                        else -> Icons.Rounded.PlayArrow
                    },
                    contentDescription = "Play/Pause",
                    tint = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MusicPlaceholder(
    title: String,
    message: String,
    accent: Color,
    secondary: Color,
    showRetry: Boolean,
    onRetry: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = accent, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = accent.copy(alpha = 0.95f))
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = secondary,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(14.dp))
        if (showRetry) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .clickable { onRetry() }
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text("Coba Lagi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))
        }
        Text("Coba cari:", fontSize = 12.sp, color = secondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Duka", "Bernadya", "Nadin Amizah").forEach { s ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .clickable { onSuggestion(s) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(s, fontSize = 13.sp, color = accent)
                }
            }
        }
    }
}

@Composable
private fun MusicShimmerRows(isDark: Boolean, cardBg: Color) {
    val transition = rememberInfiniteTransition(label = "music_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "music_shimmer_alpha"
    )
    val block = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(6) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(block.copy(alpha = alpha))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(block.copy(alpha = alpha))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.45f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(block.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}
