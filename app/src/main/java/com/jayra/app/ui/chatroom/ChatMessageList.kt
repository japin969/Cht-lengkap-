package com.jayra.app.ui.chatroom

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.model.ChatRoomUiState
import com.jayra.app.security.SecurityRepository
import kotlinx.coroutines.launch

@Composable
fun ChatMessageList(
    chatId: String,
    uiState: ChatRoomUiState,
    onReaction: (String, String) -> Unit,
    onReply: (com.jayra.app.model.Message?) -> Unit,
    onDelete: (String) -> Unit,
    onToggleChecklistItem: (String, String) -> Unit,
    onForwardMessage: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)

    Box(modifier = modifier.fillMaxSize().background(bg)) {
        when (uiState) {
            is ChatRoomUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0A84FF))
            }
            is ChatRoomUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No messages yet", color = Color.Gray, fontSize = 17.sp)
            }
            is ChatRoomUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = Color.Red)
            }
            is ChatRoomUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
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
                                SecurityRepository.copyWithAutoClear(context, "Jayra message", msg.text, scope)
                            },
                            onForwardClick = { onForwardMessage(chatId, msg.id) },
                            onToggleChecklistItem = { itemId -> onToggleChecklistItem(msg.id, itemId) }
                        )
                    }
                    if (uiState.isTyping) {
                        item { TypingIndicatorBubble() }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
