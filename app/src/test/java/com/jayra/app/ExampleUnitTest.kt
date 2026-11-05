package com.jayra.app

import com.jayra.app.model.Channel
import com.jayra.app.model.Chat
import com.jayra.app.model.Group
import com.jayra.app.model.Message
import com.jayra.app.model.MessageStatus
import com.jayra.app.model.NotificationSettings
import com.jayra.app.model.PrivacySettings
import com.jayra.app.model.Reaction
import com.jayra.app.model.User
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
        val result = com.jayra.app.model.SearchResult(
            id = "res_1",
            title = "Android Developers Community",
            subtitle = "Jetpack Compose discussion",
            type = com.jayra.app.model.SearchResultType.GROUP,
            memberCount = 5
        )
        assertEquals("res_1", result.id)
        assertEquals("Android Developers Community", result.title)
        assertEquals(com.jayra.app.model.SearchResultType.GROUP, result.type)
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
        val faq = com.jayra.app.model.FAQ(
            id = "faq_test",
            question = "Apakah Jayra aman?",
            answer = "Sangat aman dengan AES-256-GCM.",
            category = "Keamanan"
        )
        assertEquals("faq_test", faq.id)
        assertEquals("Keamanan", faq.category)

        val section = com.jayra.app.model.PrivacySection(
            id = "p1",
            title = "1. Prinsip Dasar",
            content = "Privasi adalah hak asasi."
        )
        assertEquals("p1", section.id)
        assertNotNull(section.content)
    }

    @Test
    fun testStickerAndGifModels() {
        val sticker = com.jayra.app.model.Sticker(
            id = "s1",
            name = "Senyum",
            url = "https://example.com/s1.webp",
            packName = "Populer",
            emoji = "😊"
        )
        assertEquals("s1", sticker.id)
        assertEquals("😊", sticker.emoji)
        assertEquals("Populer", sticker.packName)

        val gif = com.jayra.app.model.Gif(
            id = "g1",
            title = "Tepuk Tangan",
            url = "https://example.com/g1.gif"
        )
        assertEquals("g1", gif.id)
        assertEquals("Tepuk Tangan", gif.title)
    }

    @Test
    fun testLanguageAndThemeModels() {
        val lang = com.jayra.app.model.LanguageItem(
            code = "id",
            name = "Bahasa Indonesia",
            nativeName = "Bahasa Indonesia",
            isSelected = true
        )
        assertEquals("id", lang.code)
        assertTrue(lang.isSelected)

        val theme = com.jayra.app.model.ThemeOption(
            id = "dark",
            name = "Mode Gelap",
            description = "Tema gelap hemat baterai"
        )
        assertEquals("dark", theme.id)
    }

    @Test
    fun testPollModelAndPercentageCalculation() {
        val options = listOf(
            com.jayra.app.model.PollOption("Option A", 3),
            com.jayra.app.model.PollOption("Option B", 1)
        )
        val poll = com.jayra.app.model.Poll(
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
        val loc = com.jayra.app.model.LocationMessage(
            lat = -6.175392,
            lng = 106.827153,
            title = "Monas"
        )
        assertEquals(-6.175392, loc.lat, 0.0001)
        assertEquals(106.827153, loc.lng, 0.0001)
        assertEquals("Monas", loc.title)

        val contact = com.jayra.app.model.ContactMessage(
            name = "Pavel",
            phone = "+1 555 0192",
            avatarUrl = "https://example.com/avatar.jpg"
        )
        assertEquals("Pavel", contact.name)
        assertEquals("+1 555 0192", contact.phone)
    }

    @Test
    fun testUiStatesFase14() {
        val pollState: com.jayra.app.model.CreatePollUiState = com.jayra.app.model.CreatePollUiState.Success
        assertTrue(pollState is com.jayra.app.model.CreatePollUiState.Success)

        val locState: com.jayra.app.model.LocationUiState = com.jayra.app.model.LocationUiState.Success(
            lat = -6.175,
            lng = 106.827,
            address = "Jakarta",
            accuracyMeters = 10f
        )
        assertTrue(locState is com.jayra.app.model.LocationUiState.Success)
        assertEquals("Jakarta", (locState as com.jayra.app.model.LocationUiState.Success).address)

        val emptyContact: com.jayra.app.model.ContactSharingUiState = com.jayra.app.model.ContactSharingUiState.Empty
        assertTrue(emptyContact is com.jayra.app.model.ContactSharingUiState.Empty)
    }

    @Test
    fun testAllViewModelConstructorsForDefaultFactory() {
        val androidViewModelClasses = listOf(
            com.jayra.app.ui.music.MusicViewModel::class.java,
            com.jayra.app.ui.assistant.AssistantViewModel::class.java,
            com.jayra.app.ui.call.CallViewModel::class.java,
            com.jayra.app.ui.call.CallHistoryViewModel::class.java,
            com.jayra.app.ui.viewmodel.ChatRoomViewModel::class.java,
            com.jayra.app.ui.location.LocationViewModel::class.java,
            com.jayra.app.ui.security.PasscodeViewModel::class.java,
            com.jayra.app.ui.security.TwoStepVerificationViewModel::class.java,
            com.jayra.app.ui.security.ActiveSessionsViewModel::class.java,
            com.jayra.app.ui.settings.LanguageViewModel::class.java,
            com.jayra.app.ui.settings.ThemeViewModel::class.java,
            com.jayra.app.ui.settings.StorageDetailViewModel::class.java,
            com.jayra.app.ui.settings.DataStorageViewModel::class.java,
            com.jayra.app.ui.settings.AdvancedSettingsViewModel::class.java,
            com.jayra.app.ui.settings.ProxyViewModel::class.java,
            com.jayra.app.ui.voicetrimmer.VoiceTrimmerViewModel::class.java
        )

        for (vmClass in androidViewModelClasses) {
            val ctor = vmClass.getConstructor(android.app.Application::class.java)
            assertNotNull("Missing (Application) constructor on ${vmClass.name}", ctor)
        }

        val zeroArgViewModelClasses = listOf(
            com.jayra.app.ui.main.MainViewModel::class.java,
            com.jayra.app.ui.viewmodel.ChatListViewModel::class.java,
            com.jayra.app.ui.search.SearchViewModel::class.java,
            com.jayra.app.ui.viewmodel.SettingsViewModel::class.java,
            com.jayra.app.ui.viewmodel.ContactsViewModel::class.java,
            com.jayra.app.ui.viewmodel.MyProfileViewModel::class.java,
            com.jayra.app.ui.viewmodel.EditProfileViewModel::class.java,
            com.jayra.app.ui.viewmodel.NotificationsViewModel::class.java,
            com.jayra.app.ui.viewmodel.PrivacyViewModel::class.java,
            com.jayra.app.ui.viewmodel.ContactInfoViewModel::class.java,
            com.jayra.app.ui.viewmodel.GroupInfoViewModel::class.java,
            com.jayra.app.ui.viewmodel.ChannelInfoViewModel::class.java,
            com.jayra.app.ui.addmember.AddMemberViewModel::class.java,
            com.jayra.app.ui.memberpermissions.MemberPermissionsViewModel::class.java,
            com.jayra.app.ui.groupsettings.GroupSettingsViewModel::class.java,
            com.jayra.app.ui.creategroup.CreateGroupViewModel::class.java,
            com.jayra.app.ui.contacts.ContactSharingViewModel::class.java,
            com.jayra.app.ui.poll.CreatePollViewModel::class.java,
            com.jayra.app.ui.groupmembers.GroupMembersViewModel::class.java,
            com.jayra.app.ui.about.AboutViewModel::class.java,
            com.jayra.app.ui.stickers.StickersViewModel::class.java,
            com.jayra.app.ui.help.HelpViewModel::class.java,
            com.jayra.app.ui.help.PrivacyPolicyViewModel::class.java,
            com.jayra.app.ui.saved.SavedMessagesViewModel::class.java,
            com.jayra.app.ui.forward.ForwardViewModel::class.java,
            com.jayra.app.ui.search.GlobalSearchViewModel::class.java,
            com.jayra.app.ui.splash.SplashViewModel::class.java,
            com.jayra.app.ui.communities.CommunitiesViewModel::class.java,
            com.jayra.app.ui.story.StoryViewModel::class.java,
            com.jayra.app.ui.story.StoryViewerViewModel::class.java,
            com.jayra.app.ui.checklist.CreateChecklistViewModel::class.java
        )

        for (vmClass in zeroArgViewModelClasses) {
            val ctor = vmClass.getConstructor()
            assertNotNull("Missing () constructor on ${vmClass.name}", ctor)
        }
    }

    @Test
    fun testGlobalSearchSanitizationAndFilters() {
        val raw = "  Jayra <script>\"alert\";\\`   test  "
        val sanitized = com.jayra.app.ui.search.GlobalSearchViewModel.sanitizeSearchQuery(raw)
        assertEquals("Jayra script alert test", sanitized)
        assertEquals("", com.jayra.app.ui.search.GlobalSearchViewModel.sanitizeSearchQuery("   \"';<>\\`  "))

        val chips = com.jayra.app.model.SearchFilter.GLOBAL_SEARCH_FILTERS.map { it.globalChipLabel }
        assertEquals(listOf("Semua", "Chat", "Pesan", "Kontak", "File"), chips)

        val fileResult = com.jayra.app.model.SearchResult(
            id = "f1",
            title = "Jayra_Spec.pdf",
            subtitle = "2.4 MB",
            type = com.jayra.app.model.SearchResultType.FILE
        )
        assertTrue(com.jayra.app.ui.search.GlobalSearchViewModel.matchesFilter(fileResult, com.jayra.app.model.SearchFilter.ALL))
        assertTrue(com.jayra.app.ui.search.GlobalSearchViewModel.matchesFilter(fileResult, com.jayra.app.model.SearchFilter.FILES))
        assertTrue(!com.jayra.app.ui.search.GlobalSearchViewModel.matchesFilter(fileResult, com.jayra.app.model.SearchFilter.MESSAGES))
    }

    @Test
    fun testFase20SplashAndErrorHandler() {
        val splashState = com.jayra.app.model.SplashUiState()
        assertEquals("Jayra", splashState.appTitle)
        assertEquals("Modern, Cepat, Aman", splashState.tagline)
        assertEquals(1500L, splashState.splashDelayMillis)
        assertEquals(
            "Terjadi kesalahan tak terduga. Aplikasi akan dimulai ulang.",
            com.jayra.app.util.ErrorHandler.CRASH_TOAST_MESSAGE
        )
    }

    @Test
    fun testFase21MarkdownParserAndRichTextFormatting() {
        val raw = "Halo **Tebal** __Miring__ ~~Coret~~ `Kode` dan https://jayra.app!"
        val parsed = com.jayra.app.util.MarkdownParser.parse(raw)
        assertEquals("Halo Tebal Miring Coret Kode dan https://jayra.app!", parsed.text)

        val boldStyle = parsed.spanStyles.firstOrNull {
            it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold
        }
        assertNotNull("Expected Bold SpanStyle", boldStyle)
        assertEquals("Tebal", parsed.text.substring(boldStyle!!.start, boldStyle.end))

        val italicStyle = parsed.spanStyles.firstOrNull {
            it.item.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic
        }
        assertNotNull("Expected Italic SpanStyle", italicStyle)
        assertEquals("Miring", parsed.text.substring(italicStyle!!.start, italicStyle.end))

        val strikeStyle = parsed.spanStyles.firstOrNull {
            it.item.textDecoration == androidx.compose.ui.text.style.TextDecoration.LineThrough
        }
        assertNotNull("Expected Strikethrough SpanStyle", strikeStyle)
        assertEquals("Coret", parsed.text.substring(strikeStyle!!.start, strikeStyle.end))

        val codeStyle = parsed.spanStyles.firstOrNull {
            it.item.fontFamily == androidx.compose.ui.text.font.FontFamily.Monospace
        }
        assertNotNull("Expected Monospace SpanStyle", codeStyle)
        assertEquals("Kode", parsed.text.substring(codeStyle!!.start, codeStyle.end))

        val urlAnnotations = parsed.getStringAnnotations(
            tag = com.jayra.app.util.MarkdownParser.URL_ANNOTATION_TAG,
            start = 0,
            end = parsed.length
        )
        assertEquals(1, urlAnnotations.size)
        assertEquals("https://jayra.app", urlAnnotations.first().item)

        // Uji Markdown link [label](url)
        val mdLinkParsed = com.jayra.app.util.MarkdownParser.parse("Buka [Portal Jayra](https://jayra.app/docs).")
        assertEquals("Buka Portal Jayra.", mdLinkParsed.text)
        val mdLinkAnn = mdLinkParsed.getStringAnnotations(
            tag = com.jayra.app.util.MarkdownParser.URL_ANNOTATION_TAG,
            start = 0,
            end = mdLinkParsed.length
        )
        assertEquals(1, mdLinkAnn.size)
        assertEquals("https://jayra.app/docs", mdLinkAnn.first().item)

        // Uji penyisipan kursor & seleksi pada RichTextEditor
        val config = com.jayra.app.model.RichTextEditorConfig()
        assertEquals("Ketik pesan...", config.placeholderText)
        assertEquals(5, config.formatOptions.size)

        val boldOption = config.formatOptions.first { it.type == com.jayra.app.model.RichTextFormatType.BOLD }
        val emptyFormatted = com.jayra.app.util.MarkdownParser.applyFormatting(
            androidx.compose.ui.text.input.TextFieldValue("", androidx.compose.ui.text.TextRange(0)),
            boldOption
        )
        assertEquals("****", emptyFormatted.text)
        assertEquals(2, emptyFormatted.selection.start)

        val selectionFormatted = com.jayra.app.util.MarkdownParser.applyFormatting(
            androidx.compose.ui.text.input.TextFieldValue("Halo Jayra", androidx.compose.ui.text.TextRange(5, 10)),
            boldOption
        )
        assertEquals("Halo **Jayra**", selectionFormatted.text)
        assertEquals(7, selectionFormatted.selection.start)
        assertEquals(12, selectionFormatted.selection.end)
    }

    @Test
    fun testFase22CommunitiesAndTopicTabs() {
        val community = com.jayra.app.model.Community(
            id = "comm_1",
            name = "Komunitas Developer Jayra",
            description = "Diskusi Kotlin & Compose",
            avatarUrl = "https://example.com/comm.jpg",
            groupIds = listOf("chat_2", "chat_5"),
            creatorId = "user_telegram_me",
            memberIds = listOf("user_telegram_me")
        )
        assertEquals("comm_1", community.id)
        assertEquals(2, community.groupIds.size)

        val uiState: com.jayra.app.model.CommunityUiState =
            com.jayra.app.model.CommunityUiState.Success(listOf(community))
        assertTrue(uiState is com.jayra.app.model.CommunityUiState.Success)
        assertEquals(1, (uiState as com.jayra.app.model.CommunityUiState.Success).communities.size)

        val groupWithTopics = com.jayra.app.model.Chat(
            id = "chat_2",
            name = "Android Developers ID",
            isGroup = true,
            hasTopics = true
        )
        assertTrue(groupWithTopics.isGroup && groupWithTopics.hasTopics)

        val defaultTopics = com.jayra.app.model.ChatTopic.DEFAULT_TOPICS
        assertEquals(listOf("General", "Topik 1", "Topik 2"), defaultTopics.map { it.title })
        assertEquals(listOf("general", "topik_1", "topik_2"), defaultTopics.map { it.id })

        val topicMessage = com.jayra.app.model.Message(
            id = "msg_topic_1",
            senderId = "sarah",
            senderName = "Sarah",
            text = "Halo Topik 1",
            timestamp = "10:00 AM",
            dateGroup = "Today",
            topicId = "topik_1"
        )
        assertEquals("topik_1", topicMessage.topicId)
    }

    @Test
    fun testFase23StoryAlbumsAndViewer() {
        val story = com.jayra.app.model.Story(
            id = "story_1",
            userId = "user_pavel",
            imageUrl = "https://example.com/story1.jpg",
            timestamp = "09:15 AM",
            viewsCount = 48,
            isViewed = false,
            albumId = "album_rilis",
            albumTitle = "Rilis Jayra",
            userName = "Pavel Durov"
        )
        assertEquals("story_1", story.id)
        assertEquals("user_pavel", story.userId)
        assertEquals(48, story.viewsCount)
        assertEquals(false, story.isViewed)
        assertEquals("album_rilis", story.albumId)

        val albumUiState: com.jayra.app.model.StoryUiState =
            com.jayra.app.model.StoryUiState.Success(
                stories = listOf(story),
                selectedAlbumId = "album_rilis"
            )
        assertTrue(albumUiState is com.jayra.app.model.StoryUiState.Success)
        assertEquals(1, (albumUiState as com.jayra.app.model.StoryUiState.Success).stories.size)

        val viewerUiState: com.jayra.app.model.StoryViewerUiState =
            com.jayra.app.model.StoryViewerUiState.Success(
                stories = listOf(story),
                currentIndex = 0
            )
        assertTrue(viewerUiState is com.jayra.app.model.StoryViewerUiState.Success)
        assertEquals(5000, com.jayra.app.ui.story.StoryViewerViewModel.STORY_DURATION_MS)
        assertEquals("Stories", com.jayra.app.model.StoryStrings().screenTitle)
    }

    @Test
    fun testFase24ContactNotesAndBirthdays() {
        val birthdayMillis = 466214400000L // 1984-10-10 00:00:00 UTC
        val formatted = com.jayra.app.data.ContactNoteRepository.formatBirthdayMillis(birthdayMillis)
        assertTrue("Expected 1984 in formatted date: $formatted", formatted.contains("1984"))
        assertTrue("Expected day 10 in formatted date: $formatted", formatted.startsWith("10 "))
        assertEquals("", com.jayra.app.data.ContactNoteRepository.formatBirthdayMillis(null))

        val note = com.jayra.app.model.ContactNote(
            contactId = "pavel_durov",
            ownerId = "user_me",
            note = "Catatan rahasia kontak",
            birthdayFormatted = formatted,
            birthdayMillis = birthdayMillis
        )
        assertEquals("pavel_durov", note.contactId)
        assertEquals("Catatan rahasia kontak", note.note)
        assertEquals(birthdayMillis, note.birthdayMillis)

        val strings = com.jayra.app.model.ContactNoteStrings()
        assertEquals("Catatan Pribadi", strings.personalNoteCardTitle)
        assertEquals("Ulang Tahun", strings.birthdayCardTitle)
    }

    @Test
    fun testFase25Checklists() {
        val items = listOf(
            com.jayra.app.model.ChecklistItem(id = "t1", text = "Susun UI Checklist", isDone = true),
            com.jayra.app.model.ChecklistItem(id = "t2", text = "Uji Firestore Rules", isDone = false),
            com.jayra.app.model.ChecklistItem(id = "t3", text = "Verifikasi Dark Mode", isDone = true)
        )
        val checklist = com.jayra.app.model.Checklist(
            id = "cl_1",
            title = "Rilis Fase 25",
            items = items,
            allowOthersToToggle = true
        )
        assertEquals(2, checklist.completedCount)
        assertEquals(3, checklist.totalCount)
        assertEquals(2f / 3f, checklist.progressFraction, 0.001f)

        val summary = com.jayra.app.data.ChatRepository.buildChecklistSummaryText("Rilis Fase 25", items)
        assertTrue(summary.contains("☑️ Daftar Tugas: Rilis Fase 25"))
        assertTrue(summary.contains("[x] Susun UI Checklist"))
        assertTrue(summary.contains("[ ] Uji Firestore Rules"))

        val parsed = com.jayra.app.data.ChatRepository.parseChecklistItemsFromRaw(
            listOf(
                mapOf("id" to "t1", "text" to "Susun UI Checklist", "isDone" to true),
                mapOf("id" to "t2", "text" to "Uji Firestore Rules", "isDone" to false)
            )
        )
        assertEquals(2, parsed.size)
        assertTrue(parsed[0].isDone)
        assertEquals("Uji Firestore Rules", parsed[1].text)

        val strings = com.jayra.app.model.ChecklistStrings()
        assertEquals("Daftar Tugas Baru", strings.screenTitle)
        assertEquals("BUAT", strings.createActionLabel)
    }

    @Test
    fun testFase26VoiceTrimmerWavSynthesisAndSlicing() {
        val tempDir = kotlin.io.path.createTempDirectory("jayra_voice_trim_test").toFile()
        try {
            val srcFile = java.io.File(tempDir, "source_10s.wav")
            val outFile = java.io.File(tempDir, "trimmed_3_5s.wav")

            val sourceWave = com.jayra.app.util.AudioWaveTrimmer.generateSampleVoiceWav(
                targetFile = srcFile,
                durationMillis = 10_000L,
                barCount = 40
            )
            assertTrue("Source WAV must exist", srcFile.exists())
            assertTrue("Source WAV must exceed header size", srcFile.length() > 100_000L)
            assertEquals(40, sourceWave.size)

            val trimResult = com.jayra.app.util.AudioWaveTrimmer.trimAudioFile(
                sourceFile = srcFile,
                outputFile = outFile,
                startMs = 2_000L,
                endMs = 5_500L
            ).getOrThrow()

            assertTrue("Trimmed WAV must exist", outFile.exists())
            assertEquals(3_500L, trimResult.durationMillis)
            assertEquals(28, trimResult.waveform.size)
            assertTrue("Trimmed WAV must be smaller than 10s source", trimResult.sizeBytes < srcFile.length())

            assertEquals("00:03.5", com.jayra.app.util.AudioWaveTrimmer.formatDurationMillis(3_500L))
            assertEquals("0:04", com.jayra.app.util.AudioWaveTrimmer.formatCompactDurationMillis(3_500L))
            assertEquals("🎤 [Voice Note 0:04]", com.jayra.app.data.ChatRepository.buildVoiceNoteSummaryText(3_500L))

            val readyState = com.jayra.app.model.VoiceTrimmerUiState.Ready(
                sourceFilePath = srcFile.absolutePath,
                totalDurationMillis = 10_000L,
                startFraction = 0.2f,
                endFraction = 0.55f,
                waveform = sourceWave
            )
            assertEquals(2_000L, readyState.startMillis)
            assertEquals(5_500L, readyState.endMillis)
            assertEquals(3_500L, readyState.trimmedDurationMillis)
            assertEquals("Potong Voice Note", com.jayra.app.model.VoiceTrimmerStrings().screenTitle)
            assertEquals("Simpan", com.jayra.app.model.VoiceTrimmerStrings().saveButtonLabel)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}




