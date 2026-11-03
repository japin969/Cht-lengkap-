package com.jayra.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.AdvancedSettings
import com.jayra.app.model.AdvancedSettingsUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    onProxyClick: () -> Unit,
    viewModel: AdvancedSettingsViewModel = viewModel()
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    AdvancedSettingsContent(
        uiState = uiState,
        onBack = onBack,
        onProxyClick = onProxyClick,
        onToggle = viewModel::onToggle,
        onCallQualityChange = viewModel::onCallQualityChange,
        onUploadQualityChange = viewModel::onUploadQualityChange,
        onCacheLimitChange = viewModel::onCacheLimitChange,
        onClearCache = viewModel::onClearCache,
        onRetry = viewModel::loadSettings
    )
}

@Composable
fun AdvancedSettingsContent(
    uiState: AdvancedSettingsUiState,
    onBack: () -> Unit,
    onProxyClick: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onCallQualityChange: (String) -> Unit,
    onUploadQualityChange: (String) -> Unit,
    onCacheLimitChange: (String) -> Unit,
    onClearCache: () -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    var showCallQualityDialog by remember { mutableStateOf(false) }
    var showUploadQualityDialog by remember { mutableStateOf(false) }
    var showCacheLimitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // 1.1 TopAppBar (Tinggi 56dp)
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
                        .testTag("advanced_settings_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Pengaturan Lanjutan",
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
                is AdvancedSettingsUiState.Loading -> {
                    // 1.5 Loading Shimmer
                    AdvancedSettingsShimmer()
                }

                is AdvancedSettingsUiState.Error -> {
                    // 1.5 Error State
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

                is AdvancedSettingsUiState.Success -> {
                    val settings = uiState.settings

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1.2 Card Koneksi (Jaringan)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                        ) {
                            SettingToggleItem(
                                icon = Icons.Default.Public,
                                title = "Gunakan Data Saat Roaming",
                                checked = settings.dataRoaming,
                                onCheckedChange = { onToggle("roaming", it) },
                                showSeparator = true
                            )
                            SettingValueItem(
                                icon = Icons.Default.Security,
                                title = "Proxy",
                                value = if (settings.proxyEnabled) "Aktif" else "Nonaktif",
                                onClick = onProxyClick,
                                showSeparator = true
                            )
                            SettingValueItem(
                                icon = Icons.Default.Call,
                                title = "Kualitas Panggilan",
                                value = settings.callQuality,
                                onClick = { showCallQualityDialog = true },
                                showSeparator = true
                            )
                            SettingValueItem(
                                icon = Icons.Default.Upload,
                                title = "Kualitas Unggah Media",
                                value = settings.uploadQuality,
                                onClick = { showUploadQualityDialog = true },
                                showSeparator = false
                            )
                        }

                        // 1.3 Card Penyimpanan (Storage)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                        ) {
                            SettingToggleItem(
                                icon = Icons.Default.SdCard,
                                title = "Gunakan Penyimpanan Eksternal",
                                checked = settings.externalStorage,
                                onCheckedChange = { onToggle("externalStorage", it) },
                                showSeparator = true
                            )
                            SettingValueItem(
                                icon = Icons.Default.CleaningServices,
                                iconTint = JayraDangerRed,
                                title = "Bersihkan Cache",
                                value = settings.cacheSize,
                                onClick = onClearCache,
                                showSeparator = true
                            )
                            SettingValueItem(
                                icon = Icons.Default.Storage,
                                title = "Batas Cache",
                                value = settings.cacheLimit,
                                onClick = { showCacheLimitDialog = true },
                                showSeparator = true
                            )
                            SettingToggleItem(
                                icon = Icons.Default.PhotoLibrary,
                                title = "Simpan ke Galeri",
                                checked = settings.saveToGallery,
                                onCheckedChange = { onToggle("saveToGallery", it) },
                                showSeparator = false
                            )
                        }

                        // 1.4 Card Lainnya
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                        ) {
                            SettingToggleItem(
                                icon = Icons.Default.Animation,
                                title = "Gunakan Animasi",
                                checked = settings.animationsEnabled,
                                onCheckedChange = { onToggle("animations", it) },
                                showSeparator = true
                            )
                            SettingToggleItem(
                                icon = Icons.Default.Vibration,
                                title = "Getaran Saat Mengetik",
                                checked = settings.typingVibration,
                                onCheckedChange = { onToggle("typingVibration", it) },
                                showSeparator = true
                            )
                            SettingToggleItem(
                                icon = Icons.Default.Keyboard,
                                title = "Kirim dengan Tombol Enter",
                                checked = settings.enterToSend,
                                onCheckedChange = { onToggle("enterToSend", it) },
                                showSeparator = false
                            )
                        }
                    }

                    // Dialogs
                    if (showCallQualityDialog) {
                        val options = listOf("Rendah", "Sedang", "Tinggi", "Otomatis")
                        OptionSelectionDialog(
                            title = "Pilih Kualitas Panggilan",
                            options = options,
                            selected = settings.callQuality,
                            onSelect = {
                                onCallQualityChange(it)
                                showCallQualityDialog = false
                            },
                            onDismiss = { showCallQualityDialog = false }
                        )
                    }

                    if (showUploadQualityDialog) {
                        val options = listOf("SD", "HD", "Full HD")
                        OptionSelectionDialog(
                            title = "Kualitas Unggah Media",
                            options = options,
                            selected = settings.uploadQuality,
                            onSelect = {
                                onUploadQualityChange(it)
                                showUploadQualityDialog = false
                            },
                            onDismiss = { showUploadQualityDialog = false }
                        )
                    }

                    if (showCacheLimitDialog) {
                        val options = listOf("1 GB", "2 GB", "5 GB", "10 GB")
                        OptionSelectionDialog(
                            title = "Batas Cache",
                            options = options,
                            selected = settings.cacheLimit,
                            onSelect = {
                                onCacheLimitChange(it)
                                showCacheLimitDialog = false
                            },
                            onDismiss = { showCacheLimitDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    iconTint: Color = JayraBlue,
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

@Composable
private fun SettingValueItem(
    icon: ImageVector,
    iconTint: Color = JayraBlue,
    title: String,
    value: String,
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
                text = value,
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
private fun OptionSelectionDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (option == selected),
                                onClick = { onSelect(option) }
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selected),
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = JayraBlue)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = option, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = JayraBlue)
            }
        }
    )
}

