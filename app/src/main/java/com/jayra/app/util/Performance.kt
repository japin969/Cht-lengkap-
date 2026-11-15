package com.jayra.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build

object Performance {
    fun isLowRam(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem < 4L * 1024 * 1024 * 1024
    }
    fun isLegacy(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    fun isFlagship(context: Context): Boolean =
        !isLowRam(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
