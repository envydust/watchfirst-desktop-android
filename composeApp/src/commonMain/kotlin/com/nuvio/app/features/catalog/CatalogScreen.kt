package com.nuvio.app.features.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.WatchfirstSearchField
import com.nuvio.app.features.addons.AddonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nuvio.app.core.ui.NuvioBackButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.core.ui.NuvioPosterWatchedOverlay
import com.nuvio.app.core.ui.SkeletonPoster
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.withDuplicateSafeLazyKeys
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.navigation.LocalUseNativeNavigation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun CatalogScreen(
    title: String,
    subtitle: String,
    target: CatalogTarget,
    onBack: () -> Unit,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by CatalogRepository.uiState.collectAsStateWithLifecycle()
    val homeCatalogSettingsUiState by HomeCatalogSettingsRepository.uiState.collectAsStateWithLifecycle()
    val posterCardStyle = rememberPosterCardStyleUiState()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val initialScrollPosition = remember(
        target,
        homeCatalogSettingsUiState.hideUnreleasedContent,
    ) {
        CatalogRepository.scrollPosition(
            target = target,
        )
    }
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = initialScrollPosition.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialScrollPosition.firstVisibleItemScrollOffset,
    )
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var observedOfflineState by remember { mutableStateOf(false) }

    // WATCHFIRST: search inside this list only (member feedback), instead of the global search.
    var searchOpen by rememberSaveable(target) { mutableStateOf(false) }
    var query by rememberSaveable(target) { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    // When the addon can search this very catalog, ask it; otherwise filter the titles already loaded.
    val searchableCatalog = remember(target, addonsUiState.addons) {
        (target as? CatalogTarget.Addon)?.let { addonTarget ->
            addonsUiState.addons
                .firstOrNull { it.manifestUrl == addonTarget.manifestUrl }
                ?.manifest
                ?.catalogs
                ?.firstOrNull { it.id == addonTarget.catalogId && it.type == addonTarget.contentType }
                ?.takeIf { catalog -> catalog.extra.any { it.name.equals("search", ignoreCase = true) } }
        }
    }
    var remoteResults by remember(target) { mutableStateOf<List<MetaPreview>?>(null) }
    var remoteSearching by remember(target) { mutableStateOf(false) }
    LaunchedEffect(trimmedQuery, searchableCatalog) {
        remoteResults = null
        val addonTarget = target as? CatalogTarget.Addon
        if (trimmedQuery.length < 2 || searchableCatalog == null || addonTarget == null) {
            remoteSearching = false
            return@LaunchedEffect
        }
        remoteSearching = true
        delay(400)
        remoteResults = try {
            fetchCatalogPage(
                manifestUrl = addonTarget.manifestUrl,
                type = addonTarget.contentType,
                catalogId = addonTarget.catalogId,
                genre = addonTarget.genre,
                search = trimmedQuery,
            ).items
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            null
        }
        remoteSearching = false
    }
    // Without addon search, keep reading more pages of the list while the member searches.
    var searchPagesLoaded by remember(target) { mutableIntStateOf(0) }
    LaunchedEffect(trimmedQuery) {
        searchPagesLoaded = 0
        if (trimmedQuery.isNotEmpty()) gridState.scrollToItem(0)
    }
    LaunchedEffect(trimmedQuery, uiState.isLoading, uiState.canLoadMore, searchPagesLoaded, searchableCatalog) {
        if (trimmedQuery.isEmpty() || searchableCatalog != null) return@LaunchedEffect
        if (!uiState.isLoading && uiState.canLoadMore && searchPagesLoaded < MAX_LIST_SEARCH_PAGES) {
            searchPagesLoaded += 1
            CatalogRepository.loadMore()
        }
    }
    val visibleItems = remember(uiState.items, trimmedQuery, remoteResults) {
        if (trimmedQuery.isEmpty()) {
            uiState.items
        } else {
            val needle = trimmedQuery.listSearchKey()
            val local = uiState.items.filter { it.name.listSearchKey().contains(needle) }
            (local + remoteResults.orEmpty()).distinctBy { it.stableKey() }
        }
    }
    val searchBusy = trimmedQuery.isNotEmpty() && (
        remoteSearching ||
            (searchableCatalog == null && uiState.isLoading)
        )
    PlatformBackHandler(enabled = searchOpen) {
        searchOpen = false
        query = ""
    }

    LaunchedEffect(target, homeCatalogSettingsUiState.hideUnreleasedContent) {
        CatalogRepository.load(
            target = target,
        )
    }

    LaunchedEffect(gridState, target, homeCatalogSettingsUiState.hideUnreleasedContent) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                CatalogRepository.saveScrollPosition(
                    target = target,
                    firstVisibleItemIndex = index,
                    firstVisibleItemScrollOffset = offset,
                )
            }
    }

    LaunchedEffect(gridState, uiState.canLoadMore, uiState.isLoading) {
        snapshotFlow { gridState.layoutInfo }
            .map { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= layoutInfo.totalItemsCount - 6
            }
            .distinctUntilChanged()
            .filter { it && uiState.canLoadMore && !uiState.isLoading }
            .collect {
                CatalogRepository.loadMore()
            }
    }

    LaunchedEffect(networkStatusUiState.condition, target) {
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@LaunchedEffect
                observedOfflineState = false
                CatalogRepository.load(
                    target = target,
                    force = true,
                )
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val columns = remember(maxWidth) { catalogGridColumnsForWidth(maxWidth) }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = with(androidx.compose.ui.platform.LocalDensity.current) { headerHeightPx.toDp() } + 12.dp,
                    end = 16.dp,
                    bottom = nuvioSafeBottomPadding(28.dp),
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (uiState.items.isEmpty() && uiState.isLoading) {
                    items(columns * 3) {
                        SkeletonPoster(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = posterCardStyle.cornerRadiusDp.dp,
                            showLabels = !posterCardStyle.hideLabelsEnabled,
                        )
                    }
                } else if (uiState.items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CatalogEmptyState(
                            errorMessage = uiState.errorMessage,
                            networkCondition = networkStatusUiState.condition,
                            onRetry = {
                                NetworkStatusRepository.requestRefresh(force = true)
                                CatalogRepository.load(
                                    target = target,
                                    force = true,
                                )
                            },
                        )
                    }
                } else if (trimmedQuery.isNotEmpty() && visibleItems.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (searchBusy) {
                            CatalogLoadingFooter()
                        } else {
                            HomeEmptyStateCard(
                                title = "Nada encontrado nesta lista",
                                message = "Nenhum título de \"$title\" combina com \"$trimmedQuery\". Confira a digitação ou use a Busca geral.",
                            )
                        }
                    }
                } else {
                    items(
                        items = visibleItems.withDuplicateSafeLazyKeys { item -> item.stableKey() },
                        key = { item -> item.lazyKey },
                    ) { keyedItem ->
                        val item = keyedItem.value
                        CatalogPosterTile(
                            item = item,
                            cornerRadiusDp = posterCardStyle.cornerRadiusDp,
                            hideLabels = posterCardStyle.hideLabelsEnabled,
                            isWatched = WatchingState.isPosterWatched(
                                watchedKeys = watchedUiState.watchedKeys,
                                item = item,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            ),
                            onClick = onPosterClick?.let { { it(item) } },
                            onLongClick = onPosterLongClick?.let { { it(item) } },
                        )
                    }
                    if (uiState.isLoading || searchBusy) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CatalogLoadingFooter()
                        }
                    }
                }
            }

            CatalogHeader(
                title = title,
                subtitle = subtitle,
                modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                onBack = onBack,
                searchOpen = searchOpen,
                query = query,
                onQueryChange = { query = it.take(60) },
                onToggleSearch = {
                    if (searchOpen) query = ""
                    searchOpen = !searchOpen
                },
            )
        }
    }
}

