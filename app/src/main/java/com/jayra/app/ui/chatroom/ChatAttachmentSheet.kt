package com.jayra.app.ui.chatroom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAttachmentSheet(
    chatId: String,
    onDismiss: () -> Unit,
    onNavigateToCreatePoll: (String) -> Unit,
    onNavigateToCreateChecklist: (String) -> Unit,
    onNavigateToVoiceTrimmer: (String) -> Unit,
    onNavigateToLocation: (String) -> Unit,
    onNavigateToContact: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Lampirkan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black, modifier = Modifier.padding(bottom = 16.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AttachmentOptionItem(Icons.Rounded.Poll, "Poling", Color(0xFF0A84FF)) { onDismiss(); onNavigateToCreatePoll(chatId) }
                AttachmentOptionItem(Icons.Rounded.Checklist, "Tugas", Color(0xFF34C759)) { onDismiss(); onNavigateToCreateChecklist(chatId) }
                AttachmentOptionItem(Icons.Rounded.Mic, "Suara", Color(0xFFFF9500)) { onDismiss(); onNavigateToVoiceTrimmer(chatId) }
                AttachmentOptionItem(Icons.Rounded.LocationOn, "Lokasi", Color(0xFF4CAF50)) { onDismiss(); onNavigateToLocation(chatId) }
                AttachmentOptionItem(Icons.Rounded.Person, "Kontak", Color(0xFFFF9800)) { onDismiss(); onNavigateToContact(chatId) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AttachmentOptionItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(color).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

