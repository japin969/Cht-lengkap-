package com.jayra.app.ui.anime

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeKompiItem
import com.jayra.app.data.AnimeKompiSuggestItem
import com.jayra.app.ui.theme.JayraTheme

// ============================================================================
// COLOR CONSTANTS
// ============================================================================

private val NetflixBlack = Color(0xFF000000)
private val NetflixDarkGray = Color(0xFF181818)
private val NetflixCardGray = Color(0xFF262626)
private val NetflixWhite = Color(0xFFFFFFFF)
private val NetflixTextPrimary = Color(0xFFFFFFFF)
private val NetflixTextSecondary = Color(0xFFB3B3B3)
private val NetflixTextMetadata = Color(0xFF8C8C8C)
private val NetflixRed = Color(0xFFE50914)
private val NetflixDivider = Color(0xFF262626)

// ============================================================================
// SEARCH SCREEN
// ============================================================================

@Composable
fun AnimeSearchScreen(
    viewModel: AnimeViewModel = viewModel(),
    onBackClick: () -> Unit,
    onAnimeClick: (String) -> Unit,
) {
    val state by viewModel.searchState.collectAsState()
    val searchText = remember { mutableStateOf("") }
    val showSuggestions = remember { mutableStateOf(false) }
    val recentSearches = remember { mutableStateOf<List<String>>(emptyList()) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(Unit) {
        // TODO: Load from DataStore/SharedPreferences
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        SearchHeader(
            searchText = searchText,
            onBackClick = onBackClick,
            onSearchSubmit = { query ->
                viewModel.search(query)
                if (query.isNotBlank() && !recentSearches.value.contains(query)) {
                    recentSearches.value = (listOf(query) + recentSearches.value).take(10)
                }
                showSuggestions.value = false
            },
            onTextChange = { query ->
                searchText.value = query
                if (query.length >= 2) {
                    viewModel.searchSuggest(query)
                    showSuggestions.value = true
                } else {
                    showSuggestions.value = false
                }
            },
            focusRequester = focusRequester
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (showSuggestions.value && state.suggestions.isNotEmpty()) {
                SuggestionsDropdown(
                    suggestions = state.suggestions,
                    onItemClick = { item ->
                        item.slug?.let(onAnimeClick)
                        showSuggestions.value = false
                        searchText.value = item.title ?: ""
                    },
                    onDismiss = { showSuggestions.value = false }
                )
            }

            when {
                state.isLoading && state.results.isEmpty() -> SearchSkeletonLoading()
                state.error != null && state.results.isEmpty() -> SearchErrorState(error = state.error!!, onRetry = { searchText.value.let(viewModel::search) })
                state.results.isNotEmpty() -> SearchResultsGrid(items = state.results, onItemClick = onAnimeClick, onLoadMore = { if (state.hasMore) viewModel.search(state.query, state.currentPage + 1) })
                state.query.isNotBlank() -> SearchEmptyState(query = state.query)
                else -> RecentSearchesList(recentSearches = recentSearches.value, onItemClick = { query -> searchText.value = query; viewModel.search(query); showSuggestions.value = false }, onClearClick = { recentSearches.value = emptyList() })
            }
        }
    }
}

// ============================================================================
// SEARCH HEADER
// ============================================================================

@Composable
fun SearchHeader(
    searchText: MutableState<String>,
    onBackClick: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    onTextChange: (String) -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester
) {
    Surface(
        color = NetflixBlack,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NetflixWhite, modifier = Modifier.size(24.dp))
            }

            TextField(
                value = searchText.value,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(NetflixCardGray, RoundedCornerShape(6.dp))
                    .focusRequester(focusRequester),
                placeholder = { Text("Search anime...", color = NetflixTextMetadata, fontSize = 14.sp) },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = NetflixCardGray,
                    unfocusedContainerColor = NetflixCardGray,
                    focusedContainerColor = NetflixCardGray,
                    textColor = NetflixWhite,
                    placeholderColor = NetflixTextMetadata,
                    cursorColor = NetflixRed
                ),
                trailingIcon = {
                    if (searchText.value.isNotBlank()) {
                        IconButton(onClick = { searchText.value = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = NetflixTextMetadata, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NetflixTextMetadata, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                    }
                }
            )
        }
    }
}

