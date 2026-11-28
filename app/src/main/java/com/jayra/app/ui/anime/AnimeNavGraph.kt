package com.jayra.app.ui.anime

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text

@Composable
fun AnimeNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "anime_home") {
        composable("anime_home") { AnimeHomeScreen(onNavigate = { navController.navigate(it) { launchSingleTop = true } }) }
        composable("anime_search") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Anime Search") } }
        composable("anime_schedule") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Anime Schedule") } }
        composable("anime_settings") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Anime Settings") } }
        composable("anime_detail/{slug}") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Anime Detail") } }
        composable("anime_episode/{slug}") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Anime Episode") } }
    }
}