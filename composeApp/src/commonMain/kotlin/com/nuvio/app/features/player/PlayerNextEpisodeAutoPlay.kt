package com.nuvio.app.features.player

import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.debrid.DirectDebridPlayableResult
import com.nuvio.app.features.debrid.DirectDebridPlaybackResolver
import com.nuvio.app.features.debrid.toastMessage
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.downloads.DownloadItem
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.player.skip.NextEpisodeInfo
import com.nuvio.app.features.player.skip.PlayerNextEpisodeRules
import com.nuvio.app.features.streams.StreamAutoPlayMode
import com.nuvio.app.features.streams.StreamAutoPlaySelector
import com.nuvio.app.features.streams.StreamAutoPlaySource
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.watching.domain.isShortPlaceholderDuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal fun PlayerScreenRuntime.isAtNextEpisodeThreshold(): Boolean {
    if (playbackSnapshotKey != activePlaybackKey || !initialSeekApplied ||
        isShortPlaceholderDuration(playbackSnapshot.durationMs)
    ) return false
    // WATCHFIRST: the end of the episode always counts, even when the player reports a
    // last-moment buffering or error (common on the final segment of a stream).
    if (playbackSnapshot.isEnded) return true
    if (playbackSnapshot.isLoading || isScrubbingTimeline || errorMessage != null) return false
    // Preload: trigger source fetch before the button appears
    if (playerSettingsUiState.preloadNextEpisodeSources && !nextEpisodePreloadTriggered && nextEpisodeInfo != null && !isNetworkOffline()) {
        val preloadLeadMs = maxOf(playerSettingsUiState.streamAutoPlayTimeoutSeconds.toLong() * 1_000L, 10_000L)
            .coerceAtMost(30_000L)
        val shouldPreloadByCard = PlayerNextEpisodeRules.shouldShowNextEpisodeCard(
            positionMs = playbackSnapshot.positionMs + preloadLeadMs,
            durationMs = playbackSnapshot.durationMs,
            skipIntervals = skipIntervals,
            thresholdMode = playerSettingsUiState.nextEpisodeThresholdMode,
            thresholdPercent = playerSettingsUiState.nextEpisodeThresholdPercent,
            thresholdMinutesBeforeEnd = playerSettingsUiState.nextEpisodeThresholdMinutesBeforeEnd,
        )
        val shouldPreloadByEnd = playbackSnapshot.durationMs > 0L &&
            (playbackSnapshot.durationMs - playbackSnapshot.positionMs) <= preloadLeadMs
        if (shouldPreloadByCard || shouldPreloadByEnd) {
            preloadNextEpisodeSources()
        }
    }
    return playbackSnapshot.isEnded || PlayerNextEpisodeRules.shouldShowNextEpisodeCard(
        positionMs = playbackSnapshot.positionMs,
        durationMs = playbackSnapshot.durationMs,
        skipIntervals = skipIntervals,
        thresholdMode = playerSettingsUiState.nextEpisodeThresholdMode,
        thresholdPercent = playerSettingsUiState.nextEpisodeThresholdPercent,
        thresholdMinutesBeforeEnd = playerSettingsUiState.nextEpisodeThresholdMinutesBeforeEnd,
    )
}

/**
 * WATCHFIRST: true only when the player is stable and clearly before the point where the
 * next-episode card belongs (for example, after the user seeks back). A momentary buffering,
 * scrub or error near the end is not a reason to hide the card or cancel the next episode.
 */
internal fun PlayerScreenRuntime.isClearlyBeforeNextEpisodeThreshold(): Boolean {
    if (playbackSnapshotKey != activePlaybackKey || !initialSeekApplied || playbackSnapshot.isEnded ||
        playbackSnapshot.isLoading || isScrubbingTimeline || errorMessage != null
    ) return false
    if (isShortPlaceholderDuration(playbackSnapshot.durationMs)) return true
    return !PlayerNextEpisodeRules.shouldShowNextEpisodeCard(
        positionMs = playbackSnapshot.positionMs,
        durationMs = playbackSnapshot.durationMs,
        skipIntervals = skipIntervals,
        thresholdMode = playerSettingsUiState.nextEpisodeThresholdMode,
        thresholdPercent = playerSettingsUiState.nextEpisodeThresholdPercent,
        thresholdMinutesBeforeEnd = playerSettingsUiState.nextEpisodeThresholdMinutesBeforeEnd,
    )
}

