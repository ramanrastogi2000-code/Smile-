package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.QrCodeCard
import com.example.ui.components.SmileAvatar
import com.example.ui.i18n.LocalSmileStrings
import com.example.ui.theme.SmileErrorRed
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.viewmodel.SmileViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SmileViewModel,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val settingsState by viewModel.userSettings.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefsState.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsersState.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val settings = (settingsState as? UiState.Success)?.data

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateDataPreferences(
            autoDownloadWifi = settings?.autoDownloadWifi ?: true,
            dataSaver = settings?.dataSaver ?: false,
            readReceipts = settings?.readReceipts ?: true,
            notifications = granted
        )
    }

    var showQr by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showTwoStepDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SmileYellow)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 10.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileInk)
                }
                Text(
                    text = strings.settings,
                    style = MaterialTheme.typography.titleLarge,
                    color = SmileInk,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onEditProfile)
                            .padding(16.dp)
                    ) {
                        SmileAvatar(
                            name = profile?.displayName ?: "SMILE",
                            colorHex = profile?.avatarColor ?: "#FFD600",
                            avatarDataBase64 = profile?.avatarData,
                            size = 60.dp,
                            showOnlineDot = true,
                            online = true
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile?.displayName ?: "SMILE User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@${profile?.smileId ?: "smile"}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = profile?.about ?: strings.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showQr = true }) {
                            Icon(Icons.Default.QrCode2, contentDescription = strings.myProfileQr)
                        }
                    }
                }
            }

            item {
                Text("Appearance & Language", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Default.DarkMode,
                    title = strings.darkMode,
                    subtitle = if (localPrefs.darkMode) "Dark theme active" else "Light theme active",
                    checked = localPrefs.darkMode,
                    tag = "settings_dark_mode_switch",
                    onCheckedChange = viewModel::setDarkMode
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.languageLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = localPrefs.languageCode == "EN",
                                onClick = { viewModel.setLanguage("EN") },
                                label = { Text("English") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmileYellow, selectedLabelColor = SmileInk),
                                modifier = Modifier.testTag("lang_en_chip")
                            )
                            FilterChip(
                                selected = localPrefs.languageCode == "HI",
                                onClick = { viewModel.setLanguage("HI") },
                                label = { Text("हिन्दी") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmileYellow, selectedLabelColor = SmileInk),
                                modifier = Modifier.testTag("lang_hi_chip")
                            )
                        }
                    }
                }
            }

            item {
                SettingsClickRow(
                    icon = Icons.Default.Wallpaper,
                    title = strings.chatWallpaper,
                    subtitle = "Current: ${localPrefs.wallpaperId}",
                    onClick = { showWallpaperDialog = true }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(strings.privacySecurity, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                SettingsClickRow(
                    icon = Icons.Default.Security,
                    title = "Visibility & Read Receipts",
                    subtitle = "Photo: ${profile?.photoVisible ?: "EVERYONE"}, Last seen: ${profile?.lastSeenVisible ?: "EVERYONE"}",
                    onClick = { showPrivacyDialog = true }
                )
            }

            item {
                SettingsClickRow(
                    icon = Icons.Default.Lock,
                    title = strings.appLock,
                    subtitle = if (localPrefs.appLockEnabled) "Enabled (PIN protected)" else "Disabled",
                    onClick = { showPinDialog = true }
                )
            }

            item {
                SettingsClickRow(
                    icon = Icons.Default.Security,
                    title = strings.twoStepVerification,
                    subtitle = if (!settings?.twoStepHash.isNullOrBlank()) "PIN configured" else "Not configured",
                    onClick = { showTwoStepDialog = true }
                )
            }

            item {
                SettingsClickRow(
                    icon = Icons.Default.Block,
                    title = strings.blockedUsers,
                    subtitle = "${blockedUsers.size} blocked contact(s)",
                    onClick = { showBlockedDialog = true }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(strings.storageAndData, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Receive alerts for new messages and incoming calls",
                    checked = settings?.notificationsEnabled ?: true,
                    tag = "settings_notifications_switch",
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.updateDataPreferences(
                                autoDownloadWifi = settings?.autoDownloadWifi ?: true,
                                dataSaver = settings?.dataSaver ?: false,
                                readReceipts = settings?.readReceipts ?: true,
                                notifications = enabled
                            )
                        }
                    }
                )
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Default.Storage,
                    title = "Network Data Saver",
                    subtitle = "Compress media and limit auto-downloads on mobile data",
                    checked = settings?.dataSaver ?: false,
                    tag = "settings_data_saver_switch",
                    onCheckedChange = { saver ->
                        viewModel.updateDataPreferences(
                            autoDownloadWifi = settings?.autoDownloadWifi ?: true,
                            dataSaver = saver,
                            readReceipts = settings?.readReceipts ?: true,
                            notifications = settings?.notificationsEnabled ?: true
                        )
                    }
                )
            }

            item {
                val backupText = if (localPrefs.lastBackupEpochMs > 0L) {
                    "Last synced: ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(localPrefs.lastBackupEpochMs))}"
                } else "Real-time Firestore sync active"
                SettingsClickRow(
                    icon = Icons.Default.Backup,
                    title = strings.backupAndExport,
                    subtitle = backupText,
                    onClick = viewModel::recordBackupNow
                )
            }

            item {
                SettingsClickRow(
                    icon = Icons.Default.Help,
                    title = strings.helpAndSupport,
                    subtitle = "Report a technical issue or contact support",
                    onClick = { showSupportDialog = true }
                )
            }

            item {
                SettingsClickRow(
                    icon = Icons.Default.Policy,
                    title = strings.termsAndPrivacy,
                    subtitle = "E2EE commitment, data isolation & Terms of Service",
                    onClick = { showTermsDialog = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                SettingsClickRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = strings.logout,
                    subtitle = "Sign out of Google session on this device",
                    onClick = onSignOut
                )
            }

            item {
                SettingsClickRow(
                    icon = Icons.Default.DeleteForever,
                    title = strings.deleteAccount,
                    subtitle = "Permanently remove profile and Smile ID",
                    tint = SmileErrorRed,
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }

    if (showQr && profile != null) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            text = { QrCodeCard(profile.displayName, profile.smileId, Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, "Add me on SMILE: @${profile.smileId}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Smile Profile"))
                }) { Text("Share") }
            },
            dismissButton = { TextButton(onClick = { showQr = false }) { Text("Close") } }
        )
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text(strings.chatWallpaper) },
            text = {
                Column {
                    listOf(
                        "smile_yellow" to "Smile Warm Cream",
                        "smile_mint" to "Fresh Mint",
                        "smile_sky" to "Sky Blue",
                        "smile_minimal" to "Minimal Paper"
                    ).forEach { (id, label) ->
                        TextButton(
                            onClick = { viewModel.setWallpaper(id); showWallpaperDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.appLock) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set a 4–6 digit PIN (stored only as salted SHA-256 hash).")
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pin = it },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.configureAppLock(true, pin); showPinDialog = false },
                    enabled = pin.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) { Text("Enable Lock") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.configureAppLock(false, null); showPinDialog = false }) {
                    Text("Disable")
                }
            }
        )
    }

    if (showTwoStepDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTwoStepDialog = false },
            title = { Text(strings.twoStepVerification) },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pin = it },
                    label = { Text("6-digit Recovery PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setTwoStepPin(pin); showTwoStepDialog = false },
                    enabled = pin.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) { Text("Save PIN") }
            },
            dismissButton = { TextButton(onClick = { showTwoStepDialog = false }) { Text("Cancel") } }
        )
    }

    if (showPrivacyDialog) {
        var photoVis by remember(profile) { mutableStateOf(profile?.photoVisible ?: "EVERYONE") }
        var aboutVis by remember(profile) { mutableStateOf(profile?.aboutVisible ?: "EVERYONE") }
        var lastSeenVis by remember(profile) { mutableStateOf(profile?.lastSeenVisible ?: "EVERYONE") }
        var discoverable by remember(profile) { mutableStateOf(profile?.discoverable ?: true) }
        var readReceipts by remember(settings) { mutableStateOf(settings?.readReceipts ?: true) }

        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Controls") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Profile Photo Visibility: $photoVis", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EVERYONE", "CONTACTS", "NOBODY").forEach { opt ->
                            FilterChip(selected = photoVis == opt, onClick = { photoVis = opt }, label = { Text(opt.take(5)) })
                        }
                    }
                    Text("Last Seen & Online: $lastSeenVis", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EVERYONE", "CONTACTS", "NOBODY").forEach { opt ->
                            FilterChip(selected = lastSeenVis == opt, onClick = { lastSeenVis = opt }, label = { Text(opt.take(5)) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Discoverable in directory", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = discoverable, onCheckedChange = { discoverable = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Read receipts", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = readReceipts, onCheckedChange = { readReceipts = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePrivacyVisibility(photoVis, aboutVis, lastSeenVis, discoverable)
                        viewModel.updateDataPreferences(
                            autoDownloadWifi = settings?.autoDownloadWifi ?: true,
                            dataSaver = settings?.dataSaver ?: false,
                            readReceipts = readReceipts,
                            notifications = settings?.notificationsEnabled ?: true
                        )
                        showPrivacyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Cancel") } }
        )
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text(strings.blockedUsers) },
            text = {
                if (blockedUsers.isEmpty()) {
                    Text("No blocked users.")
                } else {
                    Column {
                        blockedUsers.forEach { b ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(b.blockedName, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.unblockUser(b.blockedUid) }) { Text("Unblock") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBlockedDialog = false }) { Text("Close") } }
        )
    }

    if (showSupportDialog) {
        var subject by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text(strings.helpAndSupport) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Issue Summary") })
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Describe the issue") }, maxLines = 4)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReport(viewModel.currentUserId, "TECHNICAL", subject, details)
                        showSupportDialog = false
                    },
                    enabled = subject.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileInk)
                ) { Text("Send Report") }
            },
            dismissButton = { TextButton(onClick = { showSupportDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("SMILE Privacy Policy & Terms") },
            text = {
                Text(
                    "1. End-to-End Encryption: 1-on-1 messages use ECDH P-256 + AES-256-GCM. Private keys are wrapped by Android Keystore.\n" +
                        "2. Zero Plaintext Secrets: PINs and phone lookup hashes use salted SHA-256 digests.\n" +
                        "3. Permissions: Contacts, Camera, Microphone, and Location are only requested at runtime when you invoke those features.\n" +
                        "4. Account Control: You can export chats or permanently delete your account anytime."
                )
            },
            confirmButton = { TextButton(onClick = { showTermsDialog = false }) { Text("Accept") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete SMILE Account?") },
            text = { Text("This permanently deletes your SMILE profile, unique Smile ID reservation, and Firebase authentication account.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccount(onSignOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileErrorRed, contentColor = SmileWhite)
                ) { Text("Delete Forever") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(tag))
        }
    }
}

@Composable
private fun SettingsClickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint ?: MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = tint ?: MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
