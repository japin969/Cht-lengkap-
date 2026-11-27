package com.jayra.app.ui.donghua

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.stiffness
import androidx.compose.animation.core.dampingRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.DonghuaCardItem
import com.jayra.app.data.DonghuaSource
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.ui.theme.JayraBlue

@Composable
fun DonghuaHomeScreen(
    viewModel: DonghuaViewModel = viewModel(),
    onItemClick: (DonghuaCardItem) -> Unit,
    onDetailClick: (String, DonghuaSource) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val glassSpec = remember(GlassEffectPrefs.enabledState.value) {
        LiquidGlassSpecV2.forTier(GlassEffectPrefs.resolveTier(), isSystemInDarkTheme())
    }

    var selectedTab by remember { mutableStateOf(DonghuaTab.DONGHUA) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Load initial data
    LaunchedEffect(Unit) {
        viewModel.loadHome()
        viewModel.loadGenres()
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
            // ── Top App Bar ─────────────────────────────────────────
            TopAppBar(
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                onSearchClick = { showSearch = true },
                glassSpec = glassSpec,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Search Overlay ──────────────────────────────────────
            AnimatedVisibility(visible = showSearch) {
                SearchOverlay(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { q ->
                        if (q.isNotBlank()) {
                            if (selectedTab == DonghuaTab.DONGHUA) {
                                viewModel.searchDonghua(q)
                            } else {
                                viewModel.searchDonghub(q)
                            }
                        }
                        showSearch = false
                    },
                    onClose = { showSearch = false },
                    glassSpec = glassSpec
                )
            }

            // ── Content ─────────────────────────────────────────────
            if (state.isLoadingHome) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = JayraBlue)
                }
            } else if (state.error != null && !state.home?.hasAnyData == true) {
                ErrorState(
                    message = state.error ?: "Koneksi bermasalah",
                    onRetry = { viewModel.loadHome() },
                    glassSpec = glassSpec
                )
            } else {
                when (selectedTab) {
                    DonghuaTab.DONGHUA -> DonghuaContent(
                        state = state,
                        viewModel = viewModel,
                        onItemClick = onItemClick,
                        onDetailClick = onDetailClick,
                        glassSpec = glassSpec
                    )
                    DonghuaTab.DONGHUB -> DonghubContent(
                        state = state,
                        viewModel = viewModel,
                        onItemClick = onItemClick,
                        onDetailClick = onDetailClick,
                        glassSpec = glassSpec
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// TAB ENUM
// ═══════════════════════════════════════════

enum class DonghuaTab {
    DONGHUA,
    DONGHUB
}

// ═══════════════════════════════════════════
// TOP APP BAR
// ═══════════════════════════════════════════

@Composable
fun TopAppBar(
    selectedTab: DonghuaTab,
    onTabChange: (DonghuaTab) -> Unit,
    onSearchClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Tab Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 0.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Title
                Text(
                    text = "Donghua",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = JayraBlue,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(Modifier.weight(1f))

                // Tab Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TabButton(
                        label = "Donghua",
                        icon = Icons.Rounded.Tv,
                        selected = selectedTab == DonghuaTab.DONGHUA,
                        onClick = { onTabChange(DonghuaTab.DONGHUA) },
                        glassSpec = glassSpec
                    )
                    TabButton(
                        label = "Donghub",
                        icon = Icons.Rounded.Movie,
                        selected = selectedTab == DonghuaTab.DONGHUB,
                        onClick = { onTabChange(DonghuaTab.DONGHUB) },
                        glassSpec = glassSpec
                    )
                }

                // Search Button
                IconButton(onClick = onSearchClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Cari",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20)
    )

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .glassSurfaceV2(
                spec = if (selected) glassSpec.surface.copy(
                    background = glassSpec.surface.background.copy(
                        gradientStops = glassSpec.surface.background.gradientStops.map { it.copy(color = JayraBlue.copy(alpha = 0.15f)) }
                    )
                ) else glassSpec.surface,
                solidColor = if (selected) JayraBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                cornerRadius = 20.dp
            )
            .scale(scale)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onRelease = onClick)
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) JayraBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) JayraBlue else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
        }
    }
}

// ═══════════════════════════════════════════
// SEARCH OVERLAY
// ═══════════════════════════════════════════

@Composable
fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClose: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 28.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Cari",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    androidx.compose.material3.TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        placeholder = { Text("Cari Donghua...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                            imeAction = androidx.compose.foundation.text.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSearch(query) }
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = JayraBlue
                        )
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// DONGHUA CONTENT
// ═══════════════════════════════════════════

