package com.example.ui.memberpermissions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramDarkChatBg
import com.example.ui.theme.TelegramDarkTopBar
import com.example.ui.theme.TelegramLightChatBg
import com.example.ui.theme.TelegramLightTopBar
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

data class PermissionItemDef(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val PERMISSION_DEFINITIONS = listOf(
    PermissionItemDef(
        key = "send_messages",
        title = "Kirim Pesan",
        description = "Mengizinkan anggota mengirim pesan teks dan stiker",
        icon = Icons.Default.Message
    ),
    PermissionItemDef(
        key = "send_media",
        title = "Kirim Media",
        description = "Mengizinkan pengiriman foto, video, berkas, dan audio",
        icon = Icons.Default.Image
    ),
    PermissionItemDef(
        key = "add_members",
        title = "Tambah Anggota",
        description = "Mengizinkan anggota mengundang orang lain ke grup",
        icon = Icons.Default.PersonAdd
    ),
    PermissionItemDef(
        key = "pin_messages",
        title = "Pin Pesan",
        description = "Mengizinkan anggota menyematkan pesan obrolan penting",
        icon = Icons.Default.PushPin
    ),
    PermissionItemDef(
        key = "change_info",
        title = "Ubah Info Grup",
        description = "Mengizinkan penggantian nama, foto, dan deskripsi grup",
        icon = Icons.Default.Edit
    )
)

@Composable
fun MemberPermissionsScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: MemberPermissionsViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(groupId) {
        viewModel.loadPermissions(groupId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MemberPermissionsContent(
        uiState = uiState,
        onBack = onBack,
        onUpdatePermission = viewModel::updatePermission
    )
}

@Composable
fun MemberPermissionsContent(
    uiState: MemberPermissionsUiState,
    onBack: () -> Unit,
    onUpdatePermission: (String, String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topBarBg = if (isDark) TelegramDarkTopBar else TelegramLightTopBar
    val screenBg = if (isDark) TelegramDarkChatBg else TelegramLightChatBg
    val cardBg = if (isDark) TelegramCardDark else TelegramCardLight

    var activeDialogPermission by remember { mutableStateOf<PermissionItemDef?>(null) }
    var selectedRoleOption by remember { mutableStateOf("Semua Anggota") }

    val currentPermissions = (uiState as? MemberPermissionsUiState.Success)?.permissions ?: mapOf(
        "send_messages" to "Semua Anggota",
        "send_media" to "Semua Anggota",
        "add_members" to "Admin",
        "pin_messages" to "Admin",
        "change_info" to "Admin"
    )

    // Edit Role Dialog
    if (activeDialogPermission != null) {
        val perm = activeDialogPermission!!
        val options = listOf("Semua Anggota", "Admin")

        AlertDialog(
            onDismissRequest = { activeDialogPermission = null },
            title = {
                Text(
                    text = perm.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Pilih siapa yang memiliki izin ini:",
                        fontSize = 14.sp,
                        color = TelegramSubtext,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    options.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRoleOption = role }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRoleOption == role,
                                onClick = { selectedRoleOption = role },
                                colors = RadioButtonDefaults.colors(selectedColor = TelegramBlue)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = role, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdatePermission(perm.key, selectedRoleOption)
                        activeDialogPermission = null
                    },
                    modifier = Modifier.testTag("perm_dialog_save")
                ) {
                    Text("Simpan", color = TelegramBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { activeDialogPermission = null },
                    modifier = Modifier.testTag("perm_dialog_cancel")
                ) {
                    Text("Batal", color = Color(0xFF8E8E93))
                }
            }
        )
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
                        .testTag("member_permissions_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Izin Anggota",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        },
        containerColor = screenBg
    ) { innerPadding ->
        if (uiState is MemberPermissionsUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TelegramBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Info Card / Description
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TelegramBlue.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TelegramBlue,
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tentukan hak akses untuk anggota biasa di dalam grup. Izin yang diatur ke 'Admin' hanya dapat dijalankan oleh pemilik dan administrator grup.",
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "DAFTAR IZIN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 6.dp)
                    )
                }

                // Card Containing 5 Permission Rows
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column {
                            PERMISSION_DEFINITIONS.forEachIndexed { index, def ->
                                val currentRole = currentPermissions[def.key] ?: "Semua Anggota"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedRoleOption = currentRole
                                            activeDialogPermission = def
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                        .testTag("perm_row_${def.key}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = def.icon,
                                            contentDescription = null,
                                            tint = TelegramBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = def.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = def.description,
                                                fontSize = 13.sp,
                                                color = TelegramSubtext
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentRole,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (currentRole == "Admin") TelegramBlue else TelegramSubtext
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Ubah",
                                            tint = TelegramSubtext,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (index < PERMISSION_DEFINITIONS.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 56.dp),
                                        thickness = 0.5.dp,
                                        color = TelegramSubtext.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun MemberPermissionsScreenLightPreview() {
    TelegramTheme(darkTheme = false) {
        Surface {
            MemberPermissionsContent(
                uiState = MemberPermissionsUiState.Success(
                    permissions = mapOf(
                        "send_messages" to "Semua Anggota",
                        "send_media" to "Semua Anggota",
                        "add_members" to "Admin",
                        "pin_messages" to "Admin",
                        "change_info" to "Admin"
                    )
                ),
                onBack = {},
                onUpdatePermission = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun MemberPermissionsScreenDarkPreview() {
    TelegramTheme(darkTheme = true) {
        Surface {
            MemberPermissionsContent(
                uiState = MemberPermissionsUiState.Success(
                    permissions = mapOf(
                        "send_messages" to "Admin",
                        "send_media" to "Admin",
                        "add_members" to "Admin",
                        "pin_messages" to "Admin",
                        "change_info" to "Admin"
                    )
                ),
                onBack = {},
                onUpdatePermission = { _, _ -> }
            )
        }
    }
}
