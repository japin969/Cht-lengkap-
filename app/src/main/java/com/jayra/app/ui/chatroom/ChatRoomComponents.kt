package com.jayra.app.ui.chatroom

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.jayra.app.model.Poll
import com.jayra.app.model.PollOption
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.model.Message
import com.jayra.app.model.MessageStatus
import com.jayra.app.model.Reaction
import com.jayra.app.model.RichTextEditorConfig
import com.jayra.app.model.RichTextFormatOption
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraBubbleInDark
import com.jayra.app.ui.theme.JayraBubbleInLight
import com.jayra.app.ui.theme.JayraBubbleOutDark
import com.jayra.app.ui.theme.JayraBubbleOutLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraInputDark
import com.jayra.app.ui.theme.JayraInputLight
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraReplyDark
import com.jayra.app.ui.theme.JayraReplyLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.util.MarkdownParser

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
    onCopyClick: () -> Unit,
    onForwardClick: () -> Unit = {},
    onToggleChecklistItem: (String) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var showMenu by remember { mutableStateOf(false) }

    // Bubble shape with authentic Jayra tail
    // Bubble Out: 0dp tail at bottomEnd
    // Bubble In: 0dp tail at bottomStart
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp)
    }

    val bubbleBg = if (isOutgoing) {
        if (isDark) JayraBubbleOutDark else JayraBubbleOutLight
    } else {
        if (isDark) JayraBubbleInDark else JayraBubbleInLight
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
                        color = JayraBlue,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Forwarded From Header inside bubble
                if (!message.forwardedFrom.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        // GANTI SVG TELEGRAM
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Forward,
                            contentDescription = null,
                            tint = JayraBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Diteruskan dari ${message.forwardedFrom}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = JayraBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Reply Preview inside bubble
                if (!message.replyToText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) JayraReplyDark else JayraReplyLight)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(JayraBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = message.replyToSender ?: "Reply",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JayraBlue,
                                    maxLines = 1
                                )
                                val parsedReplyText = remember(message.replyToText) {
                                    MarkdownParser.parse(message.replyToText)
                                }
                                Text(
                                    text = parsedReplyText,
                                    fontSize = 12.sp,
                                    color = JayraSubtext,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Message Content (Checklist, Voice Note, Poll, Location, Contact, or standard Text)
                val isChecklist = message.checklistItems.isNotEmpty()
                val isVoiceNote = message.voiceDurationMillis != null || message.text.startsWith("🎤 [Voice Note")
                val isPoll = message.text.startsWith("📊 Poling:") || message.text.startsWith("🧠 Kuis:")
                val isLocation = message.text.startsWith("📍 [Lokasi]")
                val isContact = message.text.startsWith("👤 [Kontak]")

                if (isChecklist) {
                    ChecklistBubble(
                        checklist = com.jayra.app.model.Checklist(
                            id = message.id,
                            title = message.checklistTitle ?: "Daftar Tugas",
                            items = message.checklistItems,
                            allowOthersToToggle = message.allowOthersToToggle
                        ),
                        onToggleItem = onToggleChecklistItem
                    )
                } else if (isVoiceNote) {
                    VoiceNoteBubble(
                        messageId = message.id,
                        durationMillis = message.voiceDurationMillis ?: 5_000L,
                        waveform = message.voiceWaveform,
                        filePath = message.voiceFilePath
                    )
                } else if (isPoll) {
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
                                    .background(JayraBlue),
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
                                color = JayraBlue
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
                                    .background(JayraBlue),
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
                                    color = JayraBlue
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
                    val formattedMessageText = remember(message.text) {
                        MarkdownParser.parse(message.text)
                    }
                    Text(
                        text = formattedMessageText,
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
                        color = JayraSubtext
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> {
                                Text(
                                    text = "...",
                                    fontSize = 11.sp,
                                    color = JayraSubtext
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Sent",
                                    tint = JayraSubtext,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.READ, MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read",
                                    tint = JayraBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.FAILED -> {
                                Text(
                                    text = "!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JayraDangerRed
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
                    text = { Text("Teruskan") },
                    onClick = { onForwardClick(); showMenu = false }
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
                    text = { Text("Delete", color = JayraDangerRed) },
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
                            .background(if (isDark) JayraBubbleInDark else Color.White)
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
                                color = if (reaction.userReacted) JayraBlue else JayraSubtext
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
    val bg = if (isDark) JayraBubbleInDark else JayraBubbleInLight

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
                    .background(JayraSubtext)
            )
            Box(
                modifier = Modifier
                    .offset(y = dot2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(JayraSubtext)
            )
            Box(
                modifier = Modifier
                    .offset(y = dot3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(JayraSubtext)
            )
        }
    }
}

@Composable
fun ChatInputBar(
    editorValue: TextFieldValue,
    onEditorValueChange: (TextFieldValue) -> Unit,
    onApplyFormat: (RichTextFormatOption) -> Unit,
    onSend: () -> Unit,
    replyTo: Message?,
    onCancelReply: () -> Unit,
    onAttachClick: () -> Unit = {},
    config: RichTextEditorConfig = RichTextEditorConfig(),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) JayraInputDark else JayraInputLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
    ) {
        // Reply bar banner
        if (replyTo != null) {
            val replyPreviewAnnotated = remember(replyTo.text) {
                MarkdownParser.parse(replyTo.text)
            }
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
                        .background(JayraBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${config.replyPrefixLabel} ${replyTo.senderName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = JayraBlue
                    )
                    Text(
                        text = replyPreviewAnnotated,
                        fontSize = 12.sp,
                        color = JayraSubtext,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onCancelReply,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("cancel_reply_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = config.cancelReplyContentDescription,
                        tint = JayraSubtext,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Fase 21.2: Rich Text Editor dengan Toolbar pemformatan & TextField multi-baris
        RichTextEditor(
            value = editorValue,
            onValueChange = onEditorValueChange,
            onFormatSelected = onApplyFormat,
            onSend = onSend,
            onAttachClick = onAttachClick,
            config = config
        )
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
    config: RichTextEditorConfig = RichTextEditorConfig(),
    modifier: Modifier = Modifier
) {
    var localFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = messageText,
                selection = TextRange(messageText.length)
            )
        )
    }

    LaunchedEffect(messageText) {
        if (localFieldValue.text != messageText) {
            localFieldValue = TextFieldValue(
                text = messageText,
                selection = TextRange(messageText.length)
            )
        }
    }

    ChatInputBar(
        editorValue = localFieldValue,
        onEditorValueChange = { updated ->
            localFieldValue = updated
            onMessageChange(updated.text)
        },
        onApplyFormat = { option ->
            val formatted = MarkdownParser.applyFormatting(localFieldValue, option)
            localFieldValue = formatted
            onMessageChange(formatted.text)
        },
        onSend = onSend,
        replyTo = replyTo,
        onCancelReply = onCancelReply,
        onAttachClick = onAttachClick,
        config = config,
        modifier = modifier
    )
}

@Preview(name = "Message Bubble Markdown Light", showBackground = true)
@Composable
private fun MessageBubbleMarkdownLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface(color = JayraLightChatBg) {
            Column(modifier = Modifier.padding(12.dp)) {
                DateSeparator(dateText = "Hari Ini")
                MessageBubble(
                    message = Message(
                        id = "preview_md_1",
                        senderId = "pavel",
                        senderName = "Pavel Durov",
                        text = "Uji **Tebal**, __Miring__, ~~Coret~~, `Monospace`, dan https://jayra.app!",
                        timestamp = "10:45",
                        status = MessageStatus.READ,
                        reactions = listOf(Reaction("🔥", 3, true))
                    ),
                    isOutgoing = false,
                    onReactionClick = {},
                    onReplyClick = {},
                    onDeleteClick = {},
                    onCopyClick = {}
                )
            }
        }
    }
}

@Preview(name = "Message Bubble Markdown Dark", showBackground = true)
@Composable
private fun MessageBubbleMarkdownDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface(color = JayraDarkChatBg) {
            Column(modifier = Modifier.padding(12.dp)) {
                MessageBubble(
                    message = Message(
                        id = "preview_md_2",
                        senderId = "user_telegram_me",
                        senderName = "Me",
                        text = "Dokumentasi resmi di [Portal Jayra](https://jayra.app/docs) siap digunakan!",
                        timestamp = "10:46",
                        status = MessageStatus.READ
                    ),
                    isOutgoing = true,
                    onReactionClick = {},
                    onReplyClick = {},
                    onDeleteClick = {},
                    onCopyClick = {}
                )
                TypingIndicatorBubble()
            }
        }
    }
}
