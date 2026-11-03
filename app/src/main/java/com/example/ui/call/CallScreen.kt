package com.example.ui.call

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.model.CallParticipant
import com.example.model.CallUiState
import com.example.model.User
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSeparatorDark
import com.example.ui.theme.TelegramSeparatorLight
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramSuccessGreen
import com.example.ui.theme.TelegramTheme

@Composable
fun CallScreen(
    participantId: String,
    isVideo: Boolean,
    onBack: () -> Unit,
    viewModel: CallViewModel = viewModel()
) {
    val context = LocalContext.current

    BackHandler {
        viewModel.onEndCall()
        onBack()
    }

    LaunchedEffect(participantId, isVideo) {
        viewModel.initCall(
            callId = "",
            targetParticipantId = participantId,
            videoCall = isVideo
        )
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEndCall()
        }
    }

    CallScreenContent(
        uiState = uiState,
        isVideo = isVideo,
        onBack = onBack,
        onMuteToggle = viewModel::onMuteToggle,
        onVideoToggle = viewModel::onVideoToggle,
        onSpeakerToggle = viewModel::onSpeakerToggle,
        onSwitchCamera = viewModel::onSwitchCamera,
        onAddParticipant = viewModel::onAddParticipant,
        onEndCall = viewModel::onEndCall,
        onRetry = {
            viewModel.initCall("", participantId, isVideo)
        }
    )
}

@Composable
fun CallScreenContent(
    uiState: CallUiState,
    isVideo: Boolean,
    onBack: () -> Unit,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onSwitchCamera: () -> Unit = {},
    onAddParticipant: (String) -> Unit = {},
    onEndCall: () -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var showAddParticipantDialog by remember { mutableStateOf(false) }

    val availableContacts = when (uiState) {
        is CallUiState.Connecting -> uiState.availableContacts
        is CallUiState.Connected -> uiState.availableContacts
        else -> emptyList()
    }

    if (showAddParticipantDialog) {
        AddCallParticipantDialog(
            contacts = availableContacts,
            onSelectContact = { selectedUserId ->
                onAddParticipant(selectedUserId)
                showAddParticipantDialog = false
            },
            onDismiss = { showAddParticipantDialog = false }
        )
    }

    when (uiState) {
        is CallUiState.Ended -> {
            AlertDialog(
                onDismissRequest = onBack,
                shape = RoundedCornerShape(16.dp),
                containerColor = if (isDark) TelegramCardDark else TelegramCardLight,
                title = {
                    Text(
                        text = "Panggilan Berakhir",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
                    )
                },
                text = {
                    Text(
                        text = "Durasi panggilan: ${uiState.duration}",
                        fontSize = 16.sp,
                        color = TelegramSubtext
                    )
                },
                confirmButton = {
                    TextButton(onClick = onBack) {
                        Text(
                            text = "Tutup",
                            color = TelegramBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            )
        }

        is CallUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) TelegramDarkChatBg else Color(0xFF1E2833)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = TelegramDangerRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Koneksi gagal",
                        fontSize = 17.sp,
                        color = Color(0xFFFFFFFF)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Coba Lagi", color = Color(0xFFFFFFFF), fontSize = 15.sp)
                    }
                }
            }
        }

        is CallUiState.Connecting -> {
            val isGroupCall = uiState.participants.size > 2
            when {
                isGroupCall -> {
                    GroupVideoCallLayout(
                        groupTitle = uiState.participantName,
                        participants = uiState.participants,
                        isConnecting = true,
                        duration = "00:00",
                        isMuted = false,
                        isVideoEnabled = uiState.isVideoCall,
                        isSpeakerOn = true,
                        onBack = onBack,
                        onMuteToggle = onMuteToggle,
                        onVideoToggle = onVideoToggle,
                        onSpeakerToggle = onSpeakerToggle,
                        onSwitchCamera = onSwitchCamera,
                        onOpenAddParticipant = { showAddParticipantDialog = true },
                        onEndCall = onEndCall
                    )
                }
                uiState.isVideoCall || isVideo -> {
                    VideoCallLayout(
                        participantName = uiState.participantName,
                        participantAvatar = uiState.participantAvatar,
                        isConnecting = true,
                        duration = "00:00",
                        isMuted = false,
                        isVideoEnabled = true,
                        isSpeakerOn = true,
                        isFrontCamera = uiState.isFrontCamera,
                        onBack = onBack,
                        onMuteToggle = onMuteToggle,
                        onVideoToggle = onVideoToggle,
                        onSpeakerToggle = onSpeakerToggle,
                        onSwitchCamera = onSwitchCamera,
                        onOpenAddParticipant = { showAddParticipantDialog = true },
                        onEndCall = onEndCall
                    )
                }
                else -> {
                    VoiceCallLayout(
                        participantName = uiState.participantName,
                        participantAvatar = uiState.participantAvatar,
                        isConnecting = true,
                        duration = "00:00",
                        isMuted = false,
                        isSpeakerOn = false,
                        onBack = onBack,
                        onMuteToggle = onMuteToggle,
                        onSpeakerToggle = onSpeakerToggle,
                        onSwitchCamera = onSwitchCamera,
                        onOpenAddParticipant = { showAddParticipantDialog = true },
                        onEndCall = onEndCall
                    )
                }
            }
        }

        is CallUiState.Connected -> {
            val isGroupCall = uiState.participants.size > 2
            when {
                isGroupCall -> {
                    GroupVideoCallLayout(
                        groupTitle = uiState.participantName,
                        participants = uiState.participants,
                        isConnecting = false,
                        duration = uiState.duration,
                        isMuted = uiState.isMuted,
                        isVideoEnabled = uiState.isVideoEnabled,
                        isSpeakerOn = uiState.isSpeakerOn,
                        onBack = onBack,
                        onMuteToggle = onMuteToggle,
                        onVideoToggle = onVideoToggle,
                        onSpeakerToggle = onSpeakerToggle,
                        onSwitchCamera = onSwitchCamera,
                        onOpenAddParticipant = { showAddParticipantDialog = true },
                        onEndCall = onEndCall
                    )
                }
                uiState.isVideoCall -> {
                    VideoCallLayout(
                        participantName = uiState.participantName,
                        participantAvatar = uiState.participantAvatar,
                        isConnecting = false,
                        duration = uiState.duration,
                        isMuted = uiState.isMuted,
                        isVideoEnabled = uiState.isVideoEnabled,
                        isSpeakerOn = uiState.isSpeakerOn,
                        isFrontCamera = uiState.isFrontCamera,
                        onBack = onBack,
                        onMuteToggle = onMuteToggle,
                        onVideoToggle = onVideoToggle,
                        onSpeakerToggle = onSpeakerToggle,
                        onSwitchCamera = onSwitchCamera,
                        onOpenAddParticipant = { showAddParticipantDialog = true },
                        onEndCall = onEndCall
                    )
                }
                else -> {
                    VoiceCallLayout(
                        participantName = uiState.participantName,
                        participantAvatar = uiState.participantAvatar,
                        isConnecting = false,
                        duration = uiState.duration,
                        isMuted = uiState.isMuted,
                        isSpeakerOn = uiState.isSpeakerOn,
                        onBack = onBack,
                        onMuteToggle = onMuteToggle,
                        onSpeakerToggle = onSpeakerToggle,
                        onSwitchCamera = onSwitchCamera,
                        onOpenAddParticipant = { showAddParticipantDialog = true },
                        onEndCall = onEndCall
                    )
                }
            }
        }
    }
}

