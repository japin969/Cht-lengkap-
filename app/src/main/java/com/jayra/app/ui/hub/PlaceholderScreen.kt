package com.jayra.app.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.theme.SFProFontFamily

@Composable
fun PlaceholderScreen(title: String, accentColor: Color = Color(0xFF00B4D8), onBack: () -> Unit) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bg = if (isDark) Brush.verticalGradient(listOf(Color(0xFF0A0A0F), Color(0xFF12121A)))
    else Brush.verticalGradient(listOf(Color(0xFFF5F5F7), Color(0xFFEDEDF0)))

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = if (isDark) Color.White else Color(0xFF1C1C1E))
            }
            Text(
                text = title,
                fontFamily = SFProFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Construction,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Fitur $title",
                    fontFamily = SFProFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Segera Hadir",
                    fontFamily = SFProFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF636366)
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Kembali ke Hub",
                        fontFamily = SFProFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
```