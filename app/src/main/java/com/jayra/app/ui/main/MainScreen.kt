@file:Suppress("DEPRECATION")

package com.jayra.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jayra.app.data.ChatRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.NavItem
import com.jayra.app.model.SearchResultType
import com.jayra.app.ui.about.AboutScreen
import com.jayra.app.ui.addmember.AddMemberScreen
import com.jayra.app.ui.assistant.AssistantScreen
import com.jayra.app.ui.auth.LoginScreen
import com.jayra.app.ui.auth.RegisterScreen
import com.jayra.app.ui.call.CallHistoryScreen
import com.jayra.app.ui.call.CallScreen
import com.jayra.app.ui.calls.CallsScreen
import com.jayra.app.ui.channelinfo.ChannelInfoScreen
import com.jayra.app.ui.chatlist.ChatListScreen
import com.jayra.app.ui.chatroom.ChatRoomScreen
import com.jayra.app.ui.checklist.CreateChecklistScreen
import com.jayra.app.ui.communities.CommunitiesScreen
import com.jayra.app.ui.contactinfo.ContactInfoScreen
import com.jayra.app.ui.contacts.ContactSharingScreen
import com.jayra.app.ui.contacts.ContactsScreen
import com.jayra.app.ui.forward.ForwardScreen
import com.jayra.app.ui.groupinfo.GroupInfoScreen
import com.jayra.app.ui.groupmembers.GroupMembersScreen
import com.jayra.app.ui.help.HelpScreen
import com.jayra.app.ui.help.PrivacyPolicyScreen
import com.jayra.app.ui.location.LocationSharingScreen
import com.jayra.app.ui.music.MusicHomeScreen
import com.jayra.app.ui.music.MusicPlayerScreen
import com.jayra.app.ui.music.MusicViewModel
import com.jayra.app.ui.poll.CreatePollScreen
import com.jayra.app.ui.profile.EditProfileScreen
import com.jayra.app.ui.profile.MyProfileScreen
import com.jayra.app.ui.saved.SavedMessagesScreen
import com.jayra.app.ui.search.GlobalSearchScreen
import com.jayra.app.ui.search.SearchScreen
import com.jayra.app.ui.security.ActiveSessionsScreen
import com.jayra.app.ui.security.PasscodeScreen
import com.jayra.app.ui.security.TwoStepVerificationScreen
import com.jayra.app.ui.settings.AdvancedSettingsScreen
import com.jayra.app.ui.settings.DataStorageScreen
import com.jayra.app.ui.settings.LanguageScreen
import com.jayra.app.ui.settings.NotificationsScreen
import com.jayra.app.ui.settings.PrivacyScreen
import com.jayra.app.ui.settings.ProxyScreen
import com.jayra.app.ui.settings.SettingsScreen
import com.jayra.app.ui.settings.StorageDetailScreen
import com.jayra.app.ui.settings.ThemeScreen
import com.jayra.app.ui.splash.SplashScreen
import com.jayra.app.ui.stickers.StickersScreen
import com.jayra.app.ui.story.StoryAlbumsScreen
import com.jayra.app.ui.story.StoryViewerScreen
import com.jayra.app.ui.voicetrimmer.VoiceTrimmerScreen
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraSeparatorDark
import com.jayra.app.ui.theme.JayraSeparatorLight
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(
    private val chatRepository: ChatRepository = AppModule.chatRepository
) : ViewModel() {

    private val baseTabs = listOf(
        NavItem(
            route = ROUTE_CHAT_LIST,
            label = "Chat",
            icon = Icons.Default.Chat
        ),
        NavItem(
            route = ROUTE_CALLS,
            label = "Panggilan",
            icon = Icons.Default.Call
        ),
        NavItem(
            route = ROUTE_MUSIC_HOME,
            label = "Musik",
            icon = Icons.Default.MusicNote
        ),
        NavItem(
            route = ROUTE_DRAMA_HOME,
            icon = Icons.Default.PlayArrow,
            label = "VidHub"
        ),
        NavItem(
            route = ROUTE_SETTINGS,
            label = "Pengaturan",
            icon = Icons.Default.Settings
        )
    )

    private val _navItems = MutableStateFlow(baseTabs)
    val navItems: StateFlow<List<NavItem>> = _navItems.asStateFlow()

    init {
        observeUnreadChats()
    }

    private fun observeUnreadChats() {
        viewModelScope.launch {
            chatRepository.getChats()
                .catch { emit(emptyList()) }
                .collect { chats ->
                    val totalUnread = chats.sumOf { it.unreadCount }
                    _navItems.value = baseTabs.map { item ->
                        if (item.route == ROUTE_CHAT_LIST) {
                            item.copy(badgeCount = totalUnread)
                        } else {
                            item
                        }
                    }
                }
        }
    }

    companion object {
        const val ROUTE_SPLASH = "splash"
        const val ROUTE_CHAT_LIST = "chat_list"
        const val ROUTE_CALLS = "calls"
        const val ROUTE_MUSIC_HOME = "music_home"
    const val ROUTE_DRAMA_HOME = "drama_home"
    const val ROUTE_DRAMA_DETAIL = "drama_detail?url={url}"
    const val ROUTE_KOMIK_DETAIL = "komik_detail?slug={slug}"
    const val ROUTE_KOMIK_READER = "komik_reader?slug={slug}"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_GLOBAL_SEARCH = "global_search"
        const val ROUTE_COMMUNITIES = "communities"
        const val ROUTE_STORY_ALBUMS = "story_albums"

        val BOTTOM_BAR_ROUTES = setOf(
            ROUTE_CHAT_LIST,
            ROUTE_CALLS,
            ROUTE_MUSIC_HOME,
            ROUTE_SETTINGS
        )
    }
}

