package com.example.ui.chatroom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.model.Poll
import com.example.model.PollOption
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBubbleInDark
import com.example.ui.theme.TelegramBubbleInLight
import com.example.ui.theme.TelegramBubbleOutDark
import com.example.ui.theme.TelegramBubbleOutLight
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramInputDark
import com.example.ui.theme.TelegramInputLight
import com.example.ui.theme.TelegramReplyDark
import com.example.ui.theme.TelegramReplyLight
import com.example.ui.theme.TelegramSubtext

@Composable
fun DateSeparator(dateText: String) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.22f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOutgoing: Boolean,
    onReactionClick: (String) -> Unit,
    onReplyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var showMenu by remember { mutableStateOf(false) }

    // Bubble shape with authentic Telegram tail
    // Bubble Out: 0dp tail at bottomEnd
    // Bubble In: 0dp tail at bottomStart
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp)
    }

    val bubbleBg = if (isOutgoing) {
        if (isDark) TelegramBubbleOutDark else TelegramBubbleOutLight
    } else {
        if (isDark) TelegramBubbleInDark else TelegramBubbleInLight
    }

    val textColor = if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(elevation = 1.dp, shape = bubbleShape)
                .clip(bubbleShape)
                .background(bubbleBg)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("bubble_${message.id}")
        ) {
            Column {
                // Group Sender Name if incoming
                if (!isOutgoing && message.senderName.isNotBlank() && message.senderName != "Me") {
                    Text(
                        text = message.senderName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Reply Preview inside bubble
                if (!message.replyToText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) TelegramReplyDark else TelegramReplyLight)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(TelegramBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = message.replyToSender ?: "Reply",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TelegramBlue,
                                    maxLines = 1
                                )
                                Text(
                                    text = message.replyToText,
                                    fontSize = 12.sp,
                                    color = TelegramSubtext,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Message Content (Poll, Location, Contact, or standard Text)
                val isPoll = message.text.startsWith("📊 Poling:") || message.text.startsWith("🧠 Kuis:")
                val isLocation = message.text.startsWith("📍 [Lokasi]")
                val isContact = message.text.startsWith("👤 [Kontak]")

                if (isPoll) {
                    val lines = message.text.lines()
                    val header = lines.firstOrNull() ?: ""
                    val isQuizMode = header.startsWith("🧠 Kuis:")
                    val questionText = header.substringAfter(": ").trim()
                    val optionItems = lines.drop(1).filter { it.isNotBlank() }.map { line ->
                        val textOnly = line.replaceFirst(Regex("^\\d+\\.\\s*"), "")
                        PollOption(text = textOnly, voteCount = 0)
                    }
                    if (optionItems.isNotEmpty()) {
                        PollBubble(
                            poll = Poll(
                                id = message.id,
                                question = questionText.ifBlank { "Poling" },
                                options = optionItems,
                                isQuiz = isQuizMode,
                                isAnonymous = !isQuizMode
                            )
                        )
                    } else {
                        Text(
                            text = message.text,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = textColor
                        )
                    }
                } else if (isLocation) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Lokasi Dibagikan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelegramBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.text.substringAfter("📍 [Lokasi]").trim(),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = textColor
                        )
                    }
                } else if (isContact) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Kartu Kontak",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TelegramBlue
                                )
                                Text(
                                    text = message.text.substringAfter("👤 [Kontak]").trim(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = message.text,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = textColor
                    )
                }

                // Time & Status Row
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        fontSize = 12.sp,
                        color = TelegramSubtext
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> {
                                Text(
                                    text = "...",
                                    fontSize = 11.sp,
                                    color = TelegramSubtext
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Sent",
                                    tint = TelegramSubtext,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.READ, MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read",
                                    tint = TelegramBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.FAILED -> {
                                Text(
                                    text = "!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TelegramDangerRed
                                )
                            }
                        }
                    }
                }
            }

            // Context Menu for message
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Reply") },
                    onClick = { onReplyClick(); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = { onCopyClick(); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("👍 Like") },
                    onClick = { onReactionClick("👍"); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("❤️ Heart") },
                    onClick = { onReactionClick("❤️"); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("🔥 Fire") },
                    onClick = { onReactionClick("🔥"); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = TelegramDangerRed) },
                    onClick = { onDeleteClick(); showMenu = false }
                )
            }
        }

        // Reactions container (overlapping 4dp below bubble)
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .offset(y = (-4).dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.forEach { reaction ->
                    Box(
                        modifier = Modifier
                            .shadow(1.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) TelegramBubbleInDark else Color.White)
                            .clickable { onReactionClick(reaction.emoji) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = reaction.emoji, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = reaction.count.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (reaction.userReacted) FontWeight.Bold else FontWeight.Normal,
                                color = if (reaction.userReacted) TelegramBlue else TelegramSubtext
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) TelegramBubbleInDark else TelegramBubbleInLight

    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot3"
    )

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(y = dot1.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TelegramSubtext)
            )
            Box(
                modifier = Modifier
                    .offset(y = dot2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TelegramSubtext)
            )
            Box(
                modifier = Modifier
                    .offset(y = dot3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TelegramSubtext)
            )
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    replyTo: Message?,
    onCancelReply: () -> Unit,
    onAttachClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) TelegramInputDark else TelegramInputLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
    ) {
        // Reply bar banner
        if (replyTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(32.dp)
                        .background(TelegramBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reply to ${replyTo.senderName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue
                    )
                    Text(
                        text = replyTo.text,
                        fontSize = 12.sp,
                        color = TelegramSubtext,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        tint = TelegramSubtext,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("attach_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = TelegramSubtext,
                    modifier = Modifier.size(24.dp)
                )
            }

            BasicTextField(
                value = messageText,
                onValueChange = onMessageChange,
                maxLines = 5,
                cursorBrush = SolidColor(TelegramBlue),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color.Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("chat_input_textfield"),
                decorationBox = { innerTextField ->
                    if (messageText.isEmpty()) {
                        Text(
                            text = "Message",
                            fontSize = 16.sp,
                            color = TelegramSubtext
                        )
                    }
                    innerTextField()
                }
            )

            IconButton(
                onClick = { /* Emoji picker */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = "Emoji",
                    tint = TelegramSubtext,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = messageText.isNotBlank(),
                enter = scaleIn(tween(150)) + fadeIn(),
                exit = scaleOut(tween(150)) + fadeOut()
            ) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = TelegramBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
