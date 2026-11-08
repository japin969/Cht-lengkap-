package com.jayra.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.AuthUiState
import com.jayra.app.util.iosGlass

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
    val context = LocalContext.current

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
    )

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.size(80.dp).iosGlass(cornerRadius = 40, tint = Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Register", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(32.dp))
                
                Text(
                    text = "Bergabung dengan Jayra",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Buat identitas digital Anda hari ini.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosGlass(cornerRadius = 24, tint = Color.White.copy(alpha = 0.05f), elevation = 12)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Daftar Akun", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                    IOSTextField(
                        value = email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "Alamat Email",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    IOSTextField(
                        value = password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = "Kata Sandi (min 6 karakter)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        isPassword = true
                    )

                    // Negara & Nomor (Glass Style)
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .iosGlass(cornerRadius = 14, tint = Color.White.copy(alpha = 0.05f), elevation = 0)
                            .clickable { /* Buka dialog negara nanti */ }
                            .padding(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(country?.name ?: "Pilih Negara", fontSize = 17.sp, color = Color.White)
                            Text("Ubah", color = Color(0xFF5AC8FA), fontSize = 17.sp)
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .iosGlass(cornerRadius = 14, tint = Color.White.copy(alpha = 0.05f), elevation = 0)
                            .padding(16.dp)
                    ) {
                        Text(phone.ifBlank { "+62..." }, fontSize = 17.sp, color = Color(0xFF34C759))
                    }

                    if (uiState is AuthUiState.Error) {
                        IOSErrorMessage((uiState as AuthUiState.Error).message)
                    }

                    Spacer(Modifier.height(8.dp))
                    IOSButton(
                        text = "Daftar",
                        isLoading = uiState is AuthUiState.Loading,
                        onClick = { viewModel.onRegisterClick(context, email, password) }
                    )
                }
            }
        }
    }

    if (uiState is AuthUiState.Success) {
        LaunchedEffect(Unit) { onRegisterSuccess() }
    }
}
