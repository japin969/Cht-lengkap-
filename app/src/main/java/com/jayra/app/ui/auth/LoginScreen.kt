package com.jayra.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Background gradient iOS-style (Deep Blue to Purple)
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
    )

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Logo Glass
                Box(
                    modifier = Modifier.size(80.dp).iosGlass(cornerRadius = 40, tint = Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(32.dp))
                
                // Typography Profesional
                Text(
                    text = "Jayra",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = Color.White
                )
                Text(
                    text = "Komunikasi tanpa batas, aman, dan privat.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
                )
            }

            item {
                // Glass Card Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosGlass(cornerRadius = 24, tint = Color.White.copy(alpha = 0.05f), elevation = 12)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Masuk", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    
                    IOSTextField(
                        value = email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "Alamat Email",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    IOSTextField(
                        value = password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = "Kata Sandi",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        isPassword = true
                    )

                    if (uiState is AuthUiState.Error) {
                        IOSErrorMessage((uiState as AuthUiState.Error).message)
                    }

                    Spacer(Modifier.height(8.dp))
                    IOSButton(
                        text = "Masuk",
                        isLoading = uiState is AuthUiState.Loading,
                        onClick = { viewModel.onLoginClick(context, email, password) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToRegister() },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Belum punya akun? ", color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
                    Text("Daftar", color = Color(0xFF5AC8FA), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (uiState is AuthUiState.Success) {
        LaunchedEffect(Unit) { onLoginSuccess() }
    }
}
