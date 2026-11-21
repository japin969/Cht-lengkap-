package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.KomikItem
import com.jayra.app.ui.theme.IOSBlue
import com.jayra.app.ui.theme.IOSBlueDark
import com.jayra.app.ui.theme.IOSGray6
import com.jayra.app.ui.theme.IOSGray6Dark
import com.jayra.app.ui.theme.IOSLabel
import com.jayra.app.ui.theme.IOSLabelDark
import com.jayra.app.ui.theme.IOSOrange
import com.jayra.app.ui.theme.IOSSecondaryLabel
import com.jayra.app.ui.theme.IOSSecondaryLabelDark
import com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark
import com.jayra.app.ui.theme.IOSSecondarySystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackgroundDark
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.ui.theme.GlassSurfaceSpec
import com.jayra.app.ui.theme.squircle
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.util.glassSurfaceV2

@Composable
fun KomikReaderScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val cardBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subLabelColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue

    val glassTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
    val glassSpec = remember(glassTier, isDark) { LiquidGlassSpecV2.forTier(glassTier, isDark).surface }

    val komik = state.komik

    LaunchedEffect(slug) {
        viewModel.loadDetail(slug)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (komik == null && state.detailError == null && !state.isDetailLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = subLabelColor, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Memuat komik...", color = labelColor, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            return@Box
        }

        if (komik == null && state.detailError != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = JayraDangerRed, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(state.detailError ?: "Gagal memuat komik", color = labelColor, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.loadDetail(slug) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            return@Box
        }

        val currentKomik = komik!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(squircle(18))
                    .glassSurfaceV2(glassSpec, solidColor = if (isDark) IOSGray6Dark else IOSGray6, cornerRadius = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentKomik.cover)
                        .addHeader("Referer", "https://komikindo.ch/")
                        .crossfade(true)
                        .build(),
                    contentDescription = currentKomik.cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                ) {
                    Text(
                        text = currentKomik.cleanTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val type = currentKomik.type
                if (type != null && type.isNotBlank()) {
                    Text(
                        text = type,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = subLabelColor
                    )
                }
                val rating = currentKomik.rating
                if (rating != null && rating.isNotBlank()) {
                    Text(
                        text = "$rating",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IOSOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(squircle(14))
                    .background(cardBg)
                    .border(0.75.dp, if (isDark) LiquidGlassSpecV2.DarkBorderTop else Color(0x14000000), squircle(14))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = subLabelColor, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Baca Chapter",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gambar chapter belum tersedia dari API komik saat ini.",
                        color = subLabelColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val link = currentKomik.link
                            if (link != null && link.isNotBlank()) {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(link)
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Buka Chapter di Komikindo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
