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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.AuthUiState
import com.jayra.app.model.Country
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
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val country by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val phone by viewModel.generatedPhoneNumber.collectAsStateWithLifecycle()
    val availableCountries: List<Country> by viewModel.availableCountries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val dark = isSystemInDarkTheme()

    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    var successHandled by rememberSaveable { mutableStateOf(false) }

    val titleColor = if (dark) IOSLabelDark else IOSLabel
    val subtitleColor = if (dark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val chevronColor = if (dark) IOSGray2Dark else IOSGray2
    val phoneAccent = IOSGreen

    Box(modifier = Modifier.fillMaxSize()) {
        IOSAuthBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // iOS-style back button: chevron + "Kembali" + haptic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LightImpact)
                        onBackClick()
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = IOSBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Kembali",
                    fontSize = 17.sp,
                    color = IOSBlue,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(Modifier.height(8.dp))

            IOSWordmark()

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Buat Akun",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Bergabung dengan Jayra hari ini",
                fontSize = 15.sp,
                color = subtitleColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            IOSFieldGroup {
                IOSGroupedTextField(
                    label = "Email",
                    value = email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = "nama@email.com",
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            IOSFieldGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LightImpact)
                            showCountryPicker = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEGARA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = subtitleColor,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = country?.name ?: "Pilih Negara",
                            fontSize = 17.sp,
                            color = titleColor
                        )
                    }
                    Text(
                        text = "Ubah",
                        fontSize = 15.sp,
                        color = IOSBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                IOSDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "NOMOR TELEPON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtitleColor,
                        letterSpacing = 0.4.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = phone.ifBlank { "Akan dibuat otomatis" },
                        fontSize = 17.sp,
                        color = if (phone.isBlank()) chevronColor else phoneAccent,
                        fontWeight = if (phone.isBlank()) FontWeight.Normal else FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

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
                    Spacer(Modifier.height(12.dp))
                }
            }

            IOSPrimaryButton(
                text = "Daftar",
                isLoading = uiState is AuthUiState.Loading,
                onClick = { viewModel.onRegisterClick(context, email, password) }
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sudah punya akun? ",
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

        if (showCountryPicker) {
            CountryPickerDialog(
                countries = availableCountries,
                selectedCountry = country,
                onCountrySelected = { picked ->
                    viewModel.onCountrySelected(picked)
                    showCountryPicker = false
                },
                onDismiss = { showCountryPicker = false }
            )
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success && !successHandled) {
            successHandled = true
            onRegisterSuccess()
        }
    }
}
