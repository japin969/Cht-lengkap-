package com.jayra.app.ui.assistant

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
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.AiMode
import com.jayra.app.model.AssistantUiState
import com.jayra.app.model.Message
import com.jayra.app.model.MessageStatus
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraBubbleInDark
import com.jayra.app.ui.theme.JayraBubbleInLight
import com.jayra.app.ui.theme.JayraBubbleOutDark
import com.jayra.app.ui.theme.JayraBubbleOutLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraInputDark
import com.jayra.app.ui.theme.JayraInputLight
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraStatusOnline
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.util.EmojiParser

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

    val currentMode = (uiState as? AssistantUiState.Success)?.mode ?: AiMode.NORMAL

    AssistantScreenContent(
        uiState = uiState,
        currentMode = currentMode,
        onBack = onBack,
        onSendMessage = { text -> viewModel.sendMessage(text) },
        onRetry = { viewModel.onRetry() },
        onClearHistory = { viewModel.onClearHistory() },
        onModeChange = { viewModel.setMode(it) }
    )
}

@Composable
fun AssistantScreenContent(
    uiState: AssistantUiState,
    currentMode: AiMode,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetry: () -> Unit,
    onClearHistory: () -> Unit,
    onModeChange: (AiMode) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    var showMenu by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("assistant_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(JayraBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentMode == AiMode.THINKING) Icons.Rounded.Psychology else Icons.Rounded.SmartToy,
                        contentDescription = "Avatar Jayra AI",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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
                        text = if (currentMode == AiMode.THINKING) "Berpikir mendalam..." else "Online",
                        fontSize = 13.sp,
                        color = JayraStatusOnline,
                        maxLines = 1
                    )
                }

                // Chip mode di top bar
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentMode == AiMode.THINKING) JayraBlue else Color.White.copy(alpha = 0.15f))
                        .clickable {
                            val next = if (currentMode == AiMode.THINKING) AiMode.NORMAL else AiMode.THINKING
                            onModeChange(next)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("assistant_mode_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentMode == AiMode.THINKING) Icons.Rounded.Psychology else Icons.Rounded.SmartToy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentMode == AiMode.THINKING) "Berpikir" else "Biasa",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(48.dp).testTag("assistant_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Menu Lainnya",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.SmartToy, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mode Biasa (GPT-OSS)")
                                    if (currentMode == AiMode.NORMAL) {
                                        Spacer(Modifier.width(8.dp))
                                        Text("✓", color = JayraBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = { showMenu = false; onModeChange(AiMode.NORMAL) }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Psychology, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mode Berpikir (DeepSeek R1)")
                                    if (currentMode == AiMode.THINKING) {
                                        Spacer(Modifier.width(8.dp))
                                        Text("✓", color = JayraBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = { showMenu = false; onModeChange(AiMode.THINKING) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hapus Riwayat") },
                            onClick = { showMenu = false; onClearHistory() }
                        )
                    }
                }
            }
        },
        bottomBar = {
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
                is AssistantUiState.Loading -> AssistantShimmerLoading()
                is AssistantUiState.Empty -> AssistantEmptyState(modifier = Modifier.align(Alignment.Center))
                is AssistantUiState.Error -> AssistantErrorState(
                    errorMessage = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
                is AssistantUiState.Success -> {
                    if (uiState.messages.isEmpty() && !uiState.isLoading) {
                        AssistantEmptyState(modifier = Modifier.align(Alignment.Center))
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(uiState.messages.size, uiState.isLoading) {
                            val totalCount = uiState.messages.size + if (uiState.isLoading) 1 else 0
                            if (totalCount > 0) {
                                listState.animateScrollToItem(totalCount - 1)
                                kotlinx.coroutines.delay(80)
                                listState.animateScrollToItem(totalCount - 1)
                            }
                        }

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

@Composable
private fun UserMessageBubble(message: Message, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val bubbleBg = if (isDark) JayraBubbleOutDark else JayraBubbleOutLight
    val textColor = if (isDark) Color.White else Color.Black

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 0.dp))
                .background(bubbleBg)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = remember(message.text) { EmojiParser.parse(message.text) },
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor
            )
            Row(
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = message.timestamp, fontSize = 12.sp, color = JayraSubtext)
                if (message.status == MessageStatus.SENDING) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "•", fontSize = 12.sp, color = JayraSubtext)
                } else if (message.status == MessageStatus.FAILED) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Gagal",
                        tint = JayraDangerRed,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(message: Message, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val bubbleBg = if (isDark) JayraBubbleInDark else JayraBubbleInLight
    val textColor = if (isDark) Color.White else Color.Black
    var thinkExpanded by remember(message.id) { mutableStateOf(false) }
    val hasThink = !message.thinkText.isNullOrBlank()

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
                .background(if (hasThink) JayraBlue else JayraBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasThink) Icons.Rounded.Psychology else Icons.Rounded.SmartToy,
                contentDescription = "Jayra AI",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp))
                .background(bubbleBg)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Jayra AI",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = JayraBlue,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            if (hasThink) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JayraSubtext.copy(alpha = 0.12f))
                        .clickable { thinkExpanded = !thinkExpanded }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = JayraSubtext,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (thinkExpanded) "Sembunyikan proses berpikir" else "Lihat proses berpikir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = JayraSubtext,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (thinkExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = JayraSubtext,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = thinkExpanded) {
                    Text(
                        text = message.thinkText ?: "",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontStyle = FontStyle.Italic,
                        color = JayraSubtext,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = remember(message.text) { EmojiParser.parse(message.text) },
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor
            )

            Text(
                text = message.timestamp,
                fontSize = 12.sp,
                color = JayraSubtext,
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun TypingIndicatorBubble(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val bubbleBg = if (isDark) JayraBubbleInDark else JayraBubbleInLight

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
                .background(JayraBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = "Jayra AI",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp))
                .background(bubbleBg)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "dots_bounce")
            val dot1Offset by infiniteTransition.animateFloat(
                0f, -5f,
                infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "dot1"
            )
            val dot2Offset by infiniteTransition.animateFloat(
                0f, -5f,
                infiniteRepeatable(tween(300, delayMillis = 150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "dot2"
            )
            val dot3Offset by infiniteTransition.animateFloat(
                0f, -5f,
                infiniteRepeatable(tween(300, delayMillis = 300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "dot3"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.offset(y = dot1Offset.dp).size(6.dp).clip(CircleShape).background(JayraSubtext))
                Box(modifier = Modifier.offset(y = dot2Offset.dp).size(6.dp).clip(CircleShape).background(JayraSubtext))
                Box(modifier = Modifier.offset(y = dot3Offset.dp).size(6.dp).clip(CircleShape).background(JayraSubtext))
            }
        }
    }
}

