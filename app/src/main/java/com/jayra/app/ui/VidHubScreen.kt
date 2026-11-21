package com.jayra.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jayra.app.ui.drama.DramaScreen
import com.jayra.app.ui.anime.AnimeHomeScreen
import com.jayra.app.ui.komik.KomikHomeScreen

/**
 * VidHub: wadah tab Drama / Anime / Komik.
 *
 * Setiap tab punya aksi menuju halaman pendukungnya:
 *  - Anime : Search, Bookmark, History, Lihat semua
 *  - Komik : Search, Bookmark, History, Lihat semua
 */
@Composable
fun VidHubScreen(
    onDramaClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit,
    onKomikClick: (String) -> Unit,
    onAnimeSearchClick: () -> Unit = {},
    onAnimeBookmarkClick: () -> Unit = {},
    onAnimeHistoryClick: () -> Unit = {},
    onAnimeViewAllClick: () -> Unit = {},
    onKomikSearchClick: () -> Unit = {},
    onKomikBookmarkClick: () -> Unit = {},
    onKomikHistoryClick: () -> Unit = {},
    onKomikViewAllClick: () -> Unit = {}
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
            1 -> AnimeHomeScreen(
                onAnimeClick = onAnimeClick,
                onSearchClick = onAnimeSearchClick,
                onBookmarkClick = onAnimeBookmarkClick,
                onHistoryClick = onAnimeHistoryClick,
                onViewAllClick = onAnimeViewAllClick
            )
            2 -> KomikHomeScreen(
                onKomikClick = onKomikClick,
                onSearchClick = onKomikSearchClick,
                onBookmarkClick = onKomikBookmarkClick,
                onHistoryClick = onKomikHistoryClick,
                onViewAllClick = onKomikViewAllClick
            )
        }
    }
}