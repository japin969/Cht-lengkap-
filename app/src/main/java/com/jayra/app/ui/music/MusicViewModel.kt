package com.jayra.app.ui.music

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AppleMusicApi
import com.jayra.app.data.AppleMusicItem
import com.jayra.app.data.AppleMusicPreviewResolver
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

/**
 * Music search: https://api.siputzx.my.id/api/s/applemusic?query=...&region=id
 * Semua hasil diparse ke [Song] lalu dirender dengan UI kita sendiri (bukan WebView).
 * Audio preview di-resolve dari halaman album Apple Music (HTTP + parsing, bukan WebView).
 */
@OptIn(FlowPreview::class)
class MusicViewModel @JvmOverloads constructor(
    application: Application,
    private val appleMusicApi: AppleMusicApi = AppleMusicApi.create()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MusicUiState>(MusicUiState.Empty)
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

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
    private var metaJob: Job? = null
    private var lastQuery: String = ""

    // History
    private val prefs = application.getSharedPreferences("jayra_music_prefs", android.content.Context.MODE_PRIVATE)
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    init {
        loadHistory()

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
            val response = withContext(Dispatchers.IO) { appleMusicApi.search(query) }
            val items = response.body()?.data.orEmpty()
            val songs = items.mapIndexed { index, item -> item.toSong(index) }
            if (songs.isNotEmpty()) {
                _uiState.value = MusicUiState.Success(songs)
                addHistory(query)
                prefetchMeta(songs)
            } else {
                _uiState.value = MusicUiState.Empty
            }
        } catch (e: Exception) {
            _uiState.value = MusicUiState.Error(humanError(e))
        }
    }

    /** Ambil preview + durasi per album (1 request per album, bukan per lagu). */
    private fun prefetchMeta(songs: List<Song>) {
        metaJob?.cancel()
        metaJob = viewModelScope.launch {
            val groups = songs
                .filter { it.audioUrl.isBlank() && it.pageUrl.isNotBlank() }
                .groupBy { AppleMusicPreviewResolver.albumKey(it.pageUrl) }
            groups.forEach { (_, group) ->
                val link = group.firstOrNull()?.pageUrl ?: return@forEach
                val meta = AppleMusicPreviewResolver.resolveAlbum(link)
                if (meta.isNotEmpty()) {
                    applyMeta(meta)
                    delay(120)
                }
            }
        }
    }

    private fun applyMeta(meta: Map<String, AppleMusicPreviewResolver.TrackMeta>) {
        val state = _uiState.value as? MusicUiState.Success ?: return
        val updated = state.songs.map { song ->
            val id = AppleMusicPreviewResolver.songId(song.pageUrl)
            val m = if (id != null) meta[id] else null
            if (m != null) {
                song.copy(
                    audioUrl = song.audioUrl.ifBlank { m.audioUrl.orEmpty() },
                    duration = song.duration.ifBlank {
                        m.durationMs?.let { formatDurationMs(it) } ?: ""
                    }
                )
            } else song
        }
        _uiState.value = MusicUiState.Success(updated)
        _currentSong.value?.let { current ->
            updated.firstOrNull { it.id == current.id }?.let { _currentSong.value = it }
        }
    }

    private fun AppleMusicItem.toSong(index: Int): Song {
        val artistName = artist.orEmpty()
            .substringAfter(" · ", artist.orEmpty())
            .trim()
        val albumName = link.orEmpty()
            .substringAfter("/album/", "")
            .substringBefore("/")
            .replace('-', ' ')
            .ifBlank { "" }
        val idPart = AppleMusicPreviewResolver.songId(link) ?: "am_${link.hashCode()}_$index"
        return Song(
            id = "am_$idPart",
            title = title?.trim().orEmpty().ifBlank { "Tanpa Judul" },
            artist = artistName.ifBlank { "Unknown Artist" },
            album = albumName,
            duration = "",
            coverUrl = image.orEmpty(),
            audioUrl = "",
            pageUrl = link.orEmpty(),
            source = "applemusic"
        )
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
        viewModelScope.launch {
            var url = song.audioUrl
            if (url.isBlank()) {
                _isPreparing.value = true
                val meta = try {
                    AppleMusicPreviewResolver.resolveTrack(song.pageUrl)
                } catch (_: Exception) {
                    null
                }
                _isPreparing.value = false
                url = meta?.audioUrl.orEmpty()
                if (url.isNotBlank() || meta?.durationMs != null) {
                    val enriched = song.copy(
                        audioUrl = url,
                        duration = song.duration.ifBlank {
                            meta?.durationMs?.let { formatDurationMs(it) } ?: ""
                        }
                    )
                    _currentSong.value = enriched
                    applySingleUpdate(enriched)
                }
            }
            if (url.isBlank()) {
                _isPlaying.value = false
                return@launch
            }
            startMediaPlayer(url)
        }
    }

    private fun applySingleUpdate(song: Song) {
        val state = _uiState.value as? MusicUiState.Success ?: return
        val updated = state.songs.map { if (it.id == song.id) song else it }
        _uiState.value = MusicUiState.Success(updated)
    }

    private fun startMediaPlayer(url: String) {
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
    }

    private fun formatDurationMs(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private fun humanError(e: Exception): String {
        val raw = e.localizedMessage.orEmpty()
        return when {
            raw.contains("UnknownHost", true) || raw.contains("resolve", true) ->
                "Tidak ada koneksi internet. Coba lagi."
            raw.contains("timeout", true) || raw.contains("timed out", true) ->
                "Server musik terlalu lama merespons. Coba lagi."
            else -> "Gagal mencari lagu. Coba lagi."
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
        metaJob?.cancel()
    }
}
