package com.jayra.app.ui.anime

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeKompiItem
import com.jayra.app.data.AnimeKompiScheduleData
import com.jayra.app.data.AnimeKompiScheduleItem
import com.jayra.app.ui.theme.JayraTheme

// ============================================================================
// COLOR CONSTANTS - NETFLIX STYLE
// ============================================================================

private val NetflixBlack = Color(0xFF000000)
private val NetflixDarkGray = Color(0xFF181818)
private val NetflixCardGray = Color(0xFF262626)
private val NetflixChipInactive = Color(0xFF262626)
private val NetflixChipActive = Color(0xFFFFFFFF)
private val NetflixWhite = Color(0xFFFFFFFF)
private val NetflixTextPrimary = Color(0xFFFFFFFF)
private val NetflixTextSecondary = Color(0xFFB3B3B3)
private val NetflixTextMetadata = Color(0xFF8C8C8C)
private val NetflixTextHint = Color(0xFF808080)
private val NetflixRed = Color(0xFFE50914)
private val NetflixGreen = Color(0xFF46D369)
private val NetflixOrange = Color(0xFFFF9800)
private val NetflixProgressTrack = Color(0xFF4D4D4D)
private val NetflixDivider = Color(0xFF262626)
private val NetflixButtonSecondaryBg = Color(0xFF2A2A2A)
private val NetflixButtonSecondaryBorder = Color(0x4DFFFFFF)
private val NetflixSkeletonStart = Color(0xFF262626)
private val NetflixSkeletonEnd = Color(0xFF333333)

// ============================================================================
// MAIN SCREEN
// ============================================================================

