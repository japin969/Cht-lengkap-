package com.jayra.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Setting global efek glass iOS 26 (JAYRA-UI v2.1).
 *
 * Tier render:
 *   HIGH   — API ≥31 & RAM ≥6GB       → liquidGlass (blur asli)
 *   MEDIUM — API ≥31 & RAM 4–6GB      → liquidGlass + tint lebih tebal
 *   LOW    — API <31 / low-RAM / <4GB → faux glass (gradient + border, zero blur)
 *   OFF    — user toggle off           → solid opaque
 */
object GlassEffectPrefs {
    private const val PREFS = "jayra_ui_prefs"
    private const val KEY = "glass_effect_enabled"

    private var appContext: Context? = null

    // State reaktif global (dibaca Composable)
    val enabledState: MutableState<Boolean> = mutableStateOf(false)

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabledState.value = prefs.getBoolean(KEY, true)  // default aktif
    }

    fun set(context: Context, enabled: Boolean) {
        enabledState.value = enabled
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, enabled).apply()
    }

    /** Blur benar-benar didukung OS (API 31+). */
    fun supportsRealBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Resolve render tier berdasarkan: user toggle, API level, RAM, low-RAM flag.
     * Non-composable; aman dipanggil dari remember() di Composable.
     */
    fun resolveTier(): GlassTier {
        if (!enabledState.value) return GlassTier.OFF
        if (!supportsRealBlur()) return GlassTier.LOW

        val ctx = appContext ?: return GlassTier.MEDIUM
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return GlassTier.MEDIUM

        if (am.isLowRamDevice) return GlassTier.LOW

        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val ramGB = mi.totalMem / (1024.0 * 1024.0 * 1024.0)

        return when {
            ramGB >= 6.0 -> GlassTier.HIGH
            ramGB >= 4.0 -> GlassTier.MEDIUM
            else -> GlassTier.LOW
        }
    }
}

/**
 * Tier performa render glass. Lihat dokumentasi di atas.
 */
enum class GlassTier { HIGH, MEDIUM, LOW, OFF }
