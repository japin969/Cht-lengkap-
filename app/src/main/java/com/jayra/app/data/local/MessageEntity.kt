package com.jayra.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = "",

    @ColumnInfo(name = "chatId")
    val chatId: String = "",

    @ColumnInfo(name = "senderId")
    val senderId: String = "",

    @ColumnInfo(name = "senderName")
    val senderName: String = "",

    @ColumnInfo(name = "receiverId")
    val receiverId: String = "",

    @ColumnInfo(name = "text")
    val text: String = "",

    @ColumnInfo(name = "type")
    val type: String = "TEXT",

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = 0L,

    @ColumnInfo(name = "status")
    val status: String = "SENT",

    @ColumnInfo(name = "replyToId")
    val replyToId: String? = null,

    @ColumnInfo(name = "replyToText")
    val replyToText: String? = null,

    @ColumnInfo(name = "replyToSender")
    val replyToSender: String? = null,

    @ColumnInfo(name = "forwardedFrom")
    val forwardedFrom: String? = null,

    @ColumnInfo(name = "topicId")
    val topicId: String = "general",

    @ColumnInfo(name = "checklistTitle")
    val checklistTitle: String? = null,

    @ColumnInfo(name = "checklistItems")
    val checklistItems: String = "",

    @ColumnInfo(name = "allowOthersToToggle")
    val allowOthersToToggle: Boolean = true,

    @ColumnInfo(name = "voiceFilePath")
    val voiceFilePath: String? = null,

    @ColumnInfo(name = "voiceDurationMillis")
    val voiceDurationMillis: Long? = null,

    @ColumnInfo(name = "voiceWaveform")
    val voiceWaveform: String = "",

    @ColumnInfo(name = "mediaUrl")
    val mediaUrl: String? = null,

    @ColumnInfo(name = "mediaType")
    val mediaType: String? = null,

    @ColumnInfo(name = "stickerUrl")
    val stickerUrl: String? = null,

    @ColumnInfo(name = "gifUrl")
    val gifUrl: String? = null,

    @ColumnInfo(name = "linkPreviewTitle")
    val linkPreviewTitle: String? = null,

    @ColumnInfo(name = "linkPreviewDescription")
    val linkPreviewDescription: String? = null,

    @ColumnInfo(name = "linkPreviewImageUrl")
    val linkPreviewImageUrl: String? = null,

    @ColumnInfo(name = "systemText")
    val systemText: String? = null,

    @ColumnInfo(name = "disappearingSeconds")
    val disappearingSeconds: Long? = null
)
