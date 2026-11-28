package com.jayra.app.ui.anime

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jayra.app.ui.anime.components.AnimeBackground

@Composable
fun AnimeSearchScreen() {
    Box(Modifier.fillMaxSize()) {
        AnimeBackground()
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Cari anime...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}