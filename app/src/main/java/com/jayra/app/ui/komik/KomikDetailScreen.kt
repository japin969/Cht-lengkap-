package com.jayra.app.ui.komik

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun KomikDetailScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onChapterClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(slug) { viewModel.loadDetail(slug) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${state.error}") }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = state.detail?.cover,
                        contentDescription = null,
                        modifier = Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(state.detail?.title ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(state.detail?.synopsis ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 8)
                    }
                }
            }
            item { Text("Daftar Chapter", Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
            items(state.chapters) { ch ->
                ListItem(
                    headlineContent = { Text(ch.title ?: "") },
                    modifier = Modifier.clickable {
                        onChapterClick(ch.slug ?: "")
                    }
                )
                Divider()
            }
        }
    }
}
