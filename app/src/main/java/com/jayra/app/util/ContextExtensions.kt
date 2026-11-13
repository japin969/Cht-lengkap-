package com.jayra.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Unwrap Activity dari Context (termasuk ContextWrapper berlapis).
 * Returns null jika Context bukan Activity-based (misal ApplicationContext).
 * Reference: protokol anti-memory-leak, Activity transient, tidak disimpan.
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
