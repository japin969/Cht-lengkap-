package com.jayra.app.ui.story

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.data.StoryRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Story
import com.jayra.app.model.StoryAlbumFilter
import com.jayra.app.model.StoryStrings
import com.jayra.app.model.StoryUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Fase 23.2: ViewModel untuk layar Story Albums yang memuat koleksi `stories` dari Firestore.
 */
class StoryViewModel(
    private val storyRepository: StoryRepository = AppModule.storyRepository
) : ViewModel() {

    private val _strings = MutableStateFlow(StoryStrings())
    val strings: StateFlow<StoryStrings> = _strings.asStateFlow()

    private val _uiState = MutableStateFlow<StoryUiState>(StoryUiState.Loading)
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private val _albumFilters = MutableStateFlow(
        listOf(StoryAlbumFilter(id = null, title = _strings.value.allAlbumsChip))
    )
    val albumFilters: StateFlow<List<StoryAlbumFilter>> = _albumFilters.asStateFlow()

    private val _selectedAlbumId = MutableStateFlow<String?>(null)
    val selectedAlbumId: StateFlow<String?> = _selectedAlbumId.asStateFlow()

    private val _presetPhotos = MutableStateFlow(StoryRepository.PRESET_STORY_PHOTOS)
    val presetPhotos: StateFlow<List<Pair<String, String>>> = _presetPhotos.asStateFlow()

    private val _presetAlbums = MutableStateFlow(StoryRepository.PRESET_ALBUMS)
    val presetAlbums: StateFlow<List<Pair<String, String>>> = _presetAlbums.asStateFlow()

    private var allStoriesCache: List<Story> = emptyList()
    private var observeJob: Job? = null

    init {
        loadStories()
    }

    fun loadStories() {
        observeJob?.cancel()
        _uiState.value = StoryUiState.Loading
        observeJob = viewModelScope.launch {
            storyRepository.seedInitialStoriesIfEmpty()
            storyRepository.observeStories()
                .catch { error ->
                    _uiState.value = StoryUiState.Error(
                        error.message ?: "Gagal memuat koleksi Story Albums."
                    )
                }
                .collect { stories ->
                    allStoriesCache = stories
                    rebuildAlbumFilters(stories)
                    applyAlbumFilter(_selectedAlbumId.value)
                }
        }
    }

    fun onSelectAlbum(albumId: String?) {
        _selectedAlbumId.value = albumId
        applyAlbumFilter(albumId)
    }

    private fun rebuildAlbumFilters(stories: List<Story>) {
        val dynamicAlbums = stories
            .mapNotNull { story ->
                val id = story.albumId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = story.albumTitle?.takeIf { it.isNotBlank() } ?: id
                StoryAlbumFilter(id = id, title = title)
            }
            .distinctBy { it.id }

        _albumFilters.value = listOf(
            StoryAlbumFilter(id = null, title = _strings.value.allAlbumsChip)
        ) + dynamicAlbums
    }

    private fun applyAlbumFilter(albumId: String?) {
        val filtered = if (albumId == null) {
            allStoriesCache
        } else {
            allStoriesCache.filter { it.albumId == albumId }
        }
        _uiState.value = if (filtered.isEmpty()) {
            StoryUiState.Empty
        } else {
            StoryUiState.Success(
                stories = filtered,
                selectedAlbumId = albumId
            )
        }
    }

    fun createStory(
        imageUrl: String,
        caption: String,
        albumId: String?,
        albumTitle: String?
    ) {
        viewModelScope.launch {
            val result = storyRepository.createStory(
                imageUrl = imageUrl,
                caption = caption,
                albumId = albumId,
                albumTitle = albumTitle
            )
            result.onFailure { error ->
                _uiState.value = StoryUiState.Error(
                    error.message ?: "Gagal membagikan story ke Firestore."
                )
            }
        }
    }
}

