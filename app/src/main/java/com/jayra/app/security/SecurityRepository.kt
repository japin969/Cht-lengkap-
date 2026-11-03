package com.jayra.app.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.jayra.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.CertificatePinner
import java.io.File

data class PrivacyStatus(
    val isEncrypted: Boolean = true,
    val isBiometricAvailable: Boolean = true,
    val isScreenProtected: Boolean = true,
    val isRootDetected: Boolean = false,
    val isEmulator: Boolean = false
)

object SecurityRepository {

    private const val PREFS_RATE_LIMIT = "jayra_rate_limit_prefs"
    private const val KEY_FAILED_ATTEMPTS = "passcode_failed_attempts"
    private const val KEY_LOCK_TIMESTAMP = "passcode_lock_timestamp"
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCK_DURATION_MS = 30_000L // 30 seconds

    /**
     * Checks if the device has signs of root binaries (su, magisk).
     */
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/sbin/.magisk",
            "/data/adb/magisk"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {
                // Ignore permission error
            }
        }
        return false
    }

    /**
     * Checks if running inside an emulator or virtual container.
     */
    fun isRunningOnEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    /**
     * Checks if environment is compromised.
     */
    fun isSecurityCompromised(): Boolean {
        return isDeviceRooted()
    }

    /**
     * Rate Limiting (Anti-Brute Force):
     * Returns remaining lock seconds. 0 if not locked.
     */
    fun getRemainingLockSeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_RATE_LIMIT, Context.MODE_PRIVATE)
        val lockTimestamp = prefs.getLong(KEY_LOCK_TIMESTAMP, 0L)
        if (lockTimestamp <= 0L) return 0

        val now = System.currentTimeMillis()
        val elapsed = now - lockTimestamp
        return if (elapsed < LOCK_DURATION_MS) {
            ((LOCK_DURATION_MS - elapsed) / 1000).toInt().coerceAtLeast(1)
        } else {
            // Expired lock: clear
            prefs.edit().remove(KEY_LOCK_TIMESTAMP).putInt(KEY_FAILED_ATTEMPTS, 0).apply()
            0
        }
    }

    /**
     * Records a failed passcode attempt.
     * Returns Pair(currentFailedAttempts, isNowLocked).
     */
    fun recordFailedAttempt(context: Context): Pair<Int, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_RATE_LIMIT, Context.MODE_PRIVATE)
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit()
        editor.putInt(KEY_FAILED_ATTEMPTS, attempts)

        val isLocked = attempts >= MAX_FAILED_ATTEMPTS
        if (isLocked) {
            editor.putLong(KEY_LOCK_TIMESTAMP, System.currentTimeMillis())
        }
        editor.apply()
        return Pair(attempts, isLocked)
    }

    /**
     * Resets failed attempts after successful entry.
     */
    fun resetFailedAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_RATE_LIMIT, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCK_TIMESTAMP)
            .apply()
    }

    /**
     * Military-Grade Certificate Pinning for Firebase & API Network calls.
     */
    fun getCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("*.googleapis.com", "sha256/7HIvactydQBSuUmamQx53WZXM4045OmYvcLw263ENt8=")
            .add("*.googleapis.com", "sha256/hxqRlPTuQrgMVgahEAL2TinybQYT48EFnWRU2wGhN+4=")
            .add("firestore.googleapis.com", "sha256/7HIvactydQBSuUmamQx53WZXM4045OmYvcLw263ENt8=")
            .add("*.firebaseio.com", "sha256/7HIvactydQBSuUmamQx53WZXM4045OmYvcLw263ENt8=")
            .build()
    }

    /**
     * Release-safe logging that does nothing in release builds.
     */
    fun safeLog(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    /**
     * Copies text to clipboard and safely clears it after 30 seconds to prevent clipboard sniffing.
     */
    fun copyWithAutoClear(context: Context, label: String, text: String, scope: CoroutineScope) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Disalin ke clipboard (dibersihkan dalam 30 detik)", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.Main) {
            delay(30_000L)
            try {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    fun getPrivacyStatus(): PrivacyStatus {
        return PrivacyStatus(
            isEncrypted = true,
            isBiometricAvailable = true,
            isScreenProtected = true,
            isRootDetected = isDeviceRooted(),
            isEmulator = isRunningOnEmulator()
        )
    }
}
