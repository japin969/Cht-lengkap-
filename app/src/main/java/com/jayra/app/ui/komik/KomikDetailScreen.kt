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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.jayra.app.ui.theme.IOSRed
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
import kotlinx.coroutines.launch

@Composable
fun KomikDetailScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onChapterClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val cardBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subLabelColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue
    val borderColor = if (isDark) LiquidGlassSpecV2.DarkBorderTop else Color(0x14000000)

    val glassTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
    val glassSpec = remember(glassTier, isDark) { LiquidGlassSpecV2.forTier(glassTier, isDark).surface }

    var expandSynopsis by remember { mutableStateOf(false) }

    LaunchedEffect(slug) { viewModel.loadDetail(slug) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when {
            state.isDetailLoading -> CircularProgressIndicator(
                color = accentBlue,
                modifier = Modifier.align(Alignment.Center)
            )
            state.detailError != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = JayraDangerRed, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(state.detailError ?: "Gagal memuat detail", color = labelColor, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.loadDetail(slug) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            else -> {
                val komik = state.komik
                if (komik == null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                                imageVector = MenuBook,
                                contentDescription = null,
                                tint = subLabelColor,
                                modifier = Modifier.size(44.dp)
                            )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Komik tidak ditemukan", color = labelColor, fontSize = 15.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    KomikDetailContent(
                        komik = komik,
                        glassSpec = glassSpec,
                        isDark = isDark,
                        cardBg = cardBg,
                        labelColor = labelColor,
                        subLabelColor = subLabelColor,
                        accentBlue = accentBlue,
                        borderColor = borderColor,
                        expandSynopsis = expandSynopsis,
                        onExpandSynopsis = { expandSynopsis = it },
                        onChapterClick = onChapterClick,
                        onReadExternal = {
                            val link = komik.link
                            if (link != null && link.isNotBlank()) {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(link)
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KomikDetailContent(
    komik: KomikItem,
    glassSpec: GlassSurfaceSpec,
    isDark: Boolean,
    cardBg: Color,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    borderColor: Color,
    expandSynopsis: Boolean,
    onExpandSynopsis: (Boolean) -> Unit,
    onChapterClick: (String) -> Unit,
    onReadExternal: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(squircle(20))
                    .clickable { onReadExternal() }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(komik.cover)
                        .addHeader("Referer", "https://komikindo.ch/")
                        .crossfade(true)
                        .build(),
                    contentDescription = komik.cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(colors = listOf(Color(0x0A000000), Color(0x80000000)))
                    )
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val type = komik.type
                        if (type != null && type.isNotBlank()) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x55FFFFFF))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(type, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                        val latestChapter = komik.latestChapter
                        if (latestChapter != null && latestChapter.isNotBlank()) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accentBlue)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(latestChapter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        val rating = komik.rating
                        if (rating != null && rating.isNotBlank()) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(IOSOrange.copy(alpha = 0.95f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(rating, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = komik.cleanTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onReadExternal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Baca di Komikindo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF007AFF)
                            )
                        }
                    }
                }
            }
        }

        item(key = "info") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val type = komik.type
                if (type != null && type.isNotBlank()) {
                    Text(
                        text = type,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = subLabelColor
                    )
                }
                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = subLabelColor
                )
                val rating = komik.rating
                if (rating != null && rating.isNotBlank()) {
                    Text(
                        text = "Rating $rating",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = subLabelColor
                    )
                }
            }
        }

        item(key = "synopsis") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(squircle(16))
                    .background(cardBg)
                    .border(0.75.dp, borderColor, squircle(16))
                    .clickable { onExpandSynopsis(!expandSynopsis) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sinopsis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = labelColor)
                    Text(
                        text = if (expandSynopsis) "Tampilkan lebih sedikit" else "Baca selengkapnya",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentBlue
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sinopsis komik ini akan tersedia setelah data detail komik dimuat dari server.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = subLabelColor,
                    maxLines = if (expandSynopsis) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        item(key = "ch_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Daftar Chapter",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
                Text(
                    text = "Buka di Komikindo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentBlue,
                    modifier = Modifier.clickable { onReadExternal() }
                )
            }
        }

        item(key = "ch_empty") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(squircle(14))
                    .background(cardBg)
                    .border(0.75.dp, borderColor, squircle(14))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = subLabelColor, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chapter belum tersedia dari API komik",
                        color = subLabelColor,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Klik \"Buka di Komikindo\" di atas untuk membaca chapter secara langsung.",
                        color = subLabelColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
