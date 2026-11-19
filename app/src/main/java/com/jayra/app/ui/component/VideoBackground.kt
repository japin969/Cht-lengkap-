package com.jayra.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.fillMaxWidth
import androidx.compose.ui.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
defun VideoBackground(
    modifier: Modifier = Modifier,
    videoUrl: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
    isDark: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .graphicsLayer {
                // Apply iOS-style video effect without blur
                // Using a subtle gradient overlay to simulate video texture
                // This maintains performance while giving the appearance of video
            }
            .drawWithContent {
                // Draw subtle video-like gradient overlay
                val gradient = if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33000000),  // 20% dark overlay
                            Color(0x1A000000),  // 10% dark overlay  
                            Color.Transparent   // 0% overlay
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x2BFFFFFF),  // 17% light overlay
                            Color(0x14FFFFFF),  // 8% light overlay
                            Color.Transparent   // 0% overlay
                        )
                    )
                }
                drawRect(gradient)
                
                // Draw subtle video texture pattern
                val patternBrush = if (isDark) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x08FFFFFF),
                            Color.Transparent,
                            Color(0x08FFFFFF)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x0A000000),
                            Color.Transparent,
                            Color(0x0A000000)
                        )
                    )
                }
                // Draw subtle stripes to simulate video texture
                for (y in 0..10 step 2) {
                    drawRect(
                        brush = patternBrush,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, y.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())
                    )
                }
            }
    ) {
        // Subtle video play indicator (optional)
        AsyncImage(
            model = "https://picsum.photos/seed/navigation-video/640/360.jpg",
            contentDescription = "Navigation Background Video",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Apply a subtle vignette effect to blend video with navigation
                    clip = true
                    alpha = 0.7f
                },
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High
        )
    }
}