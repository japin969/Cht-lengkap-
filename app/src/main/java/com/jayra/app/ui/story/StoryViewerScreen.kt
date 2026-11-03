package com.jayra.app.ui.story

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.jayra.app.model.StoryStrings
import com.jayra.app.model.StoryViewerUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Fase 23.3: ViewModel untuk penampil layar penuh Story Viewer (5000ms per story).
 */
class StoryViewerViewModel(
    private val storyRepository: StoryRepository = AppModule.storyRepository
) : ViewModel() {

    private val _strings = MutableStateFlow(StoryStrings())
    val strings: StateFlow<StoryStrings> = _strings.asStateFlow()

    private val _uiState = MutableStateFlow<StoryViewerUiState>(StoryViewerUiState.Loading)
    val uiState: StateFlow<StoryViewerUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var initialJumpApplied = false

    fun loadStories(initialStoryId: String) {
        observeJob?.cancel()
        initialJumpApplied = false
        _uiState.value = StoryViewerUiState.Loading
        observeJob = viewModelScope.launch {
            storyRepository.observeStories()
                .catch { error ->
                    _uiState.value = StoryViewerUiState.Error(
                        error.message ?: "Gagal memuat penampil story."
                    )
                }
                .collect { stories ->
                    if (stories.isEmpty()) {
                        _uiState.value = StoryViewerUiState.Empty
                    } else {
                        val currentState = _uiState.value
                        val resolvedIndex = if (!initialJumpApplied) {
                            initialJumpApplied = true
                            stories.indexOfFirst { it.id == initialStoryId }.coerceAtLeast(0)
                        } else if (currentState is StoryViewerUiState.Success) {
                            currentState.currentIndex.coerceIn(0, stories.lastIndex)
                        } else {
                            0
                        }
                        _uiState.value = StoryViewerUiState.Success(
                            stories = stories,
                            currentIndex = resolvedIndex
                        )
                    }
                }
        }
    }

    /**
     * Berpindah ke story berikutnya. Mengembalikan `true` jika berhasil maju,
     * atau `false` jika sudah berada di story terakhir (sehingga layar ditutup).
     */
    fun nextStory(): Boolean {
        val state = _uiState.value as? StoryViewerUiState.Success ?: return false
        return if (state.currentIndex < state.stories.lastIndex) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1)
            true
        } else {
            false
        }
    }

    fun previousStory() {
        val state = _uiState.value as? StoryViewerUiState.Success ?: return
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1)
        }
    }

    fun markCurrentStoryViewed() {
        val state = _uiState.value as? StoryViewerUiState.Success ?: return
        val story = state.stories.getOrNull(state.currentIndex) ?: return
        if (!story.isViewed) {
            viewModelScope.launch {
                storyRepository.markStoryViewed(story.id)
            }
        }
    }

    companion object {
        const val STORY_DURATION_MS = 5000
    }
}

