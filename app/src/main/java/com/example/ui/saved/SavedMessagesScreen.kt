package com.example.ui.saved

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.model.Reaction
import com.example.model.SavedMessagesUiState
import com.example.security.SecurityRepository
import com.example.ui.chatroom.ChatInputBar
import com.example.ui.chatroom.DateSeparator
import com.example.ui.chatroom.MessageBubble
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSearchDark
import com.example.ui.theme.TelegramSearchLight
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme
import kotlinx.coroutines.launch

@Composable
fun SavedMessagesScreen(
    onBack: () -> Unit,
    onForwardMessage: (String, String) -> Unit = { _, _ -> },
    onNavigateToCreatePoll: (String) -> Unit = {},
    onNavigateToCreateChecklist: (String) -> Unit = {},
    onNavigateToVoiceTrimmer: (String) -> Unit = {},
    onNavigateToLocation: (String) -> Unit = {},
    onNavigateToContact: (String) -> Unit = {},
    viewModel: SavedMessagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSearchActive = (uiState as? SavedMessagesUiState.Success)?.isSearchActive == true

    BackHandler {
        if (isSearchActive) {
            viewModel.onToggleSearch(false)
        } else {
            onBack()
        }
    }

    SavedMessagesContent(
        uiState = uiState,
        onBack = onBack,
        onSendNote = viewModel::onSendNote,
        onReply = viewModel::onReply,
        onReaction = viewModel::onReaction,
        onToggleChecklistItem = viewModel::onToggleChecklistItem,
        onDelete = viewModel::onDelete,
        onForwardMessage = onForwardMessage,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onToggleSearch = viewModel::onToggleSearch,
        onRetry = viewModel::initializeSavedMessages,
        onAttachClick = {
            val chatId = (uiState as? SavedMessagesUiState.Success)?.chatId ?: viewModel.savedChatId
            onNavigateToCreateChecklist(chatId)
        }
    )
}

