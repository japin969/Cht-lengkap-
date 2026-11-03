const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or create user registration document", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneNumber: "+628152961500",
      country: "Indonesia",
      createdAt: new Date(),
    })
  );
});

test("Authenticated owner: can register own user account with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneNumber: "+628152961500",
      country: "Indonesia",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("PII Isolation: authenticated user cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      phoneNumber: "+12025550199",
      country: "Amerika Serikat",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Shadow Field Guard: rejects registration with unauthorized extra fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneNumber: "+628152961500",
      country: "Indonesia",
      createdAt: new Date(),
      isAdmin: true,
    })
  );
});

test("Immutable Field Guard: rejects update that mutates createdAt or userId", async () => {
  const createdDate = new Date("2026-01-01T00:00:00Z");
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneNumber: "+628152961500",
      country: "Indonesia",
      createdAt: createdDate,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userId: BOB_UID,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      name: "Alice Pratama",
      updatedAt: new Date(),
    })
  );
});

test("Query Alignment: user list succeeds only when filtered by own userId", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneNumber: "+628152961500",
      country: "Indonesia",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("users").get());
});

test("FCM Token Security: only authenticated owner can update fcmToken", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneNumber: "+628152961500",
      country: "Indonesia",
      createdAt: new Date("2026-01-01T00:00:00Z"),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fcmToken: "fcm_token_alice_valid_registration_id_12345",
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      fcmToken: "fcm_token_attacker_hijack_99999",
    })
  );
});

test("Group Call Signaling: caller can create offer and invite additional participant", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const callRef = aliceDb.collection("call_signals").doc("call_room_1");

  await assertSucceeds(
    callRef.set({
      callerId: ALICE_UID,
      receiverId: BOB_UID,
      status: "ringing",
      callId: "call_room_1",
      senderId: ALICE_UID,
      type: "offer",
      isFrontCamera: true,
      participants: [ALICE_UID, BOB_UID],
      timestamp: new Date(),
    })
  );

  await assertSucceeds(
    callRef.update({
      type: "add_participant",
      status: "invited",
      senderId: ALICE_UID,
      invitedUserId: "charlie_789",
      participants: [ALICE_UID, BOB_UID, "charlie_789"],
      timestamp: new Date(),
    })
  );
});

test("Saved Messages: authenticated user can create personal chat room (chatId = uid) and store saved messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const savedChatRef = aliceDb.collection("chats").doc(ALICE_UID);
  await assertSucceeds(
    savedChatRef.set({
      id: ALICE_UID,
      name: "Pesan Tersimpan",
      lastMessage: "Catatan pribadi penting",
      timestamp: "10:00 AM",
      isPinned: true,
      isGroup: false,
      isChannel: false,
      participants: [ALICE_UID],
    })
  );

  await assertSucceeds(
    savedChatRef.collection("messages").doc("saved_msg_1").set({
      senderId: ALICE_UID,
      senderName: "Saya",
      cipherText: "enc_catatan_pribadi",
      iv: "iv_base64_123",
      timestamp: new Date(),
      timestampFormatted: "10:00 AM",
      participants: [ALICE_UID],
    })
  );

  // Another user cannot read Alice's Saved Messages
  await assertFails(
    bobDb.collection("chats").doc(ALICE_UID).collection("messages").doc("saved_msg_1").get()
  );
});

test("Message Forwarding: participant can forward message with forwardedFrom metadata to target chat", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const targetChatRef = aliceDb.collection("chats").doc("chat_alice_bob");

  await assertSucceeds(
    targetChatRef.set({
      id: "chat_alice_bob",
      name: "Bob",
      participants: [ALICE_UID, BOB_UID],
    })
  );

  await assertSucceeds(
    targetChatRef.collection("messages").doc("fwd_msg_1").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      cipherText: "enc_pesan_terusan",
      iv: "iv_base64_456",
      timestamp: new Date(),
      timestampFormatted: "10:05 AM",
      forwardedFrom: "Pavel Durov",
      participants: [ALICE_UID, BOB_UID],
    })
  );
});


