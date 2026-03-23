package com.opencontacts.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NotificationsIncomingCallsRoute(
    onBack: () -> Unit,
    appViewModel: AppViewModel,
) {
    val settings by appViewModel.appLockSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScaffold(title = "Notifications & Incoming Calls", onBack = onBack) { modifier ->
        LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsSection(title = "Incoming caller popup", subtitle = "Show caller information inline when the app is open, and use a high-priority call notification when it is not.") {
                    SettingsSwitchRow(
                        title = "Enable incoming caller popup",
                        subtitle = "Shows caller name, number, tags, folder, and quick actions.",
                        checked = settings.enableIncomingCallerPopup,
                        onCheckedChange = appViewModel::setEnableIncomingCallerPopup,
                    )
                    SettingsSpacer()
                    SettingsChoiceRow(
                        title = "Popup delivery mode",
                        subtitle = "Use the safer compatible fallback when the app is backgrounded.",
                        selected = settings.overlayPopupMode,
                        choices = listOf("IN_APP_ONLY", "HEADS_UP", "FULL_SCREEN"),
                        onSelect = appViewModel::setOverlayPopupMode,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Heads-up notifications",
                        subtitle = "Keep urgent incoming call notifications visible above normal notifications.",
                        checked = settings.headsUpNotifications,
                        onCheckedChange = appViewModel::setHeadsUpNotifications,
                    )
                }
            }
            item {
                SettingsSection(title = "Missed calls and content privacy") {
                    SettingsSwitchRow(
                        title = "Enable missed call notification",
                        subtitle = "Post a compact missed-call notification with quick actions.",
                        checked = settings.enableMissedCallNotification,
                        onCheckedChange = appViewModel::setEnableMissedCallNotification,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Show contact photo",
                        subtitle = "Add the contact photo to notifications when available.",
                        checked = settings.showPhotoInNotifications,
                        onCheckedChange = appViewModel::setShowPhotoInNotifications,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Show folder and tags",
                        subtitle = "Include classification context inside incoming and missed call surfaces.",
                        checked = settings.showFolderTagsInNotifications,
                        onCheckedChange = appViewModel::setShowFolderTagsInNotifications,
                    )
                    SettingsSpacer()
                    SettingsChoiceRow(
                        title = "Lock-screen visibility",
                        subtitle = "Choose whether secure lock screens show full caller details or hide sensitive information.",
                        selected = settings.lockScreenNotificationVisibility,
                        choices = listOf("SHOW_FULL", "HIDE_SENSITIVE"),
                        onSelect = appViewModel::setLockScreenNotificationVisibility,
                    )
                }
            }
            item {
                SettingsSection(title = "Sound and vibration") {
                    SettingsSwitchRow(
                        title = "Sound",
                        subtitle = "Play app notification sounds when the system allows it.",
                        checked = settings.soundEnabled,
                        onCheckedChange = appViewModel::setSoundEnabled,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Vibration",
                        subtitle = "Use vibration for missed-call and incoming-call attention.",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = appViewModel::setVibrationEnabled,
                    )
                }
            }
            item {
                SettingsSection(title = "Permissions and system controls") {
                    Text(
                        "Incoming call notifications rely on phone-state access and notification channels. Full-screen delivery depends on device policy and Android version.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SettingsSpacer()
                    androidx.compose.material3.TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        })
                    }) { Text("Open notification settings") }
                    androidx.compose.material3.TextButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        }
                    }) { Text("Open overlay permission") }
                }
            }
        }
    }
}
