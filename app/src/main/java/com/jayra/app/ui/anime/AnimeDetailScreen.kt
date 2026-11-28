package com.jayra.app.ui.anime

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeKompiDetailData
import com.jayra.app.data.AnimeKompiEpisodeItem
import com.jayra.app.data.AnimeKompiItem
import com.jayra.app.ui.theme.JayraTheme

// ============================================================================
// COLOR CONSTANTS - NETFLIX STYLE
// ============================================================================

private val NetflixBlack = Color(0xFF000000)
private val NetflixDarkGray = Color(0xFF181818)
private val NetflixCardGray = Color(0xFF262626)
private val NetflixWhite = Color(0xFFFFFFFF)
private val NetflixTextPrimary = Color(0xFFFFFFFF)
private val NetflixTextSecondary = Color(0xFFB3B3B3)
private val NetflixTextMetadata = Color(0xFF8C8C8C)
private val NetflixRed = Color(0xFFE50914)
private val NetflixGreen = Color(0xFF46D369)
private val NetflixButtonSecondaryBg = Color(0xFF2A2A2A)
private val NetflixDivider = Color(0xFF262626)

// ============================================================================
// MAIN DETAIL SCREEN
// ============================================================================

@Composable
fun AnimeDetailScreen(
    animeSlug: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (String) -> Unit,  // episode slug
    onBackClick: () -> Unit,
) {
    val state by viewModel.detailState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(animeSlug) {
        viewModel.loadDetail(animeSlug)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .verticalScroll(scrollState)
    ) {
        when {
            state.isLoading -> {
                DetailSkeletonLoading()
            }
            state.error != null -> {
                DetailErrorState(error = state.error!!, onRetry = { viewModel.loadDetail(animeSlug) })
            }
            state.detail != null -> {
                DetailContent(
                    detail = state.detail!!,
                    episodes = state.episodes,
                    recommendations = state.recommendations,
                    tooltip = state.tooltip,
                    onPlayEpisode = onPlayEpisode,
                    onBackClick = onBackClick
                )
            }
        }
    }
}

// ============================================================================
// DETAIL CONTENT
// ============================================================================

@Composable
fun DetailContent(
    detail: AnimeKompiDetailData,
    episodes: List<AnimeKompiEpisodeItem>,
    recommendations: List<AnimeKompiItem>,
    tooltip: com.jayra.app.data.AnimeKompiTooltipData?,
    onPlayEpisode: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val showFullSynopsis = remember { mutableStateOf(false) }
    val selectedTab = remember { mutableIntStateOf(0) } // 0=Episodes, 1=More Like This, 2=Details

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // BACKDROP SECTION
        item {
            DetailBackdrop(
                detail = detail,
                onBackClick = onBackClick,
                onShareClick = { /* TODO */ },
                onBookmarkClick = { /* TODO */ }
            )
        }

        // MAIN INFO SECTION
        item {
            DetailMainInfo(
                detail = detail,
                episodes = episodes,
                showFullSynopsis = showFullSynopsis,
                onPlayClick = { firstEpisodeSlug ->
                    firstEpisodeSlug?.let(onPlayEpisode)
                },
                onMyListClick = { /* TODO */ },
                onShareClick = { /* TODO */ },
                onDownloadClick = { /* TODO */ }
            )
        }

        // TABS
        item {
            DetailTabs(
                selectedTab = selectedTab.value,
                onTabClick = { index -> selectedTab.value = index }
            )
        }

        // TAB CONTENT
        when (selectedTab.value) {
            0 -> {
                // EPISODES TAB
                item {
                    EpisodesSection(
                        episodes = episodes,
                        detail = detail,
                        onEpisodeClick = onPlayEpisode
                    )
                }
            }
            1 -> {
                // MORE LIKE THIS TAB
                if (recommendations.isNotEmpty()) {
                    item {
                        MoreLikeThisSection(
                            title = "More Like This",
                            items = recommendations,
                            onItemClick = { /* TODO: Navigate to detail */ }
                        )
                    }
                }
            }
            2 -> {
                // DETAILS TAB
                item {
                    DetailsSection(detail = detail)
                }
            }
        }
    }
}

// ============================================================================
// BACKDROP SECTION
// ============================================================================

