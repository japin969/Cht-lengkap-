package com.jayra.app.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.google.firebase.FirebaseApp
import com.jayra.app.BuildConfig
import com.jayra.app.JayraApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * UncaughtExceptionHandler yang dipasang di JayraApp.attachBaseContext().
 *
 * Alur saat crash:
 * 1. Susun laporan (stack trace + info perangkat + fase startup + status Firebase).
 * 2. Simpan ke filesDir internal (bukan Android/data, jadi aman di Android 11+).
 * 3. Buka CrashReportActivity (proses ":crash") yang menampilkan laporan di layar.
 * 4. Matikan proses utama.
 *
 * Jika Crashlytics sudah aktif, ia membungkus handler ini: laporan fatal ditulis
 * Crashlytics dulu, baru diteruskan ke sini.
 */
class ErrorHandler @JvmOverloads constructor(
    private val appContext: Context? = JayraApp.appContextOrNull(),
    private val previousHandler: Thread.UncaughtExceptionHandler? = null
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val context = appContext ?: JayraApp.appContextOrNull()
        if (context == null || !handling.compareAndSet(false, true)) {
            fallback(thread, throwable)
            return
        }

        val report = runCatching { buildReport(context, thread, throwable) }
            .getOrElse { "Gagal menyusun laporan: $it\n${throwable.stackTraceToString()}" }

        runCatching { File(context.filesDir, REPORT_FILE).writeText(report) }

        val shown = runCatching {
            val intent = Intent(context, CrashReportActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CrashReportActivity.EXTRA_REPORT, report.take(MAX_EXTRA_CHARS))
            }
            context.startActivity(intent)
        }.isSuccess

        if (!shown) {
            handling.set(false)
            fallback(thread, throwable)
            return
        }

        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    private fun fallback(thread: Thread, throwable: Throwable) {
        val previous = previousHandler
        if (previous != null) {
            previous.uncaughtException(thread, throwable)
        } else {
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val uptime = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
        val phaseText = synchronized(phases) { phases.joinToString(" > ") }

        return buildString {
            appendLine("JAYRA CRASH REPORT")
            appendLine("Waktu     : $time")
            appendLine("Versi app : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Thread    : ${thread.name}")
            appendLine("Umur proses: $uptime ms")
            appendLine("Fase      : ${phaseText.ifEmpty { "(belum ada)" }}")
            appendLine(
                "Perangkat : ${Build.MANUFACTURER} ${Build.MODEL}, " +
                    "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
            )
            appendLine("ABI       : ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Firebase  : ${firebaseStatus(context)}")
            appendLine()
            appendLine(throwable.stackTraceToString())
        }
    }

    private fun firebaseStatus(context: Context): String {
        val appCount = runCatching { FirebaseApp.getApps(context).size.toString() }
            .getOrElse { "error(${it.javaClass.simpleName})" }
        val hasGoogleAppId = runCatching {
            context.resources.getIdentifier("google_app_id", "string", context.packageName) != 0
        }.getOrDefault(false)
        val hasDbId = runCatching {
            context.resources.getIdentifier("firestore_database_id", "string", context.packageName) != 0
        }.getOrDefault(false)
        return "FirebaseApp terdaftar=$appCount, resource google_app_id ada=$hasGoogleAppId, " +
            "firestore_database_id ada=$hasDbId"
    }

    companion object {
        const val CRASH_TOAST_MESSAGE = "Terjadi kesalahan tak terduga. Aplikasi akan dimulai ulang."
        const val REPORT_FILE = "last_crash.txt"
        private const val MAX_EXTRA_CHARS = 60_000

        private val handling = AtomicBoolean(false)
        private val phases: MutableList<String> = Collections.synchronizedList(ArrayList())

        /** Penanda fase startup; ikut dicetak di laporan crash untuk melihat sampai mana app berjalan. */
        @JvmStatic
        fun mark(phase: String) {
            val t = runCatching { SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime() }
                .getOrDefault(0L)
            phases.add("${t}ms:$phase")
        }
    }
}
