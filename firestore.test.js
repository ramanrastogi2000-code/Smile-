const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const CHARLIE_UID = "charlie_uid_003";
const ADMIN_EMAIL = "ramanrastogi2000@gmail.com";

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

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function futureDate(msOffset = 60000) {
  return new Date(Date.now() + msOffset);
}

function validUserSettings(uid) {
  const now = pastDate();
  return {
    userId: uid,
    email: `${uid}@example.com`,
    phoneNumber: "+919876543210",
    countryCode: "+91",
    language: "EN",
    darkMode: false,
    wallpaperId: "smile_default",
    autoDownloadWifi: true,
    dataSaver: false,
    readReceipts: true,
    notificationsEnabled: true,
    appLockEnabled: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validUserProfile(uid, smileId) {
  const now = pastDate();
  return {
    userId: uid,
    displayName: `User ${uid}`,
    smileId,
    about: "बात करें, मुस्कुराएं!",
    avatarColor: "#FFD600",
    publicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtest",
    online: true,
    discoverable: true,
    photoVisible: "EVERYONE",
    aboutVisible: "EVERYONE",
    lastSeenVisible: "EVERYONE",
    lastSeenAt: now,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("userProfiles").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("userProfiles").where("discoverable", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("chats").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    unauthDb.collection("statuses").where("privacyMode", "==", "EVERYONE").get()
  );
  await assertFails(
    unauthDb.collection("calls").where("participantUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    unauthDb.collection("communities").where("memberUids", "array-contains", ALICE_UID).get()
  );
});

test("User settings PII is strictly owner-isolated and rejects ghost/future fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserSettings(ALICE_UID))
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "HI",
      darkMode: true,
      updatedAt: pastDate(500),
    })
  );

  // Cross-user read/write blocked (PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      language: "HI",
      updatedAt: pastDate(500),
    })
  );

  // Ghost field & future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "EN",
      updatedAt: futureDate(),
    })
  );
});

test("UserProfile discovery, SmileID uniqueness & PhoneLookup work securely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("userProfiles")
      .doc(ALICE_UID)
      .set(validUserProfile(ALICE_UID, "alice_smile"))
  );
  await assertSucceeds(
    aliceDb.collection("smileIds").doc("alice_smile").set({
      userId: ALICE_UID,
      smileId: "alice_smile",
      displayName: "Alice",
      createdAt: pastDate(),
    })
  );

  // Duplicate SmileID reservation by Bob fails
  await assertFails(
    bobDb.collection("smileIds").doc("alice_smile").set({
      userId: BOB_UID,
      smileId: "alice_smile",
      displayName: "Fake Alice",
      createdAt: pastDate(),
    })
  );

  // Discoverable query succeeds, unfiltered query fails
  await assertSucceeds(
    bobDb.collection("userProfiles").where("discoverable", "==", true).get()
  );
  await assertFails(bobDb.collection("userProfiles").get());

  // PhoneLookup allows exact get, forbids list scraping
  await assertSucceeds(
    aliceDb.collection("phoneLookup").doc("hash_919876543210").set({
      userId: ALICE_UID,
      smileId: "alice_smile",
      displayName: "Alice",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    bobDb.collection("phoneLookup").doc("hash_919876543210").get()
  );
  await assertFails(bobDb.collection("phoneLookup").get());
});

