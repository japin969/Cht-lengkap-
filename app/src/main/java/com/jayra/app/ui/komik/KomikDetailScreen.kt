package com.jayra.app.ui.komik

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.stiffness
import androidx.compose.animation.core.dampingRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikChapter
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.util.GlassCard

@Composable
fun KomikDetailScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onChapterClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val glassSpec = remember(GlassEffectPrefs.enabledState.value) {
        LiquidGlassSpecV2.forTier(GlassEffectPrefs.resolveTier(), isSystemInDarkTheme())
    }

    LaunchedEffect(slug) { viewModel.loadDetail(slug) }

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
                Button(onClick = { viewModel.loadDetail(slug) }) { Text("Coba Lagi") }
            }
        }
    } else {
        val detail = state.detail
        val chapters = state.chapters

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp)
        ) {
            // Hero Cover Section
            item {
                HeroCoverSection(
                    detail = detail,
                    glassSpec = glassSpec
                )
            }

            // Info Section
            item {
                InfoSection(
                    detail = detail,
                    glassSpec = glassSpec,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            // Chapter List
            item {
                ChapterListSection(
                    chapters = chapters,
                    onChapterClick = onChapterClick,
                    glassSpec = glassSpec,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// HERO COVER SECTION
// ════════════════════════════════════════════════════════════════

@Composable
fun HeroCoverSection(
    detail: com.jayra.app.data.KomikDetailResponse?,
    glassSpec: LiquidGlassSpecV2
) {
    val coverUrl = detail?.cover
    val title = detail?.title ?: ""
    val synopsis = detail?.synopsis ?: ""
    val chapters = detail?.chapters.orEmpty()
    val latestChapter = chapters.firstOrNull()?.title ?: chapters.lastOrNull()?.title ?: "Chapter 1"

    val aspectRatio = 0.666f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1 / aspectRatio)
    ) {
        // Cover Image
        AsyncImage(
            model = coverUrl,
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
                Text(
                    text = "${chapters.size} Chapter",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(16.dp))

            // Read Latest Button
            val latestChapter = chapters.firstOrNull() ?: chapters.lastOrNull()
            Button(
                onClick = { latestChapter?.slug?.let { latestChapter -> onChapterClick(latestChapter.slug) } },
                enabled = chapters.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.jayra.app.ui.theme.JayraBlue,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.MenuBook,
                        contentDescription = "Read",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (chapters.isNotEmpty()) "Baca Chapter Terbaru" else "Belum ada chapter",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// INFO SECTION
// ════════════════════════════════════════════════════════════════

@Composable
fun InfoSection(
    detail: com.jayra.app.data.KomikDetailResponse?,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    val synopsis = detail?.synopsis ?: ""

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
        }
    }
}

// ════════════════════════════════════════════════════════════════
// CHAPTER LIST SECTION
// ════════════════════════════════════════════════════════════════

@Composable
fun ChapterListSection(
    chapters: List<com.jayra.app.data.KomikChapter>,
    onChapterClick: (String) -> Unit,
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
            Text("Chapter (${chapters.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // Chapter List - Reverse order (latest first)
        val reversedChapters = chapters.reversed()
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reversedChapters) { ch ->
                ChapterCard(
                    chapter = ch,
                    onClick = { onChapterClick(ch.slug ?: "") },
                    glassSpec = glassSpec
                )
            }
        }
    }
}

@Composable
fun ChapterCard(
    chapter: com.jayra.app.data.KomikChapter,
    onClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20),
        label = "chapter_card_scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 2f else 6f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20),
        label = "chapter_card_elevation"
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
                    onRelease = { pressed = false; onClick() },
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
                Text(
                    chapter.title ?: "Chapter ${chapter.chapter ?: "?"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Read Indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(com.jayra.app.ui.theme.JayraBlue)
                ) {
                    androidx.compose.material.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.MenuBook,
                        contentDescription = "Read",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// PLACEHOLDERS
// ════════════════════════════════════════════════════════════════

@Composable
private fun heroPlaceholder() = Box(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
) {
    androidx.compose.material.Icon(
        imageVector = androidx.compose.material.icons.Icons.Rounded.MenuBook,
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
        androidx.compose.material.Icon(imageVector = androidx.compose.material.icons.Icons.Rounded.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text("Gagal memuat cover", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

// ════════════════════════════════════════════════════════════════
// GLASS CARD
// ════════════════════════════════════════════════════════════════

