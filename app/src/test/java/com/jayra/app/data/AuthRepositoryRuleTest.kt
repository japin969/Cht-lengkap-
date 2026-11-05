package com.jayra.app.data

import com.jayra.app.base.FirestoreEmulatorTestBase
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

class AuthRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveRegisteredUser_authenticatedOwner_persistsAndReadsUserAccount() = runBlocking {
        val uniqueEmail = "alice_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(uniqueEmail)
        val repository = AuthRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveRegisteredUser(
                email = uniqueEmail,
                phoneNumber = "+628152961599",
                countryName = "Indonesia"
            )
        }
        assertTrue(saveResult.isSuccess)
        assertEquals(aliceUid, saveResult.getOrThrow())

        val loaded = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserAccount(aliceUid).getOrThrow()
        }
        assertNotNull(loaded)
        assertEquals(aliceUid, loaded?.id)
        assertEquals(uniqueEmail, loaded?.email)
        assertEquals("+628152961599", loaded?.phoneNumber)
        assertEquals("Indonesia", loaded?.country)
    }

    @Test
    fun observeUserAccount_authenticatedOwner_emitsRealtimeProfile() = runBlocking {
        val uniqueEmail = "stream_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(uniqueEmail)
        val repository = AuthRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveRegisteredUser(
                email = uniqueEmail,
                phoneNumber = "+819012345678",
                countryName = "Jepang"
            )
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserAccount().first { it != null }
        }
        assertNotNull(emitted)
        assertEquals("Jepang", emitted?.country)
    }

    @Test
    fun getUserAccount_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "alice_iso_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = AuthRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveRegisteredUser(
                email = aliceEmail,
                phoneNumber = "+628188887777",
                countryName = "Indonesia"
            )
        }

        val bobEmail = "bob_iso_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(bobEmail)
        val bobRepo = AuthRepository(firestore, auth)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getUserAccount(aliceUid)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user PII read")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun getUserAccount_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "alice_unauth_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val repo = AuthRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveRegisteredUser(
                email = aliceEmail,
                phoneNumber = "+447123456789",
                countryName = "Inggris"
            )
        }

        auth.signOut()

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.getUserAccount(aliceUid)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
