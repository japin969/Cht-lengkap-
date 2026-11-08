package com.jayra.app.ui.chatroom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jayra.app.model.Chat
import com.jayra.app.model.Contact

@Composable
fun ChatRoomTopBar(
    contact: Contact?,
    currentChat: Chat?,
    onBack: () -> Unit,
    onContactInfoClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF9F9F9)
    val textPrimary = if (isDark) Color.White else Color.Black

    val name = contact?.name ?: currentChat?.name ?: "Chat"
    val status = contact?.status ?: if (currentChat?.isGroup == true) "${currentChat.participants.size.coerceAtLeast(2)} anggota" else "online"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF0A84FF), modifier = Modifier.size(24.dp))
        }
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onContactInfoClick).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF0A84FF))) {
                if (contact?.avatarUrl?.isNotBlank() == true) {
                    AsyncImage(model = contact.avatarUrl, contentDescription = name, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, letterSpacing = (-0.2).sp)
                Text(status, fontSize = 13.sp, color = Color.Gray)
            }
        }
        IconButton(onClick = { }) { Icon(Icons.Rounded.Videocam, "Video", tint = Color(0xFF0A84FF)) }
        IconButton(onClick = { }) { Icon(Icons.Rounded.Call, "Call", tint = Color(0xFF0A84FF)) }
        IconButton(onClick = onMenuClick) { Icon(Icons.Rounded.MoreVert, "Menu", tint = Color(0xFF0A84FF)) }
    }
}
