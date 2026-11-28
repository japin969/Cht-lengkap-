package com.jayra.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.R
import com.jayra.app.data.DonghuaItem

/**
 * Poster tunggal. Placeholder + error selalu ada supaya tidak blank/hitam,
 * termasuk ketika URL null.
 */
@Composable
fun DonghuaPosterImage(
    url: String?,
    title: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val placeholder = painterResource(R.drawable.ic_poster_placeholder)
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url?.takeIf { it.isNotBlank() })
            .crossfade(150)
            .build(),
        contentDescription = title,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = contentScale,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
fun DonghuaPosterCard(
    item: DonghuaItem,
    onClick: (DonghuaItem) -> Unit,
    width: Dp = 112.dp
) {
    Column(
        Modifier
            .width(width)
            .clickable { onClick(item) }
    ) {
        DonghuaPosterImage(
            url = item.poster,
            title = item.title,
            modifier = Modifier
                .width(width)
                .height(width * 1.5f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val subtitle = item.latestEpisode ?: item.status
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DonghuaSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.Unspecified
    )
}
