package com.nuvio.app.features.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.access.WatchfirstAccess
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.home.MetaPreview

private val LiveRed = Color(0xFFE53935)
private val FavoriteGold = Color(0xFFFFC107)
private val CardGap = 12.dp
private val ScreenGutter = 16.dp

/**
 * "TV ao vivo" for MAX members, the same tab the WATCHFIRST PC app has: channels from the addons
 * the administrator marked as live, grouped by category, with search, addon filter and favorites.
 * These addons never show on the home screen.
 */
@Composable
internal fun LiveTvScreen(
    modifier: Modifier = Modifier,
    onPlay: (MetaPreview) -> Unit,
) {
    val colors = MaterialTheme.nuvio.colors
    val access by WatchfirstAccess.state.collectAsStateWithLifecycle()
    if (!access.liveTvEnabled) {
        LiveTvMessage(
            modifier = modifier.fillMaxSize().background(colors.background),
            title = "TV ao vivo",
            text = "A TV ao vivo está disponível no plano MAX. Fale com o administrador para mudar de plano.",
        )
        return
    }

    val addonsState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val state by LiveTvRepository.state.collectAsStateWithLifecycle()
    val liveSourceKey = remember(addonsState.addons, access.liveAddonUrls) {
        liveAddons(addonsState.addons, access.liveAddonUrls).map { it.manifestUrl }
    }
    LaunchedEffect(liveSourceKey) { LiveTvRepository.ensureLoaded(addonsState.addons, access.liveAddonUrls) }

    var query by rememberSaveable { mutableStateOf("") }
    var filter by remember { mutableStateOf<LiveTvFilter>(LiveTvFilter.All) }
    var listMode by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<LiveChannel?>(null) }
    var favorites by remember { mutableStateOf(WatchfirstAccess.loadFavoriteChannels()) }
    val toggleFavorite: (LiveChannel) -> Unit = { channel ->
        val next = if (channel.favoriteKey in favorites) favorites - channel.favoriteKey else favorites + channel.favoriteKey
        favorites = next
        WatchfirstAccess.saveFavoriteChannels(next)
    }
    val visible = remember(state.channels, query, filter, favorites) {
        groupLiveChannels(filterLiveChannels(state.channels, query, filter, favorites))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.background)) {
        val wide = maxWidth >= 600.dp
        val cardMinWidth = if (wide) 200.dp else 150.dp
        val columns = ((maxWidth - ScreenGutter * 2 + CardGap) / (cardMinWidth + CardGap)).toInt().coerceAtLeast(2)

        NuvioScreen(horizontalPadding = 0.dp) {
            stickyHeader(key = "live-header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .padding(bottom = 10.dp),
                ) {
                    NuvioScreenHeader(
                        title = "TV ao vivo",
                        modifier = Modifier.padding(horizontal = ScreenGutter),
                        actions = {
                            IconButton(
                                onClick = { LiveTvRepository.refresh(addonsState.addons, access.liveAddonUrls) },
                                enabled = !state.loading,
                            ) {
                                if (state.loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar canais", tint = colors.textPrimary)
                                }
                            }
                        },
                    )
                    LiveTvSummary(
                        state = state,
                        listMode = listMode,
                        onListModeChange = { listMode = it },
                        modifier = Modifier.padding(horizontal = ScreenGutter),
                    )
                    Spacer(Modifier.height(10.dp))
                    NuvioInputField(
                        value = query,
                        onValueChange = { query = it.take(80) },
                        placeholder = "Buscar canais...",
                        shape = RoundedCornerShape(NuvioTokens.Radius.xl),
                        modifier = Modifier.padding(horizontal = ScreenGutter),
                        trailingContent = if (query.isNotBlank()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Limpar busca", tint = colors.textSecondary)
                                }
                            }
                        } else {
                            null
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = ScreenGutter),
                    ) {
                        item(key = "all") {
                            LiveFilterChip("Todos", filter == LiveTvFilter.All) { filter = LiveTvFilter.All }
                        }
                        item(key = "favorites") {
                            LiveFilterChip("★ Favoritos", filter == LiveTvFilter.Favorites) { filter = LiveTvFilter.Favorites }
                        }
                        items(state.addonNames, key = { "addon:$it" }) { name ->
                            LiveFilterChip(name, filter == LiveTvFilter.Addon(name)) { filter = LiveTvFilter.Addon(name) }
                        }
                    }
                }
            }

            when {
                state.totalSources == 0 && state.loaded -> item(key = "empty-sources") {
                    LiveTvMessage(
                        title = "Nenhum canal liberado",
                        text = "Ainda não há addons ao vivo liberados para o seu plano. Quando o administrador liberar, os canais aparecem aqui.",
                    )
                }
                !state.loaded && state.channels.isEmpty() -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                }
                state.channels.isEmpty() -> item(key = "failed") {
                    LiveTvMessage(
                        title = "Não foi possível carregar os canais",
                        text = "Os addons ao vivo não responderam. Verifique sua conexão e toque em atualizar.",
                    )
                }
                visible.isEmpty() -> item(key = "no-match") {
                    val noFavorites = filter == LiveTvFilter.Favorites && query.isBlank()
                    LiveTvMessage(
                        title = if (noFavorites) "Nenhum favorito ainda" else "Nenhum canal encontrado",
                        text = if (noFavorites) "Toque na estrela de um canal para adicioná-lo aos favoritos." else "Tente buscar por outro nome.",
                    )
                }
                else -> visible.forEach { group ->
                    item(key = "header:" + group.category) {
                        CategoryHeader(group.category, group.channels.size, Modifier.padding(horizontal = ScreenGutter))
                    }
                    if (listMode) {
                        items(group.channels, key = { "row:" + group.category + ":" + it.manifestUrl + it.favoriteKey }) { channel ->
                            ChannelRow(
                                channel = channel,
                                favorite = channel.favoriteKey in favorites,
                                onClick = { selected = channel },
                                onToggleFavorite = { toggleFavorite(channel) },
                                modifier = Modifier.padding(horizontal = ScreenGutter),
                            )
                        }
                    } else {
                        val rows = group.channels.chunked(columns)
                        items(rows.size, key = { "cards:" + group.category + ":" + it }) { index ->
                            Row(
                                modifier = Modifier.padding(horizontal = ScreenGutter),
                                horizontalArrangement = Arrangement.spacedBy(CardGap),
                            ) {
                                rows[index].forEach { channel ->
                                    ChannelCard(
                                        channel = channel,
                                        favorite = channel.favoriteKey in favorites,
                                        onClick = { selected = channel },
                                        onToggleFavorite = { toggleFavorite(channel) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - rows[index].size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }

    val current = selected
    if (current != null) {
        Dialog(onDismissRequest = { selected = null }) {
            ChannelPanel(
                channel = current,
                favorite = current.favoriteKey in favorites,
                onClose = { selected = null },
                onPlay = {
                    selected = null
                    onPlay(current.meta)
                },
                onToggleFavorite = { toggleFavorite(current) },
                modifier = Modifier.widthIn(max = 380.dp),
            )
        }
    }
}

@Composable
private fun LiveTvSummary(
    state: LiveTvUiState,
    listMode: Boolean,
    onListModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nuvio.colors
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LiveBadge()
        Spacer(Modifier.width(10.dp))
        val summary = buildString {
            append(if (state.channels.size == 1) "1 canal" else "${state.channels.size} canais")
            if (state.totalSources > 0) {
                append(" · Addons ")
                append(state.totalSources - state.failedSources)
                append('/')
                append(state.totalSources)
            }
        }
        Text(
            text = summary,
            color = colors.textSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ViewModeToggle(listMode = listMode, onChange = onListModeChange)
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(LiveRed)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
        Spacer(Modifier.width(5.dp))
        Text("AO VIVO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ViewModeToggle(listMode: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.nuvio.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceCard)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        ToggleOption("Cartões", Icons.Rounded.GridView, selected = !listMode) { onChange(false) }
        ToggleOption("Lista", Icons.Rounded.ViewList, selected = listMode) { onChange(true) }
    }
}

@Composable
private fun ToggleOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.nuvio.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) colors.textPrimary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) colors.background else colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun LiveFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.nuvio.colors
    Text(
        text = label,
        color = if (selected) colors.background else colors.textPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.textPrimary else colors.surfaceCard)
            .border(1.dp, if (selected) Color.Transparent else colors.borderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun CategoryHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.nuvio.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 8.dp)) {
        Text(
            title,
            color = colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            count.toString(),
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceCard)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ChannelImage(channel: LiveChannel, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.nuvio.colors
    Box(modifier = modifier.background(colors.surface), contentAlignment = Alignment.Center) {
        val url = channel.imageUrl
        if (url.isNullOrBlank()) {
            Text(
                channel.meta.name.take(2).uppercase(),
                color = colors.textSecondary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = channel.meta.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(10.dp),
            )
        }
    }
}

@Composable
private fun FavoriteButton(favorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.nuvio.colors
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
            tint = if (favorite) FavoriteGold else colors.textPrimary,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun ChannelCard(
    channel: LiveChannel,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nuvio.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceCard)
            .border(1.dp, colors.borderSubtle, shape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            ChannelImage(channel, Modifier.fillMaxSize())
            FavoriteButton(favorite, onToggleFavorite, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(channel.meta.name, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(channel.addonName, color = colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChannelRow(
    channel: LiveChannel,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nuvio.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceCard)
            .border(1.dp, colors.borderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelImage(channel, Modifier.width(88.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.meta.name, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${channel.addonName} • ${channel.category}", color = colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FavoriteButton(favorite, onToggleFavorite)
    }
}

@Composable
private fun ChannelPanel(
    channel: LiveChannel,
    favorite: Boolean,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nuvio.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiveBadge()
            Spacer(Modifier.width(8.dp))
            Text("Canal selecionado", color = colors.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = colors.textPrimary)
            }
        }
        Spacer(Modifier.height(4.dp))
        ChannelImage(channel, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.height(14.dp))
        Text(channel.meta.name, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("${channel.addonName} • ${channel.category}", color = colors.textSecondary, fontSize = 13.sp)
        val description = channel.meta.description?.trim().orEmpty()
        if (description.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Text(description, color = colors.textSecondary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onPlay,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Assistir", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onToggleFavorite,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Icon(
                imageVector = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = null,
                tint = if (favorite) FavoriteGold else colors.textPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(if (favorite) "Nos favoritos" else "Adicionar aos favoritos", color = colors.textPrimary)
        }
    }
}

@Composable
private fun LiveTvMessage(title: String, text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.nuvio.colors
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 280.dp).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(text, color = colors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
        }
    }
}
