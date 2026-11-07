package com.jayra.app.ui.chatroom

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.model.Chat
import com.jayra.app.model.ChatTopic
import com.jayra.app.model.Contact
import com.jayra.app.model.Message
import com.jayra.app.model.MessageStatus
import com.jayra.app.model.Reaction
import com.jayra.app.model.RichTextEditorConfig
import com.jayra.app.model.RichTextFormatOption
import com.jayra.app.model.ChatRoomUiState
import com.jayra.app.security.SecurityRepository
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraOrange
import com.jayra.app.ui.theme.JayraSearchDark
import com.jayra.app.ui.theme.JayraStatusOnline
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.ui.viewmodel.ChatRoomViewModel
import com.jayra.app.util.MarkdownParser
import kotlinx.coroutines.launch

@Composable
fun ChatRoomScreen(
    chatId: String,
    onBack: () -> Unit,
    onContactInfoClick: (String) -> Unit,
    onNavigateToCreatePoll: (String) -> Unit = {},
    onNavigateToCreateChecklist: (String) -> Unit = {},
    onNavigateToVoiceTrimmer: (String) -> Unit = {},
    onNavigateToLocation: (String) -> Unit = {},
    onNavigateToContact: (String) -> Unit = {},
    onForwardMessage: (String, String) -> Unit = { _, _ -> },
    viewModel: ChatRoomViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(chatId) {
        viewModel.initChat(chatId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editorValue by viewModel.editorValue.collectAsStateWithLifecycle()
    val editorConfig by viewModel.editorConfig.collectAsStateWithLifecycle()
    val currentChat by viewModel.currentChat.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val selectedTopicId by viewModel.selectedTopicId.collectAsStateWithLifecycle()

    ChatRoomContent(
        chatId = chatId,
        uiState = uiState,
        currentChat = currentChat,
        topics = topics,
        selectedTopicId = selectedTopicId,
        onSelectTopic = viewModel::onSelectTopic,
        editorValue = editorValue,
        editorConfig = editorConfig,
        onEditorValueChange = viewModel::onEditorValueChange,
        onApplyFormat = viewModel::onApplyFormat,
        onBack = onBack,
        onContactInfoClick = { onContactInfoClick(chatId) },
        onSendMessage = viewModel::onSendMessage,
        onReaction = viewModel::onReaction,
        onToggleChecklistItem = viewModel::onToggleChecklistItem,
        onReply = viewModel::onReply,
        onDelete = viewModel::onDelete,
        onNavigateToCreatePoll = onNavigateToCreatePoll,
        onNavigateToCreateChecklist = onNavigateToCreateChecklist,
        onNavigateToVoiceTrimmer = onNavigateToVoiceTrimmer,
        onNavigateToLocation = onNavigateToLocation,
        onNavigateToContact = onNavigateToContact,
        onForwardMessage = onForwardMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomContent(
    chatId: String,
    uiState: ChatRoomUiState,
    onBack: () -> Unit,
    onContactInfoClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onReaction: (String, String) -> Unit,
    onReply: (Message?) -> Unit,
    onDelete: (String) -> Unit,
    currentChat: Chat? = null,
    topics: List<ChatTopic> = ChatTopic.DEFAULT_TOPICS,
    selectedTopicId: String = ChatTopic.GENERAL_TOPIC_ID,
    onSelectTopic: (String) -> Unit = {},
    editorValue: TextFieldValue? = null,
    editorConfig: RichTextEditorConfig = RichTextEditorConfig(),
    onEditorValueChange: ((TextFieldValue) -> Unit)? = null,
    onApplyFormat: ((RichTextFormatOption) -> Unit)? = null,
    onToggleChecklistItem: (String, String) -> Unit = { _, _ -> },
    onNavigateToCreatePoll: (String) -> Unit = {},
    onNavigateToCreateChecklist: (String) -> Unit = {},
    onNavigateToVoiceTrimmer: (String) -> Unit = {},
    onNavigateToLocation: (String) -> Unit = {},
    onNavigateToContact: (String) -> Unit = {},
    onForwardMessage: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val chatBg = if (isDark) JayraDarkChatBg else JayraLightChatBg

    var fallbackEditorValue by remember { mutableStateOf(TextFieldValue("")) }
    val activeEditorValue = editorValue ?: fallbackEditorValue
    val handleEditorChange: (TextFieldValue) -> Unit = onEditorValueChange ?: { updated ->
        fallbackEditorValue = updated
    }
    val handleApplyFormat: (RichTextFormatOption) -> Unit = onApplyFormat ?: { option ->
        fallbackEditorValue = MarkdownParser.applyFormatting(fallbackEditorValue, option)
    }

    var showTopMenu by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
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
    val contactName = contact?.name ?: currentChat?.name ?: "Chat"
    val contactStatus = contact?.status ?: if (currentChat?.isGroup == true) {
        "${currentChat.participants.size.coerceAtLeast(2)} anggota"
    } else {
        "online"
    }
    val showTopicTabs = currentChat?.isGroup == true && currentChat.hasTopics

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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
                            .background(JayraBlue)
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
                            color = JayraStatusOnline,
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

                // Fase 22.3: Topic Tabs jika chat.isGroup && chat.hasTopics == true
                if (showTopicTabs && topics.isNotEmpty()) {
                    val selectedIndex = topics.indexOfFirst { it.id == selectedTopicId }
                        .coerceAtLeast(0)
                    ScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = topBarBg,
                        contentColor = Color(0xFFFFFFFF),
                        edgePadding = 12.dp,
                        indicator = { tabPositions ->
                            if (selectedIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                    height = 3.dp,
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                        },
                        divider = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_topic_tabs")
                    ) {
                        topics.forEach { topic ->
                            val isSelected = topic.id == selectedTopicId
                            Tab(
                                selected = isSelected,
                                onClick = { onSelectTopic(topic.id) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("topic_tab_${topic.id}"),
                                selectedContentColor = Color(0xFFFFFFFF),
                                unselectedContentColor = Color(0xFFFFFFFF).copy(alpha = 0.72f),
                                text = {
                                    Text(
                                        text = "# ${topic.title}",
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val replyToMsg = (uiState as? ChatRoomUiState.Success)?.replyTo
            ChatInputBar(
                editorValue = activeEditorValue,
                onEditorValueChange = handleEditorChange,
                onApplyFormat = handleApplyFormat,
                onSend = {
                    val text = activeEditorValue.text
                    handleEditorChange(TextFieldValue(""))
                    onSendMessage(text)
                    scope.launch {
                        listState.animateScrollToItem(
                            ((uiState as? ChatRoomUiState.Success)?.messages?.size ?: 1)
                        )
                    }
                },
                replyTo = replyToMsg,
                onCancelReply = { onReply(null) },
                onAttachClick = { showAttachmentSheet = true },
                config = editorConfig
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
                        CircularProgressIndicator(color = JayraBlue)
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
                            tint = JayraSubtext,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No messages yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = JayraSubtext
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Say hi to start the conversation",
                            fontSize = 15.sp,
                            color = JayraSubtext
                        )
                    }
                }
                is ChatRoomUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.message, color = JayraSubtext, fontSize = 16.sp)
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
                                        label = "Jayra message",
                                        text = msg.text,
                                        scope = scope
                                    )
                                },
                                onForwardClick = { onForwardMessage(chatId, msg.id) },
                                onToggleChecklistItem = { itemId ->
                                    onToggleChecklistItem(msg.id, itemId)
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
                        .background(if (isDark) JayraSearchDark else Color.White)
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
                        tint = JayraBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showAttachmentSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF242F3D) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Lampirkan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.Poll,
                        label = "Poling",
                        backgroundColor = JayraBlue,
                        onClick = {
                            showAttachmentSheet = false
                            onNavigateToCreatePoll(chatId)
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Checklist,
                        label = "Tugas",
                        backgroundColor = JayraSuccessGreen,
                        onClick = {
                            showAttachmentSheet = false
                            onNavigateToCreateChecklist(chatId)
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Mic,
                        label = "Suara",
                        backgroundColor = JayraOrange,
                        onClick = {
                            showAttachmentSheet = false
                            onNavigateToVoiceTrimmer(chatId)
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.LocationOn,
                        label = "Lokasi",
                        backgroundColor = Color(0xFF4CAF50),
                        onClick = {
                            showAttachmentSheet = false
                            onNavigateToLocation(chatId)
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Person,
                        label = "Kontak",
                        backgroundColor = Color(0xFFFF9800),
                        onClick = {
                            showAttachmentSheet = false
                            onNavigateToContact(chatId)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AttachmentOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = JayraSubtext
        )
    }
}

@Preview(name = "Jayra Chat Room Light", showBackground = true)
@Composable
private fun ChatRoomLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            ChatRoomContent(
                chatId = "preview",
                uiState = ChatRoomUiState.Success(
                    messages = listOf(
                        Message(
                            id = "1",
                            senderId = "pavel",
                            senderName = "Pavel Durov",
                            text = "Selamat datang di **Jayra**! Coba format __miring__, ~~coret~~, `kode`, dan https://jayra.app",
                            timestamp = "10:45 AM",
                            dateGroup = "Today",
                            status = MessageStatus.READ,
                            reactions = listOf(Reaction("🔥", 4, true))
                        ),
                        Message(
                            id = "2",
                            senderId = "user_telegram_me",
                            senderName = "Me",
                            text = "Tampilan **Rich Text Editor** terlihat sangat presisi!",
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

@Preview(name = "Jayra Chat Room Dark", showBackground = true)
@Composable
private fun ChatRoomDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            ChatRoomContent(
                chatId = "preview",
                uiState = ChatRoomUiState.Success(
                    messages = listOf(
                        Message(
                            id = "1",
                            senderId = "pavel",
                            senderName = "Pavel Durov",
                            text = "Enkripsi `AES-256-GCM` aktif penuh di [Jayra Cloud](https://jayra.app).",
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

@Preview(name = "Jayra Group Topics Light", showBackground = true)
@Composable
private fun ChatRoomGroupTopicsLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            ChatRoomContent(
                chatId = "chat_2",
                uiState = ChatRoomUiState.Success(
                    messages = listOf(
                        Message(
                            id = "t1",
                            senderId = "sarah",
                            senderName = "Sarah Connor",
                            text = "Diskusi arsitektur modul **Topic Tabs** untuk rilis Jayra.",
                            timestamp = "11:15 AM",
                            dateGroup = "Today",
                            status = MessageStatus.READ,
                            topicId = "topik_1"
                        ),
                        Message(
                            id = "t2",
                            senderId = "user_telegram_me",
                            senderName = "Me",
                            text = "Filter pesan per topik sudah terhubung penuh ke `ChatRoomViewModel`.",
                            timestamp = "11:16 AM",
                            dateGroup = "Today",
                            status = MessageStatus.READ,
                            topicId = "topik_1"
                        )
                    ),
                    isTyping = false,
                    contact = Contact(
                        id = "chat_2",
                        name = "Android Developers ID",
                        status = "142 anggota, 18 online"
                    )
                ),
                currentChat = Chat(
                    id = "chat_2",
                    name = "Android Developers ID",
                    isGroup = true,
                    hasTopics = true
                ),
                topics = com.jayra.app.model.ChatTopic.DEFAULT_TOPICS,
                selectedTopicId = "topik_1",
                onSelectTopic = {},
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

@Preview(name = "Jayra Group Topics Dark", showBackground = true)
@Composable
private fun ChatRoomGroupTopicsDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            ChatRoomContent(
                chatId = "chat_2",
                uiState = ChatRoomUiState.Success(
                    messages = listOf(
                        Message(
                            id = "t1",
                            senderId = "sarah",
                            senderName = "Sarah Connor",
                            text = "Pengumuman rilis **Komunitas** tersedia di tab `General`.",
                            timestamp = "11:20 AM",
                            dateGroup = "Today",
                            status = MessageStatus.READ,
                            topicId = "general"
                        )
                    ),
                    isTyping = false,
                    contact = Contact(
                        id = "chat_2",
                        name = "Android Developers ID",
                        status = "142 anggota, 18 online"
                    )
                ),
                currentChat = Chat(
                    id = "chat_2",
                    name = "Android Developers ID",
                    isGroup = true,
                    hasTopics = true
                ),
                topics = com.jayra.app.model.ChatTopic.DEFAULT_TOPICS,
                selectedTopicId = "general",
                onSelectTopic = {},
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
