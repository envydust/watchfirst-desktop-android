package com.nuvio.app.features.settings

import com.nuvio.app.features.player.AndroidPlaybackEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsNavigationTest {
    @Test
    fun layoutChildrenReturnToLayout() {
        listOf(
            SettingsPage.Homescreen,
            SettingsPage.MetaScreen,
            SettingsPage.ContinueWatching,
            SettingsPage.PosterCustomization,
            SettingsPage.Streams,
        ).forEach { page ->
            assertEquals(SettingsPage.Layout, page.previousPage())
            assertEquals(SettingsCategory.General, page.category)
        }
        assertEquals(SettingsPage.Root, SettingsPage.Layout.previousPage())
        assertEquals(SettingsPage.Layout, SettingsPage.Appearance.previousPage())
    }

    @Test
    fun playbackChildrenReturnToPlaybackInTheGeneralGroup() {
        PlaybackSection.entries.forEach { section ->
            assertEquals(SettingsPage.Playback, section.page.previousPage())
            assertEquals(SettingsCategory.General, section.page.category)
        }
        assertEquals(SettingsPage.Root, SettingsPage.Playback.previousPage())
    }

    @Test
    fun bufferOptionsFollowThePlatformAndPlaybackEngine() {
        AndroidPlaybackEngine.entries.forEach { engine ->
            val android = visiblePlaybackSections(ios = false, engine = engine, p2pVisible = false)
            val ios = visiblePlaybackSections(ios = true, engine = engine, p2pVisible = false)
            assertEquals(engine != AndroidPlaybackEngine.Libmpv, PlaybackSection.Buffer in android)
            assertFalse(PlaybackSection.Buffer in ios)
            assertFalse(PlaybackSection.P2p in android)
            assertFalse(PlaybackSection.P2p in ios)
            assertTrue(PlaybackSection.Interface in android)
            assertTrue(PlaybackSection.Interface in ios)
        }
        assertTrue(PlaybackSection.P2p in visiblePlaybackSections(false, AndroidPlaybackEngine.Auto, true))
    }

    @Test
    fun savedTabletCategoriesRestoreToThePcGroups() {
        assertEquals(SettingsCategory.Account, settingsCategoryForName("Account"))
        assertEquals(SettingsCategory.General, settingsCategoryForName("General"))
        assertEquals(SettingsCategory.About, settingsCategoryForName("About"))
        assertEquals(SettingsCategory.Advanced, settingsCategoryForName("Advanced"))
        assertEquals(SettingsCategory.Account, settingsCategoryForName("You"))
        assertEquals(SettingsCategory.General, settingsCategoryForName("Look"))
        assertEquals(SettingsCategory.General, settingsCategoryForName("Services"))
        assertEquals(SettingsCategory.Advanced, settingsCategoryForName("System"))
        assertEquals(SettingsCategory.General, settingsCategoryForName("unknown"))
    }

    @Test
    fun aboutPagesStayInTheAboutGroup() {
        assertEquals(SettingsCategory.About, SettingsPage.About.category)
        assertEquals(SettingsCategory.About, SettingsPage.LicensesAttributions.category)
        assertEquals(SettingsCategory.Advanced, SettingsPage.Advanced.category)
    }
}
