package com.jayra.app.ui.chatlist
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MarkChatRead
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.model.Chat
import com.jayra.app.model.ChatFilter
import com.jayra.app.model.ChatListUiState
import com.jayra.app.model.MessageStatus
import com.jayra.app.ui.viewmodel.ChatListViewModel
import com.jayra.app.ui.auth.iosGlassEntrance
import com.jayra.app.ui.theme.PoeLogo
import com.jayra.app.util.iosGlass
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.jayra.app.util.glassSurface
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.util.GlassTier
import com.jayra.app.ui.theme.IOSBlue
import com.jayra.app.ui.theme.IOSGreen
import com.jayra.app.ui.theme.IOSOrange
import com.jayra.app.ui.theme.IOSRed
import com.jayra.app.ui.theme.IOSGray2
import com.jayra.app.ui.theme.IOSSecondaryLabel
import com.jayra.app.ui.theme.IOSSecondaryLabelDark
import com.jayra.app.ui.theme.IOSSecondarySystemBackground
import com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onContactInfoClick: (String) -> Unit,
    onMyProfileClick: () -> Unit = {},
    onCallsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onSavedMessagesClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onStoriesClick: () -> Unit = {},
    onAiClick: () -> Unit = {},
    onArchivedClick: () -> Unit = {},
    viewModel: ChatListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val archivedChats by viewModel.archivedChats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedChats = remember { mutableStateListOf<String>() }
    var isPaused by remember { mutableStateOf(false) }

    // Blur background saat app di background (WhatsApp iOS style)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> isPaused = true
                Lifecycle.Event.ON_RESUME -> isPaused = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    var searchActive by remember { mutableStateOf(false) }
    LaunchedEffect(searchActive) {
        if (searchActive) runCatching { searchFocus.requestFocus() }
    }
    LaunchedEffect(Unit) {
        // Android IME auto-show fires ~100ms after compose (not 16ms like
        // withFrameNanos). Delay 200ms to win the race, then force-clear.
        // Combined with manifest stateAlwaysHidden + MainActivity
        // setSoftInputMode, keyboard stays hidden on launch.
        // Slow devices fire IME auto-show later than 200ms; clear repeatedly
        // across the launch window so the race is won even at low FPS.
        for (d in listOf(120L, 180L, 300L, 600L)) {
            delay(d)
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val bgDark = isSystemInDarkTheme()
    val bgTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
    val bgSpec = remember(bgTier, bgDark) { LiquidGlassSpecV2.forTier(bgTier, bgDark) }
    val bgGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        *bgSpec.backdrop.gradientStops.map { it.position to it.color }.toTypedArray()
    )

    val bgOrbs = remember(bgSpec) { bgSpec.backdrop.radialOrbs }

    val totalUnread = (uiState as? ChatListUiState.Success)?.chats?.sumOf { it.unreadCount } ?: 0

    Box(modifier = Modifier.fillMaxSize().background(bgGradient).drawBehind {
        bgOrbs.forEach { orb ->
            val c = androidx.compose.ui.geometry.Offset(size.width * orb.centerXRatio, size.height * orb.centerYRatio)
            val r = size.minDimension * orb.radiusRatio
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(orb.color.copy(alpha = 0.25f), androidx.compose.ui.graphics.Color.Transparent),
                    center = c, radius = r
                ),
                radius = r, center = c
            )
        }
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ========== TOP BAR ==========
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosGlassEntrance(delayMillis = 0)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    AppText(
                        text = "X",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IOSBlue,
                        modifier = Modifier.clickable {
                            isSelectionMode = false
                            selectedChats.clear()
                        }
                    )
                } else {
                    AppText(
                        text = "Edit",
                        fontSize = 17.sp,
                        color = Color(0xFF5AC8FA),
                        modifier = Modifier.clickable { isSelectionMode = true }
                    )
                }
                if (isSelectionMode) {
                    AppText(
                        text = "Pilih Semua",
                        fontSize = 17.sp,
                        color = IOSBlue,
                        modifier = Modifier.clickable {
                            selectedChats.clear()
                            selectedChats.addAll((uiState as? ChatListUiState.Success)?.chats?.map { it.id } ?: emptyList())
                        }
                    )
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New Chat",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(26.dp).clickable { onContactsClick() }
                    )
                    Spacer(Modifier.width(16.dp))
                    Box {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Menu",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(26.dp).clickable { showMenu = true }
                        )
                        val menuTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
                        val menuDark = isSystemInDarkTheme()
                        val menuSpec = remember(menuTier, menuDark) { LiquidGlassSpecV2.forTier(menuTier, menuDark) }
                        val menuSolid = if (menuDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemBackground
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .glassSurfaceV2(menuSpec.surface, solidColor = menuSolid, cornerRadius = 16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { AppText("Jayra AI") },
                                onClick = { showMenu = false },
                                modifier = Modifier.height(44.dp)
                            )
                            DropdownMenuItem(
                                text = { AppText("New Group") },
                                onClick = { showMenu = false; onCreateGroupClick() },
                                modifier = Modifier.height(44.dp)
                            )
                            // Hidden until Broadcast screen exists (Batch future)
                            // DropdownMenuItem(text = { AppText("New Broadcast") }, onClick = { showMenu = false })
                            DropdownMenuItem(
                                text = { AppText("Starred Messages") },
                                onClick = { showMenu = false; onSavedMessagesClick() },
                                modifier = Modifier.height(44.dp)
                            )
                            DropdownMenuItem(
                                text = { AppText("Archived") },
                                onClick = { showMenu = false; onArchivedClick() },
                                modifier = Modifier.height(44.dp)
                            )
                            DropdownMenuItem(
                                text = { AppText("Settings") },
                                onClick = { showMenu = false; onMyProfileClick() },
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                }
            }

            }
            // ========== LARGE TITLE (iOS left-aligned) ==========
            AppText(
                text = if (isSelectionMode) "${selectedChats.size} dipilih" else if (totalUnread > 0) "Chat ($totalUnread)" else "Chat",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.04).sp,
                color = if (androidx.compose.foundation.isSystemInDarkTheme()) com.jayra.app.ui.theme.IOSLabelDark else com.jayra.app.ui.theme.IOSLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp)
                    .iosGlassEntrance(delayMillis = 45)
            )
            // ========== SEARCH BAR (A3 Glass) ==========
            val searchIsDark = isSystemInDarkTheme()
            val searchTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
            val searchSpec = remember(searchTier, searchIsDark) { LiquidGlassSpecV2.forTier(searchTier, searchIsDark) }
            val searchBg = androidx.compose.ui.graphics.Color(0x1F767680)
            val searchPlaceholderColor = if (searchIsDark) IOSSecondaryLabelDark else IOSSecondaryLabel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(40.dp)
                    .iosGlassEntrance(delayMillis = 90)
                    .glassSurfaceV2(searchSpec.surface, solidColor = searchBg, cornerRadius = 20.dp)
                    .padding(horizontal = 12.dp)
                    .clickable { searchActive = true },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = androidx.compose.ui.graphics.Color(0x8A8A8E), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        enabled = searchActive,
                        modifier = Modifier.weight(1f).focusRequester(searchFocus),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = if (androidx.compose.foundation.isSystemInDarkTheme()) com.jayra.app.ui.theme.IOSLabelDark else com.jayra.app.ui.theme.IOSLabel, fontFamily = com.jayra.app.ui.theme.SFProFontFamily),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(IOSBlue),
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchQuery.isEmpty()) AppText("Cari", fontSize = 17.sp, color = searchPlaceholderColor)
                                innerTextField()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Clear",
                            tint = searchPlaceholderColor,
                            modifier = Modifier.size(16.dp).clickable { viewModel.onSearchQueryChange("") }
                        )
                    }
                }
            }

            // ========== FILTER CHIP ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .iosGlassEntrance(delayMillis = 135),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chipTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
                val chipDark = isSystemInDarkTheme()
                val chipSpec = remember(chipTier, chipDark) { LiquidGlassSpecV2.forTier(chipTier, chipDark) }
                val chipSolid = if (chipDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemBackground
                ChatFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) IOSBlue.copy(alpha = 0.1f) else Color.Transparent,
                        label = "chip_bg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) IOSBlue else IOSSecondaryLabel,
                        label = "chip_text"
                    )
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .background(if (isSelected) bg else androidx.compose.ui.graphics.Color(0x14767680), RoundedCornerShape(18.dp))
                            .clickable { viewModel.onFilterChange(filter) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(filter.label, fontSize = 15.sp, color = textColor, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }

            // ========== LIST ==========
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is ChatListUiState.Loading -> ShimmerChatList()
                    is ChatListUiState.Empty -> EmptyState(onStartChat = onContactsClick)
                    is ChatListUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 180.dp)
                        ) {
                            if (archivedChats.isNotEmpty()) {
                                item(key = "archived_chats_folder") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = onArchivedClick)
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Archive,
                                            contentDescription = "Diarsipkan",
                                            tint = IOSBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column(Modifier.weight(1f)) {
                                            AppText(
                                                text = "Diarsipkan",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSystemInDarkTheme()) {
                                                    com.jayra.app.ui.theme.IOSLabelDark
                                                } else {
                                                    com.jayra.app.ui.theme.IOSLabel
                                                }
                                            )
                                            AppText(
                                                text = "${archivedChats.size} chat",
                                                fontSize = 14.sp,
                                                color = if (isSystemInDarkTheme()) {
                                                    IOSSecondaryLabelDark
                                                } else {
                                                    IOSSecondaryLabel
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            items(state.chats, key = { it.id }, contentType = { "chat_item" }) { chat ->
                                SwipeableChatRow(
                                    chat = chat,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedChats.contains(chat.id),
                                    onClick = {
                                        if (isSelectionMode) {
                                            if (selectedChats.contains(chat.id)) selectedChats.remove(chat.id) else selectedChats.add(chat.id)
                                        } else {
                                            onChatClick(chat.id)
                                        }
                                    },
                                    onDelete = { viewModel.deleteChat(chat.id) },
                                    onArchive = {
                                        viewModel.archiveChat(chat.id)
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Chat diarsipkan",
                                                actionLabel = "Batal",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.onUnarchiveChat(chat.id)
                                            }
                                        }
                                    },
                                    onPin = { viewModel.togglePin(chat.id) },
                                    onMute = { viewModel.toggleMute(chat.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(chat.id) },
                                    onMarkRead = { viewModel.onMarkAsRead(chat.id) }
                                )
                            }
                        }
                    }
                    is ChatListUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppText("Error: ${state.message}", color = Color.Red) }
                }
            }
        }

        // ========== ACTION BAR (Selection Mode) ==========
        androidx.compose.animation.AnimatedVisibility(
            visible = isSelectionMode && selectedChats.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
            ) + fadeIn(tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(tween(200))
        ) {
            val barTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
            val barDark = isSystemInDarkTheme()
            val barSpec = remember(barTier, barDark) { LiquidGlassSpecV2.forTier(barTier, barDark) }
            val barBg = if (barDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemBackground
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(56.dp)
                    .glassSurfaceV2(barSpec.surface, solidColor = barBg, cornerRadius = 20.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionBarItem(Icons.Rounded.Archive, "Arsipkan") { selectedChats.forEach { viewModel.archiveChat(it) }; selectedChats.clear() }
                ActionBarItem(Icons.Rounded.Delete, "Hapus", isDestructive = true) { selectedChats.forEach { viewModel.deleteChat(it) }; selectedChats.clear() }
                ActionBarItem(Icons.Rounded.NotificationsOff, "Bisukan") { selectedChats.forEach { viewModel.onMuteToggle(it) }; selectedChats.clear() }
                ActionBarItem(Icons.Rounded.PushPin, "Sematkan") { selectedChats.forEach { viewModel.onPinToggle(it) }; selectedChats.clear() }
                ActionBarItem(Icons.Rounded.MarkChatRead, "Tandai") { selectedChats.clear() }
            }
        }

        // ========== BLUR OVERLAY (App Background) ==========
        if (isPaused) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)))
        }

        // ========== SNACKBAR HOST ==========
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun EmptyState(onStartChat: () -> Unit) {
    val isDarkEmpty = androidx.compose.foundation.isSystemInDarkTheme()
    val titleColor = if (isDarkEmpty) com.jayra.app.ui.theme.IOSLabelDark else com.jayra.app.ui.theme.IOSLabel
    val subtitleColor = if (isDarkEmpty) com.jayra.app.ui.theme.IOSSecondaryLabelDark else IOSSecondaryLabel
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.ChatBubbleOutline,
            null,
            tint = subtitleColor,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        AppText("Belum ada chat", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
        Spacer(Modifier.height(4.dp))
        AppText(
            "Mulai percakapan baru",
            fontSize = 15.sp,
            color = subtitleColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SwipeableChatRow(
    chat: Chat,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMarkRead: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val view = LocalView.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onArchive()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showDeleteDialog = true
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                val color = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> IOSBlue
                    SwipeToDismissBoxValue.EndToStart -> IOSRed
                    else -> Color.Transparent
                }
                Box(modifier = Modifier.fillMaxSize().background(color), contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) {
                    Icon(
                        imageVector = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Rounded.Archive else Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp).size(28.dp)
                    )
                }
            }
        }
    ) {
        ChatRowContent(chat, isSelectionMode, isSelected, onClick, onPin, onMute, onArchive, onDelete, { showMenu = true })
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus chat ini?") },
            text = { Text("Chat \"${chat.name}\" akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("Hapus", color = IOSRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            com.jayra.app.ui.auth.IOSGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosGlassEntrance(delayMillis = 0),
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(0.dp)
            ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                AppText(chat.name, fontSize = 13.sp, color = if (isSystemInDarkTheme()) com.jayra.app.ui.theme.IOSSecondaryLabelDark else IOSSecondaryLabel, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isSystemInDarkTheme()) com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark else com.jayra.app.ui.theme.IOSSecondarySystemBackground)) {
                ActionSheetItem(
                    Icons.Rounded.Star,
                    if (chat.isFavorite) "Hapus dari Favorit" else "Favorit",
                    iconTint = IOSOrange
                ) { onToggleFavorite(); showMenu = false }
                ActionSheetItem(Icons.Rounded.MarkChatRead, "Tandai Dibaca", showDivider = true) { onMarkRead(); showMenu = false }
                ActionSheetItem(Icons.Rounded.PushPin, if (chat.isPinned) "Lepas Pin" else "Sematkan", showDivider = true) { onPin(); showMenu = false }
                ActionSheetItem(Icons.Rounded.NotificationsOff, if (chat.isMuted) "Bunyikan" else "Bisukan", showDivider = true) { onMute(); showMenu = false }
                ActionSheetItem(Icons.Rounded.Archive, "Arsipkan", showDivider = true) { onArchive(); showMenu = false }
                ActionSheetItem(Icons.Rounded.Delete, "Hapus", isDestructive = true, showDivider = true) { onDelete(); showMenu = false }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isSystemInDarkTheme()) com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark else com.jayra.app.ui.theme.IOSSecondarySystemBackground).clickable { showMenu = false }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    AppText("Batal", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IOSBlue)
                }
                Spacer(Modifier.height(8.dp))
            }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRowContent(
    chat: Chat,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isPressed) androidx.compose.ui.graphics.Color(0x1F000000) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(0),
        label = "row_bg"
    )
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "item_press_jelly"
    )
    val highlightOverlay by animateColorAsState(
        targetValue = if (isPressed) {
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
        } else Color.Transparent,
        label = "item_press_highlight"
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(rowBg)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onClick()
                    },
                    onLongClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        if (!isSelectionMode) onLongPress()
                    }
                )
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = chat.avatarUrl.ifBlank { "https://ui-avatars.com/api/?name=${chat.name}&background=0A84FF&color=fff&size=112" },
                    contentDescription = chat.name,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = chat.isOnline,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(IOSGreen, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppText(chat.name, fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.02).sp, color = if (androidx.compose.foundation.isSystemInDarkTheme()) com.jayra.app.ui.theme.IOSLabelDark else com.jayra.app.ui.theme.IOSLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chat.isPinned) {
                            Icon(Icons.Rounded.PushPin, null, tint = IOSGray2, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        if (chat.isMuted) {
                            Icon(Icons.Rounded.NotificationsOff, null, tint = IOSGray2, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        AppText(chat.timestamp, fontSize = 13.sp, color = if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.hasDraft) {
                        AppText("Draf: ", fontSize = 15.sp, color = IOSRed, fontWeight = FontWeight.SemiBold)
                    }
                    if (chat.lastMessageIsOutgoing && !chat.hasDraft) {
                        AppText("Anda: ", fontSize = 15.sp, color = if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                    }
                    AppText(
                        text = if (chat.hasDraft) chat.draftText else chat.lastMessage,
                        fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        color = if (chat.hasDraft) IOSRed else if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                    )
                    when (chat.lastMessageStatus) {
                        MessageStatus.SENT -> Icon(Icons.Rounded.Check, null, tint = IOSGray2, modifier = Modifier.size(14.dp))
                        MessageStatus.DELIVERED -> Icon(Icons.Rounded.DoneAll, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(14.dp))
                        MessageStatus.READ -> Icon(Icons.Rounded.DoneAll, null, tint = IOSBlue, modifier = Modifier.size(14.dp))
                        else -> {}
                    }
                    if (chat.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(20.dp).background(IOSBlue, CircleShape), contentAlignment = Alignment.Center) {
                            AppText(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            if (isSelectionMode) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(if (isSelected) IOSBlue else Color.Transparent, CircleShape)
                        .border(1.5.dp, if (isSelected) IOSBlue else IOSGray2, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 78.dp, end = 16.dp)
                .height(0.5.dp)
                .background(com.jayra.app.ui.theme.IOSSeparator.copy(alpha = 0.36f))
        )
    }
}

@Composable
fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    iconTint: Color = if (isDestructive) IOSRed else IOSBlue,
    showDivider: Boolean = false,
    onClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    Column {
        if (showDivider) {
            Box(Modifier.fillMaxWidth().padding(start = 62.dp).height(0.5.dp).background(com.jayra.app.ui.theme.IOSSeparator.copy(alpha = 0.36f)))
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            AppText(label, fontSize = 17.sp, color = if (isDestructive) IOSRed else if (dark) com.jayra.app.ui.theme.IOSLabelDark else com.jayra.app.ui.theme.IOSLabel)
        }
    }
}

@Composable
fun ActionBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(icon, null, tint = if (isDestructive) Color.Red else Color(0xFF007AFF), modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        AppText(label, fontSize = 12.sp, color = if (isDestructive) Color.Red else Color(0xFF007AFF))
    }
}

@Composable
private fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = com.jayra.app.ui.theme.SFProFontFamily,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null
) {
    androidx.compose.material3.Text(
        text = text, modifier = modifier, color = color, fontSize = fontSize,
        fontWeight = fontWeight, fontFamily = fontFamily, letterSpacing = letterSpacing,
        maxLines = maxLines, overflow = overflow, textAlign = textAlign
    )
}

// ========== Shimmer skeleton untuk loading state ChatList (iOS style, tanpa blur) ==========
@Composable
fun ShimmerChatList() {
    val isDark = isSystemInDarkTheme()
    val shimmerBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val shimmerHighlight = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val corner = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(corner)
                        .background(shimmerBg)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBg)
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBg)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(corner)
                        .background(shimmerBg)
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(com.jayra.app.ui.theme.IOSSeparator.copy(alpha = 0.36f))
            )
        }
    }
}
