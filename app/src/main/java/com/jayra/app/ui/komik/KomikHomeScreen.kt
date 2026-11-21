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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

@Composable
fun KomikHomeScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val cardBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subLabelColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue
    val borderColor = if (isDark) LiquidGlassSpecV2.DarkBorderTop else Color(0x14000000)

    val keyboardController = LocalSoftwareKeyboardController.current

    val glassTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
    val glassSpec = remember(glassTier, isDark) { LiquidGlassSpecV2.forTier(glassTier, isDark).surface }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(40.dp)
                .glassSurfaceV2(glassSpec, solidColor = if (isDark) IOSGray6Dark else Color(0xFFE5E5EA), cornerRadius = 12.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Cari Komik",
                tint = subLabelColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (state.searchQuery.isEmpty()) {
                    Text(
                        text = "Cari manga, manhwa, atau manhua...",
                        fontSize = 15.sp,
                        color = subLabelColor
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 15.sp, color = labelColor),
                    cursorBrush = SolidColor(accentBlue),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            viewModel.searchNow()
                        }
                    )
                )
            }
            if (state.searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Hapus",
                    tint = subLabelColor,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { viewModel.onSearchQueryChange("") }
                )
            }
        }

        if (state.searchQuery.isNotBlank()) {
            when {
                state.isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentBlue)
                }
                state.searchResults.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = JayraDangerRed, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Komik \"${state.searchQuery}\" tidak ditemukan",
                            color = subLabelColor,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.onSearchQueryChange("") },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Kembali ke Beranda", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.searchResults, key = { "sr_${it.resolvedSlug.ifBlank { it.cleanTitle }}" }) { komik ->
                        KomikCard(komik, isDark, labelColor, subLabelColor, accentBlue, glassSpec) {
                            onKomikClick(komik.resolvedSlug)
                        }
                    }
                }
            }
            return
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentBlue)
            }
            state.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 84.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = JayraDangerRed, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(state.error ?: "Gagal memuat komik", color = labelColor, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.loadHome(page = 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            else -> KomikHomeContent(
                state = state,
                glassSpec = glassSpec,
                isDark = isDark,
                cardBg = cardBg,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                accentBlue = accentBlue,
                borderColor = borderColor,
                onKomikClick = onKomikClick,
                onLoadNext = { viewModel.loadNextPage() }
            )
        }
    }
}

@Composable
private fun KomikHomeContent(
    state: KomikUiState,
    glassSpec: GlassSurfaceSpec,
    isDark: Boolean,
    cardBg: Color,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    borderColor: Color,
    onKomikClick: (String) -> Unit,
    onLoadNext: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val hero = state.items.firstOrNull()
        if (hero != null) {
            item(key = "hero") {
                KomikHeroCard(komik = hero, accentBlue = accentBlue, onClick = { onKomikClick(hero.resolvedSlug) })
            }
        }

        val rest = state.items.drop(1)
        if (rest.isNotEmpty()) {
            item(key = "section_populer") {
                KomikSectionHeader(title = "Populer", labelColor = labelColor, accentBlue = accentBlue)
            }
            Spacer(modifier = Modifier.height(8.dp))
            item(key = "row_populer") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rest.take(8), key = { "pop_${it.resolvedSlug.ifBlank { it.cleanTitle }}" }) { komik ->
                        KomikCard(komik, isDark, labelColor, subLabelColor, accentBlue, glassSpec) {
                            onKomikClick(komik.resolvedSlug)
                        }
                    }
                }
            }
        }

        val rest2 = state.items.drop(9)
        if (rest2.isNotEmpty()) {
            item(key = "section_terbaru") {
                KomikSectionHeader(title = "Update Terbaru", labelColor = labelColor, accentBlue = accentBlue)
            }
            Spacer(modifier = Modifier.height(8.dp))
            item(key = "row_terbaru") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rest2.take(8), key = { "upd_${it.resolvedSlug.ifBlank { it.cleanTitle }}" }) { komik ->
                        KomikCard(komik, isDark, labelColor, subLabelColor, accentBlue, glassSpec) {
                            onKomikClick(komik.resolvedSlug)
                        }
                    }
                }
            }
        }

        if (state.hasNext) {
            item(key = "load_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onLoadNext,
                        colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(0.75.dp, accentBlue, RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Muat Lebih Banyak",
                            color = accentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KomikSectionHeader(title: String, labelColor: Color, accentBlue: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = labelColor)
        Text(
            text = "Lihat Semua",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentBlue,
            modifier = Modifier.clickable { }
        )
    }
}

@Composable
fun KomikHeroCard(
    komik: KomikItem,
    accentBlue: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(210.dp)
            .clip(squircle(20))
            .clickable { onClick() }
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
                Brush.verticalGradient(colors = listOf(Color(0x1A000000), Color(0xAA000000)))
            )
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                komik.type?.let { t ->
                    if (t.isNotBlank()) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x55FFFFFF))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(t, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
                komik.latestChapter?.let { ch ->
                    if (ch.isNotBlank()) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accentBlue)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(ch, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = komik.cleanTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KomikCard(
    komik: KomikItem,
    isDark: Boolean,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    glassSpec: GlassSurfaceSpec,
    onClick: (String) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(116.dp).clickable { onClick(komik.resolvedSlug) }) {
        Box(
            modifier = Modifier
                .width(116.dp)
                .height(164.dp)
                .glassSurfaceV2(glassSpec, solidColor = if (isDark) IOSGray6Dark else IOSGray6, cornerRadius = 14.dp),
            contentAlignment = Alignment.Center
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
            komik.latestChapter?.let { ch ->
                if (ch.isNotBlank()) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                            .clip(RoundedCornerShape(6.dp)).background(accentBlue.copy(alpha = 0.92f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(ch, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    }
                }
            }
            komik.rating?.let { rate ->
                if (rate.isNotBlank()) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                            .clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material.icons.rounded.Star(
                                contentDescription = null,
                                tint = IOSOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(rate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = komik.cleanTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
        )
    }
}
