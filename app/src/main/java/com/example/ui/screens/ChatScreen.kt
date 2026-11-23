package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallType
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.MessageType
import com.example.data.model.UserContact
import com.example.data.model.UserProfile
import com.example.ui.components.DeliveryStatusTicks
import com.example.ui.components.SmileAvatar
import com.example.ui.i18n.LocalSmileStrings
import com.example.ui.theme.SmileDarkBubbleOutgoing
import com.example.ui.theme.SmileErrorRed
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileOnlineGreen
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowBubble
import com.example.ui.theme.SmileYellowSoft
import com.example.ui.viewmodel.ChatDetailViewModel
import com.example.ui.viewmodel.DecryptedChatMessage
import com.example.ui.viewmodel.UiState
import com.example.utils.MediaAndDeviceHelpers
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatDetailViewModel,
    myDisplayName: String,
    wallpaperId: String,
    darkMode: Boolean,
    readReceiptsEnabled: Boolean,
    allChats: List<ChatRoom>,
    contacts: List<UserContact>,
    discoverable: List<UserProfile>,
    onStartCall: (title: String, participantUids: List<String>, callType: CallType, groupCall: Boolean) -> Unit,
    onBlockUser: (uid: String, name: String) -> Unit,
    onReportUser: (uid: String, type: String, reason: String, details: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    val room by viewModel.chatRoom.collectAsStateWithLifecycle()
    val peer by viewModel.peerProfile.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val safetyFingerprint by viewModel.safetyFingerprint.collectAsStateWithLifecycle()

    val myUid = viewModel.currentUserId
    var inputText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<DecryptedChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<DecryptedChatMessage?>(null) }
    var selectedMessage by remember { mutableStateOf<DecryptedChatMessage?>(null) }
    var forwardMessage by remember { mutableStateOf<DecryptedChatMessage?>(null) }

    var showEmojiDrawer by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var messageSearchQuery by remember { mutableStateOf("") }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showContactShareDialog by remember { mutableStateOf(false) }
    var showLocationConsentDialog by remember { mutableStateOf(false) }

    var isRecording by remember { mutableStateOf(false) }
    val recorder = remember(context) { MediaAndDeviceHelpers.VoiceNoteRecorder(context) }
    val listState = rememberLazyListState()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = MediaAndDeviceHelpers.compressImageUriToBase64(context, uri, 640, 72)
            if (base64 != null) {
                viewModel.sendTextOrMediaMessage(
                    senderName = myDisplayName,
                    messageType = MessageType.IMAGE.key,
                    plaintext = "Photo",
                    mediaName = "photo.jpg",
                    mediaMime = "image/jpeg",
                    mediaData = base64
                )
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (docName, docMime) = MediaAndDeviceHelpers.readDocumentMeta(context, uri)
            viewModel.sendTextOrMediaMessage(
                senderName = myDisplayName,
                messageType = MessageType.DOCUMENT.key,
                plaintext = "Document: $docName",
                mediaName = docName,
                mediaMime = docMime
            )
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && recorder.start()) {
            isRecording = true
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val coords = MediaAndDeviceHelpers.getLastKnownLocationText(context) ?: "Location unavailable"
            viewModel.sendTextOrMediaMessage(
                senderName = myDisplayName,
                messageType = MessageType.LOCATION.key,
                plaintext = "Shared Location: $coords"
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.stopAndDiscard()
            viewModel.setTyping(false)
        }
    }

    val chatTitle = peer?.displayName ?: room?.title ?: "Chat"
    val isGroup = room?.typeEnum() == ChatType.GROUP
    val peerTyping = room?.typingUids?.any { it != myUid } == true
    val subtitle = when {
        peerTyping -> strings.typing
        isGroup -> "${room?.memberUids?.size ?: 0} members"
        peer?.online == true && peer?.lastSeenVisible != "NOBODY" -> strings.online
        else -> if (safetyFingerprint != null) "🔒 ${strings.e2eeBadge}" else "SMILE Chat"
    }

    val wallpaperBg = remember(wallpaperId, darkMode) {
        if (darkMode) Color(0xFF121317)
        else when (wallpaperId) {
            "smile_mint" -> Color(0xFFF0FDF4)
            "smile_sky" -> Color(0xFFF0F9FF)
            "smile_minimal" -> Color(0xFFF8F8FA)
            else -> Color(0xFFFFFCEB)
        }
    }

    val msgs = (messagesState as? UiState.Success)?.data ?: emptyList()
    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) {
            listState.animateScrollToItem(msgs.lastIndex)
            viewModel.markIncomingRead(msgs, readReceiptsEnabled)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = wallpaperBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SmileYellow)
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileInk)
                    }
                    SmileAvatar(
                        name = chatTitle,
                        colorHex = peer?.avatarColor ?: room?.avatarColor ?: "#FFFFFF",
                        avatarDataBase64 = peer?.avatarData,
                        size = 42.dp,
                        showOnlineDot = !isGroup,
                        online = peer?.online == true
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { if (isGroup) showGroupInfoDialog = true else showSafetyDialog = true }
                    ) {
                        Text(
                            text = chatTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = SmileInk,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (peerTyping) SmileOnlineGreen else SmileInk.copy(alpha = 0.78f),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = {
                            val peers = room?.memberUids?.filter { it != myUid } ?: emptyList()
                            if (peers.isNotEmpty()) onStartCall(chatTitle, peers, CallType.VOICE, isGroup)
                        },
                        modifier = Modifier.testTag("chat_voice_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = strings.voiceCall, tint = SmileInk)
                    }
                    IconButton(
                        onClick = {
                            val peers = room?.memberUids?.filter { it != myUid } ?: emptyList()
                            if (peers.isNotEmpty()) onStartCall(chatTitle, peers, CallType.VIDEO, isGroup)
                        },
                        modifier = Modifier.testTag("chat_video_call_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = strings.videoCall, tint = SmileInk)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Chat menu", tint = SmileInk)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Search messages") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                onClick = { showMenu = false; showSearch = !showSearch }
                            )
                            if (safetyFingerprint != null) {
                                DropdownMenuItem(
                                    text = { Text("Encryption safety code") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    onClick = { showMenu = false; showSafetyDialog = true }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Disappearing messages") },
                                leadingIcon = { Icon(Icons.Default.Timer, null) },
                                onClick = { showMenu = false; showDisappearingDialog = true }
                            )
                            if (isGroup) {
                                DropdownMenuItem(
                                    text = { Text("Group info & invite") },
                                    leadingIcon = { Icon(Icons.Default.Group, null) },
                                    onClick = { showMenu = false; showGroupInfoDialog = true }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Export chat") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showMenu = false
                                    val exportText = viewModel.buildChatExportText()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, exportText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export Chat"))
                                }
                            )
                            if (!isGroup && peer != null) {
                                DropdownMenuItem(
                                    text = { Text("Block user") },
                                    leadingIcon = { Icon(Icons.Default.Block, null) },
                                    onClick = {
                                        showMenu = false
                                        onBlockUser(peer!!.userId, peer!!.displayName)
                                        onBack()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Report") },
                                leadingIcon = { Icon(Icons.Default.Report, null) },
                                onClick = { showMenu = false; showReportDialog = true }
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = showSearch) {
                    OutlinedTextField(
                        value = messageSearchQuery,
                        onValueChange = { messageSearchQuery = it },
                        placeholder = { Text("Search in conversation...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .background(SmileWhite, RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if ((room?.disappearingHours ?: 0L) > 0L) {
                Surface(color = SmileYellowSoft, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "⏱ Disappearing messages enabled (${room?.disappearingHours}h)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmileInk,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val st = messagesState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SmileYellow)
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(st.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        val visible = st.data.filter {
                            messageSearchQuery.isBlank() || it.displayText.contains(messageSearchQuery, ignoreCase = true)
                        }
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Surface(color = SmileYellowSoft, shape = RoundedCornerShape(12.dp)) {
                                        Text(
                                            text = if (isGroup) "SMILE Group Chat • Secured in transit & at rest"
                                            else "🔒 Messages & calls are end-to-end encrypted on SMILE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SmileInk,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            items(visible, key = { it.raw.id }) { item ->
                                MessageBubbleRow(
                                    message = item,
                                    isMine = item.raw.senderId == myUid,
                                    isGroup = isGroup,
                                    darkMode = darkMode,
                                    onLongClick = { selectedMessage = item }
                                )
                            }
                        }
                    }
                }
            }

            // Reply / Edit banner
            val bannerTarget = editingMessage ?: replyTarget
            if (bannerTarget != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Box(Modifier.width(4.dp).height(30.dp).background(SmileYellow, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingMessage != null) "Editing message" else "Replying to ${bannerTarget.raw.senderName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = bannerTarget.displayText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyTarget = null; editingMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
            }

            // Emoji & Sticker quick drawer
            AnimatedVisibility(visible = showEmojiDrawer) {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            listOf("😀", "😂", "❤️", "🙏", "🔥", "🎉", "🥳", "👍").forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .clickable { inputText += emoji }
                                        .padding(6.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            listOf("😊 SMILE!", "💛 NAMASTE", "🚀 LET'S GO", "✨ AWESOME").forEach { sticker ->
                                Surface(
                                    color = SmileYellowSoft,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable {
                                        showEmojiDrawer = false
                                        viewModel.sendTextOrMediaMessage(
                                            senderName = myDisplayName,
                                            messageType = MessageType.STICKER.key,
                                            plaintext = sticker
                                        )
                                    }
                                ) {
                                    Text(sticker, style = MaterialTheme.typography.labelSmall, color = SmileInk, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Attachments quick sheet
            AnimatedVisibility(visible = showAttachSheet) {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        AttachActionChip(Icons.Default.Image, "Photo") {
                            showAttachSheet = false
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        AttachActionChip(Icons.Default.Description, "Doc/PDF") {
                            showAttachSheet = false
                            docPicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "*/*"))
                        }
                        AttachActionChip(Icons.Default.LocationOn, "Location") {
                            showAttachSheet = false
                            showLocationConsentDialog = true
                        }
                        AttachActionChip(Icons.Default.Contacts, "Contact") {
                            showAttachSheet = false
                            showContactShareDialog = true
                        }
                    }
                }
            }

            // Input bar
            val announcementsBlocked = room?.announcementsOnly == true && myUid !in (room?.adminUids ?: emptyList())
            if (announcementsBlocked) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Only group admins can send messages in this announcement group.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(26.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            IconButton(onClick = { showEmojiDrawer = !showEmojiDrawer; showAttachSheet = false }) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji & stickers")
                            }
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = {
                                    inputText = it
                                    viewModel.setTyping(it.isNotEmpty())
                                },
                                placeholder = { Text(strings.typeMessage) },
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input")
                            )
                            IconButton(onClick = { showAttachSheet = !showAttachSheet; showEmojiDrawer = false }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach media or file")
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (inputText.isNotBlank()) {
                        Surface(
                            color = SmileYellow,
                            shape = CircleShape,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val editing = editingMessage
                                    val textToSend = inputText.trim()
                                    if (editing != null) {
                                        viewModel.editMessage(editing.raw.id, textToSend, editing.raw.e2ee)
                                        editingMessage = null
                                    } else {
                                        viewModel.sendTextOrMediaMessage(
                                            senderName = myDisplayName,
                                            messageType = MessageType.TEXT.key,
                                            plaintext = textToSend,
                                            replyToId = replyTarget?.raw?.id ?: "",
                                            replyPreview = replyTarget?.displayText?.take(120) ?: ""
                                        )
                                        replyTarget = null
                                    }
                                    inputText = ""
                                    viewModel.setTyping(false)
                                }
                                .testTag("send_message_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = SmileInk)
                            }
                        }
                    } else {
                        Surface(
                            color = if (isRecording) SmileErrorRed else SmileYellow,
                            shape = CircleShape,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (isRecording) {
                                        isRecording = false
                                        val audioB64 = recorder.stopAndEncodeBase64()
                                        if (audioB64 != null) {
                                            viewModel.sendTextOrMediaMessage(
                                                senderName = myDisplayName,
                                                messageType = MessageType.AUDIO.key,
                                                plaintext = "Voice Note",
                                                mediaName = "voice_note.m4a",
                                                mediaMime = "audio/mp4",
                                                mediaData = audioB64
                                            )
                                        }
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                            PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            if (recorder.start()) isRecording = true
                                        } else {
                                            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("voice_record_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop recording" else "Record voice note",
                                    tint = if (isRecording) SmileWhite else SmileInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Message Action Dialog
    selectedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = { Text("Message Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        listOf("❤️", "😂", "👍", "🙏", "🔥").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.toggleReaction(msg.raw, emoji)
                                        selectedMessage = null
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                    TextButton(onClick = { replyTarget = msg; selectedMessage = null }) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null); Spacer(Modifier.width(8.dp)); Text("Reply")
                    }
                    TextButton(onClick = { forwardMessage = msg; selectedMessage = null }) {
                        Icon(Icons.AutoMirrored.Filled.Forward, null); Spacer(Modifier.width(8.dp)); Text("Forward")
                    }
                    TextButton(onClick = { viewModel.toggleStar(msg.raw); selectedMessage = null }) {
                        Icon(Icons.Default.Star, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (myUid in msg.raw.starredBy) "Unstar" else "Star message")
                    }
                    TextButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("SMILE Message", msg.displayText))
                        selectedMessage = null
                    }) {
                        Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copy text")
                    }
                    if (msg.raw.senderId == myUid && !msg.raw.deletedForEveryone) {
                        TextButton(onClick = {
                            editingMessage = msg
                            inputText = msg.displayText
                            selectedMessage = null
                        }) {
                            Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Edit message")
                        }
                        TextButton(onClick = {
                            viewModel.deleteForEveryone(msg.raw.id)
                            selectedMessage = null
                        }) {
                            Icon(Icons.Default.Delete, null, tint = SmileErrorRed)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete for everyone", color = SmileErrorRed)
                        }
                    }
                    TextButton(onClick = {
                        viewModel.deleteForMe(msg.raw.id)
                        selectedMessage = null
                    }) {
                        Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete for me")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selectedMessage = null }) { Text("Close") } }
        )
    }

    // Forward Picker
    forwardMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { forwardMessage = null },
            title = { Text("Forward to Chat") },
            text = {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(allChats, key = { it.id }) { target ->
                        TextButton(
                            onClick = {
                                if (target.id == room?.id) {
                                    viewModel.sendTextOrMediaMessage(myDisplayName, MessageType.TEXT.key, msg.displayText, forwarded = true)
                                } else {
                                    viewModel.forwardMessageToTarget(target, myDisplayName, msg.displayText)
                                }
                                forwardMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(target.title, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { forwardMessage = null }) { Text("Cancel") } }
        )
    }

    // Safety Fingerprint Dialog
    if (showSafetyDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyDialog = false },
            title = { Text("End-to-End Encryption") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This 1-on-1 chat uses ECDH P-256 + AES-256-GCM. Compare this safety number with ${peer?.displayName ?: "your contact"}:")
                    Surface(color = SmileYellowSoft, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = safetyFingerprint ?: "Key exchange pending peer login",
                            style = MaterialTheme.typography.titleMedium,
                            color = SmileInk,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSafetyDialog = false }) { Text("Verified") } }
        )
    }

    // Disappearing Messages Dialog
    if (showDisappearingDialog) {
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing Messages") },
            text = {
                Column {
                    listOf(0L to "Off", 24L to "24 Hours", 168L to "7 Days").forEach { (hrs, label) ->
                        TextButton(
                            onClick = {
                                viewModel.updateDisappearingHours(hrs)
                                showDisappearingDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Group Management Dialog
    if (showGroupInfoDialog && room != null) {
        val currentRoom = room!!
        val isAdmin = myUid in currentRoom.adminUids || myUid in currentRoom.coAdminUids
        var groupName by remember(currentRoom.title) { mutableStateOf(currentRoom.title) }
        var groupDesc by remember(currentRoom.description) { mutableStateOf(currentRoom.description) }
        var annOnly by remember(currentRoom.announcementsOnly) { mutableStateOf(currentRoom.announcementsOnly) }

        AlertDialog(
            onDismissRequest = { showGroupInfoDialog = false },
            title = { Text("Group Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAdmin) {
                        OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Group Name") })
                        OutlinedTextField(value = groupDesc, onValueChange = { groupDesc = it }, label = { Text("Description") })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Announcements only", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Switch(checked = annOnly, onCheckedChange = { annOnly = it })
                        }
                    } else {
                        Text(currentRoom.title, fontWeight = FontWeight.Bold)
                        Text(currentRoom.description, style = MaterialTheme.typography.bodySmall)
                    }
                    if (currentRoom.inviteCode.isNotBlank()) {
                        Text("Invite link: smile://join/${currentRoom.inviteCode}", style = MaterialTheme.typography.labelMedium)
                    }
                    Text("Members (${currentRoom.memberUids.size})", style = MaterialTheme.typography.labelLarge)
                    currentRoom.memberUids.take(6).forEach { uid ->
                        val p = discoverable.firstOrNull { it.userId == uid }
                        val roleLabel = when {
                            uid in currentRoom.adminUids -> "Admin"
                            uid in currentRoom.coAdminUids -> "Co-Admin"
                            else -> "Member"
                        }
                        Text("• ${p?.displayName ?: uid.take(10)} ($roleLabel)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (isAdmin) {
                    Button(
                        onClick = {
                            viewModel.updateGroupInfo(title = groupName, description = groupDesc, announcementsOnly = annOnly)
                            showGroupInfoDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                    ) { Text("Save") }
                }
            },
            dismissButton = { TextButton(onClick = { showGroupInfoDialog = false }) { Text("Close") } }
        )
    }

    // Location consent dialog
    if (showLocationConsentDialog) {
        AlertDialog(
            onDismissRequest = { showLocationConsentDialog = false },
            title = { Text("Share Current Location?") },
            text = { Text("SMILE will read your approximate device location once and send it into this chat.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationConsentDialog = false
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            val coords = MediaAndDeviceHelpers.getLastKnownLocationText(context) ?: "Location unavailable"
                            viewModel.sendTextOrMediaMessage(myDisplayName, MessageType.LOCATION.key, "Shared Location: $coords")
                        } else {
                            locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) { Text("Share") }
            },
            dismissButton = { TextButton(onClick = { showLocationConsentDialog = false }) { Text("Cancel") } }
        )
    }

    // Contact share dialog
    if (showContactShareDialog) {
        AlertDialog(
            onDismissRequest = { showContactShareDialog = false },
            title = { Text("Share SMILE Contact") },
            text = {
                if (contacts.isEmpty()) {
                    Text("No saved contacts yet.")
                } else {
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(contacts, key = { it.contactUid }) { c ->
                            TextButton(
                                onClick = {
                                    viewModel.sendTextOrMediaMessage(
                                        senderName = myDisplayName,
                                        messageType = MessageType.CONTACT.key,
                                        plaintext = "Contact Card: ${c.displayName} (@${c.smileId})"
                                    )
                                    showContactShareDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${c.displayName} (@${c.smileId})")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showContactShareDialog = false }) { Text("Close") } }
        )
    }

    // Report dialog
    if (showReportDialog) {
        var reason by remember { mutableStateOf("Spam or Harassment") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Conversation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") })
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details") }, maxLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportUser(peer?.userId ?: room?.id ?: "", if (isGroup) "GROUP" else "USER", reason, details)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileErrorRed, contentColor = SmileWhite)
                ) { Text("Submit Report") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleRow(
    message: DecryptedChatMessage,
    isMine: Boolean,
    isGroup: Boolean,
    darkMode: Boolean,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val raw = message.raw
    val bubbleColor = when {
        isMine && darkMode -> SmileDarkBubbleOutgoing
        isMine -> SmileYellowBubble
        else -> MaterialTheme.colorScheme.surface
    }
    val timeLabel = remember(raw.createdAt) {
        raw.createdAt?.toDate()?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) } ?: ""
    }
    val imageBitmap = remember(raw.mediaData, raw.messageType) {
        if (raw.typeEnum() == MessageType.IMAGE && !raw.mediaData.isNullOrBlank()) {
            runCatching {
                val bytes = Base64.decode(raw.mediaData, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        } else null
    }
    var playingAudio by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isGroup && !isMine) {
                    Text(
                        text = raw.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = SmileOnlineGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (raw.forwarded) {
                    Text("↪ Forwarded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (raw.replyPreview.isNotBlank()) {
                    Surface(
                        color = SmileInk.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = raw.replyPreview,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = "Shared photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (raw.typeEnum() == MessageType.AUDIO && !raw.mediaData.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SmileYellow.copy(alpha = 0.35f))
                            .clickable {
                                if (!playingAudio) {
                                    playingAudio = true
                                    MediaAndDeviceHelpers.playBase64Audio(context, raw.mediaData) {
                                        playingAudio = false
                                    }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play voice note", tint = SmileInk)
                        Spacer(Modifier.width(6.dp))
                        Text(if (playingAudio) "Playing..." else "Voice Note", color = SmileInk, style = MaterialTheme.typography.labelLarge)
                    }
                } else if (raw.typeEnum() == MessageType.DOCUMENT) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, null, tint = SmileInk)
                        Spacer(Modifier.width(6.dp))
                        Text(raw.mediaName ?: message.displayText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text(
                        text = message.displayText,
                        style = if (raw.typeEnum() == MessageType.STICKER) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (raw.e2ee) {
                        Icon(Icons.Default.Lock, contentDescription = "Encrypted", modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(3.dp))
                    }
                    if (raw.edited) {
                        Text("edited • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (raw.starredBy.isNotEmpty()) {
                        Icon(Icons.Default.Star, contentDescription = "Starred", modifier = Modifier.size(12.dp), tint = SmileYellow)
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isMine) {
                        Spacer(Modifier.width(4.dp))
                        DeliveryStatusTicks(status = raw.deliveryEnum())
                    }
                }
            }
        }
        if (raw.reactions.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = raw.reactions.values.joinToString(" "),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AttachActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(color = SmileYellow, shape = CircleShape, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = SmileInk)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
