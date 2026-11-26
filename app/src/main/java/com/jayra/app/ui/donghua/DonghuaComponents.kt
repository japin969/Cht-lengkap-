package com.jayra.app.ui.donghua

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jayra.app.data.DramaItem
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * DonghuaPosterCard — beda dari Anime: ada badge "3D/2D" + "Episode terbaru".
 * Pola aplikasi donghua terkenal (Bilibili/WeTV).
 */
@Composable
fun DonghuaPosterCard(
    item: DramaItem,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .width(MediaDimens.PosterW)
            .clickable { onClick(item.url ?: "") }
    ) {
        Box {
            com.jayra.app.ui.media.MediaPoster(
                model = item.cover,
                contentDescription = item.title,
                accent = MediaColors.Strem,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.7f)
            )
            // Badge 3D/2D (pola donghua)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MediaColors.Strem)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("3D", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title ?: "Tanpa Judul",
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MediaColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${item.chapterCount ?: 0} Episode",
            fontFamily = SFProFontFamily,
            fontSize = 10.sp,
            color = MediaColors.TextTertiary,
            maxLines = 1
        )
    }
}

@Composable
fun DonghuaGridItem(item: DramaItem, onClick: (String) -> Unit) {
    Column(Modifier.clickable { onClick(item.url ?: "") }) {
        Box {
            com.jayra.app.ui.media.MediaPoster(
                model = item.cover,
                contentDescription = item.title,
                accent = MediaColors.Strem,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
            Box(
                Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp))
                    .background(MediaColors.Strem).padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("3D", fontFamily = SFProFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            item.title ?: "Tanpa Judul",
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MediaColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}