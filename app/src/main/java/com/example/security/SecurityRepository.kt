package com.example.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

data class PrivacyStatus(
    val isEncrypted: Boolean = true,
    val isBiometricAvailable: Boolean = true,
    val isScreenProtected: Boolean = true,
    val isRootDetected: Boolean = false,
    val isEmulator: Boolean = false
)

object SecurityRepository {

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
     * Copies text to clipboard and safely clears it after 30 seconds to prevent clipboard sniffing.
     */
    fun copyWithAutoClear(context: Context, label: String, text: String, scope: CoroutineScope) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard (clears in 30s)", Toast.LENGTH_SHORT).show()

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
