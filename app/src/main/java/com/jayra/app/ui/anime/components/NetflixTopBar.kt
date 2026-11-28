package com.jayra.app.ui.anime.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.theme.SFProFontFamily

@Composable
fun NetflixTopBar(
    isScrolled: Boolean,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isScrolled) Color.Black.copy(alpha = 0.8f) else Color.Transparent)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ANIME",
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.White
        )
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.padding(8.dp).clickableSafe(onSearchClick))
        Icon(Icons.Rounded.Cast, contentDescription = "Cast", tint = Color.White, modifier = Modifier.padding(8.dp))
        Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", tint = Color.White, modifier = Modifier.padding(8.dp))
    }
}

private fun Modifier.clickableSafe(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))