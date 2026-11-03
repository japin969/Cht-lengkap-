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

    @Test
    fun testFaqAndPrivacyModels() {
        val faq = com.example.model.FAQ(
            id = "faq_test",
            question = "Apakah Jayra aman?",
            answer = "Sangat aman dengan AES-256-GCM.",
            category = "Keamanan"
        )
        assertEquals("faq_test", faq.id)
        assertEquals("Keamanan", faq.category)

        val section = com.example.model.PrivacySection(
            id = "p1",
            title = "1. Prinsip Dasar",
            content = "Privasi adalah hak asasi."
        )
        assertEquals("p1", section.id)
        assertNotNull(section.content)
    }

    @Test
    fun testStickerAndGifModels() {
        val sticker = com.example.model.Sticker(
            id = "s1",
            name = "Senyum",
            url = "https://example.com/s1.webp",
            packName = "Populer",
            emoji = "😊"
        )
        assertEquals("s1", sticker.id)
        assertEquals("😊", sticker.emoji)
        assertEquals("Populer", sticker.packName)

        val gif = com.example.model.Gif(
            id = "g1",
            title = "Tepuk Tangan",
            url = "https://example.com/g1.gif"
        )
        assertEquals("g1", gif.id)
        assertEquals("Tepuk Tangan", gif.title)
    }

    @Test
    fun testLanguageAndThemeModels() {
        val lang = com.example.model.LanguageItem(
            code = "id",
            name = "Bahasa Indonesia",
            nativeName = "Bahasa Indonesia",
            isSelected = true
        )
        assertEquals("id", lang.code)
        assertTrue(lang.isSelected)

        val theme = com.example.model.ThemeOption(
            id = "dark",
            name = "Mode Gelap",
            description = "Tema gelap hemat baterai"
        )
        assertEquals("dark", theme.id)
    }

    @Test
    fun testPollModelAndPercentageCalculation() {
        val options = listOf(
            com.example.model.PollOption("Option A", 3),
            com.example.model.PollOption("Option B", 1)
        )
        val poll = com.example.model.Poll(
            id = "poll_1",
            question = "Which language?",
            options = options,
            isAnonymous = true,
            allowMultiple = false,
            isQuiz = true,
            correctOptionIndex = 0
        )

        assertEquals("poll_1", poll.id)
        assertEquals(4, poll.totalVotes)
        assertEquals(0.75f, poll.percentageFor(0), 0.001f)
        assertEquals(0.25f, poll.percentageFor(1), 0.001f)
        assertTrue(poll.isQuiz)
        assertEquals(0, poll.correctOptionIndex)
    }

    @Test
    fun testLocationMessageAndContactMessage() {
        val loc = com.example.model.LocationMessage(
            lat = -6.175392,
            lng = 106.827153,
            title = "Monas"
        )
        assertEquals(-6.175392, loc.lat, 0.0001)
        assertEquals(106.827153, loc.lng, 0.0001)
        assertEquals("Monas", loc.title)

        val contact = com.example.model.ContactMessage(
            name = "Pavel",
            phone = "+1 555 0192",
            avatarUrl = "https://example.com/avatar.jpg"
        )
        assertEquals("Pavel", contact.name)
        assertEquals("+1 555 0192", contact.phone)
    }

    @Test
    fun testUiStatesFase14() {
        val pollState: com.example.model.CreatePollUiState = com.example.model.CreatePollUiState.Success
        assertTrue(pollState is com.example.model.CreatePollUiState.Success)

        val locState: com.example.model.LocationUiState = com.example.model.LocationUiState.Success(
            lat = -6.175,
            lng = 106.827,
            address = "Jakarta",
            accuracyMeters = 10f
        )
        assertTrue(locState is com.example.model.LocationUiState.Success)
        assertEquals("Jakarta", (locState as com.example.model.LocationUiState.Success).address)

        val emptyContact: com.example.model.ContactSharingUiState = com.example.model.ContactSharingUiState.Empty
        assertTrue(emptyContact is com.example.model.ContactSharingUiState.Empty)
    }
}

