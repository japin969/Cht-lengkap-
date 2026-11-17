package com.jayra.app.data

import android.content.Context
import com.jayra.app.R
import com.jayra.app.model.UserAccount
import com.google.firebase.Firebase
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus terautentikasi sebelum mengakses Firestore.")
    }

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid
                ?: return Result.failure(IllegalStateException("Firebase Auth tidak mengembalikan user"))
            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid
                ?: return Result.failure(IllegalStateException("Firebase Auth tidak mengembalikan user"))
            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            // CSO APPROVED: ActionCodeSettings untuk redirect ke landing page aman
            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://jayraresetpass.pages.dev/reset-password")
                .setHandleCodeInApp(false) // WAJIB FALSE untuk Plan B (Web Flow)
                .build()

            auth.sendPasswordResetEmail(email, actionCodeSettings).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveRegisteredUser(
        email: String,
        phoneNumber: String,
        countryName: String,
        username: String = "",
        name: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val docRef = firestore.collection("users").document(uid)
        val cleanUsername = username.trim().removePrefix("@").lowercase()
        val cleanName = name.trim().ifBlank { cleanUsername }
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("email", email)
            put("phoneNumber", phoneNumber)
            put("country", countryName)
            put("createdAt", FieldValue.serverTimestamp())
            if (cleanUsername.isNotEmpty()) {
                put("username", cleanUsername.take(64))
            }
            if (cleanName.isNotEmpty()) {
                put("name", cleanName.take(100))
            }
        }
        return try {
            docRef.set(payload).await()
            if (cleanName.isNotEmpty()) {
                updateAuthDisplayName(cleanName)
            }
            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileIdentity(
        username: String,
        name: String = username,
        phoneNumber: String? = null,
        countryName: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = firestore.collection("users").document(uid)
        val cleanUsername = username.trim().removePrefix("@").lowercase().take(64)
        val cleanName = name.trim().ifBlank { cleanUsername }.take(100)
        val updates = buildMap<String, Any> {
            if (cleanUsername.isNotEmpty()) put("username", cleanUsername)
            if (cleanName.isNotEmpty()) put("name", cleanName)
            if (!phoneNumber.isNullOrBlank()) put("phoneNumber", phoneNumber.trim().take(24))
            if (!countryName.isNullOrBlank()) put("country", countryName.trim().take(100))
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.update(updates).await()
            if (cleanName.isNotEmpty()) {
                updateAuthDisplayName(cleanName)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAuthDisplayName(displayName: String) {
        val user = auth.currentUser ?: return
        val clean = displayName.trim().take(100)
        if (clean.isEmpty() || user.displayName == clean) return
        try {
            user.updateProfile(
                userProfileChangeRequest {
                    this.displayName = clean
                }
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort FirebaseAuth profile sync
        }
    }

    suspend fun getUserAccount(targetUserId: String? = null): Result<UserAccount?> {
        val uid = targetUserId ?: requireUserId()
        val docRef = firestore.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            val account = snapshot.toObject(
                UserAccount::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(account)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            return Result.failure(e)
        }
    }

    fun observeUserAccount(): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        val docRef = firestore.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserAccount::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }
}
