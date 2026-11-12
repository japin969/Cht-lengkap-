package com.jayra.app.model

import androidx.compose.runtime.Immutable

enum class ChatFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    FAVORITES("Favorites"),
    GROUPS("Groups"),
    CHANNELS("Channels"),
    DRAFTS("Drafts")
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    VOICE,
    VIDEO
}

enum class CallFilter {
    ALL,
    MISSED
}

@Immutable
data class Reaction(
    val emoji: String,
    val count: Int,
    val userReacted: Boolean = false,
    val userIds: List<String> = emptyList()
)

@Immutable
data class Chat(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val lastMessage: String = "",
    val timestamp: String = "",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isRead: Boolean = true,
    val isGroup: Boolean = false,
    val hasTopics: Boolean = false,
    val isChannel: Boolean = false,
    val participants: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val hasDraft: Boolean = false,
    val draftText: String = "",
    val isOnline: Boolean = false,
    val lastMessageStatus: MessageStatus = MessageStatus.SENT
)

@Immutable
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: String = "",
    val dateGroup: String = "Today",
    val status: MessageStatus = MessageStatus.SENT,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: List<Reaction> = emptyList(),
    val forwardedFrom: String? = null,
    val topicId: String = "general",
    val checklistTitle: String? = null,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val allowOthersToToggle: Boolean = true,
    val voiceFilePath: String? = null,
    val voiceDurationMillis: Long? = null,
    val voiceWaveform: List<Float> = emptyList(),
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val stickerUrl: String? = null,
    val gifUrl: String? = null,
    val linkPreviewTitle: String? = null,
    val linkPreviewDescription: String? = null,
    val linkPreviewImageUrl: String? = null,
    val systemText: String? = null,
    val disappearingSeconds: Long? = null
)

@Immutable
data class Contact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val status: String = "online",
    val isOnline: Boolean = true,
    val mediaCount: Int = 42,
    val starredCount: Int = 7,
    val isMuted: Boolean = false,
    val saveToCamera: Boolean = true,
    val disappearingEnabled: Boolean = false
)

@Immutable
data class GroupPermissions(
    val sendMessages: String = "Semua Anggota",
    val sendMedia: String = "Semua Anggota",
    val addMembers: String = "Admin",
    val pinMessages: String = "Admin",
    val changeInfo: String = "Admin"
)

@Immutable
data class Group(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val memberCount: Int = 0,
    val description: String = "",
    val mediaCount: Int = 12,
    val starredCount: Int = 0,
    val isMuted: Boolean = false,
    val isCustomNotification: Boolean = false,
    val disappearingDuration: String = "Mati",
    val isLocked: Boolean = false,
    val isAdmin: Boolean = false,
    val creatorId: String = "",
    val members: List<String> = emptyList(),
    val memberAvatars: List<String> = emptyList(),
    val memberIds: List<String> = members,
    val adminIds: List<String> = emptyList(),
    val sensitiveContent: Boolean = false,
    val adminApproval: Boolean = false,
    val inviteViaLinkEnabled: Boolean = true,
    val mediaVisibility: Boolean = true,
    val descriptionDisplay: String
        get() = description.ifBlank { "No description" },
    val permissions: Map<String, String> = mapOf(
        "send_messages" to "Semua Anggota",
        "send_media" to "Semua Anggota",
        "add_members" to "Semua Anggota",
        "pin_messages" to "Semua Anggota",
        "change_info" to "Semua Anggota"
    )
)

@Immutable
data class Channel(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val subscriberCount: Int = 0,
    val description: String = "",
    val mediaCount: Int = 24,
    val starredCount: Int = 3,
    val isMuted: Boolean = false,
    val isCustomNotification: Boolean = false,
    val disappearingDuration: String = "Mati",
    val isSignaturesEnabled: Boolean = false,
    val isAdmin: Boolean = false,
    val creatorId: String = "",
    val subscribers: List<String> = emptyList()
)

@Immutable
data class User(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val username: String = "",
    val telegramId: String = "",
    val avatarUrl: String = "",
    val disappearingDuration: String = "Mati",
    val isAppLockEnabled: Boolean = false,
    val language: String = "Indonesia",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val lastSeenFormatted: String = "online",
    val contacts: List<String> = emptyList(),
    val blocked: Boolean = false,
    val isMuted: Boolean = false
)

@Immutable
data class CallLog(
    val id: String = "",
    val callId: String = id,
    val participantId: String = "",
    val participantName: String = "",
    val participantAvatar: String = "",
    val type: CallType = CallType.INCOMING,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0L,
    val timeFormatted: String = "10:45 AM",
    val isVideo: Boolean = false
)

