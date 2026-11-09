package com.nuvio.app.features.livetv

import com.nuvio.app.features.access.watchfirstAddonUrlKey
import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.catalog.fetchCatalogPage
import com.nuvio.app.features.catalog.mergeCatalogItems
import com.nuvio.app.features.catalog.supportsPagination
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Same limits as the WATCHFIRST PC app.
private const val MAX_PAGES_PER_CATALOG = 5
private const val MAX_GENRES_PER_CATALOG = 40
private const val PARALLEL_REQUESTS = 4

internal data class LiveChannel(
    val meta: MetaPreview,
    val addonName: String,
    val manifestUrl: String,
    val category: String,
) {
    val favoriteKey: String get() = meta.stableKey()
    val imageUrl: String? get() = meta.logo ?: meta.poster ?: meta.landscapePoster ?: meta.banner
}

internal data class LiveTvUiState(
    val loading: Boolean = false,
    val channels: List<LiveChannel> = emptyList(),
    val addonNames: List<String> = emptyList(),
    val totalSources: Int = 0,
    val failedSources: Int = 0,
    val loaded: Boolean = false,
)

internal sealed interface LiveTvFilter {
    data object All : LiveTvFilter
    data object Favorites : LiveTvFilter
    data class Addon(val name: String) : LiveTvFilter
}

internal data class LiveCatalogRequest(
    val catalog: AddonCatalog,
    val genre: String?,
)

internal data class LiveChannelGroup(
    val category: String,
    val channels: List<LiveChannel>,
)

/** Installed addons the administrator marked as live for this member. */
internal fun liveAddons(addons: List<ManagedAddon>, liveUrls: Set<String>): List<ManagedAddon> {
    val keys = liveUrls.mapTo(HashSet(), ::watchfirstAddonUrlKey)
    return addons.filter { it.isActive && watchfirstAddonUrlKey(it.manifestUrl) in keys }
}

/**
 * Catalogs that can be listed without user input. Catalogs that require a genre are
 * requested once per genre option; catalogs that require anything else (e.g. search) are skipped.
 */
internal fun liveCatalogRequests(catalogs: List<AddonCatalog>): List<LiveCatalogRequest> =
    catalogs.flatMap { catalog ->
        val required = catalog.extra.filter { it.isRequired }
        val genre = catalog.extra.firstOrNull { it.name.equals("genre", ignoreCase = true) }
        when {
            required.any { !it.name.equals("genre", ignoreCase = true) } -> emptyList()
            genre != null && genre.isRequired ->
                genre.options.take(MAX_GENRES_PER_CATALOG).map { LiveCatalogRequest(catalog, it) }
            else -> listOf(LiveCatalogRequest(catalog, null))
        }
    }

internal fun liveChannelCategory(meta: MetaPreview, request: LiveCatalogRequest): String =
    request.genre?.trim()?.takeIf { it.isNotEmpty() }
        ?: meta.genres.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        ?: request.catalog.name.trim().takeIf { it.isNotEmpty() }
        ?: "Canais"

internal fun filterLiveChannels(
    channels: List<LiveChannel>,
    query: String,
    filter: LiveTvFilter,
    favorites: Set<String>,
): List<LiveChannel> {
    val needle = query.trim().lowercase()
    return channels.filter { channel ->
        val matchesFilter = when (filter) {
            LiveTvFilter.All -> true
            LiveTvFilter.Favorites -> channel.favoriteKey in favorites
            is LiveTvFilter.Addon -> channel.addonName == filter.name
        }
        matchesFilter && (
            needle.isEmpty() ||
                channel.meta.name.lowercase().contains(needle) ||
                channel.category.lowercase().contains(needle)
            )
    }
}

/** Groups channels by category, keeping the order in which categories first appear. */
internal fun groupLiveChannels(channels: List<LiveChannel>): List<LiveChannelGroup> {
    val groups = LinkedHashMap<String, MutableList<LiveChannel>>()
    channels.forEach { groups.getOrPut(it.category) { mutableListOf() }.add(it) }
    return groups.map { (category, items) -> LiveChannelGroup(category, items) }
}

