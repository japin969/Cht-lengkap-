package com.example.di

import com.example.data.CallRepository
import com.example.data.ChannelRepository
import com.example.data.ChatRepository
import com.example.data.ContactRepository
import com.example.data.GroupRepository
import com.example.data.UserRepository
import com.example.security.EncryptionHelper
import com.example.security.SecurityRepository

/**
 * Clean dependency provider providing singletons for repositories and security components.
 */
object AppModule {
    val chatRepository: ChatRepository by lazy {
        ChatRepository()
    }

    val contactRepository: ContactRepository by lazy {
        ContactRepository()
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

    val securityRepository: SecurityRepository
        get() = SecurityRepository

    val encryptionHelper: EncryptionHelper
        get() = EncryptionHelper
}
