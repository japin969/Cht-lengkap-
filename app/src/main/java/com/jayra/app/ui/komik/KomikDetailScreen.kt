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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
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
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.ui.theme.SquircleShape

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

    var sortAscending by remember { mutableStateOf(false) }
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
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = IOSRed, modifier = Modifier.size(44.dp))
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
                val detail = state.detail
                val orderedChapters = remember(state.chapters, sortAscending) {
                    if (sortAscending) state.chapters.reversed() else state.chapters
                }
                val firstChapter = state.chapters.lastOrNull()
                val latestChapter = state.chapters.firstOrNull()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(squircle(18))
                                .background(cardBg)
                                .border(0.75.dp, borderColor, squircle(18))
                                .padding(14.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(detail?.cover)
                                    .addHeader("Referer", "https://komikindo.ch/")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = detail?.cleanTitle,
                                modifier = Modifier
                                    .width(114.dp)
                                    .height(164.dp)
                                    .clip(squircle(12))
                                    .background(if (isDark) IOSGray6Dark else IOSGray6),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail?.cleanTitle ?: "Memuat...",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = labelColor,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    detail?.rating?.takeIf { it.isNotBlank() }?.let { r ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(IOSOrange.copy(alpha = 0.16f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Rounded.Star, contentDescription = null, tint = IOSOrange, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(r, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IOSOrange)
                                        }
                                    }
                                    detail?.statusText?.takeIf { it.isNotBlank() }?.let { st ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(accentBlue.copy(alpha = 0.14f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(st, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accentBlue)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                listOfNotNull(
                                    detail?.type?.takeIf { it.isNotBlank() }?.let { "Tipe: $it" },
                                    detail?.authorName?.takeIf { it.isNotBlank() }?.let { "Penulis: $it" },
                                    detail?.artistName?.takeIf { it.isNotBlank() }?.let { "Ilustrator: $it" }
                                ).forEach { line ->
                                    Text(line, fontSize = 12.sp, color = subLabelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    if (firstChapter != null || latestChapter != null) {
                        item(key = "quick_read") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                firstChapter?.let { ch ->
                                    Button(
                                        onClick = { ch.resolvedSlug.takeIf { it.isNotBlank() }?.let(onChapterClick) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Baca Awal", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                                latestChapter?.let { ch ->
                                    Button(
                                        onClick = { ch.resolvedSlug.takeIf { it.isNotBlank() }?.let(onChapterClick) },
                                        colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(0.75.dp, accentBlue, RoundedCornerShape(12.dp))
                                    ) {
                                        Text(
                                            text = ch.title ?: "Terbaru",
                                            color = accentBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val genres = detail?.genres.orEmpty()
                    if (genres.isNotEmpty()) {
                        item(key = "genres") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(genres, key = { it }) { g ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(cardBg)
                                            .border(0.75.dp, borderColor, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(g, fontSize = 12.sp, color = labelColor, fontWeight = FontWeight.Medium)
                                    }
                                }
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
                                .clickable { expandSynopsis = !expandSynopsis }
                                .padding(14.dp)
                        ) {
                            Text("Sinopsis", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = labelColor)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = detail?.synopsis?.takeIf { it.isNotBlank() } ?: "Sinopsis belum tersedia.",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = subLabelColor,
                                maxLines = if (expandSynopsis) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (expandSynopsis) "Tampilkan lebih sedikit" else "Baca selengkapnya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentBlue
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
                                text = "Daftar Chapter (${detail?.totalChapters ?: state.chapters.size})",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = labelColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cardBg)
                                    .border(0.75.dp, borderColor, RoundedCornerShape(10.dp))
                                    .clickable { sortAscending = !sortAscending }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.SwapVert, contentDescription = "Urutkan", tint = accentBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (sortAscending) "Ch 1 Dulu" else "Terbaru",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentBlue
                                )
                            }
                        }
                    }

                    items(
                        items = orderedChapters,
                        key = { it.resolvedSlug.ifBlank { it.title.orEmpty() } }
                    ) { ch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(squircle(14))
                                .background(cardBg)
                                .border(0.75.dp, borderColor, squircle(14))
                                .clickable {
                                    val target = ch.resolvedSlug
                                    if (target.isNotBlank()) onChapterClick(target)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = accentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ch.title ?: "Chapter",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = labelColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                ch.date?.takeIf { it.isNotBlank() }?.let { d ->
                                    Text(text = d, fontSize = 12.sp, color = subLabelColor)
                                }
                            }
                            Text("Baca", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accentBlue)
                        }
                    }
                }
            }
        }
    }
}
