package com.jayra.app.ui.komik

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

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Column(Modifier.fillMaxSize()) {
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

@Composable
fun KomikCard(komik: KomikItem, onClick: (String) -> Unit) {
    Column(Modifier.clickable { onClick(komik.slug ?: "") }) {
        AsyncImage(
            model = komik.cover,
            contentDescription = komik.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(4.dp))
        Text(komik.title ?: "", maxLines = 2, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Text(komik.updatedAt ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
