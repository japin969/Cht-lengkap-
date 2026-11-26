package com.jayra.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jayra.app.data.AnimeRepository
import com.jayra.app.data.DramaRepository
import com.jayra.app.data.KomikRepository
import com.jayra.app.ui.drama.DramaScreen
import com.jayra.app.ui.anime.AnimeHomeScreen
import com.jayra.app.ui.komik.KomikHomeScreen

@Composable
fun VidHubScreen(
    onDramaClick: (String) -> Unit,
    onAnimeClick: (AnimeRepository.AnimeItem) -> Unit,
    onKomikClick: (KomikRepository.KomikItem) -> Unit,
    onDramaGenreClick: (String) -> Unit = {},
    onAnimeGenreClick: (String) -> Unit = {},
    onKomikGenreClick: (String) -> Unit = {}
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
            0 -> DramaScreen(
                onDramaClick = onDramaClick,
                onGenreClick = onDramaGenreClick
            )
            1 -> AnimeHomeScreen(
                onAnimeClick = onAnimeClick,
                onGenreClick = onAnimeGenreClick
            )
            2 -> KomikHomeScreen(
                onKomikClick = onKomikClick,
                onGenreClick = onKomikGenreClick
            )
        }
    }
}