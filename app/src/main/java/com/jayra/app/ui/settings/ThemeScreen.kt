package com.jayra.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.ThemeOption
import com.jayra.app.model.ThemeUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun ThemeScreen(
    onBack: () -> Unit,
    viewModel: ThemeViewModel = viewModel()
) {
    BackHandler { onBack() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ThemeContent(
        uiState = uiState,
        onBack = onBack,
        onThemeSelect = viewModel::selectTheme,
        onDynamicColorToggle = viewModel::toggleDynamicColor,
        onAccentColorSelect = viewModel::selectAccentColor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeContent(
    uiState: ThemeUiState,
    onBack: () -> Unit,
    onThemeSelect: (String) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onAccentColorSelect: (Long) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    val accentColors = listOf(
        0xFF0088CC to "Biru Jayra",
        0xFF2AABEE to "Biru Langit",
        0xFF00BFA5 to "Hijau Toska",
        0xFFFF8A00 to "Jingga Cerah",
        0xFFE91E63 to "Merah Muda",
        0xFF7B1FA2 to "Ungu Elegan"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tema & Tampilan",
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
                            .testTag("theme_back_button")
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
            is ThemeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = JayraBlue)
                }
            }
            is ThemeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        color = JayraSubtext,
                        fontSize = 16.sp
                    )
                }
            }
            is ThemeUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Chat Preview Card
                    Text(
                        text = "Pratinjau Obrolan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        color = if (uiState.currentTheme == "dark" || (uiState.currentTheme == "system" && isDark)) JayraDarkChatBg else JayraLightChatBg,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Incoming message bubble
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Hai! Ini adalah pratinjau tampilan tema Jayra.",
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Outgoing message bubble
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .align(Alignment.End)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(uiState.accentColorHex))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Keren sekali! Sangat nyaman di mata.",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Theme Options
                    Text(
                        text = "Mode Tampilan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            uiState.availableThemes.forEachIndexed { index, option ->
                                val isSelected = uiState.currentTheme == option.id
                                val icon = when (option.id) {
                                    "light" -> Icons.Default.LightMode
                                    "dark" -> Icons.Default.DarkMode
                                    else -> Icons.Default.SettingsBrightness
                                }

                                ThemeRadioRow(
                                    option = option,
                                    icon = icon,
                                    isSelected = isSelected,
                                    onClick = { onThemeSelect(option.id) },
                                    isLast = index == uiState.availableThemes.size - 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Accent Colors
                    Text(
                        text = "Warna Aksen",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            accentColors.forEach { (colorValue, name) ->
                                val isSelected = uiState.accentColorHex == colorValue
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorValue))
                                        .clickable { onAccentColorSelect(colorValue) }
                                        .testTag("accent_color_${name.lowercase().replace(" ", "_")}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Dipilih",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dynamic Color Toggle
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Warna Dinamis (Material You)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Menyesuaikan nuansa warna dengan wallpaper perangkat Anda.",
                                    fontSize = 12.sp,
                                    color = JayraSubtext
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.dynamicColor,
                                onCheckedChange = onDynamicColorToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = JayraBlue
                                ),
                                modifier = Modifier.testTag("dynamic_color_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ThemeRadioRow(
    option: ThemeOption,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLast: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_option_${option.id}"),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(JayraBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = JayraBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.description,
                    fontSize = 12.sp,
                    color = JayraSubtext
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = JayraBlue)
            )
        }
    }
    if (!isLast) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp)
                .height(0.5.dp)
                .background(JayraSubtext.copy(alpha = 0.2f))
        )
    }
}

@Preview(name = "ThemeScreen Light Mode", showBackground = true)
@Composable
fun ThemeScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        ThemeContent(
            uiState = ThemeUiState.Success(
                currentTheme = "system",
                availableThemes = listOf(
                    ThemeOption("system", "Ikuti Sistem", "Sesuai sistem perangkat"),
                    ThemeOption("light", "Mode Terang", "Cerah"),
                    ThemeOption("dark", "Mode Gelap", "Gelap")
                )
            ),
            onBack = {},
            onThemeSelect = {},
            onDynamicColorToggle = {},
            onAccentColorSelect = {}
        )
    }
}

@Preview(name = "ThemeScreen Dark Mode", showBackground = true)
@Composable
fun ThemeScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        ThemeContent(
            uiState = ThemeUiState.Success(
                currentTheme = "dark",
                availableThemes = listOf(
                    ThemeOption("system", "Ikuti Sistem", "Sesuai sistem perangkat"),
                    ThemeOption("light", "Mode Terang", "Cerah"),
                    ThemeOption("dark", "Mode Gelap", "Gelap")
                )
            ),
            onBack = {},
            onThemeSelect = {},
            onDynamicColorToggle = {},
            onAccentColorSelect = {}
        )
    }
}
