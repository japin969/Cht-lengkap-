package com.example.ui.channelinfo

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Channel
import com.example.model.ChannelInfoUiState
import com.example.ui.components.InfoActionGrid
import com.example.ui.components.InfoProfileHeader
import com.example.ui.components.InfoSettingRow
import com.example.ui.components.InfoSwitchRow
import com.example.ui.components.ShimmerCardList
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramOrange
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramSuccessGreen
import com.example.ui.theme.TelegramTheme
import com.example.ui.theme.TelegramYellow
import com.example.ui.viewmodel.ChannelInfoViewModel

@Composable
fun ChannelInfoScreen(
    channelId: String,
    onBack: () -> Unit,
    onSearchClick: (String) -> Unit = {},
    onMediaClick: (String) -> Unit = {},
    onStarredClick: (String) -> Unit = {},
    onDisappearingClick: (String) -> Unit = {},
    viewModel: ChannelInfoViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ChannelInfoContent(
        uiState = uiState,
        onBack = onBack,
        onAudioClick = { viewModel.onAudioClick(context) },
        onVideoClick = { viewModel.onVideoClick(context) },
        onSearchClick = { onSearchClick(channelId) },
        onMediaClick = { onMediaClick(channelId) },
        onStarredClick = { onStarredClick(channelId) },
        onMuteToggle = viewModel::onMuteToggle,
        onCustomNotificationToggle = viewModel::onCustomNotificationToggle,
        onDisappearingClick = { onDisappearingClick(channelId) },
        onSignaturesToggle = viewModel::onSignaturesToggle,
        onLeaveChannel = { viewModel.onLeaveChannel("user_telegram_me", onBack) },
        onDeleteChannel = { viewModel.onDeleteChannel(onBack) },
        onRetry = viewModel::onRetry
    )
}

