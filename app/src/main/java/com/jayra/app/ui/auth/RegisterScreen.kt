package com.jayra.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val dark = isSystemInDarkTheme()

    // BUG-006: success-once guard (survive rotasi)
    var showCountryPicker by remember { mutableStateOf(false) }
    var successHandled by rememberSaveable { mutableStateOf(false) }

    val bg = if (dark) IOSSystemBackgroundDark else IOSSystemGroupedBackground
    val titleColor = if (dark) IOSLabelDark else IOSLabel
    val subtitleColor = if (dark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val chevronColor = if (dark) IOSGray2Dark else IOSGray2
    val phoneAccent = IOSGreen

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = IOSBlue
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(colors = listOf(IOSBlue, IOSIndigo))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
            }

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
                        .clickable { showCountryPicker = true }
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

            if (uiState is AuthUiState.Error) {
                IOSErrorMessage((uiState as AuthUiState.Error).message)
                Spacer(Modifier.height(12.dp))
            }

            IOSPrimaryButton(
                text = "Daftar",
                isLoading = uiState is AuthUiState.Loading,
                onClick = { viewModel.onRegisterClick(context, email, password) }
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Sudah punya akun? ", color = subtitleColor, fontSize = 15.sp)
                Text(
                    text = "Masuk",
                    color = IOSBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onBackClick() }
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

    // BUG-006: success-once guard dengan rememberSaveable (survive rotasi)
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success && !successHandled) {
            successHandled = true
            onRegisterSuccess()
        }
    }
}
