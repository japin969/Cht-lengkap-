package com.jayra.app.ui.anime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jayra.app.ui.anime.components.AnimeBackground
import com.jayra.app.ui.anime.components.NetflixBottomNav
import com.jayra.app.ui.anime.components.NetflixHeroBanner
import com.jayra.app.ui.anime.components.NetflixRow
import com.jayra.app.ui.anime.components.NetflixTopBar
import com.jayra.app.ui.theme.SFProFontFamily

@Composable
fun AnimeHomeScreen(
    onNavigate: (String) -> Unit = {}
) {
    var isScrolled by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.value) { isScrolled = scrollState.value > 50 }

    Box(Modifier.fillMaxSize()) {
        AnimeBackground()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            NetflixTopBar(isScrolled = isScrolled, onSearchClick = { onNavigate("anime_search") })
            NetflixHeroBanner(title = "Anime", posterUrl = null, onPlay = {}, onInfo = {})
            NetflixRow(title = "Update Terbaru", items = emptyList())
            NetflixRow(title = "Upcoming", items = emptyList())
            NetflixRow(title = "Movie", items = emptyList())
            Spacer(Modifier.height(80.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter)) {
            NetflixBottomNav(currentRoute = "anime_home", onNavigate = onNavigate)
        }
    }
}