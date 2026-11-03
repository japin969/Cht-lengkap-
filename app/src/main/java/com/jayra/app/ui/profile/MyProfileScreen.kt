package com.jayra.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.model.MyProfileUiState
import com.jayra.app.model.User
import com.jayra.app.security.SecurityRepository
import com.jayra.app.ui.components.InfoActionGrid
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
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.ui.theme.JayraYellow
import com.jayra.app.ui.viewmodel.MyProfileViewModel

@Composable
fun MyProfileScreen(
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    viewModel: MyProfileViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Zero-permission Android Photo Picker for profile photo update
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Foto profil diperbarui!", Toast.LENGTH_SHORT).show()
        }
    }

    MyProfileContent(
        uiState = uiState,
        onBack = onBack,
        onEditClick = onEditProfileClick,
        onAvatarClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onCopyJayraId = { id ->
            SecurityRepository.copyWithAutoClear(
                context = context,
                label = "Jayra ID",
                text = id,
                scope = scope
            )
        },
        onCopyUsername = { username ->
            SecurityRepository.copyWithAutoClear(
                context = context,
                label = "Jayra Username",
                text = "@$username",
                scope = scope
            )
        },
        onAppLockToggle = viewModel::onAppLockToggle,
        onDisappearingToggle = viewModel::onDisappearingToggle,
        onPrivacyClick = onPrivacyClick,
        onRetry = viewModel::onRetry
    )
}

@Composable
fun MyProfileContent(
    uiState: MyProfileUiState,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onCopyJayraId: (String) -> Unit,
    onCopyUsername: (String) -> Unit,
    onAppLockToggle: (Boolean) -> Unit,
    onDisappearingToggle: () -> Unit,
    onPrivacyClick: () -> Unit,
    onRetry: () -> Unit
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("my_profile_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Profil Saya",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                TextButton(onClick = onEditClick) {
                    Text(
                        text = "Edit",
                        fontSize = 17.sp,
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
                is MyProfileUiState.Loading -> {
                    ShimmerCardList(cardCount = 4)
                }
                is MyProfileUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                is MyProfileUiState.Success -> {
                    val user = uiState.user
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Profile Section with clickable avatar for Photo Picker
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(topBarBg)
                                .padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 24.dp)
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(JayraBlue)
                                    .clickable(onClick = onAvatarClick),
                                contentAlignment = Alignment.Center
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
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Small camera badge overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(JayraBlue)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Ganti foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = user.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = user.phoneNumber,
                                fontSize = 17.sp,
                                color = JayraSubtext,
                                textAlign = TextAlign.Center
                            )

                            if (user.bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user.bio,
                                    fontSize = 15.sp,
                                    color = JayraSubtext,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + slideInVertically(tween(200))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                // Action Grid (Panggilan, Video, Cari)
                                InfoActionGrid(
                                    action1Icon = Icons.Default.Call,
                                    action1Label = "panggilan",
                                    onAction1Click = {},
                                    action2Icon = Icons.Default.Videocam,
                                    action2Label = "video",
                                    onAction2Click = {},
                                    action3Icon = Icons.Default.Search,
                                    action3Label = "cari",
                                    onAction3Click = {},
                                    modifier = Modifier.padding(top = 16.dp)
                                )

                                // Card Info (Username & Jayra ID)
                                Column(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onCopyUsername(user.username) }
                                    ) {
                                        Text(
                                            text = "Username",
                                            fontSize = 15.sp,
                                            color = JayraSubtext
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "@${user.username}",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = JayraBlue
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 12.dp)
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(separatorColor)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onCopyJayraId(user.telegramId) }
                                    ) {
                                        Text(
                                            text = "Jayra ID",
                                            fontSize = 15.sp,
                                            color = JayraSubtext
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = user.telegramId,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                    }
                                }

                                // Card Settings 1 (Pesan Sementara, Kunci Aplikasi, Privasi)
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.Default.Timer,
                                        iconTint = JayraBlue,
                                        title = "Pesan Sementara",
                                        valueText = user.disappearingDuration,
                                        onClick = onDisappearingToggle,
                                        showSeparator = true
                                    )
                                    InfoSwitchRow(
                                        icon = Icons.Default.Lock,
                                        iconTint = JayraYellow,
                                        title = "Kunci Aplikasi",
                                        checked = user.isAppLockEnabled,
                                        onCheckedChange = onAppLockToggle,
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Default.Security,
                                        iconTint = JayraSuccessGreen,
                                        title = "Privasi",
                                        onClick = onPrivacyClick,
                                        showSeparator = false
                                    )
                                }

                                // Card Settings 2 (Wallpaper & Bunyi, Penyimpanan, Bahasa)
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp, bottom = 24.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                ) {
                                    InfoSettingRow(
                                        icon = Icons.Default.Palette,
                                        iconTint = JayraOrange,
                                        title = "Wallpaper & Bunyi",
                                        onClick = {},
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Default.Folder,
                                        iconTint = JayraBlue,
                                        title = "Penyimpanan",
                                        valueText = "1.2 GB",
                                        onClick = {},
                                        showSeparator = true
                                    )
                                    InfoSettingRow(
                                        icon = Icons.Default.Language,
                                        iconTint = JayraSuccessGreen,
                                        title = "Bahasa",
                                        valueText = user.language,
                                        onClick = {},
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

@Preview(name = "My Profile Light", showBackground = true)
@Composable
private fun MyProfileLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            MyProfileContent(
                uiState = MyProfileUiState.Success(
                    User(
                        id = "user_1",
                        name = "Alex Pratama",
                        phoneNumber = "+62 812-3456-7890",
                        bio = "Mobile Engineer & Jetpack Compose Enthusiast",
                        username = "alexpratama",
                        telegramId = "782910342",
                        disappearingDuration = "Mati",
                        isAppLockEnabled = true,
                        language = "Indonesia"
                    )
                ),
                onBack = {},
                onEditClick = {},
                onAvatarClick = {},
                onCopyJayraId = {},
                onCopyUsername = {},
                onAppLockToggle = {},
                onDisappearingToggle = {},
                onPrivacyClick = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "My Profile Dark", showBackground = true)
@Composable
private fun MyProfileDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            MyProfileContent(
                uiState = MyProfileUiState.Success(
                    User(
                        id = "user_1",
                        name = "Alex Pratama",
                        phoneNumber = "+62 812-3456-7890",
                        bio = "Mobile Engineer & Jetpack Compose Enthusiast",
                        username = "alexpratama",
                        telegramId = "782910342",
                        disappearingDuration = "Mati",
                        isAppLockEnabled = false,
                        language = "Indonesia"
                    )
                ),
                onBack = {},
                onEditClick = {},
                onAvatarClick = {},
                onCopyJayraId = {},
                onCopyUsername = {},
                onAppLockToggle = {},
                onDisappearingToggle = {},
                onPrivacyClick = {},
                onRetry = {}
            )
        }
    }
}
