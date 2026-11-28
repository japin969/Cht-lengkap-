package com.jayra.app.ui.anime.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jayra.app.ui.theme.SFProFontFamily

@Composable
fun NetflixRow(
    title: String,
    items: List<Pair<String, String?>> = emptyList(),
    newItemSlugs: Set<String> = emptySet(),
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.first }) { (slug, poster) ->
                Box(Modifier.width(110.dp).aspectRatio(2f / 3f)) {
                    AsyncImage(
                        model = poster,
                        contentDescription = slug,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray, RoundedCornerShape(4.dp))
                    )
                    if (slug in newItemSlugs) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .background(Color(0xFFE50914), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Baru", color = Color.White, fontSize = 10.sp, fontFamily = SFProFontFamily)
                        }
                    }
                }
            }
        }
    }
}
```