@Composable
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit,
    onCategoryClick: (String, String, String) -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.homeState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { viewModel.loadHome() }

    val chipOptions = remember {
        listOf(ChipOption("Semua", "all"), ChipOption("Ongoing", "ongoing"), ChipOption("Completed", "completed"), ChipOption("Popular", "popular"), ChipOption("New", "new"), ChipOption("Top Rated", "top_rated"), ChipOption("Batch", "batch"), ChipOption("Movie", "movie"), ChipOption("Live-Action", "live_action"), ChipOption("Donghua", "donghua"), ChipOption("Tokusatsu", "tokusatsu"), ChipOption("Upcoming", "upcoming"), ChipOption("Schedule", "schedule"), ChipOption("Action", "action"), ChipOption("Fantasy", "fantasy"), ChipOption("Romance", "romance"), ChipOption("Comedy", "comedy"), ChipOption("Adventure", "adventure"))
    }

    Box(Modifier.fillMaxSize().background(NetflixBlack)) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            AnimeHeader(onBackClick, onSearchClick, onFilterClick)
            ChipFilterRow(chipOptions, state.selectedChip, { chip ->
                viewModel.applyFilter(genres = if (chip.id in listOf("action", "fantasy", "romance", "comedy", "adventure")) listOf(chip.id) else emptyList(), status = if (chip.id in listOf("ongoing", "completed")) listOf(chip.id) else emptyList())
                when (chip.id) {
                    "all" -> onCategoryClick("home", "", "Semua Anime")
                    "batch" -> onCategoryClick("batch", "", "Batch Anime")
                    "movie" -> onCategoryClick("movie", "", "Anime Movie")
                    "live_action" -> onCategoryClick("live-action", "", "Live-Action")
                    "donghua" -> onCategoryClick("donghua", "", "Donghua")
                    "tokusatsu" -> onCategoryClick("tokusatsu", "", "Tokusatsu")
                    "upcoming" -> onCategoryClick("upcoming", "", "Upcoming Anime")
                    "schedule" -> onCategoryClick("schedule", "", "Jadwal Rilis Anime")
                    "ongoing", "completed" -> onCategoryClick("status", chip.id, "${chip.label} Series")
                    "popular", "new", "top_rated" -> onCategoryClick("order", chip.id, "${chip.label} Anime")
                    else -> onCategoryClick("genre", chip.id, "${chip.label} Anime")
                }
            })
            if (state.isLoading && state.heroItems.isEmpty()) HomeSkeletonLoading()
            else if (state.error != null && state.heroItems.isEmpty()) HomeErrorState(state.error!!, { viewModel.retryHome() })
            else {
                if (state.heroItems.isNotEmpty()) HeroBanner(state.heroItems, { onAnimeClick(it) }, { onAnimeClick(it) }, { /* TODO */ })
                if (state.trendingItems.isNotEmpty()) AnimeRow("Trending Now", items = state.trendingItems, onItemClick = onAnimeClick, showMetadata = true, badgeType = "trending")
                val top10 = state.trendingItems.take(10).mapIndexed { i, it -> it.copy(rank = i + 1) }
                if (top10.isNotEmpty()) Top10Row("Top 10 Anime Today", top10, onAnimeClick)
                if (state.terbaruItems.isNotEmpty()) AnimeRow("New Releases", items = state.terbaruItems, onItemClick = onAnimeClick, showMetadata = true, badgeType = "new")
                if (state.popularItems.isNotEmpty()) AnimeRow("Popular on Anime", items = state.popularItems, onItemClick = onAnimeClick, showMetadata = true, badgeType = "popular")
                if (state.batchItems.isNotEmpty()) AnimeRow(title = "Batch Anime", subtitle = "Lengkap Sekaligus", items = state.batchItems, onItemClick = onAnimeClick, onSeeAllClick = { onCategoryClick("batch", "", "Batch Anime") }, showMetadata = true, badgeType = "batch")
                if (state.movieItems.isNotEmpty()) AnimeRow(title = "Anime Movie", items = state.movieItems, onItemClick = onAnimeClick, onSeeAllClick = { onCategoryClick("movie", "", "Anime Movie") }, showMetadata = true, badgeType = "movie")
                if (state.liveActionItems.isNotEmpty()) AnimeRow(title = "Live-Action", items = state.liveActionItems, onItemClick = onAnimeClick, onSeeAllClick = { onCategoryClick("live-action", "", "Live-Action") }, showMetadata = true, badgeType = "live_action")
                if (state.donghuaItems.isNotEmpty()) AnimeRow(title = "Donghua", items = state.donghuaItems, onItemClick = onAnimeClick, onSeeAllClick = { onCategoryClick("donghua", "", "Donghua") }, showMetadata = true, badgeType = "donghua")
                if (state.tokusatsuItems.isNotEmpty()) AnimeRow(title = "Tokusatsu", items = state.tokusatsuItems, onItemClick = onAnimeClick, onSeeAllClick = { onCategoryClick("tokusatsu", "", "Tokusatsu") }, showMetadata = true, badgeType = "tokusatsu")
                if (state.upcomingItems.isNotEmpty()) AnimeRow(title = "Upcoming Anime", items = state.upcomingItems, onItemClick = onAnimeClick, onSeeAllClick = { onCategoryClick("upcoming", "", "Upcoming Anime") }, showMetadata = true, badgeType = "upcoming")
                if (state.scheduleData != null) ScheduleSection(state.scheduleData!!, onAnimeClick)
                if (state.ongoingItems.isNotEmpty()) AnimeRow("Ongoing Series", items = state.ongoingItems, onItemClick = onAnimeClick, showMetadata = true, badgeType = "ongoing")
                if (state.completedItems.isNotEmpty()) AnimeRow("Completed Series", items = state.completedItems, onItemClick = onAnimeClick, showMetadata = true, badgeType = "completed")
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

data class ChipOption(val label: String, val id: String)

// ============================================================================
// HEADER
// ============================================================================

@Composable
fun AnimeHeader(onBackClick: () -> Unit, onSearchClick: () -> Unit, onFilterClick: () -> Unit) {
    Surface(color = NetflixBlack, modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NetflixWhite, modifier = Modifier.size(24.dp).padding(8.dp)) }
                Text("ANIME", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NetflixWhite, letterSpacing = (-0.3).sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, contentDescription = "Search", tint = NetflixWhite, modifier = Modifier.size(24.dp)) }
                IconButton(onClick = onFilterClick) { Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = NetflixWhite, modifier = Modifier.size(24.dp)) }
            }
        }
    }
}

// ============================================================================
// CHIP FILTER ROW
// ============================================================================

