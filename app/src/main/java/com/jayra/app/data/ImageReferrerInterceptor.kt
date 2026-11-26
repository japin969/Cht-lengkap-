package com.jayra.app.data

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Interceptor to add referrer header for image requests to prevent hotlink blocking
 */
class ImageReferrerInterceptor(private val referrer: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.toString()

        // Add referrer for image requests
        val isImageRequest = url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                            url.endsWith(".png") || url.endsWith(".webp") ||
                            url.contains("/image/") || url.contains("/cover/") ||
                            url.contains("/poster/") || url.contains("/thumb/")

        val newRequest = if (isImageRequest) {
            original.newBuilder()
                .header("Referer", referrer)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                .build()
        } else {
            original
        }
        return chain.proceed(newRequest)
    }
}