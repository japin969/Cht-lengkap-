package com.jayra.app.ui.component

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

@Composable
fun LiquidNavBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isFolded: Boolean,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val videoView = remember {
        object : VideoView(ctx) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasuredDimension(
                    MeasureSpec.getSize(widthMeasureSpec),
                    MeasureSpec.getSize(heightMeasureSpec)
                )
            }
        }.apply {
            setVideoURI(Uri.parse("android.resource://${ctx.packageName}/${R.raw.nav_bg}"))
            setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                mp.setVolume(0f, 0f)
                start()
            }
        }
    }
    DisposableEffect(Unit) { onDispose { videoView.stopPlayback() } }

    // Anti-lag: pause video saat app background
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            when (e) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> videoView.pause()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> videoView.start()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(selectedIndex) {
        scaleAnim.snapTo(1f)
        scaleAnim.animateTo(1.18f, spring(dampingRatio = 0.55f, stiffness = 420f))
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { videoView },
            update = { /* sengaja kosong — video tidak restart saat ganti tab */ },
            modifier = Modifier.fillMaxSize().scale(1.02f)
        )
        Box(Modifier.fillMaxSize().background(Color(0x2E000000)))
        Row(
            Modifier.fillMaxSize().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            jayraNavItems.forEachIndexed { i, item ->
                val selected = i == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                Column(
                    Modifier.weight(1f).height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Color(0x47FFFFFF) else Color.Transparent)
                        .clickable(interactionSource = interaction, indication = null) {
                            if (selectedIndex != i) onSelect(i)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp).graphicsLayer {
                            if (selected) {
                                scaleX = scaleAnim.value
                                scaleY = scaleAnim.value
                                rotationZ = when (item.label) {
                                    "Settings" -> 90f
                                    "Music" -> -6f
                                    else -> 0f
                                }
                            }
                        }
                    )
                    Text(
                        text = item.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.jayra.app.ui.theme.SFProFontFamily,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}