/** A fixed number of workers instead of a coroutine for every catalog. */
internal suspend fun <T, R> boundedCatalogMap(items: List<T>, parallelism: Int, block: suspend (T) -> R): List<R> = coroutineScope {
    require(parallelism > 0)
    val cursorLock = Mutex()
    var cursor = 0
    val results = MutableList<Result<R>?>(items.size) { null }
    List(minOf(parallelism, items.size)) {
        async {
            while (true) {
                val index = cursorLock.withLock {
                    if (cursor < items.size) cursor++ else null
                } ?: break
                results[index] = Result.success(block(items[index]))
            }
        }
    }.awaitAll()
    results.map { checkNotNull(it).getOrThrow() }
}

/** Loads the channels of the live addons, as the PC app's "TV ao vivo" tab does. */
internal object LiveTvRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(LiveTvUiState())
    val state: StateFlow<LiveTvUiState> = _state.asStateFlow()
    private var job: Job? = null
    private var loadedSources: List<String> = emptyList()

    fun ensureLoaded(addons: List<ManagedAddon>, liveUrls: Set<String>) {
        val sources = liveAddons(addons, liveUrls).map { it.manifestUrl }
        if (sources == loadedSources && (job?.isActive == true || _state.value.loaded)) return
        refresh(addons, liveUrls, forceRefresh = false)
    }

    fun refresh(addons: List<ManagedAddon>, liveUrls: Set<String>, forceRefresh: Boolean = true) {
        val sources = liveAddons(addons, liveUrls)
        loadedSources = sources.map { it.manifestUrl }
        job?.cancel()
        _state.value = _state.value.copy(
            loading = sources.isNotEmpty(),
            addonNames = sources.map { it.displayTitle }.distinct(),
            totalSources = sources.size,
            failedSources = 0,
            loaded = sources.isEmpty(),
            channels = if (sources.isEmpty()) emptyList() else _state.value.channels,
        )
        if (sources.isEmpty()) return
        job = scope.launch {
            val work = sources.flatMapIndexed { index, addon ->
                addon.manifest?.let { manifest ->
                    liveCatalogRequests(manifest.catalogs).map { index to it }
                }.orEmpty()
            }
            val results = boundedCatalogMap(work, PARALLEL_REQUESTS) { (index, request) ->
                try {
                    loadCatalog(sources[index], request, forceRefresh)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    null
                }
            }
            val attempted = IntArray(sources.size)
            val succeeded = IntArray(sources.size)
            val uniqueChannels = LinkedHashMap<String, LiveChannel>()
            results.forEachIndexed { index, result ->
                val sourceIndex = work[index].first
                attempted[sourceIndex]++
                if (result != null) {
                    succeeded[sourceIndex]++
                    result.forEach { channel ->
                        val key = channel.manifestUrl + "|" + channel.favoriteKey
                        if (key !in uniqueChannels) uniqueChannels[key] = channel
                    }
                }
            }
            val failedCount = sources.indices.count {
                sources[it].manifest == null || (attempted[it] > 0 && succeeded[it] == 0)
            }
            _state.value = _state.value.copy(
                loading = false,
                channels = uniqueChannels.values.toList(),
                failedSources = failedCount,
                loaded = true,
            )
        }
    }

    private suspend fun loadCatalog(
        addon: ManagedAddon,
        request: LiveCatalogRequest,
        forceRefresh: Boolean,
    ): List<LiveChannel> {
        var items = emptyList<MetaPreview>()
        var skip: Int? = null
        var pages = 0
        while (pages < MAX_PAGES_PER_CATALOG) {
            val page = fetchCatalogPage(
                manifestUrl = addon.manifestUrl,
                type = request.catalog.type,
                catalogId = request.catalog.id,
                genre = request.genre,
                skip = skip,
                forceRefresh = forceRefresh,
            )
            val merged = mergeCatalogItems(items, page.items)
            val gotNew = merged.size > items.size
            items = merged
            pages++
            if (!request.catalog.supportsPagination() || !gotNew || page.nextSkip == null) break
            skip = page.nextSkip
        }
        return items.map { meta ->
            LiveChannel(
                meta = meta,
                addonName = addon.displayTitle,
                manifestUrl = addon.manifestUrl,
                category = liveChannelCategory(meta, request),
            )
        }
    }
}
