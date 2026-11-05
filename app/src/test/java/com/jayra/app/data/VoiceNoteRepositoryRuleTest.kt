package com.jayra.app.data

import com.jayra.app.base.FirestoreEmulatorTestBase
import com.jayra.app.model.Message
import com.jayra.app.security.EncryptionHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class VoiceNoteRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun persistVoiceNoteMessageRemote_participant_streamsDurationAndWaveformToPeer() = runBlocking {
        val bobEmail = "vn_bob_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val bobUid = signInTestUser(bobEmail)

        val aliceEmail = "vn_alice_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = ChatRepository(firestore, auth)

        val chatId = "chat_vn_${UUID.randomUUID().toString().take(8)}"
        val summary = ChatRepository.buildVoiceNoteSummaryText(4_800L)
        val (cipher, iv) = EncryptionHelper.encrypt(summary)
        val waveform = listOf(0.24f, 0.58f, 0.91f, 0.44f, 0.76f)
        val voiceMsg = Message(
            id = "msg_vn_${UUID.randomUUID().toString().take(8)}",
            chatId = chatId,
            senderId = aliceUid,
            senderName = "Alice",
            text = summary,
            timestamp = "11:20 AM",
            voiceDurationMillis = 4_800L,
            voiceWaveform = waveform
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.persistVoiceNoteMessageRemote(
                chatId = chatId,
                message = voiceMsg,
                cipherText = cipher,
                iv = iv,
                timeString = "11:20 AM",
                explicitParticipants = listOf(aliceUid, bobUid)
            )
        }

        // Peer Bob signs in and receives voiceDurationMillis & voiceWaveform
        signInTestUser(bobEmail)
        val bobRepo = ChatRepository(firestore, auth)

        val streamed = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.getMessages(chatId).first { list ->
                list.any { it.id == voiceMsg.id && it.voiceDurationMillis == 4_800L }
            }
        }
        val received = streamed.first { it.id == voiceMsg.id }
        assertEquals(4_800L, received.voiceDurationMillis)
        assertEquals(5, received.voiceWaveform.size)
        assertEquals(0.91f, received.voiceWaveform[2], 0.01f)
    }

    @Test
    fun readVoiceNoteMessage_nonParticipant_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "vn_owner_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = ChatRepository(firestore, auth)

        val chatId = "chat_vn_priv_${UUID.randomUUID().toString().take(8)}"
        val msgId = "msg_vn_priv_${UUID.randomUUID().toString().take(8)}"
        val summary = ChatRepository.buildVoiceNoteSummaryText(3_200L)
        val (cipher, iv) = EncryptionHelper.encrypt(summary)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.persistVoiceNoteMessageRemote(
                chatId = chatId,
                message = Message(
                    id = msgId,
                    chatId = chatId,
                    senderId = aliceUid,
                    senderName = "Alice",
                    text = summary,
                    voiceDurationMillis = 3_200L,
                    voiceWaveform = listOf(0.3f, 0.7f, 0.5f)
                ),
                cipherText = cipher,
                iv = iv,
                timeString = "11:25 AM",
                explicitParticipants = listOf(aliceUid)
            )
        }

        val malloryEmail = "vn_outsider_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(malloryEmail)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msgId)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED when non-participant reads private voice note")
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