@Composable
fun StoryViewerScreen(
    initialStoryId: String,
    onClose: () -> Unit,
    viewModel: StoryViewerViewModel = viewModel()
) {
    BackHandler { onClose() }

    LaunchedEffect(initialStoryId) {
        viewModel.loadStories(initialStoryId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()

    StoryViewerContent(
        uiState = uiState,
        strings = strings,
        onNext = {
            val advanced = viewModel.nextStory()
            if (!advanced) {
                onClose()
            }
        },
        onPrevious = viewModel::previousStory,
        onStoryShown = viewModel::markCurrentStoryViewed,
        onClose = onClose
    )
}

@Composable
fun StoryViewerContent(
    uiState: StoryViewerUiState,
    strings: StoryStrings,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStoryShown: () -> Unit = {},
    onClose: () -> Unit,
    autoPlayEnabled: Boolean = true
) {
    val density = LocalDensity.current
    val swipeCloseThresholdPx = with(density) { 80.dp.toPx() }
    var downwardDragPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .testTag("story_viewer_fullscreen_box")
    ) {
        when (uiState) {
            is StoryViewerUiState.Loading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFFFFF),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.viewerLoadingLabel,
                        fontSize = 14.sp,
                        color = Color(0xCCFFFFFF)
                    )
                }
            }

            is StoryViewerUiState.Empty -> {
                Text(
                    text = strings.viewerEmptyLabel,
                    fontSize = 15.sp,
                    color = Color(0xFFFFFFFF),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is StoryViewerUiState.Error -> {
                Text(
                    text = uiState.message,
                    fontSize = 15.sp,
                    color = Color(0xFFFFFFFF),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }

            is StoryViewerUiState.Success -> {
                val stories = uiState.stories
                val currentIndex = uiState.currentIndex.coerceIn(0, stories.lastIndex)
                val activeStory = stories[currentIndex]

                var startProgress by remember(currentIndex, activeStory.id) {
                    mutableStateOf(!autoPlayEnabled)
                }

                val animatedProgress by animateFloatAsState(
                    targetValue = if (startProgress) 1f else 0f,
                    animationSpec = if (startProgress && autoPlayEnabled) {
                        tween(
                            durationMillis = StoryViewerViewModel.STORY_DURATION_MS,
                            easing = LinearEasing
                        )
                    } else {
                        snap()
                    },
                    label = "story_progress_5000ms"
                )

                LaunchedEffect(currentIndex, activeStory.id, autoPlayEnabled) {
                    onStoryShown()
                    if (autoPlayEnabled) {
                        startProgress = true
                        delay(StoryViewerViewModel.STORY_DURATION_MS.toLong())
                        onNext()
                    }
                }

                // Gambar Story Fullscreen (ContentScale.Fit) + Gesture Tap Kiri/Kanan & Swipe Down
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentIndex, stories.size) {
                            detectTapGestures { offset ->
                                if (offset.x >= size.width / 2f) {
                                    onNext()
                                } else {
                                    onPrevious()
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { downwardDragPx = 0f },
                                onDragCancel = { downwardDragPx = 0f },
                                onDragEnd = {
                                    if (downwardDragPx >= swipeCloseThresholdPx) {
                                        onClose()
                                    }
                                    downwardDragPx = 0f
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    downwardDragPx = (downwardDragPx + dragAmount).coerceAtLeast(0f)
                                }
                            )
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(activeStory.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = activeStory.caption.ifBlank { activeStory.userName },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("story_viewer_image")
                    )
                }

                // Gradient atas untuk keterbacaan progress bar & info penulis
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xB3000000),
                                    Color(0x00000000)
                                )
                            )
                        )
                )

                // Top LinearProgressIndicator (5000ms) + Header Penulis + Tombol Tutup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("story_viewer_progress_row"),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stories.forEachIndexed { index, story ->
                            val segmentProgress = when {
                                index < currentIndex -> 1f
                                index == currentIndex -> if (autoPlayEnabled) animatedProgress else 0.45f
                                else -> 0f
                            }
                            LinearProgressIndicator(
                                progress = { segmentProgress },
                                color = Color(0xFFFFFFFF),
                                trackColor = Color(0x4DFFFFFF),
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .testTag("story_progress_segment_${story.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(JayraBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeStory.userAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(activeStory.userAvatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = activeStory.userName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = activeStory.userName.ifBlank { "J" }.take(1).uppercase(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeStory.userName.ifBlank { activeStory.userId },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFFFFF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = activeStory.timestamp,
                                    fontSize = 12.sp,
                                    color = Color(0xCCFFFFFF)
                                )
                                if (!activeStory.albumTitle.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x33FFFFFF))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = activeStory.albumTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFFFFFF)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("story_viewer_close_button")
                        ) {
                            // GANTI SVG TELEGRAM
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.closeViewerContentDescription,
                                tint = Color(0xFFFFFFFF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Footer Caption & Jumlah Tayangan
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x00000000),
                                    Color(0xCC000000)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    if (activeStory.caption.isNotBlank()) {
                        Text(
                            text = activeStory.caption,
                            fontSize = 15.sp,
                            color = Color(0xFFFFFFFF),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // GANTI SVG TELEGRAM
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = strings.viewsSuffix,
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${activeStory.viewsCount} ${strings.viewsSuffix}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xCCFFFFFF)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Story Viewer Light", showBackground = true)
@Composable
private fun StoryViewerLightPreview() {
    val sampleStories = StoryRepository.defaultSeedStories("preview_uid").take(3)
    JayraTheme(darkTheme = false) {
        Surface {
            StoryViewerContent(
                uiState = StoryViewerUiState.Success(
                    stories = sampleStories,
                    currentIndex = 0
                ),
                strings = StoryStrings(),
                onNext = {},
                onPrevious = {},
                onClose = {},
                autoPlayEnabled = false
            )
        }
    }
}

@Preview(name = "Story Viewer Dark", showBackground = true)
@Composable
private fun StoryViewerDarkPreview() {
    val sampleStories = StoryRepository.defaultSeedStories("preview_uid").take(3)
    JayraTheme(darkTheme = true) {
        Surface {
            StoryViewerContent(
                uiState = StoryViewerUiState.Success(
                    stories = sampleStories,
                    currentIndex = 1
                ),
                strings = StoryStrings(),
                onNext = {},
                onPrevious = {},
                onClose = {},
                autoPlayEnabled = false
            )
        }
    }
}
