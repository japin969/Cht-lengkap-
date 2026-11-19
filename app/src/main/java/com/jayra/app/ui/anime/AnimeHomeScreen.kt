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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> {
                AnimeSkeleton()
            }

            state.error != null && state.popular.isEmpty() && state.ongoing.isEmpty() && state.completed.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Gagal memuat anime",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.error ?: "Koneksi bermasalah",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.retry() }) { Text("Coba Lagi") }
                }
            }

            state.popular.isEmpty() && state.ongoing.isEmpty() && state.completed.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Belum ada anime", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Daftar anime sedang kosong. Coba muat ulang.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.retry() }) { Text("Coba Lagi") }
                }
            }

            else -> {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (state.isFallback) item {
                        FallbackBanner(
                            message = state.error ?: "Menampilkan data contoh",
                            onRetry = { viewModel.retry() }
                        )
                    }
                    if (state.ongoing.isNotEmpty()) item {
                        Text("Sedang Tayang", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.ongoing) { anime ->
                                AnimePosterCard(anime) { onAnimeClick(it) }
                            }
                        }
                    }
                    if (state.popular.isNotEmpty()) item {
                        Text("Populer", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.popular) { anime ->
                                AnimePosterCard(anime) { onAnimeClick(it) }
                            }
                        }
                    }
                    if (state.completed.isNotEmpty()) item {
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
    }
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