@Composable
fun DetailBackdrop(
    detail: AnimeKompiDetailData,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        // Backdrop image
        AsyncImage(
            model = detail.backdrop ?: detail.poster,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(0.dp)),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            NetflixBlack.copy(alpha = 0.3f),
                            NetflixBlack.copy(alpha = 0.7f),
                            NetflixBlack
                        ),
                        stops = listOf(0f, 0.3f, 0.6f, 0.85f, 1f)
                    )
                )
        )

        // Top buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NetflixWhite,
                    modifier = Modifier
                        .size(24.dp)
                        .background(NetflixBlack.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                        .padding(8.dp)
                )
            }

            // Share + Bookmark
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NetflixWhite,
                        modifier = Modifier
                            .size(24.dp)
                            .background(NetflixBlack.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    )
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmark",
                        tint = NetflixWhite,
                        modifier = Modifier
                            .size(24.dp)
                            .background(NetflixBlack.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// MAIN INFO SECTION
// ============================================================================

@Composable
fun DetailMainInfo(
    detail: AnimeKompiDetailData,
    episodes: List<AnimeKompiEpisodeItem>,
    showFullSynopsis: MutableState<Boolean>,
    onPlayClick: (String?) -> Unit,
    onMyListClick: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Title
        Text(
            text = detail.title ?: "Unknown",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = NetflixWhite,
            lineHeight = 30.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // Metadata Row 1
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            val meta1 = buildList {
                detail.year?.let { add(it) }
                detail.totalEpisode?.let { add("$it Episodes") }
                detail.rating?.let { add(it) } // Age rating like TV-14
            }
            Text(
                text = meta1.joinToString(" • "),
                fontSize = 12.sp,
                color = NetflixTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Metadata Row 2
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            val meta2 = buildList {
                detail.sub?.let { add("Sub: $it") }
                detail.dub?.let { add("Dub: $it") }
                detail.genres?.take(3)?.joinToString(" • ")?.let { add(it) }
            }
            Text(
                text = meta2.joinToString(" • "),
                fontSize = 12.sp,
                color = NetflixTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Top 10 badge (if applicable)
        // TODO: Check if in top 10 from API

        // Category chips
        if (detail.type?.isNotBlank() == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                DetailCategoryChip(label = detail.type!!)
            }
        }

        // Synopsis
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = detail.synopsis ?: "Tidak ada sinopsis tersedia.",
                fontSize = 14.sp,
                color = NetflixWhite,
                lineHeight = 20.sp,
                maxLines = if (showFullSynopsis.value) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )
            if ((detail.synopsis?.length ?: 0) > 200) {
                Text(
                    text = if (showFullSynopsis.value) "Less" else "More",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NetflixTextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { showFullSynopsis.value = !showFullSynopsis.value }
                )
            }
        }

        // Cast / Studio
        if (detail.studios?.isNotEmpty() == true || detail.producers?.isNotEmpty() == true) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                val castInfo = buildList {
                    detail.studios?.joinToString(", ")?.let { add("Studio: $it") }
                    detail.producers?.joinToString(", ")?.let { add("Producer: $it") }
                }
                Text(
                    text = castInfo.joinToString(" • "),
                    fontSize = 12.sp,
                    color = NetflixTextMetadata,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // PLAY BUTTON - Full width
        val firstEpisodeSlug = episodes.firstOrNull()?.slug
        val isUpcoming = detail.status?.lowercase() == "upcoming"

        Button(
            onClick = if (isUpcoming) null else { onPlayClick(firstEpisodeSlug) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 24.dp),
            enabled = !isUpcoming,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isUpcoming) NetflixCardGray else NetflixWhite,
                contentColor = if (isUpcoming) NetflixTextMetadata else NetflixBlack
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (isUpcoming) NetflixTextMetadata else NetflixBlack, modifier = Modifier.size(20.dp))
                Text(
                    text = if (isUpcoming) "Coming Soon" else "Play",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUpcoming) NetflixTextMetadata else NetflixBlack
                )
            }
        }

        // Episode info under play button
        if (!isUpcoming && episodes.isNotEmpty()) {
            val firstEp = episodes.first()
            Text(
                text = "S1:E${firstEp.episode} • ${firstEp.title ?: "Episode ${firstEp.episode}"}",
                fontSize = 12.sp,
                color = NetflixTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        // Secondary buttons row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            // My List
            Button(
                onClick = onMyListClick,
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetflixButtonSecondaryBg,
                    contentColor = NetflixWhite
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NetflixWhite, modifier = Modifier.size(18.dp))
                    Text("My List", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NetflixWhite)
                }
            }

            // Share
            Button(
                onClick = onShareClick,
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetflixButtonSecondaryBg,
                    contentColor = NetflixWhite
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = NetflixWhite, modifier = Modifier.size(18.dp))
                    Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NetflixWhite)
                }
            }

            // Download (if available)
            // TODO: Check if download feature exists
            Button(
                onClick = onDownloadClick,
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetflixButtonSecondaryBg,
                    contentColor = NetflixWhite
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = NetflixWhite, modifier = Modifier.size(18.dp))
                    Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NetflixWhite)
                }
            }
        }

        // Divider
        androidx.compose.material3.Divider(
            color = NetflixDivider,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun DetailCategoryChip(label: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = NetflixWhite,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(NetflixCardGray)
            .clip(RoundedCornerShape(4.dp))
    )
}