@Composable
fun ChannelInfoContent(
    uiState: ChannelInfoUiState,
    onBack: () -> Unit,
    onAudioClick: () -> Unit,
    onVideoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMediaClick: () -> Unit,
    onStarredClick: () -> Unit,
    onMuteToggle: (Boolean) -> Unit,
    onCustomNotificationToggle: (Boolean) -> Unit,
    onDisappearingClick: () -> Unit,
    onSignaturesToggle: (Boolean) -> Unit,
    onLeaveChannel: () -> Unit,
    onDeleteChannel: () -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val cardBg = if (isDark) TelegramCardDark else TelegramCardLight

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("channel_info_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Info Channel",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                val isAdmin = (uiState as? ChannelInfoUiState.Success)?.channel?.isAdmin == true
                if (isAdmin) {
                    TextButton(onClick = { /* Edit channel info */ }) {
                        Text(
                            text = "Edit",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = TelegramBlue
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
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
                is ChannelInfoUiState.Loading -> {
                    ShimmerCardList(cardCount = 5)
                }
                is ChannelInfoUiState.Error -> {
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
                            tint = TelegramDangerRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.message,
                            fontSize = 17.sp,
                            color = TelegramSubtext
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                is ChannelInfoUiState.Success -> {
                    val channel = uiState.channel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Profile Section
                        InfoProfileHeader(
                            name = channel.name,
                            subtitle = "Channel · ${formatSubscribers(channel.subscriberCount)} subscriber",
                            avatarUrl = channel.avatarUrl
                        )

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + slideInVertically(tween(200))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                // Action Grid
                                InfoActionGrid(
                                    action1Icon = Icons.Default.Call,
                                    action1Label = "audio",
                                    onAction1Click = onAudioClick,
                                    action2Icon = Icons.Default.Videocam,
                                    action2Label = "video",
                                    onAction2Click = onVideoClick,
                                    action3Icon = Icons.Default.Search,
                                    action3Label = "cari",
                                    onAction3Click = onSearchClick,
                                    modifier = Modifier.padding(top = 16.dp)
                                )

                                // Card Deskripsi
                                Box(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = channel.description,
                                            fontSize = 17.sp,
                                            lineHeight = 22.sp,
                                            color = if (isDark) Color.White else Color.Black,
                                            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (channel.description.length > 90) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (isDescriptionExpanded) "Lihat Lebih Sedikit" else "Lihat Selengkapnya",
                                                fontSize = 17.sp,
                                                color = TelegramBlue,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable {
                                                    isDescriptionExpanded = !isDescriptionExpanded
                                                }
                                            )
                                        }
                                    }
                                }

                                // Card Media & Bintang
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.Default.Photo,
                                        iconTint = TelegramBlue,
                                        title = "Media, Tautan, Dok",
                                        valueText = "${channel.mediaCount}",
                                        onClick = onMediaClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Default.Star,
                                        iconTint = TelegramYellow,
                                        title = "Pesan Berbintang",
                                        valueText = if (channel.starredCount > 0) "${channel.starredCount}" else "Tidak Ada",
                                        onClick = onStarredClick,
                                        showSeparator = false
                                    )
                                }

                                // Card Settings (Bisukan, Notifikasi Kustom, Pesan Sementara, Tanda Tangan)
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSwitchRow(
                                        icon = Icons.Default.NotificationsOff,
                                        iconTint = TelegramSuccessGreen,
                                        title = "Bisukan",
                                        checked = channel.isMuted,
                                        onCheckedChange = onMuteToggle,
                                        showSeparator = true
                                    )
                                    InfoSwitchRow(
                                        icon = Icons.Default.Notifications,
                                        iconTint = TelegramOrange,
                                        title = "Notifikasi Kustom",
                                        checked = channel.isCustomNotification,
                                        onCheckedChange = onCustomNotificationToggle,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Default.Timer,
                                        iconTint = TelegramBlue,
                                        title = "Pesan Sementara",
                                        valueText = channel.disappearingDuration,
                                        onClick = onDisappearingClick,
                                        showSeparator = true
                                    )
                                    InfoSwitchRow(
                                        icon = Icons.Default.Draw,
                                        iconTint = TelegramBlue,
                                        title = "Tanda Tangan Pesan",
                                        checked = channel.isSignaturesEnabled,
                                        onCheckedChange = onSignaturesToggle,
                                        showSeparator = false
                                    )
                                }

                                // Card Danger
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp, bottom = 24.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                        iconTint = TelegramDangerRed,
                                        title = "Keluar dari Channel",
                                        titleColor = TelegramDangerRed,
                                        showChevron = false,
                                        showSeparator = channel.isAdmin,
                                        onClick = { showLeaveDialog = true }
                                    )
                                    if (channel.isAdmin) {
                                        InfoSettingRow(
                                            icon = Icons.Default.Delete,
                                            iconTint = TelegramDangerRed,
                                            title = "Hapus Channel",
                                            titleColor = TelegramDangerRed,
                                            showChevron = false,
                                            showSeparator = false,
                                            onClick = { showDeleteDialog = true }
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

    // Leave Channel Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Tinggalkan Channel?") },
            text = { Text("Anda tidak akan lagi menerima siaran dari channel ini.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onLeaveChannel()
                }) {
                    Text("Keluar", color = TelegramDangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Channel Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Channel?") },
            text = { Text("Tindakan ini tidak dapat dibatalkan. Channel dan semua postingan akan dihapus untuk semua pelanggan.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteChannel()
                }) {
                    Text("Hapus", color = TelegramDangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun formatSubscribers(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@Preview(name = "Channel Info Light", showBackground = true)
@Composable
private fun ChannelInfoLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            ChannelInfoContent(
                uiState = ChannelInfoUiState.Success(
                    Channel(
                        id = "ch_1",
                        name = "Telegram News",
                        subscriberCount = 894200,
                        description = "Berita resmi seputar Telegram. Dapatkan informasi fitur terbaru dan pembaruan arsitektur.",
                        mediaCount = 824,
                        starredCount = 45,
                        isAdmin = false,
                        isSignaturesEnabled = true
                    )
                ),
                onBack = {},
                onAudioClick = {},
                onVideoClick = {},
                onSearchClick = {},
                onMediaClick = {},
                onStarredClick = {},
                onMuteToggle = {},
                onCustomNotificationToggle = {},
                onDisappearingClick = {},
                onSignaturesToggle = {},
                onLeaveChannel = {},
                onDeleteChannel = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Channel Info Dark", showBackground = true)
@Composable
private fun ChannelInfoDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            ChannelInfoContent(
                uiState = ChannelInfoUiState.Success(
                    Channel(
                        id = "ch_1",
                        name = "Telegram News",
                        subscriberCount = 894200,
                        description = "Berita resmi seputar Telegram. Dapatkan informasi fitur terbaru dan pembaruan arsitektur.",
                        mediaCount = 824,
                        starredCount = 45,
                        isAdmin = true,
                        isMuted = true,
                        isSignaturesEnabled = true
                    )
                ),
                onBack = {},
                onAudioClick = {},
                onVideoClick = {},
                onSearchClick = {},
                onMediaClick = {},
                onStarredClick = {},
                onMuteToggle = {},
                onCustomNotificationToggle = {},
                onDisappearingClick = {},
                onSignaturesToggle = {},
                onLeaveChannel = {},
                onDeleteChannel = {},
                onRetry = {}
            )
        }
    }
}
