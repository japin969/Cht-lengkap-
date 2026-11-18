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

    // Status perangkat dihitung sekali saat inisialisasi, bukan setiap render.
    private var deviceTier: GlassTier = GlassTier.MEDIUM

    // State reaktif global (dibaca Composable).
    val enabledState: MutableState<Boolean> = mutableStateOf(false)

    fun init(context: Context) {
        val appContext = context.applicationContext
        deviceTier = resolveDeviceTier(appContext)

        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabledState.value = prefs.getBoolean(KEY, true)
    }

    fun set(context: Context, enabled: Boolean) {
        deviceTier = resolveDeviceTier(context.applicationContext)
        enabledState.value = enabled
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, enabled)
            .apply()
    }

    /** Blur benar-benar didukung OS (API 31+). */
    fun supportsRealBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Mengembalikan tier yang sudah dihitung saat init atau saat pengaturan berubah.
     * Tidak meminta informasi RAM lagi pada setiap komposisi UI.
     */
    fun resolveTier(): GlassTier {
        return if (enabledState.value) deviceTier else GlassTier.OFF
    }

    private fun resolveDeviceTier(context: Context): GlassTier {
        // NO BLUR POLICY: seluruh device dipaksa ke fakir blur (faux glass).
        // Efek kaca dibuat dari gradient + border + transparansi saja, jadi
        // tidak ada RenderEffect / blur library yang membebani GPU di HP mid/low.
        @Suppress("UNUSED_PARAMETER", "UNUSED_EXPRESSION")
        context
        return GlassTier.LOW
    }
}

/**
 * Tier performa render glass. Lihat dokumentasi di atas.
 */
enum class GlassTier { HIGH, MEDIUM, LOW, OFF }
