package com.jayra.app.ui.drama

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.DramaItem

/**
 * Home Drama — Bilibili-style: grid video 16:9 + durasi, judul 2 baris,
 * tab Donghua / Donghub terpisah jelas.
 * Tema iOS 26 glass: kaca PUTIH KRISTAL BENING (tanpa blur).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DramaScreen(
    viewModel: DramaViewModel = viewModel(),
    onDramaClick: (slug: String, source: String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Satu fitur: gabungan Donghua + Donghub dalam satu feed.
    val items = remember(state.donghua, state.donghub) {
        state.donghua + state.donghub
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            items.isEmpty() -> {
                EmptyState(state.error) { viewModel.retry() }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { drama ->
                        VideoGridCard(drama) { onDramaClick(drama.slug ?: "", drama.source ?: "donghua") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(msg: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Belum ada drama", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            msg ?: "Tarik untuk muat ulang.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
private fun VideoGridCard(drama: DramaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.92f)) // glass putih bening
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = drama.poster ?: drama.cover,
                contentDescription = drama.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // durasi / episode badge kanan bawah
            val badge = drama.currentEpisode ?: drama.episode
            if (!badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = drama.title ?: "Tanpa Judul",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black
        )
        val sub = drama.status ?: drama.type
        if (!sub.isNullOrBlank()) {
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF666666),
                maxLines = 1
            )
        }
    }
}