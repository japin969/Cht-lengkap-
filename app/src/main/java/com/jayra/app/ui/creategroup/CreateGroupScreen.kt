package com.jayra.app.ui.creategroup

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.model.User
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val groupAvatar by viewModel.groupAvatar.collectAsStateWithLifecycle()
    val selectedMemberIds by viewModel.selectedMemberIds.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoSelected(uri.toString())
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CreateGroupUiState.Error) {
            snackbarHostState.showSnackbar((uiState as CreateGroupUiState.Error).message)
            viewModel.resetState()
        }
    }

    CreateGroupContent(
        groupName = groupName,
        groupAvatar = groupAvatar,
        selectedMemberIds = selectedMemberIds,
        contacts = contacts,
        isLoading = uiState is CreateGroupUiState.Loading,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onGroupNameChange = viewModel::onGroupNameChange,
        onPickPhoto = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onToggleMember = viewModel::toggleMember,
        onRemoveMember = viewModel::removeMember,
        onCreateGroup = {
            viewModel.createGroup(onSuccess = onGroupCreated)
        }
    )
}

@Composable
fun CreateGroupContent(
    groupName: String,
    groupAvatar: String,
    selectedMemberIds: Set<String>,
    contacts: List<User>,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onGroupNameChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onToggleMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val canCreate = groupName.trim().isNotBlank() && selectedMemberIds.isNotEmpty() && !isLoading

    val selectedContacts = remember(selectedMemberIds, contacts) {
        contacts.filter { selectedMemberIds.contains(it.id) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            .testTag("create_group_back")
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
                        text = "Grup Baru",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    TextButton(
                        onClick = onCreateGroup,
                        enabled = canCreate,
                        modifier = Modifier.testTag("create_group_submit")
                    ) {
                        Text(
                            text = "Buat",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canCreate) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        },
        containerColor = screenBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profile & Name Section Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 120dp Circle Avatar Picker
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8E8E93))
                                .clickable { onPickPhoto() }
                                .testTag("create_group_avatar_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (groupAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = groupAvatar,
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
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Ganti Foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Pilih Foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Ubah Foto Grup",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = JayraBlue,
                            modifier = Modifier
                                .clickable { onPickPhoto() }
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Form Nama Grup
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = onGroupNameChange,
                            placeholder = {
                                Text(
                                    text = "Nama Grup",
                                    fontSize = 17.sp,
                                    color = JayraSubtext
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_group_name_input"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (canCreate) onCreateGroup()
                                }
                            ),
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
                                        text = "${groupName.length}/50",
                                        fontSize = 12.sp,
                                        color = JayraSubtext
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Selected Members Horizontal Row
            if (selectedContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "ANGGOTA TERPILIH (${selectedContacts.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JayraBlue,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedContacts, key = { it.id }) { user ->
                            SelectedMemberBadge(
                                user = user,
                                onRemove = { onRemoveMember(user.id) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Header Contact List
            item {
                Text(
                    text = "PILIH ANGGOTA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = JayraSubtext,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Contact List
            items(contacts, key = { it.id }) { user ->
                val isSelected = selectedMemberIds.contains(user.id)
                ContactSelectRow(
                    user = user,
                    isSelected = isSelected,
                    onToggle = { onToggleMember(user.id) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp),
                    thickness = 0.5.dp,
                    color = JayraSubtext.copy(alpha = 0.15f)
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SelectedMemberBadge(
    user: User,
    onRemove: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp)
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center)
                    .border(1.dp, JayraBlue, CircleShape)
            )

            // Close badge
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFE74C3C))
                    .clickable { onRemove() }
                    .testTag("remove_selected_${user.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = user.name.split(" ").firstOrNull() ?: user.name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = JayraSubtext
        )
    }
}

@Composable
fun ContactSelectRow(
    user: User,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = user.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (user.isOnline) "online" else "terakhir dilihat baru-baru ini",
                fontSize = 14.sp,
                color = if (user.isOnline) JayraBlue else JayraSubtext,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = JayraBlue,
                uncheckedColor = JayraSubtext
            ),
            modifier = Modifier.testTag("contact_checkbox_${user.id}")
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun CreateGroupScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            CreateGroupContent(
                groupName = "Android Jetpack Team",
                groupAvatar = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400",
                selectedMemberIds = setOf("1", "2"),
                contacts = listOf(
                    User(id = "1", name = "Pavel Durov", isOnline = true),
                    User(id = "2", name = "Alex Rivera", isOnline = false),
                    User(id = "3", name = "David Kim", isOnline = false)
                ),
                isLoading = false,
                onBack = {},
                onGroupNameChange = {},
                onPickPhoto = {},
                onToggleMember = {},
                onRemoveMember = {},
                onCreateGroup = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun CreateGroupScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            CreateGroupContent(
                groupName = "",
                groupAvatar = "",
                selectedMemberIds = emptySet(),
                contacts = listOf(
                    User(id = "1", name = "Pavel Durov", isOnline = true),
                    User(id = "2", name = "Alex Rivera", isOnline = false)
                ),
                isLoading = false,
                onBack = {},
                onGroupNameChange = {},
                onPickPhoto = {},
                onToggleMember = {},
                onRemoveMember = {},
                onCreateGroup = {}
            )
        }
    }
}
