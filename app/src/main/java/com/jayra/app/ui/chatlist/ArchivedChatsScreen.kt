package com.jayra.app.ui.chatlist
import androidx.compose.ui.draw.drawBehind
import com.jayra.app.ui.theme.IOSSeparator

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
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.IOSBlue
import com.jayra.app.ui.theme.IOSLabel
import com.jayra.app.ui.theme.IOSLabelDark
import com.jayra.app.ui.theme.IOSNavBarLightOpaque
import com.jayra.app.ui.theme.IOSNavBarDark
import com.jayra.app.ui.theme.IOSSystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackgroundDark
import com.jayra.app.ui.theme.IOSSecondaryLabel
import com.jayra.app.ui.theme.IOSSecondaryLabelDark
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.material.icons.rounded.Archive
import com.jayra.app.ui.viewmodel.ChatListViewModel

@Composable
fun ArchivedChatsScreen(
    onBack: () -> Unit,
    onChatClick: (String) -> Unit = {},
    viewModel: ChatListViewModel = viewModel()
) {
    BackHandler { onBack() }
    val archived by viewModel.archivedChats.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val tier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
    val spec = remember(tier, isDark) { LiquidGlassSpecV2.forTier(tier, isDark) }
    val navBg = if (isDark) IOSNavBarDark else IOSNavBarLightOpaque
    val screenBg = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val secondaryColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val bgGradient = Brush.verticalGradient(*spec.backdrop.gradientStops.map { it.position to it.color }.toTypedArray())
    val bgOrbs = remember(spec) { spec.backdrop.radialOrbs }

    Column(modifier = Modifier.fillMaxSize().background(bgGradient).drawBehind {
        bgOrbs.forEach { orb ->
            val c = androidx.compose.ui.geometry.Offset(size.width * orb.centerXRatio, size.height * orb.centerYRatio)
            val r = size.minDimension * orb.radiusRatio
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(orb.color.copy(alpha = 0.25f), androidx.compose.ui.graphics.Color.Transparent),
                    center = c, radius = r
                ),
                radius = r, center = c
            )
        }
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glassSurfaceV2(spec.surface, solidColor = navBg, cornerRadius = 0.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = IOSBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Diarsipkan",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                fontFamily = SFProFontFamily
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(44.dp))
        }

        if (archived.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Archive,
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Tidak ada chat diarsipkan",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    fontFamily = SFProFontFamily
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Chat yang Anda arsipkan akan muncul di sini",
                    fontSize = 15.sp,
                    color = secondaryColor,
                    fontFamily = SFProFontFamily
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(archived, key = { it.id }) { chat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatClick(chat.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = chat.avatarUrl,
                            contentDescription = chat.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = chat.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = labelColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = SFProFontFamily
                            )
                            Text(
                                text = chat.lastMessage,
                                fontSize = 15.sp,
                                color = secondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = SFProFontFamily
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(IOSBlue.copy(alpha = 0.12f))
                                .clickable { viewModel.onUnarchiveChat(chat.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Pulihkan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = IOSBlue,
                                fontFamily = SFProFontFamily
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 78.dp)
                            .height(0.5.dp)
                            .background(IOSSeparator.copy(alpha = 0.36f))
                    )
                }
            }
        }
    }
}
