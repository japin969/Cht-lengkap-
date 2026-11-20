package com.jayra.app.ui.music

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.jayra.app.data.NetworkClient
import com.jayra.app.data.NexadevApi
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

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var progressUpdateJob: Job? = null
    private var fallbackJob: Job? = null
    private var lastQuery: String = ""

    private val prefs = application.getSharedPreferences("jayra_music_prefs", Context.MODE_PRIVATE)
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    init {
        loadHistory()
        viewModelScope.launch {
            _searchQuery
                .debounce(450L)
                .distinctUntilChanged()
                .collect { query ->
                    executeSearch(query)
                }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(NetworkClient.BROWSER_UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(getApplication())
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            _isBuffering.value = false
                            startProgressTracking()
                        } else {
                            stopProgressTracking()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> _isBuffering.value = true
                            Player.STATE_READY -> {
                                _isBuffering.value = false
                                _playbackError.value = null
                                val d = duration
                                if (d > 0L) _duration.value = d
                            }
                            Player.STATE_ENDED -> {
                                _isBuffering.value = false
                                if (_isRepeat.value) {
                                    seekTo(0L)
                                    playWhenReady = true
                                } else {
                                    onNext()
                                }
                            }
                            Player.STATE_IDLE -> _isBuffering.value = false
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        val current = _currentSong.value
                        if (current != null && current.source == "spotify") {
                            fallbackToYouTubeFullTrack(current)
                        } else {
                            _playbackError.value = "Gagal memutar audio (${error.errorCodeName})"
                        }
                    }
                })
                exoPlayer = this
            }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun searchNow(query: String = _searchQuery.value) {
        viewModelScope.launch {
            executeSearch(query)
        }
    }

    private fun loadHistory() {
        val stored = prefs.getString("music_history", "") ?: ""
        _searchHistory.value = if (stored.isBlank()) emptyList()
        else stored.split("||").filter { it.isNotBlank() }
    }

    private fun addHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val capped = current.take(15)
        _searchHistory.value = capped
        prefs.edit().putString("music_history", capped.joinToString("||")).apply()
    }

    fun onHistoryClick(query: String) {
        _searchQuery.value = query
        searchNow(query)
    }

    fun onClearHistory() {
        _searchHistory.value = emptyList()
        prefs.edit().remove("music_history").apply()
    }

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    private suspend fun executeSearch(rawQuery: String) {
        val query = rawQuery.trim()
        lastQuery = query
        if (query.isBlank()) {
            _uiState.value = MusicUiState.Empty
            return
        }

        _uiState.value = MusicUiState.Loading
        val results = mutableListOf<Song>()
        try {
            val spotifyDeferred = viewModelScope.async(Dispatchers.IO) {
                runCatching {
                    val r = nexadevApi.searchSpotify(query)
                    if (r.isSuccessful && r.body()?.status == true) {
                        val payload = r.body()?.data ?: r.body()?.result
                        payload?.toSong("sp_${query.hashCode()}")
                            ?.takeIf { it.audioUrl.isNotBlank() }
                    } else null
                }.getOrNull()
            }

            val ytDeferred = viewModelScope.async(Dispatchers.IO) {
                runCatching {
                    val r = nexadevApi.searchYouTube(query)
                    if (r.isSuccessful && r.body()?.status == true) {
                        r.body()?.toSong("yt_${query.hashCode()}")
                            ?.takeIf { it.audioUrl.isNotBlank() }
                    } else null
                }.getOrNull()
            }

            spotifyDeferred.await()?.let { results.add(it) }
            ytDeferred.await()?.let { results.add(it) }

            if (results.isNotEmpty()) {
                _uiState.value = MusicUiState.Success(results)
                addHistory(query)
            } else {
                _uiState.value = MusicUiState.Error("Lagu \"$query\" tidak ditemukan atau server sibuk")
            }
        } catch (e: Exception) {
            _uiState.value = MusicUiState.Error(e.localizedMessage ?: "Gagal mencari lagu")
        }
    }

    fun onSongClick(song: Song) {
        if (_currentSong.value?.id == song.id && exoPlayer != null) {
            onPlayPauseClick()
            return
        }
        _currentSong.value = song
        playSong(song)
    }

    private fun playSong(song: Song) {
        fallbackJob?.cancel()
        _playbackError.value = null
        _currentPosition.value = 0L
        _duration.value = parseDurationMs(song.duration)

        if (song.audioUrl.isBlank()) {
            if (song.source == "spotify") {
                fallbackToYouTubeFullTrack(song)
            } else {
                _isPlaying.value = false
                _playbackError.value = "Link audio tidak tersedia"
            }
            return
        }

        try {
            _isBuffering.value = true
            val player = ensurePlayer()
            player.setMediaItem(MediaItem.fromUri(song.audioUrl))
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            _isBuffering.value = false
            _isPlaying.value = false
            _playbackError.value = e.localizedMessage ?: "Gagal memutar lagu"
        }
    }

    private fun fallbackToYouTubeFullTrack(song: Song) {
        fallbackJob?.cancel()
        _isBuffering.value = true
        fallbackJob = viewModelScope.launch {
            val ytSong = withContext(Dispatchers.IO) {
                runCatching {
                    val q = "${song.title} ${song.artist}".trim()
                    val r = nexadevApi.searchYouTube(q)
                    if (r.isSuccessful && r.body()?.status == true) {
                        r.body()?.toSong(song.id)?.takeIf { it.audioUrl.isNotBlank() }
                    } else null
                }.getOrNull()
            }

            if (ytSong != null) {
                val merged = song.copy(audioUrl = ytSong.audioUrl, source = "youtube")
                _currentSong.value = merged
                playSong(merged)
            } else {
                _isBuffering.value = false
                _playbackError.value = "Gagal memutar stream full track"
            }
        }
    }

    fun onPlayPauseClick() {
        val player = exoPlayer ?: run {
            _currentSong.value?.let { playSong(it) }
            return
        }
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
                player.prepare()
            }
            player.play()
        }
    }

    fun onSeekTo(positionMs: Long) {
        exoPlayer?.let { player ->
            player.seekTo(positionMs.coerceAtLeast(0L))
            _currentPosition.value = positionMs
        }
    }

    fun onNext() {
        val currentList = (uiState.value as? MusicUiState.Success)?.songs.orEmpty()
        if (currentList.isEmpty()) return
        val currentIndex = currentList.indexOfFirst { it.id == _currentSong.value?.id }
        val nextIndex = if (_isShuffle.value && currentList.size > 1) {
            var candidate = currentList.indices.random()
            if (candidate == currentIndex) candidate = (currentIndex + 1) % currentList.size
            candidate
        } else {
            if (currentIndex == -1 || currentIndex >= currentList.size - 1) 0 else currentIndex + 1
        }
        val nextSong = currentList[nextIndex]
        _currentSong.value = nextSong
        playSong(nextSong)
    }

    fun onPrev() {
        val currentList = (uiState.value as? MusicUiState.Success)?.songs.orEmpty()
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
            executeSearch(lastQuery.ifBlank { _searchQuery.value })
        }
    }

    private fun parseDurationMs(durationText: String): Long {
        val parts = durationText.trim().split(":")
        return try {
            when (parts.size) {
                2 -> ((parts[0].toLong() * 60L) + parts[1].toLong()) * 1000L
                3 -> ((parts[0].toLong() * 3600L) + (parts[1].toLong() * 60L) + parts[2].toLong()) * 1000L
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun startProgressTracking() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                val player = exoPlayer ?: break
                if (!player.isPlaying) break
                _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                val d = player.duration
                if (d > 0L) _duration.value = d
                delay(500L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressUpdateJob?.cancel()
    }

    private fun releasePlayer() {
        fallbackJob?.cancel()
        stopProgressTracking()
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
        } catch (_: Exception) {
        }
        exoPlayer = null
        _isPlaying.value = false
        _isBuffering.value = false
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
