package com.jayra.app.ui.media

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * Tema "iOS 18 Glass" untuk halaman Anime + Komik.
 *
 * Tujuan:
 *  - Latar gelap elegan + gradasi (bukan putih polos) — konsisten di semua
 *    halaman media (Home, Search, Detail, Play, Baca, List).
 *  - Warna glass untuk card, header, bottom bar, dan tombol yang menempel
 *    di atas gambar.
 *  - Tanpa blur berat (RenderEffect). Efek kaca memakai alpha + border tipis,
 *    jadi tetap ringan dan 60fps.
 *
 * Tema ini HANYA membungkus layar Anime + Komik. Chat, Music, Drama, dan
 * seluruh aplikasi memakai tema aslinya (tidak diubah).
 */
object MediaGlassColors {
    /** Latar dasar (near-black dengan nuansa biru). */
    val Background = Color(0xFF0A0A0F)

    /** Gradasi latar premium (atas → tengah → bawah). */
    val BackgroundGradient: List<Color> = listOf(
        Color(0xFF0B0B14),
        Color(0xFF141426),
        Color(0xFF0A0A0F)
    )

    /** Aksen lembut untuk glow gradasi. */
    val GlowIndigo = Color(0xFF2A2A4A)

    /** Permukaan card gelap. */
    val Surface = Color(0xFF16161F)

    /** Varian permukaan (input/chip). */
    val SurfaceVariant = Color(0xFF22222E)

    /** Teks utama. */
    val OnSurface = Color(0xFFF5F5F7)

    /** Teks sekunder. */
    val OnSurfaceVariant = Color(0xFFB9B9C6)

    /** Garis tipis. */
    val Outline = Color(0x33FFFFFF)

    /** Tint kaca untuk permukaan semi-transparan. */
    val GlassTint = Color(0x1FFFFFFF)

    /** Border kaca tipis. */
    val GlassBorder = Color(0x2EFFFFFF)

    /** Aksen iOS. */
    val Accent = Color(0xFF0A84FF)
}

/**
 * Skema warna gelap untuk media. Semua `MaterialTheme.colorScheme.*` di dalam
 * layar Anime/Komik akan memakai nilai ini.
 */
val MediaGlassColorScheme = darkColorScheme(
    primary = MediaGlassColors.Accent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1C1C2E),
    onPrimaryContainer = Color(0xFFEDEDF5),
    secondary = Color(0xFF5E5E7A),
    onSecondary = Color(0xFFFFFFFF),
    background = MediaGlassColors.Background,
    onBackground = MediaGlassColors.OnSurface,
    surface = MediaGlassColors.Surface,
    onSurface = MediaGlassColors.OnSurface,
    surfaceVariant = MediaGlassColors.SurfaceVariant,
    onSurfaceVariant = MediaGlassColors.OnSurfaceVariant,
    outline = MediaGlassColors.Outline,
    outlineVariant = Color(0x1AFFFFFF),
    error = Color(0xFFFF453A),
    onError = Color(0xFFFFFFFF)
)

/**
 * Bungkus layar Anime/Komik dengan tema glass gelap.
 * Mempertahankan font SF Pro dari tema aplikasi.
 */
@Composable
fun MediaGlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MediaGlassColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = SFProFontFamily)
        ) {
            content()
        }
    }
}