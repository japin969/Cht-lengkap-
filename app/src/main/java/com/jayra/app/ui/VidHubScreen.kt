package com.jayra.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jayra.app.ui.drama.DramaScreen
import com.jayra.app.ui.anime.AnimeHomeScreen
import com.jayra.app.ui.komik.KomikHomeScreen
import com.jayra.app.ui.media.LocalMediaImageLoader
import com.jayra.app.ui.media.MediaImageLoaderProvider

/**
 * VidHub: wadah tab Drama / Anime / Komik.
 *
 * Menyediakan [LocalMediaImageLoader] untuk semua screen media di bawahnya,
 * sehingga poster/cover dari kedua fitur memakai cache memori + disk yang sama
 * (poster yang pernah muncul langsung tampil lagi saat scroll kembali).
 */
@Composable
fun VidHubScreen(
    onDramaClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit,
    onKomikClick: (String) -> Unit,
    onAnimeSearchClick: () -> Unit = {},
    onAnimeBookmarkClick: () -> Unit = {},
    onAnimeViewAllClick: () -> Unit = {},
    onKomikSearchClick: () -> Unit = {},
    onKomikBookmarkClick: () -> Unit = {},
    onKomikHistoryClick: () -> Unit = {},
    onKomikViewAllClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Strem", "Anime", "Komik")
    val context = LocalContext.current
    val mediaLoader = remember(context) { MediaImageLoaderProvider.get(context) }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalMediaImageLoader provides mediaLoader
    ) {
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
}