package com.jayra.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    fun getChat(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getNonArchivedChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Transaction
    suspend fun upsertChat(
        chatId: String,
        otherUserId: String,
        otherUserName: String,
        avatarUrl: String,
        lastMessage: String,
        lastMessageTime: Long,
        unreadCount: Int,
        isPinned: Boolean,
        isMuted: Boolean,
        isRead: Boolean,
        isGroup: Boolean,
        hasTopics: Boolean,
        isChannel: Boolean,
        isArchived: Boolean,
        isFavorite: Boolean,
        hasDraft: Boolean,
        draftText: String,
        isOnline: Boolean,
        lastMessageStatus: String,
        lastMessageIsOutgoing: Boolean,
        participants: List<String>,
        timestampMillis: Long
    ) {
        insertChat(
            ChatEntity(
                chatId = chatId,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                avatarUrl = avatarUrl,
                lastMessage = lastMessage,
                lastMessageTime = lastMessageTime,
                unreadCount = unreadCount,
                isPinned = isPinned,
                isMuted = isMuted,
                isRead = isRead,
                isGroup = isGroup,
                hasTopics = hasTopics,
                isChannel = isChannel,
                isArchived = isArchived,
                isFavorite = isFavorite,
                hasDraft = hasDraft,
                draftText = draftText,
                isOnline = isOnline,
                lastMessageStatus = lastMessageStatus,
                lastMessageIsOutgoing = lastMessageIsOutgoing,
                participants = Converters().listStringToDb(participants) ?: "",
                timestampMillis = timestampMillis
            )
        )
    }

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun resetUnreadCount(chatId: String)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :lastMessageTime, unreadCount = unreadCount + 1, isRead = 0 WHERE chatId = :chatId")
    suspend fun onNewMessage(
        chatId: String,
        lastMessage: String,
        lastMessageTime: Long
    )

    @Query("UPDATE messages SET status = :newStatus WHERE chatId = :chatId AND senderId != :currentUserId AND status IN ('SENDING', 'SENT', 'DELIVERED')")
    suspend fun markAllMessagesStatus(chatId: String, currentUserId: String, newStatus: String)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: String)
}
