package com.jayra.app.ui.anime

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.AnimeSearchItem

private val IOSBlue = Color(0xFF007AFF)
private val IOSBlueDark = Color(0xFF0A84FF)
private val IOSGroupedLight = Color(0xFFF2F2F7)
private val IOSGroupedDark = Color(0xFF1C1C1E)
private val IOSSecondaryLight = Color(0xFF8A8A8E)
private val IOSSecondaryDark = Color(0xFF98989F)

@Composable
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val screenBg = if (isDark) IOSGroupedDark else IOSGroupedLight
    val cardBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val labelColor = if (isDark) Color.White else Color.Black
    val secondary = if (isDark) IOSSecondaryDark else IOSSecondaryLight
    val accent = if (isDark) IOSBlueDark else IOSBlue

    Column(
        Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // ===== LARGE TITLE =====
        Text(
            text = "Anime",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            color = labelColor,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
        )

        // ===== iOS SEARCH BAR (rounded 12dp, abu muda) =====
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
                if (state.searchQuery.isEmpty()) {
                    Text("Cari anime...", fontSize = 16.sp, color = secondary)
                }
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("anime_search_input"),
                    textStyle = TextStyle(fontSize = 16.sp, color = labelColor),
                    cursorBrush = SolidColor(accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false)
                )
            }
            if (state.searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Hapus",
                    tint = secondary,
                    modifier = Modifier
                        .size(17.dp)
                        .clickable { viewModel.onSearchQueryChange("") }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ===== SEGMENTED CONTROL iOS =====
        val tabs = listOf(
            AnimeTab.POPULAR to "Populer",
            AnimeTab.COMPLETED to "Tamat",
            AnimeTab.SCHEDULE to "Jadwal",
            AnimeTab.SEARCH to "Cari"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE9E9EB))
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tab, label) ->
                val selected = state.tab == tab
                val pillColor by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "seg_$label"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Color.White.copy(alpha = if (isDark) 0.14f * pillColor else pillColor)
                        )
                        .then(
                            if (selected && !isDark) Modifier.shadow(1.5.dp, RoundedCornerShape(7.dp))
                            else Modifier
                        )
                        .clickable { viewModel.selectTab(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) labelColor else secondary
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ===== CONTENT PER TAB =====
        Box(Modifier.fillMaxSize()) {
            when (state.tab) {
                AnimeTab.SCHEDULE -> AnimeScheduleContent(
                    state = state,
                    isDark = isDark,
                    cardBg = cardBg,
                    labelColor = labelColor,
                    secondary = secondary,
                    onRetry = viewModel::retry,
                    onAnimeClick = onAnimeClick
                )
                else -> AnimeGridContent(
                    state = state,
                    isDark = isDark,
                    cardBg = cardBg,
                    labelColor = labelColor,
                    secondary = secondary,
                    accent = accent,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::retry,
                    onAnimeClick = onAnimeClick
                )
            }
        }
    }
}

