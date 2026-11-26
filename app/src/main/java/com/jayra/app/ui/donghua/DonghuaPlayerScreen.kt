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
    // Metadata donghua (TVMaze) tidak menyediakan stream berlisensi.
    // Kalau URL bukan http valid, pakai demo CC BY 3.0 yang legal (bukan hitam).
    val isRealStream = remember(videoUrl) { videoUrl.startsWith("http") }
    val effectiveUrl = remember(videoUrl, isRealStream) {
        if (isRealStream) videoUrl else DonghuaViewModel.SAMPLE_STREAM
    }
    val servers = remember(videoUrl, isRealStream) {
        if (isRealStream) {
            listOf(
                "Server 1 (Utama)" to videoUrl,
                "Server 2 (Demo CC)" to DonghuaViewModel.SAMPLE_STREAM
            )
        } else {
            listOf("Demo (CC BY 3.0)" to DonghuaViewModel.SAMPLE_STREAM)
        }
    }
    MediaVideoPlayer(
        videoUrl = effectiveUrl,
        accent = MediaColors.Strem,
        title = if (isRealStream) title else "$title (demo)",
        onBack = onBack,
        servers = servers
    )
}