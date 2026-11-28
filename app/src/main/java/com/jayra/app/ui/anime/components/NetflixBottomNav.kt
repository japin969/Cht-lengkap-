package com.jayra.app.ui.anime.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayra.app.ui.theme.SFProFontFamily

@Composable
fun NetflixBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(bottom = 8.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf(
            "anime_home" to Icons.Rounded.Home,
            "anime_search" to Icons.Rounded.Search,
            "anime_schedule" to Icons.Rounded.CalendarToday,
            "anime_settings" to Icons.Rounded.Settings
        )
        val labels = mapOf(
            "anime_home" to "Home",
            "anime_search" to "Search",
            "anime_schedule" to "Schedule",
            "anime_settings" to "Settings"
        )
        items.forEach { (route, icon) ->
            val active = currentRoute == route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickableNoRipple { onNavigate(route) }
            ) {
                Icon(icon, contentDescription = route, tint = if (active) Color.White else Color.Gray, modifier = Modifier.size(24.dp))
                Text(
                    text = labels[route] ?: "",
                    fontFamily = SFProFontFamily,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) Color.White else Color.Gray
                )
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(
        indication = null,
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()
    ) { onClick() })