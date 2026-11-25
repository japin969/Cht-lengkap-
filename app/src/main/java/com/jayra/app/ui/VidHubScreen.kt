package com.jayra.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.data.MediaLibraryStore
import com.jayra.app.ui.anime.AnimeBookmarkScreen
import com.jayra.app.ui.anime.AnimeHomeScreen
import com.jayra.app.ui.anime.AnimeSearchScreen
import com.jayra.app.ui.drama.DramaScreen
import com.jayra.app.ui.komik.KomikBookmarkScreen
import com.jayra.app.ui.komik.KomikHistoryScreen
import com.jayra.app.ui.komik.KomikHomeScreen
import com.jayra.app.ui.komik.KomikSearchScreen
import com.jayra.app.ui.media.LocalMediaImageLoader
import com.jayra.app.ui.media.MediaImageLoaderProvider

/**
 * VidHub = HUB utama (3 card: anime / comic / strem) + shell per fitur.
 *
 * Alur: login -> chat -> HUB -> pilih card -> Home fitur (punya bottom nav
 * sendiri: Home / Search / Bookmark / History) -> back balik ke HUB.
 *
 * Setiap fitur punya aksen warna sendiri supaya user tahu sedang di fitur mana:
 *  - anime  = oranye (Crunchyroll)
 *  - comic  = hijau (Webtoon)
 *  - strem  = cyan (Bilibili)
 */
private enum class HubFeature { ANIME, COMIC, STREM }

private data class FeatureColors(
    val accent: Color,
    val background: Color,
    val card: Color,
    val onSurface: Color
)

private fun colorsFor(feature: HubFeature): FeatureColors = when (feature) {
    HubFeature.ANIME -> FeatureColors(
        accent = Color(0xFFFF6B00),
        background = Color(0xFF0A0A0A),
        card = Color(0xFF1A1A1A),
        onSurface = Color(0xFFFFFFFF)
    )
    HubFeature.COMIC -> FeatureColors(
        accent = Color(0xFF22C55E),
        background = Color(0xFF0A0A0A),
        card = Color(0xFF1A1A1A),
        onSurface = Color(0xFFFFFFFF)
    )
    HubFeature.STREM -> FeatureColors(
        accent = Color(0xFF00B8D4),
        background = Color(0xFF0A0A0A),
        card = Color(0xFF1A1A1A),
        onSurface = Color(0xFFFFFFFF)
    )
}

