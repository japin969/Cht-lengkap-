package com.jayra.app.ui.comic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.data.KomikItem
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaPoster
import com.jayra.app.ui.theme.SFProFontFamily

/** Kartu poster komik (grid) — pola Webtoon/Tapas. */
@Composable
fun ComicPosterCard(
    item: KomikItem,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .width(MediaDimens.PosterW)
            .clickable { onClick(item.slug ?: "") }
    ) {
        MediaPoster(
            model = item.cover,
            contentDescription = item.title,
            accent = MediaColors.Comic,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
        )
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
        item.updatedAt?.let {
            Text(
                it,
                fontFamily = SFProFontFamily,
                fontSize = 10.sp,
                color = MediaColors.TextTertiary,
                maxLines = 1
            )
        }
    }
}