/**
 * 17.1 Group Call Layout: LazyVerticalGrid (2 kolom) menampilkan video seluruh peserta.
 */
@Composable
private fun GroupVideoCallLayout(
    groupTitle: String,
    participants: List<CallParticipant>,
    isConnecting: Boolean,
    duration: String,
    isMuted: Boolean,
    isVideoEnabled: Boolean,
    isSpeakerOn: Boolean,
    onBack: () -> Unit,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenAddParticipant: () -> Unit,
    onEndCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1621))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header Grup Call (56dp)
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
                    .testTag("group_call_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Tutup",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = groupTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isConnecting) {
                        "Menghubungkan • ${participants.size} peserta"
                    } else {
                        "$duration • ${participants.size} peserta"
                    },
                    fontSize = 12.sp,
                    color = TelegramSubtext
                )
            }

            IconButton(
                onClick = onOpenAddParticipant,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("call_topbar_add_participant_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Tambah Peserta",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // LazyVerticalGrid 2 Kolom untuk semua peserta Grup Call
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("group_call_video_grid")
        ) {
            items(
                items = participants,
                key = { it.id }
            ) { participant ->
                GroupParticipantVideoTile(participant = participant)
            }
        }

        CallControlsDock(
            isVideoCall = true,
            isMuted = isMuted,
            isVideoEnabled = isVideoEnabled,
            isSpeakerOn = isSpeakerOn,
            onMuteToggle = onMuteToggle,
            onVideoToggle = onVideoToggle,
            onSpeakerToggle = onSpeakerToggle,
            onSwitchCamera = onSwitchCamera,
            onAddParticipantClick = onOpenAddParticipant,
            onEndCall = onEndCall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun GroupParticipantVideoTile(
    participant: CallParticipant
) {
    val borderColor = if (participant.isSpeaking) {
        TelegramSuccessGreen
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.12f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (participant.isLocalUser) {
                        listOf(Color(0xFF243B55), Color(0xFF141E30))
                    } else {
                        listOf(Color(0xFF1F2C3A), Color(0xFF121A24))
                    }
                )
            )
            .border(
                width = if (participant.isSpeaking) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("group_call_tile_${participant.id}")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(TelegramBlue.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                if (participant.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(participant.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = participant.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = participant.name,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (participant.isLocalUser) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!participant.isVideoEnabled) {
                        "Kamera Mati"
                    } else if (participant.isFrontCamera) {
                        "Kamera Depan"
                    } else {
                        "Kamera Belakang"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFFFFFFFF).copy(alpha = 0.75f)
                )
            }
        }

        // Nama & indikator mikrofon di bagian bawah kartu
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0xFF000000).copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = participant.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = if (participant.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (participant.isMuted) "Dibisukan" else "Aktif",
                tint = if (participant.isMuted) TelegramDangerRed else TelegramSuccessGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 1-on-1 Video Call Layout (Tetap seperti sebelumnya + kontrol Switch Camera & Add Participant)
@Composable
private fun VideoCallLayout(
    participantName: String,
    participantAvatar: String,
    isConnecting: Boolean,
    duration: String,
    isMuted: Boolean,
    isVideoEnabled: Boolean,
    isSpeakerOn: Boolean,
    isFrontCamera: Boolean,
    onBack: () -> Unit,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenAddParticipant: () -> Unit,
    onEndCall: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse_connecting")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Remote Video / Avatar Container
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E2833), Color(0xFF0D141C))
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(TelegramBlue),
                    contentAlignment = Alignment.Center
                ) {
                    if (participantAvatar.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(participantAvatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = participantName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFFFFFFF),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = participantName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isConnecting) {
                    Text(
                        text = "Menghubungkan...",
                        fontSize = 15.sp,
                        color = TelegramSubtext.copy(alpha = alphaAnim)
                    )
                } else {
                    Text(
                        text = duration,
                        fontSize = 15.sp,
                        color = Color(0xFFFFFFFF).copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Local Video (PIP: 120x160dp, rounded 12dp, top end)
        if (isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 64.dp, end = 16.dp)
                    .size(width = 120.dp, height = 160.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C3E50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFrontCamera) "Kamera Depan" else "Kamera Belakang",
                    fontSize = 12.sp,
                    color = Color(0xFFFFFFFF).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .background(Color.Transparent)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("call_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Tutup",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Video Call",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onOpenAddParticipant,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("video_topbar_add_participant_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Tambah Peserta",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        CallControlsDock(
            isVideoCall = true,
            isMuted = isMuted,
            isVideoEnabled = isVideoEnabled,
            isSpeakerOn = isSpeakerOn,
            onMuteToggle = onMuteToggle,
            onVideoToggle = onVideoToggle,
            onSpeakerToggle = onSpeakerToggle,
            onSwitchCamera = onSwitchCamera,
            onAddParticipantClick = onOpenAddParticipant,
            onEndCall = onEndCall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
    }
}

// Voice Call Layout
@Composable
private fun VoiceCallLayout(
    participantName: String,
    participantAvatar: String,
    isConnecting: Boolean,
    duration: String,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onBack: () -> Unit,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenAddParticipant: () -> Unit,
    onEndCall: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar

    val transition = rememberInfiniteTransition(label = "pulse_connecting_voice")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Tutup",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Voice Call",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onOpenAddParticipant,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("voice_topbar_add_participant_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Tambah Peserta",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (participantAvatar.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(participantAvatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = participantName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = participantName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isConnecting) {
                Text(
                    text = "Menghubungkan...",
                    fontSize = 15.sp,
                    color = Color(0xFFFFFFFF).copy(alpha = alphaAnim)
                )
            } else {
                Text(
                    text = duration,
                    fontSize = 17.sp,
                    color = Color(0xFFFFFFFF)
                )
            }
        }

        CallControlsDock(
            isVideoCall = false,
            isMuted = isMuted,
            isVideoEnabled = false,
            isSpeakerOn = isSpeakerOn,
            onMuteToggle = onMuteToggle,
            onVideoToggle = {},
            onSpeakerToggle = onSpeakerToggle,
            onSwitchCamera = onSwitchCamera,
            onAddParticipantClick = onOpenAddParticipant,
            onEndCall = onEndCall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
    }
}

@Composable
private fun CallControlsDock(
    isVideoCall: Boolean,
    isMuted: Boolean,
    isVideoEnabled: Boolean,
    isSpeakerOn: Boolean,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onAddParticipantClick: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Baris Aksi Tambahan Fase 17: Switch Camera & Add Participant (tap target >= 48dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isVideoCall) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFFFFFF).copy(alpha = 0.18f))
                        .clickable(onClick = onSwitchCamera)
                        .padding(horizontal = 16.dp)
                        .testTag("call_switch_camera_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Ganti Kamera",
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ganti Kamera",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFFFFF)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.18f))
                    .clickable(onClick = onAddParticipantClick)
                    .padding(horizontal = 16.dp)
                    .testTag("call_add_participant_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Tambah Peserta",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tambah Peserta",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFFFFF)
                )
            }
        }

        // Baris Kontrol Utama
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMuteToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.2f))
                    .testTag("call_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = if (isMuted) TelegramDangerRed else Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isVideoCall) {
                IconButton(
                    onClick = onVideoToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFFFFF).copy(alpha = 0.2f))
                        .testTag("call_video_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Video",
                        tint = if (isVideoEnabled) Color(0xFFFFFFFF) else TelegramDangerRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TelegramDangerRed)
                    .testTag("call_end_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Akhiri Panggilan",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = onSpeakerToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.2f))
                    .testTag("call_speaker_button")
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) {
                        Icons.AutoMirrored.Filled.VolumeUp
                    } else {
                        Icons.AutoMirrored.Filled.VolumeOff
                    },
                    contentDescription = "Speaker",
                    tint = if (isSpeakerOn) TelegramBlue else Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCallParticipantDialog(
    contacts: List<User>,
    onSelectContact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) TelegramCardDark else TelegramCardLight
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val dividerColor = if (isDark) TelegramSeparatorDark else TelegramSeparatorLight

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = containerColor,
        title = {
            Text(
                text = "Tambah Peserta Panggilan",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        },
        text = {
            if (contacts.isEmpty()) {
                Text(
                    text = "Semua kontak sudah berada di dalam panggilan ini.",
                    fontSize = 14.sp,
                    color = TelegramSubtext
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .testTag("add_call_participant_list")
                ) {
                    items(
                        items = contacts,
                        key = { it.id }
                    ) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { onSelectContact(contact.id) }
                                .padding(horizontal = 8.dp)
                                .testTag("invite_participant_${contact.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(TelegramBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TelegramBlue
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = contact.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                    Text(
                                        text = contact.lastSeenFormatted,
                                        fontSize = 12.sp,
                                        color = TelegramSubtext
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Undang ${contact.name}",
                                tint = TelegramBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Batal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TelegramBlue
                )
            }
        }
    )
}

@Preview(name = "1-on-1 Video Call Light", showBackground = true)
@Composable
private fun CallScreenVideoLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            CallScreenContent(
                uiState = CallUiState.Connected(
                    participantName = "Pavel Durov",
                    duration = "03:45",
                    isVideoCall = true,
                    isMuted = false,
                    isVideoEnabled = true,
                    isSpeakerOn = true,
                    participants = listOf(
                        CallParticipant(id = "pavel_durov", name = "Pavel Durov", isSpeaking = true),
                        CallParticipant(id = "me", name = "Anda", isLocalUser = true)
                    )
                ),
                isVideo = true,
                onBack = {},
                onMuteToggle = {},
                onVideoToggle = {},
                onSpeakerToggle = {},
                onEndCall = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Group Call 2-Col Grid Dark", showBackground = true)
@Composable
private fun CallScreenGroupDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            CallScreenContent(
                uiState = CallUiState.Connected(
                    participantName = "Android Developers Community",
                    duration = "12:08",
                    isVideoCall = true,
                    isMuted = false,
                    isVideoEnabled = true,
                    isSpeakerOn = true,
                    participants = listOf(
                        CallParticipant(id = "me", name = "Anda", isLocalUser = true, isFrontCamera = true),
                        CallParticipant(id = "pavel", name = "Pavel Durov", isSpeaking = true),
                        CallParticipant(id = "elena", name = "Elena Petrova", isSpeaking = false),
                        CallParticipant(id = "alex", name = "Alex Rivera", isMuted = true)
                    )
                ),
                isVideo = true,
                onBack = {},
                onMuteToggle = {},
                onVideoToggle = {},
                onSpeakerToggle = {},
                onEndCall = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Voice Call Light", showBackground = true)
@Composable
private fun CallScreenVoiceLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            CallScreenContent(
                uiState = CallUiState.Connected(
                    participantName = "Elena Petrova",
                    duration = "01:12",
                    isVideoCall = false,
                    isMuted = true,
                    isVideoEnabled = false,
                    isSpeakerOn = false
                ),
                isVideo = false,
                onBack = {},
                onMuteToggle = {},
                onVideoToggle = {},
                onSpeakerToggle = {},
                onEndCall = {},
                onRetry = {}
            )
        }
    }
}
