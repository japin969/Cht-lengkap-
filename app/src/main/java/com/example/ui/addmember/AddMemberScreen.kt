package com.example.ui.addmember

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.model.AddMemberUiState
import com.example.model.Group
import com.example.model.User
import com.example.ui.components.ShimmerCardList
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

@Composable
fun AddMemberScreen(
    groupId: String,
    onBack: () -> Unit,
    onMemberAdded: () -> Unit,
    viewModel: AddMemberViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedUserIds by viewModel.selectedUserIds.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AddMemberContent(
        uiState = uiState,
        searchQuery = searchQuery,
        selectedUserIds = selectedUserIds,
        onSearchChange = viewModel::onSearchQueryChange,
        onClearSearch = viewModel::onClearSearch,
        onToggleUser = viewModel::toggleUserSelection,
        onDeselectUser = viewModel::deselectUser,
        onConfirmAdd = {
            viewModel.addSelectedMembers(context) {
                onMemberAdded()
            }
        },
        onBack = onBack,
        onRetry = viewModel::onRetry
    )
}

@Composable
fun AddMemberContent(
    uiState: AddMemberUiState,
    searchQuery: String,
    selectedUserIds: Set<String>,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleUser: (String) -> Unit,
    onDeselectUser: (String) -> Unit,
    onConfirmAdd: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val selectedCount = selectedUserIds.size

    val subtitleText = if (selectedCount == 0) {
        "Pilih kontak"
    } else {
        "$selectedCount dipilih"
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
            ) {
                // TopAppBar (56dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("add_member_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = "Tambah Anggota",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitleText,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedCount > 0,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = onConfirmAdd,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("add_member_btn_done")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selesai",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Search Bar in TopBar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF2C3847) else Color(0xFF42688C))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari kontak...",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_member_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Selected contacts avatars horizontal row
                if (selectedCount > 0 && uiState is AddMemberUiState.Success) {
                    val selectedUsers = uiState.availableContacts.filter { selectedUserIds.contains(it.id) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        selectedUsers.forEach { user ->
                            SelectedContactChip(
                                user = user,
                                onDeselect = { onDeselectUser(user.id) }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedCount > 0) {
                FloatingActionButton(
                    onClick = onConfirmAdd,
                    containerColor = TelegramBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_confirm_add_members")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selesai"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(paddingValues)
        ) {
            when (uiState) {
                is AddMemberUiState.Loading -> {
                    ShimmerCardList(cardCount = 6)
                }
                is AddMemberUiState.Empty -> {
                    AddMemberEmptyView(
                        title = "Tidak ada kontak tersedia",
                        subtitle = "Semua kontak Anda sudah menjadi anggota grup ini"
                    )
                }
                is AddMemberUiState.Error -> {
                    AddMemberErrorView(
                        message = uiState.message,
                        onRetry = onRetry
                    )
                }
                is AddMemberUiState.Success -> {
                    if (uiState.availableContacts.isEmpty()) {
                        AddMemberEmptyView(
                            title = "Kontak tidak ditemukan",
                            subtitle = "Coba kata kunci pencarian yang berbeda"
                        )
                    } else {
                        AvailableContactsList(
                            contacts = uiState.availableContacts,
                            selectedUserIds = selectedUserIds,
                            onToggleUser = onToggleUser
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedContactChip(
    user: User,
    onDeselect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onDeselect)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3498DB)),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Remove badge (X)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8E8E93))
                    .align(Alignment.TopEnd),
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

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = user.name.split(" ").firstOrNull() ?: user.name,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AvailableContactsList(
    contacts: List<User>,
    selectedUserIds: Set<String>,
    onToggleUser: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color(0xFF1E2732) else Color(0xFFE8E8E8)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(
            items = contacts,
            key = { it.id }
        ) { contact ->
            val isSelected = selectedUserIds.contains(contact.id)
            ContactSelectableRow(
                user = contact,
                isSelected = isSelected,
                onClick = { onToggleUser(contact.id) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = dividerColor
            )
        }
    }
}

@Composable
private fun ContactSelectableRow(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1F1F1F)
    val selectedBg = if (isDark) Color(0xFF1A3A5C) else Color(0xFFE3F2FD)
    val defaultBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(if (isSelected) selectedBg else defaultBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar 48dp
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2ECC71)),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = user.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = user.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name and Status
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = user.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val statusText = if (user.isOnline) "online" else user.lastSeenFormatted
            val statusColor = if (user.isOnline) TelegramBlue else TelegramSubtext
            Text(
                text = statusText,
                fontSize = 13.sp,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Custom Telegram Circular Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) TelegramBlue else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isSelected) TelegramBlue else Color(0xFF8E8E93),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Dipilih",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddMemberEmptyView(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            tint = TelegramSubtext.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TelegramSubtext
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TelegramSubtext,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AddMemberErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TelegramDangerRed,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Gagal memuat kontak",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TelegramSubtext
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = TelegramSubtext
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Coba Lagi", color = Color.White)
        }
    }
}

@Preview(name = "Add Member Light")
@Composable
private fun AddMemberLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            AddMemberContent(
                uiState = AddMemberUiState.Success(
                    group = Group(id = "chat_2", name = "Android Devs"),
                    availableContacts = listOf(
                        User(id = "1", name = "Sarah Connor", username = "sconnor", isOnline = true),
                        User(id = "2", name = "David Kim", username = "davidkim", isOnline = false, lastSeenFormatted = "2 jam lalu")
                    ),
                    selectedUserIds = setOf("1")
                ),
                searchQuery = "",
                selectedUserIds = setOf("1"),
                onSearchChange = {},
                onClearSearch = {},
                onToggleUser = {},
                onDeselectUser = {},
                onConfirmAdd = {},
                onBack = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Add Member Dark")
@Composable
private fun AddMemberDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            AddMemberContent(
                uiState = AddMemberUiState.Success(
                    group = Group(id = "chat_2", name = "Android Devs"),
                    availableContacts = listOf(
                        User(id = "1", name = "Sarah Connor", username = "sconnor", isOnline = false)
                    ),
                    selectedUserIds = emptySet()
                ),
                searchQuery = "",
                selectedUserIds = emptySet(),
                onSearchChange = {},
                onClearSearch = {},
                onToggleUser = {},
                onDeselectUser = {},
                onConfirmAdd = {},
                onBack = {},
                onRetry = {}
            )
        }
    }
}
