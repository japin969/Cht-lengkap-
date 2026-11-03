package com.example.ui.help

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.PrivacyPolicyUiState
import com.example.model.PrivacySection
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramSuccessGreen
import com.example.ui.theme.TelegramTheme

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    viewModel: PrivacyPolicyViewModel = viewModel()
) {
    BackHandler { onBack() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PrivacyPolicyContent(
        uiState = uiState,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyContent(
    uiState: PrivacyPolicyUiState,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val cardBg = if (isDark) TelegramCardDark else TelegramCardLight

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kebijakan Privasi",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("privacy_policy_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg),
                modifier = Modifier.height(56.dp)
            )
        },
        containerColor = screenBg
    ) { innerPadding ->
        when (uiState) {
            is PrivacyPolicyUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TelegramBlue)
                }
            }
            is PrivacyPolicyUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        color = TelegramSubtext,
                        fontSize = 16.sp
                    )
                }
            }
            is PrivacyPolicyUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Banner Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(TelegramSuccessGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = TelegramSuccessGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = uiState.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Terakhir diperbarui: ${uiState.lastUpdated}",
                                        fontSize = 12.sp,
                                        color = TelegramSubtext
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Intro Text
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = uiState.intro,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(uiState.sections, key = { it.id }) { section ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("privacy_section_${section.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = section.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TelegramBlue
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = section.content,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFFD0D7DE) else Color(0xFF333333),
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Preview(name = "PrivacyPolicyScreen Light Mode", showBackground = true)
@Composable
fun PrivacyPolicyScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        PrivacyPolicyContent(
            uiState = PrivacyPolicyUiState.Success(
                sections = listOf(
                    PrivacySection("1", "1. Prinsip Utama", "Privasi adalah hak mendasar."),
                    PrivacySection("2", "2. Enkripsi E2EE", "Semua data diamankan.")
                )
            ),
            onBack = {}
        )
    }
}

@Preview(name = "PrivacyPolicyScreen Dark Mode", showBackground = true)
@Composable
fun PrivacyPolicyScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        PrivacyPolicyContent(
            uiState = PrivacyPolicyUiState.Success(
                sections = listOf(
                    PrivacySection("1", "1. Prinsip Utama", "Privasi adalah hak mendasar."),
                    PrivacySection("2", "2. Enkripsi E2EE", "Semua data diamankan.")
                )
            ),
            onBack = {}
        )
    }
}
