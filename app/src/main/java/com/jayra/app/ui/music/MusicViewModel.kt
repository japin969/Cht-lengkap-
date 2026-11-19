package com.jayra.app.ui.music

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.NexadevApi
import com.jayra.app.data.DeezerApi
import com.jayra.app.data.DeezerTrack
import com.jayra.app.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    private val nexadevApi: NexadevApi = NexadevApi.create(),
    private val deezerApi: DeezerApi = DeezerApi.create()
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

    private val curatedSongs = listOf(
        Song(
            id = "seed_1",
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            album = "Starboy",
            duration = "3:50",
        )
    )

    // History
    private val prefs = application.getSharedPreferences("jayra_music_prefs", android.content.Context.MODE_PRIVATE)
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    init {
        loadHistory()

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
            // Sumber data: Deezer (api.nexadev.my.id sudah mati / 403).
            // Query kosong = tampilkan chart/populer, bukan layar kosong.
            val response = withContext(Dispatchers.IO) {
                if (q.isEmpty()) {
                    deezerApi.chart(limit = 25)
                } else {
                    deezerApi.search(q, limit = 25)
                }
            }

            if (!response.isSuccessful) {
                _uiState.value = MusicUiState.Error(
                    "Gagal memuat musik (HTTP ${response.code()}). Coba lagi."
                )
                return
            }

            val tracks = response.body()?.data.orEmpty()
            val songs = tracks
                .map { it.toSong() }
                .filter { it.title.isNotBlank() }
            if (songs.isNotEmpty()) {
                _uiState.value = MusicUiState.Success(songs)
                if (q.isNotEmpty()) addHistory(q)
            } else {
                val fallback = if (q.isEmpty()) curatedSongs else curatedSongs.filter {
                    it.title.contains(q, ignoreCase = true) ||
                        it.artist.contains(q, ignoreCase = true)
                }
                _uiState.value = if (fallback.isNotEmpty()) {
                    MusicUiState.Success(fallback)
                } else {
                    MusicUiState.Empty
                }
            }
        } catch (e: Exception) {
            val fallback = if (q.isEmpty()) curatedSongs else curatedSongs.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true)
            }
            _uiState.value = if (fallback.isNotEmpty()) {
                MusicUiState.Success(fallback)
            } else {
                MusicUiState.Error(e.userMessage("Gagal mencari lagu"))
            }
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

    private fun DeezerTrack.toSong(): Song = Song(
        id = "dz_${id ?: System.currentTimeMillis()}",
        title = title ?: "Tanpa Judul",
        artist = artist?.name ?: "Unknown",
        album = album?.title ?: "",
        duration = formatDuration(duration ?: 0),
        coverUrl = album?.coverBig ?: album?.coverMedium ?: "",
        audioUrl = preview ?: "",
        source = "deezer"
    )

    private fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
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
        _playbackError.value = null

        if (song.audioUrl.isBlank()) {
            // Sebagian hasil Deezer tidak membawa preview langsung.
            // Resolve dulu (track/{id} lalu search judul) supaya tombol play tetap jalan.
            resolvePreviewAndPlay(song)
            return
        }

        startPlayer(song, song.audioUrl)
    }

    private fun resolvePreviewAndPlay(song: Song) {
        _isBuffering.value = true
        viewModelScope.launch {
            val preview = withContext(Dispatchers.IO) {
                val directId = song.id.removePrefix("dz_").toLongOrNull()
                val fromTrackId = if (directId != null) {
                    runCatching {
                        deezerApi.track(directId).body()?.preview?.takeIf { it.isNotBlank() }
                    }.getOrNull()
                } else null

                fromTrackId ?: runCatching {
                    deezerApi.search("${song.title} ${song.artist}", limit = 10)
                        .body()
                        ?.data
                        .orEmpty()
                        .firstOrNull { !it.preview.isNullOrBlank() }
                        ?.preview
                }.getOrNull()
            }

            // User sudah ganti lagu sementara resolusi berjalan → abaikan hasil lama.
            if (_currentSong.value?.id != song.id) return@launch

            _isBuffering.value = false
            if (preview.isNullOrBlank()) {
                _isPlaying.value = false
                _playbackError.value =
                    "Lagu ini tidak punya preview audio. Coba lagu lain."
            } else {
                val playable = song.copy(audioUrl = preview, source = song.source.ifBlank { "deezer" })
                _currentSong.value = playable
                startPlayer(playable, preview)
            }
        }
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
        _isBuffering.value = false
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
