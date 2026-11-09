package com.nuvio.app.features.access

/** Device-local WATCHFIRST access data: the service session token and what the person typed at login. */
internal expect object WatchfirstAccessStorage {
    fun loadToken(): String?
    fun saveToken(token: String?)
    fun loadDeviceId(): String?
    fun saveDeviceId(deviceId: String)
    fun loadName(): String?
    fun loadEmail(): String?
    fun saveIdentity(name: String, email: String)
    /** Small extra values: plan, live addon list, favorite channels. */
    fun loadValue(key: String): String?
    fun saveValue(key: String, value: String?)
}