@Composable
private fun CatalogHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    searchOpen: Boolean = false,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onToggleSearch: (() -> Unit)? = null,
) {
    if (LocalUseNativeNavigation.current) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(44.dp),
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 52.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NuvioBackButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                iconSize = 24.dp,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (onToggleSearch != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onToggleSearch),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = if (searchOpen) "Fechar busca nesta lista" else "Buscar nesta lista",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (searchOpen) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
            Spacer(modifier = Modifier.height(10.dp))
            WatchfirstSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Buscar nesta lista",
                fontSize = 18.sp,
                textFieldModifier = Modifier.focusRequester(focusRequester),
            )
        }
    }
}

private const val MAX_LIST_SEARCH_PAGES = 8

/** Lower case without Portuguese accents, so "acao" finds "Ação". */
private fun String.listSearchKey(): String = buildString(length) {
    for (char in this@listSearchKey.lowercase()) {
        append(
            when (char) {
                'á', 'à', 'â', 'ã', 'ä' -> 'a'
                'é', 'è', 'ê', 'ë' -> 'e'
                'í', 'ì', 'î', 'ï' -> 'i'
                'ó', 'ò', 'ô', 'õ', 'ö' -> 'o'
                'ú', 'ù', 'û', 'ü' -> 'u'
                'ç' -> 'c'
                'ñ' -> 'n'
                else -> char
            },
        )
    }
}

