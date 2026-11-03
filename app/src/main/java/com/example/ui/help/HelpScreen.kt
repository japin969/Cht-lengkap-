package com.example.ui.help

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FAQ
import com.example.model.HelpUiState
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
fun HelpScreen(
    onBack: () -> Unit,
    onPrivacyClick: () -> Unit = {},
    viewModel: HelpViewModel = viewModel()
) {
    BackHandler { onBack() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HelpContent(
        uiState = uiState,
        onBack = onBack,
        onSearchQueryChange = viewModel::searchFaqs,
        onPrivacyClick = onPrivacyClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpContent(
    uiState: HelpUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPrivacyClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val cardBg = if (isDark) TelegramCardDark else TelegramCardLight

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bantuan & FAQ",
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
                            .testTag("help_back_button")
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
            is HelpUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TelegramBlue)
                }
            }
            is HelpUiState.Error -> {
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
            is HelpUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada FAQ tersedia.",
                        color = TelegramSubtext,
                        fontSize = 16.sp
                    )
                }
            }
            is HelpUiState.Success -> {
                var expandedFaqId by remember { mutableStateOf<String?>(null) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Search Bar
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("help_search_input"),
                            placeholder = { Text("Cari pertanyaan...", color = TelegramSubtext) },
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
                    }

                    item {
                        // Quick Action Contacts
                        Text(
                            text = "Hubungi Kami",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TelegramBlue,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HelpActionChip(
                                title = "Dukungan Email",
                                icon = Icons.Default.Email,
                                modifier = Modifier.weight(1f),
                                cardBg = cardBg,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${uiState.supportEmail}")
                                        putExtra(Intent.EXTRA_SUBJECT, "Bantuan Jayra Android")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )

                            HelpActionChip(
                                title = "FAQ Telegram",
                                icon = Icons.Default.OpenInBrowser,
                                modifier = Modifier.weight(1f),
                                cardBg = cardBg,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.telegramFaqUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        HelpActionChip(
                            title = "Kebijakan Privasi Jayra",
                            icon = Icons.Default.Policy,
                            modifier = Modifier.fillMaxWidth(),
                            cardBg = cardBg,
                            onClick = onPrivacyClick
                        )
                    }

                    item {
                        Text(
                            text = "Pertanyaan Umum (${uiState.filteredFaqs.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TelegramBlue,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(uiState.filteredFaqs, key = { it.id }) { faq ->
                        FaqItemCard(
                            faq = faq,
                            isExpanded = expandedFaqId == faq.id,
                            cardBg = cardBg,
                            onToggle = {
                                expandedFaqId = if (expandedFaqId == faq.id) null else faq.id
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HelpActionChip(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    cardBg: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("help_action_${title.lowercase().replace(" ", "_")}"),
        color = cardBg,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TelegramBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TelegramBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FaqItemCard(
    faq: FAQ,
    isExpanded: Boolean,
    cardBg: Color,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .animateContentSize()
            .testTag("faq_card_${faq.id}"),
        color = cardBg,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = TelegramBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = faq.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TelegramBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = faq.question,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                }
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("faq_expand_button_${faq.id}")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Tutup" else "Buka",
                        tint = TelegramSubtext
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TelegramSubtext.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.answer,
                        fontSize = 14.sp,
                        color = TelegramSubtext,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Preview(name = "HelpScreen Light Mode", showBackground = true)
@Composable
fun HelpScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        HelpContent(
            uiState = HelpUiState.Success(
                faqs = listOf(
                    FAQ("1", "Apa itu Jayra?", "Jayra adalah aplikasi pesan aman berkecepatan tinggi.", "Umum"),
                    FAQ("2", "Bagaimana enkripsi bekerja?", "Enkripsi militer AES-256-GCM menjaga keamanan pesan.", "Keamanan")
                ),
                filteredFaqs = listOf(
                    FAQ("1", "Apa itu Jayra?", "Jayra adalah aplikasi pesan aman berkecepatan tinggi.", "Umum"),
                    FAQ("2", "Bagaimana enkripsi bekerja?", "Enkripsi militer AES-256-GCM menjaga keamanan pesan.", "Keamanan")
                )
            ),
            onBack = {},
            onSearchQueryChange = {},
            onPrivacyClick = {}
        )
    }
}

@Preview(name = "HelpScreen Dark Mode", showBackground = true)
@Composable
fun HelpScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        HelpContent(
            uiState = HelpUiState.Success(
                faqs = listOf(
                    FAQ("1", "Apa itu Jayra?", "Jayra adalah aplikasi pesan aman berkecepatan tinggi.", "Umum"),
                    FAQ("2", "Bagaimana enkripsi bekerja?", "Enkripsi militer AES-256-GCM menjaga keamanan pesan.", "Keamanan")
                ),
                filteredFaqs = listOf(
                    FAQ("1", "Apa itu Jayra?", "Jayra adalah aplikasi pesan aman berkecepatan tinggi.", "Umum"),
                    FAQ("2", "Bagaimana enkripsi bekerja?", "Enkripsi militer AES-256-GCM menjaga keamanan pesan.", "Keamanan")
                )
            ),
            onBack = {},
            onSearchQueryChange = {},
            onPrivacyClick = {}
        )
    }
}
