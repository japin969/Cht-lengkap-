package com.example.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.SplashUiState
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SplashViewModel(
    private val authProvider: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    fun isUserAuthenticated(): Boolean {
        return try {
            authProvider()?.currentUser != null
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun SplashScreen(
    onNavigateToChatList: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.splashDelayMillis) {
        isVisible = true
        delay(uiState.splashDelayMillis)
        if (viewModel.isUserAuthenticated()) {
            onNavigateToChatList()
        } else {
            onNavigateToLogin()
        }
    }

    SplashContent(
        uiState = uiState,
        isVisible = isVisible
    )
}

@Composable
fun SplashContent(
    uiState: SplashUiState,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) TelegramDarkTopBar else TelegramLightTopBar

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding()
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 500)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SplashLogoBadge()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.appTitle,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("splash_title_text")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = uiState.tagline,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = TelegramSubtext,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("splash_tagline_text")
                )
            }
        }
    }
}

@Composable
fun SplashLogoBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(TelegramBlue)
            .testTag("splash_logo_badge"),
        contentAlignment = Alignment.Center
    ) {
        // GANTI SVG TELEGRAM
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Logo Jayra",
            tint = Color(0xFFFFFFFF),
            modifier = Modifier.size(64.dp)
        )
    }
}

// =========================================================================
// PREVIEWS (LIGHT & DARK UNTUK SETIAP KOMPONEN)
// =========================================================================

@Preview(name = "Splash Logo Badge - Light", showBackground = true)
@Composable
private fun SplashLogoBadgeLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface(color = TelegramLightTopBar) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                SplashLogoBadge()
            }
        }
    }
}

@Preview(name = "Splash Logo Badge - Dark", showBackground = true)
@Composable
private fun SplashLogoBadgeDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface(color = TelegramDarkTopBar) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                SplashLogoBadge()
            }
        }
    }
}

@Preview(name = "Splash Screen - Light", showBackground = true)
@Composable
private fun SplashScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        SplashContent(
            uiState = SplashUiState(),
            isVisible = true
        )
    }
}

@Preview(name = "Splash Screen - Dark", showBackground = true)
@Composable
private fun SplashScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        SplashContent(
            uiState = SplashUiState(),
            isVisible = true
        )
    }
}