@Composable
private fun AdvancedSettingsShimmer() {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF233040) else Color(0xFFD8DFE5)
    val highlightColor = if (isDark) Color(0xFF324357) else Color(0xFFE8EEF3)

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim - 300f, y = translateAnim - 300f),
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
    }
}

@Preview(name = "Advanced Settings Light", showBackground = true)
@Composable
private fun AdvancedSettingsLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            AdvancedSettingsContent(
                uiState = AdvancedSettingsUiState.Success(
                    AdvancedSettings(
                        dataRoaming = false,
                        proxyEnabled = false,
                        callQuality = "Otomatis",
                        uploadQuality = "HD",
                        externalStorage = false,
                        cacheSize = "1.2 GB",
                        cacheLimit = "5 GB"
                    )
                ),
                onBack = {},
                onProxyClick = {},
                onToggle = { _, _ -> },
                onCallQualityChange = {},
                onUploadQualityChange = {},
                onCacheLimitChange = {},
                onClearCache = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Advanced Settings Dark", showBackground = true)
@Composable
private fun AdvancedSettingsDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            AdvancedSettingsContent(
                uiState = AdvancedSettingsUiState.Success(
                    AdvancedSettings(
                        dataRoaming = true,
                        proxyEnabled = true,
                        callQuality = "Tinggi",
                        uploadQuality = "Full HD",
                        externalStorage = true,
                        cacheSize = "3.4 GB",
                        cacheLimit = "10 GB"
                    )
                ),
                onBack = {},
                onProxyClick = {},
                onToggle = { _, _ -> },
                onCallQualityChange = {},
                onUploadQualityChange = {},
                onCacheLimitChange = {},
                onClearCache = {},
                onRetry = {}
            )
        }
    }
}
