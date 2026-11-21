package com.jayra.app.ui.komik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.jayra.app.data.KomikItem
import com.jayra.app.ui.theme.IOSBlue
import com.jayra.app.ui.theme.IOSBlueDark
import com.jayra.app.ui.theme.IOSLabel
import com.jayra.app.ui.theme.IOSLabelDark
import com.jayra.app.ui.theme.IOSOrange
import com.jayra.app.ui.theme.IOSRed
import com.jayra.app.ui.theme.IOSSecondaryLabel
import com.jayra.app.ui.theme.IOSSecondaryLabelDark
import com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark
import com.jayra.app.ui.theme.IOSSecondarySystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackground
import com.jayra.app.ui.theme.IOSSystemGroupedBackgroundDark
import com.jayra.app.util.glassSurfaceV2

@Composable
fun KomikDetailScreen(
    slug: String,
    viewModel: KomikViewModel = viewModel(),
    onChapterClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) IOSSystemGroupedBackgroundDark else IOSSystemGroupedBackground
    val labelColor = if (isDark) IOSLabelDark else IOSLabel
    val subLabelColor = if (isDark) IOSSecondaryLabelDark else IOSSecondaryLabel
    val accentBlue = if (isDark) IOSBlueDark else IOSBlue
    val cardBg = if (isDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemGroupedBackground

    LaunchedEffect(slug) { viewModel.loadDetail(slug) }

    if (state.isLoading && state.detailItem == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = accentBlue)
        }
    } else if (state.detailItem == null && state.error != null) {
        Box(
            modifier = Modifier.fillMaxSize().background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            DetailErrorView(
                message = state.error ?: "Gagal memuat detail",
                accentBlue = accentBlue,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                onRetry = { viewModel.loadDetail(slug) }
            )
        }
    } else {
        val item = state.detailItem
        if (item != null) {
            DetailContent(
                item = item,
                accentBlue = accentBlue,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                bgColor = bgColor,
                cardBg = cardBg,
                onChapterClick = onChapterClick
            )
        } else {
            DetailErrorView(
                message = "Komik tidak ditemukan",
                accentBlue = accentBlue,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                onRetry = { viewModel.loadDetail(slug) }
            )
        }
    }
}

@Composable
private fun DetailContent(
    item: com.jayra.app.data.KomikItem,
    accentBlue: Color,
    labelColor: Color,
    subLabelColor: Color,
    bgColor: Color,
    cardBg: Color,
    onChapterClick: (String) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header with cover
        item(key = "header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.cover)
                        .addHeader("Referer", "https://komikindo.ch/")
                        .crossfade(true)
                        .build(),
                    contentDescription = item.cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(colors = listOf(Color(0x00000000), Color(0x88000000)))
                    )
                )
                // Back button
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                ) {
                    IconButton(onClick = { /* handled by navigation */ }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                // Action buttons
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionIconButton(icon = Icons.Rounded.FavoriteBorder, onClick = { /* TODO: add to favorites */ })
                        ActionIconButton(icon = Icons.Rounded.Share, onClick = { /* TODO: share */ })
                        ActionIconButton(icon = Icons.Rounded.OpenInNew, onClick = { item.link?.let { openInBrowser(context, it) } })
                    }
                }
                // Title overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val type = item.type
                        if (type != null && type.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x55FFFFFF))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(type, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                        val latestChapter = item.latestChapter
                        if (latestChapter != null && latestChapter.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentBlue)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(latestChapter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.cleanTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val rating = item.rating
                        if (rating != null && rating.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = IOSOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(rating, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if (item.link != null && item.link!.isNotBlank()) {
                            Text("Sumber: komikindo.ch", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        // Info & Actions
        item(key = "info") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 20.dp)
            ) {
                // Description placeholder (API doesn't provide synopsis)
                Text(
                    text = "Sinopsis tidak tersedia dari API. Buka link asli untuk detail lengkap.",
                    fontSize = 14.sp,
                    color = subLabelColor,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Read button
                Button(
                    onClick = { item.link?.let { openInBrowser(context, it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Baca di Komikindo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Chapter List Section
        item(key = "chapter_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar Chapter", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = labelColor)
                Text(
                    text = "Data chapter dari API tidak tersedia",
                    fontSize = 12.sp,
                    color = subLabelColor
                )
            }
        }

        // Placeholder chapter list - API doesn't provide chapters
        items(5) { index ->
            val chapterNum = index + 1
            ChapterItem(
                title = "Chapter $chapterNum",
                subtitle = "Data chapter dari API tidak tersedia. Buka link asli.",
                accentBlue = accentBlue,
                labelColor = labelColor,
                subLabelColor = subLabelColor,
                cardBg = cardBg,
                onClick = { /* TODO: navigate to reader */ }
            )
        }

        // Link to source
        item(key = "source_link") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Buka Halaman Komik di Komikindo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.link ?: "Link tidak tersedia",
                        fontSize = 12.sp,
                        color = subLabelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { item.link?.let { openInBrowser(context, it) } },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Buka di Browser", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0x88000000))
            .size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ChapterItem(
    title: String,
    subtitle: String,
    accentBlue: Color,
    labelColor: Color,
    subLabelColor: Color,
    cardBg: Color,
    onClick: () -> Unit
) {
    androidx.compose.material3.ListItem(
        headlineContent = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = subLabelColor
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.MenuBook,
                contentDescription = "Baca",
                tint = accentBlue,
                modifier = Modifier.size(20.dp).padding(end = 8.dp)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(cardBg)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = androidx.compose.material3.ListItemDefaults.listItemColors(
            containerColor = cardBg,
            pressedContainerColor = accentBlue.copy(alpha = 0.1f)
        )
    )
}

@Composable
private fun DetailErrorView(
    message: String,
    accentBlue: Color,
    labelColor: Color,
    subLabelColor: Color,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = IOSRed,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Gagal Memuat Detail",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = subLabelColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Coba Lagi", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun openInBrowser(context: android.content.Context, url: String) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }
}