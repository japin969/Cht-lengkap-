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

class StoryRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createObserveAndMarkViewed_authenticatedUsers_persistsAndUpdatesInFirestore() = runBlocking {
        val aliceEmail = "story_alice_${UUID.randomUUID().toString().take(8)}@jayra.app"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = StoryRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createStory(
                imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800",
                caption = "Peluncuran Story Albums Jayra",
                albumId = "album_rilis",
                albumTitle = "Rilis Jayra",
                userName = "Alice Jayra"
            )
        }
        assertTrue("createStory failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val created = createResult.getOrThrow()
        assertEquals(aliceUid, created.userId)
        assertEquals("album_rilis", created.albumId)

        val bobEmail = "story_bob_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(bobEmail)
        val bobRepo = StoryRepository(firestore, auth)

        val streamedForBob = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeStories().first { list -> list.any { it.id == created.id } }
        }
        assertTrue(streamedForBob.any { it.id == created.id && !it.isViewed })

        val markResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.markStoryViewed(created.id)
        }
        assertTrue("markStoryViewed failed: ${markResult.exceptionOrNull()}", markResult.isSuccess)

        val updatedStory = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getStoryById(created.id).getOrThrow()
        }
        assertEquals(1, updatedStory?.viewsCount)
        assertEquals(true, updatedStory?.isViewed)
    }

    @Test
    fun deleteStory_crossUserNonOwner_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "story_owner_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(aliceEmail)
        val aliceRepo = StoryRepository(firestore, auth)
        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createStory(
                imageUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800",
                caption = "Story milik Alice",
                albumId = "album_tim",
                albumTitle = "Momen Tim"
            ).getOrThrow()
        }

        val bobEmail = "story_intruder_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(bobEmail)
        val bobRepo = StoryRepository(firestore, auth)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.deleteStory(created.id)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for non-owner story delete")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun getStoryById_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "story_unauth_${UUID.randomUUID().toString().take(8)}@jayra.app"
        signInTestUser(aliceEmail)
        val repo = StoryRepository(firestore, auth)
        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createStory(
                imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
                caption = "Story Terproteksi"
            ).getOrThrow()
        }

        auth.signOut()

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.getStoryById(created.id)
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
