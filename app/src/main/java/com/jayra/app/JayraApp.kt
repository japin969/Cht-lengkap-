package com.jayra.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jayra.app.di.AppModule
import com.jayra.app.util.ErrorHandler
import timber.log.Timber

class JayraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        com.jayra.app.util.GlassEffectPrefs.init(this)
        instance = this

        initTimber()

        // Eksplisit inisialisasi Firebase sebelum modul lain mengaksesnya
        try {
            FirebaseApp.initializeApp(this)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        } catch (e: Exception) {
            Timber.e(e, "FirebaseApp.initializeApp gagal")
        }

        runCatching {
            AppModule.init(applicationContext)
        }.onFailure { error ->
            Timber.w(error, "AppModule.init gagal pada startup awal")
        }

        if (!isRunningUnitTest()) {
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                ErrorHandler(
                    appContext = applicationContext,
                    previousHandler = previousHandler
                )
            )
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Lepas cache gambar saat app masuk background (mencegah OOM)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            // Cache akan dibersihkan otomatis oleh Coil
        }
    }

    private fun initTimber() {
        if (Timber.forest().isNotEmpty()) return
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseCrashLogTree())
        }
    }

    private fun isRunningUnitTest(): Boolean {
        return "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)
    }

    private class ReleaseCrashLogTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= Log.ERROR) {
                Log.e(tag ?: "JayraRelease", message, t)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: JayraApp? = null

        fun appContextOrNull(): Context? = instance?.applicationContext
    }
}
