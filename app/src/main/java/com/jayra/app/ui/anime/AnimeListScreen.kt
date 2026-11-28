package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeKompiGenreItem
import com.jayra.app.data.AnimeKompiSeasonItem
import com.jayra.app.data.AnimeKompiStudioItem
import com.jayra.app.data.AnimeKompiStatusItem
import com.jayra.app.data.AnimeKompiTypeItem
import com.jayra.app.data.AnimeKompiOrderItem
import com.jayra.app.data.AnimeKompiAzData
import com.jayra.app.data.AnimeKompiItem
import com.jayra.app.ui.theme.JayraTheme

// ============================================================================
// COLOR CONSTANTS
// ============================================================================

private val NetflixBlack = Color(0xFF000000)
private val NetflixCardGray = Color(0xFF262626)
private val NetflixDarkGray = Color(0xFF181818)
private val NetflixWhite = Color(0xFFFFFFFF)
private val NetflixTextPrimary = Color(0xFFFFFFFF)
private val NetflixTextSecondary = Color(0xFFB3B3B3)
private val NetflixTextMetadata = Color(0xFF8C8C8C)
private val NetflixRed = Color(0xFFE50914)

// ============================================================================
// LIST SCREEN (Genres, Seasons, Studios, Status, Types, Orders, A-Z)
// ============================================================================

@Composable
fun AnimeListScreen(
    listType: String, // "genres", "seasons", "studios", "status", "types", "orders", "az"
    viewModel: AnimeViewModel = viewModel(),
    onItemClick: (String, String) -> Unit, // (slug, title)
    onBackClick: () -> Unit
) {
    val state by viewModel.listState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadListMetadata()
        if (listType == "az") {
            viewModel.loadAzList()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            ListHeader(
                title = getListTitle(listType),
                onBackClick = onBackClick
            )

            // Content
            when {
                state.isLoading -> {
                    ListSkeletonLoading(listType)
                }
                state.error != null -> {
                    ListErrorState(error = state.error!!, onRetry = { viewModel.loadListMetadata() })
                }
                else -> {
                    when (listType) {
                        "genres" -> GenresList(items = state.genres, onItemClick = onItemClick)
                        "seasons" -> SeasonsList(items = state.seasons, onItemClick = onItemClick)
                        "studios" -> StudiosList(items = state.studios, onItemClick = onItemClick)
                        "status" -> StatusList(items = state.statuses, onItemClick = onItemClick)
                        "types" -> TypesList(items = state.types, onItemClick = onItemClick)
                        "orders" -> OrdersList(items = state.orders, onItemClick = onItemClick)
                        "az" -> AzList(azData = state.azData, onItemClick = onItemClick)
                        else -> ListErrorState(error = "Tipe daftar tidak dikenal", onRetry = {})
                    }
                }
            }
        }
    }
}

// ============================================================================
// LIST HEADER
// ============================================================================

@Composable
fun ListHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Surface(
        color = NetflixBlack,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NetflixWhite,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

fun getListTitle(listType: String): String {
    return when (listType) {
        "genres" -> "Genre Anime"
        "seasons" -> "Musim Anime"
        "studios" -> "Studio Anime"
        "status" -> "Status Anime"
        "types" -> "Tipe Anime"
        "orders" -> "Urutkan Anime"
        "az" -> "Semua Anime A-Z"
        else -> "Daftar Anime"
    }
}

// ============================================================================
// GENRES LIST (2-column grid)
// ============================================================================

@Composable
fun GenresList(
    items: List<AnimeKompiGenreItem>,
    onItemClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pilih Genre", fontSize = 13.sp, color = NetflixTextSecondary)
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { genre ->
                    GenreCard(genre = genre, onClick = { genre.slug?.let { onItemClick(it, genre.name ?: "") } })
                }
            }
        }
    }
}

@Composable
fun GenreCard(
    genre: AnimeKompiGenreItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        color = NetflixDarkGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Genre poster/icon
            if (genre.poster?.isNotBlank() == true) {
                AsyncImage(
                    model = genre.poster,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(50.dp).background(NetflixCardGray).clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tag, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(genre.name ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, maxLines = 1)
                genre.count?.let { count ->
                    Text("$count anime", fontSize = 12.sp, color = NetflixTextMetadata)
                }
            }
        }
    }
}

// ============================================================================
// SEASONS LIST
// ============================================================================

@Composable
fun SeasonsList(
    items: List<AnimeKompiSeasonItem>,
    onItemClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pilih Musim", fontSize = 13.sp, color = NetflixTextSecondary)
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { season ->
                    SeasonCard(season = season, onClick = { season.slug?.let { onItemClick(it, season.name ?: "") } })
                }
            }
        }
    }
}

@Composable
fun SeasonCard(
    season: AnimeKompiSeasonItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        color = NetflixDarkGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (season.poster?.isNotBlank() == true) {
                AsyncImage(
                    model = season.poster,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(50.dp).background(NetflixCardGray).clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(season.season?.substring(0, 1) ?: "?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NetflixRed)
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(season.name ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, maxLines = 1)
                season.count?.let { count ->
                    Text("$count anime", fontSize = 12.sp, color = NetflixTextMetadata)
                }
            }
        }
    }
}

// ============================================================================
// STUDIOS LIST
// ============================================================================

