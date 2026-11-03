package com.example.data

import com.example.di.AppModule
import com.example.model.Story
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Fase 23: Repository untuk koleksi Firestore `stories` (Story Albums).
 * Menggunakan instance database bernama dari AppModule dan menerapkan isolasi autentikasi penuh.
 */
class StoryRepository(
    private val firestore: FirebaseFirestore? = AppModule.firestore,
    private val auth: FirebaseAuth? = try { Firebase.auth } catch (_: Exception) { null }
) {

    private val _localStories = MutableStateFlow(defaultSeedStories("user_telegram_me"))

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk sebelum mengakses koleksi stories Firestore.")
    }

    /**
     * Mengamati aliran real-time seluruh dokumen pada `firestore.collection("stories")`.
     */
    fun observeStories(): Flow<List<Story>> = flow {
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val path = "stories"
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            val raw = doc.toObject(
                                Story::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                            raw?.copy(
                                id = doc.id,
                                userName = raw.userName.ifBlank { "Pengguna Jayra" },
                                isViewed = raw.isViewed || (uid in raw.viewerIds)
                            )
                        }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
                    .catch { error ->
                        if (error is Exception) {
                            logFirestoreError(error, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        } else if (db != null && uid == null) {
            throw IllegalStateException("Sesi autentikasi diperlukan untuk memuat daftar story.")
        } else {
            emitAll(_localStories)
        }
    }

    suspend fun getStoryById(storyId: String): Result<Story?> {
        val cleanId = storyId.trim()
        if (cleanId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID story tidak valid."))
        }
        val db = firestore
        if (db == null) {
            return Result.success(_localStories.value.find { it.id == cleanId })
        }

        val uid = auth?.currentUser?.uid
        val docRef = db.collection("stories").document(cleanId)
        return try {
            val snapshot = docRef.get().await()
            val story = if (snapshot.exists()) {
                val raw = snapshot.toObject(
                    Story::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                raw?.copy(
                    id = snapshot.id,
                    userName = raw.userName.ifBlank { "Pengguna Jayra" },
                    isViewed = raw.isViewed || (uid != null && uid in raw.viewerIds)
                )
            } else {
                null
            }
            Result.success(story)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createStory(
        imageUrl: String,
        caption: String = "",
        albumId: String? = null,
        albumTitle: String? = null,
        userName: String = "",
        userAvatarUrl: String = ""
    ): Result<Story> {
        val cleanImageUrl = imageUrl.trim().take(1000)
        if (cleanImageUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("URL gambar story wajib diisi."))
        }
        val cleanCaption = caption.trim().take(500)
        val cleanAlbumId = albumId?.trim()?.takeIf { it.isNotBlank() }?.take(128)
        val cleanAlbumTitle = albumTitle?.trim()?.takeIf { it.isNotBlank() }?.take(100)
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        val newId = "story_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        val db = firestore
        if (db == null) {
            val resolvedName = userName.trim().ifBlank { "Saya" }.take(100)
            val localItem = Story(
                id = newId,
                userId = "user_telegram_me",
                imageUrl = cleanImageUrl,
                timestamp = formattedTime,
                viewsCount = 0,
                isViewed = false,
                albumId = cleanAlbumId,
                albumTitle = cleanAlbumTitle,
                userName = resolvedName,
                userAvatarUrl = userAvatarUrl.trim().take(500),
                caption = cleanCaption,
                viewerIds = emptyList()
            )
            _localStories.update { listOf(localItem) + it }
            return Result.success(localItem)
        }

        val uid = requireUserId()
        val resolvedName = userName.trim().ifBlank {
            auth?.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: auth?.currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Pengguna Jayra"
        }.take(100)
        val cleanAvatar = userAvatarUrl.trim().take(500)

        val docRef = db.collection("stories").document(newId)
        val payload = mutableMapOf<String, Any?>(
            "id" to newId,
            "userId" to uid,
            "userName" to resolvedName,
            "userAvatarUrl" to cleanAvatar,
            "imageUrl" to cleanImageUrl,
            "caption" to cleanCaption,
            "timestamp" to formattedTime,
            "viewsCount" to 0,
            "isViewed" to false,
            "albumId" to cleanAlbumId,
            "albumTitle" to cleanAlbumTitle,
            "viewerIds" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val created = Story(
                id = newId,
                userId = uid,
                imageUrl = cleanImageUrl,
                timestamp = formattedTime,
                viewsCount = 0,
                isViewed = false,
                albumId = cleanAlbumId,
                albumTitle = cleanAlbumTitle,
                userName = resolvedName,
                userAvatarUrl = cleanAvatar,
                caption = cleanCaption,
                viewerIds = emptyList()
            )
            _localStories.update { current ->
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

    suspend fun markStoryViewed(storyId: String): Result<Unit> {
        val cleanId = storyId.trim()
        if (cleanId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID story tidak valid."))
        }
        val db = firestore
        if (db == null) {
            _localStories.update { list ->
                list.map { item ->
                    if (item.id == cleanId && !item.isViewed) {
                        item.copy(isViewed = true, viewsCount = item.viewsCount + 1)
                    } else {
                        item
                    }
                }
            }
            return Result.success(Unit)
        }

        val uid = requireUserId()
        val docRef = db.collection("stories").document(cleanId)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.success(Unit)
            }
            val current = snapshot.toObject(
                Story::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.success(Unit)

            if (uid in current.viewerIds && current.isViewed) {
                return Result.success(Unit)
            }

            val updatedViewers = (current.viewerIds + uid).distinct().take(100)
            val nextViewsCount = if (uid in current.viewerIds) {
                current.viewsCount
            } else {
                current.viewsCount + 1
            }

            docRef.update(
                mapOf(
                    "viewsCount" to nextViewsCount,
                    "isViewed" to true,
                    "viewerIds" to updatedViewers
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> {
        val cleanId = storyId.trim()
        if (cleanId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID story tidak valid."))
        }
        val db = firestore
        if (db == null) {
            _localStories.update { list -> list.filterNot { it.id == cleanId } }
            return Result.success(Unit)
        }

        requireUserId()
        val docRef = db.collection("stories").document(cleanId)
        return try {
            docRef.delete().await()
            _localStories.update { list -> list.filterNot { it.id == cleanId } }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun seedInitialStoriesIfEmpty() {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: return
        try {
            val existing = db.collection("stories")
                .limit(1)
                .get()
                .await()
            if (existing.isEmpty) {
                val seeds = defaultSeedStories(uid)
                for (seed in seeds) {
                    val docRef = db.collection("stories").document(seed.id)
                    docRef.set(
                        mapOf(
                            "id" to seed.id,
                            "userId" to uid,
                            "userName" to seed.userName,
                            "userAvatarUrl" to seed.userAvatarUrl,
                            "imageUrl" to seed.imageUrl,
                            "caption" to seed.caption,
                            "timestamp" to seed.timestamp,
                            "viewsCount" to seed.viewsCount,
                            "isViewed" to false,
                            "albumId" to seed.albumId,
                            "albumTitle" to seed.albumTitle,
                            "viewerIds" to emptyList<String>(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, "stories")
        }
    }

    private fun logFirestoreError(error: Exception, operationType: OperationType, path: String?) {
        val structured = handleFirestoreError(error, operationType, path)
        Timber.e(error, "FirestoreError: %s", structured)
    }

    companion object {
        val PRESET_STORY_PHOTOS: List<Pair<String, String>> = listOf(
            "Studio Malam" to "https://images.unsplash.com/photo-1518770660439-4636190af475?w=900",
            "Kolaborasi Tim" to "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=900",
            "Desain Sistem" to "https://images.unsplash.com/photo-1558655146-d09347e92766?w=900",
            "Pegunungan" to "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900",
            "Kopi Pagi" to "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900",
            "Kota Modern" to "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=900"
        )

        val PRESET_ALBUMS: List<Pair<String, String>> = listOf(
            "album_rilis" to "Rilis Jayra",
            "album_tim" to "Momen Tim",
            "album_arsitektur" to "Arsitektur"
        )

        fun defaultSeedStories(ownerUid: String): List<Story> = listOf(
            Story(
                id = "story_jayra_1",
                userId = ownerUid,
                imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=900",
                timestamp = "09:15 AM",
                viewsCount = 48,
                isViewed = false,
                albumId = "album_rilis",
                albumTitle = "Rilis Jayra",
                userName = "Pavel Durov",
                userAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                caption = "Peluncuran mesin enkripsi kecepatan tinggi di rilis Jayra terbaru."
            ),
            Story(
                id = "story_jayra_2",
                userId = ownerUid,
                imageUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=900",
                timestamp = "09:42 AM",
                viewsCount = 31,
                isViewed = false,
                albumId = "album_tim",
                albumTitle = "Momen Tim",
                userName = "Sarah Connor",
                userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                caption = "Sesi sinkronisasi desain antarmuka Material 3 bersama tim produk."
            ),
            Story(
                id = "story_jayra_3",
                userId = ownerUid,
                imageUrl = "https://images.unsplash.com/photo-1558655146-d09347e92766?w=900",
                timestamp = "10:05 AM",
                viewsCount = 27,
                isViewed = false,
                albumId = "album_arsitektur",
                albumTitle = "Arsitektur",
                userName = "Alexei",
                userAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                caption = "Eksplorasi tipografi dan tata letak responsif multi-perangkat."
            ),
            Story(
                id = "story_jayra_4",
                userId = ownerUid,
                imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900",
                timestamp = "10:28 AM",
                viewsCount = 64,
                isViewed = true,
                albumId = "album_tim",
                albumTitle = "Momen Tim",
                userName = "Nadia Putri",
                userAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200",
                caption = "Istirahat akhir pekan di lembah pegunungan sebelum sprint rilis."
            ),
            Story(
                id = "story_jayra_5",
                userId = ownerUid,
                imageUrl = "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900",
                timestamp = "11:00 AM",
                viewsCount = 19,
                isViewed = false,
                albumId = "album_rilis",
                albumTitle = "Rilis Jayra",
                userName = "Budi Santoso",
                userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                caption = "Kopi pagi menemani pengujian otomatis Story Albums."
            ),
            Story(
                id = "story_jayra_6",
                userId = ownerUid,
                imageUrl = "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=900",
                timestamp = "11:20 AM",
                viewsCount = 52,
                isViewed = true,
                albumId = "album_arsitektur",
                albumTitle = "Arsitektur",
                userName = "Rina Wijaya",
                userAvatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200",
                caption = "Panorama malam pusat data cloud Asia Timur."
            )
        )
    }
}
