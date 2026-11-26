package com.jayra.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.buffer
import okio.source
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * UnlimitedAI API - Kotlin migration of the Node.js UnlimitedAIScraper.
 * 
 * Streams NDJSON response line-by-line for real-time token delivery.
 * Base URL: https://app.unlimitedai.chat
 */
class UnlimitedAiApi private constructor(
    private val client: OkHttpClient,
    private val moshi: Moshi,
    private val baseUrl: String = "https://app.unlimitedai.chat",
    private val apiPath: String = "/api/chat",
    private val locale: String = "id",
    private val deviceId: String = UUID.randomUUID().toString(),
    private val chatId: String = UUID.randomUUID().toString(),
    private var sessionCookie: String? = null,
    private val userAgent: String = "Mozilla/5.0 (Android 14; Mobile; rv:144.0) Gecko/144.0 Firefox/144.0",
    private val selectedModel: String = "chat-model-reasoning"
) {

    private val jsonAdapter = moshi.adapter(UnlimitedAiResponse::class.java)
    private val deltaAdapter = moshi.adapter(UnlimitedAiDelta::class.java)

    companion object {
        fun create(): UnlimitedAiApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS) // Longer for streaming
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return UnlimitedAiApi(client, moshi)
        }
    }

    // ════════════════════════════════════════════
    // PUBLIC API - STREAMING
    // ════════════════════════════════════════════

    /**
     * Sends a prompt and streams the response via callback.
     * Each token is delivered via onToken callback as it arrives.
     * onComplete is called when streaming finishes.
     * onError is called on any error.
     */
    fun sendPromptStream(
        prompt: String,
        onToken: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = buildRequestBody(prompt)
        val headers = buildHeaders()

        val request = Request.Builder()
            .url("$baseUrl$apiPath")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .headers(headers)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                onError(e.message ?: "Network error")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    response.close()
                    onError("HTTP ${response.code}: $errorBody")
                    return
                }

                // Extract cookies
                val setCookieHeaders = response.headers("Set-Cookie")
                if (setCookieHeaders.isNotEmpty()) {
                    sessionCookie = setCookieHeaders.map { it.split(";")[0] }.joinToString("; ")
                }

                val body = response.body
                if (body == null) {
                    onError("Empty response body")
                    return
                }

                // Stream NDJSON line by line
                val source = body.source()
                val bufferedSource = source.buffer()
                
                var fullText = ""
                
                try {
                    while (true) {
                        val line = bufferedSource.readUtf8LineOrNull()
                        if (line == null) break // End of stream
                        
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        
                        // Parse NDJSON line
                        try {
                            val delta = deltaAdapter.fromJson(trimmed)
                            if (delta != null && delta.type == "delta" && delta.delta != null) {
                                val token = delta.delta!!
                                fullText += token
                                onToken(token)
                            }
                        } catch (e: Exception) {
                            // Skip malformed lines
                        }
                    }
                    onComplete(fullText)
                } catch (e: Exception) {
                    onError(e.message ?: "Stream parsing error")
                } finally {
                    response.close()
                }
            }
        })
    }

    // ═══════════════════════════════════════════
    // SYNCHRONOUS VERSION (for testing or non-streaming use)
    // ════════════════════════════════════════════

    suspend fun sendPrompt(prompt: String): UnlimitedAiResult {
        val requestBody = buildRequestBody(prompt)
        val headers = buildHeaders()

        val request = Request.Builder()
            .url("$baseUrl$apiPath")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .headers(headers)
            .build()

        return withContext(okhttp3.Dispatchers.IO) {
            val call = client.newCall(request)
            val response = call.execute()
            
            try {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext UnlimitedAiResult(success = false, error = "HTTP ${response.code}: $errorBody")
                }

                val setCookieHeaders = response.headers("Set-Cookie")
                if (setCookieHeaders.isNotEmpty()) {
                    sessionCookie = setCookieHeaders.map { it.split(";")[0] }.joinToString("; ")
                }

                val body = response.body
                if (body == null) {
                    return@withContext UnlimitedAiResult(success = false, error = "Empty response body")
                }

                val source = body.source()
                val bufferedSource = source.buffer()
                
                var fullText = ""
                
                while (true) {
                    val line = bufferedSource.readUtf8LineOrNull()
                    if (line == null) break
                    
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    
                    try {
                        val delta = deltaAdapter.fromJson(trimmed)
                        if (delta != null && delta.type == "delta" && delta.delta != null) {
                            fullText += delta.delta!!
                        }
                    } catch (_: Exception) {
                        // Skip malformed lines
                    }
                }
                
                UnlimitedAiResult(success = true, text = fullText)
            } catch (e: Exception) {
                UnlimitedAiResult(success = false, error = e.message ?: "Request failed")
            } finally {
                response.close()
            }
        }
    }

    // ═══════════════════════════════════════════
    // BUILDERS
    // ════════════════════════════════════════════

    private fun buildHeaders(): okhttp3.Headers {
        val builder = okhttp3.Headers.Builder()
            .add("Content-Type", "application/json")
            .add("x-next-intl-locale", locale)
            .add("User-Agent", userAgent)
            .add("Referer", "https://app.unlimitedai.chat/$locale")
            .add("Accept", "application/json")
            .add("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
            .add("Accept-Encoding", "gzip, deflate, br")
            .add("Origin", "https://app.unlimitedai.chat")
            .add("Connection", "keep-alive")
            .add("Sec-Fetch-Dest", "empty")
            .add("Sec-Fetch-Mode", "cors")
            .add("Sec-Fetch-Site", "same-origin")
            .add("DNT", "1")

        sessionCookie?.let { builder.add("Cookie", it) }

        return builder.build()
    }

    private fun buildRequestBody(prompt: String): String {
        val timestamp = java.time.Instant.now().toString()
        val json = moshi.adapter(UnlimitedAiRequest::class.java).toJson(
            UnlimitedAiRequest(
                chatId = chatId,
                messages = listOf(
                    UnlimitedAiMessage(
                        id = UUID.randomUUID().toString(),
                        role = "user",
                        content = prompt,
                        parts = listOf(UnlimitedAiPart(type = "text", text = prompt)),
                        createdAt = timestamp
                    ),
                    UnlimitedAiMessage(
                        id = UUID.randomUUID().toString(),
                        role = "assistant",
                        content = "",
                        parts = listOf(UnlimitedAiPart(type = "text", text = "")),
                        createdAt = timestamp
                    )
                ),
                selectedChatModel = selectedModel,
                selectedCharacter = null,
                selectedStory = null,
                deviceId = deviceId,
                locale = locale
            )
        )
        return json
    }

    // ═══════════════════════════════════════════
    // DATA CLASSES
    // ════════════════════════════════════════════

    @JsonClass(generateAdapter = true)
    data class UnlimitedAiRequest(
        val chatId: String,
        val messages: List<UnlimitedAiMessage>,
        val selectedChatModel: String,
        val selectedCharacter: String?,
        val selectedStory: String?,
        val deviceId: String,
        val locale: String
    )

    @JsonClass(generateAdapter = true)
    data class UnlimitedAiMessage(
        val id: String,
        val role: String,
        val content: String,
        val parts: List<UnlimitedAiPart>,
        val createdAt: String
    )

    @JsonClass(generateAdapter = true)
    data class UnlimitedAiPart(
        val type: String,
        val text: String
    )

    @JsonClass(generateAdapter = true)
    data class UnlimitedAiResponse(
        val type: String?,
        val delta: String?
    )

    @JsonClass(generateAdapter = true)
    data class UnlimitedAiDelta(
        val type: String?,
        val delta: String?
    )

    data class UnlimitedAiResult(
        val success: Boolean,
        val text: String? = null,
        val error: String? = null
    )
}