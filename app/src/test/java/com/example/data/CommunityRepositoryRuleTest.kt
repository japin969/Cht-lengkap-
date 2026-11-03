package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CommunityRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveCommunities_authenticatedMember_persistsAndStreamsFromFirestore() = runBlocking {
        val uniqueEmail = "comm_alice_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(uniqueEmail)
        val repository = CommunityRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCommunity(
                name = "Komunitas Engineer Jayra",
                description = "Diskusi arsitektur Android modern",
                groupIds = listOf("chat_2", "chat_5"),
                avatarUrl = "https://example.com/comm.jpg"
            )
        }
        assertTrue("createCommunity failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val created = createResult.getOrThrow()
        assertEquals(aliceUid, created.creatorId)
        assertEquals(listOf("chat_2", "chat_5"), created.groupIds)

        val streamedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCommunities().first { list -> list.any { it.id == created.id } }
        }
        assertTrue(streamedList.any { it.id == created.id && it.name == "Komunitas Engineer Jayra" })
    }

    @Test
    fun getCommunityById_crossUserNonMember_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "comm_owner_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(aliceEmail)
        val aliceRepo = CommunityRepository(firestore, auth)
        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCommunity(
                name = "Komunitas Privat Alice",
                description = "Khusus anggota tim inti",
                groupIds = listOf("chat_2")
            ).getOrThrow()
        }

        val bobEmail = "comm_outsider_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(bobEmail)
        val bobRepo = CommunityRepository(firestore, auth)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getCommunityById(created.id)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for non-member community read")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun getCommunityById_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "comm_unauth_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(aliceEmail)
        val repo = CommunityRepository(firestore, auth)
        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createCommunity(
                name = "Komunitas Terproteksi",
                description = "Wajib autentikasi",
                groupIds = listOf("chat_5")
            ).getOrThrow()
        }

        auth.signOut()

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.getCommunityById(created.id)
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
