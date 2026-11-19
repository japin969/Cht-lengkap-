package com.jayra.app.ui.komik

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@Composable
fun KomikDetailScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    onChapterClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(slug) { viewModel.loadDetail(slug) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Detail Komik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        when {
            state.isDetailLoading && state.detail == null -> {
                KomikDetailSkeleton()
            }

            state.detailError != null && state.detail == null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Gagal memuat detail komik",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.detailError ?: "Koneksi bermasalah",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.retryDetail(slug) }) { Text("Coba Lagi") }
                }
            }

            else -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Row(Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = state.detail?.cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    state.detail?.title ?: "",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(state.detail?.synopsis ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 8)
                            }
                        }
                    }
                    item { Text("Daftar Chapter", Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
                    items(state.chapters) { ch ->
                        ListItem(
                            headlineContent = { Text(ch.title ?: "") },
                            modifier = Modifier.clickable { onChapterClick(ch.slug ?: "") }
                        )
                        HorizontalDivider()
                    }
                    if (state.chapters.isEmpty()) {
                        item {
                            Text(
                                text = "Daftar chapter belum tersedia dari sumber data ini. " +
                                    "Coba Lagi kalau koneksi bermasalah.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(
                                onClick = { viewModel.retryDetail(slug) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) { Text("Coba Lagi") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KomikDetailSkeleton() {
    val transition = rememberInfiniteTransition(label = "komik_detail_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "komik_detail_skeleton_alpha"
    )
    val bone = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f)

    Row(Modifier.padding(16.dp)) {
        Box(
            Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bone)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            repeat(5) { i ->
                Box(
                    Modifier
                        .fillMaxWidth(if (i % 2 == 0) 0.9f else 0.65f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bone)
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
