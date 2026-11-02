package com.example.ui.chatroom

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Contact
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.model.Reaction
import com.example.model.ChatRoomUiState
import com.example.security.SecurityRepository
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSearchDark
import com.example.ui.theme.TelegramStatusOnline
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme
import com.example.ui.viewmodel.ChatRoomViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatRoomScreen(
    chatId: String,
    onBack: () -> Unit,
    onContactInfoClick: (String) -> Unit,
    viewModel: ChatRoomViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(chatId) {
        viewModel.initChat(chatId)
    }

    val uiState by viewModel.uiState.collectAsState()

    ChatRoomContent(
        chatId = chatId,
        uiState = uiState,
        onBack = onBack,
        onContactInfoClick = { onContactInfoClick(chatId) },
        onSendMessage = viewModel::onSendMessage,
        onReaction = viewModel::onReaction,
        onReply = viewModel::onReply,
        onDelete = viewModel::onDelete
    )
}

@Composable
fun ChatRoomContent(
    chatId: String,
    uiState: ChatRoomUiState,
    onBack: () -> Unit,
    onContactInfoClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onReaction: (String, String) -> Unit,
    onReply: (Message?) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val chatBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg

    var messageInput by remember { mutableStateOf("") }
    var showTopMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val showScrollToBottom by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == 0) false
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible < totalItems - 2
            }
        }
    }

    val contact = (uiState as? ChatRoomUiState.Success)?.contact
    val contactName = contact?.name ?: "Chat"
    val contactStatus = contact?.status ?: "online"

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (tap target 48dp)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_room_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Header Profile Area (Clickable to open Contact Info)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onContactInfoClick)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar 36dp
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TelegramBlue)
                    ) {
                        if (contact?.avatarUrl?.isNotBlank() == true) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(contact.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = contactName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = contactName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = contactName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = contactStatus,
                            fontSize = 13.sp,
                            color = TelegramStatusOnline,
                            maxLines = 1
                        )
                    }
                }

                // Action Icons (Video, Voice, Menu with 16dp spacing)
                IconButton(onClick = { /* Video Call */ }) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { /* Voice Call */ }) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box {
                    IconButton(onClick = { showTopMenu = true }) {
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
                            text = { Text("Clear History") },
                            onClick = { showTopMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Mute Notifications") },
                            onClick = { showTopMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Search in Chat") },
                            onClick = { showTopMenu = false }
                        )
                    }
                }
            }
        },
        bottomBar = {
            val replyToMsg = (uiState as? ChatRoomUiState.Success)?.replyTo
            ChatInputBar(
                messageText = messageInput,
                onMessageChange = { messageInput = it },
                onSend = {
                    val text = messageInput
                    messageInput = ""
                    onSendMessage(text)
                    scope.launch {
                        listState.animateScrollToItem(
                            ((uiState as? ChatRoomUiState.Success)?.messages?.size ?: 1)
                        )
                    }
                },
                replyTo = replyToMsg,
                onCancelReply = { onReply(null) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(chatBg)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ChatRoomUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TelegramBlue)
                    }
                }
                is ChatRoomUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Empty chat",
                            tint = TelegramSubtext,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No messages yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = TelegramSubtext
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Say hi to start the conversation",
                            fontSize = 15.sp,
                            color = TelegramSubtext
                        )
                    }
                }
                is ChatRoomUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.message, color = TelegramSubtext, fontSize = 16.sp)
                    }
                }
                is ChatRoomUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            DateSeparator(dateText = "Today")
                        }

                        items(
                            items = uiState.messages,
                            key = { it.id },
                            contentType = { "message_item" }
                        ) { msg ->
                            val isOutgoing = msg.senderId == "user_telegram_me" || msg.senderName == "Me"
                            MessageBubble(
                                message = msg,
                                isOutgoing = isOutgoing,
                                onReactionClick = { emoji -> onReaction(msg.id, emoji) },
                                onReplyClick = { onReply(msg) },
                                onDeleteClick = { onDelete(msg.id) },
                                onCopyClick = {
                                    SecurityRepository.copyWithAutoClear(
                                        context = context,
                                        label = "Telegram message",
                                        text = msg.text,
                                        scope = scope
                                    )
                                }
                            )
                        }

                        if (uiState.isTyping) {
                            item {
                                TypingIndicatorBubble()
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Scroll to Bottom Floating Action Button
            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 40 }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) TelegramSearchDark else Color.White)
                        .clickable {
                            val count = (uiState as? ChatRoomUiState.Success)?.messages?.size ?: 0
                            if (count > 0) {
                                scope.launch { listState.animateScrollToItem(count - 1) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        tint = TelegramBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "Telegram Chat Room Light", showBackground = true)
@Composable
private fun ChatRoomLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            ChatRoomContent(
                chatId = "preview",
                uiState = ChatRoomUiState.Success(
                    messages = listOf(
                        Message(
                            id = "1",
                            senderId = "pavel",
                            senderName = "Pavel Durov",
                            text = "Welcome to the Telegram Compose experience!",
                            timestamp = "10:45 AM",
                            dateGroup = "Today",
                            status = MessageStatus.READ,
                            reactions = listOf(Reaction("🔥", 4, true))
                        ),
                        Message(
                            id = "2",
                            senderId = "user_telegram_me",
                            senderName = "Me",
                            text = "Looks 99% authentic to the native app!",
                            timestamp = "10:46 AM",
                            dateGroup = "Today",
                            status = MessageStatus.READ
                        )
                    ),
                    isTyping = false,
                    contact = Contact(name = "Pavel Durov", status = "online")
                ),
                onBack = {},
                onContactInfoClick = {},
                onSendMessage = {},
                onReaction = { _, _ -> },
                onReply = {},
                onDelete = {}
            )
        }
    }
}

@Preview(name = "Telegram Chat Room Dark", showBackground = true)
@Composable
private fun ChatRoomDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            ChatRoomContent(
                chatId = "preview",
                uiState = ChatRoomUiState.Success(
                    messages = listOf(
                        Message(
                            id = "1",
                            senderId = "pavel",
                            senderName = "Pavel Durov",
                            text = "End-to-end hardware encryption is active.",
                            timestamp = "10:45 AM",
                            dateGroup = "Today",
                            status = MessageStatus.READ
                        )
                    ),
                    isTyping = true,
                    contact = Contact(name = "Pavel Durov", status = "online")
                ),
                onBack = {},
                onContactInfoClick = {},
                onSendMessage = {},
                onReaction = { _, _ -> },
                onReply = {},
                onDelete = {}
            )
        }
    }
}
