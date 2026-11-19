package com.jayra.app.ui.anime

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeSearchItem

@Composable
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val lazyListState = remember { LazyListState() }

    // Trigger load more when near end of list
    LaunchedEffect(lazyListState, state.ongoingPage, state.completedPage, state.popularPage) {
        val scrollPosition = lazyListState.firstVisibleItemIndex
        val totalItems = state.ongoing.size + state.completed.size + state.popular.size + state.schedule.values.flatten().size
        if (scrollPosition > totalItems - 5) {
            viewModel.loadMoreOngoing()
            viewModel.loadMoreCompleted()
            viewModel.loadMorePopular()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    placeholder = { Text("Cari anime...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    textStyle = TextStyle(fontSize = 16.sp),
                    leadingIcon = { Icon(Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Close, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Content
            if (searchQuery.isNotBlank()) {
                // Search results view
                when {
                    state.isSearchLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.searchError != null && state.searchResults.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Gagal mencari anime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(state.searchError ?: "Koneksi bermasalah", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { viewModel.retrySearch() }) { Text("Coba Lagi") }
                        }
                    }
                    state.searchResults.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Tidak ditemukan", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("Coba kata kunci lain", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                    else -> {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, bottom = 16.dp)) {
                            items(state.searchResults) { anime ->
                                AnimePosterCard(anime) { onAnimeClick(it) }
                            }
                        }
                    }
                }
            } else {
                // Home view with sections
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (state.isFallback) item {
                        FallbackBanner(
                            message = state.error ?: "Menampilkan data contoh",
                            onRetry = { viewModel.retry() }
                        )
                    }

                    // --- SCHEDULE (Hari ini) ---
                    if (state.schedule.isNotEmpty()) {
                        val todaySchedule = getTodaySchedule(state.schedule)
                        if (todaySchedule.isNotEmpty()) item {
                            Text("Jadwal Hari Ini", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(todaySchedule) { anime ->
                                    AnimePosterCard(anime) { onAnimeClick(it) }
                                }
                            }
                        }
                    }

                    // --- ONGOING ---
                    if (state.ongoing.isNotEmpty()) item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sedang Tayang", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            if (state.hasMoreOngoing) {
                                TextButton(onClick = { viewModel.loadMoreOngoing() }) {
                                    Text("Lainnya")
                                }
                            }
                        }
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.ongoing) { anime ->
                                AnimePosterCard(anime) { onAnimeClick(it) }
                            }
                            if (state.hasMoreOngoing && !state.isLoading) {
                                item { LoadMoreTrigger(onTrigger = { viewModel.loadMoreOngoing() }) }
                            }
                        }
                    }

                    // --- POPULAR ---
                    if (state.popular.isNotEmpty()) item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Populer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            if (state.hasMorePopular) {
                                TextButton(onClick = { viewModel.loadMorePopular() }) {
                                    Text("Lainnya")
                                }
                            }
                        }
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.popular) { anime ->
                                AnimePosterCard(anime) { onAnimeClick(it) }
                            }
                            if (state.hasMorePopular && !state.isLoading) {
                                item { LoadMoreTrigger(onTrigger = { viewModel.loadMorePopular() }) }
                            }
                        }
                    }

                    // --- COMPLETED ---
                    if (state.completed.isNotEmpty()) item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Selesai", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            if (state.hasMoreCompleted) {
                                TextButton(onClick = { viewModel.loadMoreCompleted() }) {
                                    Text("Lainnya")
                                }
                            }
                        }
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.completed) { anime ->
                                AnimePosterCard(anime) { onAnimeClick(it) }
                            }
                            if (state.hasMoreCompleted && !state.isLoading) {
                                item { LoadMoreTrigger(onTrigger = { viewModel.loadMoreCompleted() }) }
                            }
                        }
                    }

                    // --- FULL SCHEDULE (All days) ---
                    if (state.schedule.size > 1) item {
                        Text("Jadwal Lengkap", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        ScheduleSection(schedule = state.schedule) { url ->
                            onAnimeClick(url)
                        }
                    }
                }
            }
        }
    }
}

private fun getTodaySchedule(schedule: Map<String, List<AnimeSearchItem>>): List<AnimeSearchItem> {
    val days = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    val today = days[java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1]
    return schedule[today] ?: schedule.values.firstOrNull() ?: emptyList()
}

@Composable
private fun FallbackBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
private fun LoadMoreTrigger(onTrigger: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        LaunchedEffect(Unit) { onTrigger() }
        CircularProgressIndicator()
    }
}

@Composable
fun AnimePosterCard(anime: AnimeSearchItem, onClick: (String) -> Unit) {
    val link = anime.link.orEmpty()
    Column(
        Modifier
            .width(120.dp)
            .clickable(enabled = link.isNotBlank()) { onClick(link) }
    ) {
        AsyncImage(
            model = anime.image,
            contentDescription = anime.title,
            modifier = Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(4.dp))
        Text(anime.title ?: "Unknown", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Text(anime.latestEpisode ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScheduleSection(schedule: Map<String, List<AnimeSearchItem>>, onClick: (String) -> Unit) {
    val daysOrder = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    Column(Modifier.padding(horizontal = 16.dp)) {
        daysOrder.forEach { day ->
            val items = schedule[day] ?: emptyList()
            if (items.isNotEmpty()) {
                Text(day, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 0.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items) { anime ->
                        AnimePosterCard(anime) { onClick(it) }
                    }
                }
            }
        }
    }
}

/**
 * Skeleton loading (bukan spinner fullscreen) supaya layar tidak terasa "nyangkut".
 */
@Composable
private fun AnimeSkeleton() {
    val transition = rememberInfiniteTransition(label = "anime_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anime_skeleton_alpha"
    )
    val bone = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f)

    Column(Modifier.fillMaxSize()) {
        repeat(3) { section ->
            Box(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .width(120.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(bone)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(if (section == 2) 2 else 3) {
                    Column(Modifier.width(120.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bone)
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(bone)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.5f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(bone)
                        )
                    }
                }
            }
        }
    }
}