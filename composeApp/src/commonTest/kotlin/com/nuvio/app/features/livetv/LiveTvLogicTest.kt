package com.nuvio.app.features.livetv

import com.nuvio.app.features.access.WatchfirstAccessState
import com.nuvio.app.features.access.watchfirstAddonUrlKey
import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonExtraProperty
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.home.MetaPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveTvLogicTest {
    @Test fun catalogWorkersStayBoundedAndKeepResultOrder() = kotlinx.coroutines.runBlocking {
        var active = 0
        var peak = 0
        val result = boundedCatalogMap((0 until 100).toList(), 4) {
            active++
            peak = maxOf(peak, active)
            kotlinx.coroutines.delay(1)
            active--
            it * 2
        }
        assertEquals((0 until 100).map { it * 2 }, result)
        assertTrue(peak <= 4)
        assertEquals(0, active)
        assertTrue(boundedCatalogMap(emptyList<Int>(), 4) { it }.isEmpty())
    }

    private fun addon(url: String) = ManagedAddon(
        manifestUrl = url,
        manifest = AddonManifest(
            id = url, name = "Live", description = "", version = "1.0.0",
            resources = emptyList(), types = listOf("tv"), transportUrl = url,
        ),
    )

    private fun channel(name: String, category: String, addon: String = "Live", id: String = name) = LiveChannel(
        meta = MetaPreview(id = id, type = "tv", name = name),
        addonName = addon,
        manifestUrl = "https://service.test/$addon/manifest.json",
        category = category,
    )

    @Test fun onlyAddonsMarkedLiveAreUsedEvenWhenUrlFormatDiffers() {
        val live = addon("https://service.test/gateway/s/live/manifest.json")
        val vod = addon("https://service.test/gateway/s/vod/manifest.json")
        val pending = ManagedAddon(manifestUrl = "https://service.test/gateway/s/live2/manifest.json")
        val result = liveAddons(
            listOf(live, vod, pending),
            setOf("https://service.test/gateway/s/live/", "https://service.test/gateway/s/live2/manifest.json"),
        )
        assertEquals(listOf(live), result)
    }

    @Test fun liveTabIsOnlyEnabledForSignedInMaxMembers() {
        assertTrue(WatchfirstAccessState(granted = true, plan = "max").liveTvEnabled)
        assertFalse(WatchfirstAccessState(granted = true, plan = "basic").liveTvEnabled)
        assertFalse(WatchfirstAccessState(granted = false, plan = "max").liveTvEnabled)
    }

    @Test fun addonUrlKeyIgnoresManifestSuffixSlashAndCase() {
        assertEquals(
            watchfirstAddonUrlKey("https://Max.watchfirst.com.br/gateway/t/live/"),
            watchfirstAddonUrlKey("https://max.watchfirst.com.br/gateway/t/live/manifest.json?x=1"),
        )
    }

    @Test fun catalogsNeedingSearchAreSkippedAndRequiredGenresAreExpanded() {
        val plain = AddonCatalog(type = "tv", id = "all", name = "Todos")
        val search = AddonCatalog(type = "tv", id = "search", name = "Busca", extra = listOf(AddonExtraProperty("search", isRequired = true)))
        val genres = AddonCatalog(
            type = "tv", id = "cat", name = "Categorias",
            extra = listOf(AddonExtraProperty("genre", isRequired = true, options = listOf("Esportes", "Filmes"))),
        )
        val requests = liveCatalogRequests(listOf(plain, search, genres))
        assertEquals(listOf(null, "Esportes", "Filmes"), requests.map { it.genre })
        assertEquals(listOf("all", "cat", "cat"), requests.map { it.catalog.id })
    }

    @Test fun categoryPrefersRequestedGenreThenMetaGenreThenCatalogName() {
        val catalog = AddonCatalog(type = "tv", id = "all", name = "Todos os canais")
        val withGenre = MetaPreview(id = "1", type = "tv", name = "A", genres = listOf("Notícias"))
        val plain = MetaPreview(id = "2", type = "tv", name = "B")
        assertEquals("Esportes", liveChannelCategory(withGenre, LiveCatalogRequest(catalog, "Esportes")))
        assertEquals("Notícias", liveChannelCategory(withGenre, LiveCatalogRequest(catalog, null)))
        assertEquals("Todos os canais", liveChannelCategory(plain, LiveCatalogRequest(catalog, null)))
    }

    @Test fun searchFavoritesAndAddonFiltersCombine() {
        val channels = listOf(
            channel("Premiere 1", "Esportes"),
            channel("Premiere 2", "Esportes", addon = "Outro"),
            channel("Desenhos", "Infantil"),
        )
        assertEquals(2, filterLiveChannels(channels, "premiere", LiveTvFilter.All, emptySet()).size)
        assertEquals(1, filterLiveChannels(channels, "infantil", LiveTvFilter.All, emptySet()).size)
        assertEquals(listOf("Premiere 2"), filterLiveChannels(channels, "", LiveTvFilter.Addon("Outro"), emptySet()).map { it.meta.name })
        assertEquals(listOf("Desenhos"), filterLiveChannels(channels, "", LiveTvFilter.Favorites, setOf("tv:Desenhos")).map { it.meta.name })
    }

    @Test fun groupsKeepFirstSeenCategoryOrder() {
        val groups = groupLiveChannels(
            listOf(channel("A", "Esportes"), channel("B", "Filmes"), channel("C", "Esportes")),
        )
        assertEquals(listOf("Esportes", "Filmes"), groups.map { it.category })
        assertEquals(listOf("A", "C"), groups.first().channels.map { it.meta.name })
    }
}
