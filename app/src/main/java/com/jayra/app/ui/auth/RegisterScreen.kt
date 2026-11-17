package com.jayra.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
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
// RegisterScreen — iOS 26 Liquid Glass Edition
// ═══════════════════════════════════════════════════════════
// v2.3: iOS 26 backdrop (shared), IOSWordmark, iOS back button
//       (chevron + "Kembali"), AnimatedVisibility error,
//       IOSTextButton utk "Masuk" link (haptic + alpha).
//       Signature publik tidak berubah.
// ═══════════════════════════════════════════════════════════

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    val dark = isSystemInDarkTheme()

    var successHandled by rememberSaveable { mutableStateOf(false) }

    val titleColor = if (dark) IOSLabelDark else IOSLabel
    val subtitleColor = if (dark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentColor = if (dark) IOSBlueDark else IOSBlue

    Box(modifier = Modifier.fillMaxSize()) {
        IOSAuthBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onBackClick()
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Kembali",
                    fontFamily = SFProFontFamily,
                    fontSize = 17.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(Modifier.height(6.dp))

            Column(
                modifier = Modifier.iosGlassEntrance(delayMillis = 0),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IOSWordmark()
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Buat Akun",
                    fontFamily = SFProFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    letterSpacing = (-0.6).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Atur username dan akun Jayra Anda",
                    fontFamily = SFProFontFamily,
                    fontSize = 15.sp,
                    color = subtitleColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            IOSGlassCard(
                modifier = Modifier.iosGlassEntrance(delayMillis = 60)
            ) {
                IOSFieldGroup {
                    IOSGroupedTextField(
                        label = "Username",
                        value = username,
                        onValueChange = viewModel::onUsernameChange,
                        placeholder = "jayra_user",
                        prefix = "@",
                        isError = uiState is AuthUiState.Error,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )
                    IOSDivider()
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
                        placeholder = "Minimal 6 karakter",
                        isPassword = true,
                        isError = uiState is AuthUiState.Error,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.onRegisterClick(context, email, password, username)
                            }
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Username wajib diisi (min. 3 karakter). Nomor Jayra dibuat otomatis.",
                    fontFamily = SFProFontFamily,
                    fontSize = 12.sp,
                    color = subtitleColor,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

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
                    text = "Daftar",
                    isLoading = uiState is AuthUiState.Loading,
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onRegisterClick(context, email, password, username)
                    }
                )
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosGlassEntrance(delayMillis = 120),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sudah punya akun? ",
                    fontFamily = SFProFontFamily,
                    color = subtitleColor,
                    fontSize = 15.sp
                )
                IOSTextButton(
                    text = "Masuk",
                    onClick = onBackClick,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success && !successHandled) {
            successHandled = true
            onRegisterSuccess()
        }
    }
}
