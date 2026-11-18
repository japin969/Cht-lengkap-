package com.jayra.app.data.local

import androidx.room.TypeConverter
import com.jayra.app.model.ChecklistItem
import com.jayra.app.model.MessageStatus
import com.jayra.app.model.Reaction

class Converters {

    @TypeConverter
    fun listStringToDb(list: List<String>?): String? = list?.joinToString(",")

    @TypeConverter
    fun listStringFromDb(value: String?): List<String> =
        value?.split(",").filter { it.isNotEmpty() } ?: emptyList()

    @TypeConverter
    fun listFloatToDb(list: List<Float>?): String? = list?.joinToString(",")

    @TypeConverter
    fun listFloatFromDb(value: String?): List<Float> =
        value?.split(",").filter { it.isNotEmpty() }.map { it.toFloat() } ?: emptyList()

    @TypeConverter
    fun checklistItemsToDb(items: List<ChecklistItem>?): String? =
        items?.joinToString("|") { "${it.id}~${it.text}~${it.isDone}" }

    @TypeConverter
    fun checklistItemsFromDb(value: String?): List<ChecklistItem> =
        value?.split("|").filter { it.isNotEmpty() }.map { part ->
            val (id, text, isDone) = part.split("~")
            ChecklistItem(id = id, text = text, isDone = isDone.toBoolean())
        } ?: emptyList()

    @TypeConverter
    fun reactionsToDb(reactions: List<Reaction>?): String? =
        reactions?.joinToString("|") { "${it.emoji}~${it.count}~${it.userReacted}~${it.userIds.joinToString(";")}" }

    @TypeConverter
    fun reactionsFromDb(value: String?): List<Reaction> =
        value?.split("|").filter { it.isNotEmpty() }.map { part ->
            val cols = part.split("~")
            Reaction(
                emoji = cols.getOrElse(0) { "" },
                count = cols.getOrElse(1) { "0" }.toInt(),
                userReacted = cols.getOrElse(2) { "false" }.toBoolean(),
                userIds = cols.getOrElse(3) { "" }.split(";").filter { it.isNotEmpty() }
            )
        } ?: emptyList()

    @TypeConverter
    fun messageStatusToDb(status: MessageStatus?): String? = status?.name

    @TypeConverter
    fun messageStatusFromDb(value: String?): MessageStatus =
        value?.let { MessageStatus.valueOf(it) } ?: MessageStatus.SENT
}
