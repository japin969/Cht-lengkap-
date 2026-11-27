package com.jayra.app.ui.anime

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.stiffness
import androidx.compose.animation.core.dampingRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focus.LocalFocusManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.ImeAction
import androidx.compose.foundation.text.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.ErrorOutlined
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs

@Composable
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (AnimeSearchItem) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val glassSpec = remember(GlassEffectPrefs.enabledState.value) {
        LiquidGlassSpecV2.forTier(GlassEffectPrefs.resolveTier(), isSystemInDarkTheme())
    }

    var searchText by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Collect all genres from all anime
    val allGenres = remember(state) {
        (state.popular + state.ongoing + state.completed)
            .flatMap { it.type?.let { listOf(it) } ?: emptyList() }
            .distinct()
            .sorted()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // ── Top Bar with Search ─────────────────────────────────
            TopSearchBar(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onFilterClick = { showFilter = !showFilter },
                glassSpec = glassSpec,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // ── Genre Filter Chips ──────────────────────────────────
            AnimatedVisibility(visible = showFilter) {
                GenreFilterChips(
                    genres = allGenres,
                    glassSpec = glassSpec,
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)
                )
            }

            // ── Content ─────────────────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null && !state.hasAnyData) {
                ErrorState(
                    message = state.error ?: "Koneksi bermasalah",
                    onRetry = { viewModel.retry() },
                    glassSpec = glassSpec
                )
            } else if (!state.hasAnyData) {
                EmptyState(
                    title = "Belum ada anime",
                    subtitle = "Daftar anime sedang kosong. Coba muat ulang.",
                    onRetry = { viewModel.retry() },
                    glassSpec = glassSpec
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp), // Space for bottom nav
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ongoing Section
                    if (state.ongoing.isNotEmpty()) item {
                        ContentSection(
                            title = "Sedang Tayang",
                            items = state.ongoing,
                            onItemClick = onAnimeClick,
                            glassSpec = glassSpec
                        )
                    }

                    // Popular Section
                    if (state.popular.isNotEmpty()) item {
                        ContentSection(
                            title = "Populer",
                            items = state.popular,
                            onItemClick = onAnimeClick,
                            glassSpec = glassSpec
                        )
                    }

                    // Completed Section
                    if (state.completed.isNotEmpty()) item {
                        ContentSection(
                            title = "Selesai",
                            items = state.completed,
                            onItemClick = onAnimeClick,
                            glassSpec = glassSpec
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// TOP SEARCH BAR (DeepSeek/ChatGPT Style)
// ═════════════════════════════════════════════════════════════════

@Composable
fun TopSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search field with glass background
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 22.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                androidx.compose.material3.TextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = {
                        Text(
                            text = "Cari anime...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* TODO: handle search */ }),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Filter button
        IconButton(
            onClick = { /* TODO: show filter bottom sheet */ },
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// GENRE FILTER CHIPS
// ════════════════════════════════════════════════════════════════

@Composable
fun GenreFilterChips(
    genres: List<String>,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(genres) { genre ->
            GenreChip(
                label = genre,
                onClick = { /* TODO: filter by genre */ },
                glassSpec = glassSpec
            )
        }
    }
}

@Composable
fun GenreChip(
    label: String,
    onClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 200, dampingRatio = 15),
        label = "genre_chip_scale"
    )

    Box(
        modifier = modifier
            .height(32.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 16.dp)
            .scale(scale)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onPress = { pressed = true },
                    onRelease = { pressed = false; onClick() },
                    onCancel = { pressed = false }
                )
            }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 4.dp).align(Alignment.Center)
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// CONTENT SECTION (Spring Animated Cards)
// ═════════════════════════════════════════════════════════════════

@Composable
fun ContentSection(
    title: String,
    items: List<AnimeSearchItem>,
    onItemClick: (AnimeSearchItem) -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Section header
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Items in horizontal scroll
        LazyRow(
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { anime ->
                SpringAnimeCard(
                    anime = anime,
                    onClick = { onItemClick(anime) },
                    glassSpec = glassSpec
                )
            }
        }
    }
}

@Composable
fun SpringAnimeCard(
    anime: AnimeSearchItem,
    onClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20),
        label = "anime_card_scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 2f else 8f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20),
        label = "anime_card_elevation"
    )

    Column(
        modifier = modifier
            .width(130.dp)
            .scale(scale)
            .shadow(elevation = elevation.dp, shape = RoundedCornerShape(16.dp), clip = true)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onPress = { pressed = true },
                    onRelease = { pressed = false; onClick() },
                    onCancel = { pressed = false }
                )
            }
    ) {
        // Poster with glass overlay
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(195.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = anime.image,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = placeholder(),
                error = errorPlaceholder()
            )

            // Glass gradient overlay (bottom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }

        // Title & Info
        Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp, horizontal = 4.dp)
        ) {
            Text(
                anime.title ?: "Unknown",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                anime.latestEpisode ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// ERROR & EMPTY STATES (Glass Style)
// ═════════════════════════════════════════════════════════════════

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                glassSpec = glassSpec,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material.Icon(
                        imageVector = Icons.Rounded.ErrorOutlined,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Gagal memuat anime",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Coba Lagi") }
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    onRetry: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                glassSpec = glassSpec,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Coba Lagi") }
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 20.dp)
    ) {
        content()
    }
}

// ══════════════════════════════════════════════════════════════════
// PLACEHOLDERS
// ═════════════════════════════════════════════════════════════════

@Composable
private fun placeholder() = Box(
    Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
) {
    androidx.compose.material.Icon(
        imageVector = Icons.Rounded.Image,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun errorPlaceholder() = Box(
    Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.errorContainer)
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material.Icon(
            imageVector = Icons.Rounded.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.height(4.dp))
        Text("Gagal muat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

import androidx.compose.foundation.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focus.LocalFocusManager
import androidx.compose.foundation.text.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutlined
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs