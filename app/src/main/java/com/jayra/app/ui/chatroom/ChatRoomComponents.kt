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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.jayra.app.util.EmojiParser

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
    onToggleChecklistItem: (String) -> Unit = {},
    onReactionLongClick: (String) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var showMenu by remember { mutableStateOf(false) }

    // #25 System bubble (pill center)
    if (!message.systemText.isNullOrBlank()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.systemText ?: "",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFEBEBF5) else Color(0xFF3C3C43),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // #13/#14/#34: corner 18dp tail
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }
    val bubbleBg = if (isOutgoing) {
        if (isDark) Color(0xFF005C4B) else Color(0xFFDCF8C6)
    } else {
        if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    }
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val secondary = Color(0xFF8E8E93)

    // #19 palette sender deterministic
    val palette = listOf(
        Color(0xFFFF9500), Color(0xFF34C759), Color(0xFF5856D6),
        Color(0xFFFF2D55), Color(0xFFAF52DE), Color(0xFF5AC8FA), Color(0xFFFF3B30)
    )
    val senderColor = remember(message.senderId) {
        if (message.senderId.isBlank()) palette[0]
        else palette[(message.senderId.hashCode() and 0x7FFFFFFF) % palette.size]
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(0.5.dp, bubbleShape)
                .clip(bubbleShape)
                .background(bubbleBg)
                .combinedClickable(onClick = {}, onLongClick = { showMenu = true })
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("bubble_${message.id}")
        ) {
            Column {
                if (!isOutgoing && message.senderName.isNotBlank() && message.senderName != "Me") {
                    Text(message.senderName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = senderColor, modifier = Modifier.padding(bottom = 2.dp))
                }

                // #20 Forwarded
                if (!message.forwardedFrom.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 3.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Forward, null, tint = secondary, modifier = Modifier.size(12.dp))
                        Text("Diteruskan", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = secondary, maxLines = 1)
                    }
                }

                // #21 Reply (4dp #34C759)
                if (!message.replyToText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF000000).copy(alpha = 0.06f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(4.dp).height(30.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF34C759)))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(message.replyToSender ?: "Reply", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF34C759), maxLines = 1)
                                Text(MarkdownParser.parse(message.replyToText ?: ""), fontSize = 13.sp, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                val isChecklist = message.checklistItems.isNotEmpty()
                val isVoice = message.voiceDurationMillis != null || message.text.startsWith("🎤 [Voice Note")
                val isPoll = message.text.startsWith("📊 Poling:") || message.text.startsWith("🧠 Kuis:")
                val isLoc = message.text.startsWith("📍 [Lokasi]")
                val isContact = message.text.startsWith("👤 [Kontak]")
                val isDoc = message.text.startsWith("📄 [Dokumen]")

                when {
                    !message.mediaUrl.isNullOrBlank() -> AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.widthIn(max = 260.dp).height(200.dp).clip(RoundedCornerShape(14.dp))
                    )
                    !message.stickerUrl.isNullOrBlank() -> AsyncImage(
                        model = message.stickerUrl,
                        contentDescription = "Sticker",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(120.dp)
                    )
                    !message.gifUrl.isNullOrBlank() -> AsyncImage(
                        model = message.gifUrl,
                        contentDescription = "GIF",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.widthIn(max = 240.dp).height(160.dp).clip(RoundedCornerShape(12.dp))
                    )
                    isChecklist -> ChecklistBubble(
                        checklist = com.jayra.app.model.Checklist(
                            id = message.id,
                            title = message.checklistTitle ?: "Daftar Tugas",
                            items = message.checklistItems,
                            allowOthersToToggle = message.allowOthersToToggle
                        ),
                        onToggleItem = onToggleChecklistItem
                    )
                    isVoice -> VoiceNoteBubble(
                        messageId = message.id,
                        durationMillis = message.voiceDurationMillis ?: 5_000L,
                        waveform = message.voiceWaveform,
                        filePath = message.voiceFilePath
                    )
                    isPoll -> {
                        val lines = message.text.lines()
                        val header = lines.firstOrNull() ?: ""
                        val quiz = header.startsWith("🧠 Kuis:")
                        val q = header.substringAfter(": ").trim()
                        val opts = lines.drop(1).filter { it.isNotBlank() }.map { l ->
                            PollOption(text = l.replaceFirst(Regex("^\\d+\\.\\s*"), ""), voteCount = 0)
                        }
                        if (opts.isNotEmpty()) PollBubble(poll = Poll(id = message.id, question = q.ifBlank { "Polling" }, options = opts, isQuiz = quiz, isAnonymous = !quiz))
                        else Text(message.text, fontSize = 17.sp, lineHeight = 22.sp, color = textColor)
                    }
                    isLoc -> Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF007AFF)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Text("Lokasi Dibagikan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF007AFF))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(message.text.substringAfter("📍 [Lokasi]").trim(), fontSize = 15.sp, color = textColor)
                    }
                    isContact -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF007AFF)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Kartu Kontak", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF007AFF))
                            Text(message.text.substringAfter("👤 [Kontak]").trim(), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
                        }
                    }
                    isDoc -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF007AFF)), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Forward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(message.text.substringAfter("📄 [Dokumen]").trim(), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Dokumen", fontSize = 12.sp, color = secondary)
                        }
                    }
                    else -> Text(
                        text = remember(message.text) {
                            val md = MarkdownParser.parse(message.text)
                            if (EmojiParser.hasEmoji(md)) EmojiParser.parse(md.text) else md
                        },
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        color = textColor
                    )
                }

                // #33 Link preview
                if (!message.linkPreviewTitle.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF000000).copy(alpha = 0.05f)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!message.linkPreviewImageUrl.isNullOrBlank()) {
                            AsyncImage(message.linkPreviewImageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)))
                            Spacer(Modifier.width(8.dp))
                        }
                        Column {
                            Text(message.linkPreviewTitle ?: "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!message.linkPreviewDescription.isNullOrBlank()) {
                                Text(message.linkPreviewDescription, fontSize = 12.sp, color = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // #16 Time + #17/#18 status
                Row(Modifier.align(Alignment.End).padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (message.disappearingSeconds != null) {
                        Text("⏱", fontSize = 11.sp, color = secondary)
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(message.timestamp, fontSize = 13.sp, color = secondary)
                    if (isOutgoing) {
                        Spacer(Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> Text("...", fontSize = 11.sp, color = secondary)
                            MessageStatus.SENT -> Icon(Icons.Rounded.Done, "Sent", tint = secondary, modifier = Modifier.size(14.dp))
                            MessageStatus.DELIVERED -> Icon(Icons.Rounded.DoneAll, "Delivered", tint = secondary, modifier = Modifier.size(14.dp))
                            MessageStatus.READ -> Icon(Icons.Rounded.DoneAll, "Read", tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                            MessageStatus.FAILED -> Text("!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30))
                        }
                    }
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Reply") }, onClick = { onReplyClick(); showMenu = false })
                DropdownMenuItem(text = { Text("Teruskan") }, onClick = { onForwardClick(); showMenu = false })
                DropdownMenuItem(text = { Text("Copy") }, onClick = { onCopyClick(); showMenu = false })
                DropdownMenuItem(text = { Text("👍 Like") }, onClick = { onReactionClick("👍"); showMenu = false })
                DropdownMenuItem(text = { Text("❤️ Heart") }, onClick = { onReactionClick("❤️"); showMenu = false })
                DropdownMenuItem(text = { Text("🔥 Fire") }, onClick = { onReactionClick("🔥"); showMenu = false })
                DropdownMenuItem(text = { Text("Delete", color = Color(0xFFFF3B30)) }, onClick = { onDeleteClick(); showMenu = false })
            }
        }

        // #22/#23 Reactions
        if (message.reactions.isNotEmpty()) {
            Row(Modifier.offset(y = (-6).dp).padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                message.reactions.forEach { r ->
                    Box(
                        modifier = Modifier
                            .shadow(1.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF2C2C2E) else Color.White)
                            .clickable { onReactionLongClick(r.emoji) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.emoji, fontSize = 16.sp)
                            if (r.count > 1) {
                                Spacer(Modifier.width(4.dp))
                                Text(r.count.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (r.userReacted) Color(0xFF007AFF) else secondary)
                            }
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
                        imageVector = Icons.Rounded.Close,
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
