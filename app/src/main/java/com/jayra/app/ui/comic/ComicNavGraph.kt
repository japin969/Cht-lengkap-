package com.jayra.app.ui.comic

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
 * ComicFeature — root fitur Comic dengan NavGraph & BottomNav SENDIRI.
 * Terpisah total dari Anime & Strem. Saat fitur ini aktif, bottom nav utama
 * (Chat/Calls/Music/Hub/Settings) disembunyikan oleh MainScreen.
 *
 * Back: jika masih di dalam grafik comic → pop; jika sudah di root → onExit (balik ke Hub).
 */
private val comicNavItems = listOf(
    MediaNavItem("comic_home", "Home", Icons.Rounded.Home),
    MediaNavItem("comic_search", "Cari", Icons.Rounded.Search),
    MediaNavItem("comic_genre", "Genre", Icons.Rounded.Explore),
    MediaNavItem("comic_bookmark", "Simpan", Icons.Rounded.Bookmark),
    MediaNavItem("comic_history", "Riwayat", Icons.Rounded.History)
)

private val comicTopLevel = comicNavItems.map { it.route }.toSet()

@Composable
fun ComicFeature(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Back: pop internal dulu, kalau di root → keluar ke Hub
    BackHandler(enabled = true) {
        if (!navController.popBackStack()) onExit()
    }

    Scaffold(
        containerColor = MediaColors.Bg,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in comicTopLevel) {
                MediaBottomNav(
                    items = comicNavItems,
                    currentRoute = currentRoute,
                    accent = MediaColors.Comic,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo("comic_home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            ComicNavGraph(navController = navController, modifier = modifier)
        }
    }
}

@Composable
fun ComicNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "comic_home",
        modifier = modifier.fillMaxSize(),
        enterTransition = { androidx.compose.animation.EnterTransition.None },
        exitTransition = { androidx.compose.animation.ExitTransition.None },
        popEnterTransition = { androidx.compose.animation.EnterTransition.None },
        popExitTransition = { androidx.compose.animation.ExitTransition.None }
    ) {
        composable("comic_home") {
            ComicHomeScreen(
                onComicClick = { slug -> navController.navigate("comic_detail/$slug") },
                onSearchClick = { navController.navigate("comic_search") },
                onSeeAll = { navController.navigate("comic_list/Semua Komik") },
                onSeeAllBacakomik = { navController.navigate("comic_list/BacaKomik") },
                onSeeAllMaid = { navController.navigate("comic_list/Maid") },
                onBkClick = { slug -> navController.navigate("comic_bk_detail/$slug") },
                onMaidClick = { slug -> navController.navigate("comic_maid_detail/$slug") }
            )
        }
        composable("comic_search") {
            ComicSearchScreen(
                onBack = { navController.popBackStack() },
                onComicClick = { slug -> navController.navigate("comic_detail/$slug") }
            )
        }
        composable("comic_genre") {
            ComicGenreScreen(
                onBack = { navController.popBackStack() },
                onGenreClick = { tagId -> navController.navigate("comic_list/Genre:$tagId") }
            )
        }
        composable("comic_bookmark") {
            ComicBookmarkScreen(
                onBack = { navController.popBackStack() },
                onComicClick = { slug -> navController.navigate("comic_detail/$slug") },
                onBrowse = { navController.navigate("comic_home") { popUpTo("comic_home") } }
            )
        }
        composable("comic_history") {
            ComicHistoryScreen(
                onBack = { navController.popBackStack() },
                onComicClick = { slug -> navController.navigate("comic_detail/$slug") },
                onBrowse = { navController.navigate("comic_home") { popUpTo("comic_home") } }
            )
        }
        composable(
            route = "comic_list/{spec}",
            arguments = listOf(navArgument("spec") { type = NavType.StringType })
        ) { entry ->
            val spec = entry.arguments?.getString("spec") ?: "Semua Komik"
            val tagId = when {
                spec == "Semua Komik" -> null
                spec.startsWith("Genre:") -> spec.removePrefix("Genre:")
                else -> spec
            }
            val title = when (spec) {
                "Semua Komik" -> "Semua Komik"
                "BacaKomik" -> "BacaKomik"
                "Maid" -> "Maid"
                else -> "Genre"
            }
            ComicListScreen(
                title = title,
                tagId = tagId,
                onBack = { navController.popBackStack() },
                onComicClick = { slug -> navController.navigate("comic_detail/$slug") }
            )
        }
        composable(
            route = "comic_detail/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { entry ->
            val slug = entry.arguments?.getString("slug") ?: ""
            ComicDetailScreen(
                slug = slug,
                onBack = { navController.popBackStack() },
                onChapterClick = { chapterSlug -> navController.navigate("comic_reader/$chapterSlug") }
            )
        }
        composable(
            route = "comic_reader/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { entry ->
            val slug = entry.arguments?.getString("slug") ?: ""
            ComicReaderScreen(
                slug = slug,
                onBack = { navController.popBackStack() }
            )
        }

        // ── BACA KOMIK ──
        composable(
            route = "comic_bk_detail/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { entry ->
            val slug = entry.arguments?.getString("slug") ?: ""
            ComicDetailScreen(
                slug = slug,
                onBack = { navController.popBackStack() },
                onChapterClick = { ch -> navController.navigate("comic_bk_reader/$ch") },
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                mode = ComicDetailMode.BK
            )
        }
        composable(
            route = "comic_bk_reader/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { entry ->
            val slug = entry.arguments?.getString("slug") ?: ""
            ComicReaderScreen(
                slug = slug,
                onBack = { navController.popBackStack() },
                mode = ComicReaderMode.BK
            )
        }

        // ── MAID ──
        composable(
            route = "comic_maid_detail/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { entry ->
            val slug = entry.arguments?.getString("slug") ?: ""
            ComicDetailScreen(
                slug = slug,
                onBack = { navController.popBackStack() },
                onChapterClick = { ch -> navController.navigate("comic_maid_reader/$ch") },
                mode = ComicDetailMode.MAID
            )
        }
        composable(
            route = "comic_maid_reader/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { entry ->
            val slug = entry.arguments?.getString("slug") ?: ""
            ComicReaderScreen(
                slug = slug,
                onBack = { navController.popBackStack() },
                mode = ComicReaderMode.MAID
            )
        }
    }
}