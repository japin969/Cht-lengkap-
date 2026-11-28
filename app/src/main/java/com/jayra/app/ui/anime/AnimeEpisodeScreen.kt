package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun AnimeEpisodeScreen() {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Text("Anime Player", color = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}