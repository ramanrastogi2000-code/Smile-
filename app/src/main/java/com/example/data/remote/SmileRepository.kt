package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.Community
import com.example.data.model.StatusUpdate
import com.example.data.model.StatusView
import com.example.data.model.UserContact
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.security.SmileCryptoManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.snapshots
import java.util.Date
import java.util.UUID

class SmileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- USER PROFILE & SETTINGS ---

    fun observeUserSettings(userId: String): Flow<UserSettings?> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("Cross-user settings access denied.")
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(UserSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "userProfiles/$userId"
        emitAll(
            db.collection("userProfiles").document(userId)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeDiscoverableProfiles(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "userProfiles"
        emitAll(
            db.collection("userProfiles")
                .whereEqualTo("discoverable", true)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.userId.isNotBlank() }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun getUserProfileOnce(userId: String): UserProfile? = try {
        requireUserId()
        val snap = db.collection("userProfiles").document(userId).get().await()
        if (snap.exists()) snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, "userProfiles/$userId")
        null
    }

    suspend fun saveProfileSetup(
        displayName: String,
        rawSmileId: String,
        about: String,
        rawPhone: String,
        countryCode: String,
        avatarColor: String,
        avatarData: String?,
        publicKeyBase64: String,
        languageCode: String,
        darkMode: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val email = auth.currentUser?.email
        val cleanName = displayName.trim().take(80)
        val cleanSmileId = rawSmileId.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")
        val cleanAbout = about.trim().take(200).ifBlank { "बात करें, मुस्कुराएं!" }
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Name is required."))
        if (cleanSmileId.length < 3 || cleanSmileId.length > 32) {
            return Result.failure(IllegalArgumentException("Smile ID must be 3–32 lowercase letters, numbers, or underscores."))
        }

        return try {
            val smileRef = db.collection("smileIds").document(cleanSmileId)
            val existingSmile = smileRef.get().await()
            if (existingSmile.exists() && existingSmile.getString("userId") != uid) {
                return Result.failure(IllegalStateException("That Smile ID is already taken. Choose another unique ID."))
            }

            val profileRef = db.collection("userProfiles").document(uid)
            val existingProfile = profileRef.get().await()
            val oldSmileId = existingProfile.getString("smileId")
            if (oldSmileId != null && oldSmileId != cleanSmileId) {
                runCatching { db.collection("smileIds").document(oldSmileId).delete().await() }
            }

            if (!existingSmile.exists()) {
                smileRef.set(
                    mapOf(
                        "userId" to uid,
                        "smileId" to cleanSmileId,
                        "displayName" to cleanName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (existingProfile.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "smileId" to cleanSmileId,
                    "about" to cleanAbout,
                    "avatarColor" to avatarColor,
                    "publicKey" to publicKeyBase64,
                    "online" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!avatarData.isNullOrBlank()) {
                    updates["avatarData"] = avatarData.take(119000)
                }
                profileRef.update(updates).await()
            } else {
                val profile = UserProfile(
                    userId = uid,
                    displayName = cleanName,
                    smileId = cleanSmileId,
                    about = cleanAbout,
                    avatarColor = avatarColor,
                    avatarData = avatarData?.take(119000),
                    publicKey = publicKeyBase64,
                    online = true,
                    discoverable = true
                )
                profileRef.set(profile.toCreateMap()).await()
            }

            val normalizedPhone = SmileCryptoManager.normalizePhoneE164(rawPhone, countryCode)
            val settingsRef = db.collection("users").document(uid)
            val existingSettings = settingsRef.get().await()
            if (existingSettings.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "countryCode" to countryCode,
                    "language" to languageCode,
                    "darkMode" to darkMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (normalizedPhone.isNotBlank()) updates["phoneNumber"] = normalizedPhone.take(24)
                if (!email.isNullOrBlank()) updates["email"] = email.take(200)
                settingsRef.update(updates).await()
            } else {
                val settings = UserSettings(
                    userId = uid,
                    email = email?.take(200),
                    phoneNumber = normalizedPhone.takeIf { it.isNotBlank() }?.take(24),
                    countryCode = countryCode,
                    language = languageCode,
                    darkMode = darkMode
                )
                settingsRef.set(settings.toCreateMap()).await()
            }

            if (normalizedPhone.isNotBlank()) {
                val phoneHash = SmileCryptoManager.hashPhoneForLookup(normalizedPhone, countryCode)
                if (phoneHash.isNotBlank()) {
                    val lookupRef = db.collection("phoneLookup").document(phoneHash)
                    val lookupSnap = lookupRef.get().await()
                    if (lookupSnap.exists()) {
                        lookupRef.update(
                            mapOf(
                                "smileId" to cleanSmileId,
                                "displayName" to cleanName,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } else {
                        lookupRef.set(
                            mapOf(
                                "userId" to uid,
                                "smileId" to cleanSmileId,
                                "displayName" to cleanName,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "userProfiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun updatePresence(online: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val docRef = db.collection("userProfiles").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "online" to online,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "userProfiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateUserSettingsFields(fields: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateUserPrivacyFields(
        photoVisible: String,
        aboutVisible: String,
        lastSeenVisible: String,
        discoverable: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("userProfiles").document(uid).update(
                mapOf(
                    "photoVisible" to photoVisible,
                    "aboutVisible" to aboutVisible,
                    "lastSeenVisible" to lastSeenVisible,
                    "discoverable" to discoverable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "userProfiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun findUserBySmileIdOrPhone(query: String): Result<UserProfile?> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return Result.success(null)
        return try {
            requireUserId()
            val cleanSmile = trimmed.removePrefix("@").lowercase().replace(Regex("[^a-z0-9_]"), "")
            if (cleanSmile.length >= 3) {
                val smileSnap = db.collection("smileIds").document(cleanSmile).get().await()
                val foundUid = smileSnap.getString("userId")
                if (!foundUid.isNullOrBlank()) {
                    val profile = getUserProfileOnce(foundUid)
                    if (profile != null) return Result.success(profile)
                }
            }
            val phoneHash = SmileCryptoManager.hashPhoneForLookup(trimmed, "+91")
            if (phoneHash.isNotBlank()) {
                val lookupSnap = db.collection("phoneLookup").document(phoneHash).get().await()
                val foundUid = lookupSnap.getString("userId")
                if (!foundUid.isNullOrBlank()) {
                    val profile = getUserProfileOnce(foundUid)
                    if (profile != null) return Result.success(profile)
                }
            }
            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "smileIds/$trimmed")
            Result.failure(e)
        }
    }

    suspend fun syncPhoneContacts(phoneEntries: List<Pair<String, String>>): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        var matched = 0
        return try {
            for ((contactName, rawNumber) in phoneEntries.take(150)) {
                val phoneHash = SmileCryptoManager.hashPhoneForLookup(rawNumber, "+91")
                if (phoneHash.isBlank()) continue
                val lookup = db.collection("phoneLookup").document(phoneHash).get().await()
                val targetUid = lookup.getString("userId") ?: continue
                if (targetUid == uid) continue
                val smileId = lookup.getString("smileId") ?: continue
                val displayName = lookup.getString("displayName") ?: contactName
                val contactDoc = db.collection("users").document(uid).collection("contacts").document(targetUid)
                val existing = contactDoc.get().await()
                if (existing.exists()) {
                    contactDoc.update(
                        mapOf(
                            "displayName" to displayName.take(80),
                            "smileId" to smileId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    contactDoc.set(
                        mapOf(
                            "userId" to uid,
                            "contactUid" to targetUid,
                            "displayName" to displayName.take(80),
                            "smileId" to smileId,
                            "phoneHint" to rawNumber.takeLast(4),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                matched++
            }
            Result.success(matched)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/contacts")
            Result.failure(e)
        }
    }

    suspend fun saveContact(profile: UserProfile): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (profile.userId == uid) return Result.success(Unit)
        return try {
            val docRef = db.collection("users").document(uid).collection("contacts").document(profile.userId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to profile.displayName.take(80),
                        "smileId" to profile.smileId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "contactUid" to profile.userId,
                        "displayName" to profile.displayName.take(80),
                        "smileId" to profile.smileId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/contacts/${profile.userId}")
            Result.failure(e)
        }
    }

    fun observeContacts(userId: String): Flow<List<UserContact>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/contacts"
        emitAll(
            db.collection("users").document(userId).collection("contacts")
                .snapshots()
                .map { it.toObjects(UserContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    // --- BLOCKED USERS & REPORTS & NOTIFICATIONS ---

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/blockedUsers"
        emitAll(
            db.collection("users").document(userId).collection("blockedUsers")
                .snapshots()
                .map { it.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun blockUser(targetUid: String, targetName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).collection("blockedUsers").document(targetUid)
                .set(
                    mapOf(
                        "userId" to uid,
                        "blockedUid" to targetUid,
                        "blockedName" to targetName.take(80),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/blockedUsers/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).collection("blockedUsers").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/blockedUsers/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetUid: String,
        targetType: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            db.collection("reports").document(reportId).set(
                mapOf(
                    "reporterId" to uid,
                    "targetUid" to targetUid.take(128),
                    "targetType" to targetType,
                    "reason" to reason.trim().take(120).ifBlank { "General Report" },
                    "details" to details.trim().take(1000),
                    "reportStatus" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reports")
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("announcements")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "announcements", uid)
        )
    }

    // --- CHATS & GROUPS ---

    fun observeChats(userId: String): Flow<List<ChatRoom>> = flow {
        val uid = requireUserId()
        val path = "chats"
        emitAll(
            db.collection("chats")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<ChatRoom> { userId in it.pinnedBy }
                                .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeChat(chatId: String): Flow<ChatRoom?> = flow {
        val uid = requireUserId()
        val path = "chats/$chatId"
        emitAll(
            db.collection("chats").document(chatId)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    suspend fun getChatOnce(chatId: String): ChatRoom? = try {
        requireUserId()
        val snap = db.collection("chats").document(chatId).get().await()
        if (snap.exists()) snap.toObject(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, "chats/$chatId")
        null
    }

    suspend fun getOrCreateDirectChat(peer: UserProfile): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ordered = listOf(uid, peer.userId).sorted()
        val chatId = "direct_${ordered[0]}_${ordered[1]}".take(120)
        return try {
            val docRef = db.collection("chats").document(chatId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "chatType" to "DIRECT",
                        "title" to peer.displayName.take(100).ifBlank { peer.smileId },
                        "description" to "",
                        "avatarColor" to peer.avatarColor,
                        "memberUids" to ordered,
                        "adminUids" to ordered,
                        "coAdminUids" to emptyList<String>(),
                        "pinnedBy" to emptyList<String>(),
                        "archivedBy" to emptyList<String>(),
                        "mutedBy" to emptyList<String>(),
                        "typingUids" to emptyList<String>(),
                        "lastMessageText" to "",
                        "lastMessageSenderId" to "",
                        "lastMessageType" to "TEXT",
                        "disappearingHours" to 0L,
                        "announcementsOnly" to false,
                        "inviteCode" to "",
                        "communityId" to "",
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun createGroupChat(
        title: String,
        description: String,
        memberUids: List<String>,
        announcementsOnly: Boolean = false,
        communityId: String = ""
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = title.trim().take(100)
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Group name required"))
        val allMembers = (listOf(uid) + memberUids).distinct().take(100)
        val groupId = "grp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val inviteCode = UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        return try {
            db.collection("chats").document(groupId).set(
                mapOf(
                    "chatType" to "GROUP",
                    "title" to cleanTitle,
                    "description" to description.trim().take(300),
                    "avatarColor" to "#FFD600",
                    "memberUids" to allMembers,
                    "adminUids" to listOf(uid),
                    "coAdminUids" to emptyList<String>(),
                    "pinnedBy" to emptyList<String>(),
                    "archivedBy" to emptyList<String>(),
                    "mutedBy" to emptyList<String>(),
                    "typingUids" to emptyList<String>(),
                    "lastMessageText" to "Group created",
                    "lastMessageSenderId" to uid,
                    "lastMessageType" to "SYSTEM",
                    "disappearingHours" to 0L,
                    "announcementsOnly" to announcementsOnly,
                    "inviteCode" to inviteCode,
                    "communityId" to communityId,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(groupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats/$groupId")
            Result.failure(e)
        }
    }

    suspend fun toggleChatFlag(chat: ChatRoom, field: String, add: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val op = if (add) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
            db.collection("chats").document(chat.id).update(
                mapOf(
                    field to op,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/${chat.id}")
            Result.failure(e)
        }
    }

    suspend fun setTypingStatus(chatId: String, typing: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val op = if (typing) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
            db.collection("chats").document(chatId).update(
                mapOf(
                    "typingUids" to op,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun updateDisappearingTimer(chatId: String, hours: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("chats").document(chatId).update(
                mapOf(
                    "disappearingHours" to hours.coerceIn(0L, 720L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun updateGroupManagement(
        chatId: String,
        title: String? = null,
        description: String? = null,
        memberUids: List<String>? = null,
        coAdminUids: List<String>? = null,
        announcementsOnly: Boolean? = null
    ): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (title != null) updates["title"] = title.trim().take(100)
            if (description != null) updates["description"] = description.trim().take(300)
            if (memberUids != null) updates["memberUids"] = memberUids.distinct().take(100)
            if (coAdminUids != null) updates["coAdminUids"] = coAdminUids.distinct().take(20)
            if (announcementsOnly != null) updates["announcementsOnly"] = announcementsOnly
            db.collection("chats").document(chatId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    // --- MESSAGES ---

    fun observeMessages(chatId: String, userId: String, pageSize: Long = 100L): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("memberUids", userId)
                .limit(pageSize)
                .snapshots()
                .map { snap ->
                    val now = Timestamp.now()
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { msg ->
                            userId !in msg.deletedFor &&
                                (msg.expiresAt == null || msg.expiresAt.seconds > now.seconds)
                        }
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun sendMessage(
        chat: ChatRoom,
        senderName: String,
        messageType: String,
        body: String,
        iv: String = "",
        e2ee: Boolean = false,
        replyToId: String = "",
        replyPreview: String = "",
        forwarded: Boolean = false,
        mediaName: String? = null,
        mediaMime: String? = null,
        mediaData: String? = null,
        previewTextForChatList: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val expiresAt = if (chat.disappearingHours > 0L) {
            Timestamp(Date(System.currentTimeMillis() + chat.disappearingHours * 3600_000L))
        } else null
        return try {
            val payload = mapOf(
                "chatId" to chat.id,
                "senderId" to uid,
                "senderName" to senderName.take(80).ifBlank { "Smile User" },
                "memberUids" to chat.memberUids.take(100),
                "messageType" to messageType,
                "body" to body.take(4000),
                "iv" to iv.take(64),
                "e2ee" to e2ee,
                "replyToId" to replyToId.take(128),
                "replyPreview" to replyPreview.take(200),
                "forwarded" to forwarded,
                "edited" to false,
                "deletedForEveryone" to false,
                "deletedFor" to emptyList<String>(),
                "starredBy" to emptyList<String>(),
                "reactions" to emptyMap<String, String>(),
                "deliveryStatus" to "SENT",
                "mediaName" to mediaName?.take(200),
                "mediaMime" to mediaMime?.take(100),
                "mediaData" to mediaData?.take(345000),
                "expiresAt" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }.mapValues { it.value!! }

            db.collection("chats").document(chat.id)
                .collection("messages").document(messageId)
                .set(payload).await()

            db.collection("chats").document(chat.id).update(
                mapOf(
                    "lastMessageText" to previewTextForChatList.take(500),
                    "lastMessageSenderId" to uid,
                    "lastMessageType" to messageType,
                    "typingUids" to FieldValue.arrayRemove(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats/${chat.id}/messages/$messageId")
            Result.failure(e)
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newBody: String, newIv: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "body" to newBody.take(4000),
                        "iv" to newIv.take(64),
                        "edited" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForEveryone(chatId: String, messageId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "body" to "",
                        "iv" to "",
                        "deletedForEveryone" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForMe(chatId: String, messageId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "deletedFor" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
            Result.failure(e)
        }
    }

    suspend fun toggleMessageReaction(chatId: String, message: ChatMessage, emoji: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val updated = message.reactions.toMutableMap()
            if (updated[uid] == emoji) updated.remove(uid) else updated[uid] = emoji
            db.collection("chats").document(chatId).collection("messages").document(message.id)
                .update(
                    mapOf(
                        "reactions" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleStarMessage(chatId: String, message: ChatMessage): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val op = if (uid in message.starredBy) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            db.collection("chats").document(chatId).collection("messages").document(message.id)
                .update(
                    mapOf(
                        "starredBy" to op,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
            Result.failure(e)
        }
    }

    suspend fun markMessageRead(chatId: String, messageId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "deliveryStatus" to "READ",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    // --- STATUS / UPDATES (24h expiry) ---

    fun observeStatuses(userId: String): Flow<List<StatusUpdate>> = flow {
        val uid = requireUserId()
        val publicFlow = db.collection("statuses")
            .whereEqualTo("privacyMode", "EVERYONE")
            .snapshots()
            .map { it.toObjects(StatusUpdate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val mineFlow = db.collection("statuses")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(StatusUpdate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val sharedFlow = db.collection("statuses")
            .whereArrayContains("allowedUids", userId)
            .snapshots()
            .map { it.toObjects(StatusUpdate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        emitAll(
            combine(publicFlow, mineFlow, sharedFlow) { pub, mine, shared ->
                val nowSec = Timestamp.now().seconds
                (pub + mine + shared)
                    .distinctBy { it.id }
                    .filter { (it.expiresAt?.seconds ?: Long.MAX_VALUE) > nowSec }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }.logFirestoreErrors(OperationType.LIST, "statuses", uid)
        )
    }

    suspend fun postStatus(
        authorName: String,
        authorSmileId: String,
        statusType: String,
        textContent: String,
        caption: String,
        bgColor: String,
        mediaData: String?,
        privacyMode: String,
        allowedUids: List<String>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val statusId = "st_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 3600L * 1000L))
        return try {
            val payload = mapOf(
                "userId" to uid,
                "authorName" to authorName.take(80).ifBlank { "Smile User" },
                "authorSmileId" to authorSmileId.take(32).ifBlank { "smile" },
                "statusType" to statusType,
                "textContent" to textContent.trim().take(500),
                "caption" to caption.trim().take(200),
                "bgColor" to bgColor,
                "mediaData" to mediaData?.take(345000),
                "privacyMode" to privacyMode,
                "allowedUids" to allowedUids.distinct().take(100),
                "viewerUids" to emptyList<String>(),
                "expiresAt" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }.mapValues { it.value!! }
            db.collection("statuses").document(statusId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "statuses/$statusId")
            Result.failure(e)
        }
    }

    suspend fun markStatusViewed(status: StatusUpdate, viewerName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (uid == status.userId) return Result.success(Unit)
        return try {
            if (uid !in status.viewerUids) {
                db.collection("statuses").document(status.id).update(
                    mapOf(
                        "viewerUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("statuses").document(status.id)
                .collection("views").document(uid)
                .set(
                    mapOf(
                        "statusId" to status.id,
                        "viewerUid" to uid,
                        "viewerName" to viewerName.take(80).ifBlank { "Smile User" },
                        "ownerUid" to status.userId,
                        "viewedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "statuses/${status.id}")
            Result.failure(e)
        }
    }

    suspend fun getStatusViewers(statusId: String): List<StatusView> = try {
        val uid = requireUserId()
        db.collection("statuses").document(statusId).collection("views")
            .whereEqualTo("ownerUid", uid)
            .get().await()
            .toObjects(StatusView::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (_: Exception) {
        emptyList()
    }

    // --- VOICE & VIDEO CALL SIGNALING & HISTORY ---

    fun observeCalls(userId: String): Flow<List<CallRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("calls")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "calls", uid)
        )
    }

    suspend fun startCall(
        callerName: String,
        title: String,
        participantUids: List<String>,
        callType: CallType,
        groupCall: Boolean,
        sdpOffer: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val participants = (listOf(uid) + participantUids).distinct().take(20)
        if (participants.size < 2) {
            return Result.failure(IllegalArgumentException("Select at least one participant to call."))
        }
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        return try {
            db.collection("calls").document(callId).set(
                mapOf(
                    "callerId" to uid,
                    "callerName" to callerName.take(80).ifBlank { "Smile User" },
                    "title" to title.take(100).ifBlank { "Smile Call" },
                    "participantUids" to participants,
                    "callType" to callType.key,
                    "groupCall" to groupCall,
                    "callStatus" to CallStatus.RINGING.key,
                    "sdpOffer" to sdpOffer.take(8000),
                    "sdpAnswer" to "",
                    "iceCandidates" to emptyList<String>(),
                    "durationSeconds" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(callId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "calls/$callId")
            Result.failure(e)
        }
    }

    suspend fun updateCallState(
        callId: String,
        status: CallStatus,
        sdpAnswer: String? = null,
        durationSeconds: Long? = null
    ): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val updates = mutableMapOf<String, Any>(
                "callStatus" to status.key,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (sdpAnswer != null) updates["sdpAnswer"] = sdpAnswer.take(8000)
            if (durationSeconds != null) updates["durationSeconds"] = durationSeconds.coerceIn(0L, 86400L)
            db.collection("calls").document(callId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
            Result.failure(e)
        }
    }

    // --- COMMUNITIES ---

    fun observeCommunities(userId: String): Flow<List<Community>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("communities")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Community::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "communities", uid)
        )
    }

    suspend fun createCommunity(
        name: String,
        description: String,
        announcementText: String,
        memberUids: List<String>,
        groupIds: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = name.trim().take(100)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Community name required"))
        val communityId = "comm_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        return try {
            db.collection("communities").document(communityId).set(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(400),
                    "ownerId" to uid,
                    "adminUids" to listOf(uid),
                    "memberUids" to (listOf(uid) + memberUids).distinct().take(200),
                    "groupIds" to groupIds.distinct().take(50),
                    "announcementText" to announcementText.trim().take(500).ifBlank { "Welcome to $cleanName!" },
                    "avatarColor" to "#FFD600",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(communityId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "communities/$communityId")
            Result.failure(e)
        }
    }

    suspend fun postCommunityAnnouncement(communityId: String, announcement: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("communities").document(communityId).update(
                mapOf(
                    "announcementText" to announcement.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "communities/$communityId")
            Result.failure(e)
        }
    }

    suspend fun linkGroupToCommunity(communityId: String, groupId: String): Result<Unit> {
        auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("communities").document(communityId).update(
                mapOf(
                    "groupIds" to FieldValue.arrayUnion(groupId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("chats").document(groupId).update(
                mapOf(
                    "communityId" to communityId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "communities/$communityId")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        return try {
            val profile = getUserProfileOnce(uid)
            if (profile != null && profile.smileId.isNotBlank()) {
                runCatching { db.collection("smileIds").document(profile.smileId).delete().await() }
            }
            runCatching { db.collection("userProfiles").document(uid).delete().await() }
            runCatching { db.collection("users").document(uid).delete().await() }
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
