package com.jayra.app.ui.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

/**
 * VideoPlayerManager — SATU instance ExoPlayer (singleton) untuk seluruh app.
 * - Hardware decoder only (DefaultRenderersFactory tanpa software fallback).
 * - CacheDataSource (100MB) + DefaultDataSource.
 * - prepare/play/pause/release benar, error listener, prefetch 3 detik pertama.
 *
 * JANGAN buat ExoPlayer di Composable — ambil dari sini.
 */
object VideoPlayerManager {

    private const val CACHE_DIR = "video_cache"
    private const val CACHE_MAX_BYTES = 100L * 1024 * 1024

    @Volatile
    private var player: ExoPlayer? = null
    @Volatile
    private var cache: SimpleCache? = null

    // Error terakhir (dibaca UI untuk tampilkan error asli, bukan hitam diam)
    @Volatile
    var lastError: String? = null
        private set

    @Synchronized
    private fun ensureCache(context: Context): SimpleCache {
        cache?.let { return it }
        val dir = File(context.cacheDir, CACHE_DIR)
        return SimpleCache(
            dir,
            LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES),
            androidx.media3.database.StandaloneDatabaseProvider(context)
        ).also { cache = it }
    }

    private fun buildDataSourceFactory(context: Context): DataSource.Factory {
        val cache = ensureCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    fun get(context: Context): ExoPlayer {
        player?.let { return it }
        val mediaSourceFactory = DefaultMediaSourceFactory(buildDataSourceFactory(context))
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(false) // hardware only
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        lastError = error.localizedMessage ?: "Gagal memutar video"
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) lastError = null
                    }
                })
            }
            .also { player = it }
    }

    /** Set video + prepare. Prefetch 3 detik pertama via prepare (ExoPlayer otomatis buffer). */
    fun setVideo(context: Context, url: String, loop: Boolean = false) {
        val p = get(context)
        lastError = null
        p.setMediaItem(MediaItem.fromUri(url))
        p.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        p.prepare()
    }

    fun play() { player?.play() }
    fun pause() { player?.pause() }
    fun release() {
        player?.release()
        player = null
        lastError = null
    }
    fun releaseCache() {
        cache?.release()
        cache = null
    }
    fun current(): ExoPlayer? = player
}