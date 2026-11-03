package com.example.data

import com.example.di.AppModule
import com.example.model.ContactNote
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
import java.util.TimeZone

/**
 * Fase 24: Repository untuk Catatan Pribadi & Ulang Tahun Kontak pada path
 * `users/{uid}/notes/{contactId}` di Firestore.
 */
class ContactNoteRepository(
    private val firestore: FirebaseFirestore? = AppModule.firestore,
    private val auth: FirebaseAuth? = try { Firebase.auth } catch (_: Exception) { null }
) {

    private val _localNotes = MutableStateFlow<Map<String, ContactNote>>(
        mapOf(
            "pavel_durov" to ContactNote(
                contactId = "pavel_durov",
                ownerId = "user_telegram_me",
                note = "Pendiri & arsitek utama protokol enkripsi cepat.",
                birthdayFormatted = "10 Oktober 1984",
                birthdayMillis = 466214400000L
            ),
            "chat_1" to ContactNote(
                contactId = "chat_1",
                ownerId = "user_telegram_me",
                note = "Pendiri & arsitek utama protokol enkripsi cepat.",
                birthdayFormatted = "10 Oktober 1984",
                birthdayMillis = 466214400000L
            )
        )
    )

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk sebelum menyimpan catatan kontak ke Firestore.")
    }

    private fun sanitizeDocId(rawId: String): String {
        return rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "default_contact" }
    }

    fun observeContactNote(contactId: String): Flow<ContactNote> = flow {
        val cleanContactId = sanitizeDocId(contactId)
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val docRef = db.collection("users")
                .document(uid)
                .collection("notes")
                .document(cleanContactId)
            emitAll(
                docRef.snapshots()
                    .map { snapshot ->
                        if (snapshot.exists()) {
                            snapshot.toObject(
                                ContactNote::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            ) ?: ContactNote(contactId = cleanContactId, ownerId = uid)
                        } else {
                            ContactNote(contactId = cleanContactId, ownerId = uid)
                        }
                    }
                    .catch { error ->
                        if (error is Exception) {
                            logFirestoreError(error, OperationType.GET, docRef.path)
                        }
                        throw error
                    }
            )
        } else if (db != null && uid == null) {
            throw IllegalStateException("Sesi autentikasi diperlukan untuk membaca catatan kontak.")
        } else {
            emitAll(
                _localNotes.map { map ->
                    map[cleanContactId] ?: ContactNote(
                        contactId = cleanContactId,
                        ownerId = "user_telegram_me"
                    )
                }
            )
        }
    }

    suspend fun getContactNoteForOwner(
        targetOwnerUid: String? = null,
        contactId: String
    ): Result<ContactNote?> {
        val cleanContactId = sanitizeDocId(contactId)
        val db = firestore
        if (db == null) {
            return Result.success(_localNotes.value[cleanContactId])
        }
        val resolvedOwner = targetOwnerUid ?: requireUserId()
        val docRef = db.collection("users")
            .document(resolvedOwner)
            .collection("notes")
            .document(cleanContactId)
        return try {
            val snapshot = docRef.get().await()
            val note = if (snapshot.exists()) {
                snapshot.toObject(
                    ContactNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(note)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveContactNote(
        contactId: String,
        noteText: String,
        birthdayMillis: Long?,
        birthdayFormatted: String
    ): Result<ContactNote> {
        val cleanContactId = sanitizeDocId(contactId)
        val trimmedNote = noteText.trim().take(2000)
        val trimmedBirthday = birthdayFormatted.trim().take(64)

        val db = firestore
        if (db == null) {
            val updated = ContactNote(
                contactId = cleanContactId,
                ownerId = "user_telegram_me",
                note = trimmedNote,
                birthdayFormatted = trimmedBirthday,
                birthdayMillis = birthdayMillis
            )
            _localNotes.update { it + (cleanContactId to updated) }
            return Result.success(updated)
        }

        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("notes")
            .document(cleanContactId)

        val payload = mapOf(
            "contactId" to cleanContactId,
            "ownerId" to uid,
            "note" to trimmedNote,
            "birthdayFormatted" to trimmedBirthday,
            "birthdayMillis" to birthdayMillis,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val saved = ContactNote(
                contactId = cleanContactId,
                ownerId = uid,
                note = trimmedNote,
                birthdayFormatted = trimmedBirthday,
                birthdayMillis = birthdayMillis
            )
            _localNotes.update { it + (cleanContactId to saved) }
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logFirestoreError(error: Exception, operationType: OperationType, path: String?) {
        val structured = handleFirestoreError(error, operationType, path)
        Timber.e(error, "FirestoreError: %s", structured)
    }

    companion object {
        fun formatBirthdayMillis(millis: Long?): String {
            if (millis == null) return ""
            val formatter = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID")).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return formatter.format(Date(millis))
        }
    }
}
