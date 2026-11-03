package com.jayra.app.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.DataStorageUiState
import com.jayra.app.model.StorageInfo
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

@Composable
fun DataStorageScreen(
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    viewModel: DataStorageViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DataStorageContent(
        uiState = uiState,
        onBack = onBack,
        onCategoryClick = onCategoryClick,
        onToggle = viewModel::onToggle,
        onRetry = viewModel::loadStorage
    )
}

@Composable
fun DataStorageContent(
    uiState: DataStorageUiState,
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    Scaffold(
        topBar = {
            // 2.1 TopAppBar (Tinggi 56dp)
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
                        .testTag("storage_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Data & Penyimpanan",
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
                is DataStorageUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg.copy(alpha = 0.6f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg.copy(alpha = 0.6f))
                        )
                    }
                }

                is DataStorageUiState.Error -> {
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
                            color = JayraSubtext,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Coba Lagi", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }

                is DataStorageUiState.Success -> {
                    val info = uiState.storageInfo

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 2.2 Card Ringkasan Penyimpanan
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Total Penggunaan",
                                fontSize = 15.sp,
                                color = JayraSubtext,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = info.totalSize,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = JayraBlue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LinearProgressIndicator(
                                progress = { 0.24f }, // approx 1.2 GB out of 5 GB
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = JayraBlue,
                                trackColor = JayraSubtext.copy(alpha = 0.25f)
                            )
                            Text(
                                text = "Dari 5 GB",
                                fontSize = 13.sp,
                                color = JayraSubtext,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // 2.3 Card Rincian Penyimpanan
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                        ) {
                            StorageCategoryItem(
                                icon = Icons.Default.Photo,
                                iconTint = JayraOrange,
                                title = "Foto",
                                sizeText = info.photoSize,
                                onClick = { onCategoryClick("photo") },
                                showSeparator = true
                            )
                            StorageCategoryItem(
                                icon = Icons.Default.Videocam,
                                iconTint = JayraDangerRed,
                                title = "Video",
                                sizeText = info.videoSize,
                                onClick = { onCategoryClick("video") },
                                showSeparator = true
                            )
                            StorageCategoryItem(
                                icon = Icons.Default.Description,
                                iconTint = JayraBlue,
                                title = "Dokumen",
                                sizeText = info.documentSize,
                                onClick = { onCategoryClick("document") },
                                showSeparator = true
                            )
                            StorageCategoryItem(
                                icon = Icons.Default.MusicNote,
                                iconTint = JayraSuccessGreen,
                                title = "Musik",
                                sizeText = info.musicSize,
                                onClick = { onCategoryClick("music") },
                                showSeparator = true
                            )
                            StorageCategoryItem(
                                icon = Icons.Default.Folder,
                                iconTint = JayraSubtext,
                                title = "Lainnya",
                                sizeText = info.otherSize,
                                onClick = { onCategoryClick("other") },
                                showSeparator = false
                            )
                        }

                        // 2.4 Card Pengaturan Unduhan
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                        ) {
                            StorageToggleItem(
                                icon = Icons.Default.Download,
                                title = "Unduh Otomatis (Seluler)",
                                checked = info.autoDownloadCellular,
                                onCheckedChange = { onToggle("autoDownloadCellular", it) },
                                showSeparator = true
                            )
                            StorageToggleItem(
                                icon = Icons.Default.Wifi,
                                title = "Unduh Otomatis (Wi-Fi)",
                                checked = info.autoDownloadWifi,
                                onCheckedChange = { onToggle("autoDownloadWifi", it) },
                                showSeparator = true
                            )
                            StorageToggleItem(
                                icon = Icons.Default.Public,
                                title = "Unduh Otomatis (Roaming)",
                                checked = info.autoDownloadRoaming,
                                onCheckedChange = { onToggle("autoDownloadRoaming", it) },
                                showSeparator = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCategoryItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    sizeText: String,
    onClick: () -> Unit,
    showSeparator: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color.Black
    val separatorColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = sizeText,
                fontSize = 17.sp,
                color = JayraSubtext
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = JayraSubtext,
                modifier = Modifier.size(16.dp)
            )
        }
        if (showSeparator) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp)
                    .height(0.5.dp)
                    .background(separatorColor)
            )
        }
    }
}

@Composable
private fun StorageToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showSeparator: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color.Black
    val separatorColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = JayraBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = JayraBlue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                )
            )
        }
        if (showSeparator) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp)
                    .height(0.5.dp)
                    .background(separatorColor)
            )
        }
    }
}

@Preview(name = "Data Storage Light", showBackground = true)
@Composable
private fun DataStorageScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            DataStorageContent(
                uiState = DataStorageUiState.Success(
                    StorageInfo(
                        totalSize = "1.2 GB",
                        photoSize = "400 MB",
                        videoSize = "500 MB",
                        documentSize = "100 MB",
                        musicSize = "150 MB",
                        otherSize = "50 MB",
                        autoDownloadCellular = true,
                        autoDownloadWifi = true,
                        autoDownloadRoaming = false
                    )
                ),
                onBack = {},
                onCategoryClick = {},
                onToggle = { _, _ -> },
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Data Storage Dark", showBackground = true)
@Composable
private fun DataStorageScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            DataStorageContent(
                uiState = DataStorageUiState.Success(
                    StorageInfo(
                        totalSize = "1.2 GB",
                        photoSize = "400 MB",
                        videoSize = "500 MB",
                        documentSize = "100 MB",
                        musicSize = "150 MB",
                        otherSize = "50 MB",
                        autoDownloadCellular = false,
                        autoDownloadWifi = true,
                        autoDownloadRoaming = false
                    )
                ),
                onBack = {},
                onCategoryClick = {},
                onToggle = { _, _ -> },
                onRetry = {}
            )
        }
    }
}
