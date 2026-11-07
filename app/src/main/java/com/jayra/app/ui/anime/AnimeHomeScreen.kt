package com.jayra.app.ui.anime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.AnimeSearchItem

@Composable
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Text("Sedang Tayang", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.ongoing) { anime ->
                        AnimePosterCard(anime) { onAnimeClick(it) }
                    }
                }
            }
            item {
                Text("Populer", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.popular) { anime ->
                        AnimePosterCard(anime) { onAnimeClick(it) }
                    }
                }
            }
            item {
                Text("Selesai", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.completed) { anime ->
                        AnimePosterCard(anime) { onAnimeClick(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimePosterCard(anime: AnimeSearchItem, onClick: (String) -> Unit) {
    Column(Modifier.width(120.dp).clickable { onClick(anime.link ?: "") }) {
        AsyncImage(
            model = anime.image,
            contentDescription = anime.title,
            modifier = Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(4.dp))
        Text(anime.title ?: "Unknown", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Text(anime.latestEpisode ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
