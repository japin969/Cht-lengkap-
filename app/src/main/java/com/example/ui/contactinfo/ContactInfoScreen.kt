package com.example.ui.contactinfo

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Contact
import com.example.model.ContactInfoUiState
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramOrange
import com.example.ui.theme.TelegramSeparatorDark
import com.example.ui.theme.TelegramSeparatorLight
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramSuccessGreen
import com.example.ui.theme.TelegramTheme
import com.example.ui.theme.TelegramYellow
import com.example.ui.viewmodel.ContactInfoViewModel

@Composable
fun ContactInfoScreen(
    contactId: String,
    onBack: () -> Unit,
    viewModel: ContactInfoViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(contactId) {
        viewModel.loadContact(contactId)
    }

    val uiState by viewModel.uiState.collectAsState()

    ContactInfoContent(
        uiState = uiState,
        onBack = onBack,
        onMuteToggle = viewModel::onMuteToggle,
        onSaveToCameraToggle = viewModel::onSaveToCameraToggle,
        onDisappearingToggle = viewModel::onDisappearingMessagesToggle,
        onRetry = viewModel::onRetry
    )
}

@Composable
fun ContactInfoContent(
    uiState: ContactInfoUiState,
    onBack: () -> Unit,
    onMuteToggle: (Boolean) -> Unit,
    onSaveToCameraToggle: (Boolean) -> Unit,
    onDisappearingToggle: () -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val headerBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val cardBg = if (isDark) TelegramCardDark else TelegramCardLight
    val separatorColor = if (isDark) TelegramSeparatorDark else TelegramSeparatorLight

    Scaffold(
        topBar = {
            // Header with Back button, "Info Kontak" and "Edit"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(headerBg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("contact_info_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Info Kontak",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                TextButton(onClick = { /* Edit contact */ }) {
                    Text(
                        text = "Edit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
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
                is ContactInfoUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TelegramBlue)
                    }
                }
                is ContactInfoUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = uiState.message, color = TelegramSubtext, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onRetry) {
                            Text("Retry", color = TelegramBlue)
                        }
                    }
                }
                is ContactInfoUiState.Success -> {
                    val contact = uiState.contact
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Top Profile Area
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(headerBg)
                                .padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 120dp Avatar Circle
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue)
                            ) {
                                if (contact.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(contact.avatarUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = contact.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = contact.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = contact.phone,
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Action Grid (3 buttons: Panggilan, Video, Cari)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ActionGridItem(
                                    icon = Icons.Default.Call,
                                    label = "panggilan",
                                    modifier = Modifier.weight(1f)
                                )
                                ActionGridItem(
                                    icon = Icons.Default.Videocam,
                                    label = "video",
                                    modifier = Modifier.weight(1f)
                                )
                                ActionGridItem(
                                    icon = Icons.Default.Search,
                                    label = "cari",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Card Media & Bintang
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                            ) {
                                SettingItemRow(
                                    icon = Icons.Default.FilePresent,
                                    iconTint = TelegramBlue,
                                    title = "Media, Tautan, dan Dok",
                                    valueText = "${contact.mediaCount}"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .padding(start = 56.dp)
                                        .background(separatorColor)
                                )
                                SettingItemRow(
                                    icon = Icons.Default.Star,
                                    iconTint = TelegramYellow,
                                    title = "Pesan Berbintang",
                                    valueText = "${contact.starredCount}"
                                )
                            }

                            // Card Settings 1 (Bisukan, Wallpaper & Bunyi, Simpan ke Rol Kamera)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                            ) {
                                SettingSwitchRow(
                                    icon = Icons.Default.VolumeUp,
                                    iconTint = TelegramSuccessGreen,
                                    title = "Bisukan",
                                    checked = contact.isMuted,
                                    onCheckedChange = onMuteToggle
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .padding(start = 56.dp)
                                        .background(separatorColor)
                                )
                                SettingItemRow(
                                    icon = Icons.Default.Palette,
                                    iconTint = TelegramOrange,
                                    title = "Wallpaper & Bunyi",
                                    valueText = "Default"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .padding(start = 56.dp)
                                        .background(separatorColor)
                                )
                                SettingSwitchRow(
                                    icon = Icons.Default.Download,
                                    iconTint = TelegramYellow,
                                    title = "Simpan ke Rol Kamera",
                                    checked = contact.saveToCamera,
                                    onCheckedChange = onSaveToCameraToggle
                                )
                            }

                            // Card Settings 2 (Pesan Sementara)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                            ) {
                                SettingSwitchRow(
                                    icon = Icons.Default.Timer,
                                    iconTint = TelegramBlue,
                                    title = "Pesan Sementara",
                                    checked = contact.disappearingEnabled,
                                    onCheckedChange = { onDisappearingToggle() }
                                )
                            }

                            // Card 5.15 Privacy Dashboard & Security Indicators
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Security",
                                        tint = TelegramSuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Privacy & Encryption Shield",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• AES-256-GCM Hardware Keystore: ACTIVE\n• Screen Capture Blocker (FLAG_SECURE): ACTIVE\n• Clipboard Auto-Purge (30s): ACTIVE\n• Integrity Guard: VERIFIED",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = TelegramSubtext
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionGridItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { /* action */ }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TelegramBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = TelegramBlue
        )
    }
}

@Composable
private fun SettingItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    valueText: String
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Item Click */ }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            fontSize = 15.sp,
            color = TelegramSubtext
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = TelegramSubtext,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TelegramBlue
            )
        )
    }
}

@Preview(name = "Telegram Contact Info Light", showBackground = true)
@Composable
private fun ContactInfoLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            ContactInfoContent(
                uiState = ContactInfoUiState.Success(
                    Contact(
                        name = "Pavel Durov",
                        phone = "+1 (555) 739-0192",
                        mediaCount = 184,
                        starredCount = 19
                    )
                ),
                onBack = {},
                onMuteToggle = {},
                onSaveToCameraToggle = {},
                onDisappearingToggle = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Telegram Contact Info Dark", showBackground = true)
@Composable
private fun ContactInfoDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            ContactInfoContent(
                uiState = ContactInfoUiState.Success(
                    Contact(
                        name = "Pavel Durov",
                        phone = "+1 (555) 739-0192",
                        mediaCount = 184,
                        starredCount = 19
                    )
                ),
                onBack = {},
                onMuteToggle = {},
                onSaveToCameraToggle = {},
                onDisappearingToggle = {},
                onRetry = {}
            )
        }
    }
}
