package com.example.ui.groupmembers

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.model.Group
import com.example.model.GroupMembersUiState
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
import kotlin.math.roundToInt

@Composable
fun GroupMembersScreen(
    groupId: String,
    onBack: () -> Unit,
    onMemberClick: (String) -> Unit,
    onAddMemberClick: (String) -> Unit,
    viewModel: GroupMembersViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    GroupMembersContent(
        groupId = groupId,
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchChange = viewModel::onSearchQueryChange,
        onClearSearch = viewModel::onClearSearch,
        onRemoveMember = { userId -> viewModel.removeMember(userId, context) },
        onToggleAdmin = { userId -> viewModel.toggleAdmin(userId, context) },
        onBack = onBack,
        onMemberClick = onMemberClick,
        onAddMemberClick = { onAddMemberClick(groupId) },
        onRetry = viewModel::onRetry
    )
}

@Composable
fun GroupMembersContent(
    groupId: String,
    uiState: GroupMembersUiState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onToggleAdmin: (String) -> Unit,
    onBack: () -> Unit,
    onMemberClick: (String) -> Unit,
    onAddMemberClick: () -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg

    var isSearchActive by remember { mutableStateOf(false) }

    val memberCountText = when (uiState) {
        is GroupMembersUiState.Success -> "${uiState.admins.size + uiState.members.size} anggota"
        else -> "Memuat..."
    }

    val isUserAdmin = when (uiState) {
        is GroupMembersUiState.Success -> uiState.isCurrentUserAdmin
        else -> false
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
            ) {
                // TopAppBar: Height 56dp
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
                            .testTag("group_members_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (isSearchActive) {
                        // Inline search field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari anggota...",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 17.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_members_search_field")
                            )
                        }

                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    onClearSearch()
                                } else {
                                    isSearchActive = false
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // Standard title + actions
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = "Anggota",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = memberCountText,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("group_members_action_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (isUserAdmin) {
                            IconButton(
                                onClick = onAddMemberClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("group_members_action_add")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Tambah Anggota",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (isUserAdmin) {
                FloatingActionButton(
                    onClick = onAddMemberClick,
                    containerColor = TelegramBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_group_member")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Tambah Anggota"
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
                is GroupMembersUiState.Loading -> {
                    ShimmerCardList(cardCount = 6)
                }
                is GroupMembersUiState.Empty -> {
                    GroupMembersEmptyView()
                }
                is GroupMembersUiState.Error -> {
                    GroupMembersErrorView(
                        message = uiState.message,
                        onRetry = onRetry
                    )
                }
                is GroupMembersUiState.Success -> {
                    GroupMembersList(
                        admins = uiState.admins,
                        members = uiState.members,
                        isCurrentUserAdmin = uiState.isCurrentUserAdmin,
                        onMemberClick = onMemberClick,
                        onRemoveMember = onRemoveMember,
                        onToggleAdmin = onToggleAdmin
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupMembersList(
    admins: List<User>,
    members: List<User>,
    isCurrentUserAdmin: Boolean,
    onMemberClick: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onToggleAdmin: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color(0xFF1E2732) else Color(0xFFE8E8E8)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Section: ADMINISTRATORS
        if (admins.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "ADMINISTRATOR",
                    count = admins.size
                )
            }
            items(
                items = admins,
                key = { "admin_${it.id}" }
            ) { admin ->
                MemberItemRow(
                    user = admin,
                    isAdmin = true,
                    canManage = isCurrentUserAdmin && admin.id != "user_telegram_me",
                    onMemberClick = { onMemberClick(admin.id) },
                    onRemoveMember = { onRemoveMember(admin.id) },
                    onToggleAdmin = { onToggleAdmin(admin.id) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
            }
        }

        // Section: MEMBERS
        if (members.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "ANGGOTA",
                    count = members.size
                )
            }
            items(
                items = members,
                key = { "member_${it.id}" }
            ) { member ->
                MemberItemRow(
                    user = member,
                    isAdmin = false,
                    canManage = isCurrentUserAdmin && member.id != "user_telegram_me",
                    onMemberClick = { onMemberClick(member.id) },
                    onRemoveMember = { onRemoveMember(member.id) },
                    onToggleAdmin = { onToggleAdmin(member.id) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "$title ($count)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TelegramBlue,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun MemberItemRow(
    user: User,
    isAdmin: Boolean,
    canManage: Boolean,
    onMemberClick: () -> Unit,
    onRemoveMember: () -> Unit,
    onToggleAdmin: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1F1F1F)
    var showMenu by remember { mutableStateOf(false) }

    // Swipe-to-delete state
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe_offset")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        // Red background with delete action revealed on swipe left
        if (canManage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TelegramDangerRed)
                    .clickable {
                        offsetX = 0f
                        onRemoveMember()
                    }
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Anggota",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Foreground item row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .background(if (isDark) TelegramDarkChatBg else TelegramLightChatBg)
                .clickable(onClick = onMemberClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar 48dp
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isAdmin) Color(0xFF3498DB) else Color(0xFF9B59B6)),
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

            // Name & Status
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TelegramBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "admin",
                                color = TelegramBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

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

            // Context Action Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi Anggota",
                        tint = TelegramSubtext,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Kirim Pesan") },
                        leadingIcon = {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = TelegramBlue)
                        },
                        onClick = {
                            showMenu = false
                            onMemberClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Panggilan Suara") },
                        leadingIcon = {
                            Icon(Icons.Default.Call, contentDescription = null, tint = TelegramBlue)
                        },
                        onClick = {
                            showMenu = false
                            onMemberClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Panggilan Video") },
                        leadingIcon = {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = TelegramBlue)
                        },
                        onClick = {
                            showMenu = false
                            onMemberClick()
                        }
                    )
                    if (canManage) {
                        DropdownMenuItem(
                            text = { Text(if (isAdmin) "Hapus dari Admin" else "Jadikan Admin") },
                            leadingIcon = {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = TelegramBlue)
                            },
                            onClick = {
                                showMenu = false
                                onToggleAdmin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus dari Grup", color = TelegramDangerRed) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = TelegramDangerRed)
                            },
                            onClick = {
                                showMenu = false
                                onRemoveMember()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupMembersEmptyView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = TelegramSubtext.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tidak ada anggota yang cocok",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TelegramSubtext
        )
    }
}

@Composable
private fun GroupMembersErrorView(
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
            text = "Gagal memuat anggota",
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

@Preview(name = "Group Members Light")
@Composable
private fun GroupMembersLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            GroupMembersContent(
                groupId = "chat_2",
                uiState = GroupMembersUiState.Success(
                    group = Group(id = "chat_2", name = "Android Devs", memberCount = 2),
                    admins = listOf(
                        User(id = "1", name = "Pavel Durov", username = "durov", isOnline = true)
                    ),
                    members = listOf(
                        User(id = "2", name = "Alex Rivera", username = "alexrivera", isOnline = false, lastSeenFormatted = "15m lalu")
                    ),
                    isCurrentUserAdmin = true
                ),
                searchQuery = "",
                onSearchChange = {},
                onClearSearch = {},
                onRemoveMember = {},
                onToggleAdmin = {},
                onBack = {},
                onMemberClick = {},
                onAddMemberClick = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Group Members Dark")
@Composable
private fun GroupMembersDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            GroupMembersContent(
                groupId = "chat_2",
                uiState = GroupMembersUiState.Success(
                    group = Group(id = "chat_2", name = "Android Devs", memberCount = 2),
                    admins = listOf(
                        User(id = "1", name = "Pavel Durov", username = "durov", isOnline = true)
                    ),
                    members = emptyList(),
                    isCurrentUserAdmin = true
                ),
                searchQuery = "",
                onSearchChange = {},
                onClearSearch = {},
                onRemoveMember = {},
                onToggleAdmin = {},
                onBack = {},
                onMemberClick = {},
                onAddMemberClick = {},
                onRetry = {}
            )
        }
    }
}