@Immutable
data class PrivacySettings(
    val isAppLockEnabled: Boolean = false,
    val phoneNumberPrivacy: String = "Semua Orang",
    val lastSeenPrivacy: String = "Kontak Saya",
    val profilePhotoPrivacy: String = "Semua Orang",
    val disappearingDuration: String = "Mati",
    val isTwoStepVerificationEnabled: Boolean = true,
    val activeDevicesCount: Int = 3
)

@Immutable
data class NotificationSettings(
    val isPrivateChatNotificationsEnabled: Boolean = true,
    val isGroupNotificationsEnabled: Boolean = true,
    val isChannelNotificationsEnabled: Boolean = true,
    val sound: String = "Default",
    val vibrate: String = "Default",
    val isPriorityMessageEnabled: Boolean = true,
    val isDeletedMessageNoticeEnabled: Boolean = false,
    val isMessagePreviewEnabled: Boolean = true
)

sealed interface ChatListUiState {
    data object Loading : ChatListUiState
    data class Success(val chats: List<Chat>) : ChatListUiState
    data object Empty : ChatListUiState
    data class Error(val message: String) : ChatListUiState
}

sealed interface ChatRoomUiState {
    data object Loading : ChatRoomUiState
    data class Success(
        val messages: List<Message>,
        val isTyping: Boolean = false,
        val replyTo: Message? = null,
        val contact: Contact? = null
    ) : ChatRoomUiState
    data object Empty : ChatRoomUiState
    data class Error(val message: String) : ChatRoomUiState
}

sealed interface ContactInfoUiState {
    data object Loading : ContactInfoUiState
    data class Success(val contact: Contact) : ContactInfoUiState
    data class Error(val message: String) : ContactInfoUiState
}

sealed interface GroupInfoUiState {
    data object Loading : GroupInfoUiState
    data class Success(val group: Group) : GroupInfoUiState
    data class Error(val message: String) : GroupInfoUiState
}

sealed interface ChannelInfoUiState {
    data object Loading : ChannelInfoUiState
    data class Success(val channel: Channel) : ChannelInfoUiState
    data class Error(val message: String) : ChannelInfoUiState
}

sealed interface MyProfileUiState {
    data object Loading : MyProfileUiState
    data class Success(val user: User) : MyProfileUiState
    data class Error(val message: String) : MyProfileUiState
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val user: User,
        val appTheme: String = "System",
        val language: String = "Indonesia"
    ) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

sealed interface EditProfileUiState {
    data object Loading : EditProfileUiState
    data class Success(
        val user: User,
        val isSaving: Boolean = false
    ) : EditProfileUiState
    data class Error(val message: String) : EditProfileUiState
}

sealed interface PrivacyUiState {
    data object Loading : PrivacyUiState
    data class Success(val privacy: PrivacySettings) : PrivacyUiState
    data class Error(val message: String) : PrivacyUiState
}

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Success(val notifications: NotificationSettings) : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

sealed interface CallsUiState {
    data object Loading : CallsUiState
    data class Success(val calls: List<CallLog>) : CallsUiState
    data object Empty : CallsUiState
    data class Error(val message: String) : CallsUiState
}

sealed interface ContactsUiState {
    data object Loading : ContactsUiState
    data class Success(
        val contacts: List<User>,
        val onlineUsers: List<User>
    ) : ContactsUiState
    data object Empty : ContactsUiState
    data class Error(val message: String) : ContactsUiState
}

enum class SearchFilter(
    val label: String,
    val globalChipLabel: String = label
) {
    ALL(label = "Semua", globalChipLabel = "Semua"),
    CHATS(label = "Obrolan", globalChipLabel = "Chat"),
    MESSAGES(label = "Pesan", globalChipLabel = "Pesan"),
    CONTACTS(label = "Kontak", globalChipLabel = "Kontak"),
    FILES(label = "File", globalChipLabel = "File"),
    GROUPS(label = "Grup", globalChipLabel = "Grup"),
    CHANNELS(label = "Saluran", globalChipLabel = "Saluran");

    companion object {
        val GLOBAL_SEARCH_FILTERS: List<SearchFilter> = listOf(
            ALL,
            CHATS,
            MESSAGES,
            CONTACTS,
            FILES
        )
    }
}

enum class SearchResultType {
    CHAT,
    MESSAGE,
    CONTACT,
    GROUP,
    CHANNEL,
    FILE
}

