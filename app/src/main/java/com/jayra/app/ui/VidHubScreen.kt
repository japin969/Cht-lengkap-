package com.jayra.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jayra.app.ui.drama.DramaScreen
import com.jayra.app.ui.komik.KomikHomeScreen

@Composable
fun VidHubScreen(
    onDramaClick: (String) -> Unit,
    onAnimeNavigate: () -> Unit,
    onKomikClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Drama", "Anime", "Komik")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> DramaScreen(onDramaClick = onDramaClick)
            1 -> {
                // Navigate to anime home when Anime tab is selected
                LaunchedEffect(selectedTab) {
                    if (selectedTab == 1) {
                        onAnimeNavigate()
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    Text("Loading Anime...", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
                }
            }
            2 -> KomikHomeScreen(onKomikClick = onKomikClick)
        }
    }
}