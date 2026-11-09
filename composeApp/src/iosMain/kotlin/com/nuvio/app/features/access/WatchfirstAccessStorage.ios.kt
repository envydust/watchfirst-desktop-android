package com.nuvio.app.features.access

import platform.Foundation.NSUserDefaults

internal actual object WatchfirstAccessStorage {
    private const val KEY_TOKEN = "watchfirst_access_session_token"
    private const val KEY_DEVICE = "watchfirst_access_device_id"
    private const val KEY_NAME = "watchfirst_access_name"
    private const val KEY_EMAIL = "watchfirst_access_email"

    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun loadToken(): String? = defaults.stringForKey(KEY_TOKEN)?.takeIf { it.isNotBlank() }

    actual fun saveToken(token: String?) {
        if (token.isNullOrBlank()) defaults.removeObjectForKey(KEY_TOKEN) else defaults.setObject(token, forKey = KEY_TOKEN)
    }

    actual fun loadDeviceId(): String? = defaults.stringForKey(KEY_DEVICE)

    actual fun saveDeviceId(deviceId: String) {
        defaults.setObject(deviceId, forKey = KEY_DEVICE)
    }

    actual fun loadName(): String? = defaults.stringForKey(KEY_NAME)

    actual fun loadEmail(): String? = defaults.stringForKey(KEY_EMAIL)

    actual fun saveIdentity(name: String, email: String) {
        defaults.setObject(name, forKey = KEY_NAME)
        defaults.setObject(email, forKey = KEY_EMAIL)
    }
}
