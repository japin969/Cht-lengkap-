package com.jayra.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    @ColumnInfo(name = "chatId")
    val chatId: String = "",

    @ColumnInfo(name = "otherUserId")
    val otherUserId: String = "",

    @ColumnInfo(name = "otherUserName")
    val otherUserName: String = "",

    @ColumnInfo(name = "avatarUrl")
    val avatarUrl: String = "",

    @ColumnInfo(name = "lastMessage")
    val lastMessage: String = "",

    @ColumnInfo(name = "lastMessageTime")
    val lastMessageTime: Long = 0L,

    @ColumnInfo(name = "unreadCount")
    val unreadCount: Int = 0,

    @ColumnInfo(name = "isPinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "isMuted")
    val isMuted: Boolean = false,

    @ColumnInfo(name = "isRead")
    val isRead: Boolean = true,

    @ColumnInfo(name = "isGroup")
    val isGroup: Boolean = false,

    @ColumnInfo(name = "hasTopics")
    val hasTopics: Boolean = false,

    @ColumnInfo(name = "isChannel")
    val isChannel: Boolean = false,

    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "hasDraft")
    val hasDraft: Boolean = false,

    @ColumnInfo(name = "draftText")
    val draftText: String = "",

    @ColumnInfo(name = "isOnline")
    val isOnline: Boolean = false,

    @ColumnInfo(name = "lastMessageStatus")
    val lastMessageStatus: String = "SENT",

    @ColumnInfo(name = "lastMessageIsOutgoing")
    val lastMessageIsOutgoing: Boolean = false,

    @ColumnInfo(name = "participants")
    val participants: String = "",

    @ColumnInfo(name = "timestampMillis")
    val timestampMillis: Long = 0L
)
