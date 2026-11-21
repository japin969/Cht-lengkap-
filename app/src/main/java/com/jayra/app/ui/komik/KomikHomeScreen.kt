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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.ui.theme.SquircleShape

@Composable
fun KomikHomeScreen(
    viewModel: KomikViewModel = viewModel(),
    onKomikClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val cardBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground
    val searchBg = if (isDark) IOSGray6Dark else Color(0xFFE5E5EA)
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subLabelColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue
    val borderColor = if (isDark) LiquidGlassSpecV2.DarkBorderTop else Color(0x14000000)

    val keyboardController = LocalSoftwareKeyboardController.current
    val categories = remember {
        listOf(
            "home" to "Beranda",
            "komik-terbaru" to "Terbaru",
            "pustaka" to "Pustaka",
            "berwarna" to "Berwarna",
            "genre" to "Genre",
            "daftar-manga" to "Daftar"
        )
    }
    val genres = remember {
        listOf("action", "adventure", "comedy", "drama", "fantasy", "isekai", "martial-arts", "romance")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // iOS Search Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(searchBg)
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
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 15.sp, color = labelColor),
                    cursorBrush = SolidColor(accentBlue),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
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

        // Mode Pencarian
        if (state.searchQuery.isNotBlank()) {
            when {
                state.isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentBlue)
                }
                state.searchResults.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Komik \"${state.searchQuery}\" tidak ditemukan",
                        color = subLabelColor,
                        fontSize = 15.sp
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.searchResults, key = { "sr_${it.resolvedSlug.ifBlank { it.cleanTitle }}" }) { komik ->
                        KomikGridCard(komik, isDark, labelColor, subLabelColor, accentBlue, onKomikClick)
                    }
                }
            }
            return
        }

        // Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.first }) { (key, label) ->
                val active = state.activeCategory == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) accentBlue else cardBg)
                        .border(0.75.dp, if (active) accentBlue else borderColor, RoundedCornerShape(20.dp))
                        .clickable { viewModel.loadCategory(key, page = 1) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) Color.White else labelColor
                    )
                }
            }
        }

        if (state.activeCategory == "genre") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genres, key = { it }) { g ->
                    val active = state.activeGenre == g
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) accentBlue.copy(alpha = 0.16f) else cardBg)
                            .border(0.75.dp, if (active) accentBlue else borderColor, RoundedCornerShape(14.dp))
                            .clickable { viewModel.selectGenre(g) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = g.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) accentBlue else labelColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentBlue)
            }
            state.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 84.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = IOSRed, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(state.error ?: "Gagal memuat komik", color = labelColor, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.loadCategory(state.activeCategory, page = 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            state.activeCategory == "home" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val hero = state.popular.firstOrNull() ?: state.latest.firstOrNull()
                    if (hero != null) {
                        item(key = "komik_hero") {
                            KomikHeroCard(hero, accentBlue) { onKomikClick(hero.resolvedSlug) }
                        }
                    }
                    if (state.popular.isNotEmpty()) {
                        item(key = "komik_popular") {
                            KomikSectionHeader("Komik Populer", labelColor, accentBlue) {
                                viewModel.loadCategory("pustaka", page = 1)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.popular, key = { "pop_${it.resolvedSlug.ifBlank { it.cleanTitle }}" }) { komik ->
                                    KomikPosterCard(komik, isDark, labelColor, subLabelColor, accentBlue, onKomikClick)
                                }
                            }
                        }
                    }
                    if (state.latest.isNotEmpty()) {
                        item(key = "komik_latest") {
                            KomikSectionHeader("Update Terbaru", labelColor, accentBlue) {
                                viewModel.loadCategory("komik-terbaru", page = 1)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.latest, key = { "lat_${it.resolvedSlug.ifBlank { it.cleanTitle }}" }) { komik ->
                                    KomikPosterCard(komik, isDark, labelColor, subLabelColor, accentBlue, onKomikClick)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.items, key = { "grid_${it.resolvedSlug.ifBlank { it.cleanTitle }}" }) { komik ->
                        KomikGridCard(komik, isDark, labelColor, subLabelColor, accentBlue, onKomikClick)
                    }
                    if (state.hasNext) {
                        item(span = { GridItemSpan(3) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = accentBlue)
                                } else {
                                    TextButton(onClick = { viewModel.loadNextPage() }) {
                                        Text("Muat Halaman Berikutnya", color = accentBlue, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KomikSectionHeader(title: String, labelColor: Color, accentBlue: Color, onSeeAll: () -> Unit) {
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
            modifier = Modifier.clickable { onSeeAll() }
        )
    }
}

@Composable
private fun KomikHeroCard(komik: KomikItem, accentBlue: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(200.dp)
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
                Brush.verticalGradient(colors = listOf(Color(0x22000000), Color(0xDD000000)))
            )
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                komik.type?.takeIf { it.isNotBlank() }?.let { t ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x55FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(t, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
                komik.latestChapter?.takeIf { it.isNotBlank() }?.let { ch ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accentBlue)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(ch, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = komik.cleanTitle,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Baca Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
private fun KomikPosterCard(
    komik: KomikItem,
    isDark: Boolean,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    onClick: (String) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(124.dp).clickable { onClick(komik.resolvedSlug) }) {
        Box(
            modifier = Modifier.width(124.dp).height(176.dp)
                .clip(squircle(14))
                .background(if (isDark) IOSGray6Dark else IOSGray6)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(komik.cover)
                    .addHeader("Referer", "https://komikindo.ch/")
                    .crossfade(true)
                    .build(),
                contentDescription = komik.cleanTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            komik.latestChapter?.takeIf { it.isNotBlank() }?.let { ch ->
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                        .clip(RoundedCornerShape(6.dp)).background(accentBlue.copy(alpha = 0.92f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(ch, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
            komik.type?.takeIf { it.isNotBlank() }?.let { t ->
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                        .clip(RoundedCornerShape(5.dp)).background(Color(0xAA000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(t, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = komik.cleanTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            lineHeight = 17.sp
        )
        komik.updatedAt?.takeIf { it.isNotBlank() }?.let { upd ->
            Text(upd, fontSize = 11.sp, color = subLabelColor, maxLines = 1)
        }
    }
}

@Composable
private fun KomikGridCard(
    komik: KomikItem,
    isDark: Boolean,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    onClick: (String) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick(komik.resolvedSlug) }) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f)
                .clip(squircle(12))
                .background(if (isDark) IOSGray6Dark else IOSGray6)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(komik.cover)
                    .addHeader("Referer", "https://komikindo.ch/")
                    .crossfade(true)
                    .build(),
                contentDescription = komik.cleanTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            komik.rating?.takeIf { it.isNotBlank() }?.let { rate ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
                        .clip(RoundedCornerShape(6.dp)).background(Color(0xBB000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = IOSOrange, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(rate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            komik.latestChapter?.takeIf { it.isNotBlank() }?.let { ch ->
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)
                        .clip(RoundedCornerShape(5.dp)).background(accentBlue.copy(alpha = 0.9f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(ch, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = komik.cleanTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            lineHeight = 15.sp
        )
        komik.updatedAt?.takeIf { it.isNotBlank() }?.let { upd ->
            Text(upd, fontSize = 10.sp, color = subLabelColor, maxLines = 1)
        }
    }
}
