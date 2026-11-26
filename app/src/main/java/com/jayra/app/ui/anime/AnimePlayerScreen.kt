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
    val servers = remember {
        listOf(
            "Server 1 (Utama)" to videoUrl,
            "Server 2 (Cadangan)" to AnimeViewModel.SAMPLE_STREAM
        )
    }
    MediaVideoPlayer(
        videoUrl = videoUrl,
        accent = MediaColors.Anime,
        title = title,
        onBack = onBack,
        servers = servers
    )
}