@Composable
private fun AnimeGridContent(
    state: AnimeUiState,
    isDark: Boolean,
    cardBg: Color,
    labelColor: Color,
    secondary: Color,
    accent: Color,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onAnimeClick: (String) -> Unit
) {
    val items = state.currentList
    val showLoading = (state.isLoading || state.isSearching) && items.isEmpty()

    Box(Modifier.fillMaxSize()) {
        when {
            showLoading -> AnimeShimmerGrid(isDark)

            items.isEmpty() && state.error != null -> AnimeStateCard(
                title = "Gagal memuat anime",
                message = state.error,
                onRetry = onRetry,
                accent = accent,
                secondary = secondary
            )

            items.isEmpty() && state.tab == AnimeTab.SEARCH && state.searchQuery.isBlank() -> AnimeStateCard(
                title = "Cari anime dulu yuk",
                message = "Ketik judul anime di kolom pencarian di atas.",
                onRetry = null,
                accent = accent,
                secondary = secondary
            )

            items.isEmpty() -> AnimeStateCard(
                title = "Gak ketemu anime",
                message = "Coba kata kunci lain atau muat ulang.",
                onRetry = onRetry,
                accent = accent,
                secondary = secondary
            )

            else -> {
                val gridState = rememberLazyGridState()
                // Pagination: load page berikutnya saat mendekati bawah (append, bukan replace).
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val total = gridState.layoutInfo.totalItemsCount
                        total > 0 && last >= total - 4
                    }
                }
                LaunchedEffect(shouldLoadMore, items.size) {
                    if (shouldLoadMore) onLoadMore()
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = items,
                        key = { index, item -> item.link ?: "anime_$index" },
                        contentType = { _, _ -> "anime_card" }
                    ) { _, anime ->
                        AnimeCard(
                            anime = anime,
                            isDark = isDark,
                            cardBg = cardBg,
                            secondary = secondary,
                            labelColor = labelColor,
                            showCompletedBadge = state.tab == AnimeTab.COMPLETED,
                            onClick = { anime.link?.let(onAnimeClick) }
                        )
                    }
                    if (state.isAppending) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("Memuat lagi...", fontSize = 13.sp, color = secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeCard(
    anime: AnimeSearchItem,
    isDark: Boolean,
    cardBg: Color,
    secondary: Color,
    labelColor: Color,
    showCompletedBadge: Boolean,
    onClick: () -> Unit
) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(anime.image)
                    .crossfade(200)
                    .build(),
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (showCompletedBadge) {
                Text(
                    text = "Tamat",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = anime.title ?: "Anime",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                text = buildString {
                    anime.type?.takeIf { it.isNotBlank() }?.let { append(it) }
                    anime.latestEpisode?.takeIf { it.isNotBlank() }?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                }.ifBlank { "Anime" },
                fontSize = 11.sp,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnimeScheduleContent(
    state: AnimeUiState,
    isDark: Boolean,
    cardBg: Color,
    labelColor: Color,
    secondary: Color,
    onRetry: () -> Unit,
    onAnimeClick: (String) -> Unit
) {
    if (state.isLoading && state.schedule.isEmpty()) {
        AnimeShimmerGrid(isDark)
        return
    }
    if (state.schedule.isEmpty()) {
        AnimeStateCard(
            title = if (state.error != null) "Gagal memuat jadwal" else "Jadwal belum tersedia",
            message = state.error ?: "Belum ada jadwal rilis untuk minggu ini.",
            onRetry = onRetry,
            accent = IOSBlue,
            secondary = secondary
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        state.schedule.forEach { (day, list) ->
            item(key = "day_$day") {
                Text(
                    text = day,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondary,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                )
            }
            items(list.size, key = { idx -> "day_${day}_${list[idx].link ?: idx}" }) { idx ->
                val anime = list[idx]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .clickable { anime.link?.let(onAnimeClick) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(anime.image)
                            .crossfade(200)
                            .build(),
                        contentDescription = anime.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(56.dp)
                            .height(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = anime.title ?: "Anime",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = labelColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = anime.latestEpisode ?: anime.type ?: "",
                            fontSize = 12.sp,
                            color = secondary,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeStateCard(
    title: String,
    message: String,
    onRetry: (() -> Unit)?,
    accent: Color,
    secondary: Color
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
                .size(64.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.VideocamOff, null, tint = accent, modifier = Modifier.size(28.dp))
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
        if (onRetry != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .clickable { onRetry() }
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text("Coba Lagi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnimeShimmerGrid(isDark: Boolean) {
    val transition = rememberInfiniteTransition(label = "anime_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anime_shimmer_alpha"
    )
    val block = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(6) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(block.copy(alpha = alpha))
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(block.copy(alpha = alpha))
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.45f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(block.copy(alpha = alpha))
                )
            }
        }
    }
}
