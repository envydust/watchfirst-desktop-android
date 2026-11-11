package com.nuvio.app.features.player

import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.features.streams.StreamItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// WATCHFIRST (member feedback): when a source does not play, move on to another source instead of
// asking the same server again and again; when none works, show the source list.

/** Automatic source changes allowed for one episode or movie before handing over to the member. */
internal const val MAX_AUTO_SOURCE_FALLBACKS = 4
private const val FALLBACK_POLL_INTERVAL_MS = 500L
private const val FALLBACK_POLL_COUNT = 40

/** Stable key for a source, so one that already failed in this session is never picked again. */
internal fun StreamItem.fallbackKey(): String =
    playerSourceIdentityKey()
        ?: playableDirectUrl
        ?: infoHash?.let { "$it:${fileIdx ?: -1}" }
        ?: "$addonId|$streamLabel|${streamSubtitle.orEmpty()}"

/** Sources the player can open by itself (not links that only open outside the app). */
private fun StreamItem.isAutoFallbackCandidate(): Boolean =
    playableDirectUrl != null ||
        isTorrentStream ||
        !ytId.isNullOrBlank() ||
        serverTarget != null ||
        clientResolve != null

internal fun PlayerScreenRuntime.markActiveSourceFailed() {
    activeSourceUrl?.let { failedSourceKeys += it }
    activeStream?.let { failedSourceKeys += it.fallbackKey() }
}

private fun PlayerScreenRuntime.isFailedSource(stream: StreamItem): Boolean =
    stream.fallbackKey() in failedSourceKeys ||
        stream.playableDirectUrl?.let { it in failedSourceKeys } == true

/**
 * Called when the current source cannot be played (after the engine's own retries). Starts the next
 * source that has not failed yet and returns true; when no source is left, opens the source list.
 * Returns false only when there is no episode or movie to look sources up for.
 */
internal fun PlayerScreenRuntime.tryNextSourceAfterError(message: String?): Boolean {
    if (sourceFallbackJob?.isActive == true) return true
    val currentVideoId = activeVideoId ?: return false
    markActiveSourceFailed()
    removeFailedStreamFromCache()
    if (autoSourceFallbackCount >= MAX_AUTO_SOURCE_FALLBACKS) {
        showSourcesAfterFallbackGaveUp(message)
        return true
    }
    autoSourceFallbackCount += 1
    errorMessage = null
    scrubbingPositionMs = null
    NuvioToastController.show("Esta fonte não carregou. Tentando outra…")

    val type = contentType ?: parentMetaType
    val season = activeSeasonNumber
    val episode = activeEpisodeNumber
    sourceFallbackJob = scope.launch {
        PlayerStreamsRepository.loadSources(
            type = type,
            videoId = currentVideoId,
            season = season,
            episode = episode,
        )
        var next: StreamItem? = null
        var polls = 0
        while (next == null && polls < FALLBACK_POLL_COUNT) {
            val state = PlayerStreamsRepository.sourceState.value
            next = state.groups
                .asSequence()
                .flatMap { it.streams.asSequence() }
                .firstOrNull { it.isAutoFallbackCandidate() && !isFailedSource(it) }
            val finished = state.emptyStateReason != null || (!state.isAnyLoading && state.groups.isNotEmpty())
            if (next != null || (polls > 0 && finished)) break
            delay(FALLBACK_POLL_INTERVAL_MS)
            polls++
        }
        val stream = next
        if (stream == null || activeVideoId != currentVideoId) {
            if (activeVideoId == currentVideoId) showSourcesAfterFallbackGaveUp(message)
            return@launch
        }
        switchToSource(stream, saveForReuse = false)
    }
    return true
}

/** No source left to try: keep the error and put the source list on top so the member can pick. */
private fun PlayerScreenRuntime.showSourcesAfterFallbackGaveUp(message: String?) {
    errorMessage = message
    controlsVisible = !playerControlsLocked
    NuvioToastController.show("Nenhuma fonte carregou. Escolha outra na lista.")
    openSourcesPanel()
}
