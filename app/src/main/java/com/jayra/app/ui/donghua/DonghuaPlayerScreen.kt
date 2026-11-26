package com.jayra.app.ui.donghua

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaVideoPlayer

/**
 * DonghuaPlayer — memakai MediaVideoPlayer (ExoPlayer) robust.
 * Server alternatif pakai sample publik agar "ganti server" berfungsi.
 */
@Composable
fun DonghuaPlayerScreen(
    videoUrl: String,
    title: String = "Memutar Donghua",
    onBack: () -> Unit
) {
    val servers = remember {
        listOf(
            "Server 1 (Utama)" to videoUrl,
            "Server 2 (Cadangan)" to DonghuaViewModel.SAMPLE_STREAM
        )
    }
    MediaVideoPlayer(
        videoUrl = videoUrl,
        accent = MediaColors.Strem,
        title = title,
        onBack = onBack,
        servers = servers
    )
}