package com.jayra.app.ui.creategroup

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.model.User
import com.jayra.app.ui.theme.AppleEmojiFont
import com.jayra.app.ui.theme.SFProFontFamily
import com.jayra.app.util.glassSurface
import java.io.File

private val IOS_BLUE_LIGHT = Color(0xFF007AFF)
private val IOS_BLUE_DARK = Color(0xFF0A84FF)
private val IOS_BG_LIGHT = Color(0xFFF2F2F7)
private val IOS_BG_DARK = Color(0xFF000000)
private val IOS_GROUPED_LIGHT = Color(0xFFFFFFFF)
private val IOS_GROUPED_DARK = Color(0xFF1C1C1E)
private val IOS_SEPARATOR_LIGHT = Color(0xFFC6C6C8)
private val IOS_SEPARATOR_DARK = Color(0xFF38383A)
private val IOS_LABEL_LIGHT = Color(0xFF000000)
private val IOS_LABEL_DARK = Color(0xFFFFFFFF)
private val IOS_SECONDARY = Color(0xFF8E8E93)
private val IOS_GREEN = Color(0xFF34C759)
private val IOS_RED = Color(0xFFFF3B30)

private val EMOJI_PALETTE = listOf(
    "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆",
    "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗",
    "😙", "😚", "🙂", "🤗", "🤩", "🤔", "🤨", "😐",
    "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐",
    "😯", "😪", "😫", "🥱", "😴", "😌", "😛", "😜",
    "😝", "🤤", "😒", "😓", "😔", "😕", "🙃", "🤑",
    "😲", "🙁", "😖", "😞", "😟", "😤", "😢", "😭",
    "😦", "😧", "😨", "😩", "🤯", "😬", "😰", "😱",
    "🥵", "🥶", "😳", "🤪", "😵", "🥴", "😠", "😡",
    "🤬", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "😇",
    "🥳", "🥺", "🤠", "🤡", "🤥", "🤫", "🤭", "🧐"
)

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = viewModel()
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val groupAvatar by viewModel.groupAvatar.collectAsStateWithLifecycle()
    val selectedMemberIds by viewModel.selectedMemberIds.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val groupDescription by viewModel.groupDescription.collectAsStateWithLifecycle()
    val groupPermissions by viewModel.groupPermissions.collectAsStateWithLifecycle()
    val disappearingDuration by viewModel.disappearingDuration.collectAsStateWithLifecycle()
    val adminApproval by viewModel.adminApproval.collectAsStateWithLifecycle()
    val inviteViaLink by viewModel.inviteViaLink.collectAsStateWithLifecycle()
    val mediaVisibility by viewModel.mediaVisibility.collectAsStateWithLifecycle()
    val webQuery by viewModel.webSearchQuery.collectAsStateWithLifecycle()
    val webResults by viewModel.webSearchResults.collectAsStateWithLifecycle()
    val webLoading by viewModel.webSearchLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) viewModel.onPhotoSelected(uri.toString()) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            viewModel.onPhotoSelected(pendingCameraUri.toString())
        }
        pendingCameraUri = null
    }

    val launchCamera: () -> Unit = {
        try {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (_: Exception) {}
    }

    LaunchedEffect(uiState) {
        if (uiState is CreateGroupUiState.Error) {
            snackbarHostState.showSnackbar((uiState as CreateGroupUiState.Error).message)
            viewModel.resetState()
        }
    }

    CreateGroupContent(
        groupName = groupName,
        groupDescription = groupDescription,
        groupPermissions = groupPermissions,
        disappearingDuration = disappearingDuration,
        adminApproval = adminApproval,
        inviteViaLink = inviteViaLink,
        mediaVisibility = mediaVisibility,
        groupAvatar = groupAvatar,
        selectedMemberIds = selectedMemberIds,
        contacts = contacts,
        isLoading = uiState is CreateGroupUiState.Loading,
        webQuery = webQuery,
        webResults = webResults,
        webLoading = webLoading,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onGroupNameChange = viewModel::onGroupNameChange,
        onGroupDescriptionChange = viewModel::onGroupDescriptionChange,
        onPermissionChange = viewModel::onPermissionChange,
        onDisappearingChange = viewModel::onDisappearingChange,
        onAdminApprovalChange = viewModel::onAdminApprovalChange,
        onInviteViaLinkChange = viewModel::onInviteViaLinkChange,
        onMediaVisibilityChange = viewModel::onMediaVisibilityChange,
        onPickFromGallery = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onPickFromCamera = launchCamera,
        onPickEmoji = { emoji -> viewModel.onPhotoSelected("emoji:$emoji") },
        onPickWeb = { url -> viewModel.onPhotoSelected(url) },
        onRemovePhoto = { viewModel.clearAvatar() },
        onWebQueryChange = viewModel::onWebSearchQueryChange,
        onWebSearch = { viewModel.searchWebImages() },
        onToggleMember = viewModel::toggleMember,
        onRemoveMember = viewModel::removeMember,
        onCreateGroup = { viewModel.createGroup(onSuccess = onGroupCreated) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupContent(
    groupName: String,
    groupDescription: String = "",
    groupPermissions: Map<String, String> = emptyMap(),
    disappearingDuration: String = "Mati",
    adminApproval: Boolean = false,
    inviteViaLink: Boolean = true,
    mediaVisibility: Boolean = true,
    groupAvatar: String,
    selectedMemberIds: Set<String>,
    contacts: List<User>,
    isLoading: Boolean,
    webQuery: String = "",
    webResults: List<String> = emptyList(),
    webLoading: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onGroupNameChange: (String) -> Unit,
    onGroupDescriptionChange: (String) -> Unit = {},
    onPermissionChange: (String, String) -> Unit = { _, _ -> },
    onDisappearingChange: (String) -> Unit = {},
    onAdminApprovalChange: (Boolean) -> Unit = {},
    onInviteViaLinkChange: (Boolean) -> Unit = {},
    onMediaVisibilityChange: (Boolean) -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onPickFromCamera: () -> Unit = {},
    onPickEmoji: (String) -> Unit = {},
    onPickWeb: (String) -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onWebQueryChange: (String) -> Unit = {},
    onWebSearch: () -> Unit = {},
    onToggleMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val tint = if (isDark) IOS_BLUE_DARK else IOS_BLUE_LIGHT
    val bg = if (isDark) IOS_BG_DARK else IOS_BG_LIGHT
    val cardBg = if (isDark) IOS_GROUPED_DARK else IOS_GROUPED_LIGHT
    val label = if (isDark) IOS_LABEL_DARK else IOS_LABEL_LIGHT
    val sep = if (isDark) IOS_SEPARATOR_DARK else IOS_SEPARATOR_LIGHT

    val canCreate = groupName.trim().isNotBlank() && selectedMemberIds.isNotEmpty() && !isLoading

    val selectedContacts = remember(selectedMemberIds, contacts) {
        contacts.filter { selectedMemberIds.contains(it.id) }
    }

    var showActionSheet by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showWebSearch by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().glassSurface(solidColor = bg, tintAlpha = 0.85f, blurRadius = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back chevron
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp).testTag("create_group_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Title center
                    Text(
                        text = "New Group",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = label,
                        fontFamily = SFProFontFamily
                    )
                    // Create button
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = tint,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Create",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canCreate) tint else IOS_SECONDARY,
                                fontFamily = SFProFontFamily,
                                modifier = Modifier
                                    .clickable(enabled = canCreate) { onCreateGroup() }
                                    .testTag("create_group_submit")
                            )
                        }
                    }
                }
            }
        },
        containerColor = bg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // Avatar Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(IOS_SECONDARY.copy(alpha = 0.3f))
                            .clickable { showActionSheet = true }
                            .testTag("create_group_avatar_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            groupAvatar.startsWith("emoji:") -> {
                                val emoji = groupAvatar.removePrefix("emoji:")
                                Text(text = emoji, fontSize = 64.sp, fontFamily = AppleEmojiFont)
                            }
                            groupAvatar.isNotBlank() -> AsyncImage(
                                model = groupAvatar,
                                contentDescription = "Foto Grup",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Pilih Foto",
                                tint = IOS_SECONDARY,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Add Group Icon",
                        fontSize = 15.sp,
                        color = tint,
                        fontFamily = SFProFontFamily,
                        modifier = Modifier.clickable { showActionSheet = true }.padding(8.dp)
                    )
                }
            }

            // Name field
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (groupName.isEmpty()) {
                            Text(
                                text = "Group Name",
                                fontSize = 17.sp,
                                color = IOS_SECONDARY,
                                fontFamily = SFProFontFamily
                            )
                        }
                        BasicTextField(
                            value = groupName,
                            onValueChange = onGroupNameChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 17.sp,
                                color = label,
                                fontFamily = SFProFontFamily
                            ),
                            cursorBrush = SolidColor(tint),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth().testTag("create_group_name_input")
                        )
                    }
                    Text(
                        text = "${groupName.length}/100",
                        fontSize = 13.sp,
                        color = IOS_SECONDARY,
                        fontFamily = SFProFontFamily
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(0.5.dp)
                        .background(sep)
                )
            }

            // Description
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (groupDescription.isEmpty()) {
                            Text(
                                text = "Add group description...",
                                fontSize = 17.sp,
                                color = IOS_SECONDARY,
                                fontFamily = SFProFontFamily
                            )
                        }
                        BasicTextField(
                            value = groupDescription,
                            onValueChange = onGroupDescriptionChange,
                            maxLines = 4,
                            textStyle = TextStyle(
                                fontSize = 17.sp,
                                color = label,
                                fontFamily = SFProFontFamily
                            ),
                            cursorBrush = SolidColor(tint),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = "${groupDescription.length}/512",
                        fontSize = 13.sp,
                        color = IOS_SECONDARY,
                        fontFamily = SFProFontFamily
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(0.5.dp)
                        .background(sep)
                )
            }

            // Group Permissions Card
            item {
                var pickerKey by remember { mutableStateOf<String?>(null) }
                val permLabels = listOf(
                    "send_messages" to "Kirim Pesan",
                    "send_media" to "Kirim Media",
                    "add_members" to "Tambah Anggota",
                    "pin_messages" to "Sematkan Pesan",
                    "change_info" to "Ubah Info Grup"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "IZIN GRUP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = IOS_SECONDARY,
                        fontFamily = SFProFontFamily,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Column(modifier = Modifier.fillMaxWidth().background(cardBg)) {
                        permLabels.forEachIndexed { idx, (key, lbl) ->
                            val value = groupPermissions[key] ?: "Semua Anggota"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pickerKey = key }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lbl,
                                    fontSize = 17.sp,
                                    color = label,
                                    fontFamily = SFProFontFamily,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = value,
                                    fontSize = 15.sp,
                                    color = tint,
                                    fontFamily = SFProFontFamily
                                )
                            }
                            if (idx < permLabels.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp)
                                        .height(0.5.dp)
                                        .background(sep)
                                )
                            }
                        }
                    }
                }

                if (pickerKey != null) {
                    AlertDialog(
                        onDismissRequest = { pickerKey = null },
                        title = { Text("Izin", fontFamily = SFProFontFamily) },
                        text = {
                            Column {
                                listOf("Semua Anggota", "Admin").forEach { opt ->
                                    Text(
                                        text = opt,
                                        fontSize = 17.sp,
                                        color = tint,
                                        fontFamily = SFProFontFamily,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onPermissionChange(pickerKey!!, opt)
                                                pickerKey = null
                                            }
                                            .padding(vertical = 12.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { pickerKey = null }) {
                                Text("Tutup", color = tint, fontFamily = SFProFontFamily)
                            }
                        }
                    )
                }
            }

            // Section 3: Settings (Disappearing, Admin Approval, Invite Link, Lock, Media Visibility)
            item {
                var showDisappearingDialog by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "PENGATURAN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = IOS_SECONDARY,
                        fontFamily = SFProFontFamily,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Column(modifier = Modifier.fillMaxWidth().background(cardBg)) {
                        // Disappearing messages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDisappearingDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pesan Sementara",
                                fontSize = 17.sp,
                                color = label,
                                fontFamily = SFProFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = disappearingDuration,
                                fontSize = 15.sp,
                                color = tint,
                                fontFamily = SFProFontFamily
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(0.5.dp)
                                .background(sep)
                        )
                        // Admin Approval
                        SettingToggleRow(
                            label = "Persetujuan Admin",
                            checked = adminApproval,
                            tint = tint,
                            labelColor = label,
                            onCheckedChange = onAdminApprovalChange
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(0.5.dp)
                                .background(sep)
                        )
                        // Invite via link
                        SettingToggleRow(
                            label = "Undangan Lewat Link",
                            checked = inviteViaLink,
                            tint = tint,
                            labelColor = label,
                            onCheckedChange = onInviteViaLinkChange
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(0.5.dp)
                                .background(sep)
                        )
                        // Media visibility
                        SettingToggleRow(
                            label = "Media Masuk ke Galeri",
                            checked = mediaVisibility,
                            tint = tint,
                            labelColor = label,
                            onCheckedChange = onMediaVisibilityChange
                        )
                    }
                }

                if (showDisappearingDialog) {
                    AlertDialog(
                        onDismissRequest = { showDisappearingDialog = false },
                        title = { Text("Pesan Sementara", fontFamily = SFProFontFamily) },
                        text = {
                            Column {
                                listOf("Mati", "24 Jam", "7 Hari", "90 Hari").forEach { opt ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onDisappearingChange(opt)
                                                showDisappearingDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = opt,
                                            fontSize = 17.sp,
                                            color = tint,
                                            fontFamily = SFProFontFamily,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (disappearingDuration == opt) {
                                            Text("✓", fontSize = 17.sp, color = tint, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDisappearingDialog = false }) {
                                Text("Tutup", color = tint, fontFamily = SFProFontFamily)
                            }
                        }
                    )
                }
            }

            // Selected members horizontal
            if (selectedContacts.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedContacts, key = { it.id }) { user ->
                            SelectedMemberBadge(user = user, onRemove = { onRemoveMember(user.id) })
                        }
                    }
                }
            }

            // Members section header
            item {
                Text(
                    text = "MEMBERS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = IOS_SECONDARY,
                    fontFamily = SFProFontFamily,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Contacts list
            items(contacts, key = { it.id }) { user ->
                ContactRow(
                    user = user,
                    isSelected = selectedMemberIds.contains(user.id),
                    label = label,
                    cardBg = cardBg,
                    tint = tint,
                    onToggle = { onToggleMember(user.id) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp)
                        .height(0.5.dp)
                        .background(sep)
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Action Sheet (5 pilihan)
    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            containerColor = cardBg,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                ActionSheetRow(
                    icon = Icons.Rounded.PhotoCamera,
                    label = "Take Photo",
                    tint = tint,
                    labelColor = label,
                    onClick = { showActionSheet = false; onPickFromCamera() }
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Image,
                    label = "Choose Photo",
                    tint = tint,
                    labelColor = label,
                    onClick = { showActionSheet = false; onPickFromGallery() }
                )
                ActionSheetRow(
                    icon = Icons.Rounded.EmojiEmotions,
                    label = "Emoji & Sticker",
                    tint = tint,
                    labelColor = label,
                    onClick = { showActionSheet = false; showEmojiPicker = true }
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Search,
                    label = "Search Web",
                    tint = tint,
                    labelColor = label,
                    onClick = { showActionSheet = false; showWebSearch = true }
                )
                if (groupAvatar.isNotBlank()) {
                    ActionSheetRow(
                        icon = Icons.Rounded.Delete,
                        label = "Remove Photo",
                        tint = IOS_RED,
                        labelColor = IOS_RED,
                        onClick = { showActionSheet = false; onRemovePhoto() }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Emoji Picker
    if (showEmojiPicker) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            containerColor = cardBg,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Emoji",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = label,
                    fontFamily = SFProFontFamily
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(EMOJI_PALETTE.size) { i ->
                        val emoji = EMOJI_PALETTE[i]
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onPickEmoji(emoji)
                                    showEmojiPicker = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 32.sp, fontFamily = AppleEmojiFont)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Search Web Modal
    if (showWebSearch) {
        ModalBottomSheet(
            onDismissRequest = { showWebSearch = false },
            containerColor = cardBg,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Search Web",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = label,
                    fontFamily = SFProFontFamily
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IOS_SECONDARY.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = IOS_SECONDARY,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = webQuery,
                        onValueChange = onWebQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = label,
                            fontFamily = SFProFontFamily
                        ),
                        cursorBrush = SolidColor(tint),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (webQuery.isEmpty()) {
                                Text(
                                    text = "Search images…",
                                    fontSize = 15.sp,
                                    color = IOS_SECONDARY,
                                    fontFamily = SFProFontFamily
                                )
                            }
                            inner()
                        }
                    )
                    if (webQuery.isNotBlank()) {
                        Text(
                            text = "Go",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tint,
                            fontFamily = SFProFontFamily,
                            modifier = Modifier.clickable { onWebSearch() }.padding(8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (webLoading) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = tint, modifier = Modifier.size(28.dp))
                    }
                } else if (webResults.isEmpty()) {
                    Text(
                        text = "Cari gambar, hasil muncul di sini",
                        fontSize = 14.sp,
                        color = IOS_SECONDARY,
                        fontFamily = SFProFontFamily,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(webResults.size) { i ->
                            val url = webResults[i]
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onPickWeb(url)
                                        showWebSearch = false
                                    }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    labelColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 17.sp,
            color = labelColor,
            fontFamily = SFProFontFamily
        )
    }
}

@Composable
private fun SelectedMemberBadge(user: User, onRemove: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center)
                    .border(1.dp, IOS_BLUE_LIGHT, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(IOS_RED)
                    .clickable { onRemove() }
                    .testTag("remove_selected_${user.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Hapus",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = user.name.split(" ").firstOrNull() ?: user.name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = IOS_SECONDARY,
            fontFamily = SFProFontFamily
        )
    }
}

@Composable
private fun ContactRow(
    user: User,
    isSelected: Boolean,
    label: Color,
    cardBg: Color,
    tint: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(cardBg)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = user.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SFProFontFamily
            )
            Text(
                text = if (user.isOnline) "online" else "terakhir dilihat",
                fontSize = 14.sp,
                color = if (user.isOnline) IOS_GREEN else IOS_SECONDARY,
                maxLines = 1,
                fontFamily = SFProFontFamily
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) tint else Color.Transparent)
                .border(
                    width = if (isSelected) 0.dp else 1.5.dp,
                    color = if (isSelected) tint else IOS_SECONDARY,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text(
                    text = "✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    tint: Color,
    labelColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            color = labelColor,
            fontFamily = SFProFontFamily,
            modifier = Modifier.weight(1f)
        )
        com.jayra.app.ui.theme.IOSLottieToggle(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