@Immutable
data class SearchResult(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val timestamp: String = "",
    val type: SearchResultType = SearchResultType.CHAT,
    val avatarUrl: String = "",
    val memberCount: Int = 0,
    val targetId: String = id,
    val isOnline: Boolean = false
)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<SearchResult>) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface GroupMembersUiState {
    data object Loading : GroupMembersUiState
    data class Success(
        val group: Group,
        val admins: List<User>,
        val members: List<User>,
        val isCurrentUserAdmin: Boolean = true
    ) : GroupMembersUiState
    data object Empty : GroupMembersUiState
    data class Error(val message: String) : GroupMembersUiState
}

sealed interface AddMemberUiState {
    data object Loading : AddMemberUiState
    data class Success(
        val group: Group,
        val availableContacts: List<User>,
        val selectedUserIds: Set<String> = emptySet(),
        val isSaving: Boolean = false
    ) : AddMemberUiState
    data object Empty : AddMemberUiState
    data class Error(val message: String) : AddMemberUiState
}

sealed interface AssistantUiState {
    data object Loading : AssistantUiState
    data class Success(
        val messages: List<Message>,
        val isLoading: Boolean = false
    ) : AssistantUiState
    data object Empty : AssistantUiState
    data class Error(val message: String) : AssistantUiState
}

@Immutable
data class AdvancedSettings(
    val dataRoaming: Boolean = false,
    val proxyEnabled: Boolean = false,
    val callQuality: String = "Otomatis",
    val uploadQuality: String = "HD",
    val externalStorage: Boolean = false,
    val cacheSize: String = "0 MB",
    val cacheLimit: String = "5 GB",
    val saveToGallery: Boolean = false,
    val animationsEnabled: Boolean = true,
    val typingVibration: Boolean = true,
    val enterToSend: Boolean = false
)

@Immutable
data class StorageInfo(
    val totalSize: String = "0 MB",
    val photoSize: String = "0 MB",
    val videoSize: String = "0 MB",
    val documentSize: String = "0 MB",
    val musicSize: String = "0 MB",
    val otherSize: String = "0 MB",
    val autoDownloadCellular: Boolean = true,
    val autoDownloadWifi: Boolean = true,
    val autoDownloadRoaming: Boolean = false
)

@Immutable
data class FileInfo(
    val id: String = "",
    val name: String = "",
    val size: String = "",
    val date: String = "",
    val path: String = ""
)

sealed interface AdvancedSettingsUiState {
    data object Loading : AdvancedSettingsUiState
    data class Success(val settings: AdvancedSettings) : AdvancedSettingsUiState
    data class Error(val message: String) : AdvancedSettingsUiState
}

sealed interface DataStorageUiState {
    data object Loading : DataStorageUiState
    data class Success(val storageInfo: StorageInfo) : DataStorageUiState
    data class Error(val message: String) : DataStorageUiState
}

sealed interface ProxyUiState {
    data object Loading : ProxyUiState
    data class Success(
        val isEnabled: Boolean = false,
        val host: String = "",
        val port: String = "",
        val username: String = "",
        val password: String = ""
    ) : ProxyUiState
    data class Error(val message: String) : ProxyUiState
}

sealed interface StorageDetailUiState {
    data object Loading : StorageDetailUiState
    data class Success(val files: List<FileInfo>) : StorageDetailUiState
    data object Empty : StorageDetailUiState
    data class Error(val message: String) : StorageDetailUiState
}

@Immutable
data class CallParticipant(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val isMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
    val isLocalUser: Boolean = false,
    val isFrontCamera: Boolean = true
)

sealed interface CallUiState {
    data class Connecting(
        val participantName: String,
        val participantAvatar: String = "",
        val isVideoCall: Boolean = false,
        val isFrontCamera: Boolean = true,
        val participants: List<CallParticipant> = emptyList(),
        val availableContacts: List<User> = emptyList()
    ) : CallUiState

    data class Connected(
        val participantName: String,
        val participantAvatar: String = "",
        val duration: String = "00:00",
        val isVideoCall: Boolean = false,
        val isMuted: Boolean = false,
        val isVideoEnabled: Boolean = true,
        val isSpeakerOn: Boolean = false,
        val isRemoteVideoAvailable: Boolean = true,
        val isFrontCamera: Boolean = true,
        val participants: List<CallParticipant> = emptyList(),
        val availableContacts: List<User> = emptyList()
    ) : CallUiState

    data class Ended(
        val participantName: String,
        val duration: String
    ) : CallUiState

