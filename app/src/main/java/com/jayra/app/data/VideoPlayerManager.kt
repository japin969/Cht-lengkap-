package com.jayra.app.data

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * VideoPlayerManager - Singleton ExoPlayer manager.
 *
 * Features:
 * 1. Single ExoPlayer instance reused across the app
 * 2. Custom DataSource.Factory with per-domain headers (Referer, UA, Cookie)
 * 3. Media3 cache for video prefetching and offline support
 * 4. Hardware decoder preference with software fallback
 * 5. Optimized LoadControl for smooth playback
 * 6. Error listener that logs actual ExoPlayer errors
 * 7. Lifecycle management (pause on background, release on destroy)
 * 8. Prefetch support for next video
 * 9. Coroutine-based async operations
 */
@UnstableApi
object VideoPlayerManager {

    private const val TAG = "VideoPlayerManager"
    private const val CACHE_SIZE_BYTES = 200 * 1024 * 1024 // 200MB
    private const val BUFFER_DURATION_MS = 30_000 // 30 seconds
    private const val MIN_BUFFER_MS = 5_000
    private const val MAX_BUFFER_MS = 60_000
    private const val PRELOAD_DURATION_MS = 3_000 // 3 seconds prefetch

    // ═══════════════════════════════════════════
    // SINGLETON EXOPLAYER
    // ═══════════════════════════════════════════
    @Volatile
    private var _exoPlayer: ExoPlayer? = null

    @Volatile
    private var _dataSourceFactory: DataSource.Factory? = null

    @Volatile
    private var _cache: androidx.media3.datasource.cache.Cache? = null

    @Volatile
    private var _context: Context? = null

    // Video headers per domain
    private val videoHeaders = mapOf(
        "kitsu.io" to mapOf(
            "Referer" to "https://kitsu.io/",
            "Origin" to "https://kitsu.io",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        ),
        "api.mangadex.org" to mapOf(
            "Referer" to "https://mangadex.org/",
            "Origin" to "https://mangadex.org",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        ),
        "api.tvmaze.com" to mapOf(
            "Referer" to "https://www.tvmaze.com/",
            "Origin" to "https://www.tvmaze.com",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        ),
        "api.deezer.com" to mapOf(
            "Referer" to "https://www.deezer.com/",
            "Origin" to "https://www.deezer.com",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        ),
        "uploads.mangadex.org" to mapOf(
            "Referer" to "https://mangadex.org/",
            "Origin" to "https://mangadex.org",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        ),
        "media.kitsu.io" to mapOf(
            "Referer" to "https://kitsu.io/",
            "Origin" to "https://kitsu.io",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        ),
        "e-cdns-images.dzcdn.net" to mapOf(
            "Referer" to "https://www.deezer.com/",
            "Origin" to "https://www.deezer.com",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        ),
        "cdns-images.dzcdn.net" to mapOf(
            "Referer" to "https://www.deezer.com/",
            "Origin" to "https://www.deezer.com",
            "User-Agent" to NetworkModule.BROWSER_USER_AGENT
        )
    )

    // ═══════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════
    fun init(context: Context) {
        _context = context.applicationContext
        buildDataSourceFactory()
        buildExoPlayer()
        Log.d(TAG, "VideoPlayerManager initialized")
    }

