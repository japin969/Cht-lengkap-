package com.jayra.app.ui.anime.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jayra.app.ui.theme.SFProFontFamily

@Composable
fun NetflixHeroBanner(
    title: String,
    posterUrl: String?,
    genres: String = "",
    onPlay: () -> Unit = {},
    onInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().height(450.dp)) {
        AsyncImage(
            model = posterUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0A0A0F))))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(title, fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Color.White)
            if (genres.isNotBlank()) {
                Text(genres, fontFamily = SFProFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Color.LightGray)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Tonton", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
                Button(
                    onClick = onInfo,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Info", fontFamily = SFProFontFamily, color = Color.White)
                }
            }
        }
    }
}