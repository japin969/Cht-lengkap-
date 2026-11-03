package com.jayra.app.ui.security

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.PasscodeUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun PasscodeScreen(
    onBack: () -> Unit,
    onSuccessUnlock: () -> Unit,
    viewModel: PasscodeViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

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
                        .testTag("passcode_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Kunci Sandi",
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
                is PasscodeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = JayraBlue)
                    }
                }
                is PasscodeUiState.Success -> {
                    onSuccessUnlock()
                }
                is PasscodeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = JayraDangerRed, fontSize = 15.sp)
                    }
                }
                is PasscodeUiState.Entering -> {
                    PasscodeContent(
                        state = state,
                        onDigitPress = { digit -> viewModel.onDigitPress(digit, onSuccessUnlock) },
                        onBackspace = viewModel::onBackspace,
                        onBiometricClick = { viewModel.onBiometricSuccess(onSuccessUnlock) },
                        onForgotPasswordClick = { showForgotPasswordDialog = true }
                    )
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text(text = "Lupa Sandi?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Text(
                    text = "Jika Anda lupa sandi, kode pemulihan telah dikirimkan ke Email Pemulihan terdaftar Anda (Verifikasi Dua Langkah).",
                    fontSize = 14.sp,
                    color = JayraSubtext
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    modifier = Modifier.testTag("forgot_password_confirm")
                ) {
                    Text(text = "Mengerti", color = JayraBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
fun PasscodeContent(
    state: PasscodeUiState.Entering,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val numberPadBg = if (isDark) JayraCardDark else JayraCardLight
    val numberTextColor = if (isDark) Color.White else Color(0xFF222222)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Masukkan Sandi",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = JayraSubtext,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3.2 6-Digit Boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalDigits = 6
            for (i in 0 until totalDigits) {
                val isFilled = i < state.currentInput.length
                val isActive = i == state.currentInput.length

                val boxBg by animateColorAsState(
                    targetValue = if (isFilled) JayraBlue.copy(alpha = 0.15f) else Color.Transparent,
                    animationSpec = tween(200),
                    label = "boxBg"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isActive || isFilled) JayraBlue else if (isDark) Color(0xFF38383A) else Color(0xFFD0D0D0),
                    animationSpec = tween(200),
                    label = "borderColor"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(boxBg)
                        .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFilled) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(JayraBlue)
                        )
                    }
                }
            }
        }

        // Error message or Lock countdown
        Spacer(modifier = Modifier.height(12.dp))
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = JayraDangerRed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3.2 Custom NumberPad Grid (3x4)
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("biometric", "0", "backspace")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { item ->
                        when (item) {
                            "backspace" -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onBackspace)
                                        .testTag("passcode_backspace"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Hapus",
                                        tint = JayraSubtext,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            "biometric" -> {
                                if (state.isBiometricEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable(onClick = onBiometricClick)
                                            .testTag("passcode_biometric_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Sidik Jari",
                                            tint = JayraBlue,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(64.dp))
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(numberPadBg)
                                        .clickable(enabled = !state.isLocked) {
                                            onDigitPress(item)
                                        }
                                        .testTag("passcode_digit_$item"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (state.isLocked) JayraSubtext else numberTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lupa Sandi
        Text(
            text = "Lupa Sandi?",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = JayraBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable(onClick = onForgotPasswordClick)
                .padding(8.dp)
                .testTag("forgot_passcode_button")
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(name = "Passcode Light", showBackground = true)
@Composable
fun PasscodeScreenPreviewLight() {
    JayraTheme(darkTheme = false) {
        PasscodeContent(
            state = PasscodeUiState.Entering(
                currentInput = "123",
                isBiometricEnabled = true,
                isLocked = false
            ),
            onDigitPress = {},
            onBackspace = {},
            onBiometricClick = {},
            onForgotPasswordClick = {}
        )
    }
}

@Preview(name = "Passcode Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PasscodeScreenPreviewDark() {
    JayraTheme(darkTheme = true) {
        PasscodeContent(
            state = PasscodeUiState.Entering(
                currentInput = "",
                isBiometricEnabled = true,
                isLocked = true,
                lockRemainingSeconds = 25,
                errorMessage = "Terlalu banyak percobaan salah. Tunggu 25 detik."
            ),
            onDigitPress = {},
            onBackspace = {},
            onBiometricClick = {},
            onForgotPasswordClick = {}
        )
    }
}