    data class Error(val message: String) : CallUiState
}

sealed interface CallHistoryUiState {
    data object Loading : CallHistoryUiState
    data class Success(val calls: List<CallLog>) : CallHistoryUiState
    data object Empty : CallHistoryUiState
    data class Error(val message: String) : CallHistoryUiState
}

@Immutable
data class TwoStepData(
    val isEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val recoveryEmail: String = "",
    val hasPassword: Boolean = false
)

sealed interface TwoStepVerificationUiState {
    data object Loading : TwoStepVerificationUiState
    data class Success(val data: TwoStepData) : TwoStepVerificationUiState
    data class Error(val message: String) : TwoStepVerificationUiState
}

@Immutable
data class Session(
    val id: String = "",
    val deviceName: String = "",
    val location: String = "",
    val lastActive: String = "",
    val isCurrent: Boolean = false,
    val ipAddress: String = "",
    val appVersion: String = "Jayra v1.0",
    val isDesktop: Boolean = false
)

sealed interface ActiveSessionsUiState {
    data object Loading : ActiveSessionsUiState
    data class Success(val sessions: List<Session>) : ActiveSessionsUiState
    data object Empty : ActiveSessionsUiState
    data class Error(val message: String) : ActiveSessionsUiState
}

sealed interface PasscodeUiState {
    data object Loading : PasscodeUiState
    data class Entering(
        val currentInput: String = "",
        val isBiometricEnabled: Boolean = false,
        val isLocked: Boolean = false,
        val lockRemainingSeconds: Int = 0,
        val errorMessage: String? = null
    ) : PasscodeUiState
    data object Success : PasscodeUiState
    data class Error(val message: String) : PasscodeUiState
}

// ================= FASE 13: HELP, ABOUT, PRIVACY POLICY, STICKERS, THEME, LANGUAGE =================

data class FAQ(
    val id: String = "",
    val question: String = "",
    val answer: String = "",
    val category: String = "Umum"
)

sealed interface HelpUiState {
    data object Loading : HelpUiState
    data class Success(
        val faqs: List<FAQ> = emptyList(),
        val searchQuery: String = "",
        val filteredFaqs: List<FAQ> = emptyList(),
        val supportEmail: String = "support@jayra.app",
        val telegramFaqUrl: String = "https://telegram.org/faq"
    ) : HelpUiState
    data object Empty : HelpUiState
    data class Error(val message: String) : HelpUiState
}

data class PrivacySection(
    val id: String = "",
    val title: String = "",
    val content: String = ""
)

sealed interface PrivacyPolicyUiState {
    data object Loading : PrivacyPolicyUiState
    data class Success(
        val title: String = "Kebijakan Privasi Jayra",
        val lastUpdated: String = "November 2025",
        val intro: String = "Jayra berkomitmen melindungi privasi data dan keamanan percakapan Anda dengan standar enkripsi end-to-end terverifikasi.",
        val sections: List<PrivacySection> = emptyList()
    ) : PrivacyPolicyUiState
    data class Error(val message: String) : PrivacyPolicyUiState
}

sealed interface AboutUiState {
    data object Loading : AboutUiState
    data class Success(
        val appName: String = "Jayra",
        val version: String = "1.0.0",
        val buildNumber: String = "100",
        val releaseDate: String = "November 2025",
        val developer: String = "Tim Pengembang Jayra",
        val license: String = "GPL v3.0 / Open Source",
        val githubUrl: String = "https://github.com/jayra/telegram-android",
        val websiteUrl: String = "https://jayra.app"
    ) : AboutUiState
    data class Error(val message: String) : AboutUiState
}

data class Sticker(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val packName: String = "Populer",
    val emoji: String = "😊",
    val isAnimated: Boolean = false
)

data class Gif(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val previewUrl: String = "",
    val width: Int = 150,
    val height: Int = 150
)

sealed interface StickersUiState {
    data object Loading : StickersUiState
    data class Success(
        val selectedTab: Int = 0, // 0: Stiker, 1: GIF
        val stickerPacks: List<String> = emptyList(),
        val selectedPack: String = "Semua",
        val stickers: List<Sticker> = emptyList(),
        val gifs: List<Gif> = emptyList(),
        val searchQuery: String = ""
    ) : StickersUiState
    data object Empty : StickersUiState
    data class Error(val message: String) : StickersUiState
}

data class ThemeOption(
    val id: String,
    val name: String,
    val description: String
)