@Composable
fun StudiosList(
    items: List<AnimeKompiStudioItem>,
    onItemClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pilih Studio", fontSize = 13.sp, color = NetflixTextSecondary)
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { studio ->
                    StudioCard(studio = studio, onClick = { studio.slug?.let { onItemClick(it, studio.name ?: "") } })
                }
            }
        }
    }
}

@Composable
fun StudioCard(
    studio: AnimeKompiStudioItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        color = NetflixDarkGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (studio.poster?.isNotBlank() == true) {
                AsyncImage(
                    model = studio.poster,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(50.dp).background(NetflixCardGray).clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(studio.name ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, maxLines = 1)
                studio.count?.let { count ->
                    Text("$count anime", fontSize = 12.sp, color = NetflixTextMetadata)
                }
            }
        }
    }
}

// ============================================================================
// STATUS LIST
// ============================================================================

@Composable
fun StatusList(
    items: List<AnimeKompiStatusItem>,
    onItemClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pilih Status", fontSize = 13.sp, color = NetflixTextSecondary)
        }
        items(items) { status ->
            StatusCard(status = status, onClick = { status.slug?.let { onItemClick(it, status.name ?: "") } })
        }
    }
}

@Composable
fun StatusCard(
    status: AnimeKompiStatusItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() },
        color = NetflixDarkGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Status icon
                Box(
                    modifier = Modifier.size(40.dp).background(NetflixCardGray).clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (status.slug) {
                        "ongoing" -> Text("▶", fontSize = 18.sp, color = NetflixGreen)
                        "completed" -> Text("✓", fontSize = 18.sp, color = NetflixWhite)
                        "upcoming" -> Text("⏰", fontSize = 18.sp, color = NetflixRed)
                        else -> Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(status.name ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NetflixWhite)
                    status.count?.let { Text("$it anime", fontSize = 12.sp, color = NetflixTextMetadata) }
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
        }
    }
}

// ============================================================================
// TYPES LIST
// ============================================================================

@Composable
fun TypesList(
    items: List<AnimeKompiTypeItem>,
    onItemClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pilih Tipe", fontSize = 13.sp, color = NetflixTextSecondary)
        }
        items(items) { type ->
            TypeCard(type = type, onClick = { type.slug?.let { onItemClick(it, type.name ?: "") } })
        }
    }
}

@Composable
fun TypeCard(
    type: AnimeKompiTypeItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() },
        color = NetflixDarkGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(type.name ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NetflixWhite)
            type.count?.let { Text("$it anime", fontSize = 12.sp, color = NetflixTextMetadata) }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
        }
    }
}

// ============================================================================
// ORDERS LIST
// ============================================================================

@Composable
fun OrdersList(
    items: List<AnimeKompiOrderItem>,
    onItemClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pilih Urutan", fontSize = 13.sp, color = NetflixTextSecondary)
        }
        items(items) { order ->
            OrderCard(order = order, onClick = { order.slug?.let { onItemClick(it, order.name ?: "") } })
        }
    }
}

@Composable
fun OrderCard(
    order: AnimeKompiOrderItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() },
        color = NetflixDarkGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(order.name ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NetflixWhite)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(24.dp))
        }
    }
}

// ============================================================================
// A-Z LIST
// ============================================================================

@Composable
fun AzList(
    azData: AnimeKompiAzData?,
    onItemClick: (String, String) -> Unit
) {
    val letters = azData?.az?.keys?.toList()?.sorted() ?: ('A'..'Z').map { it.toString() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Letter index bar
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(letters) { letter ->
                    Text(
                        text = letter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NetflixTextSecondary,
                        modifier = Modifier
                            .width(32.dp)
                            .height(32.dp)
                            .clickable { /* TODO: Scroll to section */ }
                            .background(NetflixCardGray, RoundedCornerShape(4.dp))
                            .wrapContentSize(Alignment.Center)
                    )
                }
            }
        }

        // Sections for each letter
        letters.forEach { letter ->
            val items = azData?.az?.get(letter) ?: emptyList()
            if (items.isNotEmpty()) {
                item {
                    Text(letter, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items) { anime ->
                            AzGridCard(item = anime, onClick = { anime.slug?.let { onItemClick(it, anime.title ?: "") } })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AzGridCard(
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
            contentScale = ContentScale.Crop
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
            item.episode?.let { Text(it, fontSize = 11.sp, color = NetflixTextMetadata) }
            item.score?.let { Text("★ $it", fontSize = 11.sp, color = NetflixTextMetadata) }
        }
    }
}

// ============================================================================
// SKELETON & ERROR
// ============================================================================

@Composable
fun ListSkeletonLoading(listType: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp, horizontal = 16.dp, vertical = 16.dp)
    ) {
        when (listType) {
            "genres", "seasons", "studios" -> {
                repeat(6) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(NetflixCardGray).clip(RoundedCornerShape(8.dp)).padding(top = 12.dp))
                }
            }
            "status", "types", "orders" -> {
                repeat(8) {
                    Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(NetflixCardGray).clip(RoundedCornerShape(8.dp)).padding(top = 12.dp))
                }
            }
            "az" -> {
                repeat(5) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(22.dp).background(NetflixCardGray).padding(top = 16.dp))
                        repeat(3) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f/3f).background(NetflixCardGray).clip(RoundedCornerShape(6.dp)).padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListErrorState(
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
        Text("Gagal memuat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
        Spacer(Modifier.height(6.dp))
        Text(error, fontSize = 13.sp, color = NetflixTextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) { Text("Coba Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}