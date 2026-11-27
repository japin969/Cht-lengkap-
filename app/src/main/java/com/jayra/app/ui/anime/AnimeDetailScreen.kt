package com.jayra.app.ui.anime

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.stiffness
import androidx.compose.animation.core.dampingRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.ErrorOutlined
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
import com.jayra.app.data.AnimeEpisode
import com.jayra.app.ui.video.NativeVideoPlayer
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs

@Composable
fun AnimeDetailScreen(
    animeId: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (AnimeEpisode) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val glassSpec = remember(GlassEffectPrefs.enabledState.value) {
        LiquidGlassSpecV2.forTier(GlassEffectPrefs.resolveTier(), isSystemInDarkTheme())
    }

    var playingEpisode by remember { mutableStateOf<AnimeEpisode?>(null) }

    LaunchedEffect(animeId) { viewModel.loadDetail(animeId) }

    // Fullscreen Video Player Overlay
    if (playingEpisode != null) {
        val ep = playingEpisode!!
        val hasStreamUrl = ep.episodeUrl?.isNotBlank() == true

        if (hasStreamUrl) {
            NativeVideoPlayer(
                videoUrl = ep.episodeUrl!!,
                headers = mapOf("Referer" to "https://apii.nexadev.my.id/"),
                title = "${state.detail?.title ?: "Anime"} - ${ep.title ?: "Episode ${ep.episode}"}",
                autoPlay = true,
                onError = { error ->
                    playingEpisode = null
                },
                onCompletion = {
                    playingEpisode = null
                }
            )
        } else {
            // No video URL available
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutlined,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Video tidak tersedia", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Episode ini belum memiliki link streaming", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { playingEpisode = null },
                        colors = ButtonDefaults.buttonColors(containerColor = com.jayra.app.ui.theme.JayraBlue)
                    ) {
                        Text("Tutup", color = Color.White)
                    }
                }
            }
        }
    } else {
        // Detail Screen with DeepSeek/ChatGPT style
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp)
            ) {
                // Hero Poster Section
                item {
                    HeroPosterSection(
                        detail = state.detail,
                        glassSpec = glassSpec,
                        onPlayClick = { ep ->
                            if (ep.episodeUrl?.isNotBlank() == true) {
                                playingEpisode = ep
                            }
                        }
                    )
                }

                // Info Section
                item {
                    InfoSection(
                        detail = state.detail,
                        glassSpec = glassSpec,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }

                // Episode List
                item {
                    EpisodeListSection(
                        episodes = state.episodes,
                        onEpisodeClick = { ep ->
                            if (ep.episodeUrl?.isNotBlank() == true) {
                                playingEpisode = ep
                            }
                        },
                        glassSpec = glassSpec,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// HERO POSTER SECTION
// ══════════════════════════════════════════════════════════════════

@Composable
fun HeroPosterSection(
    detail: com.jayra.app.data.AnimeDetailData?,
    glassSpec: LiquidGlassSpecV2,
    onPlayClick: (com.jayra.app.data.AnimeEpisode) -> Unit
) {
    val posterUrl = detail?.image
    val title = detail?.title ?: ""
    val rating = detail?.rating
    val totalEpisodes = detail?.totalEpisodes
    val genres = detail?.genres.orEmpty()
    val episodes = detail?.episodes.orEmpty()
    val latestEp = episodes.firstOrNull()?.episode ?: episodes.lastOrNull()?.episode ?: "1"
    val nextEpNum = (latestEp.toIntOrNull() ?: 0) + 1

    val aspectRatio = 0.666f // 2:3 poster ratio

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1 / aspectRatio)
    ) {
        // Poster Image
        AsyncImage(
            model = posterUrl,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = heroPlaceholder(),
            error = heroErrorPlaceholder()
        )

        // Gradient Overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        // Content Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))

            // Meta Info Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating
                rating?.let { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Rating",
                            tint = Color.Yellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "%.1f".format(r / 10.0),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Total Episodes
                totalEpisodes?.let { total ->
                    Text(
                        text = "$total Episode",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }

                // Genres
                if (genres.isNotEmpty()) {
                    Text(
                        text = genres.take(3).joinToString(" • "),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                val hasEpisodes = episodes.isNotEmpty()
                val latestEp = episodes.firstOrNull() ?: episodes.lastOrNull()
                
                Button(
                    onClick = { latestEp?.let { onPlayClick(it) } },
                    enabled = hasEpisodes,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.jayra.app.ui.theme.JayraBlue,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (hasEpisodes) "Tonton Episode $nextEpNum" else "Tidak ada episode",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Add to List / Download Button (placeholder)
                IconButton(
                    onClick = { /* TODO: add to list */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Tambah ke daftar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// INFO SECTION
// ═══════════════════════════════════════════════════════════════════

@Composable
fun InfoSection(
    detail: com.jayra.app.data.AnimeDetailData?,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    val synopsis = detail?.synopsis ?: ""
    val info = detail?.info.orEmpty()

    GlassCard(
        glassSpec = glassSpec,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Synopsis
            if (synopsis.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(synopsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Info Grid
            if (info.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Informasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(info.entries.toList()) { entry ->
                            InfoRow(
                                label = entry.key,
                                value = entry.value
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// ═════════════════════════════════════════════════════════════════════
// EPISODE LIST SECTION
// ═══════════════════════════════════════════════════════════════════

@Composable
fun EpisodeListSection(
    episodes: List<com.jayra.app.data.AnimeEpisode>,
    onEpisodeClick: (com.jayra.app.data.AnimeEpisode) -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Episode (${episodes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // Episode List - Reverse order (latest first)
        val reversedEpisodes = episodes.reversed()
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reversedEpisodes) { ep ->
                EpisodeCard(
                    episode = ep,
                    onClick = { onEpisodeClick(ep) },
                    glassSpec = glassSpec
                )
            }
        }
    }
}

@Composable
fun EpisodeCard(
    episode: com.jayra.app.data.AnimeEpisode,
    onClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    val hasStreamUrl = episode.episodeUrl?.isNotBlank() == true
    var pressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20),
        label = "episode_card_scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 2f else 6f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20),
        label = "episode_card_elevation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation = elevation.dp, shape = RoundedCornerShape(12.dp), clip = true)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onPress = { pressed = true },
                    onRelease = { pressed = false; if (hasStreamUrl) onClick() },
                    onCancel = { pressed = false }
                )
            }
    ) {
        GlassCard(
            glassSpec = glassSpec,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Episode Info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Episode ${episode.episode ?: "?"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    episode.title?.let { title ->
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Play Indicator
                if (hasStreamUrl) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(com.jayra.app.ui.theme.JayraBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center).size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// PLACEHOLDERS
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun heroPlaceholder() = Box(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
) {
    androidx.compose.material.Icon(
        imageVector = Icons.Rounded.Movie,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.align(Alignment.Center).size(64.dp)
    )
}

@Composable
private fun heroErrorPlaceholder() = Box(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer)
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        androidx.compose.material.Icon(imageVector = Icons.Rounded.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text("Gagal memuat poster", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

// ════════════════════════════════════════════════════════════════════
// GLASS CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 16.dp)
    ) {
        content()
    }
}