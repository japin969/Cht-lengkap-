package com.example.di

import android.content.Context
import com.example.R
import com.example.data.AuthRepository
import com.example.data.CallRepository
import com.example.data.ChannelRepository
import com.example.data.ChatRepository
import com.example.data.CommunityRepository
import com.example.data.ContactNoteRepository
import com.example.data.ContactRepository
import com.example.data.GroupRepository
import com.example.data.StoryRepository
import com.example.data.UserRepository
import com.example.security.EncryptionHelper
import com.example.security.SecurityRepository
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
