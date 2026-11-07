package com.jayra.app.ui.music

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.NexadevApi
import com.jayra.app.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MusicUiState {
    data object Loading : MusicUiState
    data class Success(val songs: List<Song>) : MusicUiState
    data object Empty : MusicUiState
    data class Error(val message: String) : MusicUiState
}

@OptIn(FlowPreview::class)
class MusicViewModel @JvmOverloads constructor(
    application: Application,
    private val nexadevApi: NexadevApi = NexadevApi.create()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MusicUiState>(MusicUiState.Empty)
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressUpdateJob: Job? = null
    private var lastQuery: String = ""

    private val curatedSongs = listOf(
        Song(
            id = "seed_1",
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            album = "Starboy",
            duration = "3:50",
        )
    )

    init {
        // Show curated songs by default
        _uiState.value = MusicUiState.Success(curatedSongs)

        // Observe search query with 300ms debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .distinctUntilChanged()
                .collect { query ->
                    executeSearch(query)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private suspend fun executeSearch(query: String) {
        try {
            val response = NexadevApi.create().searchYouTube(query)
            if (response.isSuccessful && response.body()?.status == true) {
                val body = response.body()!!
                val song = Song(
                    id = System.currentTimeMillis().toString(),
                    title = body.title ?: "Unknown",
                    artist = "YouTube Music",
                    album = "YouTube Stream",
                    duration = body.duration ?: "0:00",
                    coverUrl = body.thumbnail ?: "",
                    audioUrl = body.data?.url ?: ""
                )
                _uiState.value = _uiState.value.copy(
                    searchResults = listOf(song),
                    isSearching = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Lagu tidak ditemukan",
                    isSearching = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Gagal mencari: ${e.localizedMessage}",
                isSearching = false
            )
        }
    }

    private suspend fun searchYouTubeFallback(query: String) {
        try {
            val ytResponse = withContext(Dispatchers.IO) {
                nexadevApi.searchYouTube(query)
            }

            if (ytResponse.isSuccessful && ytResponse.body() != null) {
                val song = ytResponse.body()!!.toSong()
                _uiState.value = MusicUiState.Success(listOf(song))
            } else {
                // If API fails, filter curated songs
                val filtered = curatedSongs.filter {
                    it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
                }
                _uiState.value = if (filtered.isNotEmpty()) {
                    MusicUiState.Success(filtered)
                } else {
                    MusicUiState.Empty
                }
            }
        } catch (e: Exception) {
            val filtered = curatedSongs.filter {
                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            }
            if (filtered.isNotEmpty()) {
                _uiState.value = MusicUiState.Success(filtered)
            } else {
                _uiState.value = MusicUiState.Error("Gagal memuat musik: ${e.localizedMessage ?: "Koneksi bermasalah"}")
            }
        }
    }

    fun onSongClick(song: Song) {
        if (_currentSong.value?.id == song.id) {
            onPlayPauseClick()
            return
        }

        _currentSong.value = song
        playSong(song)
    }

    private fun playSong(song: Song) {
        releasePlayer()

        if (song.audioUrl.isBlank()) {
            _isPlaying.value = false
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(song.audioUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = mp.duration.toLong()
                    startProgressTracking()
                }
                setOnCompletionListener {
                    if (_isRepeat.value) {
                        seekTo(0)
                        start()
                        _isPlaying.value = true
                    } else {
                        onNext()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    false
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            _isPlaying.value = false
        }
    }

    fun onPlayPauseClick() {
        val player = mediaPlayer ?: run {
            _currentSong.value?.let { playSong(it) }
            return
        }

        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressTracking()
        } else {
            player.start()
            _isPlaying.value = true
            startProgressTracking()
        }
    }

    fun onSeekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        }
    }

    fun onNext() {
        val currentList = when (val state = _uiState.value) {
            is MusicUiState.Success -> state.songs
            else -> curatedSongs
        }
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.id == _currentSong.value?.id }
        val nextIndex = if (_isShuffle.value) {
            (0 until currentList.size).random()
        } else {
            if (currentIndex == -1 || currentIndex >= currentList.size - 1) 0 else currentIndex + 1
        }

        val nextSong = currentList[nextIndex]
        _currentSong.value = nextSong
        playSong(nextSong)
    }

    fun onPrev() {
        val currentList = when (val state = _uiState.value) {
            is MusicUiState.Success -> state.songs
            else -> curatedSongs
        }
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.id == _currentSong.value?.id }
        val prevIndex = if (currentIndex <= 0) currentList.size - 1 else currentIndex - 1

        val prevSong = currentList[prevIndex]
        _currentSong.value = prevSong
        playSong(prevSong)
    }

    fun onShuffleToggle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun onRepeatToggle() {
        _isRepeat.value = !_isRepeat.value
    }

    fun onRetry() {
        viewModelScope.launch {
            executeSearch(lastQuery)
        }
    }

    private fun startProgressTracking() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.let { player ->
                    _currentPosition.value = player.currentPosition.toLong()
                    _duration.value = player.duration.toLong()
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressUpdateJob?.cancel()
    }

    private fun releasePlayer() {
        stopProgressTracking()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
