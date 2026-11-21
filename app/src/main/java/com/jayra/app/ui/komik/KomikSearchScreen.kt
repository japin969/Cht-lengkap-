package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.ui.media.MediaEmptyState
import com.jayra.app.ui.media.MediaErrorState
import com.jayra.app.ui.media.MediaLoading
import com.jayra.app.ui.media.MediaTopBar
import kotlinx.coroutines.delay

/**
 * Pencarian Komik — memakai Nexadev API (komikindo).
 *
 * Query kosong  -> panel saran + daftar populer (hasil loadHome).
 * Sedang ketik  -> debounce 350ms lalu cari ke API.
 */
@Composable
fun KomikSearchScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var searchTerm by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<com.jayra.app.data.KomikItem>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    // Debounce query -> searchTerm
    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchTerm = ""
            hasSearched = false
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        delay(350)
        searchTerm = query.trim()
    }

    LaunchedEffect(searchTerm) {
        if (searchTerm.isBlank()) return@LaunchedEffect
        searching = true
        searchError = null
        val outcome = runCatching { viewModel.search(searchTerm) }
            .fold(
                onSuccess = { it to null },
                onFailure = { emptyList<com.jayra.app.data.KomikItem>() to it.localizedMessage }
            )
        results = outcome.first
        searchError = outcome.second
        searching = false
        hasSearched = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MediaTopBar(title = "Cari Komik", onBack = onBack)

        KomikSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Ketik judul komik..."
        )

        Box(Modifier.fillMaxSize()) {
            when {
                searching -> MediaLoading()

                searchError != null && results.isEmpty() -> MediaErrorState(
                    message = searchError,
                    title = "Pencarian gagal"
                )

                hasSearched && results.isEmpty() -> MediaEmptyState(
                    title = "Tidak ditemukan",
                    message = "Tidak ada komik yang cocok dengan \"$searchTerm\".",
                    icon = Icons.Rounded.Search
                )

                hasSearched && results.isNotEmpty() -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "${results.size} hasil untuk \"$searchTerm\"",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item { KomikGrid(items = results, onClick = onKomikClick) }
                }

                else -> SearchSuggestions(state = state, onQueryPick = { query = it })
            }
        }
    }
}

@Composable
private fun SearchSuggestions(
    state: KomikUiState,
    onQueryPick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Pencarian populer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        item {
            Text(
                text = "Mulai mengetik judul komik untuk mencari.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (state.popular.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Rekomendasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            item { KomikGrid(items = state.popular, onClick = onQueryPick) }
        }
    }
}