sealed interface ThemeUiState {
    data object Loading : ThemeUiState
    data class Success(
        val currentTheme: String = "system", // "light", "dark", "system"
        val availableThemes: List<ThemeOption> = emptyList(),
        val dynamicColor: Boolean = true,
        val accentColorHex: Long = 0xFF0088CC
    ) : ThemeUiState
    data class Error(val message: String) : ThemeUiState
}

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val isSelected: Boolean = false
)

sealed interface LanguageUiState {
    data object Loading : LanguageUiState
    data class Success(
        val currentLanguageCode: String = "id",
        val languages: List<LanguageItem> = emptyList(),
        val searchQuery: String = ""
    ) : LanguageUiState
    data class Error(val message: String) : LanguageUiState
}

@Immutable
data class PollOption(
    val text: String = "",
    val voteCount: Int = 0
)

@Immutable
data class Vote(
    val userId: String? = null,
    val optionIndex: Int = 0
)

@Immutable
data class Poll(
    val id: String = "",
    val question: String = "",
    val options: List<PollOption> = emptyList(),
    val isAnonymous: Boolean = true,
    val allowMultiple: Boolean = false,
    val isQuiz: Boolean = false,
    val correctOptionIndex: Int? = null,
    val votes: List<Vote> = emptyList()
) {
    val totalVotes: Int
        get() = options.sumOf { it.voteCount }.coerceAtLeast(votes.size)

    fun percentageFor(index: Int): Float {
        val total = totalVotes
        if (total == 0) return 0f
        val count = options.getOrNull(index)?.voteCount ?: 0
        return count.toFloat() / total
    }
}

@Immutable
data class LocationMessage(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val title: String = ""
)

@Immutable
data class ContactMessage(
    val name: String = "",
    val phone: String = "",
    val avatarUrl: String = ""
)

sealed interface CreatePollUiState {
    data object Idle : CreatePollUiState
    data object Loading : CreatePollUiState
    data object Success : CreatePollUiState
    data class Error(val message: String) : CreatePollUiState
}

sealed interface LocationUiState {
    data object Loading : LocationUiState
    data class Success(
        val lat: Double,
        val lng: Double,
        val address: String = "",
        val accuracyMeters: Float = 10f
    ) : LocationUiState
    data class Error(val message: String) : LocationUiState
}

sealed interface ContactSharingUiState {
    data object Loading : ContactSharingUiState
    data class Success(
        val contacts: List<Contact>,
        val searchQuery: String = ""
    ) : ContactSharingUiState
    data object Empty : ContactSharingUiState
    data class Error(val message: String) : ContactSharingUiState
}

// ================= FASE 15: LOGIN, REGISTRASI, & NOMOR OTOMATIS PER NEGARA =================

@Immutable
data class Country(
    val name: String = "",
    val code: String = "",
    val dialCode: String = ""
)

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@com.google.firebase.firestore.IgnoreExtraProperties
data class UserAccount(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val country: String = "",
    val fcmToken: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: com.google.firebase.Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: com.google.firebase.Timestamp? = null
)

// ================= FASE 16: BOTTOM NAVIGATION BAR & PUSH NOTIFICATIONS (FCM) =================

@Immutable
data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int = 0,
    val lottieAsset: String? = null
)

// ================= FASE 18: SAVED MESSAGES & FORWARD MESSAGES =================

sealed interface SavedMessagesUiState {
    data object Loading : SavedMessagesUiState
    data class Success(
        val chatId: String,
        val headerTitle: String = "Pesan Tersimpan",
        val headerSubtitle: String = "Penyimpanan cloud pribadi",
        val messages: List<Message> = emptyList(),
        val replyTo: Message? = null,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val emptyTitle: String = "Penyimpanan Cloud Anda",
        val emptyHighlights: List<String> = listOf(
            "Simpan pesan, tautan, dan catatan pribadi di sini",
            "Teruskan pesan penting dari obrolan mana pun",
            "Akses aman dari seluruh perangkat Jayra Anda"
        )
    ) : SavedMessagesUiState
    data class Error(val message: String) : SavedMessagesUiState
}

sealed interface ForwardUiState {
    data object Loading : ForwardUiState
    data class Success(
        val sourceMessage: Message,
        val availableChats: List<Chat>,
        val selectedChatIds: Set<String> = emptySet(),
        val searchQuery: String = "",
        val comment: String = "",
        val isForwarding: Boolean = false,
        val savedMessagesChatId: String = ""
    ) : ForwardUiState
    data class Error(val message: String) : ForwardUiState
}

