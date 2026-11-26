package com.jayra.app.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.util.CoilUtils
import okhttp3.OkHttpClient

/**
 * ImageLoader global Coil dengan User-Agent browser.
 *
 * Penyebab utama poster anime (Kitsu) tidak muncul: CDN menolak request tanpa
 * User-Agent (HTTP 403). Dengan UA browser + auto-detect, semua poster
 * (MangaDex, Kitsu, TVMaze) memuat normal. Cache memori + disk dipakai agar
 * poster konsisten di Home/Search/List/Detail/Bookmark/History.
 */
object MediaImageLoader {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    @Volatile
    private var shared: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        shared?.let { return it }
        return synchronized(this) {
            shared ?: build(context).also { shared = it }
        }
    }

    private fun build(context: Context): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "image/avif,image/webp,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(req)
            }
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .crossfade(true)
            .allowHardware(true)
            .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("media_image_cache"))
                    .maxSizeBytes(80L * 1024 * 1024)
                    .build()
            }
            .build()
    }

    /** Dipakai untuk request gambar tunggal (mis. retry manual). */
    fun request(context: Context, url: String): ImageRequest =
        ImageRequest.Builder(context).data(url).crossfade(true).build()
}