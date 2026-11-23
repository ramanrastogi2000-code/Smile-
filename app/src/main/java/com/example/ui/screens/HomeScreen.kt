package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.UserProfile
import com.example.ui.components.QrCodeCard
import com.example.ui.components.SmileAvatar
import com.example.ui.components.SmileLogo
import com.example.ui.i18n.LocalSmileStrings
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileOnlineGreen
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.ui.viewmodel.SmileViewModel
import com.example.ui.viewmodel.UiState
import com.example.utils.MediaAndDeviceHelpers
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: SmileViewModel,
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCall: (CallRecord) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val statusesState by viewModel.statusesState.collectAsStateWithLifecycle()
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val communitiesState by viewModel.communitiesState.collectAsStateWithLifecycle()
    val contacts by viewModel.contactsState.collectAsStateWithLifecycle()
    val discoverable by viewModel.discoverableProfiles.collectAsStateWithLifecycle()
    val announcements by viewModel.announcementsState.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefsState.collectAsStateWithLifecycle()
    val actionMsg by viewModel.actionMessage.collectAsStateWithLifecycle()

    val myProfile = (profileState as? UiState.Success)?.data
    val myUid = viewModel.currentUserId
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var overflowOpen by remember { mutableStateOf(false) }

    var showNewChatDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showStatusCreateDialog by remember { mutableStateOf(false) }
    var showNewCallDialog by remember { mutableStateOf(false) }
    var showNewCommunityDialog by remember { mutableStateOf(false) }
    var showContactConsentDialog by remember { mutableStateOf(false) }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val entries = MediaAndDeviceHelpers.readDeviceContacts(context)
            viewModel.syncDeviceContacts(entries)
        } else {
            viewModel.setActionMessage("Contacts permission denied. You can still search by Smile ID or +91 number.")
        }
    }

    LaunchedEffect(actionMsg) {
        if (!actionMsg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionMsg!!)
            viewModel.clearActionMessage()
        }
    }

    val chats = (chatsState as? UiState.Success)?.data ?: emptyList()
    val calls = (callsState as? UiState.Success)?.data ?: emptyList()
    val incomingCall = calls.firstOrNull {
        it.statusEnum() == CallStatus.RINGING && it.callerId != myUid
    }
    val unreadCount = chats.count {
        it.lastMessageSenderId.isNotBlank() && it.lastMessageSenderId != myUid && myUid !in it.archivedBy
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
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
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        SmileLogo(size = 36.dp, showRoundedSquareBg = false)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMILE",
                                style = MaterialTheme.typography.titleLarge,
                                color = SmileInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = myProfile?.let { "@${it.smileId}" } ?: strings.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = SmileInk.copy(alpha = 0.78f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setDarkMode(!localPrefs.darkMode) },
                            modifier = Modifier.testTag("toggle_dark_mode_button")
                        ) {
                            Icon(
                                imageVector = if (localPrefs.darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = strings.darkMode,
                                tint = SmileInk
                            )
                        }
                        IconButton(
                            onClick = {
                                searchOpen = !searchOpen
                                if (!searchOpen) searchQuery = ""
                            },
                            modifier = Modifier.testTag("home_search_button")
                        ) {
                            Icon(
                                imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = SmileInk
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { overflowOpen = true },
                                modifier = Modifier.testTag("home_menu_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = SmileInk)
                            }
                            DropdownMenu(
                                expanded = overflowOpen,
                                onDismissRequest = { overflowOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.newGroup) },
                                    leadingIcon = { Icon(Icons.Default.GroupAdd, null) },
                                    onClick = { overflowOpen = false; showNewGroupDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.newCommunity) },
                                    leadingIcon = { Icon(Icons.Default.Groups, null) },
                                    onClick = { overflowOpen = false; showNewCommunityDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.myProfileQr) },
                                    leadingIcon = { Icon(Icons.Default.QrCode2, null) },
                                    onClick = { overflowOpen = false; showQrDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.syncContacts) },
                                    leadingIcon = { Icon(Icons.Default.Contacts, null) },
                                    onClick = { overflowOpen = false; showContactConsentDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.inviteFriends) },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = {
                                        overflowOpen = false
                                        val shareText = "Join me on SMILE (बात करें, मुस्कुराएं!) — my Smile ID is @${myProfile?.smileId ?: "smile"}"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, strings.inviteFriends))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.settings) },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                                    onClick = { overflowOpen = false; onOpenSettings() }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = searchOpen) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(strings.searchPlaceholder) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(SmileWhite, RoundedCornerShape(14.dp))
                                .testTag("home_search_input")
                        )
                    }

                    if (incomingCall != null) {
                        Surface(
                            color = SmileOnlineGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCall(incomingCall) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = SmileWhite)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${strings.incomingCall}: ${incomingCall.callerName} (${incomingCall.callType})",
                                    color = SmileWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("TAP", color = SmileYellow, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        val navItems = listOf(
                            Triple(0, strings.chats, Icons.AutoMirrored.Filled.Chat),
                            Triple(1, strings.updates, Icons.Default.RadioButtonChecked),
                            Triple(2, strings.calls, Icons.Default.Call),
                            Triple(3, strings.communities, Icons.Default.Groups)
                        )
                        navItems.forEach { (idx, label, icon) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    if (idx == 0 && unreadCount > 0) {
                                        BadgedBox(badge = {
                                            Badge(containerColor = SmileYellow, contentColor = SmileInk) {
                                                Text("$unreadCount")
                                            }
                                        }) {
                                            Icon(icon, contentDescription = label)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SmileInk,
                                    indicatorColor = SmileYellow,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("tab_$idx")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> showNewChatDialog = true
                            1 -> showStatusCreateDialog = true
                            2 -> showNewCallDialog = true
                            3 -> showNewCommunityDialog = true
                        }
                    },
                    containerColor = SmileYellow,
                    contentColor = SmileInk,
                    modifier = Modifier.testTag("home_fab")
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            0 -> Icons.AutoMirrored.Filled.Chat
                            1 -> Icons.Default.Add
                            2 -> Icons.Default.Call
                            else -> Icons.Default.Groups
                        },
                        contentDescription = "Create"
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        val navItems = listOf(
                            Triple(0, strings.chats, Icons.AutoMirrored.Filled.Chat),
                            Triple(1, strings.updates, Icons.Default.RadioButtonChecked),
                            Triple(2, strings.calls, Icons.Default.Call),
                            Triple(3, strings.communities, Icons.Default.Groups)
                        )
                        navItems.forEach { (idx, label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (announcements.isNotEmpty()) {
                        val topAnnouncement = announcements.first()
                        Surface(
                            color = SmileYellowSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = SmileInk, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${topAnnouncement.title}: ${topAnnouncement.body}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SmileInk,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    when (selectedTab) {
                        0 -> ChatsTabContent(
                            chatsState = chatsState,
                            myUid = myUid,
                            searchQuery = searchQuery,
                            discoverableProfiles = discoverable,
                            onOpenChat = onOpenChat,
                            onPin = viewModel::togglePinChat,
                            onArchive = viewModel::toggleArchiveChat,
                            onMute = viewModel::toggleMuteChat,
                            onFindFriends = { showNewChatDialog = true }
                        )
                        1 -> UpdatesTabContent(
                            statusesState = statusesState,
                            myUid = myUid,
                            myName = myProfile?.displayName ?: "Me",
                            mutedUids = localPrefs.mutedStatusUids,
                            showCreateDialog = showStatusCreateDialog,
                            onDismissCreate = { showStatusCreateDialog = false },
                            onCreateStatus = { type, text, caption, bg, media, privacy ->
                                viewModel.postStatus(type, text, caption, bg, media, privacy)
                                showStatusCreateDialog = false
                            },
                            onMarkViewed = viewModel::markStatusViewed,
                            onFetchViewers = viewModel::fetchStatusViewers,
                            onToggleMute = viewModel::toggleMuteStatusAuthor,
                            onReplyStatus = { status, replyText ->
                                val peer = discoverable.firstOrNull { it.userId == status.userId }
                                    ?: UserProfile(userId = status.userId, displayName = status.authorName, smileId = status.authorSmileId)
                                val snippet = status.textContent.ifBlank { status.caption.ifBlank { "Status update" } }
                                viewModel.replyToStatusInChat(
                                    peer = peer,
                                    myDisplayName = myProfile?.displayName ?: "Smile User",
                                    statusSnippet = snippet,
                                    replyText = replyText,
                                    onChatReady = onOpenChat
                                )
                            }
                        )
                        2 -> CallsTabContent(
                            callsState = callsState,
                            myUid = myUid,
                            contacts = contacts,
                            discoverable = discoverable,
                            showNewCallDialog = showNewCallDialog,
                            onDismissNewCall = { showNewCallDialog = false },
                            onStartCall = { title, participants, type, isGroup ->
                                showNewCallDialog = false
                                viewModel.startCall(title, participants, type, isGroup) { callId ->
                                    onOpenCall(
                                        CallRecord(
                                            id = callId,
                                            callerId = myUid,
                                            callerName = myProfile?.displayName ?: "Me",
                                            title = title,
                                            participantUids = listOf(myUid) + participants,
                                            callType = type.key,
                                            groupCall = isGroup,
                                            callStatus = CallStatus.RINGING.key
                                        )
                                    )
                                }
                            },
                            onOpenActiveCall = onOpenCall
                        )
                        3 -> CommunitiesTabContent(
                            communitiesState = communitiesState,
                            chats = chats,
                            myUid = myUid,
                            discoverable = discoverable,
                            showCreateDialog = showNewCommunityDialog,
                            onDismissCreate = { showNewCommunityDialog = false },
                            onCreateCommunity = { name, desc, announcement, members, groups ->
                                viewModel.createCommunity(name, desc, announcement, members, groups)
                                showNewCommunityDialog = false
                            },
                            onPostAnnouncement = viewModel::postCommunityAnnouncement,
                            onLinkGroup = viewModel::linkGroupToCommunity,
                            onOpenChat = onOpenChat
                        )
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewChatDialog(
            discoverable = discoverable.filter { it.userId != myUid },
            onDismiss = { showNewChatDialog = false },
            onSearchQuery = { query ->
                viewModel.searchFriendAndOpenChat(query) { chatId ->
                    showNewChatDialog = false
                    onOpenChat(chatId)
                }
            },
            onSelectProfile = { profile ->
                viewModel.openChatWithUser(profile) { chatId ->
                    showNewChatDialog = false
                    onOpenChat(chatId)
                }
            },
            onRequestContactSync = {
                showNewChatDialog = false
                showContactConsentDialog = true
            }
        )
    }

    if (showNewGroupDialog) {
        NewGroupDialog(
            profiles = discoverable.filter { it.userId != myUid },
            onDismiss = { showNewGroupDialog = false },
            onCreate = { title, desc, members, announcementsOnly ->
                viewModel.createGroup(title, desc, members, announcementsOnly) { groupId ->
                    showNewGroupDialog = false
                    onOpenChat(groupId)
                }
            }
        )
    }

    if (showQrDialog && myProfile != null) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, "Connect with me on SMILE! Smile ID: @${myProfile.smileId}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Smile Profile"))
                }) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrDialog = false }) { Text("Close") }
            },
            text = {
                QrCodeCard(
                    displayName = myProfile.displayName,
                    smileId = myProfile.smileId,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    if (showContactConsentDialog) {
        AlertDialog(
            onDismissRequest = { showContactConsentDialog = false },
            title = { Text("Sync Phone Contacts?") },
            text = {
                Text("With your permission, SMILE reads phone numbers from your device contacts, hashes them locally with SHA-256, and checks which friends are already on SMILE. Your raw contact list is never uploaded in plaintext.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showContactConsentDialog = false
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            val entries = MediaAndDeviceHelpers.readDeviceContacts(context)
                            viewModel.syncDeviceContacts(entries)
                        } else {
                            contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) {
                    Text("Allow & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactConsentDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ChatsTabContent(
    chatsState: UiState<List<ChatRoom>>,
    myUid: String,
    searchQuery: String,
    discoverableProfiles: List<UserProfile>,
    onOpenChat: (String) -> Unit,
    onPin: (ChatRoom) -> Unit,
    onArchive: (ChatRoom) -> Unit,
    onMute: (ChatRoom) -> Unit,
    onFindFriends: () -> Unit
) {
    val strings = LocalSmileStrings.current
    var filterMode by remember { mutableStateOf("ALL") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            listOf("ALL" to "All", "GROUPS" to "Groups", "PINNED" to "Pinned", "ARCHIVED" to "Archived").forEach { (key, label) ->
                FilterChip(
                    selected = filterMode == key,
                    onClick = { filterMode = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmileYellow,
                        selectedLabelColor = SmileInk
                    )
                )
            }
        }

        when (chatsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SmileYellow)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(chatsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                val filtered = chatsState.data.filter { chat ->
                    val archived = myUid in chat.archivedBy
                    val matchesFilter = when (filterMode) {
                        "ARCHIVED" -> archived
                        "GROUPS" -> !archived && chat.typeEnum() == ChatType.GROUP
                        "PINNED" -> !archived && myUid in chat.pinnedBy
                        else -> !archived
                    }
                    val matchesSearch = searchQuery.isBlank() ||
                        chat.title.contains(searchQuery, ignoreCase = true) ||
                        chat.lastMessageText.contains(searchQuery, ignoreCase = true)
                    matchesFilter && matchesSearch
                }

                if (filtered.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_chats),
                            contentDescription = "No chats yet",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = strings.emptyChatsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = strings.emptyChatsSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onFindFriends,
                            colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk),
                            modifier = Modifier.testTag("empty_find_friends_button")
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.findFriends, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { chat ->
                            val peer = chat.peerUid(myUid)?.let { pid ->
                                discoverableProfiles.firstOrNull { it.userId == pid }
                            }
                            val displayTitle = peer?.displayName ?: chat.title
                            val isOnline = peer?.online == true
                            val isPinned = myUid in chat.pinnedBy
                            val isMuted = myUid in chat.mutedBy
                            var menuExpanded by remember { mutableStateOf(false) }

                            val timeLabel = remember(chat.updatedAt) {
                                chat.updatedAt?.toDate()?.let {
                                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
                                } ?: ""
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChat(chat.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("chat_item_${chat.id}")
                            ) {
                                SmileAvatar(
                                    name = displayTitle,
                                    colorHex = peer?.avatarColor ?: chat.avatarColor,
                                    avatarDataBase64 = peer?.avatarData,
                                    size = 52.dp,
                                    showOnlineDot = chat.typeEnum() == ChatType.DIRECT,
                                    online = isOnline
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (chat.typeEnum() == ChatType.GROUP) {
                                            Icon(
                                                Icons.Default.Group,
                                                contentDescription = "Group",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = timeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val subtitle = if (chat.typingUids.any { it != myUid }) {
                                            strings.typing
                                        } else {
                                            chat.lastMessageText.ifBlank { "Tap to start chatting" }
                                        }
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (chat.typingUids.any { it != myUid }) SmileOnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isPinned) {
                                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (isMuted) {
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Default.NotificationsOff, contentDescription = "Muted", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Chat options")
                                    }
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text(if (isPinned) "Unpin" else "Pin chat") },
                                            leadingIcon = { Icon(Icons.Default.PushPin, null) },
                                            onClick = { menuExpanded = false; onPin(chat) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isMuted) "Unmute" else "Mute notifications") },
                                            leadingIcon = { Icon(Icons.Default.NotificationsOff, null) },
                                            onClick = { menuExpanded = false; onMute(chat) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (myUid in chat.archivedBy) "Unarchive" else "Archive") },
                                            leadingIcon = { Icon(Icons.Default.Archive, null) },
                                            onClick = { menuExpanded = false; onArchive(chat) }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatDialog(
    discoverable: List<UserProfile>,
    onDismiss: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onSelectProfile: (UserProfile) -> Unit,
    onRequestContactSync: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chat on SMILE") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Smile ID (@name) or +91 Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("find_friend_input")
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSearchQuery(query) },
                        enabled = query.trim().length >= 3,
                        colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk),
                        modifier = Modifier.weight(1f).testTag("find_friend_submit")
                    ) {
                        Text("Find & Chat")
                    }
                    OutlinedButton(
                        onClick = onRequestContactSync,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sync Contacts")
                    }
                }
                if (discoverable.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("SMILE Directory", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(discoverable.take(25), key = { it.userId }) { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectProfile(user) }
                                    .padding(vertical = 8.dp)
                            ) {
                                SmileAvatar(user.displayName, user.avatarColor, user.avatarData, size = 38.dp, showOnlineDot = true, online = user.online)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(user.displayName, fontWeight = FontWeight.SemiBold)
                                    Text("@${user.smileId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun NewGroupDialog(
    profiles: List<UserProfile>,
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var announcementsOnly by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_group_name_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Announcements only (Admins post)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Switch(checked = announcementsOnly, onCheckedChange = { announcementsOnly = it })
                }
                if (profiles.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Select Members", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.height(140.dp)) {
                        items(profiles, key = { it.userId }) { u ->
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
                                    onCheckedChange = { checked ->
                                        if (checked) selected.add(u.userId) else selected.remove(u.userId)
                                    }
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
                onClick = { onCreate(name, desc, selected.toList(), announcementsOnly) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk),
                modifier = Modifier.testTag("create_group_confirm")
            ) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
