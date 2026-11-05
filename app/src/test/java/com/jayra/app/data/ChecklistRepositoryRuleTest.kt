package com.jayra.app.data

import com.jayra.app.base.FirestoreEmulatorTestBase
import com.jayra.app.model.ChecklistItem
import com.jayra.app.model.Message
import com.jayra.app.security.EncryptionHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class ChecklistRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndToggleChecklistItem_chatParticipants_syncsChecklistItemsViaFirestore() = runBlocking {
        val bobEmail = "cl_bob_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val bobUid = signInTestUser(bobEmail)

        val aliceEmail = "cl_alice_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = ChatRepository(firestore, auth)

        val chatId = "chat_cl_${UUID.randomUUID().toString().take(8)}"
        val initialItems = listOf(
            ChecklistItem(id = "task_1", text = "Validasi Firestore Rules", isDone = false),
            ChecklistItem(id = "task_2", text = "Uji ChecklistBubble Dark Mode", isDone = false)
        )
        val summaryText = ChatRepository.buildChecklistSummaryText("Sprint Fase 25", initialItems)
        val (cipherText, iv) = EncryptionHelper.encrypt(summaryText)
        val checklistMessage = Message(
            id = "msg_cl_${UUID.randomUUID().toString().take(8)}",
            chatId = chatId,
            senderId = aliceUid,
            senderName = "Alice",
            text = summaryText,
            timestamp = "10:30 AM",
            checklistTitle = "Sprint Fase 25",
            checklistItems = initialItems,
            allowOthersToToggle = true
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.persistChecklistMessageRemote(
                chatId = chatId,
                message = checklistMessage,
                cipherText = cipherText,
                iv = iv,
                timeString = "10:30 AM",
                explicitParticipants = listOf(aliceUid, bobUid)
            )
        }

        // Participant Bob signs in and toggles task_1 to isDone = true
        signInTestUser(bobEmail)
        val bobRepo = ChatRepository(firestore, auth)

        val updatedItems = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.toggleChecklistItemRemote(
                chatId = chatId,
                messageId = checklistMessage.id,
                itemId = "task_1"
            )
        }
        assertTrue(updatedItems.first { it.id == "task_1" }.isDone)

        // Verify live Firestore snapshot deserializes checklistTitle & toggled checklistItems
        val streamedMessages = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.getMessages(chatId).first { list ->
                list.any { it.id == checklistMessage.id && it.checklistItems.any { item -> item.isDone } }
            }
        }
        val fetched = streamedMessages.first { it.id == checklistMessage.id }
        assertEquals("Sprint Fase 25", fetched.checklistTitle)
        assertEquals(2, fetched.checklistItems.size)
        assertTrue(fetched.checklistItems.first { it.id == "task_1" }.isDone)
    }

    @Test
    fun toggleChecklistItem_nonParticipant_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "cl_owner_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = ChatRepository(firestore, auth)

        val chatId = "chat_private_${UUID.randomUUID().toString().take(8)}"
        val msgId = "msg_private_${UUID.randomUUID().toString().take(8)}"
        val items = listOf(ChecklistItem(id = "task_sec", text = "Rahasia Tim", isDone = false))
        val (cipher, iv) = EncryptionHelper.encrypt("☑️ Daftar Tugas: Tim")

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.persistChecklistMessageRemote(
                chatId = chatId,
                message = Message(
                    id = msgId,
                    chatId = chatId,
                    senderId = aliceUid,
                    senderName = "Alice",
                    text = "☑️ Daftar Tugas: Tim",
                    checklistTitle = "Tim",
                    checklistItems = items,
                    allowOthersToToggle = true
                ),
                cipherText = cipher,
                iv = iv,
                timeString = "11:00 AM",
                explicitParticipants = listOf(aliceUid)
            )
        }

        // Outsider Mallory signs in and attempts to toggle Alice's checklist
        val malloryEmail = "cl_outsider_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(malloryEmail)
        val malloryRepo = ChatRepository(firestore, auth)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                malloryRepo.toggleChecklistItemRemote(
                    chatId = chatId,
                    messageId = msgId,
                    itemId = "task_sec",
                    precomputedItems = listOf(ChecklistItem(id = "task_sec", text = "Rahasia Tim", isDone = true))
                )
            }
            fail("Expected PERMISSION_DENIED when non-participant updates checklistItems")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun toggleChecklistItem_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "cl_unauth_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val repo = ChatRepository(firestore, auth)

        val chatId = "chat_unauth_${UUID.randomUUID().toString().take(8)}"
        val msgId = "msg_unauth_${UUID.randomUUID().toString().take(8)}"
        val items = listOf(ChecklistItem(id = "task_u1", text = "Wajib Login", isDone = false))
        val (cipher, iv) = EncryptionHelper.encrypt("☑️ Daftar Tugas: Login")

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.persistChecklistMessageRemote(
                chatId = chatId,
                message = Message(
                    id = msgId,
                    chatId = chatId,
                    senderId = aliceUid,
                    senderName = "Alice",
                    text = "☑️ Daftar Tugas: Login",
                    checklistTitle = "Login",
                    checklistItems = items,
                    allowOthersToToggle = true
                ),
                cipherText = cipher,
                iv = iv,
                timeString = "11:05 AM",
                explicitParticipants = listOf(aliceUid)
            )
        }

        auth.signOut()

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.toggleChecklistItemRemote(
                    chatId = chatId,
                    messageId = msgId,
                    itemId = "task_u1",
                    precomputedItems = listOf(ChecklistItem(id = "task_u1", text = "Wajib Login", isDone = true))
                )
            }
            fail("Expected PERMISSION_DENIED when unauthenticated caller updates checklistItems")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
