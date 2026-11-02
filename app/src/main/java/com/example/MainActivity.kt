package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.SearchResultType
import com.example.ui.addmember.AddMemberScreen
import com.example.ui.calls.CallsScreen
import com.example.ui.channelinfo.ChannelInfoScreen
import com.example.ui.chatlist.ChatListScreen
import com.example.ui.chatroom.ChatRoomScreen
import com.example.ui.contactinfo.ContactInfoScreen
import com.example.ui.contacts.ContactsScreen
import com.example.ui.groupinfo.GroupInfoScreen
import com.example.ui.groupmembers.GroupMembersScreen
import com.example.ui.profile.EditProfileScreen
import com.example.ui.profile.MyProfileScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.NotificationsScreen
import com.example.ui.settings.PrivacyScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.TelegramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mandatory Edge-to-Edge display
        enableEdgeToEdge()

        // Section 5.3 Screen Security: Prevent screenshots and recents screen sniffing
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            TelegramTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TelegramAppNavigation()
                }
            }
        }
    }
}

@Composable
fun TelegramAppNavigation() {
    val navController = rememberNavController()

    fun navigateToInfo(targetId: String) {
        when (targetId) {
            "chat_2", "chat_5" -> navController.navigate("group/$targetId")
            "chat_4", "durov" -> navController.navigate("channel/$targetId")
            else -> navController.navigate("contact/$targetId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "chat_list"
    ) {
        composable("chat_list") {
            ChatListScreen(
                onChatClick = { chatId ->
                    navController.navigate("chat/$chatId")
                },
                onContactInfoClick = { targetId ->
                    navigateToInfo(targetId)
                },
                onMyProfileClick = {
                    navController.navigate("settings")
                },
                onCallsClick = {
                    navController.navigate("calls")
                },
                onContactsClick = {
                    navController.navigate("contacts")
                },
                onSearchClick = {
                    navController.navigate("search")
                }
            )
        }

        composable("search") {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onNavigateToResult = { result ->
                    when (result.type) {
                        SearchResultType.CHANNEL -> navController.navigate("channel/${result.targetId}")
                        SearchResultType.GROUP -> navController.navigate("group/${result.targetId}")
                        SearchResultType.CONTACT -> navController.navigate("contact/${result.targetId}")
                        SearchResultType.CHAT, SearchResultType.MESSAGE -> navController.navigate("chat/${result.targetId}")
                    }
                }
            )
        }

        composable("calls") {
            CallsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToContactInfo = { targetId ->
                    navigateToInfo(targetId)
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
                onContactInfoClick = { cId ->
                    navigateToInfo(cId)
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
                onMembersClick = { gId ->
                    navController.navigate("group_members/$gId")
                }
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
                onMemberClick = { memberId ->
                    navController.navigate("chat/$memberId")
                },
                onAddMemberClick = { gId ->
                    navController.navigate("add_member/$gId")
                }
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

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onProfileClick = { navController.navigate("my_profile") },
                onPrivacyClick = { navController.navigate("privacy") },
                onNotificationsClick = { navController.navigate("notifications") }
            )
        }

        composable("my_profile") {
            MyProfileScreen(
                onBack = { navController.popBackStack() },
                onEditProfileClick = { navController.navigate("edit_profile") },
                onPrivacyClick = { navController.navigate("privacy") }
            )
        }

        composable("edit_profile") {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("privacy") {
            PrivacyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("notifications") {
            NotificationsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
