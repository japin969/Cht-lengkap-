package com.jayra.app.ui.media

import android.content.Context
import androidx.media3.common.MediaItem
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
 * - Hardware decoder (default MediaCodec, tanpa software fallback).
 * - CacheDataSource (cache 100MB) + DefaultDataSource.
 * - prepare/play/pause/release yang benar.
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

    /**
     * Ambil player singleton. Jika belum ada / sudah di-release, buat baru.
     * Hardware decoder only: setEnableDecoderFallback(false).
     */
    @Synchronized
    fun get(context: Context): ExoPlayer {
        player?.let { return it }
        val mediaSourceFactory = DefaultMediaSourceFactory(buildDataSourceFactory(context))
        // Hardware decoder only: DefaultRenderersFactory tanpa fallback software.
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(false)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setRenderersFactory(renderersFactory)
            .build()
            .also { player = it }
    }

    /** Set video ke player (loop/repeat optional). Tidak membuat player di Composable. */
    fun setVideo(context: Context, url: String, loop: Boolean = false) {
        val p = get(context)
        p.setMediaItem(MediaItem.fromUri(url))
        p.repeatMode = if (loop) androidx.media3.common.Player.REPEAT_MODE_ALL
            else androidx.media3.common.Player.REPEAT_MODE_OFF
        p.prepare()
    }

    fun play() { player?.play() }
    fun pause() { player?.pause() }
    fun release() {
        player?.release()
        player = null
    }
    fun releaseCache() {
        cache?.release()
        cache = null
    }
    fun current(): ExoPlayer? = player
}