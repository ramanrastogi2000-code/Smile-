package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.Community
import com.example.data.model.StatusUpdate
import com.example.data.model.StatusView
import com.example.data.model.UserContact
import com.example.data.model.UserProfile
import com.example.ui.components.SmileAvatar
import com.example.ui.i18n.LocalSmileStrings
import com.example.ui.theme.SmileErrorRed
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileOnlineGreen
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.ui.viewmodel.UiState
import com.example.utils.MediaAndDeviceHelpers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun UpdatesTabContent(
    statusesState: UiState<List<StatusUpdate>>,
    myUid: String,
    myName: String,
    mutedUids: Set<String>,
    showCreateDialog: Boolean,
    onDismissCreate: () -> Unit,
    onCreateStatus: (type: String, text: String, caption: String, bgColor: String, mediaData: String?, privacy: String) -> Unit,
    onMarkViewed: (StatusUpdate) -> Unit,
    onFetchViewers: suspend (String) -> List<StatusView>,
    onToggleMute: (String) -> Unit,
    onReplyStatus: (StatusUpdate, String) -> Unit
) {
    val strings = LocalSmileStrings.current
    var activeStatus by remember { mutableStateOf<StatusUpdate?>(null) }

    when (statusesState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmileYellow)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(statusesState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val all = statusesState.data
            val mine = all.filter { it.userId == myUid }
            val others = all.filter { it.userId != myUid && it.userId !in mutedUids }
            val recent = others.filter { myUid !in it.viewerUids }
            val viewed = others.filter { myUid in it.viewerUids }

            LazyColumn(
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (mine.isNotEmpty()) activeStatus = mine.first()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, SmileYellow, CircleShape)
                            ) {
                                SmileAvatar(name = myName, size = 46.dp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.myStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (mine.isEmpty()) strings.tapToAddStatus else "${mine.size} active update(s) • 24h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (recent.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.recentUpdates,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(recent, key = { it.id }) { status ->
                        StatusRowItem(
                            status = status,
                            isUnviewed = true,
                            onClick = {
                                onMarkViewed(status)
                                activeStatus = status
                            },
                            onToggleMute = { onToggleMute(status.userId) }
                        )
                    }
                }

                if (viewed.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.viewedUpdates,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(viewed, key = { it.id }) { status ->
                        StatusRowItem(
                            status = status,
                            isUnviewed = false,
                            onClick = { activeStatus = status },
                            onToggleMute = { onToggleMute(status.userId) }
                        )
                    }
                }

                if (mine.isEmpty() && others.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        ) {
                            Text("No status updates yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Share a text or photo status using the yellow + button. Updates expire automatically after 24 hours.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateStatusDialog(
            onDismiss = onDismissCreate,
            onCreate = onCreateStatus
        )
    }

    activeStatus?.let { status ->
        StatusViewerDialog(
            status = status,
            isMine = status.userId == myUid,
            onFetchViewers = onFetchViewers,
            onReply = { reply ->
                activeStatus = null
                onReplyStatus(status, reply)
            },
            onDismiss = { activeStatus = null }
        )
    }
}

