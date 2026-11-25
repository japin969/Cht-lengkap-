package com.jayra.app.ui.anime

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

/**
 * Poster anime dengan placeholder native.
 *
 * - Belum pernah muncul (loading)  -> ikon + background lembut
 * - Gagal dimuat                   -> ikon BrokenImage
 * - Berhasil                       -> gambar asli dengan fade-in halus
 *
 * Tidak ada animasi shimmer (mahal) dan tidak ada SubcomposeAsyncImage (lambat),
 * supaya scroll tetap mulus 30-60 FPS. Gambar yang sudah pernah dimuat
 * langsung muncul lagi dari memory cache Coil.
 */
@Composable
fun AnimePoster(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    icon: ImageVector = Icons.Rounded.Movie,
    iconSize: Dp = 30.dp,
    showPlaceholderIcon: Boolean = true
) {
    val context = LocalContext.current
    var loadState by remember(url) { mutableStateOf(LoadState.EMPTY) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (showPlaceholderIcon && loadState != LoadState.READY) {
            Icon(
                imageVector = if (loadState == LoadState.ERROR)
                    Icons.Rounded.BrokenImage else icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                modifier = Modifier.size(iconSize)
            )
        }
        if (!url.isNullOrBlank()) {
            AsyncImage(
                // Pakai ImageRequest -> shared ImageLoader (cache memori + disk
                // yang sama dengan Komik). Poster yang pernah muncul langsung
                // tampil lagi tanpa kedip putih.
                model = coil.request.ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(false)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier
                    .matchParentSize(),
                contentScale = contentScale,
                imageLoader = com.jayra.app.ui.media.LocalMediaImageLoader.current,
                onState = { state ->
                    loadState = when (state) {
                        is AsyncImagePainter.State.Success -> LoadState.READY
                        is AsyncImagePainter.State.Error -> LoadState.ERROR
                        else -> LoadState.EMPTY
                    }
                }
            )
        }
    }
}

private enum class LoadState { EMPTY, READY, ERROR }

/** Chip rating bintang. */
@Composable
fun AnimeRatingChip(rating: Double?) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = Color(0xFFFACC15),
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = rating?.let { String.format("%.1f", it) } ?: "-",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Chip info kecil (status / jumlah episode / tipe). */
@Composable
fun AnimeInfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

/**
 * Header section anime dengan subtitle + aksi.
 */
@Composable
fun AnimeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** Chip filter yang bisa dipilih. */
@Composable
fun AnimeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

/** Loading state untuk anime. */
@Composable
fun AnimeLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/** Empty state anime. */
@Composable
fun AnimeEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Error state anime dengan tombol coba lagi. */
@Composable
fun AnimeErrorState(
    message: String?,
    modifier: Modifier = Modifier,
    title: String = "Gagal memuat",
    onRetry: (() -> Unit)? = null
) {
    AnimeEmptyState(
        title = title,
        modifier = modifier,
        message = message?.takeIf { it.isNotBlank() }
            ?: "Koneksi bermasalah. Periksa jaringan lalu coba lagi.",
        actionLabel = if (onRetry != null) "Coba Lagi" else null,
        onAction = onRetry
    )
}