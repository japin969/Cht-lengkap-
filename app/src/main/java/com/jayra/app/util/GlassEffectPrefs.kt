package com.jayra.app.util

import android.content.Context
import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Setting global efek glass iOS 26.
 * - Aktif & API 31+ → Modifier.blur
 * - Aktif & API < 31 → gradient semu (fallback)
 * - Nonaktif → solid
 */
object GlassEffectPrefs {
    private const val PREFS = "jayra_ui_prefs"
    private const val KEY = "glass_effect_enabled"

    // State reaktif global (dibaca Composable)
    val enabledState: MutableState<Boolean> = mutableStateOf(false)

    fun init(context: Context) {
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
}
