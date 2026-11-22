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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * UI kit bergaya iOS untuk Komik & Anime.
 *
 * Komponen:
 *  - [AppPageBackground]  : gradasi lembut, bukan putih polos
 *  - [JellyModifier]      : animasi mantul saat ditekan
 *  - [GlassSurface]       : efek kaca ringan (tanpa blur berat)
 *  - [AirEnterTransition] : transisi mengalir saat konten muncul
 *
 * Semua ringan: hanya alpA/scale, tidak ada RenderEffect blur.
 */

/** Warna tema bersama untuk Komik & Anime. */
object MediaTheme {
    /** Gradasi latar — menggantikan putih polos. */
    val BackgroundGradient: List<Color>
        @Composable get() = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            MaterialTheme.colorScheme.background
        )

    /** Aksen komik (oranye hangat). */
    val KomikAccent = Color(0xFFF97316)

    /** Aksen anime (hijau segar). */
    val AnimeAccent = Color(0xFF22C55E)

    /** Radius kartu iOS (besar & lentur). */
    val CardRadius: Dp = 18.dp

    /** Radius chip. */
    val ChipRadius: Dp = 50.dp
}

/**
 * Latar halaman dengan gradasi lembut.
 * Menggantikan background putih polos supaya tidak terlihat kosong.
 */
@Composable
fun AppPageBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(MediaTheme.BackgroundGradient))
    ) {
        content()
    }
}

/**
 * Modifier jelly: skala sedikit mengecil saat ditekan lalu mantul balik.
 * Dibuat dengan Animatable + spring, sangat ringan.
 */
@Composable
fun Modifier.jellyScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed, enabled) {
        val target = if (pressed && enabled) pressedScale else 1f
        scale.animateTo(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    return this
        .then(Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        })
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled
        ) {}
}

/**
 * Jelly click modifier yang meneruskan onClick.
 */
@Composable
fun Modifier.jellyClick(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.94f
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed, enabled) {
        scale.animateTo(
            targetValue = if (pressed && enabled) pressedScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Permukaan kaca ringan: background semi-transparan + border tipis.
 * Tanpa blur (RenderEffect) supaya tetap 60fps.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(MediaTheme.CardRadius),
    tint: Color = Color.White.copy(alpha = 0.10f),
    borderColor: Color = Color.White.copy(alpha = 0.18f),
    border: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(tint)
    .border(border, borderColor, shape)

/**
 * Transisi "air": konten masuk dengan fade + geser halus.
 */
@Composable
fun AirEnter(
    visible: Boolean = true,
    slideOffset: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        ) + androidx.compose.animation.slideInVertically(
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            initialOffsetY = { (slideOffset.value * 2).toInt() }
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        )
    ) {
        content()
    }
}

/**
 * Kartu iOS dengan shadow lembut + sudut membulat besar.
 */
@Composable
fun IosCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(MediaTheme.CardRadius),
    containerColor: Color = Color.White,
    elevation: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.jellyClick(onClick = onClick)
                else Modifier
            )
            .clip(shape)
            .background(containerColor)
            .shadowCompat(elevation, shape)
    ) {
        content()
    }
}

/** Shadow lembut (tanpa dependency Material3 shadow yang berat). */
private fun Modifier.shadowCompat(elevation: Dp, shape: Shape): Modifier =
    this.then(
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.10f),
            spotColor = Color.Black.copy(alpha = 0.10f)
        )
    )