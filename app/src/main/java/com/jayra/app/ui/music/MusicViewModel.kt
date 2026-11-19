package com.jayra.app.ui.music

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.MusicRepository
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
    private val musicRepository: MusicRepository = MusicRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MusicUiState>(MusicUiState.Loading)
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

    /** true saat MediaPlayer sedang menyiapkan stream / resolusi preview. */
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    /** Pesan error pemutaran (mis. preview tidak tersedia / koneksi gagal). */
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressUpdateJob: Job? = null
    private var lastQuery: String = ""

    // History
    private val prefs = application.getSharedPreferences("jayra_music_prefs", android.content.Context.MODE_PRIVATE)
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    init {
        loadHistory()

        // Observe search query with 500ms debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .distinctUntilChanged()
                .collect { query ->
                    executeSearch(query)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun loadHistory() {
        val stored = prefs.getString("music_history", "") ?: ""
        _searchHistory.value = if (stored.isBlank()) emptyList()
            else stored.split("||").filter { it.isNotBlank() }
    }

    private fun addHistory(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        val capped = current.take(20)
        _searchHistory.value = capped
        prefs.edit().putString("music_history", capped.joinToString("||")).apply()
    }

    fun onHistoryClick(query: String) {
        _searchQuery.value = query
    }

    fun onClearHistory() {
        _searchHistory.value = emptyList()
        prefs.edit().remove("music_history").apply()
    }

    private suspend fun executeSearch(query: String) {
        val q = query.trim()
        lastQuery = q
        _uiState.value = MusicUiState.Loading
        try {
            val result = withContext(Dispatchers.IO) {
                if (q.isEmpty()) {
                    musicRepository.getPopularChart()
                } else {
                    musicRepository.searchAll(q)
                }
            }

            if (result.isSuccess) {
                val songs = result.getOrNull() ?: emptyList()
                val filteredSongs = songs.filter { it.title.isNotBlank() && it.audioUrl.isNotBlank() }
                if (filteredSongs.isNotEmpty()) {
                    _uiState.value = MusicUiState.Success(filteredSongs)
                    if (q.isNotEmpty()) addHistory(q)
                } else {
                    _uiState.value = MusicUiState.Empty
                }
            } else {
                _uiState.value = MusicUiState.Error(
                    "Gagal memuat musik. Coba lagi."
                )
            }
        } catch (e: Exception) {
            _uiState.value = MusicUiState.Error(e.userMessage("Gagal mencari lagu"))
        }
    }

    /** Terjemahkan error teknis jadi pesan yang bisa dipahami user HP. */
    private fun Throwable.userMessage(default: String): String = when (this) {
        is java.net.UnknownHostException -> "Tidak ada koneksi internet"
        is java.net.SocketTimeoutException -> "Koneksi lambat / timeout. Coba lagi."
        is javax.net.ssl.SSLException -> "Koneksi aman ke server gagal"
        is java.io.IOException -> localizedMessage ?: "Koneksi bermasalah"
        else -> localizedMessage ?: default
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
        _playbackError.value = null

        if (song.audioUrl.isBlank()) {
            _isPlaying.value = false
            _playbackError.value = "Lagu ini tidak punya audio URL. Coba lagu lain."
            return
        }

        startPlayer(song, song.audioUrl)
    }

    private fun startPlayer(song: Song, url: String) {
        _isBuffering.value = true
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isBuffering.value = false
                    _isPlaying.value = true
                    _playbackError.value = null
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
                setOnErrorListener { _, what, extra ->
                    _isBuffering.value = false
                    _isPlaying.value = false
                    _playbackError.value = "Gagal memutar audio (${what}/${extra}). Coba lagi."
                    releasePlayer()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _isBuffering.value = false
            _isPlaying.value = false
            _playbackError.value = e.userMessage("Gagal memutar audio")
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
            try {
                player.start()
                _isPlaying.value = true
                _playbackError.value = null
                startProgressTracking()
            } catch (e: Exception) {
                _isPlaying.value = false
                _playbackError.value = e.userMessage("Gagal melanjutkan audio")
            }
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
            else -> emptyList()
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
            else -> emptyList()
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
        _isBuffering.value = false
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}