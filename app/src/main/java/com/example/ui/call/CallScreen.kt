package com.example.ui.call

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CallUiState
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

@Composable
fun CallScreen(
    participantId: String,
    isVideo: Boolean,
    onBack: () -> Unit,
    viewModel: CallViewModel = viewModel()
) {
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
        onEndCall = {
            viewModel.onEndCall()
        },
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
    onEndCall: () -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var showMenu by remember { mutableStateOf(false) }

    when (uiState) {
        is CallUiState.Ended -> {
            AlertDialog(
                onDismissRequest = onBack,
                title = {
                    Text(
                        text = "Panggilan Berakhir",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Durasi panggilan: ${uiState.duration}",
                        fontSize = 16.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = onBack) {
                        Text("Tutup", color = TelegramBlue, fontWeight = FontWeight.SemiBold)
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
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Coba Lagi", color = Color.White)
                    }
                }
            }
        }

        is CallUiState.Connecting -> {
            if (uiState.isVideoCall) {
                VideoCallLayout(
                    participantName = uiState.participantName,
                    participantAvatar = uiState.participantAvatar,
                    isConnecting = true,
                    duration = "00:00",
                    isMuted = false,
                    isVideoEnabled = true,
                    isSpeakerOn = true,
                    onBack = onBack,
                    onMuteToggle = onMuteToggle,
                    onVideoToggle = onVideoToggle,
                    onSpeakerToggle = onSpeakerToggle,
                    onEndCall = onEndCall
                )
            } else {
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
                    onEndCall = onEndCall
                )
            }
        }

        is CallUiState.Connected -> {
            if (uiState.isVideoCall) {
                VideoCallLayout(
                    participantName = uiState.participantName,
                    participantAvatar = uiState.participantAvatar,
                    isConnecting = false,
                    duration = uiState.duration,
                    isMuted = uiState.isMuted,
                    isVideoEnabled = uiState.isVideoEnabled,
                    isSpeakerOn = uiState.isSpeakerOn,
                    onBack = onBack,
                    onMuteToggle = onMuteToggle,
                    onVideoToggle = onVideoToggle,
                    onSpeakerToggle = onSpeakerToggle,
                    onEndCall = onEndCall
                )
            } else {
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
                    onEndCall = onEndCall
                )
            }
        }
    }
}

// 1.2 Video Call Layout (Full Screen)
@Composable
private fun VideoCallLayout(
    participantName: String,
    participantAvatar: String,
    isConnecting: Boolean,
    duration: String,
    isMuted: Boolean,
    isVideoEnabled: Boolean,
    isSpeakerOn: Boolean,
    onBack: () -> Unit,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
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
            .background(Color.Black)
    ) {
        // Remote Video / Avatar Container
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Simulated / Active Remote Video Stream Background
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
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = participantName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Local Video (PIP: 120x160dp, rounded 12dp, top end, margin 16dp, shadow 4dp)
        if (isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 16.dp)
                    .size(width = 120.dp, height = 160.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C3E50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kamera Anda",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // 1.1 TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Video Call",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {},
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Kontrol Panggilan Video
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute Button 48dp
            IconButton(
                onClick = onMuteToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("call_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = if (isMuted) TelegramDangerRed else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Video Toggle Button 48dp
            IconButton(
                onClick = onVideoToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("call_video_toggle_button")
            ) {
                Icon(
                    imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Video",
                    tint = if (isVideoEnabled) Color.White else TelegramDangerRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            // End Call Button 64dp
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
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Speaker Toggle Button 48dp
            IconButton(
                onClick = onSpeakerToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("call_speaker_button")
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Speaker",
                    tint = if (isSpeakerOn) TelegramBlue else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// 1.3 Voice Call Layout
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
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Voice Call",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {},
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar 120dp CircleShape
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
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
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = participantName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isConnecting) {
                Text(
                    text = "Menghubungkan...",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = alphaAnim)
                )
            } else {
                Text(
                    text = duration,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        }

        // Kontrol Panggilan Suara
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute Button 48dp
            IconButton(
                onClick = onMuteToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("call_voice_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = if (isMuted) TelegramDangerRed else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // End Call Button 64dp
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TelegramDangerRed)
                    .testTag("call_voice_end_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Akhiri Panggilan",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Speaker Toggle Button 48dp
            IconButton(
                onClick = onSpeakerToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("call_voice_speaker_button")
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Speaker",
                    tint = if (isSpeakerOn) TelegramBlue else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(name = "Video Call Light", showBackground = true)
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
                    isSpeakerOn = true
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

@Preview(name = "Voice Call Dark", showBackground = true)
@Composable
private fun CallScreenVoiceDarkPreview() {
    TelegramTheme(darkTheme = true) {
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
