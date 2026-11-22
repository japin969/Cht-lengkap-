package com.jayra.app.ui.media

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient

/**
 * ImageLoader bersama untuk Komik & Anime.
 *
 * Tujuan:
 *  - Poster yang pernah muncul langsung tampil lagi saat scroll balik
 *    (memory cache besar + disk cache).
 *  - Request dimulai begitu item di-compose (tidak menunggu scroll).
 *
 * CATATAN KEAMANAN: OkHttpClient default + trust store sistem.
 * TIDAK ada SSL bypass, tidak ada TrustManager custom.
 */
object MediaImageLoaderProvider {

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }
    }

    private fun build(context: Context): ImageLoader {
        // OkHttp default (trust store sistem). Hanya menambah User-Agent
        // agar CDN tidak menolak request.
        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                    )
                    .build()
                chain.proceed(request)
            }
            .build()

        // ~25% dari available memory untuk bitmap cache.
        val maxMemory = (Runtime.getRuntime().maxMemory() / 4).toInt()
            .coerceAtLeast(16 * 1024 * 1024)

        val memoryCache = MemoryCache.Builder(context)
            .maxSizeBytes(maxMemory)
            .build()

        val diskCache = DiskCache.Builder()
            .directory(context.cacheDir.resolve("media_images"))
            .maxSizeBytes(80L * 1024 * 1024)
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            // Jangan pakai ulang hasil cache secara agresif — tapi tetap cache
            // di memori/disk agar scroll balik instan.
            .respectCacheHeaders(false)
            .build()
    }
}

/** Composition local untuk membagikan loader ke seluruh screen media. */
val LocalMediaImageLoader = androidx.compose.runtime.staticCompositionLocalOf<ImageLoader> {
    error("LocalMediaImageLoader not provided")
}