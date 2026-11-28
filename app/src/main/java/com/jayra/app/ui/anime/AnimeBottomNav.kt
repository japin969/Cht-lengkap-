package com.jayra.app.ui.anime

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AnimeBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            "anime_home" to Icons.Rounded.Home,
            "anime_search" to Icons.Rounded.Search,
            "anime_schedule" to Icons.Rounded.Schedule,
            "anime_settings" to Icons.Rounded.Settings
        )
        items.forEach { (route, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFE50914),
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
```