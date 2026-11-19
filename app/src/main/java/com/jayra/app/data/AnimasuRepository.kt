package com.jayra.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository tunggal untuk semua call Anime ke Animasu API.
 * Semua fungsi suspend, handle error, return Result wrapper.
 */
class AnimasuRepository(private val api: AnimasuApi = AnimasuApi.create()) {

    // --- SEARCH ---
    suspend fun search(query: String, page: Int = 1): Result<AnimasuSearchResponse> = withContext(Dispatchers.IO) {
        runCatching { api.search(query, page) }
    }

    // --- DETAIL ---
    suspend fun getDetail(detailUrl: String): Result<AnimasuDetailResponse> = withContext(Dispatchers.IO) {
        runCatching { api.detail(detailUrl) }
    }

    // --- STREAM / PLAY ---
    suspend fun getStream(streamUrl: String): Result<AnimasuStreamResponse> = withContext(Dispatchers.IO) {
        runCatching { api.stream(streamUrl) }
    }

    // --- ONGOING ---
    suspend fun getOngoing(page: Int = 1): Result<AnimasuListResponse> = withContext(Dispatchers.IO) {
        runCatching { api.ongoing(1, page) }
    }

    // --- COMPLETED ---
    suspend fun getCompleted(page: Int = 1): Result<AnimasuListResponse> = withContext(Dispatchers.IO) {
        runCatching { api.completed(1, page) }
    }

    // --- POPULAR ---
    suspend fun getPopular(page: Int = 1): Result<AnimasuListResponse> = withContext(Dispatchers.IO) {
        runCatching { api.popular(1, page) }
    }

    // --- SCHEDULE ---
    suspend fun getSchedule(): Result<AnimasuScheduleResponse> = withContext(Dispatchers.IO) {
        runCatching { api.schedule(1) }
    }
}