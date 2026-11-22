package com.jayra.app.ui.media

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * UI kit media (Komik + Anime) — iOS-inspired, native Compose.
 *
 * Prinsip:
 *  - Poster/cover WAJIB gambar asli (Coil). Placeholder hanya saat loading/error.
 *  - Tidak ada blur berat. Hanya alpha + scale (ringan, 60fps).
 *  - Tidak ada dependency baru.
 */

/** Token warna iOS untuk media. */
object MediaUiTheme {
    val CardRadius: Dp = 18.dp
    val ChipRadius: Dp = 50.dp
    val AccentAnime = Color(0xFF22C55E)
    val AccentKomik = Color(0xFFF97316)

    /** Gradasi latar — menghindari putih polos. */
    val PageBackground: List<Color>
        @Composable get() = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.background
        )
}

/**
 * Gambar dengan placeholder native.
 *
 * - loading -> ikon + background lembut
 * - error   -> ikon BrokenImage
 * - sukses  -> gambar asli
 *
 * Memakai AsyncImage biasa (bukan Subcompose) agar request dimulai segera
 * (bukan kotak putih).
 */
@Composable
fun MediaImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(MediaUiTheme.CardRadius),
    fallbackIcon: ImageVector = Icons.Rounded.Image,
    iconSize: Dp = 30.dp
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(iconSize)
            )
        } else {
            // Placeholder statis (ikon) di belakang. AsyncImage biasa dipakai
            // supaya request MULAI SEGERA saat item pertama di-compose — tidak
            // menunggu di-scroll. SubcomposeAsyncImage dihindari karena
            // menunda request sampai item benar-benar di-layout, sehingga
            // poster tampil putih sampai user menggeser.
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
                modifier = Modifier.size(iconSize)
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(false)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                imageLoader = LocalMediaImageLoader.current
            )
        }
    }
}

/** App bar iOS: back + judul + action. */
@Composable
fun MediaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                JellyIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Kembali",
                    onClick = onBack
                )
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
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
            actions()
        }
    }
}

/** Tombol ikon dengan animasi jelly. */
@Composable
fun JellyIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        scale.animateTo(
            if (pressed) 0.88f else 1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Chip info. */
@Composable
fun MediaChip(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(MediaUiTheme.ChipRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

/** Chip filter yang bisa dipilih. */
@Composable
fun MediaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        scale.animateTo(
            if (pressed) 0.92f else 1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(MediaUiTheme.ChipRadius))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        shape = RoundedCornerShape(MediaUiTheme.ChipRadius),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            maxLines = 1
        )
    }
}

/** Header section. */
@Composable
fun MediaSectionHeader(
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

/** Loading state. */
@Composable
fun MediaLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Empty state. */
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
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
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
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Error state + Retry. */
@Composable
fun MediaErrorState(
    message: String?,
    modifier: Modifier = Modifier,
    title: String = "Gagal memuat",
    onRetry: (() -> Unit)? = null
) {
    MediaEmptyState(
        title = title,
        modifier = modifier,
        message = message?.takeIf { it.isNotBlank() }
            ?: "Koneksi bermasalah. Periksa jaringan lalu coba lagi.",
        icon = Icons.Rounded.ErrorOutline,
        actionLabel = if (onRetry != null) "Coba Lagi" else null,
        onAction = onRetry
    )
}

// ═══════════════════════════════════════════════════════════════
// DEDUPLICATION
// ═══════════════════════════════════════════════════════════════

/**
 * Kunci dedup longgar: judul dinormalisasi + slug/id.
 * Judul dibersihkan dari prefix "Komik " dan tanda baca.
 */
fun normalizeKey(title: String?): String {
    return title
        ?.removePrefix("Komik ")
        ?.lowercase()
        ?.replace(Regex("[^a-z0-9]+"), "")
        ?: ""
}

/**
 * Gabungkan beberapa list, buang duplikat.
 * Bila judul sama, item dengan data lebih lengkap (image/cover terisi) diprioritaskan.
 */
fun <T> mergeDedupe(
    lists: List<List<T>>,
    keyOf: (T) -> String,
    scoreOf: (T) -> Int
): List<T> {
    val best = LinkedHashMap<String, T>()
    lists.flatten().forEach { item ->
        val key = keyOf(item)
        if (key.isBlank()) return@forEach
        val existing = best[key]
        if (existing == null || scoreOf(item) > scoreOf(existing)) {
            best[key] = item
        }
    }
    return best.values.toList()
}

/** Alias untuk kompatibilitas: NetworkImage = MediaImage. */
@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: ImageVector = Icons.Rounded.Image,
    iconSize: Dp = 30.dp,
    shape: Shape = RoundedCornerShape(MediaUiTheme.CardRadius)
) = MediaImage(
    url = url,
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = contentScale,
    shape = shape,
    fallbackIcon = fallbackIcon,
    iconSize = iconSize
)

/**
 * Tombol ikon bulat dengan animasi jelly (alias [JellyIconButton]).
 * Disediakan di sini agar dapat dipakai baik oleh Komik maupun Anime.
 */
@Composable
fun MediaIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    JellyIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        active = active
    )
}
