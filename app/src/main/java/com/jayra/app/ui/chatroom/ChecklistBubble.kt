package com.jayra.app.ui.chatroom

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.model.Checklist
import com.jayra.app.model.ChecklistItem
import com.jayra.app.model.ChecklistStrings
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraBubbleInDark
import com.jayra.app.ui.theme.JayraBubbleOutLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme

@Composable
fun ChecklistBubble(
    checklist: Checklist,
    strings: ChecklistStrings = ChecklistStrings(),
    onToggleItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val completedTextColor = JayraSubtext
    val itemSurfaceColor = if (isDark) {
        Color(0xFFFFFFFF).copy(alpha = 0.06f)
    } else {
        Color(0xFF000000).copy(alpha = 0.04f)
    }
    val trackColor = if (isDark) {
        Color(0xFFFFFFFF).copy(alpha = 0.14f)
    } else {
        JayraBlue.copy(alpha = 0.16f)
    }

    val isAllCompleted = checklist.totalCount > 0 && checklist.completedCount == checklist.totalCount
    val accentColor by animateColorAsState(
        targetValue = if (isAllCompleted) JayraSuccessGreen else JayraBlue,
        animationSpec = tween(durationMillis = 250),
        label = "checklist_accent_color"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = checklist.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250),
        label = "checklist_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("checklist_bubble_${checklist.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Badge + Progress Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Checklist,
                        contentDescription = strings.bubbleBadgeLabel,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = strings.bubbleBadgeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("checklist_progress_badge_${checklist.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${checklist.completedCount}/${checklist.totalCount} ${strings.completedProgressSuffix}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }

        // Checklist Title
        Text(
            text = checklist.title,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor,
            modifier = Modifier.testTag("checklist_title_${checklist.id}")
        )

        // Animated Progress Bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            color = accentColor,
            trackColor = trackColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .testTag("checklist_progress_bar_${checklist.id}")
        )

        // Interactive Task Rows
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            checklist.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemSurfaceColor)
                        .clickable { onToggleItem(item.id) }
                        .padding(start = 4.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)
                        .testTag("checklist_item_row_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isDone,
                        onCheckedChange = { onToggleItem(item.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = JayraSuccessGreen,
                            uncheckedColor = JayraBlue,
                            checkmarkColor = Color(0xFFFFFFFF)
                        ),
                        modifier = Modifier.testTag("checklist_checkbox_${item.id}")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.text,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium,
                        color = if (item.isDone) completedTextColor else primaryTextColor,
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(name = "Checklist Bubble Light Mode", showBackground = true)
@Composable
private fun ChecklistBubbleLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface(color = JayraBubbleOutLight) {
            Box(modifier = Modifier.padding(12.dp)) {
                ChecklistBubble(
                    checklist = Checklist(
                        id = "preview_cl_light",
                        title = "Persiapan Rilis Jayra v2.5",
                        items = listOf(
                            ChecklistItem(id = "t1", text = "Audit aturan keamanan Firestore", isDone = true),
                            ChecklistItem(id = "t2", text = "Uji komponen Compose Light & Dark", isDone = true),
                            ChecklistItem(id = "t3", text = "Publikasi catatan pembaruan tim", isDone = false)
                        )
                    ),
                    onToggleItem = {}
                )
            }
        }
    }
}

@Preview(name = "Checklist Bubble Dark Mode", showBackground = true)
@Composable
private fun ChecklistBubbleDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface(color = JayraBubbleInDark) {
            Box(modifier = Modifier.padding(12.dp)) {
                ChecklistBubble(
                    checklist = Checklist(
                        id = "preview_cl_dark",
                        title = "Checklist Sprint Mingguan",
                        items = listOf(
                            ChecklistItem(id = "d1", text = "Sinkronisasi realtime daftar tugas", isDone = true),
                            ChecklistItem(id = "d2", text = "Verifikasi unit test Robolectric", isDone = false)
                        )
                    ),
                    onToggleItem = {}
                )
            }
        }
    }
}
