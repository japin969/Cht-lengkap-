package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class ContactNoteRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveContactNote_authenticatedOwner_persistsNoteAndBirthdayToUsersNotesSubcollection() = runBlocking {
        val aliceEmail = "note_alice_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val repository = ContactNoteRepository(firestore, auth)

        val birthdayMillis = 466214400000L // 1984-10-10 UTC
        val formattedBirthday = ContactNoteRepository.formatBirthdayMillis(birthdayMillis)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveContactNote(
                contactId = "pavel_durov",
                noteText = "Catatan rahasia proyek Jayra",
                birthdayMillis = birthdayMillis,
                birthdayFormatted = formattedBirthday
            )
        }
        assertTrue("saveContactNote failed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val streamed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeContactNote("pavel_durov").first { it.note.isNotBlank() }
        }
        assertEquals(aliceUid, streamed.ownerId)
        assertEquals("pavel_durov", streamed.contactId)
        assertEquals("Catatan rahasia proyek Jayra", streamed.note)
        assertEquals(birthdayMillis, streamed.birthdayMillis)

        // Update existing note
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveContactNote(
                contactId = "pavel_durov",
                noteText = "Catatan diperbarui setelah rapat",
                birthdayMillis = birthdayMillis,
                birthdayFormatted = formattedBirthday
            )
        }
        assertTrue("update saveContactNote failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)
    }

    @Test
    fun getContactNote_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "note_owner_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = ContactNoteRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveContactNote(
                contactId = "elena_p",
                noteText = "Catatan pribadi Alice tentang Elena",
                birthdayMillis = null,
                birthdayFormatted = ""
            ).getOrThrow()
        }

        val bobEmail = "note_outsider_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(bobEmail)
        val bobRepo = ContactNoteRepository(firestore, auth)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getContactNoteForOwner(targetOwnerUid = aliceUid, contactId = "elena_p")
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user private note read")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun getContactNote_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "note_unauth_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val repo = ContactNoteRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveContactNote(
                contactId = "sarah_connor",
                noteText = "Wajib autentikasi",
                birthdayMillis = null,
                birthdayFormatted = ""
            ).getOrThrow()
        }

        auth.signOut()

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.getContactNoteForOwner(targetOwnerUid = aliceUid, contactId = "sarah_connor")
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
