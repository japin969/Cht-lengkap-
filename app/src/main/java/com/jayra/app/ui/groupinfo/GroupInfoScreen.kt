package com.jayra.app.ui.groupinfo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.model.Group
import com.jayra.app.model.GroupInfoUiState
import com.jayra.app.ui.components.InfoActionGrid
import com.jayra.app.ui.components.InfoProfileHeader
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
import com.jayra.app.ui.viewmodel.GroupInfoViewModel

@Composable
fun GroupInfoScreen(
    groupId: String,
    onBack: () -> Unit,
    onSearchClick: (String) -> Unit = {},
    onMediaClick: (String) -> Unit = {},
    onStarredClick: (String) -> Unit = {},
    onMembersClick: (String) -> Unit = {},
    onDisappearingClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {},
    viewModel: GroupInfoViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    GroupInfoContent(
        uiState = uiState,
        onBack = onBack,
        onEditClick = { onEditClick(groupId) },
        onAudioClick = { viewModel.onAudioClick(context) },
        onVideoClick = { viewModel.onVideoClick(context) },
        onSearchClick = { onSearchClick(groupId) },
        onMediaClick = { onMediaClick(groupId) },
        onStarredClick = { onStarredClick(groupId) },
        onMembersClick = { onMembersClick(groupId) },
        onMuteToggle = viewModel::onMuteToggle,
        onCustomNotificationToggle = viewModel::onCustomNotificationToggle,
        onDisappearingClick = { onDisappearingClick(groupId) },
        onLockToggle = viewModel::onLockToggle,
        onLeaveGroup = { viewModel.onLeaveGroup("user_telegram_me", onBack) },
        onDeleteGroup = { viewModel.onDeleteGroup(onBack) },
        onRetry = viewModel::onRetry
    )
}

