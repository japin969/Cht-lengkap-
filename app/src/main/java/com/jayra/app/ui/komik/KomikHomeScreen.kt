package com.jayra.app.ui.komik

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.stiffness
import androidx.compose.animation.core.dampingRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.KomikItem
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.util.GlassCard

@Composable
fun KomikHomeScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (KomikItem) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val glassSpec = remember(GlassEffectPrefs.enabledState.value) {
        LiquidGlassSpecV2.forTier(GlassEffectPrefs.resolveTier(), isSystemInDarkTheme())
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.latest.isEmpty() && state.error != null -> {
                ErrorState(
                    message = state.error ?: "Koneksi bermasalah",
                    onRetry = { viewModel.retry() },
                    glassSpec = glassSpec
                )
            }

            state.latest.isEmpty() -> {
                EmptyState(
                    title = "Belum ada komik",
                    subtitle = "Daftar komik sedang kosong. Coba muat ulang.",
                    onRetry = { viewModel.retry() },
                    glassSpec = glassSpec
                )
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Latest Komik Grid
                    KomikGridSection(
                        title = "Komik Terbaru",
                        items = state.latest,
                        onItemClick = onKomikClick,
                        glassSpec = glassSpec
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// KOMIK GRID SECTION
// ═══════════════════════════════════════════

@Composable
fun KomikGridSection(
    title: String,
    items: List<KomikItem>,
    onItemClick: (KomikItem) -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { komik ->
                SpringKomikCard(
                    komik = komik,
                    onClick = { onItemClick(komik) },
                    glassSpec = glassSpec
                )
            }
        }
    }
}

@Composable
fun SpringKomikCard(
    komik: KomikItem,
    onClick: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20),
        label = "komik_card_scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 2f else 8f,
        animationSpec = spring(stiffness = 300, dampingRatio = 20),
        label = "komik_card_elevation"
    )

    Column(
        modifier = modifier
            .weight(1f)
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
        // Cover with glass overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = komik.cover,
                contentDescription = komik.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = placeholder(),
                error = errorPlaceholder()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }

        // Title & Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp)
        ) {
            Text(
                komik.title ?: "",
                maxLines = 2,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                komik.updatedAt ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ═══════════════════════════════════════════
// ERROR & EMPTY STATES
// ═══════════════════════════════════════════

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                glassSpec = glassSpec,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.ErrorOutlined,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Gagal memuat komik",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Coba Lagi") }
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    onRetry: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                glassSpec = glassSpec,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Coba Lagi") }
                }
            }
        }
    }
}



// ═══════════════════════════════════════════
// PLACEHOLDERS
// ═══════════════════════════════════════════

@Composable
private fun placeholder() = Box(
    Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
) {
    androidx.compose.material.Icon(
        imageVector = androidx.compose.material.icons.Icons.Rounded.MenuBook,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun errorPlaceholder() = Box(
    Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.errorContainer)
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material.Icon(
            imageVector = androidx.compose.material.icons.Icons.Rounded.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.height(4.dp))
        Text("Gagal muat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

import androidx.compose.foundation.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures