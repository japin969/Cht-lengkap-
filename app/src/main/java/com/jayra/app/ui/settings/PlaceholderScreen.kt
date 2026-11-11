package com.jayra.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFFF2F2F7)).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
            }
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).background(Color(0xFFE5E5EA), CircleShape), contentAlignment = Alignment.Center) {
                    Text("🚧", fontSize = 36.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coming Soon", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fitur $title akan segera hadir.", fontSize = 15.sp, color = Color(0xFF8E8E93))
            }
        }
    }
}
