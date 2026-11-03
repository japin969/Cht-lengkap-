package com.jayra.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.model.EditProfileUiState
import com.jayra.app.model.User
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
import com.jayra.app.ui.viewmodel.EditProfileViewModel

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val name by viewModel.nameInput.collectAsStateWithLifecycle()
    val bio by viewModel.bioInput.collectAsStateWithLifecycle()
    val username by viewModel.usernameInput.collectAsStateWithLifecycle()
    val avatarUrl by viewModel.avatarUrlInput.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImagePick(uri.toString())
            Toast.makeText(context, "Foto profil dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    EditProfileContent(
        uiState = uiState,
        name = name,
        bio = bio,
        username = username,
        avatarUrl = avatarUrl,
        onNameChange = viewModel::onNameChange,
        onBioChange = viewModel::onBioChange,
        onUsernameChange = viewModel::onUsernameChange,
        onAvatarClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onSaveClick = {
            viewModel.onSaveClick(
                onSuccess = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onBack()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        },
        onBack = onBack
    )
}

@Composable
fun EditProfileContent(
    uiState: EditProfileUiState,
    name: String,
    bio: String,
    username: String,
    avatarUrl: String,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onSaveClick: () -> Unit,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val fieldBg = if (isDark) JayraCardDark else JayraCardLight
    val borderColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight
    val textColor = if (isDark) Color.White else Color.Black

    val isSaving = (uiState as? EditProfileUiState.Success)?.isSaving == true

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
                        .testTag("edit_profile_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Edit Profil",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                if (isSaving) {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = JayraBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    TextButton(
                        onClick = onSaveClick,
                        modifier = Modifier.testTag("edit_profile_save")
                    ) {
                        Text(
                            text = "Simpan",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = JayraBlue
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Section: Avatar 120dp with Camera overlay & Clickable text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(JayraBlue)
                        .clickable(onClick = onAvatarClick)
                        .testTag("edit_avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = name.take(1).ifEmpty { "U" }.uppercase(),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Semi-transparent camera overlay (32dp CameraAlt on black alpha 0.5)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Ganti Foto",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ubah Foto Profil",
                    fontSize = 17.sp,
                    color = JayraBlue,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onAvatarClick)
                )
            }

            // Form Section: Name, Bio, Username
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        focusedBorderColor = JayraBlue,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    textStyle = TextStyle(fontSize = 17.sp),
                    placeholder = { Text("Nama", fontSize = 17.sp, color = JayraSubtext) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = onBioChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bio_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        focusedBorderColor = JayraBlue,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    textStyle = TextStyle(fontSize = 17.sp),
                    placeholder = { Text("Bio", fontSize = 17.sp, color = JayraSubtext) },
                    maxLines = 3,
                    minLines = 2
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_username_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        focusedBorderColor = JayraBlue,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    textStyle = TextStyle(fontSize = 17.sp),
                    placeholder = { Text("Username", fontSize = 17.sp, color = JayraSubtext) },
                    prefix = { Text("@", fontSize = 17.sp, color = JayraSubtext) },
                    singleLine = true
                )

                Text(
                    text = "Anda dapat memilih username di Jayra. Orang lain akan dapat menemukan Anda dengan username ini dan menghubungi Anda tanpa perlu mengetahui nomor telepon Anda.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = JayraSubtext,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Preview(name = "Edit Profile Light", showBackground = true)
@Composable
private fun EditProfileLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState.Success(
                    User(
                        id = "user_me",
                        name = "Alex Pratama",
                        username = "alexpratama",
                        bio = "Mobile Engineer & Jetpack Compose Enthusiast"
                    )
                ),
                name = "Alex Pratama",
                bio = "Mobile Engineer & Jetpack Compose Enthusiast",
                username = "alexpratama",
                avatarUrl = "",
                onNameChange = {},
                onBioChange = {},
                onUsernameChange = {},
                onAvatarClick = {},
                onSaveClick = {},
                onBack = {}
            )
        }
    }
}

@Preview(name = "Edit Profile Dark", showBackground = true)
@Composable
private fun EditProfileDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState.Success(
                    User(
                        id = "user_me",
                        name = "Alex Pratama",
                        username = "alexpratama",
                        bio = "Mobile Engineer & Jetpack Compose Enthusiast"
                    )
                ),
                name = "Alex Pratama",
                bio = "Mobile Engineer & Jetpack Compose Enthusiast",
                username = "alexpratama",
                avatarUrl = "",
                onNameChange = {},
                onBioChange = {},
                onUsernameChange = {},
                onAvatarClick = {},
                onSaveClick = {},
                onBack = {}
            )
        }
    }
}