    private fun buildDataSourceFactory() {
        if (_dataSourceFactory != null) return

        val context = _context ?: return

        // Build cache
        val cacheDir = context.cacheDir.resolve("video_cache")
        _cache = androidx.media3.datasource.cache.SimpleCache(
            cacheDir,
            androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
        )

        // Default HTTP data source factory with custom headers
        val defaultFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setRequestHeaders(mapOf(
                "User-Agent" to NetworkModule.BROWSER_USER_AGENT,
                "Accept" to "*/*"
            ))

        // Wrap with cache
        _dataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(_cache!!)
            .setUpstreamDataSourceFactory(defaultFactory)
            .setCacheWriteDataSinkFactory(null) // Write to cache
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or CacheDataSource.FLAG_BLOCK_ON_CACHE)
    }

    private fun buildExoPlayer() {
        if (_exoPlayer != null) return

        val context = _context ?: return

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage("en")
                    .setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
                    .setRendererDisabled(C.TRACK_TYPE_AUDIO, false)
            )
        }

        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_DURATION_MS,
                BUFFER_DURATION_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .createDefaultLoadControl()

        _exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    dataSourceFactory!!
                )
            )
            .build()

        // Add error listener
        _exoPlayer!!.addListener(object : ExoPlayer.Listener {
            override fun onPlayerError(error: androidx.media3.exoplayer.PlaybackException) {
                Log.e(TAG, "ExoPlayer Error: ${error.errorCode} - ${error.message}", error.cause)
                logDetailedError(error)
            }

            override fun onPlaybackStateChanged(@androidx.media3.common.Player.State state: Int) {
                Log.d(TAG, "Playback state: $state")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "Is playing: $isPlaying")
            }
        })

        Log.d(TAG, "ExoPlayer created")
    }

    private fun logDetailedError(error: androidx.media3.exoplayer.PlaybackException) {
        val cause = error.cause
        var detailedCause = cause
        while (detailedCause?.cause != null) {
            detailedCause = detailedCause.cause
        }
        Log.e(TAG, "Root cause: ${detailedCause?.className}: ${detailedCause?.message}")

        // Log HTTP response code if available
        if (cause is HttpDataSource.HttpDataSourceException) {
            Log.e(TAG, "HTTP Response Code: ${cause.responseCode}")
            Log.e(TAG, "HTTP Response Message: ${cause.responseMessage}")
            Log.e(TAG, "Request URL: ${cause.uri}")
        }
    }

    // ═══════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════

    /** Get the singleton ExoPlayer instance */
    fun player(): ExoPlayer {
        if (_exoPlayer == null) {
            if (_context != null) {
                init(_context!!)
            } else {
                throw IllegalStateException("VideoPlayerManager not initialized. Call init(context) first.")
            }
        }
        return _exoPlayer!!
    }

    /** Get the DataSource.Factory for custom MediaSource creation */
    fun dataSourceFactory(): DataSource.Factory {
        if (_dataSourceFactory == null) {
            if (_context != null) {
                init(_context!!)
            } else {
                throw IllegalStateException("VideoPlayerManager not initialized. Call init(context) first.")
            }
        }
        return _dataSourceFactory!!
    }

    /** Prepare player with URL and optional headers */
    fun prepare(url: String, headers: Map<String, String> = emptyMap()) {
        val player = player()
        val mediaItem = buildMediaItem(url, headers)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    /** Prepare player with MediaItem */
    fun prepare(mediaItem: MediaItem) {
        player().apply {
            setMediaItem(mediaItem)
            prepare()
        }
    }

    /** Start playback */
    fun play() {
        player().playWhenReady = true
    }

    /** Pause playback */
    fun pause() {
        player().playWhenReady = false
    }

    /** Seek to position in milliseconds */
    fun seekTo(positionMs: Long) {
        player().seekTo(positionMs)
    }

    /** Get current position */
    fun currentPosition(): Long = player().currentPosition

    /** Get duration */
    fun duration(): Long = player().duration

    /** Check if playing */
    fun isPlaying(): Boolean = player().playWhenReady && player().playbackState == androidx.media3.common.Player.STATE_READY

    /** Release player and resources */
    fun release() {
        _exoPlayer?.run {
            stop()
            release()
        }
        _exoPlayer = null
        _cache?.release()
        _cache = null
        _dataSourceFactory = null
        Log.d(TAG, "VideoPlayerManager released")
    }

    /** Pause and clear media item (keep player for reuse) */
    fun clear() {
        _exoPlayer?.run {
            stop()
            clearMediaItems()
            playWhenReady = false
        }
    }

    /** Prefetch video (first 3 seconds) for faster startup */
    fun prefetch(url: String, headers: Map<String, String> = emptyMap()): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                val mediaItem = buildMediaItem(url, headers)
                val dataSource = dataSourceFactory().createDataSource()
                val dataSpec = androidx.media3.datasource.DataSpec(Uri.parse(url))
                    .apply {
                        headers.forEach { (k, v) -> httpRequestHeaders.set(k, v) }
                    }
                dataSource.open(dataSpec)
                var totalRead = 0L
                val buffer = ByteArray(64 * 1024)
                while (totalRead < PRELOAD_DURATION_MS * 1000 / 8) { // Rough estimate for 3 seconds
                    val read = dataSource.read(buffer)
                    if (read == C.RESULT_END_OF_INPUT) break
                    totalRead += read
                }
                dataSource.close()
                Log.d(TAG, "Prefetched $totalRead bytes for $url")
            } catch (e: Exception) {
                Log.w(TAG, "Prefetch failed for $url: ${e.message}")
            }
        }
    }

    /** Add video metadata for notification/lockscreen */
    fun setMetadata(title: String?, artist: String?, artworkUrl: String?) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title ?: "")
            .setArtist(artist ?: "")
            .setArtworkUri(artworkUrl?.let { Uri.parse(it) })
            .build()
        player().setMediaMetadata(metadata)
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    private fun buildMediaItem(url: String, extraHeaders: Map<String, String>): MediaItem {
        val uri = Uri.parse(url)
        val host = uri.host ?: ""

        val baseHeaders = videoHeaders[host] ?: emptyMap()
        val allHeaders = baseHeaders + extraHeaders

        return MediaItem.fromUri(uri).buildUpon()
            .setHttpHeaders(allHeaders)
            .build()
    }

    // ═══════════════════════════════════════════
    // LIFECYCLE HELPERS (call from Application/Activity)
    // ═══════════════════════════════════════════

    fun onAppBackground() {
        if (isPlaying()) {
            pause()
        }
    }

    fun onAppForeground() {
        // Resume handled by UI
    }
}

import androidx.media3.datasource.cache.CacheDataSource
import kotlinx.coroutines.Job