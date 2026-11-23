import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";

initializeApp();

const DATABASE_ID = "ai-studio-android-smile-33715eb6-7eef-4799-8f6b-21ffedb9e9f0";
const db = getFirestore(DATABASE_ID);

/**
 * Send FCM push notification to chat participants whenever a new message is created.
 * Honors mutedBy list and avoids leaking E2EE ciphertext in notification previews.
 */
export const notifyNewChatMessage = onDocumentCreated(
  {
    document: "chats/{chatId}/messages/{messageId}",
    database: DATABASE_ID
  },
  async (event) => {
    const msg = event.data?.data();
    if (!msg) return;

    const chatId = event.params.chatId;
    const senderId = String(msg.senderId || "");
    const senderName = String(msg.senderName || "SMILE");
    const chatSnap = await db.collection("chats").doc(chatId).get();
    const chatData = chatSnap.data() || {};
    const mutedBy: string[] = Array.isArray(chatData.mutedBy) ? chatData.mutedBy : [];
    const recipients: string[] = (Array.isArray(msg.memberUids) ? msg.memberUids : [])
      .filter((uid: string) => uid !== senderId && !mutedBy.includes(uid));

    if (recipients.length === 0) return;

    const preview = msg.e2ee ? "New encrypted message" : String(msg.body || "New message").slice(0, 120);
    const tokens: string[] = [];

    for (const uid of recipients) {
      const userSnap = await db.collection("users").doc(uid).get();
      const userData = userSnap.data();
      if (userData?.notificationsEnabled !== false && typeof userData?.fcmToken === "string" && userData.fcmToken) {
        tokens.push(userData.fcmToken);
      }
    }

    if (tokens.length > 0) {
      await getMessaging().sendEachForMulticast({
        tokens,
        notification: {
          title: senderName,
          body: preview
        },
        data: {
          type: "CHAT_MESSAGE",
          chatId
        },
        android: {
          priority: "high"
        }
      });
    }
  }
);

/**
 * Send high-priority FCM data payload on incoming voice/video calls so devices wake when backgrounded.
 */
export const notifyIncomingCall = onDocumentCreated(
  {
    document: "calls/{callId}",
    database: DATABASE_ID
  },
  async (event) => {
    const call = event.data?.data();
    if (!call || call.callStatus !== "RINGING") return;

    const callerId = String(call.callerId || "");
    const participantUids: string[] = Array.isArray(call.participantUids) ? call.participantUids : [];
    const targets = participantUids.filter((uid) => uid !== callerId);
    const tokens: string[] = [];

    for (const uid of targets) {
      const userSnap = await db.collection("users").doc(uid).get();
      const fcmToken = userSnap.data()?.fcmToken;
      if (typeof fcmToken === "string" && fcmToken) {
        tokens.push(fcmToken);
      }
    }

    if (tokens.length > 0) {
      await getMessaging().sendEachForMulticast({
        tokens,
        data: {
          type: "INCOMING_CALL",
          callId: event.params.callId,
          callerName: String(call.callerName || "SMILE Call"),
          callType: String(call.callType || "VOICE")
        },
        android: {
          priority: "high",
          ttl: 45000
        }
      });
    }
  }
);

/**
 * Hourly scheduled cleanup deleting expired 24-hour status updates.
 */
export const cleanupExpiredStatuses = onSchedule("every 60 minutes", async () => {
  const now = Timestamp.now();
  const expired = await db.collection("statuses").where("expiresAt", "<=", now).limit(200).get();
  if (expired.empty) return;

  const batch = db.batch();
  expired.docs.forEach((docSnap) => batch.delete(docSnap.ref));
  await batch.commit();
});
