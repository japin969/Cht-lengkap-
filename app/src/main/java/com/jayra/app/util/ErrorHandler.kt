package com.jayra.app.util

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Toast
import com.jayra.app.JayraApp
import com.jayra.app.MainActivity
import timber.log.Timber
import kotlin.system.exitProcess

class ErrorHandler @JvmOverloads constructor(
    private val appContext: Context? = JayraApp.appContextOrNull(),
    private val previousHandler: Thread.UncaughtExceptionHandler? = null
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 1. Log error menggunakan Timber
        Timber.e(throwable, "Uncaught Exception")

        val context = appContext ?: JayraApp.appContextOrNull()
        if (context == null || isCrashLoopDetected(context)) {
            previousHandler?.uncaughtException(thread, throwable)
            return
        }

        // 2. Tampilkan Toast pada main/prepared Looper
        showCrashToastSafely(context)

        // Beri waktu singkat agar Toast sempat dirender oleh WindowManager sebelum proses dihentikan
        try {
            Thread.sleep(450L)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        // 3. Restart MainActivity menggunakan Intent baru & bersihkan task stack
        try {
            val restartIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(restartIntent)
        } catch (restartError: Exception) {
            Timber.e(restartError, "Gagal memulai ulang MainActivity")
        }

        // 4. Hentikan proses aplikasi
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    private fun showCrashToastSafely(context: Context) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(context, CRASH_TOAST_MESSAGE, Toast.LENGTH_LONG).show()
            } else {
                val mainLooper = Looper.getMainLooper()
                if (mainLooper != null) {
                    Handler(mainLooper).post {
                        runCatching {
                            Toast.makeText(context, CRASH_TOAST_MESSAGE, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Thread {
                        runCatching {
                            Looper.prepare()
                            Toast.makeText(context, CRASH_TOAST_MESSAGE, Toast.LENGTH_LONG).show()
                            Looper.loop()
                        }
                    }.start()
                }
            }
        } catch (_: Exception) {
            // Abaikan bila sistem UI tidak lagi menerima Toast
        }
    }

    private fun isCrashLoopDetected(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastCrashAt = prefs.getLong(KEY_LAST_CRASH_TIMESTAMP, 0L)
            prefs.edit().putLong(KEY_LAST_CRASH_TIMESTAMP, now).commit()
            (now - lastCrashAt) in 1L..CRASH_LOOP_COOLDOWN_MS
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val CRASH_TOAST_MESSAGE =
            "Terjadi kesalahan tak terduga. Aplikasi akan dimulai ulang."
        private const val PREFS_NAME = "jayra_crash_guard_prefs"
        private const val KEY_LAST_CRASH_TIMESTAMP = "last_crash_timestamp"
        private const val CRASH_LOOP_COOLDOWN_MS = 4_000L
    }
}
