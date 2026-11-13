package com.jayra.app.ui.auth

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayra.app.model.AuthUiState
import com.jayra.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * BUG-002 + BUG-013: ForgotPasswordDialog — iOS-style modal reset password.
 *
 * FIX UTAMA (BUG-002):
 *  - SEBELUM: collect { ignored -> onDismiss() }  → message dibuang, user tidak tahu sukses
 *  - SESUDAH: collect { message -> successMessage = message } → message tampil
 *
 * KOMPATIBILITAS:
 *  - Di-compile dengan Channel(BUFFERED) di main (sekarang) → jalan.
 *  - Di-rebase dengan MutableSharedFlow(replay=0) setelah Opsi A merge → zero
 *    code change, stale buffer otomatis hilang.
 *
 * FLOW:
 *  1. Form: input email → klik Kirim Tautan
 *  2. Loading: tombol disable, spinner
 *  3. Success: successMessage diisi dari event → UI conditional render
 *     ("Email Terkirim" + message) → auto-dismiss 1.5s
 *  4. Error: AuthUiState.Error → inline IOSErrorMessage, form tetap
 */
@Composable
fun ForgotPasswordDialog(
    viewModel: AuthViewModel,
    initialEmail: String = "",
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dark = isSystemInDarkTheme()
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val bg = if (dark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemBackground
    val titleColor = if (dark) IOSLabelDark else IOSLabel
    val subtitleColor = if (dark) IOSSecondaryLabelDark else IOSSecondaryLabel

    // BUG-002 fix: TAMPILKAN message (bukan buang _)
    LaunchedEffect(Unit) {
        viewModel.forgotPasswordEvent.collect { message ->
            successMessage = message
        }
    }

    // Auto-dismiss setelah user baca konfirmasi sukses
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(2000L)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (successMessage != null) {
                    // === STATE SUKSES — konfirmasi ke user ===
                    Text(
                        text = "Email Terkirim",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = successMessage ?: "",
                        fontSize = 14.sp,
                        color = subtitleColor,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // === STATE FORM — input email ===
                    Text(
                        text = "Lupa Sandi?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Masukkan email Anda. Kami akan mengirim tautan untuk mereset kata sandi.",
                        fontSize = 14.sp,
                        color = subtitleColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    IOSFieldGroup {
                        IOSGroupedTextField(
                            label = "Email",
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "nama@email.com",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                    if (uiState is AuthUiState.Error) {
                        Spacer(Modifier.height(12.dp))
                        IOSErrorMessage((uiState as AuthUiState.Error).message)
                    }

                    Spacer(Modifier.height(20.dp))

                    IOSPrimaryButton(
                        text = "Kirim Tautan",
                        isLoading = uiState is AuthUiState.Loading,
                        enabled = email.isNotBlank(),
                        onClick = { viewModel.onForgotPasswordClick(email) }
                    )

                    Spacer(Modifier.height(8.dp))

                    IOSTextButton(
                        text = "Batal",
                        onClick = onDismiss,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