// ================= FASE 19: GLOBAL SEARCH (PENCARIAN GLOBAL) =================

@Immutable
data class GlobalSearchStrings(
    val searchPlaceholder: String = "Cari pesan, kontak, atau grup",
    val idleTitle: String = "Cari di Jayra",
    val idleSubtitle: String = "Temukan pesan, kontak, atau grup",
    val emptyTitlePrefix: String = "Tidak ada hasil untuk",
    val emptySubtitle: String = "Coba kata kunci lain",
    val errorTitle: String = "Gagal mencari",
    val retryButtonLabel: String = "Coba Lagi"
)

sealed interface GlobalSearchUiState {
    data object Idle : GlobalSearchUiState
    data object Loading : GlobalSearchUiState
    data class Success(val results: List<SearchResult>) : GlobalSearchUiState
    data object Empty : GlobalSearchUiState
    data class Error(val message: String) : GlobalSearchUiState
}


// ================= FASE 20: FINAL POLISH, ERROR HANDLING, & RELEASE CONFIG =================

@Immutable
data class SplashUiState(
    val appTitle: String = "Jayra",
    val tagline: String = "Modern, Cepat, Aman",
    val splashDelayMillis: Long = 1500L
)

// ================= FASE 21: RICH TEXT EDITOR & MESSAGE FORMATTING =================

enum class RichTextFormatType {
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    CODE,
    LINK
}

@Immutable
data class RichTextFormatOption(
    val type: RichTextFormatType,
    val label: String,
    val prefix: String,
    val suffix: String,
    val placeholder: String = ""
)

@Immutable
data class RichTextEditorConfig(
    val placeholderText: String = "Ketik pesan...",
    val attachContentDescription: String = "Lampirkan berkas",
    val sendContentDescription: String = "Kirim pesan",
    val cancelReplyContentDescription: String = "Batalkan balasan",
    val replyPrefixLabel: String = "Balas ke",
    val previewHeaderLabel: String = "Pratinjau",
    val formatOptions: List<RichTextFormatOption> = DEFAULT_FORMAT_OPTIONS
) {
    companion object {
        val DEFAULT_FORMAT_OPTIONS: List<RichTextFormatOption> = listOf(
            RichTextFormatOption(
                type = RichTextFormatType.BOLD,
                label = "Tebal",
                prefix = "**",
                suffix = "**"
            ),
            RichTextFormatOption(
                type = RichTextFormatType.ITALIC,
                label = "Miring",
                prefix = "__",
                suffix = "__"
            ),
            RichTextFormatOption(
                type = RichTextFormatType.STRIKETHROUGH,
                label = "Coret",
                prefix = "~~",
                suffix = "~~"
            ),
            RichTextFormatOption(
                type = RichTextFormatType.CODE,
                label = "Kode",
                prefix = "`",
                suffix = "`"
            ),
            RichTextFormatOption(
                type = RichTextFormatType.LINK,
                label = "Tautan",
                prefix = "[",
                suffix = "](https://)",
                placeholder = "teks"
            )
        )
    }
}

// ================= FASE 22: COMMUNITIES & TOPIC TABS =================

@Immutable
@com.google.firebase.firestore.IgnoreExtraProperties
data class Community(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val groupIds: List<String> = emptyList(),
    val creatorId: String = "",
    val memberIds: List<String> = emptyList(),
    @com.google.firebase.firestore.ServerTimestamp val createdAt: com.google.firebase.Timestamp? = null
)

sealed interface CommunityUiState {
    data object Loading : CommunityUiState
    data class Success(val communities: List<Community>) : CommunityUiState
    data object Empty : CommunityUiState
    data class Error(val message: String) : CommunityUiState
}

@Immutable
data class ChatTopic(
    val id: String,
    val title: String
) {
    companion object {
        const val GENERAL_TOPIC_ID = "general"

        val DEFAULT_TOPICS: List<ChatTopic> = listOf(
            ChatTopic(id = "general", title = "General"),
            ChatTopic(id = "topik_1", title = "Topik 1"),
            ChatTopic(id = "topik_2", title = "Topik 2")
        )
    }
}

