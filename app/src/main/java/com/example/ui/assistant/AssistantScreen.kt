package com.example.ui.assistant

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.model.AssistantUiState
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBubbleInDark
import com.example.ui.theme.TelegramBubbleInLight
import com.example.ui.theme.TelegramBubbleOutDark
import com.example.ui.theme.TelegramBubbleOutLight
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramInputDark
import com.example.ui.theme.TelegramInputLight
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramStatusOnline
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

@Composable
fun AssistantScreen(
    onBack: () -> Unit,
    viewModel: AssistantViewModel = viewModel()
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    AssistantScreenContent(
        uiState = uiState,
        onBack = onBack,
        onSendMessage = { text -> viewModel.sendMessage(text) },
        onRetry = { viewModel.onRetry() },
        onClearHistory = { viewModel.onClearHistory() }
    )
}

@Composable
fun AssistantScreenContent(
    uiState: AssistantUiState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetry: () -> Unit,
    onClearHistory: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    var showMenu by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            // 1.1 TopAppBar (Height 56dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button 24dp, tap target 48dp
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("assistant_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // AI Avatar 36dp CircleShape, margin start 8dp
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TelegramBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Avatar Jayra AI",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Online status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jayra AI",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Online",
                        fontSize = 13.sp,
                        color = TelegramStatusOnline,
                        maxLines = 1
                    )
                }

                // MoreVert icon 24dp, tap target 48dp
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("assistant_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu Lainnya",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hapus Riwayat") },
                            onClick = {
                                showMenu = false
                                onClearHistory()
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // 1.5 Input Bar
            AssistantInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is AssistantUiState.Loading -> {
                    // 1.6 Loading State (Shimmer 5 bubble)
                    AssistantShimmerLoading()
                }

                is AssistantUiState.Empty -> {
                    // 1.6 Empty State
                    AssistantEmptyState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is AssistantUiState.Error -> {
                    // 1.6 Error State
                    AssistantErrorState(
                        errorMessage = uiState.message,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is AssistantUiState.Success -> {
                    if (uiState.messages.isEmpty() && !uiState.isLoading) {
                        AssistantEmptyState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        val listState = rememberLazyListState()

                        LaunchedEffect(uiState.messages.size, uiState.isLoading) {
                            val totalCount = uiState.messages.size + if (uiState.isLoading) 1 else 0
                            if (totalCount > 0) {
                                listState.animateScrollToItem(totalCount - 1)
                            }
                        }

                        // 1.2 Background LazyColumn padding horizontal 8dp, vertikal 16dp
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.messages,
                                key = { it.id },
                                contentType = { if (it.senderId == "jayra_ai") "ai_bubble" else "user_bubble" }
                            ) { message ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        animationSpec = tween(durationMillis = 200),
                                        initialOffsetY = { it / 2 }
                                    ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                ) {
                                    if (message.senderId == "jayra_ai") {
                                        AiMessageBubble(message = message)
                                    } else {
                                        UserMessageBubble(message = message)
                                    }
                                }
                            }

                            if (uiState.isLoading) {
                                item(key = "typing_indicator") {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = slideInVertically(
                                            animationSpec = tween(durationMillis = 200),
                                            initialOffsetY = { it / 2 }
                                        ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                    ) {
                                        TypingIndicatorBubble()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 1.3 User Bubble (Pesan Kita)
@Composable
private fun UserMessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bubbleBg = if (isDark) TelegramBubbleOutDark else TelegramBubbleOutLight
    val textColor = if (isDark) Color.White else Color.Black

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(bubbleBg)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor
            )

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

                if (message.status == MessageStatus.SENDING) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = TelegramSubtext
                    )
                } else if (message.status == MessageStatus.FAILED) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Gagal",
                        tint = TelegramDangerRed,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// 1.3 AI Bubble (Pesan Lawan)
@Composable
private fun AiMessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bubbleBg = if (isDark) TelegramBubbleInDark else TelegramBubbleInLight
    val textColor = if (isDark) Color.White else Color.Black

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar AI 32dp CircleShape margin end 8dp
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(TelegramBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Jayra AI",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 12.dp
                    )
                )
                .background(bubbleBg)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Nama Pengirim: "Jayra AI" 14sp FontWeight.Bold TelegramBlue, margin bottom 2dp
            Text(
                text = "Jayra AI",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TelegramBlue,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            Text(
                text = message.text,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor
            )

            Text(
                text = message.timestamp,
                fontSize = 12.sp,
                color = TelegramSubtext,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp)
            )
        }
    }
}

// 1.4 Typing Indicator
@Composable
private fun TypingIndicatorBubble(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bubbleBg = if (isDark) TelegramBubbleInDark else TelegramBubbleInLight

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(TelegramBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Jayra AI",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 12.dp
                    )
                )
                .background(bubbleBg)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "dots_bounce")

            val dot1Offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot1"
            )

            val dot2Offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300, delayMillis = 150, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot2"
            )

            val dot3Offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300, delayMillis = 300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot3"
            )

            // 3 dot 6dp TelegramSubtext, spacing 3dp
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = dot1Offset.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TelegramSubtext)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot2Offset.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TelegramSubtext)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot3Offset.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TelegramSubtext)
                )
            }
        }
    }
}

