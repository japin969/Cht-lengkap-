package com.jayra.app.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * NetworkModule - Centralized network configuration.
 *
 * Provides:
 * 1. Single shared OkHttpClient with connection pooling
 * 2. LoggingInterceptor for debug builds
 * 3. Per-domain header interceptors (Referer, User-Agent, Origin)
 * 4. Centralized timeout configuration
 * 5. Moshi converter with KotlinJsonAdapterFactory
 * 6. Retrofit builder factory
 */
object NetworkModule {

    // ═══════════════════════════════════════════
    // TIMEOUT CONSTANTS
    // ═══════════════════════════════════════════
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    // ═══════════════════════════════════════════
    // USER AGENTS
    // ═══════════════════════════════════════════
    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    // ═══════════════════════════════════════════
    // DOMAIN CONFIGURATIONS
    // ═══════════════════════════════════════════
    private val domainConfigs = mapOf(
        "kitsu.io" to DomainConfig(
            referer = "https://kitsu.io/",
            origin = "https://kitsu.io",
            userAgent = BROWSER_USER_AGENT,
            accept = "application/vnd.api+json"
        ),
        "api.mangadex.org" to DomainConfig(
            referer = "https://mangadex.org/",
            origin = "https://mangadex.org",
            userAgent = BROWSER_USER_AGENT,
            accept = "application/json"
        ),
        "api.tvmaze.com" to DomainConfig(
            referer = "https://www.tvmaze.com/",
            origin = "https://www.tvmaze.com",
            userAgent = BROWSER_USER_AGENT,
            accept = "application/json"
        ),
        "api.deezer.com" to DomainConfig(
            referer = "https://www.deezer.com/",
            origin = "https://www.deezer.com",
            userAgent = BROWSER_USER_AGENT,
            accept = "application/json"
        ),
        "api.siputzx.my.id" to DomainConfig(
            referer = "https://siputzx.my.id/",
            origin = "https://siputzx.my.id",
            userAgent = BROWSER_USER_AGENT,
            accept = "application/json"
        ),
        "apii.nexadev.my.id" to DomainConfig(
            referer = "https://nexadev.my.id/",
            origin = "https://nexadev.my.id",
            userAgent = BROWSER_USER_AGENT,
            accept = "application/json"
        ),
        "api.nexadev.my.id" to DomainConfig(
            referer = "https://nexadev.my.id/",
            origin = "https://nexadev.my.id",
            userAgent = BROWSER_USER_AGENT,
            accept = "application/json"
        ),
        "uploads.mangadex.org" to DomainConfig(
            referer = "https://mangadex.org/",
            origin = "https://mangadex.org",
            userAgent = BROWSER_USER_AGENT,
            accept = "image/*,*/*;q=0.8"
        ),
        "media.kitsu.io" to DomainConfig(
            referer = "https://kitsu.io/",
            origin = "https://kitsu.io",
            userAgent = BROWSER_USER_AGENT,
            accept = "image/*,*/*;q=0.8"
        )
    )

    // ═══════════════════════════════════════════
    // SINGLETON OKHTTPCLIENT
    // ═══════════════════════════════════════════
    @Volatile
    private var _sharedClient: OkHttpClient? = null

    fun sharedClient(): OkHttpClient {
        return _sharedClient ?: synchronized(this) {
            _sharedClient ?: buildClient().also { _sharedClient = it }
        }
    }

    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)

        // Logging interceptor (debug only)
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        // Domain header interceptor
        builder.addInterceptor(DomainHeaderInterceptor(domainConfigs))

        // Default headers for all requests
        builder.addInterceptor { chain ->
            val original = chain.request()
            val newRequest = original.newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()
            chain.proceed(newRequest)
        }

        return builder.build()
    }

    // ═══════════════════════════════════════════
    // MOSHI
    // ═══════════════════════════════════════════
    @Volatile
    private var _sharedMoshi: Moshi? = null

    fun sharedMoshi(): Moshi {
        return _sharedMoshi ?: synchronized(this) {
            _sharedMoshi ?: Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .also { _sharedMoshi = it }
        }
    }

    // ═══════════════════════════════════════════
    // RETROFIT BUILDER FACTORY
    // ═══════════════════════════════════════════
    fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(sharedClient())
            .addConverterFactory(MoshiConverterFactory.create(sharedMoshi()))
            .build()
    }

    // ═══════════════════════════════════════════
    // DOMAIN HEADER INTERCEPTOR
    // ═══════════════════════════════════════════
    private class DomainHeaderInterceptor(
        private val configs: Map<String, DomainConfig>
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url
            val host = url.host()

            val config = configs[host] ?: return chain.proceed(request)

            val newRequest = request.newBuilder()
                .header("Referer", config.referer)
                .header("Origin", config.origin)
                .header("User-Agent", config.userAgent)
                .header("Accept", config.accept)
                .build()

            return chain.proceed(newRequest)
        }
    }

    // ═══════════════════════════════════════════
    // DATA CLASS
    // ═══════════════════════════════════════════
    private data class DomainConfig(
        val referer: String,
        val origin: String,
        val userAgent: String,
        val accept: String
    )

    // ═══════════════════════════════════════════
    // CLEANUP (for testing or app shutdown)
    // ═══════════════════════════════════════════
    fun shutdown() {
        _sharedClient?.connectionPool()?.evictAll()
        _sharedClient = null
        _sharedMoshi = null
    }
}

import android.os.BuildConfig