@Composable
fun DonghuaContent(
    state: DonghuaUiState,
    viewModel: DonghuaViewModel,
    onItemClick: (DonghuaCardItem) -> Unit,
    onDetailClick: (String, DonghuaSource) -> Unit,
    glassSpec: LiquidGlassSpecV2
) {
    val home = state.home?.donghuaData?.data

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        home?.recommend?.let { recommends ->
            if (recommends.isNotEmpty()) {
                item {
                    HeroBanner(
                        items = recommends.map { DonghuaCardItem.fromDonghua(it, DonghuaSource.DONGHUA) },
                        onClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Ongoing Section
        state.ongoing?.let { section ->
            if (section.hasAnyData) {
                item {
                    SectionHeader(title = "Sedang Tayang")
                    SectionGrid(
                        donghuaItems = section.donghuaItems,
                        donghubItems = section.donghubItems,
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Latest Section
        home?.latest?.let { latest ->
            if (latest.isNotEmpty()) {
                item {
                    SectionHeader(title = "Terbaru")
                    HorizontalSection(
                        items = latest.map { DonghuaCardItem.fromDonghua(it, DonghuaSource.DONGHUA) },
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Completed Section
        state.completed?.let { section ->
            if (section.hasAnyData) {
                item {
                    SectionHeader(title = "Selesai")
                    SectionGrid(
                        donghuaItems = section.donghuaItems,
                        donghubItems = section.donghubItems,
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Schedule Section
        state.schedule?.let { schedule ->
            if (schedule.hasAnyData) {
                item {
                    SectionHeader(title = "Jadwal Rilis")
                    ScheduleSection(
                        donghuaData = schedule.donghuaItems,
                        donghubData = schedule.donghubItems,
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// DONGHUB CONTENT
// ═══════════════════════════════════════════

@Composable
fun DonghubContent(
    state: DonghuaUiState,
    viewModel: DonghuaViewModel,
    onItemClick: (DonghuaCardItem) -> Unit,
    onDetailClick: (String, DonghuaSource) -> Unit,
    glassSpec: LiquidGlassSpecV2
) {
    val home = state.home?.donghubData?.data

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Featured Banner
        home?.featured?.let { featured ->
            if (featured.isNotEmpty()) {
                item {
                    HeroBanner(
                        items = featured.map { DonghuaCardItem.fromDonghub(it, DonghuaSource.DONGHUB) },
                        onClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Latest Section
        home?.latest?.let { latest ->
            if (latest.isNotEmpty()) {
                item {
                    SectionHeader(title = "Terbaru")
                    HorizontalSection(
                        items = latest.map { DonghuaCardItem.fromDonghub(it, DonghuaSource.DONGHUB) },
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Popular Section
        home?.popular?.let { popular ->
            if (popular.isNotEmpty()) {
                item {
                    SectionHeader(title = "Populer")
                    HorizontalSection(
                        items = popular.map { DonghuaCardItem.fromDonghub(it, DonghuaSource.DONGHUB) },
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Movies Section
        home?.movies?.let { movies ->
            if (movies.isNotEmpty()) {
                item {
                    SectionHeader(title = "Movie")
                    HorizontalSection(
                        items = movies.map { DonghuaCardItem.fromDonghub(it, DonghuaSource.DONGHUB) },
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Schedule Section
        state.schedule?.let { schedule ->
            if (schedule.hasAnyData) {
                item {
                    SectionHeader(title = "Jadwal Rilis")
                    ScheduleSection(
                        donghuaData = schedule.donghuaItems,
                        donghubData = schedule.donghubItems,
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }

        // Live TV Section (if any)
        home?.schedule?.let { schedule ->
            schedule.values.flatten().firstOrNull()?.let { _ ->
                item {
                    SectionHeader(title = "Live TV")
                    LiveTVSection(
                        items = schedule.values.flatten().map { DonghuaCardItem.fromDonghub(it, DonghuaSource.DONGHUB) },
                        onItemClick = { onDetailClick(it.id, it.source) },
                        glassSpec = glassSpec
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// UI COMPONENTS
// ═══════════════════════════════════════════

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun HorizontalSection(
    items: List<DonghuaCardItem>,
    onItemClick: (DonghuaCardItem) -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            DonghuaCard(
                item = item,
                onClick = { onItemClick(item) },
                glassSpec = glassSpec
            )
        }
    }
}

@Composable
fun SectionGrid(
    donghuaItems: List<DonghuaCardItem>,
    donghubItems: List<DonghuaCardItem>,
    onItemClick: (DonghuaCardItem) -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    val allItems = (donghuaItems + donghubItems).distinctBy { it.id }

    LazyVerticalGrid(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(allItems) { item ->
            DonghuaCard(
                item = item,
                onClick = { onItemClick(item) },
                glassSpec = glassSpec
            )
        }
    }
}

@Composable
fun ScheduleSection(
    donghuaData: List<DonghuaCardItem>,
    donghubData: List<DonghuaCardItem>,
    onItemClick: (DonghuaCardItem) -> Unit,
    glassSpec: LiquidGlassSpecV2
) {
    val allItems = (donghuaData + donghubData).distinctBy { it.id }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(allItems) { item ->
            DonghuaCard(
                item = item,
                onClick = { onItemClick(item) },
                glassSpec = glassSpec
            )
        }
    }
}

@Composable
fun LiveTVSection(
    items: List<DonghuaCardItem>,
    onItemClick: (DonghuaCardItem) -> Unit,
    glassSpec: LiquidGlassSpecV2
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            LiveTVCard(
                item = item,
                onClick = { onItemClick(item) },
                glassSpec = glassSpec
            )
        }
    }
}

@Composable
fun HeroBanner(
    items: List<DonghuaCardItem>,
    onClick: (DonghuaCardItem) -> Unit,
    glassSpec: LiquidGlassSpecV2
) {
    val currentIndex = remember { mutableStateOf(0) }

    // Auto-scroll
    LaunchedEffect(items) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            currentIndex.value = (currentIndex.value + 1) % items.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Background Image
        items.getOrNull(currentIndex.value)?.let { item ->
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = bannerPlaceholder(),
                error = bannerErrorPlaceholder()
            )
        }

        // Gradient Overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        // Content Overlay
        items.getOrNull(currentIndex.value)?.let { item ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.score?.let { score ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Star, contentDescription = "Rating", tint = Color.Yellow, modifier = Modifier.size(14.dp))
                            Text(text = "%.1f".format(score), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item.episode?.let { ep ->
                        Text(text = "Ep $ep", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onClick(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = JayraBlue)
                ) {
                    Text("Tonton Sekarang", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Page Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            items.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentIndex.value) 24.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (index == currentIndex.value) JayraBlue else Color.White.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp)
                        .pointerInput(Unit) {
                            androidx.compose.foundation.gestures.detectTapGestures(onRelease = { currentIndex.value = index })
                        }
                )
            }
        }
    }
}

@Composable
fun DonghuaCard(
    item: DonghuaCardItem,
    onClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20)
    )

    Box(
        modifier = modifier
            .width(120.dp)
            .scale(scale)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onPress = { pressed = true },
                    onRelease = { pressed = false; onClick() },
                    onCancel = { pressed = false }
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Poster
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier
                    .width(120.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = cardPlaceholder(),
                error = cardErrorPlaceholder()
            )

            // Episode Badge
            item.episode?.let { ep ->
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .background(JayraBlue.copy(alpha = 0.9f))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "Ep $ep",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, horizontal = 4.dp, bottom = 4.dp)
            )

            // Score
            item.score?.let { score ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(12.dp))
                    Text(text = "%.1f".format(score), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun LiveTVCard(
    item: DonghuaCardItem,
    onClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2
) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onRelease = onClick)
            }
    ) {
        AsyncImage(
            model = item.poster,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = bannerPlaceholder(),
            error = bannerErrorPlaceholder()
        )

        // Live Badge
        Box(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopStart)
                .background(Color.Red.copy(alpha = 0.9f))
                .clip(RoundedCornerShape(4.dp))
        ) {
            Text(
                text = "LIVE",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Title Overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// PLACEHOLDERS
// ═══════════════════════════════════════════

@Composable
fun bannerPlaceholder() = Box(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
) {
    Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.Center).size(48.dp))
}

@Composable
fun bannerErrorPlaceholder() = Box(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer)
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(4.dp))
        Text("Gagal muat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
fun cardPlaceholder() = Box(
    Modifier
        .width(120.dp)
        .height(170.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
) {
    Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.Center).size(32.dp))
}

@Composable
fun cardErrorPlaceholder() = Box(
    Modifier
        .width(120.dp)
        .height(170.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.errorContainer)
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text("Gagal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    glassSpec: LiquidGlassSpecV2
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            glassSpec = glassSpec,
            modifier = Modifier.padding(24.dp).fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.ErrorOutlined, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Gagal memuat Donghua", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Coba Lagi") }
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
            .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 16.dp)
    ) {
        content()
    }
}

import androidx.compose.foundation.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures