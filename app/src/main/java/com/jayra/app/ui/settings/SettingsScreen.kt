package com.jayra.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.model.SettingsUiState
import com.jayra.app.model.User
import com.jayra.app.ui.components.InfoSettingRow
import com.jayra.app.ui.components.InfoSwitchRow
import com.jayra.app.ui.components.ShimmerCardList
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraOrange
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onStorageClick: () -> Unit = {},
    onAssistantClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onStickersClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onSavedMessagesClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onStoriesClick: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onBack = onBack,
        onProfileClick = onProfileClick,
        onPrivacyClick = onPrivacyClick,
        onNotificationsClick = onNotificationsClick,
        onStorageClick = onStorageClick,
        onAssistantClick = onAssistantClick,
        onThemeClick = onThemeClick,
        onLanguageClick = onLanguageClick,
        onStickersClick = onStickersClick,
        onHelpClick = onHelpClick,
        onPrivacyPolicyClick = onPrivacyPolicyClick,
        onAboutClick = onAboutClick,
        onSavedMessagesClick = onSavedMessagesClick,
        onCommunitiesClick = onCommunitiesClick,
        onStoriesClick = onStoriesClick,
        onThemeChange = viewModel::onThemeChange,
        onLanguageChange = viewModel::onLanguageChange
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onStorageClick: () -> Unit,
    onAssistantClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onStickersClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onSavedMessagesClick: () -> Unit = {},
    onCommunitiesClick: () -> Unit = {},
    onStoriesClick: () -> Unit = {},
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("jayra_security_prefs", Context.MODE_PRIVATE) }
    val glassEnabled = com.jayra.app.util.GlassEffectPrefs.enabledState.value
    var isAssistantEnabled by remember {
        mutableStateOf(prefs.getBoolean("assistant_ai_enabled", true))
    }

    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

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
                        .testTag("settings_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Pengaturan",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
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
                is SettingsUiState.Loading -> {
                    ShimmerCardList(cardCount = 4)
                }
                is SettingsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Error",
                            tint = JayraDangerRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.message,
                            fontSize = 17.sp,
                            color = JayraSubtext
                        )
                    }
                }
                is SettingsUiState.Success -> {
                    val user = uiState.user
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp)
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + slideInVertically(tween(200))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Profile Card (56dp avatar, name, username, chevron)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                        .clickable(onClick = onProfileClick)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(JayraBlue)
                                    ) {
                                        if (user.avatarUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(user.avatarUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = user.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = user.name.take(1).uppercase(),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.name,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "@${user.username}",
                                            fontSize = 15.sp,
                                            color = JayraSubtext
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Buka Profil",
                                        tint = JayraSubtext,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Card Settings 1 (Akun: Privasi & Keamanan, Notifikasi, Data & Penyimpanan)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.Rounded.AutoAwesomeMotion,
                                        iconTint = JayraBlue,
                                        title = "Stories",
                                        onClick = onStoriesClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Bookmark,
                                        iconTint = JayraBlue,
                                        title = "Pesan Tersimpan",
                                        onClick = onSavedMessagesClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Groups,
                                        iconTint = JayraBlue,
                                        title = "Komunitas",
                                        onClick = onCommunitiesClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Lock,
                                        iconTint = JayraBlue,
                                        title = "Privasi & Keamanan",
                                        onClick = onPrivacyClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Notifications,
                                        iconTint = JayraOrange,
                                        title = "Notifikasi & Suara",
                                        onClick = onNotificationsClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Storage,
                                        iconTint = JayraSuccessGreen,
                                        title = "Data & Penyimpanan",
                                        onClick = onStorageClick,
                                        showSeparator = false
                                    )
                                }

                                // Card Asisten AI (Fase 8)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    // iOS 26 Glass Effect toggle
                                    GlassEffectToggleRow(
                                        enabled = glassEnabled,
                                        onToggle = { enabled ->
                                            com.jayra.app.util.GlassEffectPrefs.set(context, enabled)
                                        }
                                    )
                                    
                                    InfoSwitchRow(
                                        icon = Icons.Rounded.SmartToy,
                                        iconTint = JayraBlue,
                                        title = "Aktifkan Asisten AI",
                                        checked = isAssistantEnabled,
                                        onCheckedChange = { isChecked ->
                                            isAssistantEnabled = isChecked
                                            prefs.edit().putBoolean("assistant_ai_enabled", isChecked).apply()
                                        },
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.SmartToy,
                                        iconTint = JayraBlue,
                                        title = "Asisten AI (Jayra AI)",
                                        valueText = if (isAssistantEnabled) "Aktif" else "Nonaktif",
                                        onClick = {
                                            if (isAssistantEnabled) {
                                                onAssistantClick()
                                            } else {
                                                Toast.makeText(context, "Asisten AI dinonaktifkan", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        showSeparator = false
                                    )
                                }

                                // Card Settings 2 (Umum: Stiker, Tema, Bahasa)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Mood,
                                        iconTint = JayraOrange,
                                        title = "Stiker dan GIF",
                                        onClick = onStickersClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Palette,
                                        iconTint = JayraBlue,
                                        title = "Tema & Tampilan",
                                        valueText = uiState.appTheme,
                                        onClick = onThemeClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Language,
                                        iconTint = JayraBlue,
                                        title = "Bahasa",
                                        valueText = uiState.language,
                                        onClick = onLanguageClick,
                                        showSeparator = false
                                    )
                                }

                                // Card Settings 3 (Lainnya: Bantuan, Kebijakan Privasi, Tentang Jayra)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Help,
                                        iconTint = JayraBlue,
                                        title = "Bantuan & FAQ",
                                        onClick = onHelpClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Policy,
                                        iconTint = JayraSubtext,
                                        title = "Kebijakan Privasi",
                                        onClick = onPrivacyPolicyClick,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Rounded.Info,
                                        iconTint = JayraBlue,
                                        title = "Tentang Jayra",
                                        onClick = onAboutClick,
                                        showSeparator = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        val languages = listOf("Indonesia", "English", "Español", "Русский", "العربية")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Pilih Bahasa") },
            text = {
                Column {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageChange(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = lang, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        val themes = listOf("System", "Light", "Dark")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Pilih Tema") },
            text = {
                Column {
                    themes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = theme, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Preview(name = "Settings Light", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            SettingsContent(
                uiState = SettingsUiState.Success(
                    user = User(
                        id = "user_me",
                        name = "Alex Pratama",
                        username = "alexpratama"
                    ),
                    appTheme = "System",
                    language = "Indonesia"
                ),
                onBack = {},
                onProfileClick = {},
                onPrivacyClick = {},
                onNotificationsClick = {},
                onStorageClick = {},
                onAssistantClick = {},
                onThemeChange = {},
                onLanguageChange = {}
            )
        }
    }
}

@Preview(name = "Settings Dark", showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            SettingsContent(
                uiState = SettingsUiState.Success(
                    user = User(
                        id = "user_me",
                        name = "Alex Pratama",
                        username = "alexpratama"
                    ),
                    appTheme = "Dark",
                    language = "Indonesia"
                ),
                onBack = {},
                onProfileClick = {},
                onPrivacyClick = {},
                onNotificationsClick = {},
                onStorageClick = {},
                onAssistantClick = {},
                onThemeChange = {},
                onLanguageChange = {}
            )
        }
    }
}

@Composable
private fun GlassEffectToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val realBlurSupported = com.jayra.app.util.GlassEffectPrefs.supportsRealBlur()
    val subtitle = when {
        !enabled -> "Matikan efek — solid (0 lag)"
        enabled && realBlurSupported -> "Aktifkan efek — blur asli iOS 26"
        else -> "Aktifkan efek — blur ringan (anti lag)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.airbnb.lottie.compose.LottieAnimation(
            composition = com.airbnb.lottie.compose.rememberLottieComposition(
                com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(
                    com.jayra.app.R.raw.toggle
                )
            ).value,
            progress = { if (enabled) 1f else 0f },
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (enabled) "Aktifkan efek" else "Matikan efek",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF000000)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        }
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.Switch(
            checked = enabled,
            onCheckedChange = { onToggle(it) },
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759)
            )
        )
    }
}

