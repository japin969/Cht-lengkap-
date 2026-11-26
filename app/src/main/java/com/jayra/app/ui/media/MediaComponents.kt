package com.jayra.app.ui.media

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * Top bar standar untuk semua layar media: tombol back + judul + action opsional.
 * Pola sama seperti aplikasi media populer (Crunchyroll/Netflix/Webtoon).
 */
@Composable
fun MediaTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MediaColors.TextPrimary,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MediaColors.Bg)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = MediaColors.TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = MediaColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        actions()
    }
}

/**
 * Poster gambar dengan placeholder + error state yang tetap terlihat bagus
 * (pola aplikasi terkenal). Selalu Crop + aspect ratio konsisten supaya tidak gepeng.
 */
@Composable
fun MediaPoster(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    accent: Color = MediaColors.Comic,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MediaDimens.PosterRadius))
            .background(
                Brush.linearGradient(
                    listOf(MediaColors.SurfaceElevated, accent.copy(alpha = 0.10f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Shimmer placeholder untuk poster yang sedang dimuat. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(MediaDimens.PosterRadius)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(MediaColors.SurfaceHigh.copy(alpha = alpha))
    )
}

/**
 * State view seragam: loading (shimmer/spinner), empty, error + retry.
 * retry() benar-benar memanggil ulang pemuatan data.
 */
@Composable
fun MediaStateView(
    isLoading: Boolean,
    isEmpty: Boolean,
    error: String?,
    onRetry: () -> Unit,
    accent: Color,
    emptyTitle: String = "Belum ada konten",
    emptySubtitle: String = "Coba muat ulang.",
    content: @Composable () -> Unit
) {
    when {
        isLoading -> {
            Box(Modifier.fillMaxSize().background(MediaColors.Bg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        }
        error != null && isEmpty -> {
            CenterState(
                icon = { Icon(Icons.Default.Refresh, null, tint = accent, modifier = Modifier.size(34.dp)) },
                title = "Gagal memuat",
                subtitle = error,
                actionLabel = "Coba Lagi",
                onAction = onRetry,
                accent = accent
            )
        }
        isEmpty -> {
            CenterState(
                icon = { Icon(Icons.Default.Image, null, tint = accent, modifier = Modifier.size(34.dp)) },
                title = emptyTitle,
                subtitle = emptySubtitle,
                actionLabel = "Coba Lagi",
                onAction = onRetry,
                accent = accent
            )
        }
        else -> content()
    }
}

@Composable
fun CenterState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    accent: Color
) {
    Column(
        Modifier.fillMaxSize().background(MediaColors.Bg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(14.dp))
        Text(title, fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = MediaColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontFamily = SFProFontFamily, fontSize = 14.sp, color = MediaColors.TextSecondary, textAlign = TextAlign.Center)
        if (actionLabel.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(accent)
                    .clickable { onAction() }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(actionLabel, fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
            }
        }
    }
}

/** Judul section + tombol "Lihat semua" (pola aplikasi media populer). */
@Composable
fun SectionHeader(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = MediaDimens.ScreenPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MediaColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (onSeeAll != null) {
            Text(
                "Lihat semua",
                fontFamily = SFProFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = accent,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
    }
}

/** Chip filter/genre (pola aplikasi media populer). */
@Composable
fun FeatureChip(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) accent else MediaColors.SurfaceElevated)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            fontFamily = SFProFontFamily,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (selected) Color.Black else MediaColors.TextSecondary
        )
    }
}

/** Baris grid poster (3 kolom) dengan shimmer saat loading. */
@Composable
fun PosterGridPlaceholder(count: Int = 9, modifier: Modifier = Modifier) {
    val rows = (count + 2) / 3
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) {
                    ShimmerBox(
                        Modifier
                            .weight(1f)
                            .aspectRatio(0.7f)
                    )
                }
            }
        }
    }
}