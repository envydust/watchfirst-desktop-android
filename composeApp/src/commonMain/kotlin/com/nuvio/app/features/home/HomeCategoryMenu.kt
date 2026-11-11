package com.nuvio.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.catalog.CatalogTarget
import kotlinx.coroutines.launch

// WATCHFIRST (member feedback): a side menu with every Home category, grouped as Filmes, Séries and
// Animes, so a list far down the Home screen opens with one tap instead of a long scroll.

internal enum class HomeMenuGroup(val title: String, val icon: ImageVector) {
    Movies("Filmes", Icons.Rounded.Movie),
    Series("Séries", Icons.Rounded.Tv),
    Anime("Animes", Icons.Rounded.AutoAwesome),
    Other("Outras listas", Icons.AutoMirrored.Rounded.ViewList),
}

private val ANIME_HINTS = listOf("anime", "crunchyroll", "kitsu", "anilist", "myanimelist")

/** Animes by catalog type or by name (Crunchyroll, Kitsu…); otherwise by the catalog's content type. */
internal fun HomeCatalogSection.menuGroup(): HomeMenuGroup {
    val addonTarget = target as? CatalogTarget.Addon
    val text = listOf(title, subtitle, addonName, addonTarget?.catalogId.orEmpty(), addonTarget?.manifestUrl.orEmpty())
        .joinToString(" ")
        .lowercase()
    val type = target.contentType.lowercase()
    if (type == "anime" || ANIME_HINTS.any { it in text }) return HomeMenuGroup.Anime
    return when (type) {
        "movie", "movies", "film" -> HomeMenuGroup.Movies
        "series", "tv", "show", "tvshow" -> HomeMenuGroup.Series
        else -> HomeMenuGroup.Other
    }
}

/** The Home rows that are on screen, in Home order, grouped for the menu. */
internal fun groupHomeMenuSections(
    sections: List<HomeCatalogSection>,
    orderedKeys: List<String>,
): List<Pair<HomeMenuGroup, List<HomeCatalogSection>>> {
    val visible = sections.filter { it.items.isNotEmpty() }
    val byKey = visible.associateBy { it.key }
    val ordered = orderedKeys.mapNotNull { byKey[it] } + visible.filter { it.key !in orderedKeys }
    return HomeMenuGroup.entries.mapNotNull { group ->
        ordered.filter { it.menuGroup() == group }.takeIf { it.isNotEmpty() }?.let { group to it }
    }
}

/** Home with a ☰ button that opens the category menu. */
@Composable
internal fun HomeCategoryMenuHost(
    onCatalogClick: ((HomeCatalogSection) -> Unit)?,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    PlatformBackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swiping is only for closing: the Home rows scroll sideways and must keep their gestures.
        gesturesEnabled = drawerState.isOpen,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        drawerContent = {
            HomeCategoryMenuSheet(
                onClose = { scope.launch { drawerState.close() } },
                onSelect = { section ->
                    scope.launch { drawerState.close() }
                    onCatalogClick?.invoke(section)
                },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { scope.launch { drawerState.open() } },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Abrir menu de categorias",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeCategoryMenuSheet(
    onClose: () -> Unit,
    onSelect: (HomeCatalogSection) -> Unit,
) {
    val colors = MaterialTheme.nuvio.colors
    val homeUiState by HomeRepository.uiState.collectAsStateWithLifecycle()
    val settingsUiState by HomeCatalogSettingsRepository.uiState.collectAsStateWithLifecycle()
    val groups = remember(homeUiState.sections, settingsUiState.items) {
        groupHomeMenuSections(
            sections = homeUiState.sections,
            orderedKeys = settingsUiState.items.filter { it.enabled && !it.isCollection }.map { it.key },
        )
    }

    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 320.dp).fillMaxHeight(),
        drawerContainerColor = colors.background,
        drawerContentColor = colors.textPrimary,
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
    ) {
        Row(
            // The sheet already keeps clear of the status bar.
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Categorias",
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Toque para abrir a lista completa",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Fechar menu",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (groups.isEmpty()) {
            Text(
                text = "As categorias aparecem aqui assim que o Início terminar de carregar.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(20.dp),
            )
        } else LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Room for the floating bottom bar, which stays above the menu.
            contentPadding = PaddingValues(bottom = 140.dp),
        ) {
            groups.forEach { (group, sections) ->
                item(key = "group:${group.name}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = group.icon,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = group.title,
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = sections.size.toString(),
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
                items(sections, key = { "section:${group.name}:${it.key}" }) { section ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(section) }
                            .padding(start = 50.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = section.title,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            item(key = "footer") { Spacer(Modifier.height(8.dp)) }
        }
    }
}
