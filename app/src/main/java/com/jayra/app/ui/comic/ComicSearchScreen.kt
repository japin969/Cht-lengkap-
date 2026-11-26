package com.jayra.app.ui.comic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.ui.media.CenterState
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaPoster
import com.jayra.app.ui.media.PosterGridPlaceholder
import com.jayra.app.ui.theme.SFProFontFamily
import kotlinx.coroutines.delay

/**
 * ComicSearch — pola aplikasi komik terkenal: search bar + history + suggestion,
 * debounce 350ms, hasil grid poster. Filter via chip (judul).
 */
@Composable
fun ComicSearchScreen(
    onBack: () -> Unit,
    onComicClick: (String) -> Unit,
    viewModel: ComicViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(comicSearchHistory(context)) }

    // Debounce
    LaunchedEffect(query) {
        if (query.isBlank()) return@LaunchedEffect
        delay(350)
        viewModel.search(query)
        history = addComicSearchHistory(context, query)
    }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MediaColors.TextPrimary)
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari komik...", fontFamily = SFProFontFamily, color = MediaColors.TextTertiary) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = SFProFontFamily, color = MediaColors.TextPrimary, fontSize = 16.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MediaColors.SurfaceElevated,
                    unfocusedContainerColor = MediaColors.SurfaceElevated,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MediaColors.Comic
                ),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MediaColors.TextTertiary, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Box(Modifier.size(32.dp).clickable { query = "" }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, "Hapus", tint = MediaColors.TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) { viewModel.search(query); history = addComicSearchHistory(context, query) }
                }),
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
            )
        }

        when {
            state.isLoading -> PosterGridPlaceholder(modifier = Modifier.padding(top = 8.dp))
            query.isBlank() && history.isNotEmpty() -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Text("Pencarian Terakhir", fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp, color = MediaColors.TextPrimary,
                            modifier = Modifier.padding(16.dp))
                    }
                    items(history) { h ->
                        Row(
                            Modifier.fillMaxWidth().clickable { query = h }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, null, tint = MediaColors.TextTertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(h, fontFamily = SFProFontFamily, fontSize = 15.sp, color = MediaColors.TextSecondary, modifier = Modifier.weight(1f))
                            Box(Modifier.size(28.dp).clickable { history = removeComicSearchHistory(context, h) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, "Hapus", tint = MediaColors.TextTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            query.isBlank() -> {
                CenterState(
                    icon = { Icon(Icons.Default.Search, null, tint = MediaColors.Comic, modifier = Modifier.size(34.dp)) },
                    title = "Cari komik favoritmu",
                    subtitle = "Ketik judul untuk mulai mencari",
                    actionLabel = "",
                    onAction = {},
                    accent = MediaColors.Comic
                )
            }
            state.results.isEmpty() && state.error != null -> {
                CenterState(
                    icon = { Icon(Icons.Default.Search, null, tint = MediaColors.Comic, modifier = Modifier.size(34.dp)) },
                    title = "Tidak ada hasil",
                    subtitle = state.error ?: "Coba kata kunci lain",
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.retrySearch() },
                    accent = MediaColors.Comic
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(MediaDimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(MediaDimens.RowSpacing),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.results) { item ->
                        ComicGridItem(item) { onComicClick(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun ComicGridItem(item: com.jayra.app.data.KomikItem, onClick: (String) -> Unit) {
    Column(Modifier.clickable { onClick(item.slug ?: "") }) {
        MediaPoster(
            model = item.cover,
            contentDescription = item.title,
            accent = MediaColors.Comic,
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            item.title ?: "Tanpa Judul",
            fontFamily = SFProFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MediaColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Search history (SharedPreferences ringan) ──
private fun comicPrefs(context: android.content.Context) =
    context.getSharedPreferences("comic_search", android.content.Context.MODE_PRIVATE)

fun comicSearchHistory(context: android.content.Context): List<String> =
    comicPrefs(context).getString("history", "")?.split("\u0001")?.filter { it.isNotBlank() } ?: emptyList()

fun addComicSearchHistory(context: android.content.Context, q: String): List<String> {
    val list = comicSearchHistory(context).toMutableList()
    list.remove(q)
    list.add(0, q)
    if (list.size > 8) list.subList(8, list.size).clear()
    comicPrefs(context).edit().putString("history", list.joinToString("\u0001")).apply()
    return list
}

fun removeComicSearchHistory(context: android.content.Context, q: String): List<String> {
    val list = comicSearchHistory(context).toMutableList().apply { remove(q) }
    comicPrefs(context).edit().putString("history", list.joinToString("\u0001")).apply()
    return list
}