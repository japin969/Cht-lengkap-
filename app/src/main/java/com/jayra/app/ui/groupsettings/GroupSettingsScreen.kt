package com.jayra.app.ui.groupsettings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.model.Group
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun GroupSettingsScreen(
    groupId: String,
    onBack: () -> Unit,
    onPermissionsClick: (String) -> Unit,
    viewModel: GroupSettingsViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val groupDescription by viewModel.groupDescription.collectAsStateWithLifecycle()
    val groupAvatar by viewModel.groupAvatar.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAvatarChange(uri.toString())
        }
    }

    GroupSettingsContent(
        groupId = groupId,
        uiState = uiState,
        groupName = groupName,
        groupDescription = groupDescription,
        groupAvatar = groupAvatar,
        onBack = onBack,
        onPickPhoto = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onLockToggle = viewModel::onLockToggle,
        onSensitiveContentToggle = viewModel::onSensitiveContentToggle,
        onDisappearingDurationChange = viewModel::onDisappearingDurationChange,
        onPermissionsClick = { onPermissionsClick(groupId) },
        onSave = { viewModel.saveSettings(onSuccess = onBack) },
        onDeleteGroup = { viewModel.deleteGroup(onDeleted = onBack) }
    )
}

@Composable
fun GroupSettingsContent(
    groupId: String,
    uiState: GroupSettingsUiState,
    groupName: String,
    groupDescription: String,
    groupAvatar: String,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLockToggle: (Boolean) -> Unit,
    onSensitiveContentToggle: (Boolean) -> Unit,
    onDisappearingDurationChange: (String) -> Unit,
    onPermissionsClick: () -> Unit,
    onSave: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }

    val currentGroup = (uiState as? GroupSettingsUiState.Success)?.group

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("Hapus Grup?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus grup ini? Semua riwayat obrolan dan media akan dihapus secara permanen.",
                    fontSize = 15.sp,
                    color = JayraSubtext
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteGroup()
                    }
                ) {
                    Text("Hapus", color = JayraDangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal", color = JayraSubtext)
                }
            }
        )
    }

    if (showDisappearingDialog) {
        val durations = listOf("Mati", "24 Jam", "7 Hari", "1 Bulan")
        val currentDuration = currentGroup?.disappearingDuration ?: "Mati"

        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = {
                Text("Pesan Sementara", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column {
                    durations.forEach { duration ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDisappearingDurationChange(duration)
                                    showDisappearingDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentDuration == duration,
                                onClick = {
                                    onDisappearingDurationChange(duration)
                                    showDisappearingDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = JayraBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = duration, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDisappearingDialog = false }) {
                    Text("Tutup", color = JayraBlue)
                }
            }
        )
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("group_settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pengaturan Grup",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("group_settings_save")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Simpan",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = screenBg
    ) { innerPadding ->
        if (uiState is GroupSettingsUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = JayraBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Card Info Grup
                item {
                    Text(
                        text = "INFO GRUP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JayraBlue,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with camera badge
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable { onPickPhoto() }
                                        .testTag("group_settings_avatar_picker")
                                ) {
                                    AsyncImage(
                                        model = groupAvatar.ifBlank { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400" },
                                        contentDescription = "Foto Grup",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CameraAlt,
                                            contentDescription = "Ganti Foto",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                OutlinedTextField(
                                    value = groupName,
                                    onValueChange = onNameChange,
                                    label = { Text("Nama Grup", fontSize = 14.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("group_settings_name_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = JayraBlue,
                                        unfocusedBorderColor = JayraSubtext.copy(alpha = 0.4f),
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = groupDescription,
                                onValueChange = onDescriptionChange,
                                label = { Text("Deskripsi (opsional)", fontSize = 14.sp) },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_settings_desc_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = JayraBlue,
                                    unfocusedBorderColor = JayraSubtext.copy(alpha = 0.4f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                supportingText = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "${groupDescription.length}/255",
                                            fontSize = 12.sp,
                                            color = JayraSubtext
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Card Izin Anggota
                item {
                    Text(
                        text = "IZIN ANGGOTA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JayraBlue,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column {
                            // Header Clickable Row to open Permissions Screen
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPermissionsClick() }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("group_settings_permissions_btn"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = JayraBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Kelola Izin Anggota",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Buka",
                                    tint = JayraSubtext,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = JayraSubtext.copy(alpha = 0.15f)
                            )

                            // Quick Permissions Summary
                            val perms = currentGroup?.permissions ?: emptyMap()
                            PermissionSummaryItem(
                                icon = Icons.Rounded.Message,
                                title = "Kirim Pesan",
                                role = perms["send_messages"] ?: "Semua Anggota"
                            )
                            PermissionSummaryItem(
                                icon = Icons.Rounded.Image,
                                title = "Kirim Media",
                                role = perms["send_media"] ?: "Semua Anggota"
                            )
                            PermissionSummaryItem(
                                icon = Icons.Rounded.PersonAdd,
                                title = "Tambah Anggota",
                                role = perms["add_members"] ?: "Admin"
                            )
                            PermissionSummaryItem(
                                icon = Icons.Rounded.PushPin,
                                title = "Pin Pesan",
                                role = perms["pin_messages"] ?: "Admin"
                            )
                            PermissionSummaryItem(
                                icon = Icons.Rounded.Edit,
                                title = "Ubah Info Grup",
                                role = perms["change_info"] ?: "Admin"
                            )
                        }
                    }
                }

                // Card Pengaturan Tambahan
                item {
                    Text(
                        text = "PENGATURAN TAMBAHAN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JayraBlue,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column {
                            // Disappearing Messages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDisappearingDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("group_settings_disappearing_btn"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = null,
                                        tint = JayraBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Pesan Sementara",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Hapus pesan otomatis setelah dibaca",
                                            fontSize = 13.sp,
                                            color = JayraSubtext
                                        )
                                    }
                                }

                                Text(
                                    text = currentGroup?.disappearingDuration ?: "Mati",
                                    fontSize = 15.sp,
                                    color = JayraBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = JayraSubtext.copy(alpha = 0.15f)
                            )

                            // Lock Group Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = JayraBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Kunci Grup",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Hanya admin yang dapat berinteraksi",
                                            fontSize = 13.sp,
                                            color = JayraSubtext
                                        )
                                    }
                                }

                                Switch(
                                    checked = currentGroup?.isLocked == true,
                                    onCheckedChange = onLockToggle,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = JayraBlue
                                    ),
                                    modifier = Modifier.testTag("group_settings_lock_switch")
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = JayraSubtext.copy(alpha = 0.15f)
                            )

                            // Sensitive Content Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                        tint = JayraBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Sensor Konten Sensitif",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Sembunyikan media sensitif untuk anggota",
                                            fontSize = 13.sp,
                                            color = JayraSubtext
                                        )
                                    }
                                }

                                Switch(
                                    checked = currentGroup?.sensitiveContent == true,
                                    onCheckedChange = onSensitiveContentToggle,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = JayraBlue
                                    ),
                                    modifier = Modifier.testTag("group_settings_sensitive_switch")
                                )
                            }
                        }
                    }
                }

                // Zona Bahaya
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeleteConfirmDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("group_settings_delete_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Hapus Grup",
                                tint = JayraDangerRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Hapus Grup",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JayraDangerRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun PermissionSummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    role: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = JayraSubtext,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp
            )
        }
        Text(
            text = role,
            fontSize = 14.sp,
            color = if (role == "Admin") JayraBlue else JayraSubtext,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun GroupSettingsScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            GroupSettingsContent(
                groupId = "chat_2",
                uiState = GroupSettingsUiState.Success(
                    group = Group(
                        id = "chat_2",
                        name = "Android Developers Community",
                        description = "Komunitas pengembang Android resmi.",
                        avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400",
                        isLocked = false,
                        sensitiveContent = true,
                        disappearingDuration = "24 Jam"
                    )
                ),
                groupName = "Android Developers Community",
                groupDescription = "Komunitas pengembang Android resmi.",
                groupAvatar = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400",
                onBack = {},
                onPickPhoto = {},
                onNameChange = {},
                onDescriptionChange = {},
                onLockToggle = {},
                onSensitiveContentToggle = {},
                onDisappearingDurationChange = {},
                onPermissionsClick = {},
                onSave = {},
                onDeleteGroup = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun GroupSettingsScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            GroupSettingsContent(
                groupId = "chat_2",
                uiState = GroupSettingsUiState.Success(
                    group = Group(
                        id = "chat_2",
                        name = "Android Developers Community",
                        description = "Komunitas pengembang Android resmi.",
                        avatarUrl = "",
                        isLocked = true,
                        sensitiveContent = false,
                        disappearingDuration = "Mati"
                    )
                ),
                groupName = "Android Developers Community",
                groupDescription = "Komunitas pengembang Android resmi.",
                groupAvatar = "",
                onBack = {},
                onPickPhoto = {},
                onNameChange = {},
                onDescriptionChange = {},
                onLockToggle = {},
                onSensitiveContentToggle = {},
                onDisappearingDurationChange = {},
                onPermissionsClick = {},
                onSave = {},
                onDeleteGroup = {}
            )
        }
    }
}
