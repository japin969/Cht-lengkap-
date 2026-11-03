package com.jayra.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.ProxyUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun ProxyScreen(
    onBack: () -> Unit,
    viewModel: ProxyViewModel = viewModel()
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    ProxyScreenContent(
        uiState = uiState,
        onBack = onBack,
        onToggle = viewModel::onToggle,
        onSave = viewModel::onSave
    )
}

@Composable
fun ProxyScreenContent(
    uiState: ProxyUiState,
    onBack: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val separatorColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(topBarBg)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("proxy_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Proxy",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ProxyUiState.Loading -> {
                    // Loading
                }
                is ProxyUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = JayraSubtext,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProxyUiState.Success -> {
                    var host by remember(uiState.host) { mutableStateOf(uiState.host) }
                    var port by remember(uiState.port) { mutableStateOf(uiState.port) }
                    var username by remember(uiState.username) { mutableStateOf(uiState.username) }
                    var password by remember(uiState.password) { mutableStateOf(uiState.password) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggle(!uiState.isEnabled) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Proxy",
                                    tint = JayraBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Gunakan Proxy",
                                    fontSize = 17.sp,
                                    color = if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = uiState.isEnabled,
                                    onCheckedChange = onToggle,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = JayraBlue,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFE0E0E0)
                                    )
                                )
                            }

                            AnimatedVisibility(
                                visible = uiState.isEnabled,
                                enter = slideInVertically() + fadeIn()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(separatorColor)
                                    )

                                    OutlinedTextField(
                                        value = host,
                                        onValueChange = { host = it },
                                        label = { Text("Host / Server") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("proxy_host_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = JayraBlue,
                                            focusedLabelColor = JayraBlue
                                        )
                                    )

                                    OutlinedTextField(
                                        value = port,
                                        onValueChange = { port = it },
                                        label = { Text("Port (misal 1080)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("proxy_port_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = JayraBlue,
                                            focusedLabelColor = JayraBlue
                                        )
                                    )

                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        label = { Text("Nama Pengguna (Opsional)") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("proxy_username_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = JayraBlue,
                                            focusedLabelColor = JayraBlue
                                        )
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Kata Sandi (Opsional)") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("proxy_password_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = JayraBlue,
                                            focusedLabelColor = JayraBlue
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = { onSave(host, port, username, password) },
                                        colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("proxy_save_button")
                                    ) {
                                        Text(
                                            text = "Simpan",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Proxy Screen Light", showBackground = true)
@Composable
private fun ProxyScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            ProxyScreenContent(
                uiState = ProxyUiState.Success(
                    isEnabled = true,
                    host = "192.168.1.1",
                    port = "1080"
                ),
                onBack = {},
                onToggle = {},
                onSave = { _, _, _, _ -> }
            )
        }
    }
}

@Preview(name = "Proxy Screen Dark", showBackground = true)
@Composable
private fun ProxyScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            ProxyScreenContent(
                uiState = ProxyUiState.Success(
                    isEnabled = false
                ),
                onBack = {},
                onToggle = {},
                onSave = { _, _, _, _ -> }
            )
        }
    }
}