@Composable
fun ChipFilterRow(options: List<ChipOption>, selectedChip: String, onChipClick: (ChipOption) -> Unit) {
    Surface(color = NetflixBlack, modifier = Modifier.fillMaxWidth().height(40.dp).padding(vertical = 4.dp, horizontal = 16.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(0.dp)) {
            items(options) { option ->
                val isSelected = selectedChip == option.id
                FilterChip(selected = isSelected, onClick = { onChipClick(option) }, label = { Text(option.label) }, modifier = Modifier.height(32.dp), colors = FilterChipDefaults.colors(containerColor = if (isSelected) NetflixChipActive else NetflixChipInactive, selectedContainerColor = NetflixChipActive, labelColor = if (isSelected) NetflixBlack else NetflixWhite, selectedLabelColor = NetflixBlack), shape = RoundedCornerShape(16.dp))
            }
        }
    }
}

// ============================================================================
// HERO BANNER
// ============================================================================

@Composable
fun HeroBanner(items: List<AnimeKompiItem>, onPlayClick: (String) -> Unit, onInfoClick: (String) -> Unit, onMyListClick: (String) -> Unit) {
    val currentIndex = remember { mutableStateOf(0) }
    val item = items[currentIndex.value]

    Box(modifier = Modifier.fillMaxWidth().height(460.dp)) {
        AsyncImage(model = item.backdrop ?: item.poster, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, NetflixBlack.copy(alpha = 0.3f), NetflixBlack.copy(alpha = 0.7f), NetflixBlack))))
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.Start) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.isNew == true) Box(modifier = Modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(2.dp)).background(NetflixRed).padding(horizontal = 6.dp, vertical = 4.dp), contentAlignment = Alignment.Center) { Text("NEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, letterSpacing = 0.5.sp) }
                Text("Trending Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
            }
            Text(item.title ?: "Unknown", fontSize = 32.sp, fontWeight = FontWeight.Black, color = NetflixWhite, lineHeight = 34.sp, letterSpacing = (-0.5).sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) { buildList { item.year?.let { add(it) }; item.totalEpisode?.let { add("$it Episodes") }; item.score?.let { add("★ $it") }; item.type?.let { add(it) } }.joinToString(" \u2022 ").let { Text(it, fontSize = 12.sp, color = NetflixTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
            if (item.genres?.isNotEmpty() == true) Text(item.genres!!.take(3).joinToString(" \u2022 "), fontSize = 12.sp, color = NetflixTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Button(onClick = { item.slug?.let(onPlayClick) }, modifier = Modifier.width(160.dp).height(44.dp), colors = ButtonDefaults.buttonColors(NetflixWhite, NetflixBlack), shape = RoundedCornerShape(4.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NetflixBlack, modifier = Modifier.size(20.dp)); Text("Play", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixBlack) } }
                Button(onClick = { item.slug?.let(onMyListClick) }, modifier = Modifier.size(44.dp), colors = ButtonDefaults.buttonColors(NetflixButtonSecondaryBg, NetflixWhite), shape = RoundedCornerShape(4.dp)) { Icon(Icons.Default.Add, contentDescription = "My List", tint = NetflixWhite, modifier = Modifier.size(20.dp)) }
                Button(onClick = { item.slug?.let(onInfoClick) }, modifier = Modifier.size(44.dp), colors = ButtonDefaults.buttonColors(Color.Transparent, NetflixWhite), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, NetflixWhite.copy(alpha = 0.5f))) { Icon(Icons.Default.Info, contentDescription = "Info", tint = NetflixWhite, modifier = Modifier.size(20.dp)) }
            }
        }
        if (items.size > 1) Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, end = 16.dp), horizontalArrangement = Arrangement.End) { LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { items(items.size) { index -> val isActive = currentIndex.value == index; AnimatedVisibility(visible = true) { Box(modifier = Modifier.width(if (isActive) 16.dp else 4.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (isActive) NetflixWhite else NetflixWhite.copy(alpha = 0.4f)).animateContentSize(tween(300)).fillMaxHeight()) } } } }
    }
}

// ============================================================================
// ANIME ROW
// ============================================================================

