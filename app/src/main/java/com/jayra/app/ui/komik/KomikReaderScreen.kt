package com.jayra.app.ui.komik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun KomikReaderScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(slug) { viewModel.loadStream(slug) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${state.error}") }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            item { Text(state.streamTitle ?: "", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
            items(state.pages) { page ->
                AsyncImage(
                    model = page.url,
                    contentDescription = "Page ${page.page}",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}
