package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class VisibilityOption(val key: String) {
    EVERYONE("EVERYONE"),
    CONTACTS("CONTACTS"),
    NOBODY("NOBODY");

    companion object {
        fun fromString(raw: String?): VisibilityOption =
            entries.firstOrNull { it.key == raw } ?: EVERYONE
    }
}

enum class ChatType(val key: String) {
    DIRECT("DIRECT"),
    GROUP("GROUP");

    companion object {
        fun fromString(raw: String?): ChatType =
            entries.firstOrNull { it.key == raw } ?: DIRECT
    }
}

enum class MessageType(val key: String) {
    TEXT("TEXT"),
    IMAGE("IMAGE"),
    AUDIO("AUDIO"),
    DOCUMENT("DOCUMENT"),
    LOCATION("LOCATION"),
    CONTACT("CONTACT"),
    STICKER("STICKER"),
    SYSTEM("SYSTEM");

    companion object {
        fun fromString(raw: String?): MessageType =
            entries.firstOrNull { it.key == raw } ?: TEXT
    }
}

enum class DeliveryStatus(val key: String) {
    SENT("SENT"),
    DELIVERED("DELIVERED"),
    READ("READ");

    companion object {
        fun fromString(raw: String?): DeliveryStatus =
            entries.firstOrNull { it.key == raw } ?: SENT
    }
}

enum class CallType(val key: String) {
    VOICE("VOICE"),
    VIDEO("VIDEO");

    companion object {
        fun fromString(raw: String?): CallType =
            entries.firstOrNull { it.key == raw } ?: VOICE
    }
}

enum class CallStatus(val key: String) {
    RINGING("RINGING"),
    ACTIVE("ACTIVE"),
    ENDED("ENDED"),
    MISSED("MISSED"),
    DECLINED("DECLINED");

    companion object {
        fun fromString(raw: String?): CallStatus =
            entries.firstOrNull { it.key == raw } ?: RINGING
    }
}

@Keep
@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val phoneNumber: String? = null,
    val countryCode: String = "+91",
    val language: String = "EN",
    val darkMode: Boolean = false,
    val wallpaperId: String = "smile_yellow",
    val autoDownloadWifi: Boolean = true,
    val dataSaver: Boolean = false,
    val readReceipts: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val pinHash: String? = null,
    val twoStepHash: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email,
        "phoneNumber" to phoneNumber,
        "countryCode" to countryCode,
        "language" to language,
        "darkMode" to darkMode,
        "wallpaperId" to wallpaperId,
        "autoDownloadWifi" to autoDownloadWifi,
        "dataSaver" to dataSaver,
        "readReceipts" to readReceipts,
        "notificationsEnabled" to notificationsEnabled,
        "appLockEnabled" to appLockEnabled,
        "pinHash" to pinHash,
        "twoStepHash" to twoStepHash,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val smileId: String = "",
    val about: String = "बात करें, मुस्कुराएं!",
    val avatarColor: String = "#FFD600",
    val avatarData: String? = null,
    val publicKey: String? = null,
    val online: Boolean = true,
    val discoverable: Boolean = true,
    val photoVisible: String = "EVERYONE",
    val aboutVisible: String = "EVERYONE",
    val lastSeenVisible: String = "EVERYONE",
    val lastSeenAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "smileId" to smileId,
        "about" to about,
        "avatarColor" to avatarColor,
        "avatarData" to avatarData,
        "publicKey" to publicKey,
        "online" to online,
        "discoverable" to discoverable,
        "photoVisible" to photoVisible,
        "aboutVisible" to aboutVisible,
        "lastSeenVisible" to lastSeenVisible,
        "lastSeenAt" to FieldValue.serverTimestamp(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class UserContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val contactUid: String = "",
    val displayName: String = "",
    val smileId: String = "",
    val phoneHint: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedUid: String = "",
    val blockedName: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "INFO",
    val referenceId: String? = null,
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatRoom(
    @DocumentId val id: String = "",
    val chatType: String = "DIRECT",
    val title: String = "",
    val description: String = "",
    val avatarColor: String = "#FFD600",
    val memberUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val coAdminUids: List<String> = emptyList(),
    val pinnedBy: List<String> = emptyList(),
    val archivedBy: List<String> = emptyList(),
    val mutedBy: List<String> = emptyList(),
    val typingUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageType: String = "TEXT",
    val disappearingHours: Long = 0L,
    val announcementsOnly: Boolean = false,
    val inviteCode: String = "",
    val communityId: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun typeEnum(): ChatType = ChatType.fromString(chatType)
    fun peerUid(myUid: String): String? =
        if (typeEnum() == ChatType.DIRECT) memberUids.firstOrNull { it != myUid } else null
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val messageType: String = "TEXT",
    val body: String = "",
    val iv: String = "",
    val e2ee: Boolean = false,
    val replyToId: String = "",
    val replyPreview: String = "",
    val forwarded: Boolean = false,
    val edited: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val deletedFor: List<String> = emptyList(),
    val starredBy: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(),
    val deliveryStatus: String = "SENT",
    val mediaName: String? = null,
    val mediaMime: String? = null,
    val mediaData: String? = null,
    val expiresAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun typeEnum(): MessageType = MessageType.fromString(messageType)
    fun deliveryEnum(): DeliveryStatus = DeliveryStatus.fromString(deliveryStatus)
}

@Keep
@IgnoreExtraProperties
data class StatusUpdate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorSmileId: String = "",
    val statusType: String = "TEXT",
    val textContent: String = "",
    val caption: String = "",
    val bgColor: String = "#FFD600",
    val mediaData: String? = null,
    val privacyMode: String = "EVERYONE",
    val allowedUids: List<String> = emptyList(),
    val viewerUids: List<String> = emptyList(),
    val expiresAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StatusView(
    @DocumentId val id: String = "",
    val statusId: String = "",
    val viewerUid: String = "",
    val viewerName: String = "",
    val ownerUid: String = "",
    val viewedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallRecord(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val title: String = "",
    val participantUids: List<String> = emptyList(),
    val callType: String = "VOICE",
    val groupCall: Boolean = false,
    val callStatus: String = "RINGING",
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val iceCandidates: List<String> = emptyList(),
    val durationSeconds: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun typeEnum(): CallType = CallType.fromString(callType)
    fun statusEnum(): CallStatus = CallStatus.fromString(callStatus)
}

@Keep
@IgnoreExtraProperties
data class Community(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val groupIds: List<String> = emptyList(),
    val announcementText: String = "",
    val avatarColor: String = "#FFD600",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val published: Boolean = true,
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)
