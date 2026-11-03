package com.example.data

import com.example.di.AppModule
import com.example.model.Community
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID

/**
 * Fase 22.2: Repository untuk koleksi Firestore `communities`.
 * Mengisolasi akses berdasarkan keanggotaan (`memberIds` array-contains `uid`).
 */
class CommunityRepository(
    private val firestore: FirebaseFirestore? = AppModule.firestore,
    private val auth: FirebaseAuth? = try { Firebase.auth } catch (_: Exception) { null }
) {

    private val _localCommunities = MutableStateFlow(
        listOf(
            Community(
                id = "comm_jayra_devs",
                name = "Komunitas Developer Jayra",
                description = "Pusat diskusi arsitektur Kotlin, Jetpack Compose, dan WebRTC.",
                avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200",
                groupIds = listOf("chat_2", "chat_5"),
                creatorId = "user_telegram_me",
                memberIds = listOf("user_telegram_me")
            ),
            Community(
                id = "comm_ui_design",
                name = "Desainer Produk Nusantara",
                description = "Kolaborasi sistem desain Material 3, tipografi, dan riset UX.",
                avatarUrl = "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=200",
                groupIds = listOf("chat_5"),
                creatorId = "user_telegram_me",
                memberIds = listOf("user_telegram_me")
            )
        )
    )

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk sebelum mengakses komunitas Firestore.")
    }

    /**
     * Mengamati daftar komunitas milik pengguna saat ini dari koleksi `communities`.
     */
    fun observeCommunities(): Flow<List<Community>> = flow {
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val path = "communities"
            emitAll(
                db.collection(path)
                    .whereArrayContains("memberIds", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            doc.toObject(
                                Community::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )?.copy(id = doc.id)
                        }
                    }
                    .catch { error ->
                        if (error is Exception) {
                            logFirestoreError(error, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        } else if (db != null && uid == null) {
            throw IllegalStateException("Sesi autentikasi diperlukan untuk memuat komunitas.")
        } else {
            emitAll(_localCommunities)
        }
    }

    suspend fun getCommunityById(communityId: String): Result<Community?> {
        val cleanId = communityId.trim()
        if (cleanId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID komunitas tidak valid."))
        }
        val db = firestore
        if (db == null) {
            return Result.success(_localCommunities.value.find { it.id == cleanId })
        }

        val docRef = db.collection("communities").document(cleanId)
        return try {
            val snapshot = docRef.get().await()
            val community = if (snapshot.exists()) {
                snapshot.toObject(
                    Community::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.copy(id = snapshot.id)
            } else {
                null
            }
            Result.success(community)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createCommunity(
        name: String,
        description: String,
        groupIds: List<String>,
        avatarUrl: String = ""
    ): Result<Community> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama komunitas wajib diisi."))
        }
        val cleanDesc = description.trim().take(1000)
        val cleanGroups = groupIds.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(50)
        val cleanAvatar = avatarUrl.trim().take(500)
        val newId = "comm_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        val db = firestore
        if (db == null) {
            val localItem = Community(
                id = newId,
                name = trimmedName.take(120),
                description = cleanDesc,
                avatarUrl = cleanAvatar,
                groupIds = cleanGroups,
                creatorId = "user_telegram_me",
                memberIds = listOf("user_telegram_me")
            )
            _localCommunities.update { listOf(localItem) + it }
            return Result.success(localItem)
        }

        val uid = requireUserId()
        val docRef = db.collection("communities").document(newId)
        val payload = mutableMapOf<String, Any>(
            "id" to newId,
            "name" to trimmedName.take(120),
            "description" to cleanDesc,
            "avatarUrl" to cleanAvatar,
            "groupIds" to cleanGroups,
            "creatorId" to uid,
            "memberIds" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val created = Community(
                id = newId,
                name = trimmedName.take(120),
                description = cleanDesc,
                avatarUrl = cleanAvatar,
                groupIds = cleanGroups,
                creatorId = uid,
                memberIds = listOf(uid)
            )
            _localCommunities.update { current ->
                listOf(created) + current.filter { it.id != newId }
            }
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialCommunitiesIfEmpty() {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: return
        try {
            val existing = db.collection("communities")
                .whereArrayContains("memberIds", uid)
                .limit(1)
                .get()
                .await()
            if (existing.isEmpty) {
                createCommunity(
                    name = "Komunitas Developer Jayra",
                    description = "Pusat diskusi arsitektur Kotlin, Jetpack Compose, dan WebRTC.",
                    groupIds = listOf("chat_2", "chat_5"),
                    avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200"
                )
                createCommunity(
                    name = "Desainer Produk Nusantara",
                    description = "Kolaborasi sistem desain Material 3, tipografi, dan riset UX.",
                    groupIds = listOf("chat_5"),
                    avatarUrl = "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=200"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, "communities")
        }
    }

    private fun logFirestoreError(error: Exception, operationType: OperationType, path: String?) {
        val structured = handleFirestoreError(error, operationType, path)
        Timber.e(error, "FirestoreError: %s", structured)
    }
}
