package com.jayra.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.R
import com.jayra.app.model.AuthUiState
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
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val subtitle by viewModel.loginHeaderSubtitle.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    LoginContent(
        uiState = uiState,
        subtitle = subtitle,
        email = email,
        password = password,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = { viewModel.onLoginClick(context, email, password) },
        onNavigateToRegister = onNavigateToRegister
    )
}

@Composable
fun LoginContent(
    uiState: AuthUiState,
    subtitle: String,
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val borderColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = screenBg,
        topBar = {
            LoginTopBar(topBarColor = topBarBg)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .testTag("login_form_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(JayraBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            // GANTI SVG TELEGRAM
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(id = R.string.auth_login_title),
                                tint = Color(0xFFFFFFFF),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = JayraSubtext,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 17.sp, color = textColor),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.auth_email_placeholder),
                                fontSize = 17.sp,
                                color = JayraSubtext
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JayraBlue,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            cursorColor = JayraBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(fontSize = 17.sp, color = textColor),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.auth_password_placeholder),
                                fontSize = 17.sp,
                                color = JayraSubtext
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JayraBlue,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            cursorColor = JayraBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )
                }

                if (uiState is AuthUiState.Error) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JayraDangerRed.copy(alpha = 0.14f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("login_error_banner")
                        ) {
                            Text(
                                text = uiState.message,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = JayraDangerRed
                            )
                        }
                    }
                }

                item {
                    val isLoading = uiState is AuthUiState.Loading
                    Button(
                        onClick = onLoginClick,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JayraBlue,
                            contentColor = Color(0xFFFFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFFFFFFFF),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.auth_login_button),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }


                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onNavigateToRegister)
                            .padding(vertical = 12.dp)
                            .testTag("navigate_to_register_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.auth_no_account_prompt),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = JayraBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginTopBar(topBarColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.auth_login_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFFFFF)
            )
        }
    }
}

@Preview(name = "Login Screen Light", showBackground = true)
@Composable
private fun LoginScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            LoginContent(
                uiState = AuthUiState.Idle,
                subtitle = "Masuk ke akun Jayra Anda untuk menyinkronkan pesan terenkripsi di seluruh perangkat.",
                email = "alex@jayra.app",
                password = "password123",
                onEmailChange = {},
                onPasswordChange = {},
                onLoginClick = {},
                onNavigateToRegister = {}
            )
        }
    }
}

@Preview(name = "Login Screen Dark", showBackground = true)
@Composable
private fun LoginScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            LoginContent(
                uiState = AuthUiState.Error("Email atau password salah"),
                subtitle = "Masuk ke akun Jayra Anda untuk menyinkronkan pesan terenkripsi di seluruh perangkat.",
                email = "wrong@jayra.app",
                password = "123",
                onEmailChange = {},
                onPasswordChange = {},
                onLoginClick = {},
                onNavigateToRegister = {}
            )
        }
    }
}