@Composable
fun SavedMessagesContent(
    uiState: SavedMessagesUiState,
    onBack: () -> Unit,
    onSendNote: (String) -> Unit,
    onReply: (Message?) -> Unit,
    onReaction: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onForwardMessage: (String, String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onToggleChecklistItem: (String, String) -> Unit = { _, _ -> },
    onAttachClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val chatBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val listState = rememberLazyListState()
    var noteInput by remember { mutableStateOf("") }

    val successState = uiState as? SavedMessagesUiState.Success

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = chatBg,
        topBar = {
            SavedMessagesTopBar(
                title = successState?.headerTitle ?: "Pesan Tersimpan",
                subtitle = successState?.headerSubtitle ?: "Penyimpanan cloud pribadi",
                isSearchActive = successState?.isSearchActive ?: false,
                searchQuery = successState?.searchQuery ?: "",
                onBack = onBack,
                onToggleSearch = onToggleSearch,
                onSearchQueryChange = onSearchQueryChange
            )
        },
        bottomBar = {
            if (successState != null) {
                Box(modifier = Modifier.imePadding()) {
                    ChatInputBar(
                        messageText = noteInput,
                        onMessageChange = { noteInput = it },
                        onSend = {
                            val textToSend = noteInput
                            noteInput = ""
                            onSendNote(textToSend)
                            scope.launch {
                                val nextIndex = successState.messages.size
                                listState.animateScrollToItem(nextIndex)
                            }
                        },
                        replyTo = successState.replyTo,
                        onCancelReply = { onReply(null) },
                        onAttachClick = onAttachClick
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(chatBg)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is SavedMessagesUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = TelegramBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                is SavedMessagesUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.message,
                            fontSize = 15.sp,
                            color = TelegramDangerRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TelegramBlue)
                                .clickable(onClick = onRetry)
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .testTag("saved_messages_retry_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Muat Ulang",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }

                is SavedMessagesUiState.Success -> {
                    if (uiState.messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SavedMessagesEmptyCard(
                                title = if (uiState.searchQuery.isNotBlank()) {
                                    "Tidak ada catatan cocok"
                                } else {
                                    uiState.emptyTitle
                                },
                                highlights = if (uiState.searchQuery.isNotBlank()) {
                                    listOf("Tidak ditemukan pesan untuk \"${uiState.searchQuery}\"")
                                } else {
                                    uiState.emptyHighlights
                                }
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("saved_messages_list")
                        ) {
                            item(key = "saved_date_header") {
                                DateSeparator(dateText = "Hari Ini")
                            }

                            items(
                                items = uiState.messages,
                                key = { it.id },
                                contentType = { "saved_message_item" }
                            ) { msg ->
                                MessageBubble(
                                    message = msg,
                                    isOutgoing = true,
                                    onReactionClick = { emoji -> onReaction(msg.id, emoji) },
                                    onReplyClick = { onReply(msg) },
                                    onDeleteClick = { onDelete(msg.id) },
                                    onCopyClick = {
                                        SecurityRepository.copyWithAutoClear(
                                            context = context,
                                            label = "Saved Message",
                                            text = msg.text,
                                            scope = scope
                                        )
                                    },
                                    onForwardClick = {
                                        onForwardMessage(uiState.chatId, msg.id)
                                    },
                                    onToggleChecklistItem = { itemId ->
                                        onToggleChecklistItem(msg.id, itemId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedMessagesTopBar(
    title: String,
    subtitle: String,
    isSearchActive: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val searchFieldBg = if (isDark) TelegramSearchDark else TelegramSearchLight
    val searchTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isSearchActive) onToggleSearch(false) else onBack()
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("saved_messages_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TelegramBlue),
                contentAlignment = Alignment.Center
            ) {
                // GANTI SVG TELEGRAM
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = title,
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFFFFFFFF).copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onToggleSearch(!isSearchActive) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("saved_messages_search_button")
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchActive) "Tutup pencarian" else "Cari di Pesan Tersimpan",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(searchFieldBg)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TelegramSubtext,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari catatan tersimpan...",
                                fontSize = 14.sp,
                                color = TelegramSubtext
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = searchTextColor
                            ),
                            cursorBrush = SolidColor(TelegramBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_messages_search_input")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedMessagesEmptyCard(
    title: String,
    highlights: List<String>
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) TelegramCardDark else TelegramCardLight
    val titleColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val bodyColor = if (isDark) Color(0xFFD1D1D6) else Color(0xFF3A3A3C)

    Column(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TelegramBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            // GANTI SVG TELEGRAM
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = title,
                tint = TelegramBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            highlights.forEach { itemText ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // GANTI SVG TELEGRAM
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = TelegramBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = itemText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = bodyColor
                    )
                }
            }
        }
    }
}

// ================= PREVIEWS (LIGHT & DARK) =================

@Preview(name = "Saved Messages TopBar Light", showBackground = true)
@Composable
private fun SavedMessagesTopBarLightPreview() {
    TelegramTheme(darkTheme = false) {
        SavedMessagesTopBar(
            title = "Pesan Tersimpan",
            subtitle = "3 catatan tersimpan",
            isSearchActive = true,
            searchQuery = "Jadwal",
            onBack = {},
            onToggleSearch = {},
            onSearchQueryChange = {}
        )
    }
}

@Preview(name = "Saved Messages TopBar Dark", showBackground = true)
@Composable
private fun SavedMessagesTopBarDarkPreview() {
    TelegramTheme(darkTheme = true) {
        SavedMessagesTopBar(
            title = "Pesan Tersimpan",
            subtitle = "3 catatan tersimpan",
            isSearchActive = false,
            searchQuery = "",
            onBack = {},
            onToggleSearch = {},
            onSearchQueryChange = {}
        )
    }
}

@Preview(name = "Saved Messages Empty Card Light", showBackground = true)
@Composable
private fun SavedMessagesEmptyCardLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface(color = TelegramLightChatBg) {
            Box(modifier = Modifier.padding(16.dp)) {
                SavedMessagesEmptyCard(
                    title = "Penyimpanan Cloud Anda",
                    highlights = listOf(
                        "Simpan pesan, tautan, dan catatan pribadi di sini",
                        "Teruskan pesan penting dari obrolan mana pun"
                    )
                )
            }
        }
    }
}

@Preview(name = "Saved Messages Empty Card Dark", showBackground = true)
@Composable
private fun SavedMessagesEmptyCardDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface(color = TelegramDarkChatBg) {
            Box(modifier = Modifier.padding(16.dp)) {
                SavedMessagesEmptyCard(
                    title = "Penyimpanan Cloud Anda",
                    highlights = listOf(
                        "Simpan pesan, tautan, dan catatan pribadi di sini",
                        "Teruskan pesan penting dari obrolan mana pun"
                    )
                )
            }
        }
    }
}

@Preview(name = "Saved Messages Screen Light", showBackground = true)
@Composable
private fun SavedMessagesScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        SavedMessagesContent(
            uiState = SavedMessagesUiState.Success(
                chatId = "uid_me",
                headerTitle = "Pesan Tersimpan",
                headerSubtitle = "2 catatan tersimpan",
                messages = listOf(
                    Message(
                        id = "sm_1",
                        chatId = "uid_me",
                        senderId = "uid_me",
                        senderName = "Me",
                        text = "Catatan rapat arsitektur Compose 14.00 WIB",
                        timestamp = "09:15 AM",
                        status = MessageStatus.READ,
                        reactions = listOf(Reaction("📌", 1, true))
                    ),
                    Message(
                        id = "sm_2",
                        chatId = "uid_me",
                        senderId = "uid_me",
                        senderName = "Me",
                        text = "Jangan lupa rilis update keamanan enkripsi AES-256.",
                        timestamp = "09:18 AM",
                        status = MessageStatus.SENT,
                        forwardedFrom = "Pavel Durov"
                    )
                )
            ),
            onBack = {},
            onSendNote = {},
            onReply = {},
            onReaction = { _, _ -> },
            onDelete = {},
            onForwardMessage = { _, _ -> },
            onSearchQueryChange = {},
            onToggleSearch = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Saved Messages Screen Dark", showBackground = true)
@Composable
private fun SavedMessagesScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        SavedMessagesContent(
            uiState = SavedMessagesUiState.Success(
                chatId = "uid_me",
                headerTitle = "Pesan Tersimpan",
                headerSubtitle = "Penyimpanan cloud pribadi",
                messages = emptyList()
            ),
            onBack = {},
            onSendNote = {},
            onReply = {},
            onReaction = { _, _ -> },
            onDelete = {},
            onForwardMessage = { _, _ -> },
            onSearchQueryChange = {},
            onToggleSearch = {},
            onRetry = {}
        )
    }
}