internal fun PlayerScreenRuntime.cancelNextEpisodeAutoPlay() {
    nextEpisodeAutoPlayJob?.cancel()
    nextEpisodeAutoPlayJob = null
    nextEpisodeAutoPlayAutomatic = false
    nextEpisodeAutoPlaySearching = false
    nextEpisodeAutoPlaySourceName = null
    nextEpisodeAutoPlayCountdown = null
    nextEpisodePlayNowRequested = false
}

internal fun CoroutineScope.launchPlayerNextEpisodeAutoPlay(
    previousJob: Job?,
    nextEpisodeInfo: NextEpisodeInfo?,
    allEpisodes: List<MetaVideo>,
    parentMetaId: String,
    parentMetaType: String,
    contentType: String?,
    settings: PlayerSettingsUiState,
    currentStreamBingeGroup: String?,
    onDownloadedEpisodeSelected: (DownloadItem, MetaVideo) -> Unit,
    onEpisodeStreamSelected: (StreamItem, MetaVideo) -> Unit,
    onManualSelectionRequired: (MetaVideo) -> Unit,
    onSearchingChanged: (Boolean) -> Unit,
    onSourceNameChanged: (String?) -> Unit,
    onCountdownChanged: (Int?) -> Unit,
    onNextEpisodeCardVisibleChanged: (Boolean) -> Unit,
    userInitiated: Boolean = false,
    shouldPlayNow: () -> Boolean = { false },
): Job? {
    val nextVideoId = nextEpisodeInfo?.videoId ?: return null
    val nextVideo = allEpisodes.firstOrNull { video -> video.id == nextVideoId } ?: return null
    if (nextEpisodeInfo.hasAired != true) return null

    val downloadedNextEpisode = DownloadsRepository.findPlayableDownload(
        parentMetaId = parentMetaId,
        seasonNumber = nextVideo.season,
        episodeNumber = nextVideo.episode,
        videoId = nextVideo.id,
    )
    if (downloadedNextEpisode != null) {
        onDownloadedEpisodeSelected(downloadedNextEpisode, nextVideo)
        return null
    }

    // Offline: the auto-play fan-out is a network search, so skip it entirely.
    if (isNetworkOffline()) return null

    previousJob?.cancel()
    onSearchingChanged(true)
    onSourceNameChanged(null)
    onCountdownChanged(null)

    val type = contentType ?: parentMetaType
    // WATCHFIRST: a tap on "Next episode" means "play it", also when sources are picked by hand:
    // pick the best source on its own (same group first) and only fall back to the list if none.
    val userWantsAutoPick = userInitiated && settings.streamAutoPlayMode == StreamAutoPlayMode.MANUAL
    val shouldAutoSelectInManualMode =
        settings.streamAutoPlayMode == StreamAutoPlayMode.MANUAL &&
            (
                settings.streamAutoPlayNextEpisodeEnabled ||
                    settings.streamAutoPlayPreferBingeGroup ||
                    userWantsAutoPick
                )

    val bingeGroupOnlyManualMode =
        shouldAutoSelectInManualMode && !userWantsAutoPick &&
            (!settings.streamAutoPlayNextEpisodeEnabled ||
                !settings.streamAutoPlayNextEpisodeFallbackEnabled) &&
            settings.streamAutoPlayPreferBingeGroup

    val effectiveMode = if (shouldAutoSelectInManualMode) {
        StreamAutoPlayMode.FIRST_STREAM
    } else {
        settings.streamAutoPlayMode
    }
    val effectiveSource = if (shouldAutoSelectInManualMode) {
        StreamAutoPlaySource.ALL_SOURCES
    } else {
        settings.streamAutoPlaySource
    }
    val effectiveSelectedAddons = if (shouldAutoSelectInManualMode) {
        emptySet()
    } else {
        settings.streamAutoPlaySelectedAddons
    }
    val effectiveSelectedPlugins = if (shouldAutoSelectInManualMode) {
        emptySet()
    } else {
        settings.streamAutoPlaySelectedPlugins
    }
    val effectiveRegex = if (shouldAutoSelectInManualMode) {
        ""
    } else {
        settings.streamAutoPlayRegex
    }
    val preferredBingeGroup = if (settings.streamAutoPlayPreferBingeGroup || userWantsAutoPick) {
        currentStreamBingeGroup
    } else {
        null
    }
    val preferBingeGroupInSelection = settings.streamAutoPlayPreferBingeGroup ||
        (userWantsAutoPick && preferredBingeGroup != null)

    return launch {
        // Connectivity may have dropped between the request and the actual search.
        if (isNetworkOffline()) {
            onSearchingChanged(false)
            return@launch
        }

        PlayerStreamsRepository.loadEpisodeStreams(
            type = type,
            videoId = nextVideo.id,
            season = nextVideo.season,
            episode = nextVideo.episode,
        )

        if (effectiveMode == StreamAutoPlayMode.MANUAL) {
            onSearchingChanged(false)
            onNextEpisodeCardVisibleChanged(false)
            onManualSelectionRequired(nextVideo)
            return@launch
        }

        val installedAddonNames = AddonRepository.uiState.value.addons
            .enabledAddons()
            .map { it.displayTitle }
            .toSet()
        val debridSettings = DebridSettingsRepository.snapshot()

        // WATCHFIRST: after a tap, wait a little longer for the first sources before giving up.
        val timeoutSeconds = if (userInitiated && settings.streamAutoPlayTimeoutSeconds in 1..30) {
            maxOf(settings.streamAutoPlayTimeoutSeconds, USER_NEXT_EPISODE_MIN_WAIT_SECONDS)
        } else {
            settings.streamAutoPlayTimeoutSeconds
        }
        var autoSelectTriggered = false
        var timeoutElapsed = false
        var selectedStream: StreamItem? = null
        val autoSelectSettled = CompletableDeferred<Unit>()

        fun settleAutoSelect() {
            if (!autoSelectSettled.isCompleted) {
                autoSelectSettled.complete(Unit)
            }
        }

        fun selectStream(stream: StreamItem) {
            autoSelectTriggered = true
            selectedStream = stream
            settleAutoSelect()
        }

        fun finishWithoutSelection() {
            autoSelectTriggered = true
            settleAutoSelect()
        }

        fun trySelectStream(streams: List<StreamItem>): StreamItem? =
            StreamAutoPlaySelector.selectAutoPlayStream(
                streams = streams,
                mode = effectiveMode,
                regexPattern = effectiveRegex,
                source = effectiveSource,
                installedAddonNames = installedAddonNames,
                selectedAddons = effectiveSelectedAddons,
                selectedPlugins = effectiveSelectedPlugins,
                preferredBingeGroup = preferredBingeGroup,
                preferBingeGroupInSelection = preferBingeGroupInSelection,
                bingeGroupOnly = bingeGroupOnlyManualMode,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                activeResolverProviderId = debridSettings.activeResolverProviderId,
            )

        fun tryBingeGroupOnly(streams: List<StreamItem>): StreamItem? {
            if (preferredBingeGroup == null || !preferBingeGroupInSelection) return null
            return StreamAutoPlaySelector.selectAutoPlayStream(
                streams = streams,
                mode = effectiveMode,
                regexPattern = effectiveRegex,
                source = effectiveSource,
                installedAddonNames = installedAddonNames,
                selectedAddons = effectiveSelectedAddons,
                selectedPlugins = effectiveSelectedPlugins,
                preferredBingeGroup = preferredBingeGroup,
                preferBingeGroupInSelection = true,
                bingeGroupOnly = true,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                activeResolverProviderId = debridSettings.activeResolverProviderId,
            )
        }

        val innerJob = launch {
            PlayerStreamsRepository.episodeStreamsState.collectLatest { state ->
                if (state.groups.isEmpty() && state.isAnyLoading) return@collectLatest

                val allStreams = state.groups.flatMap { it.streams }

                if (autoSelectTriggered) {
                    // Already resolved.
                } else if (timeoutElapsed) {
                    if (allStreams.isNotEmpty()) {
                        val candidate = trySelectStream(allStreams)
                        if (candidate != null) {
                            selectStream(candidate)
                        }
                    }
                } else if (allStreams.isNotEmpty()) {
                    val earlyMatch = tryBingeGroupOnly(allStreams)
                    if (earlyMatch != null) {
                        selectStream(earlyMatch)
                    }
                }

                if (!autoSelectTriggered && !state.isAnyLoading) {
                    if (allStreams.isNotEmpty()) {
                        val candidate = trySelectStream(allStreams)
                        if (candidate != null) {
                            selectStream(candidate)
                        }
                    }
                    if (!autoSelectTriggered) {
                        finishWithoutSelection()
                    }
                    return@collectLatest
                }

                if (autoSelectTriggered) return@collectLatest
            }
        }

        val timeoutMs = timeoutSeconds * 1_000L
        val isBoundedTimeout = timeoutSeconds in 1..30

        if (isBoundedTimeout) {
            // If streams are already cached (preload), autoSelectSettled
            // completes immediately. Don't wait the full timeout in that case.
            val settled = kotlinx.coroutines.withTimeoutOrNull(timeoutMs) { autoSelectSettled.await() }
            if (settled == null) {
                timeoutElapsed = true
                if (!autoSelectTriggered) {
                    val allStreams = PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }
                    if (allStreams.isNotEmpty()) {
                        val candidate = trySelectStream(allStreams)
                        if (candidate != null) {
                            selectStream(candidate)
                        }
                    }
                }
            }
            innerJob.cancel()
            if (selectedStream == null && !autoSelectTriggered) {
                finishWithoutSelection()
            }
        } else {
            timeoutElapsed = true
            if (!autoSelectTriggered) {
                val allStreams = PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }
                if (allStreams.isNotEmpty()) {
                    trySelectStream(allStreams)?.let(::selectStream)
                }
            }
            val completed = withTimeoutOrNull(NEXT_EPISODE_HARD_TIMEOUT_MS) { autoSelectSettled.await() }
            innerJob.cancel()
            if (completed == null && !autoSelectTriggered) {
                val allStreams = PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }
                if (allStreams.isNotEmpty()) {
                    selectedStream = trySelectStream(allStreams)
                }
                finishWithoutSelection()
            }
        }

        val selected = selectedStream?.let { stream ->
            when (val result = DirectDebridPlaybackResolver.resolveToPlayableStream(stream, nextVideo.season, nextVideo.episode)) {
                is DirectDebridPlayableResult.Success -> result.stream
                else -> {
                    result.toastMessage()?.let { NuvioToastController.show(it) }
                    PlayerStreamsRepository.loadEpisodeStreams(
                        type = type,
                        videoId = nextVideo.id,
                        season = nextVideo.season,
                        episode = nextVideo.episode,
                        forceRefresh = true,
                    )
                    null
                }
            }
        }
        onSearchingChanged(false)
        if (selected != null) {
            onSourceNameChanged((selected.name?.takeIf { it.isNotBlank() } ?: selected.addonName).trim())
            // WATCHFIRST: no countdown after a tap, and a tap during the countdown plays right away.
            if (!userInitiated) {
                var remaining = 3
                while (remaining > 0 && !shouldPlayNow()) {
                    onCountdownChanged(remaining)
                    var waitedMs = 0L
                    while (waitedMs < 1_000L && !shouldPlayNow()) {
                        delay(100)
                        waitedMs += 100
                    }
                    remaining--
                }
            }
            onEpisodeStreamSelected(selected, nextVideo)
            onNextEpisodeCardVisibleChanged(false)
            onCountdownChanged(null)
            onSourceNameChanged(null)
        } else {
            onManualSelectionRequired(nextVideo)
            onNextEpisodeCardVisibleChanged(false)
        }
    }
}

private const val USER_NEXT_EPISODE_MIN_WAIT_SECONDS = 8

internal fun PlayerScreenRuntime.preloadNextEpisodeSources() {
    if (nextEpisodePreloadTriggered) return
    val nextEp = nextEpisodeInfo ?: return
    if (nextEp.hasAired != true) return
    val type = contentType ?: return
    // Offline: preloading streams is a network search; retry once connectivity returns.
    if (isNetworkOffline()) return

    nextEpisodePreloadTriggered = true
    nextEpisodePreloadJob?.cancel()
    nextEpisodePreloadJob = scope.launch {
        PlayerStreamsRepository.loadEpisodeStreams(
            type = type,
            videoId = nextEp.videoId,
            season = nextEp.season,
            episode = nextEp.episode
        )
    }
}

internal fun PlayerScreenRuntime.cancelNextEpisodePreload() {
    nextEpisodePreloadJob?.cancel()
    nextEpisodePreloadJob = null
    nextEpisodePreloadTriggered = false
}
