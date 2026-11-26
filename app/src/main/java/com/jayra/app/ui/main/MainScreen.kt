@file:Suppress("DEPRECATION")

package com.jayra.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.jayra.app.util.glassSurfaceV2
import com.jayra.app.ui.theme.LiquidGlassSpecV2
import com.jayra.app.util.GlassEffectPrefs
import com.jayra.app.ui.theme.IOSSecondarySystemBackground
import com.jayra.app.ui.theme.IOSSecondarySystemBackgroundDark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.jayra.app.ui.theme.LottieIcon
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
            icon = Icons.Rounded.Chat,
            lottieAsset = "message_circle"
        ),
        NavItem(
            route = ROUTE_CALLS,
            label = "Panggilan",
            icon = Icons.Rounded.Call,
            lottieAsset = "phone"
        ),
        NavItem(
            route = ROUTE_MUSIC_HOME,
            label = "Musik",
            icon = Icons.Rounded.MusicNote
        ),
        NavItem(
            route = ROUTE_ANIME_HOME,
            label = "Anime",
            icon = Icons.Rounded.Movie
        ),
        NavItem(
            route = ROUTE_DRAMA_HOME,
            label = "Drama",
            icon = Icons.Rounded.Tv
        ),
        NavItem(
            route = ROUTE_KOMIK_HOME,
            label = "Komik",
            icon = Icons.Rounded.MenuBook
        ),
        NavItem(
            route = ROUTE_NEKO_HOME,
            label = "Neko",
            icon = Icons.Rounded.Pets
        ),
        NavItem(
            route = ROUTE_SETTINGS,
            label = "Pengaturan",
            icon = Icons.Rounded.Settings,
            lottieAsset = "settings"
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
    const val ROUTE_ANIME_HOME = "anime_home"
    const val ROUTE_KOMIK_HOME = "komik_home"
    const val ROUTE_NEKO_HOME = "neko_home"
    const val ROUTE_NEKO_DETAIL = "neko_detail?url={url}"
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
            ROUTE_ANIME_HOME,
            ROUTE_DRAMA_HOME,
            ROUTE_KOMIK_HOME,
            ROUTE_NEKO_HOME,
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
        bottomBar = { }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().background(screenBg)) {
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
            if (showBottomBar) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    LiquidNavBarRow(
                        selectedIndex = when (currentRoute) {
                            "chat_list" -> 0
                            "calls" -> 1
                            "music_home" -> 2
                            "drama_home", "vidhub" -> 3
                            "settings" -> 4
                            else -> 0
                        },
                        onSelect = { index ->
                            val route = when (index) {
                                0 -> "chat_list"
                                1 -> "calls"
                                2 -> "music_home"
                                3 -> "drama_home"
                                4 -> "settings"
                                else -> "chat_list"
                            }
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
        }
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
    val barBgColor = if (isDark) androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.9f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
    val dividerColor = if (isDark) JayraSeparatorDark else JayraSeparatorLight

    Surface(
        color = barBgColor,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f))
            .navigationBarsPadding()
            .testTag("jayra_bottom_navigation_bar")
    ) {
        val navBgDark = isSystemInDarkTheme()
        val navBgTier = remember(GlassEffectPrefs.enabledState.value) { GlassEffectPrefs.resolveTier() }
        val navBgSpec = remember(navBgTier, navBgDark) { LiquidGlassSpecV2.forTier(navBgTier, navBgDark) }
        val navBgSolid = if (navBgDark) IOSSecondarySystemBackgroundDark else IOSSecondarySystemBackground
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor
            )
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clipToBounds()
                    .glassSurfaceV2(navBgSpec.surface, solidColor = navBgSolid, cornerRadius = 0.dp)
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
                            if (item.lottieAsset != null) {
                                LottieIcon(
                                    assetName = item.lottieAsset,
                                    size = 26.dp
                                )
                            } else {
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
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = animatedTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = animatedTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
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
        when {
            // Grup (termasuk grup baru hasil Create Group dengan id "group_<uuid>")
            targetId.startsWith("group_") || targetId == "chat_2" || targetId == "chat_5" ->
                navController.navigate("group/$targetId")
            // Channel
            targetId.startsWith("channel_") || targetId == "chat_4" || targetId == "durov" ->
                navController.navigate("channel/$targetId")
            // Sisanya = chat pribadi → info profil orang
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
        sizeTransform = { null },
        modifier = modifier.background(
            if (isSystemInDarkTheme()) JayraDarkChatBg else JayraLightChatBg
        ),
        enterTransition = {
            if (initialState.destination.route in MainViewModel.BOTTOM_BAR_ROUTES &&
                targetState.destination.route in MainViewModel.BOTTOM_BAR_ROUTES
            ) {
                androidx.compose.animation.EnterTransition.None
            } else {
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> fullWidth / 4 }
                ) + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
            }
        },
        exitTransition = {
            if (initialState.destination.route in MainViewModel.BOTTOM_BAR_ROUTES &&
                targetState.destination.route in MainViewModel.BOTTOM_BAR_ROUTES
            ) {
                androidx.compose.animation.ExitTransition.None
            } else {
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    targetOffsetX = { fullWidth -> -fullWidth / 4 }
                )
            }
        },
        popEnterTransition = {
            if (initialState.destination.route in MainViewModel.BOTTOM_BAR_ROUTES &&
                targetState.destination.route in MainViewModel.BOTTOM_BAR_ROUTES
            ) {
                androidx.compose.animation.EnterTransition.None
            } else {
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> -fullWidth / 4 }
                ) + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
            }
        },
        popExitTransition = {
            if (initialState.destination.route in MainViewModel.BOTTOM_BAR_ROUTES &&
                targetState.destination.route in MainViewModel.BOTTOM_BAR_ROUTES
            ) {
                androidx.compose.animation.ExitTransition.None
            } else {
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    targetOffsetX = { fullWidth -> fullWidth / 4 }
                )
            }
        }
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
                        popUpTo(0) { inclusive = true }
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
                        popUpTo(0) { inclusive = true }
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
                    onArchivedClick = { navController.navigate("archived_chats") },
                    onChatClick = { chatId ->
                        if (chatId.isNotBlank()) {
                            navController.navigate("chat/$chatId") {
                                launchSingleTop = true
                            }
                        }
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
                    onCreateGroupClick = {
                        navController.navigate("creategroup")
                    },
                    onCommunitiesClick = {
                        navController.navigate(MainViewModel.ROUTE_COMMUNITIES)
                    },
                    onStoriesClick = {
                        navController.navigate(MainViewModel.ROUTE_STORY_ALBUMS)
                    },
                    onAiClick = { navController.navigate("assistant") }
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

        composable("anime_home") {
            com.jayra.app.ui.anime.AnimeHomeScreen(
                onAnimeClick = { anime ->
                    navController.navigate("anime_detail/${anime.id ?: anime.link ?: anime.title ?: ""}")
                }
            )
        }

        composable("drama_home") {
            com.jayra.app.ui.drama.DramaScreen(
                onDramaClick = { url ->
                    val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                    navController.navigate("drama_detail?url=$encoded")
                }
            )
        }

        composable("komik_home") {
            com.jayra.app.ui.komik.KomikHomeScreen(
                onKomikClick = { komik ->
                    navController.navigate("komik_detail/${komik.slug}")
                }
            )
        }

        composable("neko_home") {
            com.jayra.app.ui.neko.NekoHomeScreen(
                onNekoClick = { neko ->
                    navController.navigate("neko_detail/${neko.link ?: neko.title ?: ""}")
                }
            )
        }

        // Old VidHubScreen route removed - replaced with separate feature routes
