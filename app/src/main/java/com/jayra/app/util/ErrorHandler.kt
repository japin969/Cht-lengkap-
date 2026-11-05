package com.jayra.app.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.SystemClock
import android.provider.MediaStore
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
 * Handler yang dipasang di JayraApp.attachBaseContext().
 * TIDAK restart aplikasi (menghindari crash loop).
 * TIDAK startActivity dari background (Android 10+ batasi).
 *
 * Alur:
 * 1. Susun laporan (stack trace + info perangkat + fase startup).
 * 2. Simpan ke filesDir (internal) DAN Downloads/ (MediaStore, bisa dibaca tanpa root).
 * 3. Serahkan ke previousHandler (Android tampilkan crash dialog).
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

        // Simpan ke filesDir internal
        runCatching { File(context.filesDir, REPORT_FILE).writeText(report) }

        // Simpan ke getExternalFilesDir (bisa dibaca file manager di Android 7-9)
        runCatching {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                File(extDir, REPORT_FILE).writeText(report)
            }
        }

        // Simpan ke Downloads/ via MediaStore (Android 10+)
        runCatching { saveToDownloads(context, report) }

        // Kembalikan ke handler default Android (tampilkan dialog crash, jangan restart)
        fallback(thread, throwable)
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

    private fun saveToDownloads(context: Context, report: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val fileName = "jayra_crash_${System.currentTimeMillis()}.txt"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(report.toByteArray())
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
        return "FirebaseApp terdaftar=$appCount, google_app_id=$hasGoogleAppId, firestore_database_id=$hasDbId"
    }

    companion object {
        const val CRASH_TOAST_MESSAGE = "Terjadi kesalahan tak terduga."
        const val REPORT_FILE = "last_crash.txt"

        private val handling = AtomicBoolean(false)
        private val phases: MutableList<String> = Collections.synchronizedList(ArrayList())

        @JvmStatic
        fun mark(phase: String) {
            val t = runCatching { SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime() }
                .getOrDefault(0L)
            phases.add("${t}ms:$phase")
        }
    }
}
