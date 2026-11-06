package com.jayra.app.ui.drama

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.data.DramaItem

@Composable
fun DramaScreen(
    viewModel: DramaViewModel = viewModel(),
    onDramaClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${state.error}", color = Color.Red)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Banner Utama (Featured)
            if (state.featured.isNotEmpty()) {
                item {
                    BannerItem(drama = state.featured.first()) { onDramaClick(it) }
                }
            }

            // Section Rekomendasi
            item {
                Text(
                    text = "Trending Minggu Ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.featured.drop(1)) { drama ->
                        DramaPosterCard(drama) { onDramaClick(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun BannerItem(drama: DramaItem, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .clickable { onClick(drama.url ?: "") }
    ) {
        AsyncImage(
            model = drama.cover,
            contentDescription = drama.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Gradient overlay agar teks terbaca
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 300f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = drama.title ?: "Unknown",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = drama.intro ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onClick(drama.url ?: "") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Tonton Sekarang")
            }
        }
    }
}

@Composable
fun DramaPosterCard(drama: DramaItem, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick(drama.url ?: "") }
    ) {
        AsyncImage(
            model = drama.cover,
            contentDescription = drama.title,
            modifier = Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = drama.title ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2
        )
        Text(
            text = "${drama.chapterCount ?: 0} Episode",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
