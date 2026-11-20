package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayra.app.ui.theme.IOSBlueDark
import com.jayra.app.ui.theme.IOSRed
import com.jayra.app.ui.theme.IOSSecondaryLabelDark

@Composable
fun KomikReaderScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var activeSlug by remember(slug) { mutableStateOf(slug) }
    val listState = rememberLazyListState()

    LaunchedEffect(activeSlug) {
        viewModel.loadStream(activeSlug)
        listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141416))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.streamTitle ?: "Memuat chapter...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                state.comicTitle?.takeIf { it.isNotBlank() }?.let { comic ->
                    Text(
                        text = comic,
                        fontSize = 12.sp,
                        color = IOSSecondaryLabelDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (state.images.isNotEmpty()) {
                Text(
                    text = "${state.images.size} Hal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IOSBlueDark
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isStreamLoading -> CircularProgressIndicator(
                    color = IOSBlueDark,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.streamError != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = IOSRed, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.streamError ?: "Gagal memuat chapter",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.loadStream(activeSlug) },
                        colors = ButtonDefaults.buttonColors(containerColor = IOSBlueDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = state.images,
                        key = { index, url -> "${index}_$url" }
                    ) { index, imageUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .addHeader("Referer", "https://komikindo.ch/")
                                .crossfade(false)
                                .build(),
                            contentDescription = "Halaman ${index + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    val prevSlug = state.navigation?.prev?.resolvedSlug.orEmpty()
                    val nextSlug = state.navigation?.next?.resolvedSlug.orEmpty()
                    if (prevSlug.isNotBlank() || nextSlug.isNotBlank()) {
                        item(key = "chapter_nav") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF141416))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (prevSlug.isNotBlank()) {
                                    TextButton(onClick = { activeSlug = prevSlug }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = IOSBlueDark)
                                        Text("Prev", color = IOSBlueDark, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Spacer(Modifier.size(1.dp))
                                }

                                if (nextSlug.isNotBlank()) {
                                    TextButton(onClick = { activeSlug = nextSlug }) {
                                        Text("Next", color = IOSBlueDark, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = IOSBlueDark)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
