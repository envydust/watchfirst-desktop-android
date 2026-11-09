package com.nuvio.app.features.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioShelfSectionHeader
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.home.HomeUiState
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.livetv.LiveChannel
import com.nuvio.app.features.livetv.LiveChannelTile
import com.nuvio.app.features.watchprogress.CurrentDateProvider

private val TileShape = RoundedCornerShape(10.dp)
private val YearPattern = Regex("\\b(?:19|20)\\d{2}\\b")

/** Trending titles of the member's own catalogs: the home highlights first, then the head of each row. */
internal fun trendingSearchTitles(home: HomeUiState, limit: Int = 16): List<MetaPreview> =
    (home.heroItems + home.sections.flatMap { it.items.take(4) })
        .filter { (it.banner ?: it.landscapePoster ?: it.poster) != null }
        .distinctBy { it.stableKey() }
        .take(limit)

/** "Novidade" only for titles released in the current year; no other claim is made. */
private fun MetaPreview.isFromThisYear(currentYear: String): Boolean {
    val year = YearPattern.find(rawReleaseDate ?: releaseInfo ?: "")?.value ?: return false
    return year == currentYear
}

@Composable
internal fun SearchRecentChips(
    recentSearches: List<String>,
    onSearchPress: (String) -> Unit,
    onRemoveSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nuvio.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Buscas recentes",
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(recentSearches, key = { it }) { recent ->
                val shape = RoundedCornerShape(20.dp)
                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(shape)
                        .background(colors.surfaceCard)
                        .border(1.dp, colors.borderSubtle, shape)
                        .clickable { onSearchPress(recent) }
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                    Text(
                        text = recent,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp).widthIn(max = 220.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onRemoveSearch(recent) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remover \"$recent\"", tint = colors.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchTrendingRow(
    titles: List<MetaPreview>,
    tileWidth: Dp,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onPosterLongClick: ((MetaPreview) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val currentYear = remember { CurrentDateProvider.todayIsoDate().take(4) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NuvioShelfSectionHeader(title = "Em alta no WATCHFIRST", modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(titles, key = { it.stableKey() }) { meta ->
                TrendingTile(
                    meta = meta,
                    isNew = meta.isFromThisYear(currentYear),
                    onClick = { onPosterClick?.invoke(meta) },
                    onLongClick = onPosterLongClick?.let { { it(meta) } },
                    modifier = Modifier.width(tileWidth),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrendingTile(
    meta: MetaPreview,
    isNew: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nuvio.colors
    var logoFailed by remember(meta.logo) { mutableStateOf(false) }
    val landscape = meta.banner ?: meta.landscapePoster
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(TileShape)
            .border(1.dp, colors.borderSubtle, TileShape)
            .background(colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = landscape ?: meta.poster,
            contentDescription = meta.name,
            contentScale = ContentScale.Crop,
            alignment = if (landscape != null) Alignment.Center else Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.82f),
                    ),
                ),
        )
        val logo = meta.logo?.takeIf { it.isNotBlank() && !logoFailed }
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                onError = { logoFailed = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .fillMaxWidth(0.62f)
                    .height(30.dp),
            )
        } else {
            Text(
                text = meta.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            )
        }
        if (isNew) {
            Text(
                text = "Novidade",
                color = Color(0xFF080C10),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF5F7F8))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
internal fun SearchLiveHighlightsRow(
    channels: List<LiveChannel>,
    tileWidth: Dp,
    onChannelClick: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NuvioShelfSectionHeader(title = "Destaques ao vivo", modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(channels, key = { it.manifestUrl + "|" + it.favoriteKey }) { channel ->
                LiveChannelTile(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    modifier = Modifier.width(tileWidth),
                )
            }
        }
    }
}
