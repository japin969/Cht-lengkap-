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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subLabelColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue
    val cardBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground
    val borderColor = if (isDark) Color(0x14FFFFFF) else Color(0x14000000)
    val glassTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
    val glassSpec = remember(glassTier, isDark) { LiquidGlassSpecV2.forTier(glassTier, isDark).surface }
    val context = LocalContext.current

    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.searchQuery) {
        if (state.searchQuery.isNotEmpty() && searchText != state.searchQuery) {
            searchText = state.searchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(44.dp)
                .glassSurfaceV2(glassSpec, solidColor = if (isDark) IOSGray6Dark else IOSGray6, cornerRadius = 12.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Cari Komik",
                tint = subLabelColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f).focusRequester(focusRequester)) {
                if (searchText.isEmpty() && !state.isSearching && state.searchResults.isEmpty()) {
                    Text(
                        text = "Cari manga, manhwa, manhua...",
                        fontSize = 15.sp,
                        color = subLabelColor
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = labelColor),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(accentBlue),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            viewModel.search(searchText)
                        }
                    )
                )
            }
            if (searchText.isNotEmpty() || state.searchResults.isNotEmpty()) {
                IconButton(
                    onClick = {
                        searchText = ""
                        viewModel.clearSearch()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus",
                        tint = subLabelColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Content
        if (state.searchQuery.isNotBlank() || state.searchResults.isNotEmpty()) {
            SearchResultsSection(
                state = state,
                accentBlue = accentBlue,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                glassSpec = glassSpec,
                isDark = isDark,
                onKomikClick = onKomikClick
            )
        } else {
            HomeContentSection(
                state = state,
                accentBlue = accentBlue,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                cardBg = cardBg,
                borderColor = borderColor,
                glassSpec = glassSpec,
                isDark = isDark,
                context = context,
                onKomikClick = onKomikClick,
                onLoadNext = { viewModel.loadNextPage() }
            )
        }
    }
}

@Composable
private fun SearchResultsSection(
    state: KomikUiState,
    accentBlue: Color,
    labelColor: Color,
    subLabelColor: Color,
    glassSpec: GlassSurfaceSpec,
    isDark: Boolean,
    onKomikClick: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hasil pencarian: \"${state.searchQuery}\"",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
                if (state.searchResults.isNotEmpty()) {
                    Text(
                        text = "${state.searchResults.size} komik",
                        fontSize = 13.sp,
                        color = subLabelColor
                    )
                }
            }
        }

        when {
            state.isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentBlue)
            }
            state.searchResults.isEmpty() && state.error != null -> ErrorView(
                message = state.error ?: "Terjadi kesalahan",
                accentBlue = accentBlue,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                onRetry = { /* retry handled by viewModel */ }
            )
            state.searchResults.isEmpty() -> EmptyView(
                title = "Tidak ditemukan",
                subtitle = "Coba kata kunci lain",
                icon = Icons.Rounded.SearchOff,
                accentBlue = accentBlue,
                labelColor = labelColor,
                subLabelColor = subLabelColor
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.searchResults.size) { index ->
                    val komik = state.searchResults[index]
                    KomikCard(
                        komik = komik,
                        isDark = isDark,
                        labelColor = labelColor,
                        subLabelColor = subLabelColor,
                        accentBlue = accentBlue,
                        glassSpec = glassSpec,
                        context = LocalContext.current
                    ) {
                        onKomikClick(komik.resolvedSlug)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContentSection(
    state: KomikUiState,
    accentBlue: Color,
    labelColor: Color,
    subLabelColor: Color,
    cardBg: Color,
    borderColor: Color,
    glassSpec: GlassSurfaceSpec,
    isDark: Boolean,
    context: android.content.Context,
    onKomikClick: (String) -> Unit,
    onLoadNext: () -> Unit
) {
    when {
        state.isLoading && state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accentBlue)
        }
        state.items.isEmpty() && state.error != null -> ErrorView(
            message = state.error,
            accentBlue = accentBlue,
            labelColor = labelColor,
            subLabelColor = subLabelColor,
            onRetry = onLoadNext
        )
        state.items.isEmpty() -> EmptyView(
            title = "Belum ada komik",
            subtitle = "Daftar komik sedang kosong",
            icon = Icons.Rounded.Warning,
            accentBlue = accentBlue,
            labelColor = labelColor,
            subLabelColor = subLabelColor,
            onRetry = onLoadNext
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card
            val hero = state.items.firstOrNull()
            if (hero != null) {
                item(key = "hero") {
                    KomikHeroCard(
                        komik = hero,
                        accentBlue = accentBlue,
                        context = context,
                        onClick = { onKomikClick(hero.resolvedSlug) }
                    )
                }
            }

            // Popular Section (horizontal scroll)
            val popular = state.items.drop(1).take(8)
            if (popular.isNotEmpty()) {
                item(key = "section_popular") {
                    SectionHeader(
                        title = "Populer",
                        labelColor = labelColor,
                        accentBlue = accentBlue,
                        onSeeAll = { /* TODO: navigate to all popular */ }
                    )
                }
                item(key = "row_popular") {
                    HorizontalCardRow(
                        items = popular,
                        isDark = isDark,
                        labelColor = labelColor,
                        subLabelColor = subLabelColor,
                        accentBlue = accentBlue,
                        glassSpec = glassSpec,
                        context = context,
                        onKomikClick = onKomikClick
                    )
                }
            }

            // Latest Updates Section (horizontal scroll)
            val latest = state.items.drop(9).take(8)
            if (latest.isNotEmpty()) {
                item(key = "section_latest") {
                    SectionHeader(
                        title = "Update Terbaru",
                        labelColor = labelColor,
                        accentBlue = accentBlue,
                        onSeeAll = { /* TODO: navigate to all latest */ }
                    )
                }
                item(key = "row_latest") {
                    HorizontalCardRow(
                        items = latest,
                        isDark = isDark,
                        labelColor = labelColor,
                        subLabelColor = subLabelColor,
                        accentBlue = accentBlue,
                        glassSpec = glassSpec,
                        context = context,
                        onKomikClick = onKomikClick
                    )
                }
            }

            // All Items Grid
            val rest = state.items.drop(17)
            if (rest.isNotEmpty()) {
                item(key = "section_all") {
                    SectionHeader(
                        title = "Semua Komik",
                        labelColor = labelColor,
                        accentBlue = accentBlue,
                        onSeeAll = { }
                    )
                }
                item(key = "grid_all") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rest.size) { index ->
                            val komik = rest[index]
                            KomikCard(
                                komik = komik,
                                isDark = isDark,
                                labelColor = labelColor,
                                subLabelColor = subLabelColor,
                                accentBlue = accentBlue,
                                glassSpec = glassSpec,
                                context = context
                            ) {
                                onKomikClick(komik.resolvedSlug)
                            }
                        }
                    }
                }
            }

            // Load More Button
            if (state.hasNext && !state.isLoading) {
                item(key = "load_more") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onLoadNext,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(1.dp, accentBlue, RoundedCornerShape(12.dp))
                                .padding(horizontal = 32.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Muat Lebih Banyak",
                                color = accentBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else if (state.isLoading) {
                item(key = "loading_more") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    labelColor: Color,
    accentBlue: Color,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = labelColor)
        Text(
            text = "Lihat Semua",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentBlue,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSeeAll() }
                .padding(end = 16.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun HorizontalCardRow(
    items: List<KomikItem>,
    isDark: Boolean,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    glassSpec: GlassSurfaceSpec,
    context: android.content.Context,
    onKomikClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(220.dp)
    ) {
        items(items.size) { index ->
            val komik = items[index]
            KomikCard(
                komik = komik,
                isDark = isDark,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                accentBlue = accentBlue,
                glassSpec = glassSpec,
                context = context
            ) {
                onKomikClick(komik.resolvedSlug)
            }
        }
    }
}

@Composable
private fun KomikHeroCard(
    komik: KomikItem,
    accentBlue: Color,
    context: android.content.Context,
    onClick: () -> Unit
) {
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
                Brush.verticalGradient(colors = listOf(Color(0x1A000000), Color(0xAA000000)))
            )
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val type = komik.type
                if (type != null && type.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x55FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(type, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
                val latestChapter = komik.latestChapter
                if (latestChapter != null && latestChapter.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentBlue)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(latestChapter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
private fun KomikCard(
    komik: KomikItem,
    isDark: Boolean,
    labelColor: Color,
    subLabelColor: Color,
    accentBlue: Color,
    glassSpec: GlassSurfaceSpec,
    context: android.content.Context,
    onClick: (String) -> Unit
) {
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
            val latestChapter = komik.latestChapter
            if (latestChapter != null && latestChapter.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentBlue.copy(alpha = 0.92f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(latestChapter, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
            val rating = komik.rating
            if (rating != null && rating.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xAA000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = IOSOrange,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(rating, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

@Composable
private fun EmptyView(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentBlue: Color,
    labelColor: Color,
    subLabelColor: Color,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = subLabelColor.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = subLabelColor,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            onRetry?.let { retry ->
                Spacer(modifier = Modifier.height(20.dp))
                androidx.compose.material3.Button(
                    onClick = retry,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    accentBlue: Color,
    labelColor: Color,
    subLabelColor: Color,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = IOSRed,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Terjadi Kesalahan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = subLabelColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}