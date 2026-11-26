package com.jayra.app.ui.comic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.data.MediaEntry
import com.jayra.app.data.MediaStore
import com.jayra.app.ui.media.CenterState
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaDimens
import com.jayra.app.ui.media.MediaPoster
import com.jayra.app.ui.theme.SFProFontFamily

/**
 * ComicBookmark — koleksi komik tersimpan + progress baca + tombol hapus.
 * Pola aplikasi komik terkenal (Webtoon "My Library").
 */
@Composable
fun ComicBookmarkScreen(
    onBack: () -> Unit,
    onComicClick: (String) -> Unit,
    onBrowse: () -> Unit
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(MediaStore.bookmarks(context, "comic")) }

    Column(Modifier.fillMaxSize().background(MediaColors.Bg)) {
        MediaListTopBar("Bookmark", onBack)
        if (entries.isEmpty()) {
            CenterState(
                icon = { Icon(Icons.Default.BookmarkBorder, null, tint = MediaColors.Comic, modifier = Modifier.size(34.dp)) },
                title = "Belum ada bookmark",
                subtitle = "Simpan komik favoritmu agar mudah dibaca lagi.",
                actionLabel = "Jelajahi Komik",
                onAction = onBrowse,
                accent = MediaColors.Comic
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(MediaDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries) { entry ->
                    BookmarkRow(entry, onOpen = { onComicClick(entry.id) }, onRemove = {
                        MediaStore.toggleBookmark(context, "comic", entry)
                        entries = MediaStore.bookmarks(context, "comic")
                    })
                }
            }
        }
    }
}

@Composable
fun BookmarkRow(entry: MediaEntry, onOpen: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MediaColors.SurfaceElevated)
            .clickable { onOpen() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaPoster(entry.cover, entry.title, accent = MediaColors.Comic, modifier = Modifier.size(54.dp, 76.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                color = MediaColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.progressLabel.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(entry.progressLabel, fontFamily = SFProFontFamily, fontSize = 12.sp, color = MediaColors.Comic)
                Spacer(Modifier.height(6.dp))
                LinearProgress(progress = entry.progress)
            }
        }
        Box(Modifier.size(36.dp).clickable { onRemove() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Bookmark, "Hapus", tint = MediaColors.Comic, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun LinearProgress(progress: Float) {
    Box(
        Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MediaColors.SurfaceHigh)
    ) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MediaColors.Comic)
        )
    }
}

@Composable
fun MediaListTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MediaColors.Bg).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MediaColors.TextPrimary)
        }
        Spacer(Modifier.size(4.dp))
        Text(title, fontFamily = SFProFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
            color = MediaColors.TextPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
    }
}