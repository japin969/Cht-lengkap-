package com.jayra.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.AuthUiState
import com.jayra.app.ui.theme.*

// ═══════════════════════════════════════════════════════════
// LoginScreen — iOS 26 Liquid Glass Edition
// ═══════════════════════════════════════════════════════════
// v2.3: backdrop dipindah ke IOSComponents.IOSAuthBackdrop
//       (shared dgn RegisterScreen). "Daftar" link pakai
//       IOSTextButton (haptic + press alpha konsisten).
// ═══════════════════════════════════════════════════════════

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val dark = isSystemInDarkTheme()
    var showForgotPassword by rememberSaveable { mutableStateOf(false) }
    var successHandled by rememberSaveable { mutableStateOf(false) }

    val titleColor = if (dark) IOSLabelDark else IOSLabel
    val subtitleColor = if (dark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val separator = if (dark) IOSSeparatorDark else IOSSeparatorOpaque

    Box(modifier = Modifier.fillMaxSize()) {
        IOSAuthBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.iosGlassEntrance(delayMillis = 0),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IOSWordmark()
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Selamat Datang",
                    fontFamily = SFProFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    letterSpacing = (-0.6).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Masuk ke akun Jayra Anda",
                    fontFamily = SFProFontFamily,
                    fontSize = 15.sp,
                    color = subtitleColor,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
            IOSGlassCard(
                modifier = Modifier.iosGlassEntrance(delayMillis = 60)
            ) {
                    IOSFieldGroup {
                    IOSGroupedTextField(
                        label = "Email",
                        value = email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "nama@email.com",
                        isError = uiState is AuthUiState.Error,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )
                    IOSDivider()
                    IOSGroupedTextField(
                        label = "Kata Sandi",
                        value = password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = "Masukkan kata sandi",
                        isPassword = true,
                        isError = uiState is AuthUiState.Error,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.onLoginClick(context, email, password)
                            }
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IOSTextButton(text = "Lupa Sandi?", onClick = { showForgotPassword = true })
                }
                Spacer(Modifier.height(18.dp))
                AnimatedVisibility(
                    visible = uiState is AuthUiState.Error,
                    enter = slideInVertically(
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                        initialOffsetY = { -it / 2 }
                    ) + fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        IOSErrorMessage((uiState as? AuthUiState.Error)?.message.orEmpty())
                        Spacer(Modifier.height(14.dp))
                    }
                }
                IOSPrimaryButton(
                    text = "Masuk",
                    isLoading = uiState is AuthUiState.Loading,
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onLoginClick(context, email, password)
                    }
                )
            }
            Spacer(Modifier.height(26.dp))
            Column(
                modifier = Modifier.fillMaxWidth().iosGlassEntrance(delayMillis = 120),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f).height(0.5.dp).background(separator))
                    Text(
                        text = "atau",
                        fontFamily = SFProFontFamily,
                        color = subtitleColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Box(Modifier.weight(1f).height(0.5.dp).background(separator))
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Belum punya akun? ",
                        fontFamily = SFProFontFamily,
                        color = subtitleColor,
                        fontSize = 15.sp
                    )
                    IOSTextButton(
                        text = "Daftar",
                        onClick = onNavigateToRegister,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            viewModel = viewModel,
            initialEmail = email,
            onDismiss = { showForgotPassword = false }
        )
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success && !successHandled) {
            successHandled = true
            onLoginSuccess()
        }
    }
}
