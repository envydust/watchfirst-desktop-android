package com.nuvio.app.core.performance

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences

internal actual object PerformanceModePlatform {
    private const val PREFS_NAME = "watchfirst_performance"
    private const val KEY_CHOICE = "performance_mode"
    private const val WEAK_RAM_BYTES = 3L * 1024 * 1024 * 1024

    private var preferences: SharedPreferences? = null
    private var reason: String = ""
    private var weak: Boolean = false

    /** Called from MainActivity, like the other storages. */
    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memory = ActivityManager.MemoryInfo().also { info -> manager?.getMemoryInfo(info) }
        val cores = Runtime.getRuntime().availableProcessors()
        reason = when {
            manager?.isLowRamDevice == true -> "aparelho de pouca memória"
            memory.totalMem in 1 until WEAK_RAM_BYTES -> "menos de 3 GB de memória"
            cores <= 4 -> "$cores núcleos de processamento"
            else -> ""
        }
        weak = reason.isNotEmpty()
    }

    actual val weakDeviceDetected: Boolean get() = weak

    actual val weakDeviceReason: String get() = reason

    actual fun loadChoice(): String? = preferences?.getString(KEY_CHOICE, null)

    actual fun saveChoice(value: String) {
        preferences?.edit()?.putString(KEY_CHOICE, value)?.apply()
    }
}
