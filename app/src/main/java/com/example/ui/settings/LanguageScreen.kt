package com.example.ui.settings

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.model.LanguageItem
import com.example.model.LanguageUiState
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    viewModel: LanguageViewModel = viewModel()
) {
    BackHandler { onBack() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LanguageContent(
        uiState = uiState,
        onBack = onBack,
        onLanguageSelect = viewModel::selectLanguage,
        onSearchQueryChange = viewModel::searchLanguage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageContent(
    uiState: LanguageUiState,
    onBack: () -> Unit,
    onLanguageSelect: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit
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
                        text = "Bahasa",
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
                            .testTag("language_back_button")
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
            is LanguageUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TelegramBlue)
                }
            }
            is LanguageUiState.Error -> {
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
            is LanguageUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("language_search_input"),
                        placeholder = { Text("Cari bahasa...", color = TelegramSubtext) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = TelegramSubtext
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = TelegramSubtext
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TelegramBlue,
                            unfocusedBorderColor = if (isDark) Color(0xFF2C384A) else Color(0xFFE0E0E0),
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg
                        )
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = cardBg,
                        tonalElevation = 1.dp
                    ) {
                        LazyColumn {
                            itemsIndexed(uiState.languages, key = { _, item -> item.code }) { index, item ->
                                LanguageRowItem(
                                    item = item,
                                    onClick = { onLanguageSelect(item.code) },
                                    isLast = index == uiState.languages.size - 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun LanguageRowItem(
    item: LanguageItem,
    onClick: () -> Unit,
    isLast: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("language_item_${item.code}"),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.nativeName,
                    fontSize = 13.sp,
                    color = TelegramSubtext
                )
            }
            if (item.isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Dipilih",
                    tint = TelegramBlue,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                RadioButton(
                    selected = false,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = TelegramBlue)
                )
            }
        }
    }
    if (!isLast) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
                .height(0.5.dp)
                .background(TelegramSubtext.copy(alpha = 0.2f))
        )
    }
}

@Preview(name = "LanguageScreen Light Mode", showBackground = true)
@Composable
fun LanguageScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        LanguageContent(
            uiState = LanguageUiState.Success(
                currentLanguageCode = "id",
                languages = listOf(
                    LanguageItem("id", "Bahasa Indonesia", "Bahasa Indonesia", isSelected = true),
                    LanguageItem("en", "English", "English", isSelected = false)
                )
            ),
            onBack = {},
            onLanguageSelect = {},
            onSearchQueryChange = {}
        )
    }
}

@Preview(name = "LanguageScreen Dark Mode", showBackground = true)
@Composable
fun LanguageScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        LanguageContent(
            uiState = LanguageUiState.Success(
                currentLanguageCode = "en",
                languages = listOf(
                    LanguageItem("id", "Bahasa Indonesia", "Bahasa Indonesia", isSelected = false),
                    LanguageItem("en", "English", "English", isSelected = true)
                )
            ),
            onBack = {},
            onLanguageSelect = {},
            onSearchQueryChange = {}
        )
    }
}