test("Chats and Messages enforce membership, denormalized memberUids, and sender rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const now = pastDate(5000);
  const chatId = "direct_alice_bob";
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatType: "DIRECT",
      title: "Bob",
      description: "",
      avatarColor: "#FFD600",
      memberUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID, BOB_UID],
      coAdminUids: [],
      pinnedBy: [],
      archivedBy: [],
      mutedBy: [],
      typingUids: [],
      lastMessageText: "",
      lastMessageSenderId: "",
      lastMessageType: "TEXT",
      disappearingHours: 0,
      announcementsOnly: false,
      inviteCode: "",
      communityId: "",
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Scoped query works; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("chats").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(bobDb.collection("chats").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).get());

  // Send E2EE message as Alice
  const msgId = "msg_001";
  await assertSucceeds(
    aliceDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .doc(msgId)
      .set({
        chatId,
        senderId: ALICE_UID,
        senderName: "Alice",
        memberUids: [ALICE_UID, BOB_UID],
        messageType: "TEXT",
        body: "ciphertext_base64==",
        iv: "iv_base64==",
        e2ee: true,
        replyToId: "",
        replyPreview: "",
        forwarded: false,
        edited: false,
        deletedForEveryone: false,
        deletedFor: [],
        starredBy: [],
        reactions: {},
        deliveryStatus: "SENT",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Reject @DocumentId `id` leakage
  await assertFails(
    aliceDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .doc("msg_bad")
      .set({
        id: "msg_bad",
        chatId,
        senderId: ALICE_UID,
        senderName: "Alice",
        memberUids: [ALICE_UID, BOB_UID],
        messageType: "TEXT",
        body: "bad",
        iv: "",
        e2ee: false,
        replyToId: "",
        replyPreview: "",
        forwarded: false,
        edited: false,
        deletedForEveryone: false,
        deletedFor: [],
        starredBy: [],
        reactions: {},
        deliveryStatus: "SENT",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Bob can query messages with array-contains & add reaction/read status, but CANNOT edit Alice's body
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .doc(msgId)
      .update({
        reactions: { [BOB_UID]: "❤️" },
        deliveryStatus: "READ",
        updatedAt: pastDate(500),
      })
  );
  await assertFails(
    bobDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .doc(msgId)
      .update({
        body: "tampered",
        edited: true,
        updatedAt: pastDate(500),
      })
  );

  // Charlie cannot read message doc, run unfiltered list, or query Alice's messages
  await assertFails(
    charlieDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .doc(msgId)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
});

test("Statuses, Calls, Communities, Reports and Admin isolation behave securely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext("admin_uid", {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  const now = pastDate(5000);

  // Status create + view receipt
  await assertSucceeds(
    aliceDb.collection("statuses").doc("st_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      authorSmileId: "alice_smile",
      statusType: "TEXT",
      textContent: "Hello SMILE!",
      caption: "Morning",
      bgColor: "#FFD600",
      privacyMode: "EVERYONE",
      allowedUids: [],
      viewerUids: [],
      expiresAt: futureDate(86400000),
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("statuses").where("privacyMode", "==", "EVERYONE").get()
  );
  await assertSucceeds(
    bobDb.collection("statuses").doc("st_1").update({
      viewerUids: [BOB_UID],
      updatedAt: pastDate(500),
    })
  );

  // Call signaling + terminal state lock
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Alice",
      title: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      callType: "VIDEO",
      groupCall: false,
      callStatus: "RINGING",
      sdpOffer: "v=0...",
      sdpAnswer: "",
      iceCandidates: [],
      durationSeconds: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      callStatus: "ENDED",
      durationSeconds: 42,
      updatedAt: pastDate(500),
    })
  );
  // Terminal state locking: cannot re-open ENDED call
  await assertFails(
    aliceDb.collection("calls").doc("call_1").update({
      callStatus: "ACTIVE",
      updatedAt: pastDate(100),
    })
  );
  await assertFails(charlieDb.collection("calls").doc("call_1").get());

  // Communities
  await assertSucceeds(
    aliceDb.collection("communities").doc("comm_1").set({
      name: "SMILE Creators",
      description: "Community group",
      ownerId: ALICE_UID,
      adminUids: [ALICE_UID],
      memberUids: [ALICE_UID, BOB_UID],
      groupIds: [],
      announcementText: "Welcome!",
      avatarColor: "#FFD600",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb
      .collection("communities")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    bobDb.collection("communities").doc("comm_1").update({
      name: "Hijacked",
      updatedAt: pastDate(100),
    })
  );

  // Reports & Admin protection (email_verified required, no self-registration in adminUsers)
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetUid: BOB_UID,
      targetType: "USER",
      reason: "Spam",
      details: "Test report",
      reportStatus: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("reports").doc("rep_1").get());
  await assertFails(
    spoofedAdminDb.collection("reports").doc("rep_1").get()
  );
  await assertSucceeds(adminDb.collection("reports").doc("rep_1").get());
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_1").update({
      reportStatus: "RESOLVED",
      updatedAt: pastDate(100),
    })
  );
  await assertFails(
    aliceDb.collection("adminUsers").doc(ALICE_UID).set({ role: "OWNER" })
  );
});
