package com.opencontacts.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val retentionOptions = listOf(7, 30, 365, 3650)
private fun retentionLabel(days: Int): String = when (days) {
    7 -> "1 Week"
    30 -> "1 Month"
    365 -> "1 Year"
    else -> "Lifetime"
}

@Composable
fun PreferencesRoute(
    onBack: () -> Unit,
    appViewModel: AppViewModel,
) {
    val settings by appViewModel.appLockSettings.collectAsStateWithLifecycle()
    var sliderValue by remember(settings.trashRetentionDays) {
        mutableFloatStateOf(retentionOptions.indexOfFirst { it >= settings.trashRetentionDays }.coerceAtLeast(0).toFloat())
    }
    val retentionDays = retentionOptions[sliderValue.toInt().coerceIn(retentionOptions.indices)]

    SettingsScaffold(title = "Preferences", onBack = onBack) { modifier ->
        LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsSection(title = "Trash retention", subtitle = "Choose how long deleted contacts stay in the trash before permanent removal.") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { appViewModel.setTrashRetentionDays(retentionDays) },
                            valueRange = 0f..3f,
                            steps = 2,
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            retentionOptions.forEach { option ->
                                Text(retentionLabel(option), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            "Deleted contacts will be kept for ${retentionLabel(retentionDays).lowercase()} before permanent removal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Contact list") {
                    SettingsChoiceRow(
                        title = "Default sort order",
                        subtitle = "Choose how contacts are ordered when no search query is active.",
                        selected = settings.defaultContactSortOrder,
                        choices = listOf("FIRST_NAME", "LAST_NAME", "RECENTLY_ADDED", "MOST_CONTACTED"),
                        onSelect = appViewModel::setDefaultContactSortOrder,
                    )
                    SettingsSpacer()
                    SettingsChoiceRow(
                        title = "List density",
                        subtitle = "Compact fits more contacts on screen. Comfortable keeps larger spacing.",
                        selected = settings.contactListDensity,
                        choices = listOf("COMPACT", "COMFORTABLE"),
                        onSelect = appViewModel::setContactListDensity,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Show contact photos in list",
                        subtitle = "Hide photos for a cleaner and lighter scrolling experience.",
                        checked = settings.showContactPhotosInList,
                        onCheckedChange = appViewModel::setShowContactPhotosInList,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Show favorites first",
                        subtitle = "Keep favorite contacts near the top of the contacts tab.",
                        checked = settings.showFavoritesFirst,
                        onCheckedChange = appViewModel::setShowFavoritesFirst,
                    )
                }
            }
            item {
                SettingsSection(title = "Behavior") {
                    SettingsChoiceRow(
                        title = "Default start tab",
                        subtitle = "Choose what opens first on the home screen.",
                        selected = settings.defaultStartTab,
                        choices = listOf("CONTACTS", "CALL_LOG"),
                        onSelect = appViewModel::setDefaultStartTab,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Open contact directly on tap",
                        subtitle = "When disabled, tapping during selection mode only adjusts selection.",
                        checked = settings.openContactDirectlyOnTap,
                        onCheckedChange = appViewModel::setOpenContactDirectlyOnTap,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Show recent calls preview",
                        subtitle = "Display recent call entries inside the contact details page.",
                        checked = settings.showRecentCallsPreview,
                        onCheckedChange = appViewModel::setShowRecentCallsPreview,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Auto-collapse call groups",
                        subtitle = "Keep grouped call history compact unless expanded.",
                        checked = settings.autoCollapseCallGroups,
                        onCheckedChange = appViewModel::setAutoCollapseCallGroups,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Show blocked contacts in search",
                        subtitle = "Hide blocked contacts from inline search results when disabled.",
                        checked = settings.showBlockedContactsInSearch,
                        onCheckedChange = appViewModel::setShowBlockedContactsInSearch,
                    )
                }
            }
            item {
                SettingsSection(title = "Safety and export") {
                    SettingsSwitchRow(
                        title = "Confirm before delete",
                        subtitle = "Ask before deleting a contact or multiple selected contacts.",
                        checked = settings.confirmBeforeDelete,
                        onCheckedChange = appViewModel::setConfirmBeforeDelete,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Confirm before block/unblock",
                        subtitle = "Require confirmation before changing blocked state.",
                        checked = settings.confirmBeforeBlockUnblock,
                        onCheckedChange = appViewModel::setConfirmBeforeBlockUnblock,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Include timestamp in exported filenames",
                        subtitle = "Safer for repeated exports and easier to trace later.",
                        checked = settings.includeTimestampInExportFileName,
                        onCheckedChange = appViewModel::setIncludeTimestampInExportFileName,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Hide empty folders and tags",
                        subtitle = "Keep lists cleaner by hiding classifications with no contacts.",
                        checked = settings.hideEmptyFoldersAndTags,
                        onCheckedChange = appViewModel::setHideEmptyFoldersAndTags,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Haptic feedback",
                        subtitle = "Use subtle vibration for key actions such as quick toggles and keypad interactions.",
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = appViewModel::setHapticFeedbackEnabled,
                    )
                }
            }
        }
    }
}
