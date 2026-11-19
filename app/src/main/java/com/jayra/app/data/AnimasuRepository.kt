package com.jayra.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository tunggal untuk semua call Anime ke Animasu API.
 * Semua fungsi suspend, handle error, return Result wrapper.
 */
class AnimasuRepository(private val api: AnimasuApi = AnimasuApi.create()) {

    private suspend fun <T : Any> safeApiCall(block: () -> Response<T>): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = block()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SEARCH ---
    suspend fun search(query: String, page: Int = 1): Result<AnimasuSearchResponse> = safeApiCall { api.search(query, page) }

    // --- DETAIL ---
    suspend fun getDetail(detailUrl: String): Result<AnimasuDetailResponse> = safeApiCall { api.detail(detailUrl) }

    // --- STREAM / PLAY ---
    suspend fun getStream(streamUrl: String): Result<AnimasuStreamResponse> = safeApiCall { api.stream(streamUrl) }

    // --- ONGOING ---
    suspend fun getOngoing(page: Int = 1): Result<AnimasuListResponse> = safeApiCall { api.ongoing(1, page) }

    // --- COMPLETED ---
    suspend fun getCompleted(page: Int = 1): Result<AnimasuListResponse> = safeApiCall { api.completed(1, page) }

    // --- POPULAR ---
    suspend fun getPopular(page: Int = 1): Result<AnimasuListResponse> = safeApiCall { api.popular(1, page) }

    // --- SCHEDULE ---
    suspend fun getSchedule(): Result<AnimasuScheduleResponse> = safeApiCall { api.schedule(1) }
}