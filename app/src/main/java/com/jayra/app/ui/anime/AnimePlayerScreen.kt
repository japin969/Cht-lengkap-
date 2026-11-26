package com.jayra.app.ui.anime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaVideoPlayer

/**
 * AnimePlayer — memakai MediaVideoPlayer (ExoPlayer) yang robust:
 * loading jelas, error + retry + ganti server, kontrol lengkap,
 * lifecycle benar, posisi tersimpan saat rotasi.
 *
 * Server alternatif memakai sample publik agar "ganti server" berfungsi.
 */
@Composable
fun AnimePlayerScreen(
    videoUrl: String,
    title: String = "Memutar Anime",
    onBack: () -> Unit
) {
    // Kitsu (sumber metadata anime) tidak menyediakan stream berlisensi.
    // Kalau URL episode bukan URL http yang valid (mis. hanya id anime),
    // jangan paksa play ke ExoPlayer (itu bikin "source error" / hitam).
    // Pakai demo CC BY 3.0 (BigBuckBunny, legal) sebagai fallback yang jelas.
    val isRealStream = remember(videoUrl) { videoUrl.startsWith("http") }
    val effectiveUrl = remember(videoUrl, isRealStream) {
        if (isRealStream) videoUrl else AnimeViewModel.SAMPLE_STREAM
    }
    val servers = remember(videoUrl, isRealStream) {
        if (isRealStream) {
            listOf(
                "Server 1 (Utama)" to videoUrl,
                "Server 2 (Demo CC)" to AnimeViewModel.SAMPLE_STREAM
            )
        } else {
            listOf("Demo (CC BY 3.0)" to AnimeViewModel.SAMPLE_STREAM)
        }
    }
    MediaVideoPlayer(
        videoUrl = effectiveUrl,
        accent = MediaColors.Anime,
        title = if (isRealStream) title else "$title (demo)",
        onBack = onBack,
        servers = servers
    )
}