@Composable
private fun CatalogPosterTile(
    item: MetaPreview,
    cornerRadiusDp: Int,
    hideLabels: Boolean,
    isWatched: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(item.posterShape.catalogAspectRatio())
                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                .background(MaterialTheme.colorScheme.surface)
                .nuvioCardDepth(
                    shape = RoundedCornerShape(cornerRadiusDp.dp),
                    surface = NuvioCardDepthSurface.Posters,
                )
                .posterCardClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    zoomImageUrl = item.poster,
                    zoomCornerRadius = cornerRadiusDp.dp,
                ),
        ) {
            if (item.poster != null) {
                val platformContext = coil3.compose.LocalPlatformContext.current
                val hasFallback = !item.rawPosterUrl.isNullOrBlank() && item.rawPosterUrl != item.poster
                val imageModel = remember(item.poster, item.rawPosterUrl, platformContext) {
                    if (hasFallback) {
                        coil3.request.ImageRequest.Builder(platformContext)
                            .data(item.poster)
                            .memoryCacheKeyExtras(
                                mapOf(com.nuvio.app.core.poster.CustomPosterFallbackInterceptor.FALLBACK_URL_KEY to item.rawPosterUrl!!)
                            )
                            .build()
                    } else {
                        item.poster
                    }
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            NuvioPosterWatchedOverlay(isWatched = isWatched)
        }
        if (!hideLabels) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CatalogEmptyState(
    errorMessage: String?,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
) {
    if (
        !errorMessage.isNullOrBlank() &&
        (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable)
    ) {
        NuvioNetworkOfflineCard(
            condition = networkCondition,
            onRetry = onRetry,
        )
        return
    }

    val loadFailed = !errorMessage.isNullOrBlank()
    HomeEmptyStateCard(
        title = stringResource(
            if (loadFailed) Res.string.catalog_load_failed_title else Res.string.catalog_empty_title,
        ),
        message = errorMessage ?: stringResource(Res.string.catalog_empty_message),
        actionLabel = if (loadFailed) stringResource(Res.string.action_retry) else null,
        onActionClick = if (loadFailed) onRetry else null,
    )
}

@Composable
private fun CatalogLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        NuvioLoadingIndicator(
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun PosterShape.catalogAspectRatio(): Float =
    when (this) {
        PosterShape.Poster -> 0.68f
        PosterShape.Square -> 1f
        PosterShape.Landscape -> 1.78f
    }

private fun catalogGridColumnsForWidth(screenWidth: Dp): Int =
    when {
        screenWidth >= 1400.dp -> 7
        screenWidth >= 1200.dp -> 6
        screenWidth >= 1000.dp -> 5
        screenWidth >= 840.dp -> 4
        else -> 3
    }
