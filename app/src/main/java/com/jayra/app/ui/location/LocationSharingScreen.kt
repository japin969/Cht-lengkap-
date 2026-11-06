package com.jayra.app.ui.location

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.model.LocationUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSharingScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: LocationViewModel = viewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationTitle by viewModel.locationTitle.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()

    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val cardBg = if (isDark) Color(0xFF242F3D) else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.fetchLocation()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bagikan Lokasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.testTag("location_topbar_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("location_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fetchLocation() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("location_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Segarkan Lokasi",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg)
            )
        },
        containerColor = screenBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Setup manual banner note
            Card(
                colors = CardDefaults.cardColors(containerColor = JayraBlue.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = JayraBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Konfigurasi Peta",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = JayraBlue
                        )
                        Text(
                            text = "PERLU SETUP MANUAL: tambah dependency com.google.maps.android:maps-compose dan API Key Google Maps di AndroidManifest.xml.",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Map Visualizer Canvas Container
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E2833) else Color(0xFFE8EEF4)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .testTag("map_visualizer_card")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Decorative map grid
                    val gridColor = if (isDark) Color(0xFF2A394A) else Color(0xFFD0DCE8)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 40.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }

                        // Pulse circle around center pin
                        drawCircle(
                            color = Color(0x330088CC),
                            radius = 70.dp.toPx(),
                            center = Offset(size.width / 2, size.height / 2)
                        )
                        drawCircle(
                            color = Color(0x550088CC),
                            radius = 40.dp.toPx(),
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }

                    // Center Pin
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Pin Lokasi",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp, 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                    }

                    // My Location Button overlay
                    IconButton(
                        onClick = { viewModel.fetchLocation() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(JayraBlue)
                            .testTag("map_my_location_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Posisikan saya",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Location Info Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (val state = uiState) {
                        is LocationUiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = JayraBlue,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Mencari lokasi GPS...",
                                    fontSize = 14.sp,
                                    color = textPrimary
                                )
                            }
                        }
                        is LocationUiState.Success -> {
                            Text(
                                text = "Detail Lokasi Terdeteksi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JayraBlue
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.address.ifBlank { "Alamat tidak tersedia" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Koordinat: %.5f, %.5f".format(state.lat, state.lng),
                                    fontSize = 12.sp,
                                    color = JayraSubtext
                                )
                                Text(
                                    text = "Akurasi: ±%.0fm".format(state.accuracyMeters),
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is LocationUiState.Error -> {
                            Text(
                                text = state.message,
                                fontSize = 14.sp,
                                color = Color(0xFFE53935)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Label Lokasi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JayraBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = locationTitle,
                        onValueChange = { viewModel.updateTitle(it) },
                        placeholder = { Text("Contoh: Lokasi Terkini Saya, Kantor", color = JayraSubtext) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_label_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JayraBlue,
                            unfocusedBorderColor = JayraSubtext.copy(alpha = 0.3f),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Share Location Action Button
            Button(
                onClick = {
                    viewModel.shareLocation(chatId = chatId, onSuccess = onBack)
                },
                enabled = uiState is LocationUiState.Success && !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = JayraBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("share_location_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShareLocation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bagikan Lokasi Ini",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(name = "Location Sharing Light", showBackground = true)
@Composable
private fun LocationSharingScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        LocationSharingScreen(chatId = "chat_preview", onBack = {})
    }
}

@Preview(name = "Location Sharing Dark", showBackground = true)
@Composable
private fun LocationSharingScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        LocationSharingScreen(chatId = "chat_preview", onBack = {})
    }
}
