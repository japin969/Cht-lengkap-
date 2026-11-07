package com.jayra.app.ui.chatlist

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.Chat
import com.jayra.app.model.ChatFilter
import com.jayra.app.model.ChatListUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.ui.viewmodel.ChatListViewModel
import kotlin.math.roundToInt

@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onContactInfoClick: (String) -> Unit,
    onMyProfileClick: () -> Unit = {},
    onCallsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onSavedMessagesClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onStoriesClick: () -> Unit = {},
    viewModel: ChatListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    ChatListContent(
        uiState = uiState,
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onChatClick = onChatClick,
        onContactInfoClick = onContactInfoClick,
        onMyProfileClick = onMyProfileClick,
        onCallsClick = onCallsClick,
        onContactsClick = onContactsClick,
        onSearchClick = onSearchClick,
        onCreateGroupClick = onCreateGroupClick,
        onSavedMessagesClick = onSavedMessagesClick,
        onCommunitiesClick = onCommunitiesClick,
        onStoriesClick = onStoriesClick,
        onSwipeArchive = viewModel::onSwipeArchive,
        onSwipeDelete = viewModel::onSwipeDelete,
        onPinToggle = viewModel::onPinToggle,
        onMuteToggle = viewModel::onMuteToggle,
        onMarkAsRead = viewModel::onMarkAsRead,
        onRetry = viewModel::onRetry
    )
}

