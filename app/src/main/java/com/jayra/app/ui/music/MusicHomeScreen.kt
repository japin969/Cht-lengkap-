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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.jayra.app.ui.theme.IOSBlue
import com.jayra.app.ui.theme.IOSBlueDark
import com.jayra.app.ui.theme.IOSGray5
import com.jayra.app.ui.theme.IOSGray5Dark
import com.jayra.app.ui.theme.IOSGray6
import com.jayra.app.ui.theme.IOSGray6Dark
import com.jayra.app.ui.theme.IOSGreen
import com.jayra.app.ui.theme.IOSGreenDark
import com.jayra.app.ui.theme.IOSLabel
import com.jayra.app.ui.theme.IOSLabelDark
import com.jayra.app.ui.theme.IOSRed
import com.jayra.app.ui.theme.IOSRedDark
import com.jayra.app.ui.theme.IOSSecondaryLabel
import com.jayra.app.ui.theme.IOSSecondaryLabelDark
import com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark
import com.jayra.app.ui.theme.IOSSecondarySystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackgroundDark
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.ui.theme.squircle
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.GlassSurfaceSpec

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
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    MusicHomeContent(
        uiState = uiState,
        searchQuery = searchQuery,
        currentSong = currentSong,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        currentPosition = currentPosition,
        duration = duration,
        searchHistory = searchHistory,
        onHistoryClick = viewModel::onHistoryClick,
        onClearHistory = viewModel::onClearHistory,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSearchSubmit = { viewModel.searchNow() },
        onSongClick = { song ->
            viewModel.onSongClick(song)
            onSongClick(song)
        },
        onPlayPauseClick = viewModel::onPlayPauseClick,
        onNextClick = viewModel::onNext,
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
    isBuffering: Boolean = false,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    searchHistory: List<String> = emptyList(),
    onHistoryClick: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit = {},
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onRetry: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val cardBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground
    val searchBg = if (isDark) IOSGray6Dark else Color(0xFFE5E5EA)
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subLabelColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue
    val borderColor = if (isDark) LiquidGlassSpecV2.DarkBorderTop else Color(0x14000000)

    // Glass spec (tanpa blur) untuk komponen Music — spes dari LiquidGlassSpecV2
    val glassTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
    val glassSpec = remember(glassTier, isDark) { LiquidGlassSpecV2.forTier(glassTier, isDark).surface }

    val keyboardController = LocalSoftwareKeyboardController.current
    var showMenu by remember { mutableStateOf(false) }

    // Ruang aman agar floating LiquidNavBarRow di MainScreen tidak menutupi MiniPlayer/item terbawah
    val navSafeBottom = if (onBack == null) 88.dp else 12.dp

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("music_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = accentBlue
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = "Musik",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Menu Lainnya",
                                tint = accentBlue
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
                            if (searchHistory.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Hapus Riwayat") },
                                    onClick = {
                                        showMenu = false
                                        onClearHistory()
                                    }
                                )
                            }
                        }
                    }
                }

                // iOS Search Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(40.dp)
                        .glassSurfaceV2(glassSpec, solidColor = searchBg, cornerRadius = 12.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Cari",
                        tint = subLabelColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari lagu atau artis...",
                                fontSize = 15.sp,
                                color = subLabelColor
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
                                color = labelColor
                            ),
                            cursorBrush = SolidColor(accentBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    onSearchSubmit()
                                }
                            )
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hapus teks",
                            tint = subLabelColor,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSearchQueryChange("") }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pencarian Terakhir",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subLabelColor
                            )
                            Text(
                                text = "Hapus",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentBlue,
                                modifier = Modifier.clickable { onClearHistory() }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(searchHistory, key = { it }) { q ->
                                Box(
                                    modifier = Modifier
                                        .glassSurfaceV2(glassSpec, solidColor = cardBg, cornerRadius = 16.dp)
                                        .clickable { onHistoryClick(q) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = q,
                                        fontSize = 13.sp,
                                        color = labelColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (uiState) {
                        is MusicUiState.Loading -> MusicShimmerLoading(isDark = isDark)
                        is MusicUiState.Empty -> MusicEmptyState(
                            isDark = isDark,
                            accentBlue = accentBlue,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(bottom = navSafeBottom)
                        )
                        is MusicUiState.Error -> MusicErrorState(
                            message = uiState.message,
                            accentBlue = accentBlue,
                            labelColor = labelColor,
                            onRetry = onRetry,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(bottom = navSafeBottom)
                        )
                        is MusicUiState.Success -> {
                            if (uiState.songs.isEmpty()) {
                                MusicEmptyState(
                                    isDark = isDark,
                                    accentBlue = accentBlue,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(bottom = navSafeBottom)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 8.dp,
                                        bottom = navSafeBottom + if (currentSong != null) 80.dp else 12.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                            isBuffering = isCurrent && isBuffering,
                                            isDark = isDark,
                                            cardBg = cardBg,
                                            borderColor = borderColor,
                                            glassSpec = glassSpec,
                                            labelColor = labelColor,
                                            subLabelColor = subLabelColor,
                                            accentBlue = accentBlue,
                                            onClick = { onSongClick(song) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navSafeBottom)
            ) {
                if (currentSong != null) {
                    MusicMiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        progress = if (duration > 0L) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                        isDark = isDark,
                        accentBlue = accentBlue,
                        glassSpec = glassSpec,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onClick = { onSongClick(currentSong) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicSongItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    glassSpec: GlassSurfaceSpec,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    onClick: () -> Unit
) {
    val shape = squircle(14)
    val titleColor = if (isCurrentSong) accentBlue else labelColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurfaceV2(glassSpec, solidColor = cardBg, cornerRadius = 14.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("song_item_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(squircle(10))
                .background(if (isDark) IOSGray6Dark else IOSGray6),
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
                    tint = subLabelColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (song.source.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    val isYt = song.source == "youtube"
                    val badge = if (isYt) "YouJay" else "SpoJay"
                    val badgeColor = if (isYt) {
                        if (isDark) IOSRedDark else IOSRed
                    } else {
                        if (isDark) IOSGreenDark else IOSGreen
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(badgeColor.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 14.sp,
                color = subLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.duration} • Full Track",
                fontSize = 12.sp,
                color = subLabelColor
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accentBlue
                )
            } else {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("play_song_${song.id}")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                        tint = accentBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    isDark: Boolean,
    accentBlue: Color,
    glassSpec: GlassSurfaceSpec,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onClick: () -> Unit
) {
    val bg = if (isDark) LiquidGlassSpecV2.DarkGlassElevated else LiquidGlassSpecV2.LightGlassElevated
    val border = if (isDark) LiquidGlassSpecV2.DarkBorderTop else Color(0x1F000000)
    val label = if (isDark) IOSLabelDark else IOSLabel
    val sub = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .glassSurfaceV2(glassSpec, solidColor = bg, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(squircle(9))
                    .background(accentBlue.copy(alpha = 0.15f)),
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
                        tint = accentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isBuffering) "Menghubungkan audio..." else song.artist,
                    fontSize = 12.sp,
                    color = if (isBuffering) accentBlue else sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = accentBlue
                )
            } else {
                IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = accentBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            IconButton(onClick = onNextClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Berikutnya",
                    tint = label,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = accentBlue,
            trackColor = Color.Transparent
        )
    }
}

@Composable
private fun MusicEmptyState(
    isDark: Boolean,
    accentBlue: Color,
    modifier: Modifier = Modifier
) {
    val label = if (isDark) IOSLabelDark else IOSLabel
    val sub = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accentBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = "Music",
                tint = accentBlue,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Putar Musik Full Track",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = label,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Ketik judul lagu atau nama artis di kolom pencarian",
            fontSize = 14.sp,
            color = sub,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MusicErrorState(
    message: String,
    accentBlue: Color,
    labelColor: Color,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = "Warning",
            tint = IOSRed,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message.ifBlank { "Gagal memuat musik" },
            fontSize = 16.sp,
            color = labelColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Coba Lagi",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun MusicShimmerLoading(isDark: Boolean) {
    val baseColor = if (isDark) IOSGray6Dark else IOSGray5
    val highlightColor = if (isDark) IOSGray5Dark else IOSGray6

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim - 250f, y = 0f),
        end = Offset(x = translateAnim, y = 250f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(squircle(14))
                    .background(brush)
            )
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
                        Song(id = "1", title = "Karena Kamu", artist = "Geisha", duration = "3:54", source = "spotify"),
                        Song(id = "2", title = "Geisha - Karena Kamu (Lyrics)", artist = "RinaiMelodi", duration = "03:55", source = "youtube")
                    )
                ),
                searchQuery = "karna kamu",
                currentSong = Song(id = "1", title = "Karena Kamu", artist = "Geisha", duration = "3:54", source = "spotify"),
                isPlaying = true,
                onSearchQueryChange = {},
                onSongClick = {},
                onPlayPauseClick = {},
                onRetry = {}
            )
        }
    }
}
