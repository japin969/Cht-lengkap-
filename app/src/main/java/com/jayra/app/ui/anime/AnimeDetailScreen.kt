package com.jayra.app.ui.anime

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import com.jayra.app.ui.anime.components.AnimeBackground
import com.jayra.app.ui.theme.SFProFontFamily

@Composable
fun AnimeDetailScreen() {
    Box(Modifier.fillMaxSize()) {
        AnimeBackground()
        IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column(Modifier.align(Alignment.Center)) {
            Text("Anime Detail", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
        }
    }
}