@Composable
fun AnimeRow(title: String, subtitle: String? = null, items: List<AnimeKompiItem>, onItemClick: (String) -> Unit, onSeeAllClick: (() -> Unit)? = null, showMetadata: Boolean = false, badgeType: String = "anime") {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
            onSeeAllClick?.let { Text("See All >", fontSize = 12.sp, color = NetflixTextSecondary, modifier = Modifier.fillMaxWidth().padding(end = 16.dp).clickable { it() }) }
        }
        subtitle?.let { Text(it, fontSize = 12.sp, color = NetflixTextMetadata, modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) { items(items) { AnimePosterCard(it, { it.slug?.let(onItemClick) }, badgeType, showMetadata) } }
    }
}

// ============================================================================
// TOP 10 ROW
// ============================================================================

@Composable
fun Top10Row(title: String, items: List<AnimeKompiItem>, onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NetflixWhite) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) { items(items) { val r = it.rank ?: 0; Top10PosterCard(it, r, { it.slug?.let(onItemClick) }) } }
    }
}

@Composable
fun Top10PosterCard(item: AnimeKompiItem, rank: Int, onClick: () -> Unit) {
    val isOne = rank == 1
    Box(modifier = Modifier.width(110.dp).clickable { onClick() }) {
        Box(modifier = Modifier.offset(x = (-14).dp).zIndex(0f)) { Text("$rank", fontSize = 90.sp, fontWeight = FontWeight.Black, color = if (isOne) NetflixRed else NetflixCardGray, letterSpacing = (-4).sp) }
        Column(modifier = Modifier.zIndex(1f)) { AsyncImage(model = item.poster, contentDescription = item.title, modifier = Modifier.width(110.dp).height(165.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop) }
    }
}

// ============================================================================
// ANIME POSTER CARD
// ============================================================================

@Composable
fun AnimePosterCard(item: AnimeKompiItem, onClick: () -> Unit, badgeType: String = "anime", showMetadata: Boolean = false) {
    Column(modifier = Modifier.width(120.dp).clickable { onClick() }) {
        Box(modifier = Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(6.dp))) {
            AsyncImage(model = item.poster, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            when (badgeType) {
                "new", "trending" -> if (item.isNew == true) Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(2.dp)).background(NetflixRed).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("NEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, letterSpacing = 0.5.sp) }
                "movie" -> Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(2.dp)).background(NetflixRed).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("Movie", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, letterSpacing = 0.5.sp) }
                "batch" -> Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(2.dp)).background(NetflixCardGray.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("Batch", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, letterSpacing = 0.5.sp) }
                "live_action" -> Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(2.dp)).background(NetflixCardGray.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("Live-Action", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, letterSpacing = 0.5.sp) }
                "upcoming" -> Box(modifier = Modifier.fillMaxSize().background(NetflixBlack.copy(alpha = 0.4f)).clip(RoundedCornerShape(6.dp))) { Text("Coming Soon", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) }
                "ongoing" -> Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(2.dp)).background(NetflixBlack.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(Modifier.size(8.dp).background(NetflixGreen).clip(RoundedCornerShape(4.dp))); Text("Ongoing", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixGreen) } }
                "completed" -> Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(2.dp)).background(NetflixCardGray.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("Completed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixTextMetadata) }
                "donghua" -> item.episode?.let { ep -> Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(2.dp)).background(NetflixBlack.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(ep, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixWhite) } }
            }
        }
        Text(item.title ?: "Unknown", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NetflixTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        if (showMetadata) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
            item.episode?.let { Text(it, fontSize = 11.sp, color = NetflixTextMetadata) }
            item.score?.let { Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { Icon(Icons.Default.Star, contentDescription = null, tint = NetflixOrange, modifier = Modifier.size(11.dp)); Text(it, fontSize = 11.sp, color = NetflixTextMetadata) } }
        }
    }
}

// ============================================================================
// SCHEDULE SECTION
// ============================================================================

