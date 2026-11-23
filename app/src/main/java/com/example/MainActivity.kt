package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.CallRecord
import com.example.data.prefs.SmileLocalPreferences
import com.example.data.remote.SmileAuthManager
import com.example.data.remote.SmileRepository
import com.example.ui.i18n.LocalSmileStrings
import com.example.ui.i18n.stringsForLanguage
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CallScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.SmileTheme
import com.example.ui.theme.SmileYellow
import com.example.ui.viewmodel.ChatDetailViewModel
import com.example.ui.viewmodel.SmileViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmileRootApp()
        }
    }
}

@Composable
private fun SmileRootApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember { SmileRepository(context) }
    val localPrefs = remember { SmileLocalPreferences(context) }
    val localPrefsState by localPrefs.state.collectAsStateWithLifecycle(
        initialValue = com.example.data.prefs.LocalPrefsState()
    )

    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    SmileTheme(darkTheme = localPrefsState.darkMode) {
        CompositionLocalProvider(
            LocalSmileStrings provides stringsForLanguage(localPrefsState.languageCode)
        ) {
            if (showSplash) {
                SplashScreen(onFinished = { showSplash = false })
            } else {
                val user = currentUser
                if (user == null) {
                    AuthScreen(
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                        }
                    )
                } else {
                    key(user.uid) {
                        AuthenticatedSmileSession(
                            user = user,
                            repository = repository,
                            localPrefs = localPrefs
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedSmileSession(
    user: FirebaseUser,
    repository: SmileRepository,
    localPrefs: SmileLocalPreferences
) {
    val activityContext = LocalContext.current
    val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: SmileViewModel = viewModel(
        key = "smile_vm_${user.uid}",
        factory = remember(user.uid) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SmileViewModel(repository, localPrefs, user.uid) as T
                }
            }
        }
    )

    DisposableEffect(lifecycleOwner, user.uid) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.updatePresence(true)
                Lifecycle.Event.ON_STOP -> viewModel.updatePresence(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.updatePresence(false)
        }
    }

    val localPrefsState by viewModel.localPrefsState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val settingsState by viewModel.userSettings.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    if (localPrefsState.appLockEnabled && localPrefsState.appLockPinHash.isNotBlank() && !isUnlocked) {
        AppLockScreen(onVerifyPin = viewModel::unlockWithPin)
        return
    }

    when (val profState = profileState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SmileYellow)
            }
        }
        is UiState.Error -> {
            ProfileSetupScreen(
                initialName = user.displayName.orEmpty(),
                initialSmileId = user.email?.substringBefore("@")?.lowercase()?.replace(Regex("[^a-z0-9_]"), "") ?: "",
                initialAbout = "बात करें, मुस्कुराएं!",
                initialPhone = "",
                initialAvatarColor = "#FFD600",
                initialAvatarData = null,
                actionMessage = actionMessage ?: profState.message,
                onClearMessage = viewModel::clearActionMessage,
                onSave = { name, smileId, about, phone, code, color, avatarData ->
                    viewModel.saveProfile(name, smileId, about, phone, code, color, avatarData)
                }
            )
        }
        is UiState.Success -> {
            val profile = profState.data
            if (profile == null) {
                val suggestedId = (user.email?.substringBefore("@") ?: user.displayName ?: "smile")
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .take(16)
                    .ifBlank { "smile_${user.uid.take(6).lowercase()}" }
                ProfileSetupScreen(
                    initialName = user.displayName.orEmpty(),
                    initialSmileId = suggestedId,
                    initialAbout = "बात करें, मुस्कुराएं!",
                    initialPhone = "",
                    initialAvatarColor = "#FFD600",
                    initialAvatarData = null,
                    actionMessage = actionMessage,
                    onClearMessage = viewModel::clearActionMessage,
                    onSave = { name, smileId, about, phone, code, color, avatarData ->
                        viewModel.saveProfile(name, smileId, about, phone, code, color, avatarData)
                    }
                )
            } else {
                val navController = rememberNavController()
                val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
                val callsState by viewModel.callsState.collectAsStateWithLifecycle()
                val contacts by viewModel.contactsState.collectAsStateWithLifecycle()
                val discoverable by viewModel.discoverableProfiles.collectAsStateWithLifecycle()
                val settings = (settingsState as? UiState.Success)?.data
                val allChats = (chatsState as? UiState.Success)?.data ?: emptyList()
                val allCalls = (callsState as? UiState.Success)?.data ?: emptyList()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onOpenChat = { chatId ->
                                navController.navigate("chat/${Uri.encode(chatId)}")
                            },
                            onOpenSettings = {
                                navController.navigate("settings")
                            },
                            onOpenCall = { call ->
                                navController.navigate("call/${Uri.encode(call.id)}")
                            }
                        )
                    }

                    composable(
                        route = "chat/{chatId}",
                        arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedId = backStackEntry.arguments?.getString("chatId").orEmpty()
                        val chatId = Uri.decode(encodedId)
                        val chatVm: ChatDetailViewModel = viewModel(
                            key = "chat_${user.uid}_$chatId",
                            factory = remember(user.uid, chatId) {
                                object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return ChatDetailViewModel(repository, localPrefs, chatId, user.uid) as T
                                    }
                                }
                            }
                        )
                        ChatScreen(
                            viewModel = chatVm,
                            myDisplayName = profile.displayName,
                            wallpaperId = localPrefsState.wallpaperId,
                            darkMode = localPrefsState.darkMode,
                            readReceiptsEnabled = settings?.readReceipts ?: true,
                            allChats = allChats,
                            contacts = contacts,
                            discoverable = discoverable,
                            onStartCall = { title, peers, type, isGroup ->
                                viewModel.startCall(title, peers, type, isGroup) { callId ->
                                    navController.navigate("call/${Uri.encode(callId)}")
                                }
                            },
                            onBlockUser = viewModel::blockUser,
                            onReportUser = viewModel::submitReport,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onEditProfile = { navController.navigate("profile_edit") },
                            onSignOut = {
                                viewModel.updatePresence(false)
                                SmileAuthManager.signOut(credentialManager = credentialManager, scope = scope)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("profile_edit") {
                        BackHandler { navController.popBackStack() }
                        ProfileSetupScreen(
                            initialName = profile.displayName,
                            initialSmileId = profile.smileId,
                            initialAbout = profile.about,
                            initialPhone = settings?.phoneNumber?.removePrefix("+91").orEmpty(),
                            initialAvatarColor = profile.avatarColor,
                            initialAvatarData = profile.avatarData,
                            actionMessage = actionMessage,
                            onClearMessage = viewModel::clearActionMessage,
                            onSave = { name, smileId, about, phone, code, color, avatarData ->
                                viewModel.saveProfile(
                                    displayName = name,
                                    smileId = smileId,
                                    about = about,
                                    phone = phone,
                                    countryCode = code,
                                    avatarColor = color,
                                    avatarData = avatarData,
                                    onSuccess = { navController.popBackStack() }
                                )
                            }
                        )
                    }

                    composable(
                        route = "call/{callId}",
                        arguments = listOf(navArgument("callId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedId = backStackEntry.arguments?.getString("callId").orEmpty()
                        val callId = Uri.decode(encodedId)
                        val activeCall = allCalls.firstOrNull { it.id == callId }
                            ?: CallRecord(id = callId, callerId = user.uid, title = "SMILE Call")
                        CallScreen(
                            call = activeCall,
                            currentUserId = user.uid,
                            onAccept = { viewModel.answerCall(callId) },
                            onEnd = { status, duration -> viewModel.endCall(callId, status, duration) },
                            onClose = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
