package com.jayra.app.ui.call

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.jayra.app.model.CallFilter
import com.jayra.app.model.CallHistoryUiState
import com.jayra.app.model.CallLog
import com.jayra.app.model.CallType
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun CallHistoryScreen(
    onBack: () -> Unit,
    onCallClick: (participantId: String, type: String) -> Unit,
    viewModel: CallHistoryViewModel = viewModel()
) {
    BackHandler {
        onBack()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    CallHistoryContent(
        uiState = uiState,
        selectedTab = selectedTab,
        onBack = onBack,
        onTabSelected = viewModel::onTabChange,
        onCallClick = onCallClick,
        onRetry = viewModel::loadCalls
    )
}

@Composable
fun CallHistoryContent(
    uiState: CallHistoryUiState,
    selectedTab: CallFilter,
    onBack: () -> Unit,
    onTabSelected: (CallFilter) -> Unit,
    onCallClick: (participantId: String, type: String) -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarColor = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val backgroundColor = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val contentTextColor = if (isDark) Color.White else Color(0xFF222222)
    val dividerColor = if (isDark) Color(0xFF1E2834) else Color(0xFFE5E5E5)

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarColor)
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
                        onClick = onBack,
                        modifier = Modifier.testTag("call_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Riwayat Panggilan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                }

                // Tab Bar
                val tabs = listOf(
                    CallFilter.ALL to "Semua",
                    CallFilter.MISSED to "Terlewat"
                )
                val selectedIndex = if (selectedTab == CallFilter.ALL) 0 else 1

                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = topBarColor,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            height = 2.dp,
                            color = JayraBlue
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, (filter, title) ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { onTabSelected(filter) },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedIndex == index) Color.White else Color.White.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier.testTag("call_tab_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is CallHistoryUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = JayraBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                is CallHistoryUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E2834) else Color(0xFFE8EEF3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = JayraBlue,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Belum ada riwayat panggilan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = JayraSubtext,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is CallHistoryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.message,
                            fontSize = 14.sp,
                            color = JayraSubtext,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                            modifier = Modifier.testTag("call_history_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Coba Lagi", fontSize = 14.sp)
                        }
                    }
                }

                is CallHistoryUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("call_history_list")
                    ) {
                        items(uiState.calls, key = { it.id.ifEmpty { "${it.timestamp}_${it.participantId}" } }) { call ->
                            CallItemRow(
                                call = call,
                                isDark = isDark,
                                contentTextColor = contentTextColor,
                                onClick = {
                                    onCallClick(call.participantId, if (call.isVideo) "video" else "voice")
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallItemRow(
    call: CallLog,
    isDark: Boolean,
    contentTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar 56dp
        if (call.participantAvatar.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(call.participantAvatar)
                    .crossfade(true)
                    .build(),
                contentDescription = call.participantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
        } else {
            val initials = call.participantName.take(2).uppercase().ifEmpty { "?" }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(JayraBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = call.participantName.ifEmpty { "Pengguna" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = contentTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (callIcon, iconColor) = when (call.type) {
                    CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF2ECC71) // Hijau
                    CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF3498DB) // Biru Jayra
                    CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFE74C3C) // Merah
                    CallType.VOICE -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF3498DB)
                    CallType.VIDEO -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF3498DB)
                }

                Icon(
                    imageVector = callIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = call.timeFormatted.ifEmpty { "Baru saja" },
                    fontSize = 13.sp,
                    color = JayraSubtext
                )
            }
        }

        // Trailing Call Icon (24dp)
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag("call_action_${call.id}")
        ) {
            Icon(
                imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = if (call.isVideo) "Panggilan Video" else "Panggilan Suara",
                tint = JayraSubtext,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(name = "Call History Light", showBackground = true)
@Composable
fun CallHistoryScreenPreviewLight() {
    JayraTheme(darkTheme = false) {
        CallHistoryContent(
            uiState = CallHistoryUiState.Success(
                listOf(
                    CallLog(
                        id = "1",
                        participantId = "user_1",
                        participantName = "Pavel Durov",
                        type = CallType.OUTGOING,
                        timeFormatted = "Kemarin, 18:20",
                        isVideo = false
                    ),
                    CallLog(
                        id = "2",
                        participantId = "user_2",
                        participantName = "Alice Martin",
                        type = CallType.MISSED,
                        timeFormatted = "Hari ini, 09:12",
                        isVideo = true
                    ),
                    CallLog(
                        id = "3",
                        participantId = "user_3",
                        participantName = "Bob Designer",
                        type = CallType.INCOMING,
                        timeFormatted = "2 Okt, 14:05",
                        isVideo = false
                    )
                )
            ),
            selectedTab = CallFilter.ALL,
            onBack = {},
            onTabSelected = {},
            onCallClick = { _, _ -> },
            onRetry = {}
        )
    }
}

@Preview(name = "Call History Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CallHistoryScreenPreviewDark() {
    JayraTheme(darkTheme = true) {
        CallHistoryContent(
            uiState = CallHistoryUiState.Success(
                listOf(
                    CallLog(
                        id = "1",
                        participantId = "user_1",
                        participantName = "Pavel Durov",
                        type = CallType.OUTGOING,
                        timeFormatted = "Kemarin, 18:20",
                        isVideo = false
                    ),
                    CallLog(
                        id = "2",
                        participantId = "user_2",
                        participantName = "Alice Martin",
                        type = CallType.MISSED,
                        timeFormatted = "Hari ini, 09:12",
                        isVideo = true
                    )
                )
            ),
            selectedTab = CallFilter.MISSED,
            onBack = {},
            onTabSelected = {},
            onCallClick = { _, _ -> },
            onRetry = {}
        )
    }
}
