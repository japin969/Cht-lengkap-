package com.example.ui.stickers

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Gif
import com.example.model.Sticker
import com.example.model.StickersUiState
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

@Composable
fun StickersScreen(
    onBack: () -> Unit,
    viewModel: StickersViewModel = viewModel()
) {
    BackHandler { onBack() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StickersContent(
        uiState = uiState,
        onBack = onBack,
        onTabSelect = viewModel::selectTab,
        onPackSelect = viewModel::selectPack,
        onSearchQueryChange = viewModel::search
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickersContent(
    uiState: StickersUiState,
    onBack: () -> Unit,
    onTabSelect: (Int) -> Unit,
    onPackSelect: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val cardBg = if (isDark) TelegramCardDark else TelegramCardLight

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stiker dan GIF",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stickers_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg),
                modifier = Modifier.height(56.dp)
            )
        },
        containerColor = screenBg
    ) { innerPadding ->
        when (uiState) {
            is StickersUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TelegramBlue)
                }
            }
            is StickersUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        color = TelegramSubtext,
                        fontSize = 16.sp
                    )
                }
            }
            is StickersUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada stiker ditemukan.",
                        color = TelegramSubtext,
                        fontSize = 16.sp
                    )
                }
            }
            is StickersUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("stickers_search_input"),
                        placeholder = {
                            Text(
                                if (uiState.selectedTab == 0) "Cari stiker atau emoji..." else "Cari GIF seru...",
                                color = TelegramSubtext
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = TelegramSubtext
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = TelegramSubtext
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TelegramBlue,
                            unfocusedBorderColor = if (isDark) Color(0xFF2C384A) else Color(0xFFE0E0E0),
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg
                        )
                    )

                    // Tab Row
                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = cardBg,
                        contentColor = TelegramBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                                color = TelegramBlue,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { onTabSelect(0) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("tab_stickers"),
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mood,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Stiker", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { onTabSelect(1) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("tab_gifs"),
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Gif,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("GIF", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }

                    if (uiState.selectedTab == 0) {
                        // Sticker pack chips
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.stickerPacks) { pack ->
                                FilterChip(
                                    selected = uiState.selectedPack == pack,
                                    onClick = { onPackSelect(pack) },
                                    label = { Text(pack) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TelegramBlue,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("pack_chip_$pack")
                                )
                            }
                        }

                        // Stickers Grid 3 Columns
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.stickers, key = { it.id }) { sticker ->
                                StickerGridItem(
                                    sticker = sticker,
                                    cardBg = cardBg,
                                    onClick = {
                                        Toast.makeText(context, "Stiker ${sticker.name} dipilih", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    } else {
                        // GIFs Grid 2 Columns
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.gifs, key = { it.id }) { gif ->
                                GifGridItem(
                                    gif = gif,
                                    cardBg = cardBg,
                                    onClick = {
                                        Toast.makeText(context, "GIF: ${gif.title}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerGridItem(
    sticker: Sticker,
    cardBg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("sticker_item_${sticker.id}"),
        color = cardBg,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(sticker.url)
                    .crossfade(true)
                    .build(),
                contentDescription = sticker.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Emoji overlay badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sticker.emoji,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun GifGridItem(
    gif: Gif,
    cardBg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("gif_item_${gif.id}"),
        color = cardBg,
        tonalElevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(gif.url)
                    .crossfade(true)
                    .build(),
                contentDescription = gif.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = gif.title,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview(name = "StickersScreen Light Mode", showBackground = true)
@Composable
fun StickersScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        StickersContent(
            uiState = StickersUiState.Success(
                selectedTab = 0,
                stickerPacks = listOf("Semua", "Populer", "Kucing"),
                selectedPack = "Semua",
                stickers = listOf(
                    Sticker("1", "Senyum", "https://picsum.photos/200", "Populer", "😊"),
                    Sticker("2", "Kucing", "https://picsum.photos/201", "Kucing", "🐱")
                )
            ),
            onBack = {},
            onTabSelect = {},
            onPackSelect = {},
            onSearchQueryChange = {}
        )
    }
}

@Preview(name = "StickersScreen Dark Mode", showBackground = true)
@Composable
fun StickersScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        StickersContent(
            uiState = StickersUiState.Success(
                selectedTab = 0,
                stickerPacks = listOf("Semua", "Populer", "Kucing"),
                selectedPack = "Semua",
                stickers = listOf(
                    Sticker("1", "Senyum", "https://picsum.photos/200", "Populer", "😊"),
                    Sticker("2", "Kucing", "https://picsum.photos/201", "Kucing", "🐱")
                )
            ),
            onBack = {},
            onTabSelect = {},
            onPackSelect = {},
            onSearchQueryChange = {}
        )
    }
}
