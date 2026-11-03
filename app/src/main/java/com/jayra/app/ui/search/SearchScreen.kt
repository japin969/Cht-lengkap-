package com.jayra.app.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.model.SearchFilter
import com.jayra.app.model.SearchResult
import com.jayra.app.model.SearchResultType
import com.jayra.app.model.SearchUiState
import com.jayra.app.ui.components.ShimmerCardList
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigateToResult: (SearchResult) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    BackHandler { onBack() }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        isLoading = isLoading,
        uiState = uiState,
        onQueryChange = viewModel::onSearchQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onFilterChange = viewModel::onFilterChange,
        onRetry = viewModel::onRetry,
        onBack = onBack,
        onResultClick = onNavigateToResult
    )
}

@Composable
fun SearchContent(
    searchQuery: String,
    selectedFilter: SearchFilter,
    isLoading: Boolean,
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onResultClick: (SearchResult) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
            ) {
                // TopAppBar: Height 56dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("search_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Search input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari pesan, kontak, grup...",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 17.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("search_input_field")
                        )
                    }

                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = onClearQuery,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("search_btn_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val chipBg = if (isSelected) {
                            JayraBlue
                        } else {
                            if (isDark) Color(0xFF2C3847) else Color(0xFF42688C)
                        }
                        val chipTextColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .clickable { onFilterChange(filter) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("filter_chip_${filter.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.label,
                                color = chipTextColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Shimmer placeholder list
                    ShimmerCardList(cardCount = 8)
                }
                uiState is SearchUiState.Loading -> {
                    ShimmerCardList(cardCount = 8)
                }
                uiState is SearchUiState.Empty -> {
                    SearchEmptyView()
                }
                uiState is SearchUiState.Error -> {
                    SearchErrorView(
                        message = uiState.message,
                        onRetry = onRetry
                    )
                }
                uiState is SearchUiState.Success -> {
                    SearchResultsList(
                        results = uiState.results,
                        searchQuery = searchQuery,
                        onResultClick = onResultClick
                    )
                }
                uiState is SearchUiState.Idle -> {
                    SearchEmptyView(
                        title = "Mulai Pencarian",
                        subtitle = "Ketik nama, username, teks pesan, atau saluran"
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    searchQuery: String,
    onResultClick: (SearchResult) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color(0xFF1E2732) else Color(0xFFE8E8E8)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            items = results,
            key = { "${it.type}_${it.id}_${it.targetId}" }
        ) { item ->
            SearchResultRow(
                result = item,
                searchQuery = searchQuery,
                onClick = { onResultClick(item) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = dividerColor
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    searchQuery: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1F1F1F)
    val highlightBg = if (isDark) Color(0xFF34495E) else Color(0xFFFFF9C4)
    val highlightColor = if (isDark) JayraBlue else Color(0xFF1A73E8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar (48dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(getAvatarColorForType(result.type)),
            contentAlignment = Alignment.Center
        ) {
            if (result.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = result.avatarUrl,
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback icon based on type
                Icon(
                    imageVector = when (result.type) {
                        SearchResultType.CHANNEL -> Icons.Default.VolumeUp
                        SearchResultType.GROUP -> Icons.Default.Group
                        SearchResultType.CONTACT -> Icons.Default.Person
                        SearchResultType.MESSAGE -> Icons.Default.Forum
                        SearchResultType.CHAT -> Icons.Default.Person
                        SearchResultType.FILE -> Icons.Default.Forum
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle with keyword highlight
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = highlightText(
                        text = result.title,
                        query = searchQuery,
                        normalColor = textColor,
                        highlightColor = highlightColor,
                        highlightBg = highlightBg
                    ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (result.timestamp.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.timestamp,
                        fontSize = 12.sp,
                        color = JayraSubtext
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = highlightText(
                    text = result.subtitle,
                    query = searchQuery,
                    normalColor = JayraSubtext,
                    highlightColor = highlightColor,
                    highlightBg = highlightBg
                ),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun highlightText(
    text: String,
    query: String,
    normalColor: Color,
    highlightColor: Color,
    highlightBg: Color
): androidx.compose.ui.text.AnnotatedString {
    val cleanQuery = query.trim()
    if (cleanQuery.isEmpty() || !text.contains(cleanQuery, ignoreCase = true)) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = normalColor)) {
                append(text)
            }
        }
    }

    return buildAnnotatedString {
        var startIndex = 0
        val lowerText = text.lowercase()
        val lowerQuery = cleanQuery.lowercase()

        while (startIndex < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
            if (matchIndex == -1) {
                withStyle(SpanStyle(color = normalColor)) {
                    append(text.substring(startIndex))
                }
                break
            }

            if (matchIndex > startIndex) {
                withStyle(SpanStyle(color = normalColor)) {
                    append(text.substring(startIndex, matchIndex))
                }
            }

            val endIndex = matchIndex + cleanQuery.length
            withStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold,
                    background = highlightBg
                )
            ) {
                append(text.substring(matchIndex, endIndex))
            }

            startIndex = endIndex
        }
    }
}

private fun getAvatarColorForType(type: SearchResultType): Color {
    return when (type) {
        SearchResultType.CHAT -> Color(0xFF3498DB)
        SearchResultType.CHANNEL -> Color(0xFFE67E22)
        SearchResultType.GROUP -> Color(0xFF2ECC71)
        SearchResultType.CONTACT -> Color(0xFF9B59B6)
        SearchResultType.MESSAGE -> Color(0xFF1ABC9C)
        SearchResultType.FILE -> Color(0xFF3498DB)
    }
}

@Composable
private fun SearchEmptyView(
    title: String = "Tidak ada hasil",
    subtitle: String = "Coba kata kunci lain atau periksa ejaan"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = JayraSubtext.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = JayraSubtext
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = JayraSubtext.copy(alpha = 0.8f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SearchErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFE74C3C),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Terjadi Kesalahan",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = JayraSubtext
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = JayraSubtext
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Coba Lagi", color = Color.White)
        }
    }
}

@Preview(name = "Search Screen Light")
@Composable
private fun SearchScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            SearchContent(
                searchQuery = "Jetpack",
                selectedFilter = SearchFilter.ALL,
                isLoading = false,
                uiState = SearchUiState.Success(
                    listOf(
                        SearchResult(
                            id = "1",
                            title = "Android Developers Community",
                            subtitle = "Jetpack Compose 1.8 has released exciting new performance improvements!",
                            timestamp = "09:30 AM",
                            type = SearchResultType.GROUP,
                            avatarUrl = ""
                        ),
                        SearchResult(
                            id = "2",
                            title = "Pavel Durov",
                            subtitle = "@durov • online",
                            timestamp = "10:45 AM",
                            type = SearchResultType.CONTACT,
                            avatarUrl = ""
                        )
                    )
                ),
                onQueryChange = {},
                onClearQuery = {},
                onFilterChange = {},
                onRetry = {},
                onBack = {},
                onResultClick = {}
            )
        }
    }
}

@Preview(name = "Search Screen Dark")
@Composable
private fun SearchScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            SearchContent(
                searchQuery = "",
                selectedFilter = SearchFilter.ALL,
                isLoading = false,
                uiState = SearchUiState.Empty,
                onQueryChange = {},
                onClearQuery = {},
                onFilterChange = {},
                onRetry = {},
                onBack = {},
                onResultClick = {}
            )
        }
    }
}
