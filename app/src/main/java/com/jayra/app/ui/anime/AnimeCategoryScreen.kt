package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.jayra.app.data.AnimeKompiItem
import com.jayra.app.ui.theme.JayraTheme

// ============================================================================
// COLOR CONSTANTS
// ============================================================================

private val NetflixBlack = Color(0xFF000000)
private val NetflixCardGray = Color(0xFF262626)
private val NetflixWhite = Color(0xFFFFFFFF)
private val NetflixTextPrimary = Color(0xFFFFFFFF)
private val NetflixTextSecondary = Color(0xFFB3B3B3)
private val NetflixTextMetadata = Color(0xFF8C8C8C)
private val NetflixRed = Color(0xFFE50914)

// ============================================================================
// CATEGORY SCREEN (Grid view for Batch, Movie, Live-Action, etc.)
// ============================================================================

@Composable
fun AnimeCategoryScreen(
    categoryType: String, // "batch", "movie", "live-action", "donghua", "tokusatsu", "upcoming", "genre", "season", "studio", "status", "type", "order"
    slug: String = "",
    title: String,
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.categoryState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(categoryType + slug) {
        viewModel.loadCategory(categoryType, slug = slug.ifBlank { null }, title = title)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            CategoryHeader(
                title = title,
                onBackClick = onBackClick
            )

            // Content
            when {
                state.isLoading && state.items.isEmpty() -> {
                    CategorySkeletonLoading()
                }
                state.error != null && state.items.isEmpty() -> {
                    CategoryErrorState(
                        error = state.error!!,
                        onRetry = { viewModel.loadCategory(categoryType, slug = slug.ifBlank { null }, page = 1, title = title) }
                    )
                }
                else -> {
                    CategoryGrid(
                        items = state.items,
                        categoryType = categoryType,
                        onItemClick = onAnimeClick,
                        onLoadMore = {
                            if (state.hasMore) {
                                viewModel.loadCategory(categoryType, slug = slug.ifBlank { null }, page = state.currentPage + 1, title = title)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// CATEGORY HEADER
// ============================================================================

@Composable
fun CategoryHeader(
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

// ============================================================================
// CATEGORY GRID
// ============================================================================

@Composable
fun CategoryGrid(
    items: List<AnimeKompiItem>,
    categoryType: String,
    onItemClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grid
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    CategoryGridCard(
                        item = item,
                        categoryType = categoryType,
                        onClick = { item.slug?.let(onItemClick) }
                    )
                }
                // Load more trigger
                item {
                    if (items.isNotEmpty()) {
                        LaunchedEffect(Unit) { onLoadMore() }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGridCard(
    item: AnimeKompiItem,
    categoryType: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Poster with badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Category-specific badges
            when (categoryType) {
                "batch" -> Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Text("Batch", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixWhite,
                        modifier = Modifier.clip(RoundedCornerShape(2.dp)).background(NetflixCardGray.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
                "movie" -> Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Text("Movie", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixWhite,
                        modifier = Modifier.clip(RoundedCornerShape(2.dp)).background(NetflixRed).padding(horizontal = 6.dp, vertical = 2.dp))
                }
                "live-action" -> Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Text("Live-Action", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NetflixWhite,
                        modifier = Modifier.clip(RoundedCornerShape(2.dp)).background(NetflixCardGray.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
                "donghua" -> item.episode?.let { ep ->
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Text(ep, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetflixWhite,
                            modifier = Modifier.clip(RoundedCornerShape(2.dp)).background(NetflixBlack.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                "upcoming" -> Box(
                    modifier = Modifier.fillMaxSize().background(NetflixBlack.copy(alpha = 0.4f)).clip(RoundedCornerShape(6.dp))
                ) {
                    Text("Coming Soon", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NetflixWhite, modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center))
                }
            }
        }

        // Title
        Text(
            text = item.title ?: "Unknown",
            fontSize = 12.sp,
            color = NetflixTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Metadata
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            item.episode?.let {
                Text(it, fontSize = 11.sp, color = NetflixTextMetadata)
            }
            item.score?.let {
                Text("★ $it", fontSize = 11.sp, color = NetflixTextMetadata)
            }
        }
    }
}

// ============================================================================
// SKELETON & ERROR
// ============================================================================

@Composable
fun CategorySkeletonLoading() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(top = 56.dp)
    ) {
        items(12) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f/3f).background(NetflixCardGray).clip(RoundedCornerShape(6.dp)))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.width(80.dp).height(12.dp).background(NetflixCardGray))
                Box(modifier = Modifier.width(60.dp).height(11.dp).background(NetflixCardGray))
            }
        }
    }
}

@Composable
fun CategoryErrorState(
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