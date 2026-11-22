package com.jayra.app.ui.media

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Utilitas gambar untuk screen Komik & Anime.
 *
 * Tujuan utama: TIDAK ADA gambar yang tampil sebagai kotak kosong / blank.
 *  - loading  -> shimmer + ikon
 *  - error    -> ikon fallback + tombol coba lagi
 *  - sukses   -> gambar asli dari Nexadev
 *
 * Selain itu, URL dari Nexadev yang memakai proxy WordPress (`i0/i1/i2/i3.wp.com`)
 * dicoba bergantian antar host bila salah satu gagal di jaringan pengguna.
 */

private val WP_HOSTS = listOf("i0.wp.com", "i1.wp.com", "i2.wp.com", "i3.wp.com")

/**
 * Buat daftar kandidat URL. Bila URL adalah proxy WordPress, hasilkan varian
 * untuk setiap host proxy. Bila bukan, kembalikan URL apa adanya.
 */
fun wpCandidates(url: String?): List<String> {
    if (url.isNullOrBlank()) return emptyList()
    val match = Regex("^https?://i\\d\\.wp\\.com/(.+)$").find(url)
    return if (match != null) {
        val path = match.groupValues[1]
        WP_HOSTS.map { "https://$it/$path" }
    } else {
        listOf(url)
    }
}

/**
 * ImageLoader khusus media (anime) — TIDAK mengubah konfigurasi global app.
 * Dipasang lewat CompositionLocalProvider(LocalImageLoader provides ...) hanya
 * di screen anime.
 *
 * Bukan bypass SSL: OkHttp default + trust store sistem, hanya menambah
 * User-Agent (seperti browser) dan retry koneksi.
 */
fun createMediaImageLoader(context: Context): ImageLoader {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", MEDIA_USER_AGENT)
                .header(
                    "Accept",
                    "image/avif,image/webp,image/jpeg,image/png,image/*,*/*;q=0.8"
                )
                .build()
            chain.proceed(request)
        }
        .build()

    return ImageLoader.Builder(context)
        .okHttpClient(client)
        .crossfade(true)
        .allowHardware(true)
        .build()
}

private const val MEDIA_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

/**
 * Gambar dengan placeholder native + retry antar host CDN.
 */
@Composable
fun MediaImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(12.dp),
    fallbackIcon: ImageVector = Icons.Rounded.BrokenImage,
    iconSize: Dp = 30.dp,
    showRetry: Boolean = true
) {
    val candidates = remember(url) { wpCandidates(url) }
    var attempt by remember(url) { mutableIntStateOf(0) }
    val currentIndex = attempt.coerceAtMost(candidates.lastIndex.coerceAtLeast(0))
    val current = candidates.getOrNull(currentIndex)

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (current == null) {
            FallbackContent(fallbackIcon, iconSize)
        } else {
            SubcomposeAsyncImage(
                model = current,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = {
                    ShimmerContent(iconSize = iconSize)
                },
                error = {
                    val hasNext = currentIndex < candidates.lastIndex
                    if (hasNext && showRetry) {
                        // Coba host proxy berikutnya secara otomatis.
                        LaunchedEffect(current) { attempt = currentIndex + 1 }
                    }
                    FallbackContent(fallbackIcon, iconSize)
                }
            )
        }
    }
}

/** Versi ringkas tanpa shimmer (untuk daftar panjang). */
@Composable
fun SimpleMediaImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(12.dp),
    fallbackIcon: ImageVector = Icons.Rounded.Image,
    iconSize: Dp = 28.dp
) = MediaImage(
    url = url,
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = contentScale,
    shape = shape,
    fallbackIcon = fallbackIcon,
    iconSize = iconSize,
    showRetry = false
)

@Composable
private fun ShimmerContent(iconSize: Dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = androidx.compose.ui.geometry.Offset(
                        -400f + progress * 800f,
                        -400f + progress * 800f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        progress * 800f,
                        progress * 800f
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun FallbackContent(icon: ImageVector, iconSize: Dp) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.size(iconSize)
    )
}

/**
 * Empty state sederhana untuk daftar media.
 */
@Composable
fun MediaEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Rounded.Image,
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
/** Indikator loading tengah layar. */
@Composable
fun MediaLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/** Error state dengan tombol coba lagi. */
@Composable
fun MediaErrorState(
    message: String?,
    modifier: Modifier = Modifier,
    title: String = "Gagal memuat",
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Coba Lagi"
) {
    MediaEmptyState(
        title = title,
        modifier = modifier,
        message = message?.takeIf { it.isNotBlank() }
            ?: "Koneksi bermasalah. Periksa jaringan lalu coba lagi.",
        icon = Icons.Rounded.CloudOff,
        actionLabel = if (onRetry != null) retryLabel else null,
        onAction = onRetry
    )
}


/**
 * Cover gambar dengan placeholder native.
 * Dipakai untuk hero / banner yang butuh fallback ikon.
 */
@Composable
fun CoverImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(12.dp),
    fallbackIcon: ImageVector = Icons.Rounded.Image
) = MediaImage(
    url = url,
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = contentScale,
    shape = shape,
    fallbackIcon = fallbackIcon,
    iconSize = 28.dp,
    showRetry = false
)
