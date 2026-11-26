package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.ui.video.NativeVideoPlayer

@Composable
fun AnimeDetailScreen(
    animeId: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (AnimeEpisode) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(animeId) { viewModel.loadDetail(animeId) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loadDetail(animeId) }) { Text("Coba Lagi") }
            }
        }
    } else {
        val detail = state.detail
        val episodes = state.episodes

        LazyColumn(Modifier.fillMaxSize()) {
            // Header with poster and info
            item {
                Row(Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = detail?.image,
                        contentDescription = null,
                        modifier = Modifier
                            .width(120.dp)
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = placeholder(),
                        error = errorPlaceholder()
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(detail?.title ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Rating: ${detail?.rating ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        Text("Total: ${detail?.totalEpisodes ?: 0} Episode", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(detail?.synopsis ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 6)
                        Spacer(Modifier.height(8.dp))
                        // Genres as chips
                        detail?.genres?.let { genres ->
                            if (genres.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    genres.take(5).forEach { genre ->
                                        Chip(onClick = { /* filter by genre */ }) {
                                            Text(genre, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    if (genres.size > 5) {
                                        Chip(onClick = { /* show all genres */ }) {
                                            Text("+${genres.size - 5} lagi", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Episode list
            item {
                Text("Daftar Episode (${episodes.size})", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            items(episodes) { ep ->
                val hasStreamUrl = ep.episodeUrl?.isNotBlank() == true
                ListItem(
                    headlineContent = { Text(ep.title ?: "Episode ${ep.episode}") },
                    leadingContent = {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (hasStreamUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    supportingContent = {
                        if (!hasStreamUrl) {
                            Text("Stream tidak tersedia", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasStreamUrl) {
                            if (hasStreamUrl) onPlayEpisode(ep)
                        }
                        .padding(vertical = 4.dp)
                )
                Divider()
            }
        }
    }
}

// ═══════════════════════════════════════════
// NATIVE ANIME PLAYER SCREEN (NO WEBVIEW!)
// ═══════════════════════════════════════════

// Local StreamUrl data class (replaces deleted AnimeRepository.StreamUrl)
data class StreamUrl(
    val streamUrl: String? = null,
    val alternativeLinks: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val referer: String? = null
)

@Composable
fun AnimePlayerScreen(
    streamUrl: StreamUrl?,
    animeTitle: String? = null,
    episodeTitle: String? = null,
    onBack: () -> Unit,
    onNextEpisode: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val videoUrl = streamUrl?.streamUrl
    val altLinks = streamUrl?.alternativeLinks ?: emptyList()
    val headers = streamUrl?.headers ?: emptyMap()
    val referer = streamUrl?.referer

    var currentUrl by remember { mutableStateOf(videoUrl) }
    var currentHeaders by remember { mutableStateOf(headers) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Prepare headers with referer
    val combinedHeaders = remember(currentHeaders, referer) {
        (currentHeaders + (if (referer != null && referer.isNotBlank()) mapOf("Referer" to referer) else emptyMap())).toMap()
    }

    // Handle alternative links on error
    val tryNextSource = { (error: String) ->
        val nextAlt = altLinks.find { it != currentUrl }
        if (nextAlt != null) {
            currentUrl = nextAlt
            errorMessage = null
            isLoading = true
        } else {
            errorMessage = "Semua sumber video gagal: $error"
            onError?.invoke(errorMessage)
        }
    }

    // Reset loading when URL changes
    LaunchedEffect(currentUrl) {
        isLoading = true
        errorMessage = null
    }

    if (currentUrl.isNullOrBlank()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.material.Icon(
                    imageVector = Icons.Rounded.ErrorOutlined,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Tidak ada URL video", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Episode ini tidak memiliki link streaming", color = Color.LightGray, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = JayraBlue)) {
                    Text("Kembali", color = Color.White)
                }
            }
        }
    } else {
        NativeVideoPlayer(
            videoUrl = currentUrl!!,
            headers = combinedHeaders,
            title = "${animeTitle ?: "Anime"} - ${episodeTitle ?: "Episode"}",
            autoPlay = true,
            onError = { error ->
                errorMessage = error
                isLoading = false
                tryNextSource(error)
            },
            onCompletion = {
                onNextEpisode?.invoke()
            },
            onPrepared = {
                isLoading = false
            }
        )
    }
}

// ═══════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════

@Composable
private fun placeholder() = Box(
    Modifier
        .width(120.dp)
        .height(180.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
) {
    androidx.compose.material.Icon(
        imageVector = Icons.Rounded.Image,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun errorPlaceholder() = Box(
    Modifier
        .width(120.dp)
        .height(180.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.errorContainer)
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material.Icon(
            imageVector = Icons.Rounded.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.height(4.dp))
        Text("Gagal muat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Chip
import androidx.compose.ui.graphics.Color
import com.jayra.app.ui.theme.JayraBlue