@Composable
fun MainScreen(
    initialChatId: String? = null,
    initialCallParticipantId: String? = null,
    initialCallType: String = "voice",
    initialOpenGlobalSearch: Boolean = false,
    onNotificationNavigationHandled: () -> Unit = {},
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val sharedMusicViewModel: MusicViewModel = viewModel()
    val navItems by viewModel.navItems.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val firebaseAuth = remember {
        try {
            Firebase.auth
        } catch (_: Exception) {
            null
        }
    }
    var currentUser by remember { mutableStateOf(firebaseAuth?.currentUser) }

    DisposableEffect(firebaseAuth) {
        if (firebaseAuth == null) {
            onDispose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            firebaseAuth.addAuthStateListener(listener)
            onDispose {
                firebaseAuth.removeAuthStateListener(listener)
            }
        }
    }

    // 1.2 Navigasi otomatis dari Notifikasi (extra_chat_id, panggilan masuk, atau global search)
    LaunchedEffect(
        initialChatId,
        initialCallParticipantId,
        initialOpenGlobalSearch,
        currentUser,
        currentRoute
    ) {
        if (currentUser != null &&
            currentRoute != null &&
            currentRoute != MainViewModel.ROUTE_SPLASH
        ) {
            when {
                !initialChatId.isNullOrBlank() -> {
                    navController.navigate("chat/$initialChatId") {
                        launchSingleTop = true
                    }
                    onNotificationNavigationHandled()
                }
                !initialCallParticipantId.isNullOrBlank() -> {
                    navController.navigate("call/$initialCallParticipantId/$initialCallType") {
                        launchSingleTop = true
                    }
                    onNotificationNavigationHandled()
                }
                initialOpenGlobalSearch -> {
                    navController.navigate(MainViewModel.ROUTE_GLOBAL_SEARCH) {
                        launchSingleTop = true
                    }
                    onNotificationNavigationHandled()
                }
            }
        }
    }

    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val showBottomBar = currentRoute in MainViewModel.BOTTOM_BAR_ROUTES && currentUser != null

    Scaffold(
        contentWindowInsets = if (showBottomBar) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
        containerColor = screenBg,
        bottomBar = {
            if (showBottomBar) {
                JayraBottomNavigationBar(
                    items = navItems,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(MainViewModel.ROUTE_CHAT_LIST) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        MainNavigationGraph(
            navController = navController,
            currentUser = currentUser,
            sharedMusicViewModel = sharedMusicViewModel,
            modifier = if (showBottomBar) {
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            } else {
                Modifier.fillMaxSize()
            }
        )
    }
}

@Composable
fun JayraBottomNavigationBar(
    items: List<NavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val barBgColor = if (isDark) JayraCardDark else JayraCardLight
    val dividerColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Surface(
        color = barBgColor,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("jayra_bottom_navigation_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor
            )
            NavigationBar(
                containerColor = barBgColor,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val animatedTint by animateColorAsState(
                        targetValue = if (isSelected) JayraBlue else JayraSubtext,
                        animationSpec = tween(durationMillis = 200),
                        label = "nav_item_tint_${item.route}"
                    )
                    val animatedIndicatorBg by animateColorAsState(
                        targetValue = if (isSelected) {
                            JayraBlue.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = tween(durationMillis = 200),
                        label = "nav_item_indicator_${item.route}"
                    )

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onItemClick(item.route) },
                        alwaysShowLabel = true,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("bottom_nav_item_${item.route}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = JayraBlue,
                            unselectedIconColor = JayraSubtext,
                            selectedTextColor = JayraBlue,
                            unselectedTextColor = JayraSubtext,
                            indicatorColor = Color.Transparent
                        ),
                        icon = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(animatedIndicatorBg)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.badgeCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = JayraDangerRed,
                                                contentColor = Color(0xFFFFFFFF)
                                            ) {
                                                Text(
                                                    text = if (item.badgeCount > 99) {
                                                        "99+"
                                                    } else {
                                                        item.badgeCount.toString()
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    ) {
                                        // GANTI SVG TELEGRAM
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = animatedTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    // GANTI SVG TELEGRAM
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = animatedTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = animatedTint
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainNavigationGraph(
    navController: NavHostController,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    sharedMusicViewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    fun navigateToInfo(targetId: String) {
        when (targetId) {
            "chat_2", "chat_5" -> navController.navigate("group/$targetId")
            "chat_4", "durov" -> navController.navigate("channel/$targetId")
            else -> navController.navigate("contact/$targetId")
        }
    }

    fun navigateBackToChatsTab() {
        navController.navigate(MainViewModel.ROUTE_CHAT_LIST) {
            popUpTo(MainViewModel.ROUTE_CHAT_LIST) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = MainViewModel.ROUTE_SPLASH,
        modifier = modifier
    ) {
        composable(MainViewModel.ROUTE_SPLASH) {
            SplashScreen(
                onNavigateToChatList = {
                    navController.navigate(MainViewModel.ROUTE_CHAT_LIST) {
                        popUpTo(MainViewModel.ROUTE_SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo(MainViewModel.ROUTE_SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = {
                    navController.navigate(MainViewModel.ROUTE_CHAT_LIST) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(MainViewModel.ROUTE_CHAT_LIST) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable(MainViewModel.ROUTE_CHAT_LIST) {
            if (currentUser == null) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(MainViewModel.ROUTE_CHAT_LIST) { inclusive = true }
                    }
                }
            } else {
                ChatListScreen(
                    onChatClick = { chatId ->
                        navController.navigate("chat/$chatId")
                    },
                    onContactInfoClick = { targetId ->
                        navigateToInfo(targetId)
                    },
                    onMyProfileClick = {
                        navController.navigate(MainViewModel.ROUTE_SETTINGS) {
                            popUpTo(MainViewModel.ROUTE_CHAT_LIST) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCallsClick = {
                        navController.navigate(MainViewModel.ROUTE_CALLS) {
                            popUpTo(MainViewModel.ROUTE_CHAT_LIST) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onContactsClick = {
                        navController.navigate("contacts")
                    },
                    onSearchClick = {
                        navController.navigate(MainViewModel.ROUTE_GLOBAL_SEARCH)
                    },
                    onSavedMessagesClick = {
                        navController.navigate("saved_messages")
                    },
                    onCommunitiesClick = {
                        navController.navigate(MainViewModel.ROUTE_COMMUNITIES)
                    },
                    onStoriesClick = {
                        navController.navigate(MainViewModel.ROUTE_STORY_ALBUMS)
                    }
                )
            }
        }

        composable(MainViewModel.ROUTE_CALLS) {
            CallsScreen(
                onBackClick = { navigateBackToChatsTab() },
                onNavigateToContactInfo = { targetId ->
                    navigateToInfo(targetId)
                }
            )
        }

        composable(MainViewModel.ROUTE_MUSIC_HOME) {
            MusicHomeScreen(
                onBack = null,
                onSongClick = {
                    navController.navigate("music_player")
                },
                viewModel = sharedMusicViewModel
            )
        }

        composable("music_player") {
            MusicPlayerScreen(
                onBack = { navController.popBackStack() },
                viewModel = sharedMusicViewModel
            )
        }

        composable(MainViewModel.ROUTE_DRAMA_HOME) {
            com.jayra.app.ui.VidHubScreen(
                onDramaClick = { url ->
                    val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                    navController.navigate("drama_detail?url=$encoded")
                },
                onAnimeClick = { url ->
                    val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                    navController.navigate("anime_detail?url=$encoded")
                },
                onKomikClick = { slug ->
                    navController.navigate("komik_detail?slug=$slug")
                }
            )
        }

        composable(
            route = "anime_detail?url={url}",
            arguments = listOf(androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val decoded = java.net.URLDecoder.decode(url, "UTF-8")
            com.jayra.app.ui.anime.AnimeDetailScreen(
                animeUrl = decoded,
                onPlayEpisode = { epUrl ->
                    val epEncoded = java.net.URLEncoder.encode(epUrl, "UTF-8")
                    navController.navigate("anime_player?url=$epEncoded")
                }
            )
        }

        composable(
            route = "anime_player?url={url}",
            arguments = listOf(androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val decoded = java.net.URLDecoder.decode(url, "UTF-8")
            val vm: com.jayra.app.ui.anime.AnimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val state by vm.uiState.collectAsState()
            LaunchedEffect(decoded) { vm.loadStream(decoded) }
            val stream = state.streamUrl
            if (stream.isNullOrBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                com.jayra.app.ui.anime.AnimePlayerScreen(videoUrl = stream)
            }
        }

        composable(
            route = MainViewModel.ROUTE_DRAMA_DETAIL,
            arguments = listOf(androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            com.jayra.app.ui.drama.DramaDetailScreen(dramaUrl = java.net.URLDecoder.decode(url, "UTF-8"))
        }

        
        composable(
            route = MainViewModel.ROUTE_KOMIK_DETAIL,
            arguments = listOf(androidx.navigation.navArgument("slug") { type = androidx.navigation.NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            com.jayra.app.ui.komik.KomikDetailScreen(
                slug = slug,
                onChapterClick = { chapterSlug ->
                    navController.navigate("komik_reader?slug=$chapterSlug")
                }
            )
        }

        composable(
            route = MainViewModel.ROUTE_KOMIK_READER,
            arguments = listOf(androidx.navigation.navArgument("slug") { type = androidx.navigation.NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            com.jayra.app.ui.komik.KomikReaderScreen(slug = slug)
        }

        composable(MainViewModel.ROUTE_SETTINGS) {
            SettingsScreen(
                onBack = { navigateBackToChatsTab() },
                onProfileClick = { navController.navigate("my_profile") },
                onPrivacyClick = { navController.navigate("privacy") },
                onNotificationsClick = { navController.navigate("notifications") },
                onStorageClick = { navController.navigate("data_storage") },
                onAssistantClick = { navController.navigate("assistant") },
                onThemeClick = { navController.navigate("theme") },
                onLanguageClick = { navController.navigate("language") },
                onStickersClick = { navController.navigate("stickers") },
                onHelpClick = { navController.navigate("help") },
                onPrivacyPolicyClick = { navController.navigate("privacy_policy") },
                onAboutClick = { navController.navigate("about") },
                onSavedMessagesClick = { navController.navigate("saved_messages") },
                onCommunitiesClick = { navController.navigate(MainViewModel.ROUTE_COMMUNITIES) },
                onStoriesClick = { navController.navigate(MainViewModel.ROUTE_STORY_ALBUMS) }
            )
        }

        composable(MainViewModel.ROUTE_COMMUNITIES) {
            CommunitiesScreen(
                onBackClick = { navController.popBackStack() },
                onGroupClick = { groupId ->
                    navController.navigate("chat/$groupId")
                }
            )
        }

        composable(MainViewModel.ROUTE_STORY_ALBUMS) {
            StoryAlbumsScreen(
                onBackClick = { navController.popBackStack() },
                onStoryClick = { storyId ->
                    navController.navigate("story_viewer/$storyId")
                }
            )
        }

        composable(
            route = "story_viewer/{initialStoryId}",
            arguments = listOf(navArgument("initialStoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val initialStoryId = backStackEntry.arguments?.getString("initialStoryId") ?: ""
            StoryViewerScreen(
                initialStoryId = initialStoryId,
                onClose = { navController.popBackStack() }
            )
        }

        composable(MainViewModel.ROUTE_GLOBAL_SEARCH) {
            val storageCategories = setOf("photo", "video", "document", "music", "other")
            GlobalSearchScreen(
                onBackClick = { navController.popBackStack() },
                onResultClick = { type, targetId ->
                    when (type) {
                        SearchResultType.CHANNEL -> navController.navigate("channel/$targetId")
                        SearchResultType.GROUP -> navController.navigate("group/$targetId")
                        SearchResultType.CONTACT -> navController.navigate("contact/$targetId")
                        SearchResultType.CHAT,
                        SearchResultType.MESSAGE -> navController.navigate("chat/$targetId")
                        SearchResultType.FILE -> {
                            if (targetId in storageCategories) {
                                navController.navigate("storage_detail/$targetId")
                            } else {
                                navController.navigate("chat/$targetId")
                            }
                        }
                    }
                }
            )
        }

        composable("search") {
            val storageCategories = setOf("photo", "video", "document", "music", "other")
            SearchScreen(
                onBack = { navController.popBackStack() },
                onNavigateToResult = { result ->
                    when (result.type) {
                        SearchResultType.CHANNEL -> navController.navigate("channel/${result.targetId}")
                        SearchResultType.GROUP -> navController.navigate("group/${result.targetId}")
                        SearchResultType.CONTACT -> navController.navigate("contact/${result.targetId}")
                        SearchResultType.CHAT, SearchResultType.MESSAGE -> navController.navigate("chat/${result.targetId}")
                        SearchResultType.FILE -> {
                            if (result.targetId in storageCategories) {
                                navController.navigate("storage_detail/${result.targetId}")
                            } else {
                                navController.navigate("chat/${result.targetId}")
                            }
                        }
                    }
                }
            )
        }

        composable("contacts") {
            ContactsScreen(
                onBackClick = { navController.popBackStack() },
                onContactClick = { targetId ->
                    navController.navigate("chat/$targetId")
                }
            )
        }

        composable(
            route = "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: "chat_1"
            ChatRoomScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() },
                onContactInfoClick = { cId -> navigateToInfo(cId) },
                onNavigateToCreatePoll = { cId -> navController.navigate("create_poll/$cId") },
                onNavigateToCreateChecklist = { cId -> navController.navigate("create_checklist/$cId") },
                onNavigateToVoiceTrimmer = { cId -> navController.navigate("voice_trimmer/$cId") },
                onNavigateToLocation = { cId -> navController.navigate("location_sharing/$cId") },
                onNavigateToContact = { cId -> navController.navigate("contact_sharing/$cId") },
                onForwardMessage = { srcChatId, msgId ->
                    navController.navigate("forward/$srcChatId/$msgId")
                }
            )
        }

        composable(
            route = "contact/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: "chat_1"
            ContactInfoScreen(
                contactId = contactId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "group/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: "chat_2"
            GroupInfoScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onMembersClick = { gId -> navController.navigate("group_members/$gId") }
            )
        }

        composable(
            route = "group_members/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: "chat_2"
            GroupMembersScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onMemberClick = { memberId -> navController.navigate("chat/$memberId") },
                onAddMemberClick = { gId -> navController.navigate("add_member/$gId") }
            )
        }

        composable(
            route = "add_member/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: "chat_2"
            AddMemberScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onMemberAdded = { navController.popBackStack() }
            )
        }

        composable(
            route = "channel/{channelId}",
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: "chat_4"
            ChannelInfoScreen(
                channelId = channelId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("advanced_settings") {
            AdvancedSettingsScreen(
                onBack = { navController.popBackStack() },
                onProxyClick = { navController.navigate("proxy") }
            )
        }

        composable("data_storage") {
            DataStorageScreen(
                onBack = { navController.popBackStack() },
                onCategoryClick = { category -> navController.navigate("storage_detail/$category") }
            )
        }

        composable("proxy") {
            ProxyScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "storage_detail/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "photo"
            StorageDetailScreen(
                category = category,
                onBack = { navController.popBackStack() }
            )
        }

        composable("assistant") {
            AssistantScreen(onBack = { navController.popBackStack() })
        }

        composable("my_profile") {
            MyProfileScreen(
                onBack = { navController.popBackStack() },
                onEditProfileClick = { navController.navigate("edit_profile") },
                onPrivacyClick = { navController.navigate("privacy") }
            )
        }

        composable("edit_profile") {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }

        composable("privacy") {
            PrivacyScreen(
                onBack = { navController.popBackStack() },
                onTwoStepClick = { navController.navigate("two_step_verification") },
                onActiveDevicesClick = { navController.navigate("active_sessions") },
                onPasscodeClick = { navController.navigate("passcode") }
            )
        }

        composable("two_step_verification") {
            TwoStepVerificationScreen(onBack = { navController.popBackStack() })
        }

        composable("active_sessions") {
            ActiveSessionsScreen(onBack = { navController.popBackStack() })
        }

        composable("passcode") {
            PasscodeScreen(
                onBack = { navController.popBackStack() },
                onSuccessUnlock = { navController.popBackStack() }
            )
        }

        composable("notifications") {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "call/{participantId}/{type}",
            arguments = listOf(
                navArgument("participantId") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val participantId = backStackEntry.arguments?.getString("participantId") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: "voice"
            CallScreen(
                participantId = participantId,
                isVideo = (type == "video"),
                onBack = { navController.popBackStack() }
            )
        }

        composable("call_history") {
            CallHistoryScreen(
                onBack = { navController.popBackStack() },
                onCallClick = { participantId, type ->
                    navController.navigate("call/$participantId/$type")
                }
            )
        }

        composable("help") {
            HelpScreen(
                onBack = { navController.popBackStack() },
                onPrivacyClick = { navController.navigate("privacy_policy") }
            )
        }

        composable("privacy_policy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable("about") {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onPrivacyClick = { navController.navigate("privacy_policy") }
            )
        }

        composable("stickers") {
            StickersScreen(onBack = { navController.popBackStack() })
        }

        composable("theme") {
            ThemeScreen(onBack = { navController.popBackStack() })
        }

        composable("language") {
            LanguageScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "create_poll/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: "chat_1"
            CreatePollScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "create_checklist/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: "chat_1"
            CreateChecklistScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "voice_trimmer/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: "chat_1"
            VoiceTrimmerScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "location_sharing/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: "chat_1"
            LocationSharingScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "contact_sharing/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: "chat_1"
            ContactSharingScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("saved_messages") {
            SavedMessagesScreen(
                onBack = { navController.popBackStack() },
                onForwardMessage = { srcChatId, msgId ->
                    navController.navigate("forward/$srcChatId/$msgId")
                },
                onNavigateToCreatePoll = { cId -> navController.navigate("create_poll/$cId") },
                onNavigateToCreateChecklist = { cId -> navController.navigate("create_checklist/$cId") },
                onNavigateToVoiceTrimmer = { cId -> navController.navigate("voice_trimmer/$cId") },
                onNavigateToLocation = { cId -> navController.navigate("location_sharing/$cId") },
                onNavigateToContact = { cId -> navController.navigate("contact_sharing/$cId") }
            )
        }

        composable(
            route = "forward/{sourceChatId}/{messageId}",
            arguments = listOf(
                navArgument("sourceChatId") { type = NavType.StringType },
                navArgument("messageId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sourceChatId = backStackEntry.arguments?.getString("sourceChatId") ?: "chat_1"
            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
            ForwardScreen(
                sourceChatId = sourceChatId,
                messageId = messageId,
                onBack = { navController.popBackStack() },
                onForwardCompleted = { singleTargetChatId ->
                    val currentUid = currentUser?.uid ?: AppModule.chatRepository.currentUserId
                    when {
                        singleTargetChatId == null -> {
                            navController.popBackStack()
                        }
                        singleTargetChatId == currentUid -> {
                            navController.navigate("saved_messages") {
                                popUpTo("forward/{sourceChatId}/{messageId}") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        else -> {
                            navController.navigate("chat/$singleTargetChatId") {
                                popUpTo("forward/{sourceChatId}/{messageId}") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MainScreenPreviewContent(
    selectedRoute: String,
    unreadCount: Int
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val previewTabs = listOf(
        NavItem("chat_list", "Chat", Icons.Default.Chat, badgeCount = unreadCount),
        NavItem("calls", "Panggilan", Icons.Default.Call),
        NavItem("music_home", "Musik", Icons.Default.MusicNote),
        NavItem("settings", "Pengaturan", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = screenBg,
        bottomBar = {
            JayraBottomNavigationBar(
                items = previewTabs,
                currentRoute = selectedRoute,
                onItemClick = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Jayra",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tab aktif: $selectedRoute",
                fontSize = 14.sp,
                color = JayraSubtext
            )
        }
    }
}

@Preview(name = "Main Screen BottomNav Light", showBackground = true)
@Composable
private fun MainScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            MainScreenPreviewContent(
                selectedRoute = "chat_list",
                unreadCount = 3
            )
        }
    }
}

@Preview(name = "Main Screen BottomNav Dark", showBackground = true)
@Composable
private fun MainScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            MainScreenPreviewContent(
                selectedRoute = "music_home",
                unreadCount = 12
            )
        }
    }
}
