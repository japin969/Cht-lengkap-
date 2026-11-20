package com.jayra.app.ui.anime

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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.jayra.app.data.AnimeSearchItem
import com.jayra.app.ui.theme.IOSBlue
import com.jayra.app.ui.theme.IOSBlueDark
import com.jayra.app.ui.theme.IOSGray6
import com.jayra.app.ui.theme.IOSGray6Dark
import com.jayra.app.ui.theme.IOSLabel
import com.jayra.app.ui.theme.IOSLabelDark
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
fun AnimeHomeScreen(
    viewModel: AnimeViewModel = viewModel(),
    onAnimeClick: (String) -> Unit
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
    val categories = remember { listOf("Semua", "Sedang Tayang", "Populer", "Tamat") }
    var selectedCategory by remember { mutableStateOf("Semua") }

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
                .clip(RoundedCornerShape(12.dp))
                .background(searchBg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Cari Anime",
                tint = subLabelColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (state.searchQuery.isEmpty()) {
                    Text(text = "Cari judul anime...", fontSize = 15.sp, color = subLabelColor)
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
                        text = "Anime \"${state.searchQuery}\" tidak ditemukan",
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
                    itemsIndexed(state.searchResults, key = { idx, it -> "sr_${idx}_${it.link}" }) { _, anime ->
                        AnimeGridCard(anime, isDark, labelColor, subLabelColor, accentBlue) {
                            anime.link?.let(onAnimeClick)
                        }
                    }
                }
            }
            return
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it }) { cat ->
                val active = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) accentBlue else cardBg)
                        .border(0.75.dp, if (active) accentBlue else borderColor, RoundedCornerShape(20.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) Color.White else labelColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
                    Text(state.error ?: "Gagal memuat anime", color = labelColor, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.loadHome() },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            selectedCategory != "Semua" -> {
                val list = when (selectedCategory) {
                    "Sedang Tayang" -> state.ongoing
                    "Populer" -> state.popular
                    else -> state.completed
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(list, key = { idx, it -> "cat_${idx}_${it.link}" }) { _, anime ->
                        AnimeGridCard(anime, isDark, labelColor, subLabelColor, accentBlue) {
                            anime.link?.let(onAnimeClick)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val hero = state.ongoing.firstOrNull() ?: state.popular.firstOrNull()
                    if (hero != null) {
                        item(key = "hero") {
                            AnimeHeroCard(hero, accentBlue) { hero.link?.let(onAnimeClick) }
                        }
                    }
                    if (state.ongoing.isNotEmpty()) {
                        item(key = "ongoing_section") {
                            AnimeSectionHeader("Sedang Tayang", labelColor, accentBlue) { selectedCategory = "Sedang Tayang" }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(state.ongoing, key = { idx, it -> "og_${idx}_${it.link}" }) { _, anime ->
                                    AnimePosterCard(anime, isDark, labelColor, subLabelColor, accentBlue) {
                                        anime.link?.let(onAnimeClick)
                                    }
                                }
                            }
                        }
                    }
                    if (state.popular.isNotEmpty()) {
                        item(key = "popular_section") {
                            AnimeSectionHeader("Anime Populer", labelColor, accentBlue) { selectedCategory = "Populer" }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(state.popular, key = { idx, it -> "pop_${idx}_${it.link}" }) { _, anime ->
                                    AnimePosterCard(anime, isDark, labelColor, subLabelColor, accentBlue) {
                                        anime.link?.let(onAnimeClick)
                                    }
                                }
                            }
                        }
                    }
                    if (state.completed.isNotEmpty()) {
                        item(key = "completed_section") {
                            AnimeSectionHeader("Tamat / Rekomendasi", labelColor, accentBlue) { selectedCategory = "Tamat" }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(state.completed, key = { idx, it -> "cmp_${idx}_${it.link}" }) { _, anime ->
                                    AnimePosterCard(anime, isDark, labelColor, subLabelColor, accentBlue) {
                                        anime.link?.let(onAnimeClick)
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
private fun AnimeSectionHeader(title: String, labelColor: Color, accentBlue: Color, onSeeAll: () -> Unit) {
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
private fun AnimeHeroCard(anime: AnimeSearchItem, accentBlue: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(210.dp)
            .clip(SquircleShape(20.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(anime.image).crossfade(true).build(),
            contentDescription = anime.title,
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
                anime.displayBadge?.let { badge ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accentBlue)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(badge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                anime.type?.takeIf { it.isNotBlank() }?.let { type ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x55FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(type, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = anime.title ?: "Anime Pilihan",
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
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tonton Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
fun AnimePosterCard(
    anime: AnimeSearchItem,
    isDark: Boolean = isSystemInDarkTheme(),
    labelColor: Color = if (isDark) IOSLabelDark else IOSLabel,
    subLabelColor: Color = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel,
    accentBlue: Color = if (isDark) IOSBlueDark else IOSBlue,
    onClick: (String) -> Unit
) {
    Column(modifier = Modifier.width(124.dp).clickable { onClick(anime.link ?: "") }) {
        Box(
            modifier = Modifier.width(124.dp).height(176.dp)
                .clip(SquircleShape(14.dp))
                .background(if (isDark) IOSGray6Dark else IOSGray6)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(anime.image).crossfade(true).build(),
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            anime.displayBadge?.let { badge ->
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                        .clip(RoundedCornerShape(6.dp)).background(accentBlue.copy(alpha = 0.92f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = anime.title ?: "Unknown",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
        )
        anime.type?.takeIf { it.isNotBlank() }?.let { t ->
            Text(t, fontSize = 11.sp, color = subLabelColor, maxLines = 1)
        }
    }
}

@Composable
private fun AnimeGridCard(
    anime: AnimeSearchItem,
    isDark: Boolean,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f)
                .clip(SquircleShape(12.dp))
                .background(if (isDark) IOSGray6Dark else IOSGray6)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(anime.image).crossfade(true).build(),
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            anime.displayBadge?.let { badge ->
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                        .clip(RoundedCornerShape(5.dp)).background(accentBlue.copy(alpha = 0.9f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = anime.title ?: "",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )
    }
}
