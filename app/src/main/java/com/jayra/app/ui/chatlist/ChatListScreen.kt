package com.jayra.app.ui.chatlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MarkChatRead
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.model.Chat
import com.jayra.app.model.ChatFilter
import com.jayra.app.model.ChatListUiState
import com.jayra.app.model.MessageStatus
import com.jayra.app.ui.viewmodel.ChatListViewModel
import com.jayra.app.util.iosGlass

@OptIn(ExperimentalMaterial3Api::class)
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
    onAiClick: () -> Unit = {},
    viewModel: ChatListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val bgColor = Color(0xFFF2F2F7)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ========== TOP BAR ==========
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit (kiri)
                AppText(
                    text = "Edit",
                    fontSize = 17.sp,
                    color = Color(0xFF0A84FF),
                    modifier = Modifier.clickable { /* mode seleksi nanti */ }
                )
                // Large Title
                AppText(
                    text = "Chat",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color.Black
                )
                // Add & Menu (kanan)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New Chat",
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(26.dp).clickable { onContactsClick() }
                    )
                    Spacer(Modifier.width(16.dp))
                    Box {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Menu",
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(26.dp).clickable { showMenu = true }
                        )
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { AppText("New Group") }, onClick = { showMenu = false; onCreateGroupClick() })
                            DropdownMenuItem(text = { AppText("New Broadcast") }, onClick = { showMenu = false })
                            DropdownMenuItem(text = { AppText("Linked Devices") }, onClick = { showMenu = false })
                            DropdownMenuItem(text = { AppText("Starred Messages") }, onClick = { showMenu = false; onSavedMessagesClick() })
                            DropdownMenuItem(text = { AppText("Settings") }, onClick = { showMenu = false; onMyProfileClick() })
                        }
                    }
                }
            }

            // ========== SEARCH BAR ==========
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(36.dp)
                    .background(Color(0xFFE5E5EA), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = Color.Black, fontFamily = com.jayra.app.ui.theme.SFProFontFamily),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) AppText("Search", fontSize = 17.sp, color = Color.Gray)
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Clear",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp).clickable { viewModel.onSearchQueryChange("") }
                        )
                    }
                }
            }

            // ========== FILTER CHIP ==========
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF25D366) else Color(0xFFE5E5EA),
                        label = "chip_bg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.Gray,
                        label = "chip_text"
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .clickable { viewModel.onFilterChange(filter) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        AppText(filter.label, fontSize = 14.sp, color = textColor, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            // ========== LIST + PULL TO REFRESH ==========
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.loadChats()
                    isRefreshing = false
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is ChatListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0A84FF)) }
                    is ChatListUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppText("Belum ada chat", color = Color.Gray, fontSize = 17.sp) }
                    is ChatListUiState.Success -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp)) {
                            items(state.chats, key = { it.id }, contentType = { "chat_item" }) { chat ->
                                IOSChatItemRow(
                                    chat = chat,
                                    onClick = { onChatClick(chat.id) },
                                    onDelete = { viewModel.deleteChat(chat.id) },
                                    onArchive = { viewModel.archiveChat(chat.id) },
                                    onPin = { viewModel.togglePin(chat.id) },
                                    onMute = { viewModel.toggleMute(chat.id) }
                                )
                            }
                        }
                    }
                    is ChatListUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppText("Error: ${state.message}", color = Color.Red) }
                }
            }
        }

        // ========== FAB (AI + Compose) ==========
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val interactionAi = remember { MutableInteractionSource() }
            val pressedAi by interactionAi.collectIsPressedAsState()
            val scaleAi by animateFloatAsState(if (pressedAi) 0.9f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f))
            Box(
                modifier = Modifier.size(52.dp).graphicsLayer { scaleX = scaleAi; scaleY = scaleAi }
                    .iosGlass(cornerRadius = 26, tint = Color(0xFF6E48AA).copy(alpha = 0.95f))
                    .clickable(interactionSource = interactionAi, indication = null) { onAiClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, "Jayra AI", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            val interactionFab = remember { MutableInteractionSource() }
            val pressedFab by interactionFab.collectIsPressedAsState()
            val scaleFab by animateFloatAsState(if (pressedFab) 0.9f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f))
            Box(
                modifier = Modifier.size(52.dp).graphicsLayer { scaleX = scaleFab; scaleY = scaleFab }
                    .background(Color(0xFF0A84FF), CircleShape)
                    .clickable(interactionSource = interactionFab, indication = null) { onContactsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Edit, "New Chat", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IOSChatItemRow(
    chat: Chat,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "item_press"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(Color.White)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = chat.avatarUrl.ifBlank { "https://ui-avatars.com/api/?name=${chat.name}&background=0A84FF&color=fff&size=112" },
                    contentDescription = chat.name,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                if (chat.isOnline) {
                    Box(
                        modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                            .background(Color.White, CircleShape).padding(2.dp)
                            .background(Color(0xFF25D366), CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppText(chat.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chat.isPinned) {
                            Icon(Icons.Rounded.PushPin, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        if (chat.isMuted) {
                            Icon(Icons.Rounded.NotificationsOff, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        AppText(chat.timestamp, fontSize = 13.sp, color = Color.Gray)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.hasDraft) {
                        AppText("Draft: ", fontSize = 15.sp, color = Color(0xFF25D366), fontWeight = FontWeight.SemiBold)
                    }
                    AppText(
                        text = if (chat.hasDraft) chat.draftText else chat.lastMessage,
                        fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        color = if (chat.hasDraft) Color(0xFF25D366) else Color.Gray
                    )
                    // Centang status pesan terakhir
                    when (chat.lastMessageStatus) {
                        MessageStatus.SENT -> Icon(Icons.Rounded.Check, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        MessageStatus.DELIVERED -> Icon(Icons.Rounded.DoneAll, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        MessageStatus.READ -> Icon(Icons.Rounded.DoneAll, null, tint = Color(0xFF0A84FF), modifier = Modifier.size(14.dp))
                        else -> {}
                    }
                    if (chat.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(20.dp).background(Color(0xFF25D366), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(start = 84.dp).height(0.5.dp).background(Color(0xFFE5E5EA)))
    }

    if (showMenu) {
        ModalBottomSheet(onDismissRequest = { showMenu = false }, containerColor = Color.White, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                AppText(chat.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                ActionSheetItem(Icons.Rounded.MarkChatRead, "Tandai Dibaca") { showMenu = false }
                ActionSheetItem(Icons.Rounded.PushPin, if (chat.isPinned) "Lepas Pin" else "Sematkan") { onPin(); showMenu = false }
                ActionSheetItem(Icons.Rounded.NotificationsOff, if (chat.isMuted) "Bunyikan" else "Bisukan") { onMute(); showMenu = false }
                ActionSheetItem(Icons.Rounded.Archive, "Arsipkan") { onArchive(); showMenu = false }
                ActionSheetItem(Icons.Rounded.Delete, "Hapus", isDestructive = true) { onDelete(); showMenu = false }
            }
        }
    }
}

@Composable
fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isDestructive) Color.Red else Color(0xFF0A84FF), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        AppText(label, fontSize = 17.sp, color = if (isDestructive) Color.Red else Color.Black)
    }
}

@Composable
private fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = com.jayra.app.ui.theme.SFProFontFamily,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null
) {
    androidx.compose.material3.Text(
        text = text, modifier = modifier, color = color, fontSize = fontSize,
        fontWeight = fontWeight, fontFamily = fontFamily, letterSpacing = letterSpacing,
        maxLines = maxLines, overflow = overflow, textAlign = textAlign
    )
}