test("Communities: member can create and query communities while non-member and unauthenticated users are denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const commRef = aliceDb.collection("communities").doc("comm_android_id");
  await assertSucceeds(
    commRef.set({
      id: "comm_android_id",
      name: "Komunitas Engineer Android",
      description: "Wadah kolaborasi grup Kotlin & Jetpack Compose",
      avatarUrl: "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200",
      groupIds: ["chat_2", "chat_5"],
      creatorId: ALICE_UID,
      memberIds: [ALICE_UID],
      createdAt: new Date(),
    })
  );

  // Aligned member query succeeds
  await assertSucceeds(
    aliceDb.collection("communities").where("memberIds", "array-contains", ALICE_UID).get()
  );

  // Non-member Bob and unauthenticated user cannot read Alice's private community
  await assertFails(bobDb.collection("communities").doc("comm_android_id").get());
  await assertFails(unauthDb.collection("communities").doc("comm_android_id").get());
});

test("Topic Tabs: group chat supports hasTopics and messages support topicId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const groupChatRef = aliceDb.collection("chats").doc("group_topics_1");

  await assertSucceeds(
    groupChatRef.set({
      id: "group_topics_1",
      name: "Forum Arsitektur Jayra",
      isGroup: true,
      hasTopics: true,
      participants: [ALICE_UID, BOB_UID],
    })
  );

  await assertSucceeds(
    groupChatRef.collection("messages").doc("topic_msg_1").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      cipherText: "enc_pesan_topik_1",
      iv: "iv_topic_1",
      topicId: "topik_1",
      timestamp: new Date(),
      timestampFormatted: "10:15 AM",
      participants: [ALICE_UID, BOB_UID],
    })
  );
});

test("Story Albums: authenticated author creates story, viewer marks viewed, unauthenticated & spoofed writes denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Unauthenticated cannot read or list stories
  await assertFails(unauthDb.collection("stories").get());

  // Spoofed userId creation is rejected
  await assertFails(
    bobDb.collection("stories").doc("story_spoof").set({
      id: "story_spoof",
      userId: ALICE_UID,
      userName: "Alice",
      imageUrl: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600",
      timestamp: "10:30 AM",
      viewsCount: 0,
      isViewed: false,
      albumId: "album_rilis",
    })
  );

  // Authenticated author Alice creates a Story Album entry
  const storyRef = aliceDb.collection("stories").doc("story_rilis_1");
  await assertSucceeds(
    storyRef.set({
      id: "story_rilis_1",
      userId: ALICE_UID,
      userName: "Alice Jayra",
      userAvatarUrl: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
      imageUrl: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800",
      caption: "Peluncuran fitur Story Albums Jayra!",
      timestamp: "10:30 AM",
      viewsCount: 0,
      isViewed: false,
      albumId: "album_rilis",
      albumTitle: "Rilis Jayra",
      viewerIds: [],
      createdAt: new Date(),
    })
  );

  // Authenticated Bob can list stories and mark story as viewed (+1 viewsCount)
  await assertSucceeds(bobDb.collection("stories").get());
  await assertSucceeds(
    bobDb.collection("stories").doc("story_rilis_1").update({
      viewsCount: 1,
      isViewed: true,
      viewerIds: [BOB_UID],
    })
  );

  // Non-owner Bob cannot hijack caption or delete Alice's story
  await assertFails(
    bobDb.collection("stories").doc("story_rilis_1").update({
      caption: "Bajak deskripsi!",
    })
  );
  await assertFails(bobDb.collection("stories").doc("story_rilis_1").delete());

  // Owner Alice can delete her own story
  await assertSucceeds(storyRef.delete());
});

