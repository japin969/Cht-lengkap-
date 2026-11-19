package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.AnimeEpisode

private val IOSBlue = Color(0xFF007AFF)
private val IOSBlueDark = Color(0xFF0A84FF)

@Composable
fun AnimeDetailScreen(
    animeUrl: String,
    viewModel: AnimeViewModel = viewModel(),
    onPlayEpisode: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val cardBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val labelColor = if (isDark) Color.White else Color.Black
    val secondary = if (isDark) Color(0xFF98989F) else Color(0xFF8A8A8E)
    val accent = if (isDark) IOSBlueDark else IOSBlue

    LaunchedEffect(animeUrl) { viewModel.loadDetail(animeUrl) }

    Box(
        Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        when {
            state.isLoading && state.detail == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }

            state.detail == null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Gagal memuat detail", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.error ?: "Coba lagi sebentar.",
                        fontSize = 14.sp,
                        color = secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent)
                            .clickable { viewModel.loadDetail(animeUrl) }
                            .padding(horizontal = 22.dp, vertical = 10.dp)
                    ) {
                        Text("Coba Lagi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            else -> {
                val detail = state.detail!!
                var synopsisExpanded by remember { mutableStateOf(false) }

                LazyColumn(Modifier.fillMaxSize()) {
                    // ===== BACKDROP POSTER + GRADIENT =====
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(detail.image)
                                    .crossfade(200)
                                    .build(),
                                contentDescription = detail.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Black.copy(alpha = 0.35f),
                                            0.55f to Color.Transparent,
                                            1f to screenBg
                                        )
                                    )
                            )
                            if (onBack != null) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .clickable(onClick = onBack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronLeft,
                                        contentDescription = "Kembali",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ===== JUDUL & META =====
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = detail.title ?: "Anime",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = labelColor,
                                lineHeight = 27.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetaChip(
                                    text = "★ ${detail.rating?.takeIf { it > 0.0 }?.let { formatRating(it) } ?: "-"}",
                                    isDark = isDark
                                )
                                MetaChip(
                                    text = "${detail.totalEpisodes ?: state.episodes.size} Episode",
                                    isDark = isDark
                                )
                                detail.info?.get("Status")?.takeIf { it.isNotBlank() }?.let {
                                    MetaChip(text = it, isDark = isDark)
                                }
                            }

                            val genres = detail.genres.orEmpty().filter { it.isNotBlank() }
                            if (genres.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(
                                        androidx.compose.foundation.rememberScrollState()
                                    )
                                ) {
                                    genres.forEach { g ->
                                        Text(
                                            text = g,
                                            fontSize = 12.sp,
                                            color = accent,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(accent.copy(alpha = 0.12f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                        }
                    }

                    // ===== SINOPSIS =====
                    item {
                        val synopsis = detail.synopsis?.replace(Regex("<[^>]*>"), "")?.trim()
                        if (!synopsis.isNullOrBlank()) {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = "Sinopsis",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = labelColor
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = synopsis,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = secondary,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (synopsis.length > 220) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (synopsisExpanded) "Sembunyikan" else "Selengkapnya",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accent,
                                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                            }
                        }
                    }

                    // ===== DAFTAR EPISODE =====
                    item {
                        Text(
                            text = "Daftar Episode",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = labelColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    if (state.episodes.isEmpty()) {
                        item {
                            Text(
                                text = "Episode belum tersedia.",
                                fontSize = 14.sp,
                                color = secondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(
                            items = state.episodes,
                            key = { it.episodeUrl ?: it.title ?: it.episode ?: "ep" }
                        ) { ep ->
                            EpisodeRow(
                                episode = ep,
                                cardBg = cardBg,
                                labelColor = labelColor,
                                secondary = secondary,
                                accent = accent,
                                onClick = {
                                    val url = ep.episodeUrl
                                    if (!url.isNullOrBlank()) onPlayEpisode(url)
                                }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, isDark: Boolean) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = if (isDark) Color(0xFFEBEBF5) else Color(0xFF3C3C43),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE9E9EB))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun EpisodeRow(
    episode: AnimeEpisode,
    cardBg: Color,
    labelColor: Color,
    secondary: Color,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("episode_row_${episode.episode ?: episode.title}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = episode.episode?.takeIf { it.isNotBlank() } ?: "▶",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = episode.title?.takeIf { it.isNotBlank() }
                ?: "Episode ${episode.episode ?: ""}",
            fontSize = 15.sp,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = secondary,
            modifier = Modifier.size(20.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = secondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun formatRating(value: Double): String =
    if (value > 10.0) String.format("%.1f", value / 10.0) else String.format("%.1f", value)
