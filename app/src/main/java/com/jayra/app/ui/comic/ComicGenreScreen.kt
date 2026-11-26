package com.jayra.app.ui.comic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * ComicGenre — daftar genre (tag sankavollerei comic). Tap → ComicList filter.
 */
private val comicGenres = listOf(
    "action", "adventure", "comedy", "drama", "fantasi", "horror",
    "romance", "sci-fi", "shounen", "slice of life", "sports", "supernatural",
    "thriller", "mystery", "historical", "school", "magic", "martial arts"
)

@Composable
fun ComicGenreScreen(
    onBack: () -> Unit,
    onGenreClick: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        Row(
            Modifier.fillMaxWidth().background(MediaColors.Bg).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MediaColors.TextPrimary)
            }
            Spacer(Modifier.size(4.dp))
            Text("Genre", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
                color = MediaColors.TextPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MediaDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(comicGenres) { genre ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MediaColors.SurfaceElevated)
                        .clickable { onGenreClick(genre) }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(MediaColors.Comic))
                    Spacer(Modifier.size(12.dp))
                    Text(genre.replaceFirstChar { it.uppercase() }, fontFamily = SFProFontFamily, fontWeight = FontWeight.Medium,
                        fontSize = 15.sp, color = MediaColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text("›", fontFamily = SFProFontFamily, fontSize = 20.sp, color = MediaColors.TextTertiary)
                }
            }
        }
    }
}