test("Contact Notes & Birthdays: owner can create and update private contact notes while cross-user and unauthenticated access is denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("pavel_durov");

  await assertSucceeds(
    noteRef.set({
      contactId: "pavel_durov",
      ownerId: ALICE_UID,
      note: "Diskusi arsitektur enkripsi MTProto & rilis Jayra Q4.",
      birthdayFormatted: "10 Oktober 1984",
      birthdayMillis: 466214400000,
      updatedAt: new Date(),
    })
  );

  // Subsequent set() / update() on existing note succeeds for owner
  await assertSucceeds(
    noteRef.set({
      contactId: "pavel_durov",
      ownerId: ALICE_UID,
      note: "Catatan diperbarui: kirim undangan review kode.",
      birthdayFormatted: "10 Oktober 1984",
      birthdayMillis: 466214400000,
      updatedAt: new Date(),
    })
  );

  // Owner can read and list own private notes
  await assertSucceeds(noteRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notes").get());

  // Cross-user Bob and unauthenticated user are strictly denied read & write
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("pavel_durov").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("pavel_durov").set({
      contactId: "pavel_durov",
      ownerId: ALICE_UID,
      note: "Bajak catatan!",
      birthdayFormatted: "",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notes").doc("pavel_durov").get()
  );
});

test("Checklists in Chat: participants can create checklist messages and toggle checklistItems while non-participants and cipherText tampering are denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const malloryDb = testEnv.authenticatedContext("mallory_uid").firestore();

  const chatId = "chat_checklist_team";
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      id: chatId,
      name: "Tim Rilis Jayra",
      participants: [ALICE_UID, BOB_UID],
      isGroup: true,
      hasTopics: false,
      lastMessage: "Daftar Tugas dibuat",
      timestamp: "10:45 AM",
    })
  );

  const msgRef = aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_checklist_1");
  await assertSucceeds(
    msgRef.set({
      senderId: ALICE_UID,
      senderName: "Alice",
      cipherText: "encrypted_checklist_summary",
      iv: "iv_123",
      timestamp: new Date(),
      timestampFormatted: "10:45 AM",
      topicId: "general",
      checklistTitle: "Sprint Rilis Fase 25",
      checklistItems: [
        { id: "task_1", text: "Validasi Firestore Rules", isDone: false },
        { id: "task_2", text: "Verifikasi UI Dark Mode", isDone: false },
      ],
      allowOthersToToggle: true,
      participants: [ALICE_UID, BOB_UID],
    })
  );

  // Participant Bob can toggle task_1 to isDone: true
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_checklist_1").update({
      checklistItems: [
        { id: "task_1", text: "Validasi Firestore Rules", isDone: true },
        { id: "task_2", text: "Verifikasi UI Dark Mode", isDone: false },
      ],
    })
  );

  // Participant Bob CANNOT tamper with cipherText or checklistTitle during update
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_checklist_1").update({
      cipherText: "tampered_cipher",
      checklistItems: [
        { id: "task_1", text: "Validasi Firestore Rules", isDone: true },
        { id: "task_2", text: "Verifikasi UI Dark Mode", isDone: true },
      ],
    })
  );

  // Non-participant Mallory and unauthenticated caller CANNOT toggle checklistItems
  await assertFails(
    malloryDb.collection("chats").doc(chatId).collection("messages").doc("msg_checklist_1").update({
      checklistItems: [
        { id: "task_1", text: "Validasi Firestore Rules", isDone: false },
        { id: "task_2", text: "Verifikasi UI Dark Mode", isDone: false },
      ],
    })
  );
  await assertFails(
    unauthDb.collection("chats").doc(chatId).collection("messages").doc("msg_checklist_1").update({
      checklistItems: [
        { id: "task_1", text: "Validasi Firestore Rules", isDone: false },
        { id: "task_2", text: "Verifikasi UI Dark Mode", isDone: false },
      ],
    })
  );
});

test("Voice Trimming: participant can send trimmed voice note metadata while invalid duration and non-participants are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const malloryDb = testEnv.authenticatedContext("mallory_uid").firestore();

  const chatId = "chat_voice_trimmer_1";
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      id: chatId,
      name: "Diskusi Audio Jayra",
      participants: [ALICE_UID, BOB_UID],
      isGroup: false,
      hasTopics: false,
      lastMessage: "🎤 Voice Note",
      timestamp: "11:15 AM",
    })
  );

  // Valid trimmed voice note creation succeeds
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_voice_1").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      cipherText: "enc_voice_summary",
      iv: "iv_voice_1",
      timestamp: new Date(),
      timestampFormatted: "11:15 AM",
      topicId: "general",
      voiceDurationMillis: 4500,
      voiceWaveform: [0.25, 0.62, 0.88, 0.45, 0.91, 0.34],
      participants: [ALICE_UID, BOB_UID],
    })
  );

  // Invalid negative duration is rejected
  await assertFails(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_voice_bad").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      cipherText: "enc_voice_summary",
      iv: "iv_voice_bad",
      timestamp: new Date(),
      timestampFormatted: "11:16 AM",
      topicId: "general",
      voiceDurationMillis: -50,
      voiceWaveform: [0.4, 0.5],
      participants: [ALICE_UID, BOB_UID],
    })
  );

  // Non-participant Mallory cannot read or send voice note in Alice & Bob's chat
  await assertFails(
    malloryDb.collection("chats").doc(chatId).collection("messages").doc("msg_voice_1").get()
  );
});


