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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.PrivacySettings
import com.jayra.app.model.PrivacyUiState
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
import com.jayra.app.ui.viewmodel.PrivacyViewModel

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onTwoStepClick: () -> Unit = {},
    onActiveDevicesClick: () -> Unit = {},
    onDisappearingClick: () -> Unit = {},
    onPasscodeClick: () -> Unit = {},
    viewModel: PrivacyViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PrivacyContent(
        uiState = uiState,
        onBack = onBack,
        onAppLockToggle = viewModel::onAppLockToggle,
        onTwoStepClick = onTwoStepClick,
        onActiveDevicesClick = onActiveDevicesClick,
        onPhoneNumberPrivacyChange = viewModel::onPhoneNumberPrivacyChange,
        onLastSeenPrivacyChange = viewModel::onLastSeenPrivacyChange,
        onProfilePhotoPrivacyChange = viewModel::onProfilePhotoPrivacyChange,
        onDisappearingClick = onDisappearingClick,
        onPasscodeClick = onPasscodeClick
    )
}

@Composable
fun PrivacyContent(
    uiState: PrivacyUiState,
    onBack: () -> Unit,
    onAppLockToggle: (Boolean) -> Unit,
    onTwoStepClick: () -> Unit,
    onActiveDevicesClick: () -> Unit,
    onPhoneNumberPrivacyChange: (String) -> Unit,
    onLastSeenPrivacyChange: (String) -> Unit,
    onProfilePhotoPrivacyChange: (String) -> Unit,
    onDisappearingClick: () -> Unit,
    onPasscodeClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    // Privacy Dialog State
    var activePrivacyDialog by remember { mutableStateOf<String?>(null) } // "phone", "last_seen", "photo"

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
                        .testTag("privacy_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Privasi & Keamanan",
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
                is PrivacyUiState.Loading -> {
                    ShimmerCardList(cardCount = 3)
                }
                is PrivacyUiState.Error -> {
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
                is PrivacyUiState.Success -> {
                    val settings = uiState.privacy
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
                                // Card 1: Keamanan (Kunci Aplikasi, Verifikasi 2 Langkah, Perangkat Aktif)
                                Column {
                                    Text(
                                        text = "Keamanan",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JayraBlue,
                                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cardBg)
                                    ) {
                                        InfoSwitchRow(
                                            icon = Icons.Rounded.Lock,
                                            iconTint = JayraBlue,
                                            title = "Kunci Aplikasi",
                                            checked = settings.isAppLockEnabled,
                                            onCheckedChange = onAppLockToggle,
                                            showSeparator = true
                                        )
                                        InfoSettingRow(
                                            icon = Icons.Rounded.Security,
                                            iconTint = JayraSuccessGreen,
                                            title = "Verifikasi Dua Langkah",
                                            valueText = if (settings.isTwoStepVerificationEnabled) "Aktif" else "Mati",
                                            onClick = onTwoStepClick,
                                            showSeparator = true
                                        )
                                        InfoSettingRow(
                                            icon = Icons.Rounded.Devices,
                                            iconTint = JayraOrange,
                                            title = "Perangkat Aktif",
                                            valueText = "${settings.activeDevicesCount}",
                                            onClick = onActiveDevicesClick,
                                            showSeparator = false
                                        )
                                    }
                                }

                                // Card 2: Privasi (Nomor Telepon, Terakhir Dilihat, Foto Profil, Pesan Sementara, Kunci Sandi)
                                Column {
                                    Text(
                                        text = "Privasi",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = JayraBlue,
                                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cardBg)
                                    ) {
                                        InfoSettingRow(
                                            icon = Icons.Rounded.Phone,
                                            iconTint = JayraBlue,
                                            title = "Nomor Telepon",
                                            valueText = settings.phoneNumberPrivacy,
                                            onClick = { activePrivacyDialog = "phone" },
                                            showSeparator = true
                                        )
                                        InfoSettingRow(
                                            icon = Icons.Rounded.Visibility,
                                            iconTint = JayraSuccessGreen,
                                            title = "Terakhir Dilihat & Online",
                                            valueText = settings.lastSeenPrivacy,
                                            onClick = { activePrivacyDialog = "last_seen" },
                                            showSeparator = true
                                        )
                                        InfoSettingRow(
                                            icon = Icons.Rounded.AccountCircle,
                                            iconTint = JayraOrange,
                                            title = "Foto Profil",
                                            valueText = settings.profilePhotoPrivacy,
                                            onClick = { activePrivacyDialog = "photo" },
                                            showSeparator = true
                                        )
                                        InfoSettingRow(
                                            icon = Icons.Rounded.Timer,
                                            iconTint = JayraBlue,
                                            title = "Pesan Sementara",
                                            valueText = settings.disappearingDuration,
                                            onClick = onDisappearingClick,
                                            showSeparator = true
                                        )
                                        InfoSettingRow(
                                            icon = Icons.Rounded.Key,
                                            iconTint = JayraSubtext,
                                            title = "Kunci Sandi",
                                            onClick = onPasscodeClick,
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
    }

    // Privacy Choice Dialog
    if (activePrivacyDialog != null) {
        val dialogType = activePrivacyDialog!!
        val title = when (dialogType) {
            "phone" -> "Siapa yang dapat melihat nomor saya?"
            "last_seen" -> "Siapa yang dapat melihat terakhir dilihat?"
            else -> "Siapa yang dapat melihat foto profil saya?"
        }
        val currentValue = when (dialogType) {
            "phone" -> (uiState as? PrivacyUiState.Success)?.privacy?.phoneNumberPrivacy ?: "Semua Orang"
            "last_seen" -> (uiState as? PrivacyUiState.Success)?.privacy?.lastSeenPrivacy ?: "Kontak Saya"
            else -> (uiState as? PrivacyUiState.Success)?.privacy?.profilePhotoPrivacy ?: "Semua Orang"
        }
        val options = listOf("Semua Orang", "Kontak Saya", "Tidak Ada")

        AlertDialog(
            onDismissRequest = { activePrivacyDialog = null },
            title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (dialogType) {
                                        "phone" -> onPhoneNumberPrivacyChange(option)
                                        "last_seen" -> onLastSeenPrivacyChange(option)
                                        "photo" -> onProfilePhotoPrivacyChange(option)
                                    }
                                    activePrivacyDialog = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == currentValue),
                                onClick = {
                                    when (dialogType) {
                                        "phone" -> onPhoneNumberPrivacyChange(option)
                                        "last_seen" -> onLastSeenPrivacyChange(option)
                                        "photo" -> onProfilePhotoPrivacyChange(option)
                                    }
                                    activePrivacyDialog = null
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = JayraBlue)
                            )
                            Text(text = option, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activePrivacyDialog = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Preview(name = "Privacy Light", showBackground = true)
@Composable
private fun PrivacyLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            PrivacyContent(
                uiState = PrivacyUiState.Success(
                    PrivacySettings(
                        isAppLockEnabled = true,
                        phoneNumberPrivacy = "Semua Orang",
                        lastSeenPrivacy = "Kontak Saya",
                        profilePhotoPrivacy = "Semua Orang",
                        disappearingDuration = "Mati",
                        isTwoStepVerificationEnabled = true,
                        activeDevicesCount = 3
                    )
                ),
                onBack = {},
                onAppLockToggle = {},
                onTwoStepClick = {},
                onActiveDevicesClick = {},
                onPhoneNumberPrivacyChange = {},
                onLastSeenPrivacyChange = {},
                onProfilePhotoPrivacyChange = {},
                onDisappearingClick = {},
                onPasscodeClick = {}
            )
        }
    }
}

@Preview(name = "Privacy Dark", showBackground = true)
@Composable
private fun PrivacyDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            PrivacyContent(
                uiState = PrivacyUiState.Success(
                    PrivacySettings(
                        isAppLockEnabled = false,
                        phoneNumberPrivacy = "Kontak Saya",
                        lastSeenPrivacy = "Tidak Ada",
                        profilePhotoPrivacy = "Kontak Saya",
                        disappearingDuration = "24 Jam",
                        isTwoStepVerificationEnabled = true,
                        activeDevicesCount = 2
                    )
                ),
                onBack = {},
                onAppLockToggle = {},
                onTwoStepClick = {},
                onActiveDevicesClick = {},
                onPhoneNumberPrivacyChange = {},
                onLastSeenPrivacyChange = {},
                onProfilePhotoPrivacyChange = {},
                onDisappearingClick = {},
                onPasscodeClick = {}
            )
        }
    }
}
