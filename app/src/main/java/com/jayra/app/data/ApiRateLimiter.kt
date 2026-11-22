package com.jayra.app.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Rate limiter sederhana & aman untuk API Drama (DramaBox via anichin).
 *
 * Aturan trial: 50 request / menit. Kita konservatif: minimal jeda
 * [minIntervalMs] (default 1200ms) antar request. Kalau kena 429, pemanggil
 * harus berhenti & fallback ke cache (TIDAK retry langsung berkali-kali).
 *
 * "Rem" ini tidak boleh dihapus / di-bypass — ini yang menjaga API tidak
 * memblokir kita (429) dan menjaga aplikasi tetap responsif.
 */
object ApiRateLimiter {

    /** Jeda minimal antar request (ms). 1.2s -> aman di bawah 50 req/min. */
    private const val minIntervalMs = 1200L

    private val mutex = Mutex()
    private var lastRequestAt = 0L

    /**
     * Jalankan [block] setelah memastikan jeda minimal sejak request
     * sebelumnya. Semua request Drama WAJIB lewat sini.
     */
    suspend fun <T> throttle(block: suspend () -> T): T = mutex.withLock {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestAt
        if (elapsed < minIntervalMs) {
            delay(minIntervalMs - elapsed)
        }
        lastRequestAt = System.currentTimeMillis()
        block()
    }

    /** Dipakai saat sudah kena 429: beri jeda lebih panjang sebelum lanjut. */
    suspend fun penalize() = mutex.withLock {
        lastRequestAt = System.currentTimeMillis() + 5000L
    }
}