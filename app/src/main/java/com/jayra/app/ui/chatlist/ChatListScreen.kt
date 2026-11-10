package com.jayra.app.ui.chatlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.model.Chat
import com.jayra.app.model.ChatFilter
import com.jayra.app.model.ChatListUiState
import com.jayra.app.model.MessageStatus
import com.jayra.app.ui.viewmodel.ChatListViewModel
import com.jayra.app.util.iosGlass
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
    var showMenu by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedChats = remember { mutableStateListOf<String>() }
    var isPaused by remember { mutableStateOf(false) }

    // Blur background saat app di background (WhatsApp iOS style)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> isPaused = true
                Lifecycle.Event.ON_RESUME -> isPaused = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val bgColor = Color(0xFFF2F2F7)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ========== TOP BAR ==========
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    AppText(
                        text = "Cancel",
                        fontSize = 17.sp,
                        color = Color(0xFF0A84FF),
                        modifier = Modifier.clickable {
                            isSelectionMode = false
                            selectedChats.clear()
                        }
                    )
                } else {
                    AppText(
                        text = "Edit",
                        fontSize = 17.sp,
                        color = Color(0xFF0A84FF),
                        modifier = Modifier.clickable { isSelectionMode = true }
                    )
                }
                AppText(
                    text = if (isSelectionMode) "${selectedChats.size} Dipilih" else "Chat",
                    fontSize = if (isSelectionMode) 22.sp else 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color.Black
                )
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
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchQuery.isEmpty()) AppText("Search", fontSize = 17.sp, color = Color.Gray)
                                innerTextField()
                            }
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

            // ========== LIST ==========
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is ChatListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0A84FF)) }
                    is ChatListUiState.Empty -> EmptyState(onStartChat = onContactsClick)
                    is ChatListUiState.Success -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp)) {
                            items(state.chats, key = { it.id }, contentType = { "chat_item" }) { chat ->
                                SwipeableChatRow(
                                    chat = chat,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedChats.contains(chat.id),
                                    onClick = {
                                        if (isSelectionMode) {
                                            if (selectedChats.contains(chat.id)) selectedChats.remove(chat.id) else selectedChats.add(chat.id)
                                        } else {
                                            onChatClick(chat.id)
                                        }
                                    },
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

        // ========== FAB ==========
        if (!isSelectionMode) {
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

        // ========== ACTION BAR (Selection Mode) ==========
        if (isSelectionMode && selectedChats.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionBarItem(Icons.Rounded.Archive, "Arsipkan") { selectedChats.forEach { viewModel.archiveChat(it) }; selectedChats.clear() }
                ActionBarItem(Icons.Rounded.Delete, "Hapus", isDestructive = true) { selectedChats.forEach { viewModel.deleteChat(it) }; selectedChats.clear() }
                ActionBarItem(Icons.Rounded.MarkChatRead, "Tandai") { selectedChats.clear() }
            }
        }

        // ========== BLUR OVERLAY (App Background) ==========
        if (isPaused) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)))
        }
    }
}

@Composable
fun EmptyState(onStartChat: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Search, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        AppText("Belum ada chat", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        Spacer(Modifier.height(8.dp))
        AppText("Mulai percakapan baru dengan tombol di bawah", fontSize = 15.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStartChat,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            AppText("Mulai Chat", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SwipeableChatRow(
    chat: Chat,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "item_press"
    )

    Box {
        // Background swipe (archive kiri, delete kanan)
        Row(
            modifier = Modifier.fillMaxSize().background(
                if (offsetX > 0) Color(0xFF0A84FF) else Color(0xFFFF3B30)
            ),
            horizontalArrangement = if (offsetX > 0) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (offsetX > 0) {
                Icon(Icons.Rounded.Archive, null, tint = Color.White, modifier = Modifier.padding(start = 24.dp).size(28.dp))
            } else if (offsetX < 0) {
                Icon(Icons.Rounded.Delete, null, tint = Color.White, modifier = Modifier.padding(end = 24.dp).size(28.dp))
            }
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-200f, 200f)
                        },
                        onDragEnd = {
                            if (offsetX > 100f) onArchive()
                            else if (offsetX < -100f) onDelete()
                            offsetX = 0f
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(Color.White)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = { if (!isSelectionMode) showMenu = true }
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Search,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF0A84FF) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Box {
                    AsyncImage(
                        model = chat.avatarUrl.ifBlank { "https://ui-avatars.com/api/?name=${chat.name}&background=0A84FF&color=fff&size=112" },
                        contentDescription = chat.name,
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
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
                        when (chat.lastMessageStatus) {
                            MessageStatus.SENT -> Icon(Icons.Rounded.Check, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            MessageStatus.DELIVERED -> Icon(Icons.Rounded.DoneAll, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            MessageStatus.READ -> Icon(Icons.Rounded.DoneAll, null, tint = Color(0xFF0A84FF), modifier = Modifier.size(14.dp))
                            else -> {}
                        }
                        if (chat.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.size(20.dp).background(Color(0xFF25D366), CircleShape), contentAlignment = Alignment.Center) {
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
fun ActionSheetItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isDestructive: Boolean = false, onClick: () -> Unit) {
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
fun ActionBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(icon, null, tint = if (isDestructive) Color.Red else Color(0xFF0A84FF), modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        AppText(label, fontSize = 12.sp, color = if (isDestructive) Color.Red else Color(0xFF0A84FF))
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
