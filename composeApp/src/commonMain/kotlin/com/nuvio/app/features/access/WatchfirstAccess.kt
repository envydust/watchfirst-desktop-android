package com.nuvio.app.features.access

import co.touchlab.kermit.Logger
import com.nuvio.app.core.network.createApiHttpClient
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.plugins.PluginRepository
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Same service and origin the WATCHFIRST Windows app uses. */
internal const val WATCHFIRST_ORIGIN = "https://membros.watchfirst.com.br"
internal const val WATCHFIRST_BASIC_SITE = "https://membros.watchfirst.com.br"
internal const val WATCHFIRST_MAX_SITE = "https://max.watchfirst.com.br"

private const val LIFETIME = 253402300799000L
private val GATEWAY_URL = Regex("^https://[a-z0-9-]+\\.watchfirst\\.com\\.br/gateway/")

data class WatchfirstMember(
    val id: String,
    val name: String,
    val plan: String,
    val expiresAt: Long,
    val lifetime: Boolean,
)

data class WatchfirstAddon(
    val id: String,
    val name: String,
    val kind: String,
    val content: String,
    val url: String,
)

data class WatchfirstAccessState(
    /** A session token exists on this device. Content is reachable only through it. */
    val granted: Boolean,
    val member: WatchfirstMember? = null,
    val addons: List<WatchfirstAddon> = emptyList(),
    /** Shown on the login screen after the service ends the session (blocked, expired, removed). */
    val notice: String? = null,
)

class WatchfirstAccessException(message: String) : Exception(message)

/**
 * WATCHFIRST access for the mobile app. Mirrors the Windows app: the person types name and e-mail,
 * then the single-use code generated on the member site (or an access code from the owner). The
 * service returns a session token; the member's addons arrive as gateway URLs bound to that token.
 */
object WatchfirstAccess {
    private val log = Logger.withTag("WatchfirstAccess")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client by lazy { createApiHttpClient() }
    private val syncLock = Mutex()
    private var verifyJob: Job? = null

    private val _state = MutableStateFlow(
        WatchfirstAccessState(granted = WatchfirstAccessStorage.loadToken() != null),
    )
    val state: StateFlow<WatchfirstAccessState> = _state.asStateFlow()

    val savedName: String get() = WatchfirstAccessStorage.loadName().orEmpty()
    val savedEmail: String get() = WatchfirstAccessStorage.loadEmail().orEmpty()

    /** Starts the periodic check of the stored session. Safe to call more than once. */
    fun start() {
        if (verifyJob?.isActive == true) return
        verifyJob = scope.launch {
            while (true) {
                if (WatchfirstAccessStorage.loadToken() != null) verifySilently()
                delay(5 * 60_000L)
            }
        }
    }

