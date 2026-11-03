package com.example.data

import com.example.model.Group
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupRepository(
    private val firestore: FirebaseFirestore? = com.example.di.AppModule.firestore
) {
    private val _localGroups = MutableStateFlow<Map<String, Group>>(
        mapOf(
            "chat_2" to Group(
                id = "chat_2",
                name = "Android Developers Community",
                avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400",
                memberCount = 5,
                description = "Komunitas pengembang Android resmi. Tempat berkumpul para developer untuk mendiskusikan Jetpack Compose, Kotlin Coroutines, Room, Material 3, Firebase, performa 120 FPS, dan tren mobile engineering modern.",
                mediaCount = 142,
                starredCount = 15,
                isMuted = false,
                isCustomNotification = true,
                disappearingDuration = "Mati",
                isLocked = false,
                isAdmin = true,
                creatorId = "user_telegram_me",
                members = listOf("user_telegram_me", "pavel_durov", "alex_rivera", "david_kim", "elena_p"),
                memberIds = listOf("user_telegram_me", "pavel_durov", "alex_rivera", "david_kim", "elena_p"),
                adminIds = listOf("user_telegram_me", "pavel_durov"),
                memberAvatars = listOf(
                    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120",
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120",
                    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120"
                )
            ),
            "chat_5" to Group(
                id = "chat_5",
                name = "Design System Team",
                avatarUrl = "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=400",
                memberCount = 3,
                description = "Grup kolaborasi desainer dan engineer untuk sistem desain Telegram UI, token warna, typography, dan komponen interaktif.",
                mediaCount = 56,
                starredCount = 3,
                isMuted = true,
                isCustomNotification = false,
                disappearingDuration = "Mati",
                isLocked = false,
                isAdmin = false,
                creatorId = "david_kim",
                members = listOf("david_kim", "user_telegram_me", "jessica_t"),
                memberIds = listOf("david_kim", "user_telegram_me", "jessica_t"),
                adminIds = listOf("david_kim"),
                memberAvatars = listOf(
                    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120",
                    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120",
                    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=120"
                )
            )
        )
    )

    fun getGroup(groupId: String): Flow<Group> = callbackFlow {
        if (firestore != null) {
            val listener = firestore.collection("groups").document(groupId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        val fallback = _localGroups.value[groupId] ?: getFallbackGroup(groupId)
                        trySend(fallback)
                        return@addSnapshotListener
                    }
                    val remote = snapshot.toObject(Group::class.java)?.copy(id = snapshot.id)
                    if (remote != null) {
                        trySend(remote)
                    } else {
                        trySend(_localGroups.value[groupId] ?: getFallbackGroup(groupId))
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val job = launch {
                _localGroups.collect { map ->
                    trySend(map[groupId] ?: getFallbackGroup(groupId))
                }
            }
            awaitClose { job.cancel() }
        }
    }

    fun getAllGroups(): Flow<List<Group>> = callbackFlow {
        val job = launch {
            _localGroups.collect { map ->
                trySend(map.values.toList())
            }
        }
        awaitClose { job.cancel() }
    }

    private fun getFallbackGroup(groupId: String): Group {
        return Group(
            id = groupId,
            name = "Grup Telegram",
            avatarUrl = "",
            memberCount = 1,
            description = "Deskripsi grup.",
            isAdmin = true
        )
    }

    suspend fun updateMute(groupId: String, muted: Boolean) {
        _localGroups.update { map ->
            val g = map[groupId] ?: getFallbackGroup(groupId)
            map + (groupId to g.copy(isMuted = muted))
        }
        try {
            firestore?.collection("groups")?.document(groupId)?.update("muted", muted)
        } catch (_: Exception) {}
    }

    suspend fun updateCustomNotification(groupId: String, enabled: Boolean) {
        _localGroups.update { map ->
            val g = map[groupId] ?: getFallbackGroup(groupId)
            map + (groupId to g.copy(isCustomNotification = enabled))
        }
        try {
            firestore?.collection("groups")?.document(groupId)?.update("customNotification", enabled)
        } catch (_: Exception) {}
    }

    suspend fun updateLocked(groupId: String, locked: Boolean) {
        _localGroups.update { map ->
            val g = map[groupId] ?: getFallbackGroup(groupId)
            map + (groupId to g.copy(isLocked = locked))
        }
        try {
            firestore?.collection("groups")?.document(groupId)?.update("locked", locked)
        } catch (_: Exception) {}
    }

    suspend fun updateDescription(groupId: String, description: String) {
        _localGroups.update { map ->
            val g = map[groupId] ?: getFallbackGroup(groupId)
            map + (groupId to g.copy(description = description))
        }
        try {
            firestore?.collection("groups")?.document(groupId)?.update("description", description)
        } catch (_: Exception) {}
    }

    suspend fun leaveGroup(groupId: String, userId: String) {
        _localGroups.update { map ->
            val g = map[groupId] ?: return@update map
            map + (groupId to g.copy(
                memberCount = (g.memberCount - 1).coerceAtLeast(0),
                members = g.members - userId
            ))
        }
        try {
            firestore?.collection("groups")?.document(groupId)
                ?.update("members", FieldValue.arrayRemove(userId))
        } catch (_: Exception) {}
    }

    suspend fun addMembers(groupId: String, userIds: List<String>) {
        _localGroups.update { map ->
            val g = map[groupId] ?: return@update map
            val updatedMembers = (g.members + userIds).distinct()
            map + (groupId to g.copy(
                members = updatedMembers,
                memberIds = updatedMembers,
                memberCount = updatedMembers.size
            ))
        }
        try {
            firestore?.collection("groups")?.document(groupId)
                ?.update("members", FieldValue.arrayUnion(*userIds.toTypedArray()))
        } catch (_: Exception) {}
    }

    suspend fun removeMember(groupId: String, userId: String) {
        _localGroups.update { map ->
            val g = map[groupId] ?: return@update map
            val updatedMembers = g.members - userId
            val updatedAdmins = g.adminIds - userId
            map + (groupId to g.copy(
                members = updatedMembers,
                memberIds = updatedMembers,
                adminIds = updatedAdmins,
                memberCount = updatedMembers.size.coerceAtLeast(0)
            ))
        }
        try {
            firestore?.collection("groups")?.document(groupId)
                ?.update("members", FieldValue.arrayRemove(userId))
        } catch (_: Exception) {}
    }

    suspend fun toggleAdmin(groupId: String, userId: String) {
        _localGroups.update { map ->
            val g = map[groupId] ?: return@update map
            val isNowAdmin = !g.adminIds.contains(userId)
            val updatedAdmins = if (isNowAdmin) g.adminIds + userId else g.adminIds - userId
            map + (groupId to g.copy(adminIds = updatedAdmins))
        }
        try {
            val currentAdmins = _localGroups.value[groupId]?.adminIds ?: emptyList()
            firestore?.collection("groups")?.document(groupId)
                ?.update("adminIds", currentAdmins)
        } catch (_: Exception) {}
    }

    suspend fun createGroup(
        name: String,
        memberIds: List<String>,
        avatarUrl: String = "",
        description: String = ""
    ): String {
        val newGroupId = "group_${System.currentTimeMillis()}"
        val allMembers = (listOf("user_telegram_me") + memberIds).distinct()
        val newGroup = Group(
            id = newGroupId,
            name = name,
            avatarUrl = avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400" },
            memberCount = allMembers.size,
            description = description,
            mediaCount = 0,
            starredCount = 0,
            isMuted = false,
            isCustomNotification = false,
            disappearingDuration = "Mati",
            isLocked = false,
            isAdmin = true,
            creatorId = "user_telegram_me",
            members = allMembers,
            memberIds = allMembers,
            adminIds = listOf("user_telegram_me"),
            memberAvatars = listOf(
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120"
            ),
            sensitiveContent = false,
            permissions = mapOf(
                "send_messages" to "Semua Anggota",
                "send_media" to "Semua Anggota",
                "add_members" to "Admin",
                "pin_messages" to "Admin",
                "change_info" to "Admin"
            )
        )
        _localGroups.update { it + (newGroupId to newGroup) }
        try {
            firestore?.collection("groups")?.document(newGroupId)?.set(newGroup)
        } catch (_: Exception) {}
        return newGroupId
    }

    suspend fun updateGroupInfo(groupId: String, name: String, description: String, avatarUrl: String) {
        _localGroups.update { map ->
            val g = map[groupId] ?: getFallbackGroup(groupId)
            map + (groupId to g.copy(
                name = name.ifBlank { g.name },
                description = description,
                avatarUrl = if (avatarUrl.isNotBlank()) avatarUrl else g.avatarUrl
            ))
        }
        try {
            val updates = mutableMapOf<String, Any>("name" to name, "description" to description)
            if (avatarUrl.isNotBlank()) {
                updates["avatarUrl"] = avatarUrl
            }
            firestore?.collection("groups")?.document(groupId)?.update(updates)
        } catch (_: Exception) {}
    }

    suspend fun updateGroupPermission(groupId: String, permissionKey: String, role: String) {
        _localGroups.update { map ->
            val g = map[groupId] ?: getFallbackGroup(groupId)
            val updatedPerms = g.permissions.toMutableMap()
            updatedPerms[permissionKey] = role
            map + (groupId to g.copy(permissions = updatedPerms))
        }
        try {
            firestore?.collection("groups")?.document(groupId)
                ?.update("permissions.$permissionKey", role)
        } catch (_: Exception) {}
    }

    suspend fun updateSensitiveContent(groupId: String, enabled: Boolean) {
        _localGroups.update { map ->
            val g = map[groupId] ?: getFallbackGroup(groupId)
            map + (groupId to g.copy(sensitiveContent = enabled))
        }
        try {
            firestore?.collection("groups")?.document(groupId)
                ?.update("sensitiveContent", enabled)
        } catch (_: Exception) {}
    }

    suspend fun updateDisappearingDuration(groupId: String, duration: String) {
        _localGroups.update { map ->
            val g = map[groupId] ?: getFallbackGroup(groupId)
            map + (groupId to g.copy(disappearingDuration = duration))
        }
        try {
            firestore?.collection("groups")?.document(groupId)
                ?.update("disappearingDuration", duration)
        } catch (_: Exception) {}
    }

    suspend fun deleteGroup(groupId: String) {
        _localGroups.update { map ->
            map - groupId
        }
        try {
            firestore?.collection("groups")?.document(groupId)?.delete()
        } catch (_: Exception) {}
    }
}