@Immutable
data class CommunitiesStrings(
    val screenTitle: String = "Komunitas",
    val backContentDescription: String = "Kembali",
    val addContentDescription: String = "Tambah Komunitas",
    val groupCountSuffix: String = "Grup",
    val emptyTitle: String = "Belum Ada Komunitas",
    val emptySubtitle: String = "Gabungkan beberapa grup topik ke dalam satu wadah komunitas Jayra.",
    val createButtonLabel: String = "Buat Komunitas",
    val retryButtonLabel: String = "Coba Lagi",
    val dialogTitle: String = "Komunitas Baru",
    val nameLabel: String = "Nama Komunitas",
    val descriptionLabel: String = "Deskripsi",
    val selectGroupsLabel: String = "Pilih Grup Terhubung",
    val saveLabel: String = "Simpan",
    val cancelLabel: String = "Batal",
    val selectGroupSheetTitle: String = "Grup dalam Komunitas"
)

// ================= FASE 23: STORY ALBUMS =================

@Immutable
@com.google.firebase.firestore.IgnoreExtraProperties
data class Story(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val timestamp: String = "",
    val viewsCount: Int = 0,
    val isViewed: Boolean = false,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val userName: String = "",
    val userAvatarUrl: String = "",
    val caption: String = "",
    val viewerIds: List<String> = emptyList(),
    @com.google.firebase.firestore.ServerTimestamp val createdAt: com.google.firebase.Timestamp? = null
)

sealed interface StoryUiState {
    data object Loading : StoryUiState
    data class Success(
        val stories: List<Story>,
        val selectedAlbumId: String? = null
    ) : StoryUiState
    data object Empty : StoryUiState
    data class Error(val message: String) : StoryUiState
}

sealed interface StoryViewerUiState {
    data object Loading : StoryViewerUiState
    data class Success(
        val stories: List<Story>,
        val currentIndex: Int
    ) : StoryViewerUiState
    data object Empty : StoryViewerUiState
    data class Error(val message: String) : StoryViewerUiState
}

@Immutable
data class StoryAlbumFilter(
    val id: String?,
    val title: String
)

@Immutable
data class StoryStrings(
    val screenTitle: String = "Stories",
    val backContentDescription: String = "Kembali",
    val addStoryContentDescription: String = "Tambah Story",
    val allAlbumsChip: String = "Semua",
    val emptyTitle: String = "Belum Ada Story",
    val emptySubtitle: String = "Bagikan momen foto dan susun dalam album koleksi di profil Jayra Anda.",
    val addStoryButtonLabel: String = "Buat Story Baru",
    val retryButtonLabel: String = "Coba Lagi",
    val viewsSuffix: String = "tayangan",
    val dialogTitle: String = "Story Baru",
    val captionLabel: String = "Keterangan Story",
    val imageUrlLabel: String = "URL Foto Story",
    val albumLabel: String = "Pilih Album Story",
    val presetPhotosLabel: String = "Pilih Template Foto",
    val saveLabel: String = "Bagikan",
    val cancelLabel: String = "Batal",
    val closeViewerContentDescription: String = "Tutup Story",
    val previousHintDescription: String = "Story sebelumnya",
    val nextHintDescription: String = "Story berikutnya",
    val viewerLoadingLabel: String = "Memuat Story...",
    val viewerEmptyLabel: String = "Story tidak ditemukan"
)

// ================= FASE 24: NOTES FOR CONTACTS & BIRTHDAYS =================

@Immutable
@com.google.firebase.firestore.IgnoreExtraProperties
data class ContactNote(
    val contactId: String = "",
    val ownerId: String = "",
    val note: String = "",
    val birthdayFormatted: String = "",
    val birthdayMillis: Long? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: com.google.firebase.Timestamp? = null
)

@Immutable
data class ContactNoteStrings(
    val personalNoteCardTitle: String = "Catatan Pribadi",
    val personalNoteSubtitle: String = "Hanya terlihat oleh Anda dan tersimpan terisolasi di akun Jayra Anda.",
    val personalNotePlaceholder: String = "Tulis catatan khusus tentang kontak ini...",
    val saveNoteButtonLabel: String = "Simpan Catatan",
    val savingLabel: String = "Menyimpan...",
    val savedBadgeLabel: String = "Tersimpan",
    val birthdayCardTitle: String = "Ulang Tahun",
    val birthdayEmptyLabel: String = "Belum diatur",
    val selectBirthdayButtonLabel: String = "Pilih Tanggal",
    val changeBirthdayButtonLabel: String = "Ubah Tanggal",
    val clearBirthdayButtonLabel: String = "Hapus",
    val datePickerTitle: String = "Pilih Tanggal Ulang Tahun",
    val confirmButtonLabel: String = "Simpan",
    val cancelButtonLabel: String = "Batal"
)

// ================= FASE 25: CHECKLISTS (DAFTAR TUGAS DI CHAT) =================