// ============================================================================
// TABS
// ============================================================================

@Composable
fun DetailTabs(
    selectedTab: Int,
    onTabClick: (Int) -> Unit
) {
    val tabs = listOf("Episodes", "More Like This", "Details")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) NetflixWhite else NetflixTextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { onTabClick(index) }
                    .padding(horizontal = 8.dp)
                    .then(
                        if (isSelected) Modifier.align(Alignment.BottomCenter) else Modifier
                    )
            )
            if (isSelected) {
                // Underline indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(NetflixWhite)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ============================================================================
// EPISODES SECTION
// ============================================================================

@Composable
fun EpisodesSection(
    episodes: List<AnimeKompiEpisodeItem>,
    detail: AnimeKompiDetailData,
    onEpisodeClick: (String) -> Unit
) {
    val sortAscending = remember { mutableStateOf(true) }
    val searchQuery = remember { mutableStateOf("") }
    val showSearch = remember { mutableStateOf(false) }
    val selectedSeason = remember { mutableStateOf<String?>(null) }

    // Filter episodes based on search
    val filteredEpisodes = episodes.filter { ep ->
        (searchQuery.value.isBlank() || ep.title?.lowercase().contains(searchQuery.value.lowercase()) == true)
    }.sortedBy { it.episode?.toIntOrNull() ?: 0 }.apply {
        if (!sortAscending.value) reverse()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Episodes header with season dropdown, sort, search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Episodes", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)

            // Season dropdown (if multiple seasons)
            // TODO: Implement season dropdown from API

            // Sort button
            Button(
                onClick = { sortAscending.value = !sortAscending.value },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetflixCardGray,
                    contentColor = NetflixWhite
                ),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, tint = NetflixWhite, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (sortAscending.value) "1 - ${episodes.size}" : "${episodes.size} - 1",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NetflixWhite
                    )
                }
            }

            // Search button
            IconButton(onClick = { showSearch.value = !showSearch.value }) {
                Icon(Icons.Default.Search, contentDescription = "Search episodes", tint = NetflixWhite, modifier = Modifier.size(20.dp).padding(8.dp))
            }
        }

        // Search bar
        if (showSearch.value) {
            TextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                placeholder = { Text("Search episodes...", color = NetflixTextMetadata) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(NetflixCardGray)
                    .clip(RoundedCornerShape(6.dp))
                    .height(44.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = NetflixCardGray,
                    unfocusedContainerColor = NetflixCardGray,
                    focusedContainerColor = NetflixCardGray,
                    textColor = NetflixWhite,
                    placeholderColor = NetflixTextMetadata,
                    cursorColor = NetflixRed
                ),
                keyboardActions = androidx.compose.ui.window.KeyboardActions(
                    onDone = { showSearch.value = false }
                )
            )
        }

        // Episodes list
        if (filteredEpisodes.isEmpty()) {
            EmptyEpisodesState()
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(filteredEpisodes) { episode ->
                    EpisodeItem(
                        episode = episode,
                        isCurrent = false, // TODO: Track current playing
                        progress = 0f, // TODO: Get from local history
                        onClick = { episode.slug?.let(onEpisodeClick) },
                        onLongClick = { /* TODO: Show bottom sheet */ }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyEpisodesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Belum ada episode", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
        Spacer(Modifier.height(4.dp))
        Text("Episode akan muncul di sini", fontSize = 13.sp, color = NetflixTextSecondary)
    }
}

// ============================================================================
// EPISODE ITEM
// ============================================================================

@Composable
fun EpisodeItem(
    episode: AnimeKompiEpisodeItem,
    isCurrent: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isWatched = progress >= 0.95f
    val isWatching = progress > 0f && progress < 0.95f

    Surface(
        color = if (isCurrent) NetflixDarkGray else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .combinedClickable(
                onLongClick = { onLongClick() },
                onClick = { onClick() }
            )
            .then(if (isCurrent) Modifier.border(3.dp, NetflixRed, RoundedCornerShape(6.dp)) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(78.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    model = episode.thumbnail,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholderSkeleton(),
                    error = errorImage()
                )

                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NetflixBlack.copy(alpha = 0.6f))
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Watched", tint = NetflixGreen, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp))
                    }
                } else {
                    // Play overlay
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = NetflixWhite,
                        modifier = Modifier
                            .size(24.dp)
                            .background(NetflixBlack.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                            .padding(16.dp)
                            .align(Alignment.Center)
                    )
                }

                if (episode.isUpcoming == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NetflixBlack.copy(alpha = 0.6f))
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("Coming Soon", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
                            episode.releaseDate?.let { date ->
                                Text(date, fontSize = 10.sp, color = NetflixTextSecondary)
                            }
                        }
                    }
                }
            }

            // Episode info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .padding(top = 4.dp)
            ) {
                // Episode number and title
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${episode.episode}. ${episode.title ?: "Episode ${episode.episode}"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = NetflixWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    episode.duration?.let { dur ->
                        Text(dur, fontSize = 12.sp, color = NetflixTextMetadata)
                    }
                }

                // Synopsis
                episode.synopsis?.let { synopsis ->
                    if (synopsis.isNotBlank()) {
                        Text(
                            text = synopsis,
                            fontSize = 13.sp,
                            color = NetflixTextSecondary,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Progress bar if partially watched
                if (isWatching) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(NetflixProgressTrack)
                                .clip(RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width({ (this@Box.maxWidth * progress).toInt() })
                                    .background(NetflixRed)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                        Text(
                            text = "${(progress * 100).toInt()}% watched",
                            fontSize = 11.sp,
                            color = NetflixTextMetadata,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // NEW badge
                if (episode.isNew == true) {
                    Text(
                        text = "NEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NetflixWhite,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .background(NetflixRed)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }

                // Release date
                episode.releaseDate?.let { date ->
                    Text(
                        text = date,
                        fontSize = 12.sp,
                        color = NetflixTextMetadata,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// MORE LIKE THIS SECTION
// ============================================================================

@Composable
fun MoreLikeThisSection(
    title: String,
    items: List<AnimeKompiItem>,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = NetflixWhite,
            modifier = Modifier.padding(top = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                MoreLikeThisCard(item = item, onClick = { item.slug?.let(onItemClick) })
            }
        }
    }
}

@Composable
fun MoreLikeThisCard(
    item: AnimeKompiItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.poster,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
            placeholder = placeholderSkeleton(),
            error = errorImage()
        )
        Text(
            text = item.title ?: "Unknown",
            fontSize = 12.sp,
            color = NetflixTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            // Match percentage if available
            // TODO: Get from API
            item.score?.let { score ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$score%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NetflixGreen)
                    Text("Match", fontSize = 11.sp, color = NetflixTextMetadata)
                }
            }
        }
    }
}

// ============================================================================
// DETAILS SECTION
// ============================================================================

@Composable
fun DetailsSection(detail: AnimeKompiDetailData) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        DetailRow("Judul", detail.title)
        detail.alternativeTitle?.let { DetailRow("Judul Alternatif", it) }
        detail.year?.let { DetailRow("Tahun", it) }
        detail.status?.let { DetailRow("Status", it) }
        detail.totalEpisode?.let { DetailRow("Total Episode", it) }
        detail.rating?.let { DetailRow("Rating Umur", it) }
        detail.genres?.joinToString(", ")?.let { DetailRow("Genre", it) }
        detail.studios?.joinToString(", ")?.let { DetailRow("Studio", it) }
        detail.season?.let { DetailRow("Musim", it) }
        detail.type?.let { DetailRow("Tipe", it) }
        detail.duration?.let { DetailRow("Durasi", it) }
        detail.releaseDate?.let { DetailRow("Tanggal Rilis", it) }
        detail.synopsis?.let { DetailRow("Sinopsis", it, isLong = true) }
    }
}

@Composable
fun DetailRow(label: String, value: String, isLong: Boolean = false) {
    if (value.isNotBlank()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (isLong) {
                Text(label, fontSize = 13.sp, color = NetflixTextMetadata)
                Spacer(Modifier.height(4.dp))
                Text(value, fontSize = 13.sp, color = NetflixWhite)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, fontSize = 13.sp, color = NetflixTextMetadata)
                    Text(value, fontSize = 13.sp, color = NetflixWhite, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 16.dp))
                }
            }
            androidx.compose.material3.Divider(color = NetflixDivider, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ============================================================================
// SKELETON LOADING
// ============================================================================

@Composable
fun DetailSkeletonLoading() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Backdrop skeleton
        Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(NetflixCardGray))
        Spacer(Modifier.height(16.dp))
        // Title skeleton
        Box(modifier = Modifier.width(200.dp).height(32.dp).background(NetflixCardGray))
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.width(300.dp).height(16.dp).background(NetflixCardGray))
        Spacer(Modifier.height(16.dp))
        // Synopsis skeleton
        repeat(4) { Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(NetflixCardGray).padding(vertical = 4.dp)) }
        Spacer(Modifier.height(24.dp))
        // Play button skeleton
        Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(NetflixCardGray).clip(RoundedCornerShape(4.dp)))
        Spacer(Modifier.height(12.dp))
        // Secondary buttons skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { Box(modifier = Modifier.weight(1f).height(40.dp).background(NetflixCardGray).clip(RoundedCornerShape(4.dp))) }
        }
    }
}

// ============================================================================
// ERROR STATE
// ============================================================================

@Composable
fun DetailErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Gagal memuat detail", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
        Spacer(Modifier.height(6.dp))
        Text(error, fontSize = 13.sp, color = NetflixTextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) { Text("Coba Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}