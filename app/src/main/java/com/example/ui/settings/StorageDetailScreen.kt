package com.example.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FileInfo
import com.example.model.StorageDetailUiState
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDangerRed
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSeparatorDark
import com.example.ui.theme.TelegramSeparatorLight
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

@Composable
fun StorageDetailScreen(
    category: String,
    onBack: () -> Unit,
    viewModel: StorageDetailViewModel = viewModel()
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(category) {
        viewModel.loadFiles(category)
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    StorageDetailContent(
        category = category,
        uiState = uiState,
        onBack = onBack,
        onDeleteFile = { file -> viewModel.onDeleteFile(file, category) },
        onRetry = { viewModel.loadFiles(category) }
    )
}

@Composable
fun StorageDetailContent(
    category: String,
    uiState: StorageDetailUiState,
    onBack: () -> Unit,
    onDeleteFile: (FileInfo) -> Unit,
    onRetry: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val separatorColor = if (isDark) TelegramSeparatorDark else TelegramSeparatorLight

    val categoryTitle = when (category.lowercase()) {
        "photo" -> "Foto"
        "video" -> "Video"
        "document" -> "Dokumen"
        "music" -> "Musik"
        else -> "Lainnya"
    }

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
                        .testTag("storage_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = categoryTitle,
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
                is StorageDetailUiState.Loading -> {
                    // Loading
                }

                is StorageDetailUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Kosong",
                            tint = TelegramSubtext,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tidak ada berkas di kategori ini",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = TelegramSubtext,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is StorageDetailUiState.Error -> {
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
                            tint = TelegramDangerRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.message,
                            fontSize = 17.sp,
                            color = TelegramSubtext,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Coba Lagi", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }

                is StorageDetailUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = uiState.files,
                            key = { it.id },
                            contentType = { "file_item" }
                        ) { file ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color.White else Color.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row {
                                            Text(
                                                text = file.size,
                                                fontSize = 13.sp,
                                                color = TelegramSubtext
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "•",
                                                fontSize = 13.sp,
                                                color = TelegramSubtext
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = file.date,
                                                fontSize = 13.sp,
                                                color = TelegramSubtext
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteFile(file) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("delete_file_${file.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus berkas",
                                            tint = TelegramDangerRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp)
                                        .height(0.5.dp)
                                        .background(separatorColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Storage Detail Light", showBackground = true)
@Composable
private fun StorageDetailScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            StorageDetailContent(
                category = "photo",
                uiState = StorageDetailUiState.Success(
                    listOf(
                        FileInfo("1", "IMG_20261102_001.jpg", "3.2 MB", "02 Nov 2026", ""),
                        FileInfo("2", "IMG_20261101_042.png", "2.8 MB", "01 Nov 2026", "")
                    )
                ),
                onBack = {},
                onDeleteFile = {},
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Storage Detail Dark", showBackground = true)
@Composable
private fun StorageDetailScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            StorageDetailContent(
                category = "video",
                uiState = StorageDetailUiState.Success(
                    listOf(
                        FileInfo("1", "VID_20261028_102.mp4", "45.0 MB", "28 Okt 2026", "")
                    )
                ),
                onBack = {},
                onDeleteFile = {},
                onRetry = {}
            )
        }
    }
}
