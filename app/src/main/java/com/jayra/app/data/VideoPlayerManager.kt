package com.jayra.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * VideoPlayerManager - Singleton ExoPlayer manager.
 * Provides a single ExoPlayer instance reused across the app.
 */
@UnstableApi
object VideoPlayerManager {

    private const val TAG = "VideoPlayerManager"
    private const val CACHE_SIZE_BYTES = 200 * 1024 * 1024 // 200MB
    private const val BUFFER_DURATION_MS = 30_000
    private const val MIN_BUFFER_MS = 5_000
    private const val MAX_BUFFER_MS = 60_000
    private const val PRELOAD_DURATION_MS = 3_000

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
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        ),
        "api.mangadex.org" to mapOf(
            "Referer" to "https://mangadex.org/",
            "Origin" to "https://mangadex.org",
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        ),
        "api.tvmaze.com" to mapOf(
            "Referer" to "https://www.tvmaze.com/",
            "Origin" to "https://www.tvmaze.com",
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        ),
        "api.deezer.com" to mapOf(
            "Referer" to "https://www.deezer.com/",
            "Origin" to "https://www.deezer.com",
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        ),
        "uploads.mangadex.org" to mapOf(
            "Referer" to "https://mangadex.org/",
            "Origin" to "https://mangadex.org",
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        ),
        "media.kitsu.io" to mapOf(
            "Referer" to "https://kitsu.io/",
            "Origin" to "https://kitsu.io",
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        )
    )

    fun init(context: Context) {
        _context = context.applicationContext
        buildDataSourceFactory()
        buildExoPlayer()
        Log.d(TAG, "VideoPlayerManager initialized")
    }

    private fun buildDataSourceFactory() {
        if (_dataSourceFactory != null) return

        val context = _context ?: return

        val cacheDir = context.cacheDir.resolve("video_cache")
        _cache = androidx.media3.datasource.cache.SimpleCache(
            cacheDir,
            androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
        )

        val defaultFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setRequestHeaders(mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Accept" to "*/*"
            ))

        _dataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(_cache!!)
            .setUpstreamDataSourceFactory(defaultFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or androidx.media3.datasource.cache.CacheDataSource.FLAG_BLOCK_ON_CACHE)
    }

    private fun buildExoPlayer() {
        if (_exoPlayer != null) return

        val context = _context ?: return

        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setAllocator(androidx.media3.exoplayer.upstream.DefaultAllocator(true, C.BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, BUFFER_DURATION_MS, BUFFER_DURATION_MS)
            .setPrioritizeTimeOverSizeThresholds(true)
            .createDefaultLoadControl()

        _exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory!!)
            )
            .build()

        _exoPlayer!!.addListener(object : androidx.media3.exoplayer.ExoPlayer.Listener {
            override fun onPlayerError(error: androidx.media3.exoplayer.PlaybackException) {
                Log.e(TAG, "ExoPlayer Error: ${error.errorCode} - ${error.message}", error.cause)
            }
        })

        Log.d(TAG, "ExoPlayer created")
    }

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

    fun prepare(url: String, headers: Map<String, String> = emptyMap()) {
        val player = player()
        val mediaItem = buildMediaItem(url, headers)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    fun play() {
        player().playWhenReady = true
    }

    fun pause() {
        player().playWhenReady = false
    }

    fun seekTo(positionMs: Long) {
        player().seekTo(positionMs)
    }

    fun currentPosition(): Long = player().currentPosition

    fun duration(): Long = player().duration

    fun isPlaying(): Boolean = player().playWhenReady && player().playbackState == androidx.media3.common.Player.STATE_READY

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

    fun clear() {
        _exoPlayer?.run {
            stop()
            clearMediaItems()
            playWhenReady = false
        }
    }

    fun prefetch(url: String, headers: Map<String, String> = emptyMap()): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                val mediaItem = buildMediaItem(url, headers)
                val dataSource = dataSourceFactory().createDataSource()
                val dataSpec = androidx.media3.datasource.DataSpec(Uri.parse(url)).apply {
                    headers.forEach { (k, v) -> httpRequestHeaders.set(k, v) }
                }
                dataSource.open(dataSpec)
                var totalRead = 0L
                val buffer = ByteArray(64 * 1024)
                while (totalRead < PRELOAD_DURATION_MS * 1000 / 8) {
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

    private fun buildMediaItem(url: String, extraHeaders: Map<String, String>): MediaItem {
        val uri = Uri.parse(url)
        val host = uri.host ?: ""

        val baseHeaders = videoHeaders[host] ?: emptyMap()
        val allHeaders = baseHeaders + extraHeaders

        return MediaItem.fromUri(uri).buildUpon()
            .setHttpHeaders(allHeaders)
            .build()
    }

    fun onAppBackground() {
        if (isPlaying()) {
            pause()
        }
    }

    fun onAppForeground() {
        // Resume handled by UI
    }
}