// ============================================================================
// SUGGESTIONS DROPDOWN
// ============================================================================

@Composable
fun SuggestionsDropdown(
    suggestions: List<AnimeKompiSuggestItem>,
    onItemClick: (AnimeKompiSuggestItem) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NetflixBlack.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, top = 8.dp)
                .background(NetflixDarkGray, RoundedCornerShape(12.dp))
        ) {
            suggestions.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { onItemClick(item) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.poster,
                        contentDescription = null,
                        modifier = Modifier
                            .width(40.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                        Text(item.title ?: "Unknown", fontSize = 14.sp, color = NetflixWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item.type?.let { Text(it, fontSize = 12.sp, color = NetflixTextMetadata) }
                            item.episode?.let { Text(it, fontSize = 12.sp, color = NetflixTextMetadata) }
                            item.score?.let { Text("★ $it", fontSize = 12.sp, color = NetflixTextMetadata) }
                        }
                    }
                }
                if (item != suggestions.last()) {
                    Divider(color = NetflixDivider, thickness = 1.dp)
                }
            }
        }
    }
}

// ============================================================================
// SEARCH RESULTS GRID
// ============================================================================

@Composable
fun SearchResultsGrid(
    items: List<AnimeKompiItem>,
    onItemClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "${items.size} hasil ditemukan",
                fontSize = 13.sp,
                color = NetflixTextSecondary
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    SearchResultCard(item = item, onClick = { item.slug?.let(onItemClick) })
                }
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
fun SearchResultCard(
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
// RECENT SEARCHES LIST
// ============================================================================

@Composable
fun RecentSearchesList(
    recentSearches: List<String>,
    onItemClick: (String) -> Unit,
    onClearClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        if (recentSearches.isEmpty()) {
            SearchEmptyInitialState()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pencarian Terbaru", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
                TextButton(onClick = onClearClick) {
                    Text("Hapus Semua", fontSize = 13.sp, color = NetflixTextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentSearches) { query ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .background(NetflixCardGray, RoundedCornerShape(8.dp))
                            .clickable { onItemClick(query) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = NetflixTextSecondary, modifier = Modifier.size(20.dp))
                            Text(query, fontSize = 14.sp, color = NetflixWhite)
                        }
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = NetflixTextMetadata, modifier = Modifier.size(20.dp).clickable { /* TODO */ }.padding(8.dp))
                    }
                }
            }
        }
    }
}

// ============================================================================
// EMPTY STATES
// ============================================================================

@Composable
fun SearchEmptyInitialState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Cari anime favoritmu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
        Spacer(Modifier.height(8.dp))
        Text("Ketik judul, genre, atau kata kunci", fontSize = 13.sp, color = NetflixTextSecondary)
    }
}

@Composable
fun SearchEmptyState(query: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SearchOff, contentDescription = null, tint = NetflixTextMetadata, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Tidak ditemukan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
        Spacer(Modifier.height(8.dp))
        Text("Tidak ada hasil untuk \"$query\"", fontSize = 13.sp, color = NetflixTextSecondary)
        Spacer(Modifier.height(8.dp))
        Text("Coba kata kunci lain", fontSize = 13.sp, color = NetflixTextSecondary)
    }
}

// ============================================================================
// SKELETON & ERROR
// ============================================================================

@Composable
fun SearchSkeletonLoading() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(9) {
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
fun SearchErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Gagal mencari", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NetflixWhite)
        Spacer(Modifier.height(6.dp))
        Text(error, fontSize = 13.sp, color = NetflixTextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) { Text("Coba Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}