package com.jayra.app.ui.donghua

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.DonghuaSource
import com.jayra.app.ui.video.NativeVideoPlayer
import com.jayra.app.ui.theme.JayraBlue

@Composable
fun DonghuaPlayerScreen(
    slug: String,
    source: DonghuaSource = DonghuaSource.DONGHUA,
    viewModel: DonghuaViewModel = viewModel(),
    onBack: () -> Unit,
    onNextEpisode: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load stream when slug changes
    LaunchedEffect(slug) {
        viewModel.loadStream(slug, source)
    }

    // Reset loading when stream URL changes
    LaunchedEffect(state.streamUrl) {
        isLoading = true
        errorMessage = null
    }

    if (state.isLoadingStream) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = JayraBlue)
        }
    } else if (state.streamUrl == null || state.streamUrl?.streamUrl?.isNotBlank() != true) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.material.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.ErrorOutlined,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Tidak ada URL video", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Episode ini tidak memiliki link streaming", color = Color.LightGray, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = JayraBlue)
                ) {
                    Text("Kembali", color = Color.White)
                }
            }
        }
    } else {
        val streamUrl = state.streamUrl!!
        val headers = streamUrl.headers + mapOf("Referer" to (streamUrl.referer ?: "https://www.sankavollerei.web.id/"))

        NativeVideoPlayer(
            videoUrl = streamUrl.streamUrl!!,
            headers = headers,
            title = "Donghua Episode",
            autoPlay = true,
            onError = { error ->
                errorMessage = error
                isLoading = false
                onError?.invoke(error)
            },
            onCompletion = {
                onNextEpisode?.invoke()
            },
            onPrepared = {
                isLoading = false
            }
        )
    }
}