package com.jayra.app.ui.about

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
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
import com.jayra.app.model.AboutUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraOrange
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onPrivacyClick: () -> Unit = {},
    viewModel: AboutViewModel = viewModel()
) {
    BackHandler { onBack() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AboutContent(
        uiState = uiState,
        onBack = onBack,
        onPrivacyClick = onPrivacyClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContent(
    uiState: AboutUiState,
    onBack: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) JayraCardDark else JayraCardLight

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tentang Jayra",
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
                            .testTag("about_back_button")
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
            is AboutUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = JayraBlue)
                }
            }
            is AboutUiState.Error -> {
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
            is AboutUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Jayra Logo (120dp Circle with Gradient)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2AABEE),
                                        Color(0xFF229ED9),
                                        Color(0xFF0088CC)
                                    )
                                )
                            )
                            .testTag("jayra_app_logo"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Jayra Logo",
                            tint = Color.White,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(end = 4.dp, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = uiState.appName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1E1E1E)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Versi ${uiState.version} (Build ${uiState.buildNumber})",
                        fontSize = 14.sp,
                        color = JayraSubtext
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Info Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AboutRowItem(
                                title = "Pengembang",
                                subtitle = uiState.developer,
                                icon = Icons.Rounded.VerifiedUser,
                                iconTint = JayraBlue
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            AboutRowItem(
                                title = "Lisensi",
                                subtitle = uiState.license,
                                icon = Icons.Rounded.Code,
                                iconTint = JayraOrange
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            AboutRowItem(
                                title = "Rilis Terakhir",
                                subtitle = uiState.releaseDate,
                                icon = Icons.Rounded.Star,
                                iconTint = JayraSuccessGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Links Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            AboutClickableRow(
                                title = "Kode Sumber Terbuka (GitHub)",
                                icon = Icons.Rounded.Code,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.githubUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )
                            AboutClickableRow(
                                title = "Situs Web Resmi",
                                icon = Icons.Rounded.Language,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.websiteUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )
                            AboutClickableRow(
                                title = "Beri Nilai Aplikasi",
                                icon = Icons.Rounded.Star,
                                onClick = {
                                    Toast.makeText(context, "Terima kasih telah mendukung Jayra!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            AboutClickableRow(
                                title = "Kebijakan Privasi & Ketentuan",
                                icon = Icons.Rounded.Policy,
                                onClick = onPrivacyClick,
                                isLast = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Jayra Messenger untuk Android\nDibuat dengan penuh dedikasi untuk kebebasan komunikasi.",
                        fontSize = 12.sp,
                        color = JayraSubtext,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun AboutRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 12.sp, color = JayraSubtext)
            Text(text = subtitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AboutClickableRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isLast: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_link_${title.lowercase().replace(" ", "_")}"),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = JayraBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = JayraSubtext,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    if (!isLast) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp)
                .height(0.5.dp)
                .background(JayraSubtext.copy(alpha = 0.2f))
        )
    }
}

@Preview(name = "AboutScreen Light Mode", showBackground = true)
@Composable
fun AboutScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        AboutContent(
            uiState = AboutUiState.Success(),
            onBack = {},
            onPrivacyClick = {}
        )
    }
}

@Preview(name = "AboutScreen Dark Mode", showBackground = true)
@Composable
fun AboutScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        AboutContent(
            uiState = AboutUiState.Success(),
            onBack = {},
            onPrivacyClick = {}
        )
    }
}
