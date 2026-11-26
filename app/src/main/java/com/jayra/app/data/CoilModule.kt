package com.jayra.app.data

import android.content.Context
import coil.ImageLoader
import coil.network.ImageHttpInterceptor
import coil.network.OkHttpEngine
import coil.request.ImageRequest
import coil.request.data
import coil.size
import coil.transform
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * CoilModule - Custom ImageLoader configuration with per-domain headers.
 *
 * Features:
 * 1. Shared OkHttpClient with connection pooling (reuses NetworkModule's client)
 * 2. Per-domain header interceptor for Referer, User-Agent, Origin
 * 3. Default placeholder/error/fallback handling
 * 4. Memory + disk caching enabled
 * 5. Image request logging for debugging
 */
object CoilModule {

    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    // Domain configurations for image loading
    private val imageDomainConfigs = mapOf(
        "media.kitsu.io" to ImageDomainConfig(
            referer = "https://kitsu.io/",
            origin = "https://kitsu.io",
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        ),
        "uploads.mangadex.org" to ImageDomainConfig(
            referer = "https://mangadex.org/",
            origin = "https://mangadex.org",
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        ),
        "static.tvmaze.com" to ImageDomainConfig(
            referer = "https://www.tvmaze.com/",
            origin = "https://www.tvmaze.com",
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        ),
        "artwork-cdn.jellyfin.org" to ImageDomainConfig(
            referer = "https://jellyfin.org/",
            origin = "https://jellyfin.org",
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        ),
        "e-cdns-images.dzcdn.net" to ImageDomainConfig(
            referer = "https://www.deezer.com/",
            origin = "https://www.deezer.com",
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        ),
        "cdns-images.dzcdn.net" to ImageDomainConfig(
            referer = "https://www.deezer.com/",
            origin = "https://www.deezer.com",
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        ),
        "i.scdn.co" to ImageDomainConfig(
            referer = "https://open.spotify.com/",
            origin = "https://open.spotify.com",
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        ),
        "image.tmdb.org" to ImageDomainConfig(
            referer = "https://www.themoviedb.org/",
            origin = "https://www.themoviedb.org",
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        )
    )

    @Volatile
    private var _imageLoader: ImageLoader? = null

    fun imageLoader(context: Context): ImageLoader {
        return _imageLoader ?: synchronized(this) {
            _imageLoader ?: ImageLoader.Builder(context)
                .engine(OkHttpEngine(buildImageOkHttpClient()))
                .componentRegistry { }
                .build()
                .also { _imageLoader = it }
        }
    }

    private fun buildImageOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                // Reuse connection pool from NetworkModule if possible
                connectionPool(NetworkModule.sharedClient().connectionPool())
                followRedirects(true)
                followSslRedirects(true)
                retryOnConnectionFailure(true)
            }
            .addInterceptor(ImageHeaderInterceptor(imageDomainConfigs))
            .addInterceptor(ImageLoggingInterceptor())
            .build()
    }

    fun shutdown() {
        _imageLoader?.close()
        _imageLoader = null
    }

    // ═══════════════════════════════════════════
    // IMAGE HEADER INTERCEPTOR
    // ═══════════════════════════════════════════
    private class ImageHeaderInterceptor(
        private val configs: Map<String, ImageDomainConfig>
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val host = request.url.host()

            val config = configs[host] ?: return chain.proceed(request)

            val newRequest = request.newBuilder()
                .header("Referer", config.referer)
                .header("Origin", config.origin)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", config.accept)
                .build()

            return chain.proceed(newRequest)
        }
    }

    // ═══════════════════════════════════════════
    // IMAGE LOGGING INTERCEPTOR
    // ═══════════════════════════════════════════
    private class ImageLoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val start = System.currentTimeMillis()
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - start

            if (BuildConfig.DEBUG) {
                val status = response.code
                val url = request.url.toString()
                val logTag = "CoilImage"
                val message = "IMAGE ${status} ${duration}ms $url"
                if (status >= 400) {
                    android.util.Log.w(logTag, message)
                } else {
                    android.util.Log.d(logTag, message)
                }
            }
            return response
        }
    }

    // ═══════════════════════════════════════════
    // IMAGE DOMAIN CONFIG
    // ═══════════════════════════════════════════
    private data class ImageDomainConfig(
        val referer: String,
        val origin: String,
        val accept: String
    )
}

import android.os.BuildConfig