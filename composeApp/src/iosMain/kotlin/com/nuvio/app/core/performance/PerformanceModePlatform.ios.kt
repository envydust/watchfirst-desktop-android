package com.nuvio.app.core.performance

import platform.Foundation.NSUserDefaults

internal actual object PerformanceModePlatform {
    private const val KEY_CHOICE = "watchfirst_performance_mode"

    actual val weakDeviceDetected: Boolean get() = false

    actual val weakDeviceReason: String get() = ""

    actual fun loadChoice(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY_CHOICE)

    actual fun saveChoice(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = KEY_CHOICE)
    }
}
