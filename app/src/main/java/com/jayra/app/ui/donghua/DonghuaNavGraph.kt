package com.jayra.app.ui.donghua

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
 * DonghuaFeature (Strem) — root fitur Donghua dengan NavGraph & BottomNav SENDIRI.
 * Terpisah total dari Comic & Anime.
 */
private val donghuaNavItems = listOf(
    MediaNavItem("donghua_home", "Home", Icons.Rounded.Home),
    MediaNavItem("donghua_search", "Cari", Icons.Rounded.Search),
    MediaNavItem("donghua_genre", "Genre", Icons.Rounded.Explore),
    MediaNavItem("donghua_bookmark", "Simpan", Icons.Rounded.Bookmark),
    MediaNavItem("donghua_history", "Riwayat", Icons.Rounded.History)
)

private val donghuaTopLevel = donghuaNavItems.map { it.route }.toSet()

@Composable
fun DonghuaFeature(
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
            if (currentRoute in donghuaTopLevel) {
                MediaBottomNav(
                    items = donghuaNavItems,
                    currentRoute = currentRoute,
                    accent = MediaColors.Strem,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo("donghua_home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            DonghuaNavGraph(navController = navController, modifier = modifier)
        }
    }
}

@Composable
fun DonghuaNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "donghua_home",
        modifier = modifier.fillMaxSize(),
        enterTransition = { androidx.compose.animation.EnterTransition.None },
        exitTransition = { androidx.compose.animation.ExitTransition.None },
        popEnterTransition = { androidx.compose.animation.EnterTransition.None },
        popExitTransition = { androidx.compose.animation.ExitTransition.None }
    ) {
        composable("donghua_home") {
            DonghuaHomeScreen(
                onDonghuaClick = { url -> navController.navigate("donghua_detail/$url") },
                onSearchClick = { navController.navigate("donghua_search") },
                onSeeAll = { navController.navigate("donghua_list/Semua Donghua") }
            )
        }
        composable("donghua_search") {
            DonghuaSearchScreen(
                onBack = { navController.popBackStack() },
                onDonghuaClick = { url -> navController.navigate("donghua_detail/$url") }
            )
        }
        composable("donghua_genre") {
            DonghuaGenreScreen(
                onBack = { navController.popBackStack() },
                onGenreClick = { genre -> navController.navigate("donghua_list/$genre") }
            )
        }
        composable("donghua_bookmark") {
            DonghuaBookmarkScreen(
                onBack = { navController.popBackStack() },
                onDonghuaClick = { url -> navController.navigate("donghua_detail/$url") },
                onBrowse = { navController.navigate("donghua_home") { popUpTo("donghua_home") } }
            )
        }
        composable("donghua_history") {
            DonghuaHistoryScreen(
                onBack = { navController.popBackStack() },
                onDonghuaClick = { url -> navController.navigate("donghua_detail/$url") },
                onBrowse = { navController.navigate("donghua_home") { popUpTo("donghua_home") } }
            )
        }
        composable(
            route = "donghua_list/{spec}",
            arguments = listOf(navArgument("spec") { type = NavType.StringType })
        ) { entry ->
            val spec = entry.arguments?.getString("spec") ?: "Semua Donghua"
            val genre = if (spec == "Semua Donghua") null else spec
            DonghuaListScreen(
                title = genre ?: "Semua Donghua",
                genre = genre,
                onBack = { navController.popBackStack() },
                onDonghuaClick = { url -> navController.navigate("donghua_detail/$url") }
            )
        }
        composable(
            route = "donghua_detail/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { entry ->
            val url = entry.arguments?.getString("url") ?: ""
            DonghuaDetailScreen(
                dramaUrl = url,
                onBack = { navController.popBackStack() },
                onPlayEpisode = { epUrl -> navController.navigate("donghua_player/$epUrl") }
            )
        }
        composable(
            route = "donghua_player/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { entry ->
            val url = entry.arguments?.getString("url") ?: ""
            DonghuaPlayerScreen(
                videoUrl = url,
                onBack = { navController.popBackStack() }
            )
        }
    }
}