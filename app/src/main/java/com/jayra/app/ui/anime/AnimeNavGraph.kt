package com.jayra.app.ui.anime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jayra.app.ui.media.MediaBottomNav
import com.jayra.app.ui.media.MediaColors
import com.jayra.app.ui.media.MediaNavItem

/**
 * AnimeFeature — root fitur Anime dengan NavGraph & BottomNav SENDIRI.
 * Terpisah total dari Comic & Strem. Bottom nav utama disembunyikan MainScreen.
 */
private val animeNavItems = listOf(
    MediaNavItem("anime_home", "Home", Icons.Rounded.Home),
    MediaNavItem("anime_search", "Cari", Icons.Rounded.Search),
    MediaNavItem("anime_genre", "Genre", Icons.Rounded.Explore),
    MediaNavItem("anime_bookmark", "My List", Icons.Rounded.Bookmark),
    MediaNavItem("anime_history", "Riwayat", Icons.Rounded.History)
)

private val animeTopLevel = animeNavItems.map { it.route }.toSet()

@Composable
fun AnimeFeature(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    BackHandler(enabled = true) {
        if (!navController.popBackStack()) onExit()
    }

    Scaffold(
        containerColor = MediaColors.Bg,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in animeTopLevel) {
                MediaBottomNav(
                    items = animeNavItems,
                    currentRoute = currentRoute,
                    accent = MediaColors.Anime,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo("anime_home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AnimeNavGraph(navController = navController, modifier = modifier)
        }
    }
}

@Composable
fun AnimeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "anime_home",
        modifier = modifier.fillMaxSize(),
        enterTransition = { androidx.compose.animation.EnterTransition.None },
        exitTransition = { androidx.compose.animation.ExitTransition.None },
        popEnterTransition = { androidx.compose.animation.EnterTransition.None },
        popExitTransition = { androidx.compose.animation.ExitTransition.None }
    ) {
        composable("anime_home") {
            AnimeHomeScreen(
                onAnimeClick = { id -> navController.navigate("anime_detail/$id") },
                onSearchClick = { navController.navigate("anime_search") },
                onSeeAll = { navController.navigate("anime_list/Semua Anime") },
                onScheduleClick = { navController.navigate("anime_schedule") },
            )
        }
        composable("anime_search") {
            AnimeSearchScreen(
                onBack = { navController.popBackStack() },
                onAnimeClick = { id -> navController.navigate("anime_detail/$id") }
            )
        }
        composable("anime_genre") {
            AnimeGenreScreen(
                onBack = { navController.popBackStack() },
                onGenreClick = { genre -> navController.navigate("anime_list/$genre") }
            )
        }
        composable("anime_bookmark") {
            AnimeBookmarkScreen(
                onBack = { navController.popBackStack() },
                onAnimeClick = { id -> navController.navigate("anime_detail/$id") },
                onBrowse = { navController.navigate("anime_home") { popUpTo("anime_home") } }
            )
        }
        composable("anime_history") {
            AnimeHistoryScreen(
                onBack = { navController.popBackStack() },
                onAnimeClick = { id -> navController.navigate("anime_detail/$id") },
                onBrowse = { navController.navigate("anime_home") { popUpTo("anime_home") } }
            )
        }
        composable(
            route = "anime_list/{spec}",
            arguments = listOf(navArgument("spec") { type = NavType.StringType })
        ) { entry ->
            val spec = entry.arguments?.getString("spec") ?: "Semua Anime"
            val genre = if (spec == "Semua Anime") null else spec
            AnimeListScreen(
                title = if (genre != null) genre else "Semua Anime",
                genre = genre,
                onBack = { navController.popBackStack() },
                onAnimeClick = { id -> navController.navigate("anime_detail/$id") }
            )
        }
        composable(
            route = "anime_detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: ""
            AnimeDetailScreen(
                animeUrl = id,
                onBack = { navController.popBackStack() },
                onPlayEpisode = { epUrl -> navController.navigate("anime_player/$epUrl") }
            )
        }
        composable(
            route = "anime_player/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { entry ->
            val url = entry.arguments?.getString("url") ?: ""
            AnimePlayerScreen(
                videoUrl = url,
                onBack = { navController.popBackStack() }
            )
        }

        composable("anime_schedule") {
            AnimeScheduleScreen(
                onBack = { navController.popBackStack() },
                onAnimeClick = { id -> navController.navigate("anime_detail/$id") }
            )
        }



    }
}