// 1.5 Input Bar
@Composable
private fun AssistantInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val inputBarBg = if (isDark) TelegramInputDark else TelegramInputLight
    val textColor = if (isDark) Color.White else Color.Black

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(inputBarBg)
            .navigationBarsPadding()
            .imePadding()
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attach (Icons.Default.AttachFile) 24dp TelegramSubtext
        IconButton(
            onClick = {},
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Lampirkan file",
                tint = TelegramSubtext,
                modifier = Modifier.size(24.dp)
            )
        }

        // TextField "Tanya sesuatu..." 16sp maxLines 5
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "Tanya sesuatu...",
                    fontSize = 16.sp,
                    color = TelegramSubtext
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assistant_text_input"),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = textColor
                ),
                cursorBrush = SolidColor(TelegramBlue),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = true)
            )
        }

        // Emoji (Icons.Default.EmojiEmotions) 24dp TelegramSubtext
        IconButton(
            onClick = {},
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEmotions,
                contentDescription = "Pilih emoji",
                tint = TelegramSubtext,
                modifier = Modifier.size(24.dp)
            )
        }

        // Send (Icons.AutoMirrored.Filled.Send) 24dp TelegramBlue, scaleIn 150ms
        AnimatedVisibility(
            visible = text.isNotBlank(),
            enter = scaleIn(animationSpec = tween(durationMillis = 150)),
            exit = scaleOut(animationSpec = tween(durationMillis = 150))
        ) {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("assistant_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim pesan",
                    tint = TelegramBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// 1.6 Empty State
@Composable
private fun AssistantEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "Bot",
            tint = TelegramSubtext,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Halo! Ada yang bisa saya bantu?",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = TelegramSubtext,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tanyakan apa saja ke Jayra AI",
            fontSize = 15.sp,
            color = TelegramSubtext,
            textAlign = TextAlign.Center
        )
    }
}

// 1.6 Error State
@Composable
private fun AssistantErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = TelegramDangerRed,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (errorMessage.isNotBlank()) errorMessage else "Gagal mengirim pesan",
            fontSize = 17.sp,
            color = TelegramSubtext,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("assistant_retry_button")
        ) {
            Text(
                text = "Coba Lagi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

// 1.6 Shimmer Loading (5 bubbles, Brush.linearGradient 1000ms infinite)
@Composable
private fun AssistantShimmerLoading(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF233040) else Color(0xFFD8DFE5)
    val highlightColor = if (isDark) Color(0xFF324357) else Color(0xFFE8EEF3)

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim - 300f, y = translateAnim - 300f),
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(5) { index ->
            val isUser = index % 2 == 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .width(if (index % 3 == 0) 220.dp else 170.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Preview(name = "Assistant Screen Light", showBackground = true)
@Composable
private fun AssistantScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            AssistantScreenContent(
                uiState = AssistantUiState.Success(
                    messages = listOf(
                        Message(
                            id = "1",
                            senderId = "jayra_ai",
                            senderName = "Jayra AI",
                            text = "Halo! Saya Jayra AI. Ada yang bisa saya bantu hari ini?",
                            timestamp = "10:00 AM",
                            status = MessageStatus.READ
                        ),
                        Message(
                            id = "2",
                            senderId = "user_me",
                            senderName = "User",
                            text = "Bagaimana arsitektur keamanan Jayra?",
                            timestamp = "10:01 AM",
                            status = MessageStatus.READ
                        ),
                        Message(
                            id = "3",
                            senderId = "jayra_ai",
                            senderName = "Jayra AI",
                            text = "Jayra dilengkapi enkripsi AES-256-GCM, proteksi FLAG_SECURE, sanitasi anti-injeksi, dan pembatasan frekuensi pengiriman.",
                            timestamp = "10:01 AM",
                            status = MessageStatus.READ
                        )
                    ),
                    isLoading = false
                ),
                onBack = {},
                onSendMessage = {},
                onRetry = {},
                onClearHistory = {}
            )
        }
    }
}

@Preview(name = "Assistant Screen Dark", showBackground = true)
@Composable
private fun AssistantScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            AssistantScreenContent(
                uiState = AssistantUiState.Success(
                    messages = listOf(
                        Message(
                            id = "1",
                            senderId = "jayra_ai",
                            senderName = "Jayra AI",
                            text = "Halo! Saya Jayra AI siap membantu Anda.",
                            timestamp = "10:00 AM",
                            status = MessageStatus.READ
                        )
                    ),
                    isLoading = true
                ),
                onBack = {},
                onSendMessage = {},
                onRetry = {},
                onClearHistory = {}
            )
        }
    }
}
