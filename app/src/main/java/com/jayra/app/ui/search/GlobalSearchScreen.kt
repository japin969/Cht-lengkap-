package com.jayra.app.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.model.GlobalSearchStrings
import com.jayra.app.model.GlobalSearchUiState
import com.jayra.app.model.SearchFilter
import com.jayra.app.model.SearchResult
import com.jayra.app.model.SearchResultType
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSearchDark
import com.jayra.app.ui.theme.JayraSearchLight
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun GlobalSearchScreen(
    onBackClick: () -> Unit,
    onResultClick: (SearchResultType, String) -> Unit,
    viewModel: GlobalSearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val uiStrings by viewModel.uiStrings.collectAsStateWithLifecycle()

    BackHandler(enabled = searchQuery.isNotEmpty()) {
        viewModel.onClearQuery()
    }

    GlobalSearchContent(
        uiState = uiState,
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        strings = uiStrings,
        autoFocusSearch = true,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onFilterChange = viewModel::onFilterChange,
        onRetry = viewModel::onRetry,
        onBackClick = onBackClick,
        onResultClick = onResultClick
    )
}

@Composable
fun GlobalSearchContent(
    uiState: GlobalSearchUiState,
    searchQuery: String,
    selectedFilter: SearchFilter,
    strings: GlobalSearchStrings = GlobalSearchStrings(),
    autoFocusSearch: Boolean = false,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onResultClick: (SearchResultType, String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val listSurfaceBg = if (isDark) JayraCardDark else JayraCardLight

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = screenBg,
        topBar = {
            GlobalSearchTopBar(
                query = searchQuery,
                placeholder = strings.searchPlaceholder,
                autoFocus = autoFocusSearch,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            GlobalSearchFilterRow(
                filters = SearchFilter.GLOBAL_SEARCH_FILTERS,
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterChange
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(listSurfaceBg)
            ) {
                when (uiState) {
                    is GlobalSearchUiState.Idle -> {
                        GlobalSearchIdleView(
                            title = strings.idleTitle,
                            subtitle = strings.idleSubtitle
                        )
                    }

                    is GlobalSearchUiState.Loading -> {
                        GlobalSearchShimmerList()
                    }

                    is GlobalSearchUiState.Empty -> {
                        GlobalSearchEmptyView(
                            query = searchQuery,
                            emptyTitlePrefix = strings.emptyTitlePrefix,
                            subtitle = strings.emptySubtitle
                        )
                    }

                    is GlobalSearchUiState.Error -> {
                        GlobalSearchErrorView(
                            title = strings.errorTitle,
                            errorMessage = uiState.message,
                            retryLabel = strings.retryButtonLabel,
                            onRetry = onRetry
                        )
                    }

                    is GlobalSearchUiState.Success -> {
                        val allResults = uiState.results
                        val filteredResults by remember(allResults, selectedFilter) {
                            derivedStateOf {
                                allResults.filter { item ->
                                    GlobalSearchViewModel.matchesFilter(item, selectedFilter)
                                }
                            }
                        }

                        if (filteredResults.isEmpty()) {
                            GlobalSearchEmptyView(
                                query = searchQuery,
                                emptyTitlePrefix = strings.emptyTitlePrefix,
                                subtitle = strings.emptySubtitle
                            )
                        } else {
                            GlobalSearchResultsList(
                                results = filteredResults,
                                searchQuery = searchQuery,
                                onResultClick = onResultClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchTopBar(
    query: String,
    placeholder: String,
    autoFocus: Boolean = false,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val topBarColor = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val searchFieldBg = if (isDark) JayraSearchDark else JayraSearchLight
    val inputTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Surface(
        color = topBarColor,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_search_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("global_search_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(searchFieldBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 15.sp,
                            color = JayraSubtext,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = inputTextColor
                        ),
                        cursorBrush = SolidColor(JayraBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("global_search_input_field")
                    )
                }

                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(durationMillis = 150)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150))
                ) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .clickable(onClick = onClearQuery)
                            .testTag("global_search_clear_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hapus teks pencarian",
                            tint = JayraSubtext,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchFilterRow(
    filters: List<SearchFilter>,
    selectedFilter: SearchFilter,
    onFilterSelected: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val inactiveChipBg = if (isDark) JayraCardDark else JayraCardLight

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
            .testTag("global_search_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = filters,
            key = { it.name }
        ) { filter ->
            val isSelected = filter == selectedFilter
            val chipBg by animateColorAsState(
                targetValue = if (isSelected) JayraBlue else inactiveChipBg,
                animationSpec = tween(durationMillis = 200),
                label = "chip_bg_${filter.name}"
            )
            val chipTextColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFFFFFF) else JayraSubtext,
                animationSpec = tween(durationMillis = 200),
                label = "chip_text_${filter.name}"
            )
            val onChipClick = remember(filter, onFilterSelected) {
                { onFilterSelected(filter) }
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(chipBg)
                    .clickable(onClick = onChipClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("global_search_chip_${filter.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.globalChipLabel,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = chipTextColor
                )
            }
        }
    }
}

@Composable
fun GlobalSearchResultsList(
    results: List<SearchResult>,
    searchQuery: String,
    onResultClick: (SearchResultType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("global_search_results_list")
    ) {
        items(
            items = results,
            key = { it.id }
        ) { result ->
            val onItemClick = remember(result.id, result.type, result.targetId, onResultClick) {
                { onResultClick(result.type, result.targetId) }
            }
            val onContactMessageClick = remember(result.id, result.targetId, onResultClick) {
                { onResultClick(SearchResultType.CHAT, result.targetId) }
            }

            when (result.type) {
                SearchResultType.CHAT,
                SearchResultType.GROUP,
                SearchResultType.CHANNEL -> {
                    GlobalSearchChatItemRow(
                        result = result,
                        searchQuery = searchQuery,
                        onClick = onItemClick
                    )
                }

                SearchResultType.MESSAGE -> {
                    GlobalSearchMessageItemRow(
                        result = result,
                        searchQuery = searchQuery,
                        onClick = onItemClick
                    )
                }

                SearchResultType.CONTACT -> {
                    GlobalSearchContactItemRow(
                        result = result,
                        searchQuery = searchQuery,
                        onClick = onItemClick,
                        onMessageIconClick = onContactMessageClick
                    )
                }

                SearchResultType.FILE -> {
                    GlobalSearchFileItemRow(
                        result = result,
                        searchQuery = searchQuery,
                        onClick = onItemClick
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor,
                modifier = Modifier.padding(start = 64.dp)
            )
        }
    }
}

@Composable
fun GlobalSearchChatItemRow(
    result: SearchResult,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag("global_search_item_chat_${result.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlobalSearchAvatar(
            title = result.title,
            avatarUrl = result.avatarUrl,
            type = result.type
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildHighlightedText(
                        text = result.title,
                        query = searchQuery,
                        normalColor = primaryTextColor,
                        isDark = isDark
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (result.timestamp.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.timestamp,
                        fontSize = 13.sp,
                        color = JayraSubtext
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildHighlightedText(
                    text = result.subtitle,
                    query = searchQuery,
                    normalColor = JayraSubtext,
                    isDark = isDark
                ),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlobalSearchMessageItemRow(
    result: SearchResult,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("global_search_item_message_${result.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlobalSearchAvatar(
            title = result.title,
            avatarUrl = result.avatarUrl,
            type = SearchResultType.MESSAGE
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildHighlightedText(
                        text = result.title,
                        query = searchQuery,
                        normalColor = primaryTextColor,
                        isDark = isDark
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (result.timestamp.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.timestamp,
                        fontSize = 13.sp,
                        color = JayraSubtext
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildHighlightedText(
                    text = result.subtitle,
                    query = searchQuery,
                    normalColor = JayraSubtext,
                    isDark = isDark
                ),
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlobalSearchContactItemRow(
    result: SearchResult,
    searchQuery: String,
    onClick: () -> Unit,
    onMessageIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val isOnlineStatus = result.isOnline || result.subtitle.equals("online", ignoreCase = true)
    val statusColor = if (isOnlineStatus) JayraBlue else JayraSubtext

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp)
            .testTag("global_search_item_contact_${result.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            GlobalSearchAvatar(
                title = result.title,
                avatarUrl = result.avatarUrl,
                type = SearchResultType.CONTACT
            )
            if (isOnlineStatus) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(if (isDark) JayraCardDark else JayraCardLight)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(JayraSuccessGreen)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = buildHighlightedText(
                    text = result.title,
                    query = searchQuery,
                    normalColor = primaryTextColor,
                    isDark = isDark
                ),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = result.subtitle.ifBlank { "last seen recently" },
                fontSize = 13.sp,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onMessageIconClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("global_search_contact_message_btn_${result.id}")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = "Kirim pesan ke ${result.title}",
                tint = JayraBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun GlobalSearchFileItemRow(
    result: SearchResult,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag("global_search_item_file_${result.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(JayraBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = result.title,
                tint = JayraBlue,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = buildHighlightedText(
                    text = result.title,
                    query = searchQuery,
                    normalColor = primaryTextColor,
                    isDark = isDark
                ),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = result.subtitle,
                fontSize = 15.sp,
                color = JayraSubtext,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GlobalSearchAvatar(
    title: String,
    avatarUrl: String,
    type: SearchResultType
) {
    val fallbackBg = when (type) {
        SearchResultType.CHAT -> JayraBlue
        SearchResultType.GROUP -> JayraSuccessGreen
        SearchResultType.CHANNEL -> Color(0xFFE67E22)
        SearchResultType.CONTACT -> Color(0xFF8E44AD)
        SearchResultType.MESSAGE -> Color(0xFF2980B9)
        SearchResultType.FILE -> JayraBlue
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(fallbackBg),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = title.trim().firstOrNull()?.uppercaseChar()
            if (initial != null && type != SearchResultType.CHANNEL && type != SearchResultType.GROUP) {
                Text(
                    text = initial.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF)
                )
            } else {
                Icon(
                    imageVector = when (type) {
                        SearchResultType.CHANNEL -> Icons.Rounded.VolumeUp
                        SearchResultType.GROUP -> Icons.Rounded.Group
                        SearchResultType.CONTACT -> Icons.Rounded.Person
                        SearchResultType.MESSAGE -> Icons.Rounded.Forum
                        SearchResultType.CHAT -> Icons.Rounded.Person
                        SearchResultType.FILE -> Icons.Rounded.Description
                    },
                    contentDescription = title,
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun GlobalSearchIdleView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag("global_search_idle_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = JayraSubtext,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 15.sp,
            color = JayraSubtext,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GlobalSearchEmptyView(
    query: String,
    emptyTitlePrefix: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val sanitizedDisplay = query.trim()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag("global_search_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = JayraSubtext,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$emptyTitlePrefix '$sanitizedDisplay'",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 15.sp,
            color = JayraSubtext,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GlobalSearchShimmerList(
    itemCount: Int = 8,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE6E8EC)
    val highlightColor = if (isDark) Color(0xFF3E3E42) else Color(0xFFF7F8FA)
    val dividerColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    val infiniteTransition = rememberInfiniteTransition(label = "global_search_shimmer_transition")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "global_search_shimmer_offset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 220f)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("global_search_loading_shimmer")
    ) {
        items(count = itemCount, key = { index -> "shimmer_item_$index" }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor,
                modifier = Modifier.padding(start = 64.dp)
            )
        }
    }
}

@Composable
fun GlobalSearchErrorView(
    title: String,
    errorMessage: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag("global_search_error_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = "Peringatan kesalahan pencarian",
            tint = JayraDangerRed,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            textAlign = TextAlign.Center
        )

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                fontSize = 15.sp,
                color = JayraSubtext,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = JayraBlue,
                contentColor = Color(0xFFFFFFFF)
            ),
            modifier = Modifier
                .defaultMinSize(minWidth = 140.dp, minHeight = 48.dp)
                .testTag("global_search_retry_button")
        ) {
            Text(
                text = retryLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun buildHighlightedText(
    text: String,
    query: String,
    normalColor: Color,
    isDark: Boolean
): AnnotatedString {
    val cleanQuery = query.trim()
    if (cleanQuery.isEmpty() || !text.contains(cleanQuery, ignoreCase = true)) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = normalColor)) {
                append(text)
            }
        }
    }

    val highlightBg = if (isDark) Color(0xFF263B52) else Color(0xFFE3F2FD)
    return buildAnnotatedString {
        var currentIndex = 0
        val lowerText = text.lowercase()
        val lowerQuery = cleanQuery.lowercase()

        while (currentIndex < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
            if (matchIndex == -1) {
                withStyle(SpanStyle(color = normalColor)) {
                    append(text.substring(currentIndex))
                }
                break
            }

            if (matchIndex > currentIndex) {
                withStyle(SpanStyle(color = normalColor)) {
                    append(text.substring(currentIndex, matchIndex))
                }
            }

            val endIndex = matchIndex + cleanQuery.length
            withStyle(
                SpanStyle(
                    color = JayraBlue,
                    fontWeight = FontWeight.SemiBold,
                    background = highlightBg
                )
            ) {
                append(text.substring(matchIndex, endIndex))
            }
            currentIndex = endIndex
        }
    }
}

// =========================================================================
// PREVIEWS (LIGHT & DARK UNTUK SETIAP KOMPONEN)
// =========================================================================

private val previewSampleResults = listOf(
    SearchResult(
        id = "prev_chat_1",
        title = "Pavel Durov",
        subtitle = "Jayra mendukung enkripsi end-to-end penuh.",
        timestamp = "10:45 AM",
        type = SearchResultType.CHAT,
        targetId = "chat_1"
    ),
    SearchResult(
        id = "prev_msg_1",
        title = "Android Developers Community",
        subtitle = "Jetpack Compose 1.8 meningkatkan performa pencarian di Jayra secara signifikan.",
        timestamp = "09:30 AM",
        type = SearchResultType.MESSAGE,
        targetId = "chat_2"
    ),
    SearchResult(
        id = "prev_contact_1",
        title = "Elena Petrova",
        subtitle = "online",
        timestamp = "online",
        type = SearchResultType.CONTACT,
        targetId = "chat_3",
        isOnline = true
    ),
    SearchResult(
        id = "prev_file_1",
        title = "Jayra_Architecture_Spec.pdf",
        subtitle = "2.4 MB • 02 Nov 2026",
        timestamp = "2.4 MB",
        type = SearchResultType.FILE,
        targetId = "document"
    )
)

@Preview(name = "TopBar & Filter - Light", showBackground = true)
@Composable
private fun GlobalSearchHeaderLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface(color = JayraLightChatBg) {
            Column {
                GlobalSearchTopBar(
                    query = "Jayra",
                    placeholder = "Cari pesan, kontak, atau grup",
                    onQueryChange = {},
                    onClearQuery = {},
                    onBackClick = {}
                )
                GlobalSearchFilterRow(
                    filters = SearchFilter.GLOBAL_SEARCH_FILTERS,
                    selectedFilter = SearchFilter.ALL,
                    onFilterSelected = {}
                )
            }
        }
    }
}

@Preview(name = "TopBar & Filter - Dark", showBackground = true)
@Composable
private fun GlobalSearchHeaderDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface(color = JayraDarkChatBg) {
            Column {
                GlobalSearchTopBar(
                    query = "Jayra",
                    placeholder = "Cari pesan, kontak, atau grup",
                    onQueryChange = {},
                    onClearQuery = {},
                    onBackClick = {}
                )
                GlobalSearchFilterRow(
                    filters = SearchFilter.GLOBAL_SEARCH_FILTERS,
                    selectedFilter = SearchFilter.MESSAGES,
                    onFilterSelected = {}
                )
            }
        }
    }
}

@Preview(name = "Result Items (Chat, Message, Contact, File) - Light", showBackground = true)
@Composable
private fun GlobalSearchItemsLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface(color = JayraCardLight) {
            Column {
                GlobalSearchChatItemRow(
                    result = previewSampleResults[0],
                    searchQuery = "Jayra",
                    onClick = {}
                )
                GlobalSearchMessageItemRow(
                    result = previewSampleResults[1],
                    searchQuery = "Jayra",
                    onClick = {}
                )
                GlobalSearchContactItemRow(
                    result = previewSampleResults[2],
                    searchQuery = "Elena",
                    onClick = {},
                    onMessageIconClick = {}
                )
                GlobalSearchFileItemRow(
                    result = previewSampleResults[3],
                    searchQuery = "Jayra",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Result Items (Chat, Message, Contact, File) - Dark", showBackground = true)
@Composable
private fun GlobalSearchItemsDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface(color = JayraCardDark) {
            Column {
                GlobalSearchChatItemRow(
                    result = previewSampleResults[0],
                    searchQuery = "Jayra",
                    onClick = {}
                )
                GlobalSearchMessageItemRow(
                    result = previewSampleResults[1],
                    searchQuery = "Jayra",
                    onClick = {}
                )
                GlobalSearchContactItemRow(
                    result = previewSampleResults[2],
                    searchQuery = "Elena",
                    onClick = {},
                    onMessageIconClick = {}
                )
                GlobalSearchFileItemRow(
                    result = previewSampleResults[3],
                    searchQuery = "Jayra",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Global Search Success Screen - Light", showBackground = true)
@Composable
private fun GlobalSearchSuccessLightPreview() {
    JayraTheme(darkTheme = false) {
        GlobalSearchContent(
            uiState = GlobalSearchUiState.Success(previewSampleResults),
            searchQuery = "Jayra",
            selectedFilter = SearchFilter.ALL,
            onQueryChange = {},
            onClearQuery = {},
            onFilterChange = {},
            onRetry = {},
            onBackClick = {},
            onResultClick = { _, _ -> }
        )
    }
}

@Preview(name = "Global Search Success Screen - Dark", showBackground = true)
@Composable
private fun GlobalSearchSuccessDarkPreview() {
    JayraTheme(darkTheme = true) {
        GlobalSearchContent(
            uiState = GlobalSearchUiState.Success(previewSampleResults),
            searchQuery = "Jayra",
            selectedFilter = SearchFilter.ALL,
            onQueryChange = {},
            onClearQuery = {},
            onFilterChange = {},
            onRetry = {},
            onBackClick = {},
            onResultClick = { _, _ -> }
        )
    }
}

@Preview(name = "Global Search Idle & Empty - Light", showBackground = true)
@Composable
private fun GlobalSearchIdleLightPreview() {
    JayraTheme(darkTheme = false) {
        GlobalSearchContent(
            uiState = GlobalSearchUiState.Idle,
            searchQuery = "",
            selectedFilter = SearchFilter.ALL,
            onQueryChange = {},
            onClearQuery = {},
            onFilterChange = {},
            onRetry = {},
            onBackClick = {},
            onResultClick = { _, _ -> }
        )
    }
}

@Preview(name = "Global Search Empty - Dark", showBackground = true)
@Composable
private fun GlobalSearchEmptyDarkPreview() {
    JayraTheme(darkTheme = true) {
        GlobalSearchContent(
            uiState = GlobalSearchUiState.Empty,
            searchQuery = "kata_kunci_tidak_ada",
            selectedFilter = SearchFilter.ALL,
            onQueryChange = {},
            onClearQuery = {},
            onFilterChange = {},
            onRetry = {},
            onBackClick = {},
            onResultClick = { _, _ -> }
        )
    }
}

@Preview(name = "Global Search Shimmer Loading - Light", showBackground = true)
@Composable
private fun GlobalSearchShimmerLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface(color = JayraCardLight) {
            GlobalSearchShimmerList(itemCount = 8)
        }
    }
}

@Preview(name = "Global Search Shimmer Loading - Dark", showBackground = true)
@Composable
private fun GlobalSearchShimmerDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface(color = JayraCardDark) {
            GlobalSearchShimmerList(itemCount = 8)
        }
    }
}

@Preview(name = "Global Search Error - Light", showBackground = true)
@Composable
private fun GlobalSearchErrorLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface(color = JayraCardLight) {
            GlobalSearchErrorView(
                title = "Gagal mencari",
                errorMessage = "Periksa koneksi internet Anda lalu coba kembali.",
                retryLabel = "Coba Lagi",
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Global Search Error - Dark", showBackground = true)
@Composable
private fun GlobalSearchErrorDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface(color = JayraCardDark) {
            GlobalSearchErrorView(
                title = "Gagal mencari",
                errorMessage = "Periksa koneksi internet Anda lalu coba kembali.",
                retryLabel = "Coba Lagi",
                onRetry = {}
            )
        }
    }
}
