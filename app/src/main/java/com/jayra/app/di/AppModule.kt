package com.jayra.app.di

import android.content.Context
import com.jayra.app.R
import com.jayra.app.data.AuthRepository
import com.jayra.app.data.CallRepository
import com.jayra.app.data.ChannelRepository
import com.jayra.app.data.ChatRepository
import com.jayra.app.data.CommunityRepository
import com.jayra.app.data.ContactNoteRepository
import com.jayra.app.data.ContactRepository
import com.jayra.app.data.GroupRepository
import com.jayra.app.data.StoryRepository
import com.jayra.app.data.UserRepository
import com.jayra.app.security.EncryptionHelper
import com.jayra.app.security.SecurityRepository
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Clean dependency provider providing singletons for repositories and security components.
 * Resolves the named Enterprise Firestore database ID from firebase_applet_config.xml.
 */
object AppModule {
    @Volatile
    private var databaseId: String? = null

    fun init(context: Context) {
        if (databaseId == null) {
            synchronized(this) {
                if (databaseId == null) {
                    databaseId = context.applicationContext.getString(R.string.firestore_database_id)
                }
            }
        }
    }

    fun provideFirestore(context: Context): FirebaseFirestore? {
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        databaseId = dbId
        return try {
            FirebaseFirestore.getInstance(dbId)
        } catch (_: Exception) {
            null
        }
    }

    val firestore: FirebaseFirestore?
        get() {
            val dbId = databaseId ?: return null
            return try {
                FirebaseFirestore.getInstance(dbId)
            } catch (_: Exception) {
                null
            }
        }

    fun provideAuthRepository(context: Context): AuthRepository {
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        return AuthRepository(FirebaseFirestore.getInstance(dbId))
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository()
    }

    val contactRepository: ContactRepository by lazy {
        ContactRepository()
    }

    val contactNoteRepository: ContactNoteRepository by lazy {
        ContactNoteRepository()
    }

    val groupRepository: GroupRepository by lazy {
        GroupRepository()
    }

    val channelRepository: ChannelRepository by lazy {
        ChannelRepository()
    }

    val userRepository: UserRepository by lazy {
        UserRepository()
    }

    val callRepository: CallRepository by lazy {
        CallRepository()
    }

    val communityRepository: CommunityRepository by lazy {
        CommunityRepository()
    }

    val storyRepository: StoryRepository by lazy {
        StoryRepository()
    }

    val securityRepository: SecurityRepository
        get() = SecurityRepository

    val encryptionHelper: EncryptionHelper
        get() = EncryptionHelper
}
