package com.jayra.app.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.theme.SFProFontFamily

data class MediaNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Bottom nav generik untuk fitur media. Setiap fitur (comic/anime/strem) membuat
 * instance-nya sendiri dengan item & accent sendiri — jadi bottom nav terpisah,
 * tidak menumpuk, dan hilang saat pindah fitur.
 */
@Composable
fun MediaBottomNav(
    items: List<MediaNavItem>,
    currentRoute: String?,
    accent: Color,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MediaColors.Surface)
            .navigationBarsPadding()
            .height(MediaDimens.BottomBarHeight)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            val tint = if (selected) accent else MediaColors.TextTertiary
            Column(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { if (!selected) onSelect(item.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                    item.label,
                    fontFamily = SFProFontFamily,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 10.sp,
                    color = tint,
                    lineHeight = 10.sp
                )
            }
        }
    }
}