    /** Connects with the code from the member site (WF-XXXX-XXXX) or an owner-issued access code. */
    suspend fun connect(name: String, email: String, rawCode: String) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()
        val code = rawCode.trim().uppercase()
        WatchfirstAccessStorage.saveIdentity(cleanName, cleanEmail)
        val accessCode = Regex("^WF-?([A-F0-9]{48})$").matchEntire(code)
        if (accessCode != null) {
            val data = call(
                method = HttpMethod.Post,
                path = "/api/session",
                body = buildJsonObject {
                    put("code", "WF-" + accessCode.groupValues[1])
                    put("deviceId", deviceId())
                    put("deviceName", "WATCHFIRST Android")
                },
            )
            val token = data.string("token")
            if (!token.matches(Regex("[a-f0-9]{48}"))) throw WatchfirstAccessException("O serviço de acesso não respondeu. Tente novamente.")
            WatchfirstAccessStorage.saveToken(token)
            apply(data)
            return
        }
        val data = call(
            method = HttpMethod.Post,
            path = "/api/approval/link",
            body = buildJsonObject {
                put("code", code)
                put("name", cleanName)
                put("email", cleanEmail)
                put("deviceId", deviceId())
            },
        )
        val token = data.string("token")
        if (!token.matches(Regex("[a-f0-9]{48}"))) throw WatchfirstAccessException("O serviço de acesso não respondeu. Tente novamente.")
        WatchfirstAccessStorage.saveToken(token)
        apply(call(HttpMethod.Get, "/api/session", token = token))
    }

    /** Ends the session on the service and on this device. */
    suspend fun signOut() {
        val token = WatchfirstAccessStorage.loadToken()
        if (token != null) {
            runCatching { call(HttpMethod.Delete, "/api/session", token = token) }
        }
        lockLocally(notice = null)
    }

    /**
     * Brings the active profile's addons and plugins in line with the member's plan: installs the
     * gateway URLs that are missing and removes WATCHFIRST gateway URLs that are no longer granted.
     * Addons the person added on their own are never touched.
     */
    suspend fun syncContent() = syncLock.withLock<Unit> {
        val desired = if (_state.value.granted) _state.value.addons else emptyList()
        runCatching {
            AddonRepository.initialize()
            val wantedAddons = desired.filter { it.kind == "stremio" }.map { it.url }
            val installedAddons = AddonRepository.uiState.value.addons.map { it.manifestUrl }
            installedAddons.filter { GATEWAY_URL.containsMatchIn(it) && it !in wantedAddons }
                .forEach(AddonRepository::removeAddon)
            wantedAddons.filter { it !in installedAddons }.forEach { url ->
                AddonRepository.addAddon(url)
            }
        }.onFailure { if (it is CancellationException) throw it; log.w(it) { "Addon sync failed" } }
        runCatching {
            PluginRepository.initialize()
            val wantedPlugins = desired.filter { it.kind == "plugin" }.map { it.url }
            val installedPlugins = PluginRepository.uiState.value.repositories.map { it.manifestUrl }
            installedPlugins.filter { GATEWAY_URL.containsMatchIn(it) && it !in wantedPlugins }
                .forEach(PluginRepository::removeRepository)
            wantedPlugins.filter { it !in installedPlugins }.forEach { url ->
                PluginRepository.addRepository(url)
            }
        }.onFailure { if (it is CancellationException) throw it; log.w(it) { "Plugin sync failed" } }
    }

    private suspend fun verifySilently() {
        val token = WatchfirstAccessStorage.loadToken() ?: return
        try {
            apply(call(HttpMethod.Get, "/api/session", token = token))
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: HttpFailure) {
            // Only the service saying the session is over locks the app; network trouble keeps access.
            if (failure.status == 401 || failure.status == 403) lockLocally(notice = failure.message)
        } catch (failure: Exception) {
            log.w(failure) { "Access check failed; keeping the current session" }
        }
    }

    private fun apply(data: JsonObject) {
        val m = data["member"]?.jsonObject ?: throw WatchfirstAccessException("O serviço de acesso não respondeu. Tente novamente.")
        val expiresAt = m["expiresAt"]?.jsonPrimitive?.longOrNull ?: 0L
        val member = WatchfirstMember(
            id = m.string("id"),
            name = m.string("name"),
            plan = m.string("plan"),
            expiresAt = expiresAt,
            lifetime = m["lifetime"]?.jsonPrimitive?.booleanOrNull == true || expiresAt == LIFETIME,
        )
        val addons = data["addons"]?.jsonArray.orEmpty().map { element ->
            val a = element.jsonObject
            WatchfirstAddon(a.string("id"), a.string("name"), a.string("kind"), a.string("content"), a.string("url"))
        }.filter { it.url.startsWith("https://") && (it.content != "live" || member.plan == "max") }
        _state.value = WatchfirstAccessState(granted = true, member = member, addons = addons)
    }

    private fun lockLocally(notice: String?) {
        WatchfirstAccessStorage.saveToken(null)
        _state.update { WatchfirstAccessState(granted = false, notice = notice) }
        scope.launch { syncContent() }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun deviceId(): String = WatchfirstAccessStorage.loadDeviceId()
        ?: ("android-" + Uuid.random().toString()).also(WatchfirstAccessStorage::saveDeviceId)

    private class HttpFailure(val status: Int, message: String) : Exception(message)

    private suspend fun call(
        method: HttpMethod,
        path: String,
        body: JsonObject? = null,
        token: String? = null,
    ): JsonObject {
        val response = try {
            client.request(WATCHFIRST_ORIGIN + path) {
                this.method = method
                header(HttpHeaders.Accept, "application/json")
                if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
                if (body != null) setBody(TextContent(body.toString(), ContentType.Application.Json))
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            throw WatchfirstAccessException("Sem conexão com o WATCHFIRST. Verifique a internet e tente de novo.")
        }
        val text = response.bodyAsText()
        val data = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
        val status = response.status.value
        if (status !in 200..299) {
            val message = data?.string("error")?.ifBlank { null }
                ?: "Não foi possível validar o acesso (erro $status). Tente novamente."
            throw HttpFailure(status, message)
        }
        return data ?: throw WatchfirstAccessException("O serviço de acesso não respondeu. Tente novamente.")
    }

    private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.content.orEmpty()
}
