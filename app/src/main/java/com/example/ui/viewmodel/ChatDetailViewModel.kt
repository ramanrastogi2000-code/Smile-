package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.DeliveryStatus
import com.example.data.model.UserProfile
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
import javax.crypto.SecretKey

data class DecryptedChatMessage(
    val raw: ChatMessage,
    val displayText: String
)

class ChatDetailViewModel(
    private val repository: SmileRepository,
    private val localPrefs: SmileLocalPreferences,
    private val chatId: String,
    val currentUserId: String
) : ViewModel() {

    val chatRoom: StateFlow<ChatRoom?> = repository.observeChat(chatId)
        .asUiState("Failed to load chat")
        .combine(MutableStateFlow(Unit)) { s, _ -> (s as? UiState.Success)?.data }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _peerProfile = MutableStateFlow<UserProfile?>(null)
    val peerProfile: StateFlow<UserProfile?> = _peerProfile.asStateFlow()

    private val _sharedAesKey = MutableStateFlow<SecretKey?>(null)
    private val _safetyFingerprint = MutableStateFlow<String?>(null)
    val safetyFingerprint: StateFlow<String?> = _safetyFingerprint.asStateFlow()

    private val _lastSendTimestampMs = MutableStateFlow(0L)

    val messagesState: StateFlow<UiState<List<DecryptedChatMessage>>> = combine(
        repository.observeMessages(chatId, currentUserId).asUiState("Failed to load messages"),
        _sharedAesKey
    ) { state, sharedKey ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(state.message)
            is UiState.Success -> {
                val decrypted = state.data.map { msg ->
                    val text = when {
                        msg.deletedForEveryone -> "This message was deleted"
                        msg.e2ee && msg.iv.isNotBlank() && sharedKey != null ->
                            SmileCryptoManager.decryptWithSharedKey(msg.body, msg.iv, sharedKey)
                                ?: "[Encrypted message]"
                        else -> msg.body
                    }
                    DecryptedChatMessage(raw = msg, displayText = text)
                }
                UiState.Success(decrypted)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        viewModelScope.launch {
            chatRoom.collect { room ->
                val peerUid = room?.peerUid(currentUserId)
                if (peerUid != null && _peerProfile.value?.userId != peerUid) {
                    val peer = repository.getUserProfileOnce(peerUid)
                    _peerProfile.value = peer
                    val (myPub, myPriv) = localPrefs.getOrCreateKeyPair(currentUserId)
                    val peerPub = peer?.publicKey
                    if (!peerPub.isNullOrBlank()) {
                        _sharedAesKey.value = SmileCryptoManager.deriveSharedAesKey(myPriv, peerPub)
                        _safetyFingerprint.value = SmileCryptoManager.computeSafetyNumber(myPub, peerPub)
                    }
                }
            }
        }
    }

    fun sendTextOrMediaMessage(
        senderName: String,
        messageType: String,
        plaintext: String,
        replyToId: String = "",
        replyPreview: String = "",
        forwarded: Boolean = false,
        mediaName: String? = null,
        mediaMime: String? = null,
        mediaData: String? = null,
        onError: (String) -> Unit = {}
    ) {
        val room = chatRoom.value ?: return
        // Rate limit / spam protection (min 250ms between sends)
        val now = System.currentTimeMillis()
        if (now - _lastSendTimestampMs.value < 250L) return
        _lastSendTimestampMs.value = now

        val sharedKey = _sharedAesKey.value
        val useE2ee = room.typeEnum() == ChatType.DIRECT && sharedKey != null && plaintext.isNotEmpty()
        val encrypted = if (useE2ee) {
            SmileCryptoManager.encryptWithSharedKey(plaintext, sharedKey!!)
        } else null

        val chatPreview = when (messageType) {
            "IMAGE" -> "📷 Photo"
            "AUDIO" -> "🎤 Voice note"
            "DOCUMENT" -> "📄 ${mediaName ?: "Document"}"
            "LOCATION" -> "📍 Location"
            "CONTACT" -> "👤 Contact"
            "STICKER" -> plaintext
            else -> if (useE2ee) "🔒 Encrypted message" else plaintext.take(80)
        }

        viewModelScope.launch {
            repository.sendMessage(
                chat = room,
                senderName = senderName,
                messageType = messageType,
                body = encrypted?.ciphertextBase64 ?: plaintext,
                iv = encrypted?.ivBase64 ?: "",
                e2ee = useE2ee,
                replyToId = replyToId,
                replyPreview = replyPreview,
                forwarded = forwarded,
                mediaName = mediaName,
                mediaMime = mediaMime,
                mediaData = mediaData,
                previewTextForChatList = chatPreview
            ).onFailure {
                onError(it.localizedMessage ?: "Failed to send message")
            }
        }
    }

    fun forwardMessageToTarget(
        targetChat: ChatRoom,
        senderName: String,
        plaintext: String,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val peerUid = targetChat.peerUid(currentUserId)
            val useE2ee: Boolean
            val bodyToSend: String
            val ivToSend: String
            if (targetChat.typeEnum() == ChatType.DIRECT && peerUid != null) {
                val peer = repository.getUserProfileOnce(peerUid)
                val (_, myPriv) = localPrefs.getOrCreateKeyPair(currentUserId)
                val sharedKey = peer?.publicKey?.takeIf { it.isNotBlank() }?.let {
                    SmileCryptoManager.deriveSharedAesKey(myPriv, it)
                }
                val enc = sharedKey?.let { SmileCryptoManager.encryptWithSharedKey(plaintext, it) }
                useE2ee = enc != null
                bodyToSend = enc?.ciphertextBase64 ?: plaintext
                ivToSend = enc?.ivBase64 ?: ""
            } else {
                useE2ee = false
                bodyToSend = plaintext
                ivToSend = ""
            }
            repository.sendMessage(
                chat = targetChat,
                senderName = senderName,
                messageType = "TEXT",
                body = bodyToSend,
                iv = ivToSend,
                e2ee = useE2ee,
                forwarded = true,
                previewTextForChatList = if (useE2ee) "🔒 Forwarded message" else plaintext.take(80)
            ).onSuccess {
                onComplete("Forwarded to ${targetChat.title}")
            }.onFailure {
                onComplete(it.localizedMessage ?: "Forward failed")
            }
        }
    }

    fun editMessage(messageId: String, newPlaintext: String, wasE2ee: Boolean) {
        val sharedKey = _sharedAesKey.value
        val useE2ee = wasE2ee && sharedKey != null
        val enc = if (useE2ee) SmileCryptoManager.encryptWithSharedKey(newPlaintext, sharedKey!!) else null
        viewModelScope.launch {
            repository.editMessage(
                chatId = chatId,
                messageId = messageId,
                newBody = enc?.ciphertextBase64 ?: newPlaintext,
                newIv = enc?.ivBase64 ?: ""
            )
        }
    }

    fun deleteForEveryone(messageId: String) {
        viewModelScope.launch { repository.deleteMessageForEveryone(chatId, messageId) }
    }

    fun deleteForMe(messageId: String) {
        viewModelScope.launch { repository.deleteMessageForMe(chatId, messageId) }
    }

    fun toggleReaction(message: ChatMessage, emoji: String) {
        viewModelScope.launch { repository.toggleMessageReaction(chatId, message, emoji) }
    }

    fun toggleStar(message: ChatMessage) {
        viewModelScope.launch { repository.toggleStarMessage(chatId, message) }
    }

    fun setTyping(typing: Boolean) {
        viewModelScope.launch { repository.setTypingStatus(chatId, typing) }
    }

    fun markIncomingRead(messages: List<DecryptedChatMessage>, readReceiptsEnabled: Boolean) {
        if (!readReceiptsEnabled) return
        viewModelScope.launch {
            messages.filter {
                it.raw.senderId != currentUserId &&
                    it.raw.deliveryEnum() != DeliveryStatus.READ
            }.takeLast(10).forEach {
                repository.markMessageRead(chatId, it.raw.id)
            }
        }
    }

    fun updateDisappearingHours(hours: Long) {
        viewModelScope.launch { repository.updateDisappearingTimer(chatId, hours) }
    }

    fun updateGroupInfo(
        title: String? = null,
        description: String? = null,
        memberUids: List<String>? = null,
        coAdminUids: List<String>? = null,
        announcementsOnly: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateGroupManagement(chatId, title, description, memberUids, coAdminUids, announcementsOnly)
        }
    }

    fun buildChatExportText(): String {
        val roomTitle = chatRoom.value?.title ?: "SMILE Chat"
        val msgs = (messagesState.value as? UiState.Success)?.data ?: emptyList()
        return buildString {
            appendLine("=== SMILE Chat Export: $roomTitle ===")
            msgs.forEach { m ->
                appendLine("[${m.raw.senderName}]: ${m.displayText}")
            }
        }
    }
}
