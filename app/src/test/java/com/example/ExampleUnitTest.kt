package com.example

import com.example.model.Channel
import com.example.model.Chat
import com.example.model.Group
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.model.NotificationSettings
import com.example.model.PrivacySettings
import com.example.model.Reaction
import com.example.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTelegramMessageModel() {
        val reaction = Reaction(emoji = "🔥", count = 3, userReacted = true)
        val message = Message(
            id = "msg_test",
            chatId = "chat_test",
            senderId = "user_me",
            senderName = "Me",
            text = "Hello Telegram!",
            timestamp = "12:00 PM",
            status = MessageStatus.SENT,
            reactions = listOf(reaction)
        )

        assertEquals("msg_test", message.id)
        assertEquals("Hello Telegram!", message.text)
        assertEquals(1, message.reactions.size)
        assertTrue(message.reactions.first().userReacted)
    }

    @Test
    fun testTelegramChatModel() {
        val chat = Chat(
            id = "c1",
            name = "Pavel Durov",
            unreadCount = 2,
            isPinned = true
        )
        assertTrue(chat.isPinned)
        assertEquals(2, chat.unreadCount)
        assertNotNull(chat.name)
    }

    @Test
    fun testGroupModel() {
        val group = Group(
            id = "g1",
            name = "Android Developers Community",
            memberCount = 3428,
            isAdmin = true
        )
        assertEquals("Android Developers Community", group.name)
        assertEquals(3428, group.memberCount)
        assertTrue(group.isAdmin)
    }

    @Test
    fun testChannelModel() {
        val channel = Channel(
            id = "ch1",
            name = "Telegram News",
            subscriberCount = 894200,
            isSignaturesEnabled = true
        )
        assertEquals("Telegram News", channel.name)
        assertEquals(894200, channel.subscriberCount)
        assertTrue(channel.isSignaturesEnabled)
    }

    @Test
    fun testUserModel() {
        val user = User(
            id = "u1",
            name = "Alex Pratama",
            username = "alexpratama",
            telegramId = "782910342",
            isAppLockEnabled = true
        )
        assertEquals("Alex Pratama", user.name)
        assertEquals("alexpratama", user.username)
        assertTrue(user.isAppLockEnabled)
    }

    @Test
    fun testPrivacySettingsModel() {
        val privacy = PrivacySettings(
            isAppLockEnabled = true,
            phoneNumberPrivacy = "Semua Orang",
            lastSeenPrivacy = "Kontak Saya",
            profilePhotoPrivacy = "Semua Orang",
            activeDevicesCount = 3
        )
        assertTrue(privacy.isAppLockEnabled)
        assertEquals("Semua Orang", privacy.phoneNumberPrivacy)
        assertEquals(3, privacy.activeDevicesCount)
    }

    @Test
    fun testNotificationSettingsModel() {
        val notif = NotificationSettings(
            isPrivateChatNotificationsEnabled = true,
            isGroupNotificationsEnabled = true,
            sound = "Telegram Note",
            isPriorityMessageEnabled = true
        )
        assertTrue(notif.isPrivateChatNotificationsEnabled)
        assertEquals("Telegram Note", notif.sound)
        assertTrue(notif.isPriorityMessageEnabled)
    }

    @Test
    fun testSearchResultModel() {
        val result = com.example.model.SearchResult(
            id = "res_1",
            title = "Android Developers Community",
            subtitle = "Jetpack Compose discussion",
            type = com.example.model.SearchResultType.GROUP,
            memberCount = 5
        )
        assertEquals("res_1", result.id)
        assertEquals("Android Developers Community", result.title)
        assertEquals(com.example.model.SearchResultType.GROUP, result.type)
        assertEquals(5, result.memberCount)
    }

    @Test
    fun testGroupMembersAndAdminIds() {
        val group = Group(
            id = "g_test",
            name = "Compose Clan",
            members = listOf("u1", "u2", "u3"),
            memberIds = listOf("u1", "u2", "u3"),
            adminIds = listOf("u1")
        )
        assertEquals(3, group.memberIds.size)
        assertEquals(1, group.adminIds.size)
        assertTrue(group.adminIds.contains("u1"))
    }
}