@Composable
private fun StatusRowItem(
    status: StatusUpdate,
    isUnviewed: Boolean,
    onClick: () -> Unit,
    onToggleMute: () -> Unit
) {
    val timeLabel = remember(status.createdAt) {
        status.createdAt?.toDate()?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
        } ?: "Just now"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.5.dp, if (isUnviewed) SmileYellow else Color.Gray.copy(alpha = 0.4f), CircleShape)
        ) {
            SmileAvatar(name = status.authorName, size = 48.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(status.authorName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${status.caption.ifBlank { status.textContent.ifBlank { "Photo status" } }} • $timeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        IconButton(onClick = onToggleMute) {
            Icon(Icons.Default.NotificationsOff, contentDescription = "Mute status", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreateStatusDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String?, String) -> Unit
) {
    val context = LocalContext.current
    var textContent by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var bgColor by remember { mutableStateOf("#FFD600") }
    var privacy by remember { mutableStateOf("EVERYONE") }
    var mediaBase64 by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            mediaBase64 = MediaAndDeviceHelpers.compressImageUriToBase64(context, uri, 640, 72)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Status (24h)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (mediaBase64 != null) "Photo Attached ✓" else "Pick Photo Status")
                }
                if (mediaBase64 == null) {
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        label = { Text("Text Status") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("status_text_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#FFD600", "#16A34A", "#2563EB", "#DC2626", "#111111").forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(if (bgColor == hex) 2.5.dp else 1.dp, SmileWhite, CircleShape)
                                    .clickable { bgColor = hex }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("EVERYONE", "CONTACTS").forEach { mode ->
                        FilterChip(
                            selected = privacy == mode,
                            onClick = { privacy = mode },
                            label = { Text(mode.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmileYellow, selectedLabelColor = SmileInk)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val type = if (mediaBase64 != null) "IMAGE" else "TEXT"
                    onCreate(type, textContent, caption, bgColor, mediaBase64, privacy)
                },
                enabled = mediaBase64 != null || textContent.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk),
                modifier = Modifier.testTag("publish_status_button")
            ) {
                Text("Publish")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StatusViewerDialog(
    status: StatusUpdate,
    isMine: Boolean,
    onFetchViewers: suspend (String) -> List<StatusView>,
    onReply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var viewers by remember { mutableStateOf<List<StatusView>>(emptyList()) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val bitmap = remember(status.mediaData) {
        status.mediaData?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    val bgColor = remember(status.bgColor) {
        runCatching { Color(android.graphics.Color.parseColor(status.bgColor)) }.getOrDefault(SmileYellow)
    }

    LaunchedEffect(status.id) {
        if (isMine) {
            viewers = onFetchViewers(status.id)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (bitmap != null) Color.Black else bgColor)
                .safeDrawingPadding()
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Status photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    Text(
                        text = status.textContent,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (bgColor == SmileYellow) SmileInk else SmileWhite,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                SmileAvatar(name = status.authorName, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.authorName, color = SmileWhite, fontWeight = FontWeight.Bold)
                    Text("@${status.authorSmileId}", color = SmileWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SmileWhite)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(16.dp)
            ) {
                if (status.caption.isNotBlank()) {
                    Text(status.caption, color = SmileWhite, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                if (isMine) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                scope.launch { viewers = onFetchViewers(status.id) }
                                showViewersSheet = !showViewersSheet
                            }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = SmileYellow)
                        Spacer(Modifier.width(8.dp))
                        Text("${status.viewerUids.size} view(s)", color = SmileWhite, fontWeight = FontWeight.SemiBold)
                    }
                    if (showViewersSheet && viewers.isNotEmpty()) {
                        viewers.forEach { v ->
                            Text("• ${v.viewerName}", color = SmileWhite.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Reply to status...", color = SmileWhite.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { if (replyText.isNotBlank()) onReply(replyText) },
                            modifier = Modifier.background(SmileYellow, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply", tint = SmileInk)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallsTabContent(
    callsState: UiState<List<CallRecord>>,
    myUid: String,
    contacts: List<UserContact>,
    discoverable: List<UserProfile>,
    showNewCallDialog: Boolean,
    onDismissNewCall: () -> Unit,
    onStartCall: (title: String, participantUids: List<String>, callType: CallType, groupCall: Boolean) -> Unit,
    onOpenActiveCall: (CallRecord) -> Unit
) {
    when (callsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmileYellow)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(callsState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val calls = callsState.data
            if (calls.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(56.dp), tint = SmileYellow)
                        Spacer(Modifier.height(12.dp))
                        Text("No calls yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Tap the yellow call button to start a 1-on-1 or Group Voice/Video call.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(calls, key = { it.id }) { call ->
                        val isOutgoing = call.callerId == myUid
                        val isMissed = call.statusEnum() == CallStatus.MISSED || call.statusEnum() == CallStatus.DECLINED
                        val label = if (isOutgoing) call.title else call.callerName
                        val timeText = remember(call.createdAt) {
                            call.createdAt?.toDate()?.let {
                                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(it)
                            } ?: ""
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (call.statusEnum() == CallStatus.RINGING || call.statusEnum() == CallStatus.ACTIVE) {
                                        onOpenActiveCall(call)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            SmileAvatar(name = label, size = 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isMissed) SmileErrorRed else MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            isMissed -> Icons.AutoMirrored.Filled.CallMissed
                                            isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                                            else -> Icons.AutoMirrored.Filled.CallReceived
                                        },
                                        contentDescription = null,
                                        tint = if (isMissed) SmileErrorRed else SmileOnlineGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${call.callStatus} • $timeText" + if (call.durationSeconds > 0) " (${call.durationSeconds}s)" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = {
                                val peers = call.participantUids.filter { it != myUid }
                                onStartCall(label, peers, call.typeEnum(), call.groupCall)
                            }) {
                                Icon(
                                    imageVector = if (call.typeEnum() == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = "Call back",
                                    tint = SmileOnlineGreen
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 76.dp, end = 16.dp))
                    }
                }
            }
        }
    }

    if (showNewCallDialog) {
        val candidates = discoverable.filter { it.userId != myUid }
        val selected = remember { mutableStateListOf<String>() }
        var callType by remember { mutableStateOf(CallType.VOICE) }
        AlertDialog(
            onDismissRequest = onDismissNewCall,
            title = { Text("Start Voice or Video Call") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = callType == CallType.VOICE,
                            onClick = { callType = CallType.VOICE },
                            label = { Text("Voice Call") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmileYellow, selectedLabelColor = SmileInk)
                        )
                        FilterChip(
                            selected = callType == CallType.VIDEO,
                            onClick = { callType = CallType.VIDEO },
                            label = { Text("Video Call") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmileYellow, selectedLabelColor = SmileInk)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (candidates.isEmpty()) {
                        Text("Find or sync contacts first to place a call.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LazyColumn(modifier = Modifier.height(180.dp)) {
                            items(candidates, key = { it.userId }) { u ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (u.userId in selected) selected.remove(u.userId) else selected.add(u.userId)
                                        }
                                ) {
                                    Checkbox(
                                        checked = u.userId in selected,
                                        onCheckedChange = { c -> if (c) selected.add(u.userId) else selected.remove(u.userId) }
                                    )
                                    Text("${u.displayName} (@${u.smileId})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val names = candidates.filter { it.userId in selected }.joinToString(", ") { it.displayName }
                        onStartCall(names.ifBlank { "SMILE Call" }, selected.toList(), callType, selected.size > 1)
                    },
                    enabled = selected.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) {
                    Text("Call Now")
                }
            },
            dismissButton = { TextButton(onClick = onDismissNewCall) { Text("Cancel") } }
        )
    }
}

@Composable
fun CommunitiesTabContent(
    communitiesState: UiState<List<Community>>,
    chats: List<ChatRoom>,
    myUid: String,
    discoverable: List<UserProfile>,
    showCreateDialog: Boolean,
    onDismissCreate: () -> Unit,
    onCreateCommunity: (name: String, desc: String, announcement: String, members: List<String>, groupIds: List<String>) -> Unit,
    onPostAnnouncement: (communityId: String, text: String) -> Unit,
    onLinkGroup: (communityId: String, groupId: String) -> Unit,
    onOpenChat: (String) -> Unit
) {
    val strings = LocalSmileStrings.current
    var selectedCommunity by remember { mutableStateOf<Community?>(null) }

    when (communitiesState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmileYellow)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(communitiesState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val communities = communitiesState.data
            if (communities.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(60.dp), tint = SmileYellow)
                        Spacer(Modifier.height(12.dp))
                        Text("Bring groups together", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Create a Community to organize multiple groups and broadcast official announcements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(communities, key = { it.id }) { comm ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCommunity = comm }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SmileAvatar(name = comm.name, colorHex = comm.avatarColor, size = 48.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(comm.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${comm.memberUids.size} members • ${comm.groupIds.size} linked groups",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (comm.announcementText.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Surface(color = SmileYellowSoft, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                                            Icon(Icons.Default.Campaign, null, tint = SmileInk, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(comm.announcementText, color = SmileInk, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var announcement by remember { mutableStateOf("") }
        val selectedMembers = remember { mutableStateListOf<String>() }
        val selectedGroups = remember { mutableStateListOf<String>() }
        val myGroups = chats.filter { it.typeEnum() == ChatType.GROUP }

        AlertDialog(
            onDismissRequest = onDismissCreate,
            title = { Text(strings.createCommunity) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Community Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("community_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        label = { Text("Initial Announcement") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (myGroups.isNotEmpty()) {
                        Text("Link Existing Groups", style = MaterialTheme.typography.labelMedium)
                        myGroups.take(4).forEach { g ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = g.id in selectedGroups,
                                    onCheckedChange = { c -> if (c) selectedGroups.add(g.id) else selectedGroups.remove(g.id) }
                                )
                                Text(g.title, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onCreateCommunity(name, desc, announcement, selectedMembers.toList(), selectedGroups.toList()) },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = onDismissCreate) { Text("Cancel") } }
        )
    }

    selectedCommunity?.let { comm ->
        val isAdmin = myUid in comm.adminUids || myUid == comm.ownerId
        var newAnnouncement by remember { mutableStateOf(comm.announcementText) }
        val linkedGroups = chats.filter { it.id in comm.groupIds || it.communityId == comm.id }
        val unlinkedGroups = chats.filter { it.typeEnum() == ChatType.GROUP && it.id !in comm.groupIds }

        AlertDialog(
            onDismissRequest = { selectedCommunity = null },
            title = { Text(comm.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (comm.description.isNotBlank()) {
                        Text(comm.description, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isAdmin) {
                        OutlinedTextField(
                            value = newAnnouncement,
                            onValueChange = { newAnnouncement = it },
                            label = { Text("Update Announcement") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onPostAnnouncement(comm.id, newAnnouncement)
                                selectedCommunity = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Broadcast Announcement")
                        }
                    }
                    Text("Linked Groups (${linkedGroups.size})", style = MaterialTheme.typography.labelLarge)
                    linkedGroups.forEach { grp ->
                        OutlinedButton(
                            onClick = {
                                selectedCommunity = null
                                onOpenChat(grp.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(grp.title)
                        }
                    }
                    if (isAdmin && unlinkedGroups.isNotEmpty()) {
                        Text("Add Group to Community", style = MaterialTheme.typography.labelMedium)
                        unlinkedGroups.take(3).forEach { grp ->
                            TextButton(onClick = {
                                onLinkGroup(comm.id, grp.id)
                                selectedCommunity = null
                            }) {
                                Icon(Icons.Default.Link, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Link ${grp.title}")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCommunity = null }) { Text("Close") }
            }
        )
    }
}
