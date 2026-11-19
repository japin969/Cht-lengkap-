package com.jayra.app.ui.component

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.TextureView
import android.view.Surface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jayra.app.R

data class JayraNavItem(
    val label: String,
    @androidx.annotation.DrawableRes val iconRes: Int
)

val jayraNavItems = listOf(
    JayraNavItem("Chat", R.drawable.ic_chat),
    JayraNavItem("Calls", R.drawable.ic_panggilan),
    JayraNavItem("Music", R.drawable.ic_music),
    JayraNavItem("Hub", R.drawable.ic_hub),
    JayraNavItem("Settings", R.drawable.ic_setting)
)

/**
 * Bottom nav "liquid glass" DENGAN video background (res/raw/nav_bg.mp4).
 *
 * Layer dari bawah ke atas:
 *  1. Fallback gradient gelap (kalau video gagal dimuat, nav tidak pernah hitam polos)
 *  2. Video nav_bg.mp4 (TextureView, muted, looping, center-crop)
 *  3. Scrim gelap supaya ikon & label tetap terbaca
 *  4. Sheen + border kaca + item navigasi
 *
 * Kenapa TextureView, bukan VideoView?
 *  - VideoView = SurfaceView: tidak ikut ter-clip rounded corner Compose dan bisa
 *    "menembus" layer lain. TextureView aman di dalam Compose dan tetap ringan
 *    karena areanya cuma 64dp.
 *
 * Anti-lag:
 *  - satu instance player saja, tidak restart saat ganti tab
 *  - pause otomatis saat app di background, resume saat app kembali
 *  - tidak ada blur / RenderEffect
 */
@Composable
fun LiquidNavBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isFolded: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val context = LocalContext.current

    val fallbackTop = Color(0xFF1B2733)
    val fallbackBottom = Color(0xFF0E1621)
    val borderColor = Color(0x4DFFFFFF)
    val activeColor = Color.White
    val inactiveColor = Color(0xB3FFFFFF)

    val videoPlayer = remember(context) { NavVideoPlayer(context.applicationContext) }
    val textureView = remember(context) {
        val view = CropTextureView(context)
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                videoPlayer.bind(view, st)
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                view.applyCrop()
            }

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                videoPlayer.releasePlayer()
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }
        view
    }

    DisposableEffect(videoPlayer) {
        onDispose { videoPlayer.releasePlayer() }
    }

    // Hemat baterai: pause saat app background, lanjut saat app kembali.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> videoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> videoPlayer.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 3.dp, shape = shape, clip = false)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(fallbackTop, fallbackBottom)))
    ) {
        // Layer 1: video background
        AndroidView(
            factory = { textureView },
            // update kosong: video tidak di-restart setiap ganti tab.
            update = { },
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: scrim supaya teks/ikon selalu kontras
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x99000000), Color(0x66000000))))
        )

        // Layer 3: sheen + border kaca
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0x1FFFFFFF), Color(0x0AFFFFFF), Color.Transparent)
                        )
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.28f),
                        start = Offset(0f, 1f),
                        end = Offset(size.width, 1f),
                        strokeWidth = 1.5f
                    )
                }
                .border(width = 1.dp, color = borderColor, shape = shape)
        )

        // Layer 4: item navigasi (bentuk & posisi tidak diubah)
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            jayraNavItems.forEachIndexed { i, item ->
                val selected = i == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                val pillShape = remember { RoundedCornerShape(16.dp) }
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.95f,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                    label = "nav-scale-$i"
                )

                Column(
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(pillShape)
                        .background(if (selected) Color(0x2EFFFFFF) else Color.Transparent)
                        .clickable(
                            interactionSource = interaction,
                            indication = LocalIndication.current
                        ) {
                            if (selectedIndex != i) onSelect(i)
                        }
                        .scale(scale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        tint = if (selected) activeColor else inactiveColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = if (selected) activeColor else inactiveColor,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = com.jayra.app.ui.theme.SFProFontFamily,
                        lineHeight = 10.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    // Dot kecil penanda tab aktif
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(if (selected) activeColor else Color.Transparent)
                    )
                }
            }
        }
    }
}

/** TextureView yang meng-crop video ala "cover" (tidak gepeng). */
private class CropTextureView(context: Context) : TextureView(context) {

    private var videoWidth = 0
    private var videoHeight = 0

    fun setVideoSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            videoWidth = width
            videoHeight = height
            applyCrop()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyCrop()
    }

    fun applyCrop() {
        val vw = videoWidth
        val vh = videoHeight
        if (vw <= 0 || vh <= 0 || width <= 0 || height <= 0) return
        val scaleFactor = maxOf(width.toFloat() / vw, height.toFloat() / vh)
        val dx = (width - vw * scaleFactor) / 2f
        val dy = (height - vh * scaleFactor) / 2f
        val matrix = Matrix()
        matrix.setScale(scaleFactor, scaleFactor)
        matrix.postTranslate(dx, dy)
        setTransform(matrix)
    }
}

/** Pemutar video nav: muted, looping, dan aman di-pause/resume. */
private class NavVideoPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var prepared = false

    fun bind(textureView: CropTextureView, texture: SurfaceTexture) {
        releasePlayer()
        surface = Surface(texture)
        try {
            context.resources.openRawResourceFd(R.raw.nav_bg)?.use { afd ->
                player = MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    setSurface(surface)
                    isLooping = true
                    setVolume(0f, 0f) // muted: syarat autoplay + hemat baterai
                    setOnPreparedListener { mp ->
                        prepared = true
                        textureView.setVideoSize(mp.videoWidth, mp.videoHeight)
                        mp.start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                    prepareAsync()
                }
            }
        } catch (_: Exception) {
            releasePlayer()
        }
    }

    fun pause() {
        prepared = prepared && player != null
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    fun resume() {
        if (prepared) runCatching { player?.start() }
    }

    fun releasePlayer() {
        prepared = false
        runCatching { player?.release() }
        player = null
        runCatching { surface?.release() }
        surface = null
    }
}
