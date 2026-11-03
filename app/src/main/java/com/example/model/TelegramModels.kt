package com.example.model

import androidx.compose.runtime.Immutable

enum class ChatFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    GROUPS("Groups"),
    CHANNELS("Channels")
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
    val userReacted: Boolean = false
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
    val isChannel: Boolean = false,
    val participants: List<String> = emptyList(),
    val isArchived: Boolean = false
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
    val reactions: List<Reaction> = emptyList()
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
    val permissions: Map<String, String> = mapOf(
        "send_messages" to "Semua Anggota",
        "send_media" to "Semua Anggota",
        "add_members" to "Admin",
        "pin_messages" to "Admin",
        "change_info" to "Admin"
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

enum class SearchFilter(val label: String) {
    ALL("Semua"),
    CHATS("Obrolan"),
    MESSAGES("Pesan"),
    CONTACTS("Kontak"),
    GROUPS("Grup"),
    CHANNELS("Saluran")
}

enum class SearchResultType {
    CHAT,
    MESSAGE,
    CONTACT,
    GROUP,
    CHANNEL
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
    val targetId: String = id
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



