package com.jayra.app.ui.chatlist

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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.MarkChatRead
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.jayra.app.ui.viewmodel.ChatListViewModel
import com.jayra.app.util.iosGlass

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    
    val bgColor = Color(0xFFF2F2F7)
    val surfaceColor = Color.White

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // iOS Large Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "Chat",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color.Black
                )
                Icon(
                    imageVector = Icons.Rounded.AutoAwesomeMotion,
                    contentDescription = "New Chat",
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(24.dp).clickable { onContactsClick() }
                )
            }

            // iOS Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(38.dp)
                    .background(Color(0xFFE5E5EA), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    AppText("Cari", color = Color.Gray, fontSize = 17.sp)
                }
            }

            // iOS Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .background(Color(0xFFE5E5EA), RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChatFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .background(if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { viewModel.onFilterChange(filter) },
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = filter.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color.Gray
                        )
                    }
                }
            }

            // List
            when (val state = uiState) {
                is ChatListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0A84FF)) }
                is ChatListUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppText("Belum ada chat", color = Color.Gray, fontSize = 17.sp) }
                is ChatListUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
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

        // Floating Buttons
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val interactionAi = remember { MutableInteractionSource() }
            val pressedAi by interactionAi.collectIsPressedAsState()
            val scaleAi by animateFloatAsState(if (pressedAi) 0.9f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(scaleAi)
                    .iosGlass(cornerRadius = 26, tint = Color(0xFF6E48AA).copy(alpha = 0.95f))
                    .clickable(interactionSource = interactionAi, indication = null) { onAiClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesomeMotion, "Jayra AI", tint = Color.White, modifier = Modifier.size(24.dp))
            }

            val interactionFab = remember { MutableInteractionSource() }
            val pressedFab by interactionFab.collectIsPressedAsState()
            val scaleFab by animateFloatAsState(if (pressedFab) 0.9f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(scaleFab)
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
            // Avatar + online indicator
            Box {
                AsyncImage(
                    model = chat.avatarUrl.ifBlank { "https://ui-avatars.com/api/?name=${chat.name}&background=0A84FF&color=fff&size=112" },
                    contentDescription = chat.name,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(Color(0xFF25D366), CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = if (chat.hasDraft) Color(0xFF25D366) else Color.Gray
                    )
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(20.dp)
                                .background(Color(0xFF25D366), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        // Separator inset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 84.dp)
                .height(0.5.dp)
                .background(Color(0xFFE5E5EA))
        )
    }
    
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Text(chat.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isDestructive) Color.Red else Color(0xFF0A84FF), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 17.sp, color = if (isDestructive) Color.Red else Color.Black)
    }
}


// AppText: Text dengan font SF Pro default
@androidx.compose.runtime.Composable
private fun AppText(
    text: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = com.jayra.app.ui.theme.SFProFontFamily,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign
    )
}
