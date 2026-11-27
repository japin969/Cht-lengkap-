package com.jayra.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jayra.app.di.AppModule
import com.jayra.app.util.ErrorHandler
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit

class JayraApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        // Header ala browser agar gambar dari host ber-Cloudflare (anichin.moe,
        // donghive.vip) tidak ditolak 403. Tanpa ini poster tidak muncul.
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.sankavollerei.web.id/")
                    .build()
                chain.proceed(req)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttp)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("coil")).build() }
            .crossfade(true)
            .build()
    }

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
