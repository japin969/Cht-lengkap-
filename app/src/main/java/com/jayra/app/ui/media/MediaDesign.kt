package com.jayra.app.ui.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens untuk tiga fitur media (comic, anime, strem/donghua).
 * Semua teks memakai SF Pro (default LocalTextStyle dari JayraTheme).
 * Dark mode: background gelap, card gelap, teks putih.
 *
 * Setiap fitur punya warna accent sendiri supaya user langsung tahu sedang
 * berada di fitur mana hanya dari warna (pola aplikasi terkenal:
 * Webtoon = hijau, Crunchyroll = oranye, Bilibili = pink).
 */
object MediaColors {
    // ── Dark surfaces (dipakai semua fitur) ──
    val Bg = Color(0xFF0B0B0F)
    val Surface = Color(0xFF15151C)
    val SurfaceElevated = Color(0xFF1E1E27)
    val SurfaceHigh = Color(0xFF262631)
    val Border = Color(0xFF2E2E3A)

    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFA6A6B3)
    val TextTertiary = Color(0xFF6E6E7A)

    val Danger = Color(0xFFFF453A)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xFFFFD60A)

    // ── Accent per fitur ──
    // Comic → hijau segar (seperti Webtoon/Tapas)
    val Comic = Color(0xFF3DD68C)
    val ComicDim = Color(0xFF1B3A2C)

    // Anime → oranye (seperti Crunchyroll)
    val Anime = Color(0xFFFF7A1A)
    val AnimeDim = Color(0xFF3A2413)

    // Strem / Donghua → pink (seperti Bilibili)
    val Strem = Color(0xFFFF5C8A)
    val StremDim = Color(0xFF3A1B26)

    fun accentFor(feature: MediaFeature): Color = when (feature) {
        MediaFeature.COMIC -> Comic
        MediaFeature.ANIME -> Anime
        MediaFeature.STREM -> Strem
    }

    fun accentDimFor(feature: MediaFeature): Color = when (feature) {
        MediaFeature.COMIC -> ComicDim
        MediaFeature.ANIME -> AnimeDim
        MediaFeature.STREM -> StremDim
    }
}

enum class MediaFeature { COMIC, ANIME, STREM }

/** Spacing & ukuran standar (mirip aplikasi media populer). */
object MediaDimens {
    val ScreenPadding = 16.dp
    val CardRadius = 12.dp
    val PosterRadius = 10.dp
    val PosterW = 118.dp
    val PosterH = 168.dp
    val RowSpacing = 12.dp
    val SectionSpacing = 22.dp
    val BottomBarHeight = 64.dp
}