@Immutable
data class ChecklistItem(
    val id: String = "",
    val text: String = "",
    val isDone: Boolean = false
)

@Immutable
data class Checklist(
    val id: String = "",
    val title: String = "",
    val items: List<ChecklistItem> = emptyList(),
    val allowOthersToToggle: Boolean = true
) {
    val completedCount: Int
        get() = items.count { it.isDone }

    val totalCount: Int
        get() = items.size

    val progressFraction: Float
        get() = if (items.isEmpty()) 0f else completedCount.toFloat() / items.size.toFloat()
}

sealed interface CreateChecklistUiState {
    data object Idle : CreateChecklistUiState
    data object Loading : CreateChecklistUiState
    data object Success : CreateChecklistUiState
    data class Error(val message: String) : CreateChecklistUiState
}

@Immutable
data class ChecklistStrings(
    val screenTitle: String = "Daftar Tugas Baru",
    val backContentDescription: String = "Kembali",
    val createActionLabel: String = "BUAT",
    val titleSectionHeader: String = "Judul Daftar Tugas",
    val titlePlaceholder: String = "Contoh: Persiapan Rilis Jayra...",
    val itemsSectionHeader: String = "Daftar Tugas",
    val itemPlaceholderPrefix: String = "Tugas",
    val removeItemContentDescription: String = "Hapus tugas",
    val addItemButtonLabel: String = "Tambah Tugas",
    val settingsSectionHeader: String = "Pengaturan",
    val allowOthersTitle: String = "Izinkan Anggota Menandai Selesai",
    val allowOthersSubtitle: String = "Peserta obrolan dapat mencentang tugas secara kolaboratif",
    val bubbleBadgeLabel: String = "Daftar Tugas",
    val completedProgressSuffix: String = "selesai",
    val attachmentSheetLabel: String = "Tugas"
)

// ================= FASE 26: VOICE TRIMMING (POTONG VOICE NOTE) =================

@Immutable
data class TrimmedAudioResult(
    val outputFilePath: String,
    val durationMillis: Long,
    val sizeBytes: Long,
    val waveform: List<Float>
)

sealed interface VoiceTrimmerUiState {
    data object Loading : VoiceTrimmerUiState
    data class Ready(
        val sourceFilePath: String,
        val totalDurationMillis: Long,
        val startFraction: Float = 0.15f,
        val endFraction: Float = 0.80f,
        val waveform: List<Float> = emptyList(),
        val isPlayingPreview: Boolean = false,
        val playheadFraction: Float = 0.15f,
        val isRecordingMic: Boolean = false,
        val recordingElapsedMillis: Long = 0L
    ) : VoiceTrimmerUiState {
        val startMillis: Long
            get() = (totalDurationMillis * startFraction.coerceIn(0f, 1f)).toLong()

        val endMillis: Long
            get() = (totalDurationMillis * endFraction.coerceIn(0f, 1f)).toLong()

        val trimmedDurationMillis: Long
            get() = (endMillis - startMillis).coerceAtLeast(0L)
    }
    data object Saving : VoiceTrimmerUiState
    data class Error(val message: String) : VoiceTrimmerUiState
}

@Immutable
data class VoiceTrimmerStrings(
    val screenTitle: String = "Potong Voice Note",
    val backContentDescription: String = "Kembali",
    val waveformHeaderTitle: String = "Gelombang Suara",
    val waveformSubtitle: String = "Geser penanda awal dan akhir untuk memotong bagian rekaman terbaik",
    val rangeSliderLabel: String = "Rentang Potong Cepat",
    val startSliderLabel: String = "Batas Awal",
    val endSliderLabel: String = "Batas Akhir",
    val totalDurationLabel: String = "Total Asli",
    val trimmedDurationLabel: String = "Hasil Potong",
    val previewPlayLabel: String = "Putar Potongan",
    val previewPauseLabel: String = "Jeda Pratinjau",
    val recordMicLabel: String = "Rekam Mikrofon",
    val stopRecordLabel: String = "Hentikan Rekaman",
    val resetSampleLabel: String = " Contoh Suara",
    val saveButtonLabel: String = "Simpan",
    val savingLabel: String = "Memotong & Mengirim...",
    val retryLabel: String = "Coba Lagi",
    val permissionDeniedMessage: String = "Izin mikrofon ditolak. Anda tetap dapat memotong contoh rekaman suara.",
    val voiceBubbleLabel: String = "Voice Note",
    val attachmentSheetLabel: String = "Suara"
)


