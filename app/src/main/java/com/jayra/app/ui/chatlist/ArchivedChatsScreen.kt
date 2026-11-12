package com.jayra.app.ui.chatlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jayra.app.ui.theme.SFProFontFamily
import com.jayra.app.ui.viewmodel.ChatListViewModel

@Composable
fun ArchivedChatsScreen(
    onBack: () -> Unit,
    onChatClick: (String) -> Unit = {},
    viewModel: ChatListViewModel = viewModel()
) {
    BackHandler { onBack() }
    val archived by viewModel.archivedChats.collectAsStateWithLifecycle()

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E3A5F), Color(0xFF4A2E6B))
    )

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color(0xFF5AC8FA),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Diarsipkan",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = SFProFontFamily
                )
            }

            if (archived.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada chat diarsipkan",
                        fontSize = 15.sp,
                        color = Color(0x99FFFFFF),
                        fontFamily = SFProFontFamily
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(archived, key = { it.id }) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1AFFFFFF))
                                .clickable { onChatClick(chat.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = chat.avatarUrl,
                                contentDescription = chat.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = chat.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = SFProFontFamily
                                )
                                Text(
                                    text = chat.lastMessage,
                                    fontSize = 15.sp,
                                    color = Color(0x99FFFFFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = SFProFontFamily
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33FFFFFF))
                                    .clickable { viewModel.onUnarchiveChat(chat.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Pulihkan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    fontFamily = SFProFontFamily
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 76.dp)
                                .height(0.5.dp)
                                .background(Color(0x33FFFFFF))
                        )
                    }
                }
            }
        }
    }
}
