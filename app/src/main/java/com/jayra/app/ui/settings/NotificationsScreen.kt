package com.jayra.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.NotificationSettings
import com.jayra.app.model.NotificationsUiState
import com.jayra.app.ui.components.InfoSettingRow
import com.jayra.app.ui.components.InfoSwitchRow
import com.jayra.app.ui.components.ShimmerCardList
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraOrange
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.ui.theme.JayraYellow
import com.jayra.app.ui.viewmodel.NotificationsViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationsContent(
        uiState = uiState,
        onBack = onBack,
        onPrivateToggle = viewModel::onPrivateToggle,
        onGroupToggle = viewModel::onGroupToggle,
        onChannelToggle = viewModel::onChannelToggle,
        onSoundChange = viewModel::onSoundChange,
        onVibrateChange = viewModel::onVibrateChange,
        onPriorityToggle = viewModel::onPriorityToggle,
        onDeletedToggle = viewModel::onDeletedToggle,
        onPreviewToggle = viewModel::onPreviewToggle
    )
}

@Composable
fun NotificationsContent(
    uiState: NotificationsUiState,
    onBack: () -> Unit,
    onPrivateToggle: (Boolean) -> Unit,
    onGroupToggle: (Boolean) -> Unit,
    onChannelToggle: (Boolean) -> Unit,
    onSoundChange: (String) -> Unit,
    onVibrateChange: (String) -> Unit,
    onPriorityToggle: (Boolean) -> Unit,
    onDeletedToggle: (Boolean) -> Unit,
    onPreviewToggle: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    var showSoundDialog by remember { mutableStateOf(false) }
    var showVibrateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("notifications_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Notifikasi & Suara",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is NotificationsUiState.Loading -> {
                    ShimmerCardList(cardCount = 3)
                }
                is NotificationsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = JayraDangerRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.message,
                            fontSize = 17.sp,
                            color = JayraSubtext
                        )
                    }
                }
                is NotificationsUiState.Success -> {
                    val settings = uiState.notifications
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp)
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + slideInVertically(tween(200))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Card 1: Pesan (Notifikasi Pribadi, Grup, Channel, Suara, Getar)
                                Column {
                                    Text(
                                        text = "Notifikasi untuk Obrolan",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JayraBlue,
                                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cardBg)
                                    ) {
                                        InfoSwitchRow(
                                            icon = Icons.Default.Chat,
                                            iconTint = JayraBlue,
                                            title = "Notifikasi Pribadi",
                                            checked = settings.isPrivateChatNotificationsEnabled,
                                            onCheckedChange = onPrivateToggle,
                                            showSeparator = true
                                        )
                                        InfoSwitchRow(
                                            icon = Icons.Default.Group,
                                            iconTint = JayraOrange,
                                            title = "Notifikasi Grup",
                                            checked = settings.isGroupNotificationsEnabled,
                                            onCheckedChange = onGroupToggle,
                                            showSeparator = true
                                        )
                                        InfoSwitchRow(
                                            icon = Icons.Default.Campaign,
                                            iconTint = JayraSuccessGreen,
                                            title = "Notifikasi Channel",
                                            checked = settings.isChannelNotificationsEnabled,
                                            onCheckedChange = onChannelToggle,
                                            showSeparator = true
                                        )
                                        InfoSettingRow(
                                            icon = Icons.Default.MusicNote,
                                            iconTint = JayraYellow,
                                            title = "Suara",
                                            valueText = settings.sound,
                                            onClick = { showSoundDialog = true },
                                            showSeparator = true
                                        )
                                        InfoSettingRow(
                                            icon = Icons.Default.Vibration,
                                            iconTint = JayraSubtext,
                                            title = "Getar",
                                            valueText = settings.vibrate,
                                            onClick = { showVibrateDialog = true },
                                            showSeparator = false
                                        )
                                    }
                                }

                                // Card 2: Lainnya (Pesan Prioritas, Pesan Dihapus, Pratinjau Pesan)
                                Column {
                                    Text(
                                        text = "Lainnya",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JayraBlue,
                                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cardBg)
                                    ) {
                                        InfoSwitchRow(
                                            icon = Icons.Default.PriorityHigh,
                                            iconTint = JayraOrange,
                                            title = "Pesan Prioritas",
                                            checked = settings.isPriorityMessageEnabled,
                                            onCheckedChange = onPriorityToggle,
                                            showSeparator = true
                                        )
                                        InfoSwitchRow(
                                            icon = Icons.Default.DeleteSweep,
                                            iconTint = JayraDangerRed,
                                            title = "Pesan Dihapus",
                                            checked = settings.isDeletedMessageNoticeEnabled,
                                            onCheckedChange = onDeletedToggle,
                                            showSeparator = true
                                        )
                                        InfoSwitchRow(
                                            icon = Icons.Default.Visibility,
                                            iconTint = JayraBlue,
                                            title = "Pratinjau Pesan",
                                            checked = settings.isMessagePreviewEnabled,
                                            onCheckedChange = onPreviewToggle,
                                            showSeparator = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sound Choice Dialog
    if (showSoundDialog) {
        val sounds = listOf("Default", "Chime", "Chord", "Jayra Note", "Silent")
        val currentSound = (uiState as? NotificationsUiState.Success)?.notifications?.sound ?: "Default"
        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            title = { Text("Pilih Suara Notifikasi") },
            text = {
                Column {
                    sounds.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSoundChange(s)
                                    showSoundDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (s == currentSound),
                                onClick = {
                                    onSoundChange(s)
                                    showSoundDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = JayraBlue)
                            )
                            Text(text = s, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSoundDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Vibrate Choice Dialog
    if (showVibrateDialog) {
        val vibrates = listOf("Default", "Pendek", "Panjang", "Mati")
        val currentVibrate = (uiState as? NotificationsUiState.Success)?.notifications?.vibrate ?: "Default"
        AlertDialog(
            onDismissRequest = { showVibrateDialog = false },
            title = { Text("Pola Getar") },
            text = {
                Column {
                    vibrates.forEach { v ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onVibrateChange(v)
                                    showVibrateDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (v == currentVibrate),
                                onClick = {
                                    onVibrateChange(v)
                                    showVibrateDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = JayraBlue)
                            )
                            Text(text = v, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVibrateDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Preview(name = "Notifications Light", showBackground = true)
@Composable
private fun NotificationsLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            NotificationsContent(
                uiState = NotificationsUiState.Success(
                    NotificationSettings(
                        isPrivateChatNotificationsEnabled = true,
                        isGroupNotificationsEnabled = true,
                        isChannelNotificationsEnabled = true,
                        sound = "Default",
                        vibrate = "Default",
                        isPriorityMessageEnabled = true,
                        isDeletedMessageNoticeEnabled = false,
                        isMessagePreviewEnabled = true
                    )
                ),
                onBack = {},
                onPrivateToggle = {},
                onGroupToggle = {},
                onChannelToggle = {},
                onSoundChange = {},
                onVibrateChange = {},
                onPriorityToggle = {},
                onDeletedToggle = {},
                onPreviewToggle = {}
            )
        }
    }
}

@Preview(name = "Notifications Dark", showBackground = true)
@Composable
private fun NotificationsDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            NotificationsContent(
                uiState = NotificationsUiState.Success(
                    NotificationSettings(
                        isPrivateChatNotificationsEnabled = false,
                        isGroupNotificationsEnabled = true,
                        isChannelNotificationsEnabled = false,
                        sound = "Chime",
                        vibrate = "Pendek",
                        isPriorityMessageEnabled = false,
                        isDeletedMessageNoticeEnabled = true,
                        isMessagePreviewEnabled = true
                    )
                ),
                onBack = {},
                onPrivateToggle = {},
                onGroupToggle = {},
                onChannelToggle = {},
                onSoundChange = {},
                onVibrateChange = {},
                onPriorityToggle = {},
                onDeletedToggle = {},
                onPreviewToggle = {}
            )
        }
    }
}
