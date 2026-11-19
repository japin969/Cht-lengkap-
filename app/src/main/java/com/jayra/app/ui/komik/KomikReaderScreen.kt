package com.jayra.app.ui.komik

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun KomikReaderScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(slug) { viewModel.loadStream(slug) }

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
                    text = state.streamTitle ?: "Baca Komik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        when {
            state.isReaderLoading -> {
                ReaderSkeleton()
            }

            state.readerError != null && state.pages.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Gagal memuat chapter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.readerError ?: "Koneksi bermasalah",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.retryStream(slug) }) { Text("Coba Lagi") }
                }
            }

            else -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (onBack == null) {
                        item { Text(state.streamTitle ?: "", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
                    }
                    items(state.pages) { page ->
                        AsyncImage(
                            model = page.url,
                            contentDescription = "Page ${page.page}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderSkeleton() {
    val transition = rememberInfiniteTransition(label = "reader_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reader_skeleton_alpha"
    )
    val bone = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            Modifier
                .fillMaxWidth(0.6f)
                .height(16.dp)
                .background(bone)
        )
        Spacer(Modifier.height(12.dp))
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(bone)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