@Composable
fun GroupInfoContent(
    uiState: GroupInfoUiState,
    onBack: () -> Unit,
    onEditClick: () -> Unit = {},
    onAudioClick: () -> Unit,
    onVideoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMediaClick: () -> Unit,
    onStarredClick: () -> Unit,
    onMembersClick: () -> Unit,
    onMuteToggle: (Boolean) -> Unit,
    onCustomNotificationToggle: (Boolean) -> Unit,
    onDisappearingClick: () -> Unit,
    onLockToggle: (Boolean) -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

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
                        .testTag("group_info_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Info Grup",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                val isAdmin = (uiState as? GroupInfoUiState.Success)?.group?.isAdmin == true
                if (isAdmin) {
                    TextButton(onClick = onEditClick) {
                        Text(
                            text = "Edit",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = JayraBlue
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
                is GroupInfoUiState.Loading -> {
                    ShimmerCardList(cardCount = 5)
                }
                is GroupInfoUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                is GroupInfoUiState.Success -> {
                    val group = uiState.group
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Profile Section (Header)
                        InfoProfileHeader(
                            name = group.name,
                            subtitle = "Grup · ${group.memberCount} peserta",
                            avatarUrl = group.avatarUrl
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
                                // 2. Action Grid (Audio, Video, Cari)
                                InfoActionGrid(
                                    action1Icon = Icons.Rounded.Call,
                                    action1Label = "audio",
                                    onAction1Click = onAudioClick,
                                    action2Icon = Icons.Rounded.Videocam,
                                    action2Label = "video",
                                    onAction2Click = onVideoClick,
                                    action3Icon = Icons.Rounded.Search,
                                    action3Label = "cari",
                                    onAction3Click = onSearchClick,
                                    modifier = Modifier.padding(top = 16.dp)
                                )

                                // 3. Card Deskripsi
                                Box(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        if (group.description.isNotBlank()) {
                                            Text(
                                                text = group.description,
                                                fontSize = 17.sp,
                                                lineHeight = 22.sp,
                                                color = if (isDark) Color.White else Color.Black,
                                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (group.description.length > 90) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = if (isDescriptionExpanded) "Lihat Lebih Sedikit" else "Lihat Selengkapnya",
                                                    fontSize = 17.sp,
                                                    color = JayraBlue,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.clickable {
                                                        isDescriptionExpanded = !isDescriptionExpanded
                                                    }
                                                )
                                            }
                                        } else if (group.isAdmin) {
                                            Text(
                                                text = "Tambah deskripsi grup",
                                                fontSize = 17.sp,
                                                color = JayraSubtext
                                            )
                                        }
                                    }
                                }

                                // 4. Card Media & Bintang
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Photo,
                                        iconTint = JayraBlue,
                                        title = "Media, Tautan, Dok",
                                        valueText = "${group.mediaCount}",
                                        onClick = onMediaClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Star,
                                        iconTint = JayraYellow,
                                        title = "Pesan Berbintang",
                                        valueText = if (group.starredCount > 0) "${group.starredCount}" else "Tidak Ada",
                                        onClick = onStarredClick,
                                        showSeparator = false
                                    )
                                }

                                // 5. Card Anggota
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                        .clickable(onClick = onMembersClick)
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Anggota",
                                            fontSize = 15.sp,
                                            color = JayraSubtext
                                        )
                                        Text(
                                            text = "${group.memberCount} anggota",
                                            fontSize = 17.sp,
                                            color = JayraBlue,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.clickable(onClick = onMembersClick)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(group.memberAvatars) { avatarUrl ->
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(JayraBlue)
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(avatarUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "Avatar anggota",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }

                                // 6. Card Settings
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSwitchRow(
                                        icon = Icons.Rounded.NotificationsOff,
                                        iconTint = JayraSuccessGreen,
                                        title = "Bisukan",
                                        checked = group.isMuted,
                                        onCheckedChange = onMuteToggle,
                                        showSeparator = true
                                    )
                                    InfoSwitchRow(
                                        icon = Icons.Rounded.Notifications,
                                        iconTint = JayraOrange,
                                        title = "Notifikasi Kustom",
                                        checked = group.isCustomNotification,
                                        onCheckedChange = onCustomNotificationToggle,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Timer,
                                        iconTint = JayraBlue,
                                        title = "Pesan Sementara",
                                        valueText = group.disappearingDuration,
                                        onClick = onDisappearingClick,
                                        showSeparator = true
                                    )
                                    InfoSwitchRow(
                                        icon = Icons.Rounded.Lock,
                                        iconTint = JayraYellow,
                                        title = "Kunci Grup",
                                        checked = group.isLocked,
                                        onCheckedChange = onLockToggle,
                                        showSeparator = false
                                    )
                                }

                                // 7. Card Danger
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp, bottom = 24.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                        iconTint = JayraDangerRed,
                                        title = "Keluar dari Grup",
                                        titleColor = JayraDangerRed,
                                        showChevron = false,
                                        showSeparator = group.isAdmin,
                                        onClick = { showLeaveDialog = true }
                                    )
                                    if (group.isAdmin) {
                                        InfoSettingRow(
                                            icon = Icons.Rounded.Delete,
                                            iconTint = JayraDangerRed,
                                            title = "Hapus Grup",
                                            titleColor = JayraDangerRed,
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

    // Leave Group Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Keluar dari Grup?") },
            text = { Text("Anda tidak akan lagi menerima pesan dari grup ini.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onLeaveGroup()
                }) {
                    Text("Keluar", color = JayraDangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Group Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Grup?") },
            text = { Text("Tindakan ini tidak dapat dibatalkan. Seluruh pesan dan anggota akan dihapus secara permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteGroup()
                }) {
                    Text("Hapus", color = JayraDangerRed)
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

@Preview(name = "Group Info Light", showBackground = true)
@Composable
private fun GroupInfoLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            GroupInfoContent(
                uiState = GroupInfoUiState.Success(
                    Group(
                        id = "group_1",
                        name = "Android Developers Community",
                        memberCount = 3428,
                        description = "Komunitas resmi pengembang Android. Diskusi Jetpack Compose, Kotlin Multiplatform, dan tips performa 120 FPS.",
                        mediaCount = 142,
                        starredCount = 15,
                        isAdmin = true,
                        isCustomNotification = true
                    )
                ),
                onBack = {},
                onAudioClick = {},
                onVideoClick = {},
                onSearchClick = {},
                onMediaClick = {},
                onStarredClick = {},
                onMembersClick = {},
                onMuteToggle = {},
                onCustomNotificationToggle = {},
                onDisappearingClick = {},
                onLockToggle = {},
                onLeaveGroup = {},
                onDeleteGroup = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Group Info Dark", showBackground = true)
@Composable
private fun GroupInfoDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            GroupInfoContent(
                uiState = GroupInfoUiState.Success(
                    Group(
                        id = "group_1",
                        name = "Android Developers Community",
                        memberCount = 3428,
                        description = "Komunitas resmi pengembang Android. Diskusi Jetpack Compose, Kotlin Multiplatform, dan tips performa 120 FPS.",
                        mediaCount = 142,
                        starredCount = 15,
                        isAdmin = true,
                        isMuted = true
                    )
                ),
                onBack = {},
                onAudioClick = {},
                onVideoClick = {},
                onSearchClick = {},
                onMediaClick = {},
                onStarredClick = {},
                onMembersClick = {},
                onMuteToggle = {},
                onCustomNotificationToggle = {},
                onDisappearingClick = {},
                onLockToggle = {},
                onLeaveGroup = {},
                onDeleteGroup = {},
                onRetry = {}
            )
        }
    }
}