@Composable
fun ChatListContent(
    uiState: ChatListUiState,
    searchQuery: String,
    selectedFilter: ChatFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ChatFilter) -> Unit,
    onChatClick: (String) -> Unit,
    onContactInfoClick: (String) -> Unit,
    onMyProfileClick: () -> Unit,
    onCallsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onSavedMessagesClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onStoriesClick: () -> Unit = {},
    onSwipeArchive: (String) -> Unit,
    onSwipeDelete: (String) -> Unit,
    onPinToggle: (String) -> Unit,
    onMuteToggle: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    var showTopMenu by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(topBarBg)) {
                // TopAppBar (56dp height)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Jayra",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onStoriesClick,
                            modifier = Modifier.testTag("action_stories")
                        ) {
                            // GANTI SVG TELEGRAM
                            Icon(
                                imageVector = Icons.Default.AutoAwesomeMotion,
                                contentDescription = "Stories",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier.testTag("action_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showTopMenu = true },
                                modifier = Modifier.testTag("action_more")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Stories") },
                                    leadingIcon = {
                                        // GANTI SVG TELEGRAM
                                        Icon(Icons.Default.AutoAwesomeMotion, contentDescription = null, tint = JayraBlue)
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        onStoriesClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pesan Tersimpan") },
                                    leadingIcon = {
                                        // GANTI SVG TELEGRAM
                                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = JayraBlue)
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        onSavedMessagesClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Komunitas") },
                                    leadingIcon = {
                                        // GANTI SVG TELEGRAM
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = JayraBlue)
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        onCommunitiesClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Panggilan") },
                                    leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = JayraBlue) },
                                    onClick = {
                                        showTopMenu = false
                                        onCallsClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Kontak") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = JayraBlue) },
                                    onClick = {
                                        showTopMenu = false
                                        onContactsClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Group") },
                                    onClick = {
                                        showTopMenu = false
                                        onCreateGroupClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Secret Chat") },
                                    onClick = {
                                        showTopMenu = false
                                        android.widget.Toast.makeText(
                                            context,
                                            "Fitur Secret Chat akan segera hadir",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Channel") },
                                    onClick = {
                                        showTopMenu = false
                                        android.widget.Toast.makeText(
                                            context,
                                            "Fitur Channel akan segera hadir",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings / Profil Saya") },
                                    onClick = {
                                        showTopMenu = false
                                        onMyProfileClick()
                                    }
                                )
                            }
                        }
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    JayraSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange
                    )
                }

                // Filter Chips
                FilterChipRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterChange
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Jayra AI Pill (Meta AI Style)
                FloatingActionButton(
                    onClick = { /* Arahkan ke AssistantScreen nanti */ },
                    containerColor = Color(0xFF6E48AA),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Jayra AI",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // FAB Chat Baru
                FloatingActionButton(
                    onClick = onContactsClick,
                    containerColor = JayraBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_new_chat")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Message",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ChatListUiState.Loading -> {
                    ShimmerChatList(itemCount = 8)
                }
                is ChatListUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "No chats",
                            tint = JayraSubtext,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No chats yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = JayraSubtext
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start a new conversation",
                            fontSize = 15.sp,
                            color = JayraSubtext
                        )
                    }
                }
                is ChatListUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
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
                is ChatListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.chats,
                            key = { it.id },
                            contentType = { "chat_item" }
                        ) { chat ->
                            SwipeableChatItem(
                                chat = chat,
                                onClick = { onChatClick(chat.id) },
                                onArchive = { onSwipeArchive(chat.id) },
                                onDelete = { onSwipeDelete(chat.id) },
                                onTogglePin = { onPinToggle(chat.id) },
                                onToggleMute = { onMuteToggle(chat.id) },
                                onMarkAsRead = { onMarkAsRead(chat.id) },
                                onOpenInfo = { onContactInfoClick(chat.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableChatItem(
    chat: Chat,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onMarkAsRead: () -> Unit,
    onOpenInfo: () -> Unit
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(durationMillis = 200),
        label = "swipe_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        // Background swipe indicators
        if (animatedOffset > 0) {
            // Archive Background (Blue)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JayraBlue)
                    .padding(start = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archive",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (animatedOffset < 0) {
            // Delete Background (Red)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JayraDangerRed)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Foreground Chat Item Row
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxSize()
                .background(if (isSystemInDarkTheme()) Color(0xFF17212B) else Color.White)
        ) {
            ChatItemRow(
                chat = chat,
                onClick = onClick,
                onArchive = onArchive,
                onDelete = onDelete,
                onTogglePin = onTogglePin,
                onToggleMute = onToggleMute,
                onMarkAsRead = onMarkAsRead,
                onOpenInfo = onOpenInfo
            )
        }
    }
}

@Preview(name = "Jayra Chat List Light", showBackground = true)
@Composable
private fun ChatListLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            ChatListContent(
                uiState = ChatListUiState.Success(
                    listOf(
                        Chat(
                            id = "1",
                            name = "Pavel Durov",
                            lastMessage = "Jayra now supports voice and video calls",
                            timestamp = "10:45 AM",
                            unreadCount = 2,
                            isPinned = true,
                            isRead = false
                        ),
                        Chat(
                            id = "2",
                            name = "Android Devs",
                            lastMessage = "Jetpack Compose 1.8 update is live!",
                            timestamp = "Yesterday",
                            unreadCount = 0,
                            isPinned = false,
                            isRead = true
                        )
                    )
                ),
                searchQuery = "",
                selectedFilter = ChatFilter.ALL,
                onSearchQueryChange = {},
                onFilterChange = {},
                onChatClick = {},
                onContactInfoClick = {},
                onMyProfileClick = {},
                onSwipeArchive = {},
                onSwipeDelete = {},
                onPinToggle = {},
                onMuteToggle = {},
                onMarkAsRead = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Jayra Chat List Dark", showBackground = true)
@Composable
private fun ChatListDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            ChatListContent(
                uiState = ChatListUiState.Success(
                    listOf(
                        Chat(
                            id = "1",
                            name = "Pavel Durov",
                            lastMessage = "Jayra now supports voice and video calls",
                            timestamp = "10:45 AM",
                            unreadCount = 2,
                            isPinned = true,
                            isRead = false
                        )
                    )
                ),
                searchQuery = "",
                selectedFilter = ChatFilter.ALL,
                onSearchQueryChange = {},
                onFilterChange = {},
                onChatClick = {},
                onContactInfoClick = {},
                onMyProfileClick = {},
                onSwipeArchive = {},
                onSwipeDelete = {},
                onPinToggle = {},
                onMuteToggle = {},
                onMarkAsRead = {},
                onRetry = {}
            )
        }
    }
}
