package com.nuvio.app.core.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the user picked in Settings. [Auto] follows the hardware check. */
enum class PerformanceModeChoice(val storageValue: String) {
    Auto("auto"),
    Light("light"),
    Full("full");

    companion object {
        fun fromStorage(value: String?): PerformanceModeChoice =
            entries.firstOrNull { it.storageValue == value } ?: Auto
    }
}

internal expect object PerformanceModePlatform {
    /** True when the device looks weak (little RAM, few cores, integrated-only graphics...). */
    val weakDeviceDetected: Boolean

    /** Short human-readable reason shown in Settings, empty when the device is not weak. */
    val weakDeviceReason: String

    fun loadChoice(): String?
    fun saveChoice(value: String)
}

/**
 * Light mode turns off effects that cost a lot on weak hardware (blur, hero auto-advance, hover
 * previews, crossfades, animated artwork) while keeping every feature available.
 */
object PerformanceMode {
    private val _choice = MutableStateFlow(PerformanceModeChoice.Auto)
    val choice: StateFlow<PerformanceModeChoice> = _choice.asStateFlow()

    private val _light = MutableStateFlow(false)
    val light: StateFlow<Boolean> = _light.asStateFlow()

    private var loaded = false
    private var softwareRendering = false

    /** Current value for code that runs outside composition (image loader, players...). */
    val isLight: Boolean
        get() {
            ensureLoaded()
            return _light.value
        }

    val autoDetectedWeak: Boolean
        get() = softwareRendering || runCatching { PerformanceModePlatform.weakDeviceDetected }.getOrDefault(false)

    val autoDetectedReason: String
        get() = if (softwareRendering) {
            "sem aceleração gráfica"
        } else {
            runCatching { PerformanceModePlatform.weakDeviceReason }.getOrDefault("")
        }

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        _choice.value = PerformanceModeChoice.fromStorage(runCatching { PerformanceModePlatform.loadChoice() }.getOrNull())
        recompute()
    }

    fun setChoice(choice: PerformanceModeChoice) {
        ensureLoaded()
        if (_choice.value == choice) return
        _choice.value = choice
        runCatching { PerformanceModePlatform.saveChoice(choice.storageValue) }
        recompute()
    }

    /** Desktop: the window ended up on a CPU (software) renderer, so every effect is paid by the CPU. */
    fun reportSoftwareRendering() {
        ensureLoaded()
        if (softwareRendering) return
        softwareRendering = true
        recompute()
    }

    private fun recompute() {
        _light.value = when (_choice.value) {
            PerformanceModeChoice.Light -> true
            PerformanceModeChoice.Full -> false
            PerformanceModeChoice.Auto -> autoDetectedWeak
        }
    }
}

/** True while light mode is on. Read it in composables instead of collecting the flow again. */
val LocalLightMode: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

@Composable
internal fun rememberLightModeState(): Boolean {
    val flow = remember {
        PerformanceMode.ensureLoaded()
        PerformanceMode.light
    }
    val light by flow.collectAsStateWithLifecycle()
    return light
}
