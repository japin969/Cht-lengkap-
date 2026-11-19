package com.jayra.app.ui.komik

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.KomikItem

@Composable
fun KomikHomeScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> {
                KomikSkeleton()
            }

            state.latest.isEmpty() && state.error != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Gagal memuat komik",
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

            state.latest.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Belum ada komik", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Daftar komik sedang kosong. Coba muat ulang.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.retry() }) { Text("Coba Lagi") }
                }
            }

            else -> {
                Column(Modifier.fillMaxSize()) {
                    if (state.isFallback) {
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
                                text = state.error ?: "Memakai sumber cadangan",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.retry() }) { Text("Coba Lagi") }
                        }
                    }
                    Text("Komik Terbaru", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.latest) { komik ->
                            KomikCard(komik) { onKomikClick(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KomikCard(komik: KomikItem, onClick: (String) -> Unit) {
    val slug = komik.slug.orEmpty()
    Column(Modifier.clickable(enabled = slug.isNotBlank()) { onClick(slug) }) {
        AsyncImage(
            model = komik.cover,
            contentDescription = komik.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(4.dp))
        Text(komik.title ?: "", maxLines = 2, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Text(komik.updatedAt ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Skeleton grid loading — bukan spinner fullscreen. */
@Composable
private fun KomikSkeleton() {
    val transition = rememberInfiniteTransition(label = "komik_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "komik_skeleton_alpha"
    )
    val bone = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f)

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .padding(16.dp)
                .width(140.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(bone)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(12) {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bone)
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.85f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(bone)
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(bone)
                    )
                }
            }
        }
    }
}
