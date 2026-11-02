package com.example.ui.calls

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CallFilter
import com.example.model.CallLog
import com.example.model.CallType
import com.example.model.CallsUiState
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSearchDark
import com.example.ui.theme.TelegramSearchLight
import com.example.ui.theme.TelegramSeparatorDark
import com.example.ui.theme.TelegramSeparatorLight
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramSuccessGreen
import com.example.ui.viewmodel.CallsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onBackClick: () -> Unit,
    onNavigateToContactInfo: (String) -> Unit = {},
    viewModel: CallsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showNewCallDialog by remember { mutableStateOf(false) }

    CallsScreenContent(
        uiState = uiState,
        currentFilter = filter,
        isSearching = isSearching,
        searchQuery = searchQuery,
        onFilterSelected = viewModel::setFilter,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onToggleSearch = viewModel::toggleSearch,
        onBackClick = onBackClick,
        onStartVoiceCall = { participantId, name ->
            viewModel.startVoiceCall(context, participantId, name)
        },
        onStartVideoCall = { participantId, name ->
            viewModel.startVideoCall(context, participantId, name)
        },
        onDeleteCall = viewModel::deleteCall,
        onBlockContact = viewModel::blockContact,
        onContactInfo = onNavigateToContactInfo,
        onFabClick = { showNewCallDialog = true }
    )

    if (showNewCallDialog) {
        NewCallDialog(
            onDismiss = { showNewCallDialog = false },
            onCallVoice = { id, name ->
                showNewCallDialog = false
                viewModel.startVoiceCall(context, id, name)
            },
            onCallVideo = { id, name ->
                showNewCallDialog = false
                viewModel.startVideoCall(context, id, name)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreenContent(
    uiState: CallsUiState,
    currentFilter: CallFilter,
    isSearching: Boolean,
    searchQuery: String,
    onFilterSelected: (CallFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onStartVoiceCall: (String, String) -> Unit,
    onStartVideoCall: (String, String) -> Unit,
    onDeleteCall: (String) -> Unit,
    onBlockContact: (String) -> Unit,
    onContactInfo: (String) -> Unit,
    onFabClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramCardDark else TelegramCardLight
    var showMoreMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calls_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
            ) {
                // TopAppBar 56dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("calls_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }

                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    "Cari panggilan...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 17.sp
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calls_search_input")
                        )
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    onSearchQueryChange("")
                                } else {
                                    onToggleSearch(false)
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Tutup Pencarian",
                                tint = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "Panggilan",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )

                        IconButton(
                            onClick = { onToggleSearch(true) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("calls_search_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = Color.White
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("calls_more_options")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opsi Lainnya",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pengaturan Panggilan") },
                                    onClick = { showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus Semua Riwayat") },
                                    onClick = { showMoreMenu = false }
                                )
                            }
                        }
                    }
                }

                // Telegram Calls Tab Bar (Semua / Terlewat)
                CallsTabBar(
                    selectedFilter = currentFilter,
                    onFilterSelected = onFilterSelected
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = TelegramBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_new_call")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Panggilan Baru",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(screenBg)
        ) {
            when (uiState) {
                is CallsUiState.Loading -> {
                    CallsShimmerList()
                }
                is CallsUiState.Empty -> {
                    CallsEmptyState()
                }
                is CallsUiState.Error -> {
                    CallsErrorState(message = uiState.message)
                }
                is CallsUiState.Success -> {
                    if (uiState.calls.isEmpty()) {
                        CallsEmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("calls_list")
                        ) {
                            items(
                                items = uiState.calls,
                                key = { it.id }
                            ) { call ->
                                CallItemRow(
                                    call = call,
                                    onStartCall = {
                                        if (call.isVideo) {
                                            onStartVideoCall(call.participantId, call.participantName)
                                        } else {
                                            onStartVoiceCall(call.participantId, call.participantName)
                                        }
                                    },
                                    onDeleteCall = { onDeleteCall(call.id) },
                                    onVoiceCall = { onStartVoiceCall(call.participantId, call.participantName) },
                                    onVideoCall = { onStartVideoCall(call.participantId, call.participantName) },
                                    onBlockUser = { onBlockContact(call.participantId) },
                                    onContactInfo = { onContactInfo(call.participantId) }
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
fun CallsTabBar(
    selectedFilter: CallFilter,
    onFilterSelected: (CallFilter) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val barBg = if (isDark) TelegramCardDark else TelegramCardLight
    val activeColor = TelegramBlue
    val inactiveColor = TelegramSubtext

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(barBg),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CallTabItem(
            title = "Semua",
            isSelected = selectedFilter == CallFilter.ALL,
            onClick = { onFilterSelected(CallFilter.ALL) },
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            modifier = Modifier.weight(1f)
        )
        CallTabItem(
            title = "Terlewat",
            isSelected = selectedFilter == CallFilter.MISSED,
            onClick = { onFilterSelected(CallFilter.MISSED) },
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CallTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "tab_text_color"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(2.dp)
                .background(if (isSelected) activeColor else Color.Transparent)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallItemRow(
    call: CallLog,
    onStartCall: () -> Unit,
    onDeleteCall: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onBlockUser: () -> Unit,
    onContactInfo: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val itemBg = if (isDark) TelegramCardDark else TelegramCardLight
    val textColor = if (isDark) Color.White else Color.Black
    val dividerColor = if (isDark) TelegramSeparatorDark else TelegramSeparatorLight

    var showMenu by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteCall()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onStartCall()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bg = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> TelegramDangerRed
                SwipeToDismissBoxValue.StartToEnd -> TelegramSuccessGreen
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                }
            ) {
                if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Riwayat",
                            tint = Color.White
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Panggil",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Panggil", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(itemBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clickable { showMenu = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar 50dp
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(TelegramBlue.copy(alpha = 0.2f))
                        .clickable { onContactInfo() },
                    contentAlignment = Alignment.Center
                ) {
                    if (call.participantAvatar.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(call.participantAvatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = call.participantName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = call.participantName.take(1).uppercase(),
                            color = TelegramBlue,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Detail Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = call.participantName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (icon, tint) = when (call.type) {
                            CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to TelegramSuccessGreen
                            CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to TelegramBlue
                            CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed to TelegramDangerRed
                            CallType.VOICE -> Icons.AutoMirrored.Filled.CallMade to TelegramBlue
                            CallType.VIDEO -> Icons.AutoMirrored.Filled.CallMade to TelegramBlue
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = call.timeFormatted,
                            fontSize = 14.sp,
                            color = TelegramSubtext
                        )

                        if (call.isVideo) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = TelegramSubtext,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Trailing Call Button (48dp target)
                IconButton(
                    onClick = onStartCall,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("call_action_button_${call.id}")
                ) {
                    Icon(
                        imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Panggil Balik",
                        tint = TelegramBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Panggil Suara") },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = TelegramBlue) },
                        onClick = {
                            showMenu = false
                            onVoiceCall()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Panggil Video") },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = TelegramBlue) },
                        onClick = {
                            showMenu = false
                            onVideoCall()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Info Kontak") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onContactInfo()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Hapus dari Riwayat", color = TelegramDangerRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TelegramDangerRed) },
                        onClick = {
                            showMenu = false
                            onDeleteCall()
                        }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                thickness = 0.5.dp,
                color = dividerColor
            )
        }
    }
}

@Composable
fun CallsShimmerList() {
    val infiniteTransition = rememberInfiniteTransition(label = "calls_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "calls_shimmer_alpha"
    )

    val isDark = isSystemInDarkTheme()
    val shimmerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

    Column(modifier = Modifier.fillMaxSize()) {
        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(shimmerColor.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor.copy(alpha = alpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(shimmerColor.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
fun CallsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TelegramBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = TelegramBlue,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tidak Ada Panggilan Terkini",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) Color.White else Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Panggilan suara dan video berkualitas tinggi Anda akan tersimpan rapi di sini.",
            fontSize = 14.sp,
            color = TelegramSubtext,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun CallsErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Gagal memuat riwayat panggilan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TelegramDangerRed
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = TelegramSubtext,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NewCallDialog(
    onDismiss: () -> Unit,
    onCallVoice: (String, String) -> Unit,
    onCallVideo: (String, String) -> Unit
) {
    val contacts = listOf(
        "pavel_durov" to "Pavel Durov",
        "alex_rivera" to "Alex Rivera",
        "david_kim" to "David Kim",
        "elena_p" to "Elena Petrova",
        "jessica_t" to "Jessica Taylor"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Panggilan Baru",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pilih kontak untuk memulai panggilan:",
                    fontSize = 14.sp,
                    color = TelegramSubtext,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(contacts) { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clickable { onCallVoice(id, name) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = TelegramBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onCallVoice(id, name) }) {
                                Icon(Icons.Default.Call, contentDescription = "Voice", tint = TelegramBlue)
                            }
                            IconButton(onClick = { onCallVideo(id, name) }) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = TelegramBlue)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TelegramBlue)
            }
        }
    )
}

// ==========================================
// PREVIEWS (Light & Dark Mode)
// ==========================================

@Preview(name = "Calls Screen Light", showBackground = true)
@Composable
fun CallsScreenLightPreview() {
    val sampleCalls = listOf(
        CallLog(
            id = "c1",
            participantId = "pavel_durov",
            participantName = "Pavel Durov",
            participantAvatar = "",
            type = CallType.INCOMING,
            duration = 180L,
            timeFormatted = "10:45 AM",
            isVideo = false
        ),
        CallLog(
            id = "c2",
            participantId = "elena_p",
            participantName = "Elena Petrova",
            participantAvatar = "",
            type = CallType.MISSED,
            duration = 0L,
            timeFormatted = "08:15 AM",
            isVideo = true
        )
    )

    CallsScreenContent(
        uiState = CallsUiState.Success(sampleCalls),
        currentFilter = CallFilter.ALL,
        isSearching = false,
        searchQuery = "",
        onFilterSelected = {},
        onSearchQueryChange = {},
        onToggleSearch = {},
        onBackClick = {},
        onStartVoiceCall = { _, _ -> },
        onStartVideoCall = { _, _ -> },
        onDeleteCall = {},
        onBlockContact = {},
        onContactInfo = {},
        onFabClick = {}
    )
}

@Preview(name = "Calls Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CallsScreenDarkPreview() {
    val sampleCalls = listOf(
        CallLog(
            id = "c1",
            participantId = "pavel_durov",
            participantName = "Pavel Durov",
            participantAvatar = "",
            type = CallType.INCOMING,
            duration = 180L,
            timeFormatted = "10:45 AM",
            isVideo = false
        ),
        CallLog(
            id = "c2",
            participantId = "elena_p",
            participantName = "Elena Petrova",
            participantAvatar = "",
            type = CallType.MISSED,
            duration = 0L,
            timeFormatted = "08:15 AM",
            isVideo = true
        )
    )

    CallsScreenContent(
        uiState = CallsUiState.Success(sampleCalls),
        currentFilter = CallFilter.MISSED,
        isSearching = false,
        searchQuery = "",
        onFilterSelected = {},
        onSearchQueryChange = {},
        onToggleSearch = {},
        onBackClick = {},
        onStartVoiceCall = { _, _ -> },
        onStartVideoCall = { _, _ -> },
        onDeleteCall = {},
        onBlockContact = {},
        onContactInfo = {},
        onFabClick = {}
    )
}
