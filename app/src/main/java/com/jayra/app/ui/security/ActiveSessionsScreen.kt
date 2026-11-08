package com.jayra.app.ui.security

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.ActiveSessionsUiState
import com.jayra.app.model.Session
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun ActiveSessionsScreen(
    onBack: () -> Unit,
    viewModel: ActiveSessionsViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTerminateAllDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .testTag("active_sessions_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Perangkat Aktif",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = screenBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ActiveSessionsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = JayraBlue)
                    }
                }
                is ActiveSessionsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = JayraDangerRed, fontSize = 15.sp)
                    }
                }
                is ActiveSessionsUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Devices,
                            contentDescription = null,
                            tint = JayraSubtext,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tidak ada sesi aktif lain",
                            fontSize = 16.sp,
                            color = JayraSubtext
                        )
                    }
                }
                is ActiveSessionsUiState.Success -> {
                    ActiveSessionsContent(
                        sessions = state.sessions,
                        onEndSession = viewModel::onEndSession,
                        onTerminateAllClick = { showTerminateAllDialog = true }
                    )
                }
            }
        }
    }

    if (showTerminateAllDialog) {
        AlertDialog(
            onDismissRequest = { showTerminateAllDialog = false },
            title = { Text(text = "Akhiri Semua Sesi Lain?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Text(
                    text = "Semua perangkat lain akan dikeluarkan dari akun Jayra Anda. Anda harus login kembali pada perangkat tersebut.",
                    fontSize = 14.sp,
                    color = JayraSubtext
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEndAllOtherSessions()
                        showTerminateAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JayraDangerRed),
                    modifier = Modifier.testTag("confirm_terminate_all_button")
                ) {
                    Text(text = "Akhiri Semua", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTerminateAllDialog = false }) {
                    Text(text = "Batal", color = JayraSubtext)
                }
            }
        )
    }
}

@Composable
fun ActiveSessionsContent(
    sessions: List<Session>,
    onEndSession: (String) -> Unit,
    onTerminateAllClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val separatorColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    val hasOtherSessions = sessions.any { !it.isCurrent }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("active_sessions_list")
    ) {
        // 2.3 Tombol "Akhiri Semua Sesi Lain"
        if (hasOtherSessions) {
            item {
                Button(
                    onClick = onTerminateAllClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JayraDangerRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp)
                        .testTag("terminate_all_sessions_button")
                ) {
                    Text(
                        text = "Akhiri Semua Sesi Lain",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        item {
            Text(
                text = "DAFTAR SESI PERANGKAT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = JayraBlue,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        items(sessions, key = { it.id }) { session ->
            SessionItemRow(
                session = session,
                cardBg = cardBg,
                separatorColor = separatorColor,
                onEndSession = { onEndSession(session.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SessionItemRow(
    session: Session,
    cardBg: Color,
    separatorColor: Color,
    onEndSession: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF222222)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = if (session.isDesktop) Icons.Rounded.Computer else Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = JayraBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.deviceName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (session.isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(JayraSuccessGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Perangkat Ini",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = JayraSuccessGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${session.location} • ${session.ipAddress}",
                    fontSize = 15.sp,
                    color = JayraSubtext,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${session.appVersion} — ${session.lastActive}",
                    fontSize = 13.sp,
                    color = JayraSubtext,
                    maxLines = 1
                )
            }

            // More Menu for non-current sessions
            if (!session.isCurrent) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("session_menu_${session.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Opsi Sesi",
                            tint = JayraSubtext,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Akhiri Sesi",
                                    color = JayraDangerRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEndSession()
                            },
                            modifier = Modifier.testTag("terminate_session_${session.id}")
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            thickness = 0.5.dp,
            color = separatorColor
        )
    }
}

@Preview(name = "Active Sessions Light", showBackground = true)
@Composable
fun ActiveSessionsScreenPreviewLight() {
    JayraTheme(darkTheme = false) {
        ActiveSessionsContent(
            sessions = listOf(
                Session(
                    id = "1",
                    deviceName = "Jayra Android",
                    location = "Jakarta, Indonesia",
                    lastActive = "Online sekarang",
                    isCurrent = true,
                    ipAddress = "103.144.17.82",
                    isDesktop = false
                ),
                Session(
                    id = "2",
                    deviceName = "Jayra Desktop",
                    location = "Bandung, Indonesia",
                    lastActive = "2 jam yang lalu",
                    isCurrent = false,
                    ipAddress = "180.252.164.12",
                    isDesktop = true
                )
            ),
            onEndSession = {},
            onTerminateAllClick = {}
        )
    }
}

@Preview(name = "Active Sessions Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ActiveSessionsScreenPreviewDark() {
    JayraTheme(darkTheme = true) {
        ActiveSessionsContent(
            sessions = listOf(
                Session(
                    id = "1",
                    deviceName = "Jayra Android",
                    location = "Jakarta, Indonesia",
                    lastActive = "Online sekarang",
                    isCurrent = true,
                    ipAddress = "103.144.17.82",
                    isDesktop = false
                )
            ),
            onEndSession = {},
            onTerminateAllClick = {}
        )
    }
}
