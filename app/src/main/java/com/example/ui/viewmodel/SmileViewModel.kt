package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.Community
import com.example.data.model.StatusUpdate
import com.example.data.model.StatusView
import com.example.data.model.UserContact
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.prefs.LocalPrefsState
import com.example.data.prefs.SmileLocalPreferences
import com.example.data.remote.SmileRepository
import com.example.security.SmileCryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmileViewModel(
    private val repository: SmileRepository,
    private val localPrefs: SmileLocalPreferences,
    val currentUserId: String
) : ViewModel() {

    val localPrefsState: StateFlow<LocalPrefsState> = localPrefs.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalPrefsState())

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .asUiState("Failed to load profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val userSettings: StateFlow<UiState<UserSettings?>> = repository.observeUserSettings(currentUserId)
        .asUiState("Failed to load settings")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val discoverableProfiles: StateFlow<List<UserProfile>> = repository.observeDiscoverableProfiles()
        .asUiState("Failed to load users")
        .combine(MutableStateFlow(Unit)) { state, _ ->
            (state as? UiState.Success)?.data ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val chatsState: StateFlow<UiState<List<ChatRoom>>> = repository.observeChats(currentUserId)
        .asUiState("Failed to load chats")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contactsState: StateFlow<List<UserContact>> = repository.observeContacts(currentUserId)
        .asUiState("Failed to load contacts")
        .combine(MutableStateFlow(Unit)) { s, _ -> (s as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedUsersState: StateFlow<List<BlockedUser>> = repository.observeBlockedUsers(currentUserId)
        .asUiState("Failed to load blocked users")
        .combine(MutableStateFlow(Unit)) { s, _ -> (s as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val statusesState: StateFlow<UiState<List<StatusUpdate>>> = repository.observeStatuses(currentUserId)
        .asUiState("Failed to load status updates")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val callsState: StateFlow<UiState<List<CallRecord>>> = repository.observeCalls(currentUserId)
        .asUiState("Failed to load calls")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val communitiesState: StateFlow<UiState<List<Community>>> = repository.observeCommunities(currentUserId)
        .asUiState("Failed to load communities")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val announcementsState: StateFlow<List<Announcement>> = repository.observeAnnouncements()
        .asUiState("Failed to load announcements")
        .combine(MutableStateFlow(Unit)) { s, _ -> (s as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notificationsState: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .asUiState("Failed to load notifications")
        .combine(MutableStateFlow(Unit)) { s, _ -> (s as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _myPublicKey = MutableStateFlow("")
    val myPublicKey: StateFlow<String> = _myPublicKey.asStateFlow()

    init {
        viewModelScope.launch {
            val (pubKey, _) = localPrefs.getOrCreateKeyPair(currentUserId)
            _myPublicKey.value = pubKey
            repository.updatePresence(true)
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun setActionMessage(msg: String) {
        _actionMessage.value = msg
    }

    fun unlockWithPin(pin: String): Boolean {
        val storedHash = localPrefsState.value.appLockPinHash
        val ok = storedHash.isBlank() || SmileCryptoManager.verifyPin(pin, storedHash)
        if (ok) _isAppUnlocked.value = true
        return ok
    }

    fun markSessionUnlocked() {
        _isAppUnlocked.value = true
    }

    fun saveProfile(
        displayName: String,
        smileId: String,
        about: String,
        phone: String,
        countryCode: String,
        avatarColor: String,
        avatarData: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val (pubKey, _) = localPrefs.getOrCreateKeyPair(currentUserId)
            val result = repository.saveProfileSetup(
                displayName = displayName,
                rawSmileId = smileId,
                about = about,
                rawPhone = phone,
                countryCode = countryCode,
                avatarColor = avatarColor,
                avatarData = avatarData,
                publicKeyBase64 = pubKey,
                languageCode = localPrefsState.value.languageCode,
                darkMode = localPrefsState.value.darkMode
            )
            result.onSuccess {
                _actionMessage.value = "Profile saved"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun searchFriendAndOpenChat(query: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val found = repository.findUserBySmileIdOrPhone(query)
            found.onSuccess { profile ->
                if (profile == null) {
                    _actionMessage.value = "No SMILE user found for '$query'"
                } else if (profile.userId == currentUserId) {
                    _actionMessage.value = "That is your own SMILE ID!"
                } else {
                    repository.saveContact(profile)
                    repository.getOrCreateDirectChat(profile).onSuccess(onChatReady)
                        .onFailure { _actionMessage.value = it.localizedMessage }
                }
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Search failed"
            }
        }
    }

    fun openChatWithUser(profile: UserProfile, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            repository.saveContact(profile)
            repository.getOrCreateDirectChat(profile)
                .onSuccess(onChatReady)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Unable to open chat" }
        }
    }

    fun replyToStatusInChat(
        peer: UserProfile,
        myDisplayName: String,
        statusSnippet: String,
        replyText: String,
        onChatReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.saveContact(peer)
            repository.getOrCreateDirectChat(peer).onSuccess { chatId ->
                val cleanReply = replyText.trim()
                if (cleanReply.isNotEmpty()) {
                    val chat = repository.getChatOnce(chatId)
                    if (chat != null) {
                        val (_, myPriv) = localPrefs.getOrCreateKeyPair(currentUserId)
                        val peerFull = repository.getUserProfileOnce(peer.userId)
                        val sharedKey = peerFull?.publicKey?.takeIf { it.isNotBlank() }?.let {
                            SmileCryptoManager.deriveSharedAesKey(myPriv, it)
                        }
                        val fullText = "Status reply (${statusSnippet.take(40)}): $cleanReply"
                        val enc = sharedKey?.let { SmileCryptoManager.encryptWithSharedKey(fullText, it) }
                        repository.sendMessage(
                            chat = chat,
                            senderName = myDisplayName,
                            messageType = "TEXT",
                            body = enc?.ciphertextBase64 ?: fullText,
                            iv = enc?.ivBase64 ?: "",
                            e2ee = enc != null,
                            replyPreview = statusSnippet.take(80),
                            previewTextForChatList = if (enc != null) "🔒 Status reply" else fullText.take(80)
                        )
                    }
                }
                onChatReady(chatId)
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Unable to reply to status"
            }
        }
    }

    fun createGroup(
        title: String,
        description: String,
        memberUids: List<String>,
        announcementsOnly: Boolean,
        onGroupCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.createGroupChat(title, description, memberUids, announcementsOnly)
                .onSuccess { groupId ->
                    _actionMessage.value = "Group '$title' created"
                    onGroupCreated(groupId)
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to create group" }
        }
    }

    fun syncDeviceContacts(entries: List<Pair<String, String>>) {
        viewModelScope.launch {
            repository.syncPhoneContacts(entries)
                .onSuccess { count ->
                    _actionMessage.value = "Synced $count SMILE contact(s)"
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Contact sync failed" }
        }
    }

    fun togglePinChat(chat: ChatRoom) {
        viewModelScope.launch {
            repository.toggleChatFlag(chat, "pinnedBy", currentUserId !in chat.pinnedBy)
        }
    }

    fun toggleArchiveChat(chat: ChatRoom) {
        viewModelScope.launch {
            repository.toggleChatFlag(chat, "archivedBy", currentUserId !in chat.archivedBy)
        }
    }

    fun toggleMuteChat(chat: ChatRoom) {
        viewModelScope.launch {
            repository.toggleChatFlag(chat, "mutedBy", currentUserId !in chat.mutedBy)
        }
    }

    fun postStatus(
        statusType: String,
        textContent: String,
        caption: String,
        bgColor: String,
        mediaData: String?,
        privacyMode: String
    ) {
        val profile = (userProfile.value as? UiState.Success)?.data
        val authorName = profile?.displayName ?: "Smile User"
        val smileId = profile?.smileId ?: "smile"
        viewModelScope.launch {
            repository.postStatus(
                authorName = authorName,
                authorSmileId = smileId,
                statusType = statusType,
                textContent = textContent,
                caption = caption,
                bgColor = bgColor,
                mediaData = mediaData,
                privacyMode = privacyMode,
                allowedUids = emptyList()
            ).onSuccess {
                _actionMessage.value = "Status published (expires in 24h)"
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to publish status"
            }
        }
    }

    fun markStatusViewed(status: StatusUpdate) {
        val profile = (userProfile.value as? UiState.Success)?.data
        val viewerName = profile?.displayName ?: "Smile User"
        viewModelScope.launch {
            repository.markStatusViewed(status, viewerName)
        }
    }

    suspend fun fetchStatusViewers(statusId: String): List<StatusView> =
        repository.getStatusViewers(statusId)

    fun toggleMuteStatusAuthor(targetUid: String) {
        viewModelScope.launch {
            localPrefs.toggleMutedStatusUser(targetUid)
        }
    }

    fun startCall(
        title: String,
        participantUids: List<String>,
        callType: CallType,
        groupCall: Boolean,
        onCallStarted: (String) -> Unit
    ) {
        val profile = (userProfile.value as? UiState.Success)?.data
        val callerName = profile?.displayName ?: "Smile User"
        val sdpOffer = "v=0\r\no=smile 1 2 IN IP4 0.0.0.0\r\ns=SMILE-${callType.key}\r\nt=0 0\r\n"
        viewModelScope.launch {
            repository.startCall(
                callerName = callerName,
                title = title,
                participantUids = participantUids,
                callType = callType,
                groupCall = groupCall,
                sdpOffer = sdpOffer
            ).onSuccess(onCallStarted)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to start call" }
        }
    }

    fun answerCall(callId: String) {
        val sdpAnswer = "v=0\r\no=smile-answer 1 2 IN IP4 0.0.0.0\r\ns=SMILE-ANSWER\r\nt=0 0\r\n"
        viewModelScope.launch {
            repository.updateCallState(callId, CallStatus.ACTIVE, sdpAnswer = sdpAnswer)
        }
    }

    fun endCall(callId: String, status: CallStatus, durationSeconds: Long) {
        viewModelScope.launch {
            repository.updateCallState(callId, status, durationSeconds = durationSeconds)
        }
    }

    fun createCommunity(
        name: String,
        description: String,
        announcement: String,
        memberUids: List<String>,
        groupIds: List<String>
    ) {
        viewModelScope.launch {
            repository.createCommunity(name, description, announcement, memberUids, groupIds)
                .onSuccess { _actionMessage.value = "Community '$name' created" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to create community" }
        }
    }

    fun postCommunityAnnouncement(communityId: String, text: String) {
        viewModelScope.launch {
            repository.postCommunityAnnouncement(communityId, text)
                .onSuccess { _actionMessage.value = "Announcement posted" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun linkGroupToCommunity(communityId: String, groupId: String) {
        viewModelScope.launch {
            repository.linkGroupToCommunity(communityId, groupId)
                .onSuccess { _actionMessage.value = "Group linked to community" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            localPrefs.setDarkMode(enabled)
            repository.updateUserSettingsFields(mapOf("darkMode" to enabled))
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            localPrefs.setLanguage(code)
            repository.updateUserSettingsFields(mapOf("language" to code))
        }
    }

    fun setWallpaper(wallpaperId: String) {
        viewModelScope.launch {
            localPrefs.setWallpaper(wallpaperId)
            repository.updateUserSettingsFields(mapOf("wallpaperId" to wallpaperId))
        }
    }

    fun configureAppLock(enabled: Boolean, pin: String?) {
        viewModelScope.launch {
            val hash = if (enabled && !pin.isNullOrBlank()) SmileCryptoManager.hashPinWithSalt(pin) else ""
            localPrefs.setAppLock(enabled, if (enabled) hash else "")
            val fields = mutableMapOf<String, Any>("appLockEnabled" to enabled)
            if (enabled && hash.isNotBlank()) fields["pinHash"] = hash
            repository.updateUserSettingsFields(fields)
            _actionMessage.value = if (enabled) "App PIN lock enabled" else "App PIN lock disabled"
        }
    }

    fun setTwoStepPin(pin: String) {
        viewModelScope.launch {
            val hash = SmileCryptoManager.hashPinWithSalt(pin)
            repository.updateUserSettingsFields(mapOf("twoStepHash" to hash))
                .onSuccess { _actionMessage.value = "Two-step verification updated" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun updateDataPreferences(autoDownloadWifi: Boolean, dataSaver: Boolean, readReceipts: Boolean, notifications: Boolean) {
        viewModelScope.launch {
            repository.updateUserSettingsFields(
                mapOf(
                    "autoDownloadWifi" to autoDownloadWifi,
                    "dataSaver" to dataSaver,
                    "readReceipts" to readReceipts,
                    "notificationsEnabled" to notifications
                )
            )
        }
    }

    fun updatePrivacyVisibility(photo: String, about: String, lastSeen: String, discoverable: Boolean) {
        viewModelScope.launch {
            repository.updateUserPrivacyFields(photo, about, lastSeen, discoverable)
                .onSuccess { _actionMessage.value = "Privacy settings updated" }
        }
    }

    fun blockUser(targetUid: String, targetName: String) {
        viewModelScope.launch {
            repository.blockUser(targetUid, targetName)
                .onSuccess { _actionMessage.value = "$targetName blocked" }
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            repository.unblockUser(targetUid)
                .onSuccess { _actionMessage.value = "User unblocked" }
        }
    }

    fun submitReport(targetUid: String, targetType: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitReport(targetUid, targetType, reason, details)
                .onSuccess { _actionMessage.value = "Report submitted securely" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun recordBackupNow() {
        viewModelScope.launch {
            localPrefs.recordBackupTimestamp(System.currentTimeMillis())
            _actionMessage.value = "Cloud backup synced with Firestore"
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccountData()
                .onSuccess { onDeleted() }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Re-sign in recently to delete account" }
        }
    }

    fun updatePresence(online: Boolean) {
        viewModelScope.launch {
            repository.updatePresence(online)
        }
    }
}
