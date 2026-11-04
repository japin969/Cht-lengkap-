package com.jayra.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.jayra.app.di.AppModule
import com.jayra.app.util.ErrorHandler
import timber.log.Timber

class JayraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

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

        // Daftarkan Global UncaughtExceptionHandler pada runtime perangkat nyata
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
