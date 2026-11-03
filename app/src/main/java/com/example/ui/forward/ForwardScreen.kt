package com.example.ui.forward

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Chat
import com.example.model.ForwardUiState
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramInputDark
import com.example.ui.theme.TelegramInputLight
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSearchDark
import com.example.ui.theme.TelegramSearchLight
import com.example.ui.theme.TelegramSeparatorDark
import com.example.ui.theme.TelegramSeparatorLight
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

@Composable
fun ForwardScreen(
    sourceChatId: String,
    messageId: String,
    onBack: () -> Unit,
    onForwardCompleted: (singleTargetChatId: String?) -> Unit,
    viewModel: ForwardViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler { onBack() }

    LaunchedEffect(sourceChatId, messageId) {
        viewModel.loadForwardContext(sourceChatId, messageId)
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    ForwardContent(
        uiState = uiState,
        onBack = onBack,
        onToggleChatSelection = viewModel::onToggleChatSelection,
        onClearSelection = viewModel::onClearSelection,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCommentChange = viewModel::onCommentChange,
        onConfirmForward = {
            viewModel.onConfirmForward(onForwardCompleted = onForwardCompleted)
        }
    )
}

@Composable
fun ForwardContent(
    uiState: ForwardUiState,
    onBack: () -> Unit,
    onToggleChatSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onConfirmForward: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val cardBg = if (isDark) TelegramCardDark else TelegramCardLight
    val dividerColor = if (isDark) TelegramSeparatorDark else TelegramSeparatorLight

    val successState = uiState as? ForwardUiState.Success
    val selectedCount = successState?.selectedChatIds?.size ?: 0

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = screenBg,
        topBar = {
            ForwardTopBar(
                selectedCount = selectedCount,
                searchQuery = successState?.searchQuery ?: "",
                onBack = onBack,
                onClearSelection = onClearSelection,
                onSearchQueryChange = onSearchQueryChange
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedCount > 0,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                ForwardBottomActionBar(
                    selectedCount = selectedCount,
                    comment = successState?.comment ?: "",
                    isForwarding = successState?.isForwarding ?: false,
                    onCommentChange = onCommentChange,
                    onConfirmForward = onConfirmForward
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ForwardUiState.Loading -> {
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

                is ForwardUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.message,
                            fontSize = 15.sp,
                            color = TelegramDangerRed
                        )
                    }
                }

                is ForwardUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ForwardSourcePreviewBanner(sourceMessage = uiState.sourceMessage)

                        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                        if (uiState.availableChats.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada obrolan ditemukan untuk \"${uiState.searchQuery}\"",
                                    fontSize = 15.sp,
                                    color = TelegramSubtext
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cardBg)
                                    .testTag("forward_chat_list")
                            ) {
                                items(
                                    items = uiState.availableChats,
                                    key = { it.id },
                                    contentType = { "forward_chat_row" }
                                ) { chat ->
                                    val isSelected = chat.id in uiState.selectedChatIds
                                    val isSavedMessages = chat.id == uiState.savedMessagesChatId
                                    ForwardChatItemRow(
                                        chat = chat,
                                        isSelected = isSelected,
                                        isSavedMessages = isSavedMessages,
                                        onToggle = { onToggleChatSelection(chat.id) }
                                    )
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = dividerColor,
                                        modifier = Modifier.padding(start = 76.dp)
                                    )
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
fun ForwardTopBar(
    selectedCount: Int,
    searchQuery: String,
    onBack: () -> Unit,
    onClearSelection: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val searchBg = if (isDark) TelegramSearchDark else TelegramSearchLight
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
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("forward_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = if (selectedCount > 0) "Dipilih: $selectedCount" else "Teruskan Pesan",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.weight(1f)
            )

            if (selectedCount > 0) {
                TextButton(
                    onClick = onClearSelection,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("forward_reset_selection")
                ) {
                    Text(
                        text = "Reset",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFFFFF)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(searchBg)
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
                            text = "Cari obrolan atau grup...",
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
                            .testTag("forward_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus pencarian",
                            tint = TelegramSubtext,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForwardSourcePreviewBanner(sourceMessage: Message) {
    val isDark = isSystemInDarkTheme()
    val bannerBg = if (isDark) TelegramInputDark else TelegramCardLight
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val authorLabel = sourceMessage.forwardedFrom?.takeIf { it.isNotBlank() }
        ?: sourceMessage.senderName.ifBlank { "Pesan" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // GANTI SVG TELEGRAM
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Forward,
            contentDescription = null,
            tint = TelegramBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TelegramBlue)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Teruskan dari $authorLabel",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TelegramBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sourceMessage.text,
                fontSize = 14.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ForwardChatItemRow(
    chat: Chat,
    isSelected: Boolean,
    isSavedMessages: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val baseBg = if (isDark) TelegramCardDark else TelegramCardLight
    val rowBg by animateColorAsState(
        targetValue = if (isSelected) TelegramBlue.copy(alpha = 0.10f) else baseBg,
        animationSpec = tween(durationMillis = 180),
        label = "forward_row_bg_${chat.id}"
    )
    val titleColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(rowBg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp)
            .testTag("forward_chat_item_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(TelegramBlue),
            contentAlignment = Alignment.Center
        ) {
            when {
                isSavedMessages -> {
                    // GANTI SVG TELEGRAM
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = chat.name,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
                chat.avatarUrl.isNotBlank() -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(chat.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = chat.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                chat.isGroup -> {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = chat.name,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
                chat.isChannel -> {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = chat.name,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Text(
                        text = chat.name.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    isSavedMessages -> "Penyimpanan cloud pribadi"
                    chat.isGroup -> "Grup • ${chat.lastMessage}"
                    chat.isChannel -> "Saluran • ${chat.lastMessage}"
                    else -> chat.lastMessage.ifBlank { "Obrolan pribadi" }
                },
                fontSize = 13.sp,
                color = TelegramSubtext,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = TelegramBlue,
                uncheckedColor = TelegramSubtext,
                checkmarkColor = Color(0xFFFFFFFF)
            ),
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("forward_checkbox_${chat.id}")
        )
    }
}

@Composable
fun ForwardBottomActionBar(
    selectedCount: Int,
    comment: String,
    isForwarding: Boolean,
    onCommentChange: (String) -> Unit,
    onConfirmForward: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val barBg = if (isDark) TelegramInputDark else TelegramInputLight
    val fieldBg = if (isDark) TelegramSearchDark else TelegramSearchLight
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val dividerColor = if (isDark) TelegramSeparatorDark else TelegramSeparatorLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(barBg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(fieldBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (comment.isEmpty()) {
                    Text(
                        text = "Tambahkan komentar...",
                        fontSize = 14.sp,
                        color = TelegramSubtext
                    )
                }
                BasicTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = textColor
                    ),
                    cursorBrush = SolidColor(TelegramBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forward_comment_input")
                )
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(TelegramBlue)
                    .clickable(enabled = !isForwarding, onClick = onConfirmForward)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .testTag("forward_confirm_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isForwarding) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFFFFF),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Teruskan ($selectedCount)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFFFFF)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim terusan",
                            tint = Color(0xFFFFFFFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ================= PREVIEWS (LIGHT & DARK) =================

@Preview(name = "Forward Chat Row Light", showBackground = true)
@Composable
private fun ForwardChatItemRowLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            ForwardChatItemRow(
                chat = Chat(
                    id = "uid_me",
                    name = "Pesan Tersimpan",
                    lastMessage = "Simpan ke penyimpanan cloud pribadi"
                ),
                isSelected = true,
                isSavedMessages = true,
                onToggle = {}
            )
        }
    }
}

@Preview(name = "Forward Chat Row Dark", showBackground = true)
@Composable
private fun ForwardChatItemRowDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            ForwardChatItemRow(
                chat = Chat(
                    id = "chat_2",
                    name = "Android Developers Community",
                    lastMessage = "Jetpack Compose 1.8 telah rilis!",
                    isGroup = true
                ),
                isSelected = false,
                isSavedMessages = false,
                onToggle = {}
            )
        }
    }
}

@Preview(name = "Forward Screen Light", showBackground = true)
@Composable
private fun ForwardScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        ForwardContent(
            uiState = ForwardUiState.Success(
                sourceMessage = Message(
                    id = "msg_1",
                    chatId = "chat_1",
                    senderId = "pavel_durov",
                    senderName = "Pavel Durov",
                    text = "Telegram mendukung panggilan suara & video terenkripsi E2EE.",
                    status = MessageStatus.READ
                ),
                availableChats = listOf(
                    Chat(id = "uid_me", name = "Pesan Tersimpan", lastMessage = "Cloud pribadi"),
                    Chat(id = "chat_1", name = "Pavel Durov", lastMessage = "Halo!"),
                    Chat(id = "chat_2", name = "Android Developers Community", lastMessage = "Compose 1.8", isGroup = true)
                ),
                selectedChatIds = setOf("uid_me", "chat_2"),
                savedMessagesChatId = "uid_me"
            ),
            onBack = {},
            onToggleChatSelection = {},
            onClearSelection = {},
            onSearchQueryChange = {},
            onCommentChange = {},
            onConfirmForward = {}
        )
    }
}

@Preview(name = "Forward Screen Dark", showBackground = true)
@Composable
private fun ForwardScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        ForwardContent(
            uiState = ForwardUiState.Success(
                sourceMessage = Message(
                    id = "msg_2",
                    chatId = "chat_3",
                    senderId = "elena_p",
                    senderName = "Elena Petrova",
                    text = "Apakah kita jadi bertemu sore ini di kafe?",
                    status = MessageStatus.READ
                ),
                availableChats = listOf(
                    Chat(id = "uid_me", name = "Pesan Tersimpan", lastMessage = "Cloud pribadi"),
                    Chat(id = "chat_3", name = "Elena Petrova", lastMessage = "Sore ini")
                ),
                selectedChatIds = setOf("chat_3"),
                comment = "Cek jadwal ini ya",
                savedMessagesChatId = "uid_me"
            ),
            onBack = {},
            onToggleChatSelection = {},
            onClearSelection = {},
            onSearchQueryChange = {},
            onCommentChange = {},
            onConfirmForward = {}
        )
    }
}
