package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.DonghuaItem
import com.jayra.app.data.DonghuaSource

/**
 * Home fitur Donghua (katalog Donghua + Donghub dalam satu fitur).
 */
@Composable
fun DonghuaHomeScreen(
    onItemClick: (DonghuaSource, String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: DonghuaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val home = state.home

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Donghua",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Rounded.Search, contentDescription = "Cari donghua")
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DonghuaSource.entries.forEach { source ->
                FilterChip(
                    selected = state.source == source,
                    onClick = { viewModel.setSource(source) },
                    label = { Text(source.label) }
                )
            }
        }

        when {
            state.isLoading && home == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.homeError != null && home == null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.homeError ?: "Gagal memuat katalog",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.retry() }) { Text("Coba Lagi") }
                }
            }

            home == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    val hero = home.hero.firstOrNull()
                    if (hero != null) {
                        item("hero") {
                            DonghuaHeroBanner(hero) { onItemClick(it.source, it.slug) }
                        }
                    }

                    if (state.isLoading) {
                        item("loading") {
                            LinearProgressIndicator(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (home.latest.isNotEmpty()) {
                        item("latest-title") {
                            DonghuaSectionTitle(
                                if (state.source == DonghuaSource.DONGHUA) "Terbaru" else "Update Terbaru"
                            )
                        }
                        item("latest-row") {
                            DonghuaRow(home.latest) { onItemClick(it.source, it.slug) }
                        }
                    }

                    if (home.ongoing.isNotEmpty()) {
                        item("ongoing-title") {
                            DonghuaSectionTitle(
                                if (state.source == DonghuaSource.DONGHUA) "Sedang Tayang" else "Populer"
                            )
                        }
                        item("ongoing-row") {
                            DonghuaRow(home.ongoing) { onItemClick(it.source, it.slug) }
                        }
                    }

                    if (home.completed.isNotEmpty()) {
                        item("completed-title") {
                            DonghuaSectionTitle("Selesai")
                        }
                        item("completed-row") {
                            DonghuaRow(home.completed) { onItemClick(it.source, it.slug) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonghuaRow(
    items: List<DonghuaItem>,
    onClick: (DonghuaItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { "${it.source}-${it.slug}-${it.title}" }) { item ->
            DonghuaPosterCard(item = item, onClick = onClick)
        }
    }
}

@Composable
private fun DonghuaHeroBanner(item: DonghuaItem, onClick: (DonghuaItem) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick(item) }
    ) {
        DonghuaPosterImage(
            url = item.poster,
            title = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = listOfNotNull(item.status, item.type, item.latestEpisode)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
