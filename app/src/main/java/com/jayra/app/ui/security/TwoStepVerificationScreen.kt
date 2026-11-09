package com.jayra.app.ui.security

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.TwoStepData
import com.jayra.app.model.TwoStepVerificationUiState
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
fun TwoStepVerificationScreen(
    onBack: () -> Unit,
    viewModel: TwoStepVerificationViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var errorMessageDialog by remember { mutableStateOf<String?>(null) }

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
                        .testTag("two_step_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Verifikasi Dua Langkah",
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
                is TwoStepVerificationUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = JayraBlue)
                    }
                }
                is TwoStepVerificationUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = JayraDangerRed, fontSize = 15.sp)
                    }
                }
                is TwoStepVerificationUiState.Success -> {
                    TwoStepVerificationContent(
                        data = state.data,
                        onOpenPasswordDialog = { showPasswordDialog = true },
                        onOpenEmailDialog = { showEmailDialog = true },
                        onBiometricToggle = viewModel::onBiometricToggle,
                        onToggle2FA = { enabled ->
                            viewModel.onToggle2FA(
                                enabled = enabled,
                                onError = { msg -> errorMessageDialog = msg }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showPasswordDialog) {
        SetPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { pwd ->
                viewModel.onPasswordChange(
                    password = pwd,
                    onSuccess = { showPasswordDialog = false },
                    onError = { msg -> errorMessageDialog = msg }
                )
            }
        )
    }

    if (showEmailDialog) {
        val currentEmail = (uiState as? TwoStepVerificationUiState.Success)?.data?.recoveryEmail ?: ""
        SetEmailDialog(
            initialEmail = currentEmail,
            onDismiss = { showEmailDialog = false },
            onSave = { email ->
                viewModel.onEmailChange(
                    email = email,
                    onSuccess = { showEmailDialog = false },
                    onError = { msg -> errorMessageDialog = msg }
                )
            }
        )
    }

    errorMessageDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessageDialog = null },
            title = { Text(text = "Keamanan Tingkat Militer", fontWeight = FontWeight.Bold) },
            text = { Text(text = msg, fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = { errorMessageDialog = null },
                    modifier = Modifier.testTag("security_error_dismiss")
                ) {
                    Text(text = "Mengerti", color = JayraBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
fun TwoStepVerificationContent(
    data: TwoStepData,
    onOpenPasswordDialog: () -> Unit,
    onOpenEmailDialog: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onToggle2FA: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val textColor = if (isDark) Color.White else Color(0xFF222222)
    val separatorColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        // 1.2 Card Status (Military-Grade)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Status Keamanan",
                    fontSize = 15.sp,
                    color = JayraSubtext
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (data.isEnabled) "Aktif (Tingkat Militer)" else "Nonaktif",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (data.isEnabled) JayraSuccessGreen else JayraDangerRed
                    )
                    if (data.isEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = "Terlindungi",
                            tint = JayraSuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enkripsi AES-256-GCM + Keystore",
                    fontSize = 12.sp,
                    color = JayraSubtext
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1.3 Card Pengaturan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
        ) {
            // Item 1: Kata Sandi Kuat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPasswordDialog)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = JayraBlue,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kata Sandi Kuat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Text(
                        text = if (data.hasPassword) "Sudah diatur" else "Belum diatur",
                        fontSize = 14.sp,
                        color = if (data.hasPassword) JayraSuccessGreen else JayraSubtext
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = JayraSubtext,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.5.dp,
                color = separatorColor
            )

            // Item 2: Email Pemulihan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenEmailDialog)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null,
                    tint = JayraBlue,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Email Pemulihan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Text(
                        text = data.recoveryEmail.ifEmpty { "Belum diatur" },
                        fontSize = 14.sp,
                        color = if (data.recoveryEmail.isNotEmpty()) JayraSuccessGreen else JayraSubtext
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = JayraSubtext,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.5.dp,
                color = separatorColor
            )

            // Item 3: Kunci Biometrik
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Fingerprint,
                    contentDescription = null,
                    tint = JayraBlue,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kunci Biometrik",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Text(
                        text = "Gunakan sidik jari atau pengenalan wajah",
                        fontSize = 13.sp,
                        color = JayraSubtext
                    )
                }

                Switch(
                    checked = data.isBiometricEnabled,
                    onCheckedChange = onBiometricToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = JayraBlue
                    ),
                    modifier = Modifier.testTag("biometric_switch")
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.5.dp,
                color = separatorColor
            )

            // Item 4: Aktifkan Verifikasi Dua Langkah
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = JayraBlue,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aktifkan Verifikasi Dua Langkah",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Text(
                        text = "Wajib sandi tambahan saat login baru",
                        fontSize = 13.sp,
                        color = JayraSubtext
                    )
                }

                Switch(
                    checked = data.isEnabled,
                    onCheckedChange = onToggle2FA,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = JayraBlue
                    ),
                    modifier = Modifier.testTag("two_step_toggle")
                )
            }
        }
    }
}

@Composable
fun SetPasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Kata Sandi Kuat (12+ Karakter)", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column {
                Text(
                    text = "Wajib berisi minimal 12 karakter dengan huruf besar, huruf kecil, angka, dan simbol (@$!%*?&).",
                    fontSize = 13.sp,
                    color = JayraSubtext
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                    },
                    label = { Text("Kata Sandi Baru") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("two_step_password_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    label = { Text("Ulangi Kata Sandi") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth().testTag("two_step_password_confirm_input")
                )

                if (localError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = localError ?: "", color = JayraDangerRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password != confirmPassword) {
                        localError = "Konfirmasi kata sandi tidak cocok."
                        return@Button
                    }
                    onSave(password)
                },
                colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                modifier = Modifier.testTag("save_password_button")
            ) {
                Text("Simpan", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = JayraSubtext)
            }
        }
    )
}

@Composable
fun SetEmailDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Email Pemulihan", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column {
                Text(
                    text = "Email ini digunakan untuk memulihkan akses akun Jayra Anda jika lupa kata sandi.",
                    fontSize = 13.sp,
                    color = JayraSubtext
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Alamat Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth().testTag("recovery_email_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(email) },
                colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                modifier = Modifier.testTag("save_email_button")
            ) {
                Text("Simpan", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = JayraSubtext)
            }
        }
    )
}

@Preview(name = "Two Step Verification Light", showBackground = true)
@Composable
fun TwoStepVerificationScreenPreviewLight() {
    JayraTheme(darkTheme = false) {
        TwoStepVerificationContent(
            data = TwoStepData(
                isEnabled = true,
                isBiometricEnabled = true,
                recoveryEmail = "soal8456@gmail.com",
                hasPassword = true
            ),
            onOpenPasswordDialog = {},
            onOpenEmailDialog = {},
            onBiometricToggle = {},
            onToggle2FA = {}
        )
    }
}

@Preview(name = "Two Step Verification Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TwoStepVerificationScreenPreviewDark() {
    JayraTheme(darkTheme = true) {
        TwoStepVerificationContent(
            data = TwoStepData(
                isEnabled = false,
                isBiometricEnabled = false,
                recoveryEmail = "",
                hasPassword = false
            ),
            onOpenPasswordDialog = {},
            onOpenEmailDialog = {},
            onBiometricToggle = {},
            onToggle2FA = {}
        )
    }
}