@Composable
fun ScheduleSection(scheduleData: AnimeKompiScheduleData, onItemClick: (String) -> Unit) {
    val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val selectedDay = remember { mutableStateOf(days[0]) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text("Jadwal Rilis Anime", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.padding(horizontal = 16.dp))
        Surface(color = NetflixBlack, modifier = Modifier.fillMaxWidth().height(40.dp).padding(vertical = 8.dp, horizontal = 16.dp)) { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(days) { day -> val sel = selectedDay.value == day; FilterChip(selected = sel, onClick = { selectedDay.value = day }, label = { Text(day, fontSize = 13.sp) }, modifier = Modifier.height(32.dp), colors = FilterChipDefaults.colors(containerColor = if (sel) NetflixWhite else NetflixCardGray, labelColor = if (sel) NetflixBlack else NetflixWhite), shape = RoundedCornerShape(16.dp)) } } }
        val dayItems = scheduleData.schedule?.get(selectedDay.value) ?: emptyList()
        if (dayItems.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) { items(dayItems) { SchedulePosterCard(it, { it.slug?.let(onItemClick) }) } } else Text("Tidak ada jadwal hari ini", fontSize = 13.sp, color = NetflixTextSecondary, modifier = Modifier.fillMaxWidth().padding(16.dp, 24.dp))
    }
}

@Composable
fun SchedulePosterCard(item: AnimeKompiScheduleItem, onClick: () -> Unit) {
    Column(modifier = Modifier.width(100.dp).height(150.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(6.dp))) { AsyncImage(model = item.poster, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (item.time?.isNotBlank() == true || item.episode?.isNotBlank() == true) Text("${item.time ?: ""} ${item.episode ?: ""}".trim(), fontSize = 10.sp, color = NetflixTextMetadata, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

// ============================================================================
// CONTINUE WATCHING ROW
// ============================================================================

@Composable
fun ContinueWatchingRow(items: List<AnimeKompiItem>, onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Continue Watching", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NetflixWhite) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) { items(items) { ContinueWatchingCard(it, { it.slug?.let(onItemClick) }) } }
    }
}

@Composable
fun ContinueWatchingCard(item: AnimeKompiItem, onClick: () -> Unit) {
    val progress = 0.6f
    Column(modifier = Modifier.width(160.dp).clickable { onClick() }) {
        Box(modifier = Modifier.width(160.dp).height(90.dp).clip(RoundedCornerShape(6.dp))) {
            AsyncImage(model = item.backdrop ?: item.poster, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(NetflixBlack.copy(alpha = 0.3f)).clip(RoundedCornerShape(6.dp))) { Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = NetflixWhite, modifier = Modifier.size(32.dp).wrapContentSize(Alignment.Center).background(NetflixBlack.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(16.dp)) }
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter).background(NetflixProgressTrack).clip(RoundedCornerShape(2.dp))) { Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(NetflixRed).clip(RoundedCornerShape(2.dp))) }
        }
        Text(item.title ?: "Unknown", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NetflixTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text("S1:E${(progress * 10).toInt()} \u2022 ${(10 * (1 - progress)).toInt()} min left", fontSize = 11.sp, color = NetflixTextMetadata, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
    }
}

// ============================================================================
// SKELETON LOADING
// ============================================================================

@Composable
fun HomeSkeletonLoading() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(460.dp).background(NetflixSkeletonStart))
        repeat(15) {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Box(modifier = Modifier.width(150.dp).height(22.dp).padding(horizontal = 16.dp).background(NetflixSkeletonStart))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp)) {
                    items(5) {
                        Column {
                            Box(modifier = Modifier.width(120.dp).height(180.dp).background(NetflixSkeletonStart).clip(RoundedCornerShape(6.dp)))
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.width(80.dp).height(12.dp).background(NetflixSkeletonStart))
                            Box(Modifier.width(60.dp).height(11.dp).background(NetflixSkeletonStart))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// ERROR STATE
// ============================================================================

@Composable
fun HomeErrorState(error: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("Gagal memuat anime", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite); Spacer(Modifier.height(6.dp)); Text(error, fontSize = 13.sp, color = NetflixTextSecondary, textAlign = TextAlign.Center); Spacer(Modifier.height(14.dp)); Button(onClick = onRetry) { Text("Coba Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
}

// ============================================================================
// IMAGE PLACEHOLDER HELPERS
// ============================================================================

@Composable
fun placeholderSkeleton(): Painter = remember {
    androidx.compose.ui.res.painterResource(id = android.R.color.transparent)
}

@Composable
fun errorImage(): Painter = remember {
    androidx.compose.ui.res.painterResource(id = android.R.color.transparent)
}