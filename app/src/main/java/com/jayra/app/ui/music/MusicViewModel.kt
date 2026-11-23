package com.jayra.app.ui.music

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.NexadevApi
import com.jayra.app.data.DeezerApi
import com.jayra.app.data.DeezerTrack
import com.jayra.app.data.RavyMusicApi
import com.jayra.app.data.RavySong
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
    private val deezerApi: DeezerApi = DeezerApi.create(),
    private val ravyApi: RavyMusicApi = RavyMusicApi.create()
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
        if (query.isBlank()) {
            _uiState.value = MusicUiState.Success(emptyList())
            return
        }
        lastQuery = query
        _uiState.value = MusicUiState.Loading
        try {
            // Sumber utama: Rayy Music (search + full mp3). 20 hasil/query.
            val ravyResp = withContext(Dispatchers.IO) { ravyApi.search(query) }
            val ravySongs = ravyResp.body()?.result?.songs.orEmpty().map { it.toSong() }
            if (ravySongs.isNotEmpty()) {
                _uiState.value = MusicUiState.Success(ravySongs)
                addHistory(query)
                return
            }
            // Fallback: Deezer.
            val response = withContext(Dispatchers.IO) { deezerApi.search(query) }
            val songs = response.body()?.data.orEmpty().map { it: com.jayra.app.data.DeezerTrack -> it.toSong() }
            if (songs.isNotEmpty()) {
                _uiState.value = MusicUiState.Success(songs)
                addHistory(query)
                return
            }
            val fallback = curatedSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
            }
            _uiState.value = if (fallback.isNotEmpty()) MusicUiState.Success(fallback)
            else MusicUiState.Empty
        } catch (e: Exception) {
            _uiState.value = MusicUiState.Error(e.localizedMessage ?: "Gagal mencari lagu")
        }
    }

    /** Konversi lagu Rayy -> Song (audio di-resolve saat play). */
    private fun RavySong.toSong(): Song = Song(
        id = "ravy_" + (videoId ?: System.currentTimeMillis().toString()),
        title = title ?: "Tanpa Judul",
        artist = artist ?: "Unknown",
        album = album ?: "",
        duration = duration ?: "0:00",
        coverUrl = thumbnail ?: "",
        audioUrl = "",
        source = "ravy"
    )

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

        if (song.audioUrl.isBlank()) {
            // Lagu Rayy: resolve mp3 dulu via ytplay (paralel, async).
            viewModelScope.launch {
                val url = resolveRavyAudio(song)
                if (url != null) {
                    playResolved(song, url)
                } else {
                    _isPlaying.value = false
                }
            }
            return
        }
        playResolved(song, song.audioUrl)
    }

    /** Resolve URL mp3 dari Rayy ytplay. */
    private suspend fun resolveRavyAudio(song: Song): String? = try {
        // cari videoId dari id "ravy_<videoId>"
        val vid = song.id.removePrefix("ravy_")
        val ytUrl = "https://music.youtube.com/watch?v=" + vid
        val resp = withContext(Dispatchers.IO) {
            ravyApi.ytPlay(com.jayra.app.data.RavyYtPlayRequest(ytUrl))
        }
        resp.body()?.result?.download?.audio
    } catch (e: Exception) {
        null
    }

    private fun playResolved(song: Song, audioUrl: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioUrl)
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
