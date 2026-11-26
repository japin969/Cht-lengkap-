package com.jayra.app.ui.neko

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.stiffness
import androidx.compose.animation.core.dampingRatio
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.NekoItem
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs

@Composable
fun NekoHomeScreen(
    viewModel: NekoViewModel = viewModel(),
    onNekoClick: (NekoItem) -> Unit
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

            state.error != null && !state.hasAnyData -> {
                ErrorState(
                    message = state.error ?: "Koneksi bermasalah",
                    onRetry = { viewModel.retry() },
                    glassSpec = glassSpec
                )
            }

            !state.hasAnyData -> {
                EmptyState(
                    title = "Belum ada neko",
                    subtitle = "Daftar neko sedang kosong. Coba muat ulang.",
                    onRetry = { viewModel.retry() },
                    glassSpec = glassSpec
                )
            }

            else -> {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (state.ongoing.isNotEmpty()) item {
                        SectionHeader("Sedang Tayang")
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.ongoing) { neko ->
                                NekoPosterCard(neko) { onNekoClick(it) }
                            }
                        }
                    }
                    if (state.popular.isNotEmpty()) item {
                        SectionHeader("Populer")
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.popular) { neko ->
                                NekoPosterCard(neko) { onNekoClick(it) }
                            }
                        }
                    }
                    if (state.completed.isNotEmpty()) item {
                        SectionHeader("Selesai")
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.completed) { neko ->
                                NekoPosterCard(neko) { onNekoClick(it) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(16.dp),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
fun NekoPosterCard(neko: NekoItem, onClick: (NekoItem) -> Unit) {
    Column(Modifier.width(120.dp).clickable { onClick(neko) }) {
        AsyncImage(
            model = neko.image,
            contentDescription = neko.title,
            modifier = Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            placeholder = placeholder(),
            error = errorPlaceholder()
        )
        Spacer(Modifier.height(4.dp))
        Text(neko.title ?: "Unknown", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Text(neko.latestEpisode ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun placeholder() = Box(
    Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
) {
    androidx.compose.material.Icon(imageVector = androidx.compose.material.icons.Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.Center))
}

@Composable
private fun errorPlaceholder() = Box(
    Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.errorContainer)
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        androidx.compose.material.Icon(imageVector = androidx.compose.material.icons.Icons.Rounded.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.height(4.dp))
        Text("Gagal muat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(glassSpec = glassSpec, modifier = Modifier.padding(24.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material.Icon(imageVector = androidx.compose.material.icons.Icons.Rounded.ErrorOutlined, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(text = "Gagal memuat neko", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(glassSpec = glassSpec, modifier = Modifier.padding(24.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Coba Lagi") }
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    glassSpec: LiquidGlassSpecV2,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(24.dp)
            .glassSurfaceV2(glassSpec.surface, solidColor = MaterialTheme.colorScheme.surface, cornerRadius = 20.dp)
    ) {
        content()
    }
}

import androidx.compose.foundation.Arrangement