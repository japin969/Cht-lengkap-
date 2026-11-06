package com.jayra.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.jayra.app.model.Country
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
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val countries by viewModel.availableCountries.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val generatedPhoneNumber by viewModel.generatedPhoneNumber.collectAsStateWithLifecycle()
    val subtitle by viewModel.registerHeaderSubtitle.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onRegisterSuccess()
        }
    }

    RegisterContent(
        uiState = uiState,
        subtitle = subtitle,
        email = email,
        password = password,
        countries = countries,
        selectedCountry = selectedCountry,
        generatedPhoneNumber = generatedPhoneNumber,
        onBackClick = onBackClick,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onCountrySelected = viewModel::onCountrySelected,
        onRegisterClick = { viewModel.onRegisterClick(context, email, password) },
    )
}

@Composable
fun RegisterContent(
    uiState: AuthUiState,
    subtitle: String,
    email: String,
    password: String,
    countries: List<Country>,
    selectedCountry: Country?,
    generatedPhoneNumber: String,
    onBackClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onCountrySelected: (Country) -> Unit,
    onRegisterClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val borderColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    var showCountryDialog by remember { mutableStateOf(false) }
    val registerInteractionSource = remember { MutableInteractionSource() }
    val isPressed by registerInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "register_button_scale"
    )

    if (showCountryDialog) {
        CountryPickerDialog(
            countries = countries,
            selectedCountry = selectedCountry,
            onCountrySelected = onCountrySelected,
            onDismiss = { showCountryDialog = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = screenBg,
        topBar = {
            RegisterTopBar(
                topBarColor = topBarBg,
                onBackClick = onBackClick
            )
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
                    .testTag("register_form_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(JayraBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // GANTI SVG TELEGRAM
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = stringResource(id = R.string.auth_register_title),
                                tint = JayraBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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
                            .testTag("register_email_input")
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
                            .testTag("register_password_input")
                    )
                }

                item {
                    CountrySelectorCard(
                        selectedCountry = selectedCountry,
                        cardBg = cardBg,
                        borderColor = borderColor,
                        textColor = textColor,
                        onCountryClick = { showCountryDialog = true }
                    )
                }

                item {
                    OutlinedTextField(
                        value = generatedPhoneNumber,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (generatedPhoneNumber.isNotEmpty()) JayraSuccessGreen else textColor
                        ),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.auth_auto_phone_placeholder),
                                fontSize = 17.sp,
                                color = JayraSubtext
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JayraBlue,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_generated_phone_input")
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
                                .testTag("register_error_banner")
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
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(animationSpec = tween(durationMillis = 150)) + fadeIn(tween(150))
                    ) {
                        Button(
                            onClick = onRegisterClick,
                            enabled = !isLoading,
                            interactionSource = registerInteractionSource,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JayraBlue,
                                contentColor = Color(0xFFFFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .graphicsLayer {
                                    scaleX = buttonScale
                                    scaleY = buttonScale
                                }
                                .testTag("register_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color(0xFFFFFFFF),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.auth_register_button),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun RegisterTopBar(
    topBarColor: Color,
    onBackClick: () -> Unit
) {
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
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("register_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.auth_back_cd),
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(id = R.string.auth_register_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFFFFF)
            )
        }
    }
}

@Composable
private fun CountrySelectorCard(
    selectedCountry: Country?,
    cardBg: Color,
    borderColor: Color,
    textColor: Color,
    onCountryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onCountryClick)
            .padding(horizontal = 16.dp)
            .testTag("register_country_selector"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // GANTI SVG TELEGRAM
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = stringResource(id = R.string.auth_country_flag_cd),
                tint = JayraBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = selectedCountry?.let { "${it.name} (${it.dialCode})" }
                    ?: stringResource(id = R.string.auth_select_country_label),
                fontSize = 17.sp,
                fontWeight = if (selectedCountry != null) FontWeight.Medium else FontWeight.Normal,
                color = if (selectedCountry != null) textColor else JayraSubtext
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(id = R.string.auth_country_chevron_cd),
            tint = JayraSubtext,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(name = "Register Screen Light", showBackground = true)
@Composable
private fun RegisterScreenLightPreview() {
    val sampleCountries = listOf(
        Country(name = "Indonesia", code = "ID", dialCode = "+62"),
        Country(name = "Amerika Serikat", code = "US", dialCode = "+1")
    )
    JayraTheme(darkTheme = false) {
        Surface {
            RegisterContent(
                uiState = AuthUiState.Idle,
                subtitle = "Daftarkan email dan pilih negara untuk mendapatkan nomor Jayra otomatis.",
                email = "alex@jayra.app",
                password = "password123",
                countries = sampleCountries,
                selectedCountry = sampleCountries.first(),
                generatedPhoneNumber = "+628152961584",
                onBackClick = {},
                onEmailChange = {},
                onPasswordChange = {},
                onCountrySelected = {},
                onRegisterClick = {},
            )
        }
    }
}

@Preview(name = "Register Screen Dark", showBackground = true)
@Composable
private fun RegisterScreenDarkPreview() {
    val sampleCountries = listOf(
        Country(name = "Indonesia", code = "ID", dialCode = "+62"),
        Country(name = "Jepang", code = "JP", dialCode = "+81")
    )
    JayraTheme(darkTheme = true) {
        Surface {
            RegisterContent(
                uiState = AuthUiState.Idle,
                subtitle = "Daftarkan email dan pilih negara untuk mendapatkan nomor Jayra otomatis.",
                email = "",
                password = "",
                countries = sampleCountries,
                selectedCountry = null,
                generatedPhoneNumber = "",
                onBackClick = {},
                onEmailChange = {},
                onPasswordChange = {},
                onCountrySelected = {},
                onRegisterClick = {},
            )
        }
    }
}