@Composable
fun StoryAlbumsScreen(
    onBackClick: () -> Unit,
    onStoryClick: (String) -> Unit,
    viewModel: StoryViewModel = viewModel()
) {
    BackHandler { onBackClick() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val albumFilters by viewModel.albumFilters.collectAsStateWithLifecycle()
    val selectedAlbumId by viewModel.selectedAlbumId.collectAsStateWithLifecycle()
    val presetPhotos by viewModel.presetPhotos.collectAsStateWithLifecycle()
    val presetAlbums by viewModel.presetAlbums.collectAsStateWithLifecycle()

    StoryAlbumsContent(
        uiState = uiState,
        strings = strings,
        albumFilters = albumFilters,
        selectedAlbumId = selectedAlbumId,
        presetPhotos = presetPhotos,
        presetAlbums = presetAlbums,
        onBackClick = onBackClick,
        onStoryClick = onStoryClick,
        onSelectAlbum = viewModel::onSelectAlbum,
        onCreateStory = viewModel::createStory,
        onRetry = viewModel::loadStories
    )
}

@Composable
fun StoryAlbumsContent(
    uiState: StoryUiState,
    strings: StoryStrings,
    albumFilters: List<StoryAlbumFilter>,
    selectedAlbumId: String?,
    presetPhotos: List<Pair<String, String>> = StoryRepository.PRESET_STORY_PHOTOS,
    presetAlbums: List<Pair<String, String>> = StoryRepository.PRESET_ALBUMS,
    onBackClick: () -> Unit,
    onStoryClick: (String) -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onCreateStory: (imageUrl: String, caption: String, albumId: String?, albumTitle: String?) -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C242B)

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("stories_back_button")
                ) {
                    // GANTI SVG TELEGRAM
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backContentDescription,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = strings.screenTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("stories_add_button")
                ) {
                    // GANTI SVG TELEGRAM
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.addStoryContentDescription,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
        ) {
            if (albumFilters.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg)
                        .padding(vertical = 8.dp)
                        .testTag("story_album_filter_row"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = albumFilters,
                        key = { it.id ?: "all_albums" }
                    ) { filter ->
                        val isSelected = selectedAlbumId == filter.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectAlbum(filter.id) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("story_album_chip_${filter.id ?: "all"}"),
                            label = {
                                Text(
                                    text = filter.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    // GANTI SVG TELEGRAM
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JayraBlue.copy(alpha = 0.16f),
                                selectedLabelColor = JayraBlue,
                                selectedLeadingIconColor = JayraBlue,
                                containerColor = screenBg,
                                labelColor = JayraSubtext
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState) {
                    is StoryUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = JayraBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("stories_loading_indicator")
                            )
                        }
                    }

                    is StoryUiState.Empty -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(JayraBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                // GANTI SVG TELEGRAM
                                Icon(
                                    imageVector = Icons.Default.AutoAwesomeMotion,
                                    contentDescription = strings.screenTitle,
                                    tint = JayraBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = strings.emptyTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.emptySubtitle,
                                fontSize = 14.sp,
                                color = JayraSubtext,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("stories_empty_create_button")
                            ) {
                                Text(
                                    text = strings.addStoryButtonLabel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                        }
                    }

                    is StoryUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // GANTI SVG TELEGRAM
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = strings.retryButtonLabel,
                                tint = JayraDangerRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.message,
                                fontSize = 15.sp,
                                color = JayraSubtext,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("stories_retry_button")
                            ) {
                                Text(
                                    text = strings.retryButtonLabel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                        }
                    }

                    is StoryUiState.Success -> {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(durationMillis = 220))
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("stories_vertical_grid"),
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 16.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = uiState.stories,
                                    key = { it.id }
                                ) { story ->
                                    StoryGridItemCard(
                                        story = story,
                                        viewsSuffix = strings.viewsSuffix,
                                        primaryTextColor = primaryTextColor,
                                        cardBg = cardBg,
                                        onClick = { onStoryClick(story.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateStoryDialog(
            strings = strings,
            presetPhotos = presetPhotos,
            presetAlbums = presetAlbums,
            onDismiss = { showCreateDialog = false },
            onSave = { imageUrl, caption, albumId, albumTitle ->
                onCreateStory(imageUrl, caption, albumId, albumTitle)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun StoryGridItemCard(
    story: Story,
    viewsSuffix: String,
    primaryTextColor: Color,
    cardBg: Color,
    onClick: () -> Unit
) {
    val ringColor by animateColorAsState(
        targetValue = if (!story.isViewed) {
            JayraBlue
        } else {
            JayraSubtext.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "story_ring_color_${story.id}"
    )
    val ringWidth = if (!story.isViewed) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag("story_grid_item_${story.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(
                    width = ringWidth,
                    color = ringColor,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(story.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = story.userName.ifBlank { story.id },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient scrim bawah & atas agar badge terbaca kontras tinggi
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x66000000),
                                Color(0x00000000),
                                Color(0x99000000)
                            )
                        )
                    )
            )

            if (!story.albumTitle.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = story.albumTitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GANTI SVG TELEGRAM
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = viewsSuffix,
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${story.viewsCount}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = story.userName.ifBlank { story.userId },
            fontSize = 13.sp,
            fontWeight = if (!story.isViewed) FontWeight.SemiBold else FontWeight.Normal,
            color = primaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
private fun CreateStoryDialog(
    strings: StoryStrings,
    presetPhotos: List<Pair<String, String>>,
    presetAlbums: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (imageUrl: String, caption: String, albumId: String?, albumTitle: String?) -> Unit
) {
    var selectedImageUrl by remember {
        mutableStateOf(presetPhotos.firstOrNull()?.second ?: "")
    }
    var caption by remember { mutableStateOf("") }
    var selectedAlbumIndex by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = strings.dialogTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.presetPhotosLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = JayraSubtext
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetPhotos) { (label, url) ->
                        val isChosen = selectedImageUrl == url
                        FilterChip(
                            selected = isChosen,
                            onClick = { selectedImageUrl = url },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = selectedImageUrl,
                    onValueChange = { selectedImageUrl = it },
                    label = { Text(strings.imageUrlLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_story_url_input")
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text(strings.captionLabel) },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_story_caption_input")
                )

                Text(
                    text = strings.albumLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = JayraSubtext
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetAlbums.indices.toList()) { idx ->
                        val (_, albumTitle) = presetAlbums[idx]
                        FilterChip(
                            selected = selectedAlbumIndex == idx,
                            onClick = { selectedAlbumIndex = idx },
                            label = {
                                Text(
                                    text = albumTitle,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val albumPair = presetAlbums.getOrNull(selectedAlbumIndex)
                    onSave(
                        selectedImageUrl.trim(),
                        caption.trim(),
                        albumPair?.first,
                        albumPair?.second
                    )
                },
                enabled = selectedImageUrl.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("create_story_submit_button")
            ) {
                Text(
                    text = strings.saveLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = strings.cancelLabel,
                    fontSize = 14.sp,
                    color = JayraSubtext
                )
            }
        }
    )
}

@Preview(name = "Story Albums Light", showBackground = true)
@Composable
private fun StoryAlbumsLightPreview() {
    val sampleStories = StoryRepository.defaultSeedStories("preview_uid")
    JayraTheme(darkTheme = false) {
        Surface {
            StoryAlbumsContent(
                uiState = StoryUiState.Success(
                    stories = sampleStories,
                    selectedAlbumId = null
                ),
                strings = StoryStrings(),
                albumFilters = listOf(
                    StoryAlbumFilter(null, "Semua"),
                    StoryAlbumFilter("album_rilis", "Rilis Jayra"),
                    StoryAlbumFilter("album_tim", "Momen Tim"),
                    StoryAlbumFilter("album_arsitektur", "Arsitektur")
                ),
                selectedAlbumId = null,
                onBackClick = {},
                onStoryClick = {},
                onSelectAlbum = {},
                onCreateStory = { _, _, _, _ -> },
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Story Albums Dark", showBackground = true)
@Composable
private fun StoryAlbumsDarkPreview() {
    val sampleStories = StoryRepository.defaultSeedStories("preview_uid")
    JayraTheme(darkTheme = true) {
        Surface {
            StoryAlbumsContent(
                uiState = StoryUiState.Success(
                    stories = sampleStories,
                    selectedAlbumId = "album_rilis"
                ),
                strings = StoryStrings(),
                albumFilters = listOf(
                    StoryAlbumFilter(null, "Semua"),
                    StoryAlbumFilter("album_rilis", "Rilis Jayra"),
                    StoryAlbumFilter("album_tim", "Momen Tim")
                ),
                selectedAlbumId = "album_rilis",
                onBackClick = {},
                onStoryClick = {},
                onSelectAlbum = {},
                onCreateStory = { _, _, _, _ -> },
                onRetry = {}
            )
        }
    }
}
