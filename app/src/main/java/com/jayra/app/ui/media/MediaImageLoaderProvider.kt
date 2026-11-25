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
        // OkHttp default (trust store sistem) + header browser.
        // Banyak CDN anime (anichin.moe, donghive.vip, i0.wp.com/animasu.love)
        // menolak request tanpa User-Agent / Referer yang wajar (403 Cloudflare).
        // Interceptor ini menambahkan header yang benar per host sumber gambar.
        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.toString()
                val referer = refererFor(url)
                val builder = original.newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                if (referer != null) builder.header("Referer", referer)
                chain.proceed(builder.build())
            }
            .build()

        // ~35% dari available memory untuk bitmap cache (lebih banyak poster
        // cached -> scroll & balik halaman instan, tidak putih).
        val maxMemory = (Runtime.getRuntime().maxMemory() * 35 / 100).toInt()
            .coerceAtLeast(24 * 1024 * 1024)

        val memoryCache = MemoryCache.Builder(context)
            .maxSizeBytes(maxMemory)
            .build()

        val diskCache = DiskCache.Builder()
            .directory(context.cacheDir.resolve("media_images"))
            .maxSizeBytes(150L * 1024 * 1024)
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            // Cache aktif penuh: memory + disk + network.
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Jangan pakai ulang hasil cache secara agresif — tapi tetap cache
            // di memori/disk agar scroll balik instan.
            .respectCacheHeaders(false)
            .build()
    }

    /**
     * Referer yang wajar per domain sumber poster. CDN anime sering menolak
     * request tanpa Referer dari domain asalnya.
     */
    private fun refererFor(url: String): String? {
        val host = runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() ?: return null
        return when {
            host.contains("anichin.") -> "https://anichin.moe/"
            host.contains("donghive.") -> "https://donghive.vip/"
            host.contains("animasu") || host.contains("wp.com") -> "https://animasu.love/"
            host.contains("sankavollerei") -> "https://www.sankavollerei.web.id/"
            else -> null
        }
    }
}

/** Composition local untuk membagikan loader ke seluruh screen media. */
val LocalMediaImageLoader = androidx.compose.runtime.staticCompositionLocalOf<ImageLoader> {
    error("LocalMediaImageLoader not provided")
}