@Composable
private fun AssistantInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val inputBarBg = if (isDark) JayraInputDark else JayraInputLight
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
        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Rounded.AttachFile,
                contentDescription = "Lampirkan file",
                tint = JayraSubtext,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                Text(text = "Tanya sesuatu...", fontSize = 16.sp, color = JayraSubtext)
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().testTag("assistant_text_input"),
                textStyle = TextStyle(fontSize = 16.sp, color = textColor),
                cursorBrush = SolidColor(JayraBlue),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = true)
            )
        }

        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Rounded.EmojiEmotions,
                contentDescription = "Pilih emoji",
                tint = JayraSubtext,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedVisibility(
            visible = text.isNotBlank(),
            enter = scaleIn(animationSpec = tween(durationMillis = 150)),
            exit = scaleOut(animationSpec = tween(durationMillis = 150))
        ) {
            IconButton(
                onClick = onSend,
                modifier = Modifier.size(40.dp).testTag("assistant_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim pesan",
                    tint = JayraBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistantEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.SmartToy,
            contentDescription = "Bot",
            tint = JayraSubtext,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Halo! Ada yang bisa saya bantu?",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = JayraSubtext,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tanyakan apa saja ke Jayra AI",
            fontSize = 15.sp,
            color = JayraSubtext,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AssistantErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = "Error",
            tint = JayraDangerRed,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (errorMessage.isNotBlank()) errorMessage else "Gagal mengirim pesan",
            fontSize = 17.sp,
            color = JayraSubtext,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("assistant_retry_button")
        ) {
            Text(text = "Coba Lagi", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
private fun AssistantShimmerLoading(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF233040) else Color(0xFFD8DFE5)
    val highlightColor = if (isDark) Color(0xFF324357) else Color(0xFFE8EEF3)

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        0f, 1000f,
        infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim - 300f, y = translateAnim - 300f),
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
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
