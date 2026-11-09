package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Tune
import com.nuvio.app.core.build.AppFeaturePolicy
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Settings home, laid out like the WATCHFIRST Windows app: Conta, Geral, Sobre and Avançadas.
 * As on the PC, content discovery, integrations, tracking and Watch Party stay out: the addons come
 * from the member's plan. On tablets the sidebar shows one section at a time ([category]).
 */
internal fun LazyListScope.settingsRootContent(
    isTablet: Boolean,
    onPageClick: (SettingsPage) -> Unit,
    onSwitchProfileClick: (() -> Unit)? = null,
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    category: SettingsCategory? = null,
) {
    fun shows(section: SettingsCategory) = category == null || category == section

    if (shows(SettingsCategory.Account) && onSwitchProfileClick != null) {
        item(key = "settings-account") {
            SettingsSection(
                title = stringResource(Res.string.compose_settings_root_account_section),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_root_switch_profile_title),
                        description = stringResource(Res.string.compose_settings_root_switch_profile_description),
                        icon = Icons.Rounded.People,
                        isTablet = isTablet,
                        onClick = onSwitchProfileClick,
                    )
                }
            }
        }
    }
    if (shows(SettingsCategory.General)) {
        item(key = "settings-general") {
            SettingsSection(
                title = stringResource(Res.string.compose_settings_root_general_section),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_appearance),
                        description = stringResource(Res.string.compose_settings_root_appearance_description),
                        icon = Icons.Rounded.Palette,
                        isTablet = isTablet,
                        onClick = { onPageClick(SettingsPage.Layout) },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_playback),
                        description = stringResource(Res.string.settings_playback_subtitle),
                        icon = SettingsPage.Playback.icon,
                        isTablet = isTablet,
                        onClick = { onPageClick(SettingsPage.Playback) },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_notifications),
                        description = stringResource(Res.string.compose_settings_root_notifications_description),
                        icon = SettingsPage.Notifications.icon,
                        isTablet = isTablet,
                        onClick = { onPageClick(SettingsPage.Notifications) },
                    )
                }
            }
        }
    }
    if (shows(SettingsCategory.About)) {
        aboutSettingsSection(
            isTablet = isTablet,
            onSupportersContributorsClick = { onPageClick(SettingsPage.SupportersContributors) },
            onLicensesAttributionsClick = { onPageClick(SettingsPage.LicensesAttributions) },
            onCheckForUpdatesClick = onCheckForUpdatesClick,
            onTestUpdateBannerClick = onTestUpdateBannerClick,
            showSupportersContributorsPage = AppFeaturePolicy.supportersContributorsPageEnabled,
        )
    }
    if (shows(SettingsCategory.Advanced)) {
        item(key = "settings-advanced") {
            SettingsSection(
                title = stringResource(Res.string.compose_settings_root_advanced_section),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_advanced),
                        description = stringResource(Res.string.compose_settings_root_advanced_description),
                        icon = Icons.Rounded.Tune,
                        isTablet = isTablet,
                        onClick = { onPageClick(SettingsPage.Advanced) },
                    )
                }
            }
        }
    }
    settingsFooterContent(isTablet = isTablet)
}

/** AMOLED, app icon and language sit inside Layout, the single display entry the PC app has. */
internal fun LazyListScope.layoutDisplayEntry(
    isTablet: Boolean,
    onAppearanceClick: () -> Unit,
) {
    item(key = "layout-display") {
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_display),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.settings_appearance_title),
                    description = stringResource(Res.string.settings_appearance_description),
                    icon = SettingsPage.Appearance.icon,
                    isTablet = isTablet,
                    onClick = onAppearanceClick,
                )
            }
        }
    }
}
