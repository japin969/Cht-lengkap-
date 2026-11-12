package com.jayra.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.Song
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSearchDark
import com.jayra.app.ui.theme.JayraSearchLight
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

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
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    MusicHomeContent(
        uiState = uiState,
        searchQuery = searchQuery,
        currentSong = currentSong,
        isPlaying = isPlaying,
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
    val loginGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(Color(0xFF1E3A5F), Color(0xFF4A2E6B))
    )
    val topBarBg = Color.Transparent
    val screenBg = Color.Transparent
    val searchBg = Color(0x33FFFFFF)
    val textColor = Color.White
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // 1.1 TopAppBar (Tinggi 56dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("music_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Text(
                    text = "Musik",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                // Ikon Search 24dp Putih
                IconButton(
                    onClick = { /* Search field is already integrated below */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Cari",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Ikon MoreVert 24dp Putih
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
                            text = { Text("Segarkan") },
                            onClick = {
                                showMenu = false
                                onRetry()
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Mini Player bar when a song is selected/playing
            AnimatedVisibility(
                visible = currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
            ) {
                if (currentSong != null) {
                    MusicMiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onClick = { onSongClick(currentSong) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(loginGradient)
                .padding(innerPadding)
        ) {
            // 1.2 Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(searchBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color(0x99FFFFFF),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Cari lagu atau artis",
                            fontSize = 15.sp,
                            color = Color(0x99FFFFFF)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_search_input"),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = textColor
                        ),
                        cursorBrush = SolidColor(JayraBlue),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false)
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus teks",
                        tint = JayraSubtext,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }

            // History (muncul saat search kosong)
            if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pencarian terakhir",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0x99FFFFFF)
                        )
                        Text(
                            text = "Hapus",
                            fontSize = 13.sp,
                            color = Color(0xFF5AC8FA),
                            modifier = Modifier.clickable { onClearHistory() }
                        )
                    }
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(searchHistory, key = { it }) { q ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x33FFFFFF))
                                    .clickable { onHistoryClick(q) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = q,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 1.3 & 1.4 State UI: Hasil Pencarian, Loading, Empty, Error
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState) {
                    is MusicUiState.Loading -> {
                        MusicShimmerLoading()
                    }

                    is MusicUiState.Empty -> {
                        MusicEmptyState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is MusicUiState.Error -> {
                        MusicErrorState(
                            message = uiState.message,
                            onRetry = onRetry,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is MusicUiState.Success -> {
                        if (uiState.songs.isEmpty()) {
                            MusicEmptyState(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(
                                    items = uiState.songs,
                                    key = { it.id },
                                    contentType = { "song_item" }
                                ) { song ->
                                    val isCurrent = currentSong?.id == song.id
                                    MusicSongItem(
                                        song = song,
                                        isCurrentSong = isCurrent,
                                        isPlaying = isCurrent && isPlaying,
                                        onClick = { onSongClick(song) }
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

// 1.3 Item Hasil Pencarian
@Composable
private fun MusicSongItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val separatorColor = Color(0x33FFFFFF)
    val titleColor = if (isCurrentSong) Color(0xFF5AC8FA) else Color.White
    val searchBg = Color(0x1AFFFFFF)
    val subtitleColor = Color(0x99FFFFFF)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1AFFFFFF))
            .clickable(onClick = onClick)
            .testTag("song_item_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Album 56dp RoundedCornerShape(8dp) margin end 16dp
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(searchBg),
                contentAlignment = Alignment.Center
            ) {
                if (song.coverUrl.isNotBlank()) {
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
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Column (weight 1f)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.source.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        val badge = if (song.source == "youtube") "YouJay" else "SpoJay"
                        val badgeColor = if (song.source == "youtube") Color(0xFFFF3B30) else Color(0xFF34C759)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontSize = 15.sp,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.duration,
                    fontSize = 13.sp,
                    color = subtitleColor
                )
            }

            // Ikon Play 24dp JayraBlue di kanan, tap target 48dp
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("play_song_${song.id}")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = JayraBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Separator 0.5dp margin start 88dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 88.dp)
                .height(0.5.dp)
                .background(separatorColor)
        )
    }
}

// Mini Player bar
@Composable
private fun MusicMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(Color(0xB31E3A5F), Color(0xCC4A2E6B))
            ))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0x33FFFFFF))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(JayraBlue),
                contentAlignment = Alignment.Center
            ) {
                if (song.coverUrl.isNotBlank()) {
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
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = Color(0x99FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF5AC8FA),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// 1.4 Empty State
@Composable
private fun MusicEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "Music",
            tint = Color(0x99FFFFFF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cari lagu favoritmu",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xCCFFFFFF),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ketik judul lagu atau nama artis",
            fontSize = 15.sp,
            color = Color(0x99FFFFFF),
            textAlign = TextAlign.Center
        )
    }
}

// 1.4 Error State
@Composable
private fun MusicErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = "Warning",
            tint = JayraDangerRed,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (message.isNotBlank()) message else "Gagal memuat musik",
            fontSize = 17.sp,
            color = Color(0xCCFFFFFF),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Coba Lagi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

// 1.4 Shimmer Loading (8 item)
@Composable
private fun MusicShimmerLoading() {
    val isDark = isSystemInDarkTheme()
    val baseColor = Color(0x1AFFFFFF)
    val highlightColor = Color(0x33FFFFFF)

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim - 300f, y = translateAnim - 300f),
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }
    }
}

@Preview(name = "Music Home Light", showBackground = true)
@Composable
private fun MusicHomeScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            MusicHomeContent(
                uiState = MusicUiState.Success(
                    songs = listOf(
                        Song(
                            id = "1",
                            title = "Starboy",
                            artist = "The Weeknd, Daft Punk",
                            album = "Starboy",
                            duration = "3:50"
                        ),
                        Song(
                            id = "2",
                            title = "Blinding Lights",
                            artist = "The Weeknd",
                            album = "After Hours",
                            duration = "3:20"
                        )
                    )
                ),
                searchQuery = "",
                currentSong = Song(
                    id = "1",
                    title = "Starboy",
                    artist = "The Weeknd, Daft Punk",
                    album = "Starboy",
                    duration = "3:50"
                ),
                isPlaying = true,
                onSearchQueryChange = {},
                onSongClick = {},
                onPlayPauseClick = {},
                onRetry = {},
                onBack = {}
            )
        }
    }
}

@Preview(name = "Music Home Dark", showBackground = true)
@Composable
private fun MusicHomeScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            MusicHomeContent(
                uiState = MusicUiState.Success(
                    songs = listOf(
                        Song(
                            id = "1",
                            title = "Starboy",
                            artist = "The Weeknd",
                            album = "Starboy",
                            duration = "3:50"
                        )
                    )
                ),
                searchQuery = "Weeknd",
                currentSong = null,
                isPlaying = false,
                onSearchQueryChange = {},
                onSongClick = {},
                onPlayPauseClick = {},
                onRetry = {},
                onBack = {}
            )
        }
    }
}