@Composable
fun VidHubScreen(
    onDramaClick: (String) -> Unit,
    onAnimeClick: (String) -> Unit,
    onKomikClick: (String) -> Unit,
    onAnimeSearchClick: () -> Unit = {},
    onAnimeBookmarkClick: () -> Unit = {},
    onAnimeViewAllClick: () -> Unit = {},
    onKomikSearchClick: () -> Unit = {},
    onKomikBookmarkClick: () -> Unit = {},
    onKomikHistoryClick: () -> Unit = {},
    onKomikViewAllClick: () -> Unit = {}
) {
    var feature by remember { mutableStateOf<HubFeature?>(null) }
    val context = LocalContext.current
    val mediaLoader = remember(context) { MediaImageLoaderProvider.get(context) }

    CompositionLocalProvider(
        LocalMediaImageLoader provides mediaLoader
    ) {
        val selected = feature
        if (selected == null) {
            HubScreen(onSelect = { feature = it })
        } else {
            FeatureShell(
                feature = selected,
                onExit = { feature = null },
                onAnimeClick = onAnimeClick,
                onDramaClick = onDramaClick,
                onKomikClick = onKomikClick,
                onAnimeViewAll = onAnimeViewAllClick,
                onKomikViewAll = onKomikViewAllClick
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
 * HUB — 1 screen, header "HUB", 3 card gede
 * ═══════════════════════════════════════════════════════════════════ */

@Composable
private fun HubScreen(onSelect: (HubFeature) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .statusBarsPaddingCompat()
    ) {
        Text(
            text = "HUB",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        Text(
            text = "Pilih tontonanmu",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HubCard(
                title = "anime",
                subtitle = "Episode terbaru, subtitle Indonesia",
                icon = Icons.Rounded.Movie,
                colors = colorsFor(HubFeature.ANIME),
                onClick = { onSelect(HubFeature.ANIME) }
            )
            HubCard(
                title = "comic",
                subtitle = "Baca chapter terbaru tiap hari",
                icon = Icons.Rounded.MenuBook,
                colors = colorsFor(HubFeature.COMIC),
                onClick = { onSelect(HubFeature.COMIC) }
            )
            HubCard(
                title = "strem",
                subtitle = "Donghua & streaming sub Indo",
                icon = Icons.Rounded.PlayArrow,
                colors = colorsFor(HubFeature.STREM),
                onClick = { onSelect(HubFeature.STREM) }
            )
        }
    }
}

@Composable
private fun HubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: FeatureColors,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(colors.card, colors.card.copy(alpha = 0.85f))
                )
            )
            .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = colors.accent, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
 * SHELL PER FITUR — top bar + konten + bottom nav sendiri
 * ═══════════════════════════════════════════════════════════════════ */

private data class NavItem(val label: String, val icon: ImageVector)

private val featureNav = listOf(
    NavItem("Home", Icons.Rounded.Home),
    NavItem("Search", Icons.Rounded.Search),
    NavItem("Bookmark", Icons.Rounded.Bookmark),
    NavItem("History", Icons.Rounded.History)
)

@Composable
private fun FeatureShell(
    feature: HubFeature,
    onExit: () -> Unit,
    onAnimeClick: (String) -> Unit,
    onDramaClick: (String) -> Unit,
    onKomikClick: (String) -> Unit,
    onAnimeViewAll: () -> Unit,
    onKomikViewAll: () -> Unit
) {
    val colors = colorsFor(feature)
    var tab by remember(feature) { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        FeatureTopBar(
            title = feature.name.lowercase(),
            accent = colors.accent,
            onBack = onExit,
            onSearch = { tab = 1 }
        )
        Box(Modifier.weight(1f)) {
            when (feature) {
                HubFeature.ANIME -> when (tab) {
                    0 -> AnimeHomeScreen(
                        onAnimeClick = onAnimeClick,
                        onSearchClick = { tab = 1 },
                        onViewAllClick = onAnimeViewAll
                    )
                    1 -> AnimeSearchScreen(onAnimeClick = onAnimeClick, onBack = { tab = 0 })
                    2 -> AnimeBookmarkScreen(
                        onAnimeClick = onAnimeClick,
                        onBack = { tab = 0 },
                        onBrowseClick = { tab = 0 }
                    )
                    else -> SimpleHistoryList(
                        kind = "anime",
                        accent = colors.accent,
                        card = colors.card,
                        onClick = onAnimeClick
                    )
                }

                HubFeature.COMIC -> when (tab) {
                    0 -> KomikHomeScreen(
                        onKomikClick = onKomikClick,
                        onSearchClick = { tab = 1 },
                        onBookmarkClick = { tab = 2 },
                        onHistoryClick = { tab = 3 },
                        onViewAllClick = onKomikViewAll
                    )
                    1 -> KomikSearchScreen(onKomikClick = onKomikClick, onBack = { tab = 0 })
                    2 -> KomikBookmarkScreen(
                        onKomikClick = onKomikClick,
                        onBack = { tab = 0 },
                        onBrowseClick = { tab = 0 }
                    )
                    else -> KomikHistoryScreen(
                        onKomikClick = onKomikClick,
                        onBack = { tab = 0 },
                        onBrowseClick = { tab = 0 }
                    )
                }

                HubFeature.STREM -> when (tab) {
                    0, 1 -> DramaScreen(onDramaClick = onDramaClick)
                    2 -> SimpleHistoryList(
                        kind = "drama",
                        accent = colors.accent,
                        card = colors.card,
                        onClick = onDramaClick,
                        bookmarks = true
                    )
                    else -> SimpleHistoryList(
                        kind = "drama",
                        accent = colors.accent,
                        card = colors.card,
                        onClick = onDramaClick
                    )
                }
            }
        }
        FeatureBottomNav(
            items = featureNav,
            selected = tab,
            accent = colors.accent,
            card = colors.card,
            onSelect = { tab = it }
        )
    }
}

@Composable
private fun FeatureTopBar(
    title: String,
    accent: Color,
    onBack: () -> Unit,
    onSearch: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPaddingCompat()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali ke HUB", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Search, "Cari", tint = accent)
        }
    }
}

@Composable
private fun FeatureBottomNav(
    items: List<NavItem>,
    selected: Int,
    accent: Color,
    card: Color,
    onSelect: (Int) -> Unit
) {
    Surface(color = card) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPaddingCompat()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) accent else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = if (isSelected) accent else Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
 * HISTORY / BOOKMARK SEDERHANA (dipakai anime & strem)
 * ═══════════════════════════════════════════════════════════════════ */

@Composable
private fun SimpleHistoryList(
    kind: String,
    accent: Color,
    card: Color,
    onClick: (String) -> Unit,
    bookmarks: Boolean = false
) {
    val context = LocalContext.current
    var entries by remember(kind, bookmarks) {
        mutableStateOf(
            if (bookmarks) MediaLibraryStore.bookmarks(context).filter { it.kind == kind }
            else MediaLibraryStore.history(context, kind)
        )
    }

    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (bookmarks) Icons.Rounded.Bookmark else Icons.Rounded.History,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (bookmarks) "Belum ada bookmark" else "Belum ada riwayat",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(card)
                        .clickable { onClick(entry.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        entry.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                            Text(
                                sub,
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ── helper insets (tanpa import tambahan di file lain) ───────────── */

@Composable
private fun Modifier.statusBarsPaddingCompat(): Modifier =
    this.then(statusBarsPadding())

@Composable
private fun Modifier.navigationBarsPaddingCompat(): Modifier =
    this.then(navigationBarsPadding())