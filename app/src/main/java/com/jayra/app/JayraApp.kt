package com.jayra.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.jayra.app.di.AppModule
import com.jayra.app.util.ErrorHandler
import timber.log.Timber

class JayraApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Pasang handler sedini mungkin, sebelum ContentProvider Firebase jalan
        if (!isRunningUnitTest()) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                ErrorHandler(appContext = base.applicationContext, previousHandler = previous)
            )
        }
        ErrorHandler.mark("attachBaseContext.done")
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ErrorHandler.mark("app.onCreate.start")

        // Inisialisasi Crashlytics (opsional, tidak crash kalau Firebase belum siap)
        runCatching {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                .setCrashlyticsCollectionEnabled(true)
        }

        // Inisialisasi Timber terlebih dahulu
        initTimber()

        // Inisialisasi modul dependensi aplikasi (Firestore named DB, dsb.)
        runCatching {
            AppModule.init(applicationContext)
        }.onFailure { error ->
            Timber.w(error, "AppModule.init gagal pada startup awal")
        }

        ErrorHandler.mark("app.onCreate.done")
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
