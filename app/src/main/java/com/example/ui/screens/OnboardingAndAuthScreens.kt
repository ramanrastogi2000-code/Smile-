package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.remote.SmileAuthManager
import com.example.ui.components.E2eePill
import com.example.ui.components.SmileAvatar
import com.example.ui.components.SmileLogo
import com.example.ui.i18n.LocalSmileStrings
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.utils.MediaAndDeviceHelpers
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(950)
        onFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SmileYellow, Color(0xFFFFE54C), SmileWhite)
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 10.dp,
                color = SmileYellow
            ) {
                SmileLogo(size = 112.dp, showRoundedSquareBg = true)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "SMILE",
                style = MaterialTheme.typography.displayLarge,
                color = SmileInk,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "बात करें, मुस्कुराएं!",
                style = MaterialTheme.typography.titleMedium,
                color = SmileInk.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalSmileStrings.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        SmileAuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_smile_banner),
                        contentDescription = "SMILE brand banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SmileInk.copy(alpha = 0.10f))
                    ) {
                        SmileLogo(size = 78.dp)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "SMILE",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                E2eePill(strings.e2eeBadge)

                Spacer(Modifier.height(16.dp))
                Text(
                    text = strings.authSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SmileInk)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "ECDH P-256 + AES-256-GCM encrypted 1-on-1 chats",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SmileInk)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Private +91 phone discovery via SHA-256 hashing",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        SmileAuthManager.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmileYellow,
                        contentColor = SmileInk
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = SmileInk,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    initialName: String,
    initialSmileId: String,
    initialAbout: String,
    initialPhone: String,
    initialAvatarColor: String,
    initialAvatarData: String?,
    actionMessage: String?,
    onClearMessage: () -> Unit,
    onSave: (
        displayName: String,
        smileId: String,
        about: String,
        phone: String,
        countryCode: String,
        avatarColor: String,
        avatarData: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    var name by remember(initialName) { mutableStateOf(initialName) }
    var smileId by remember(initialSmileId) { mutableStateOf(initialSmileId) }
    var about by remember(initialAbout) { mutableStateOf(initialAbout.ifBlank { "बात करें, मुस्कुराएं!" }) }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var avatarColor by remember(initialAvatarColor) { mutableStateOf(initialAvatarColor) }
    var avatarData by remember(initialAvatarData) { mutableStateOf(initialAvatarData) }
    var saving by remember { mutableStateOf(false) }

    val colorChoices = listOf("#FFD600", "#FFE57F", "#A7F3D0", "#BAE6FD", "#FDE68A", "#FBCFE8")
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarData = MediaAndDeviceHelpers.compressImageUriToBase64(context, uri, 256, 70)
        }
    }

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            saving = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Surface(
                    color = SmileYellow,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        SmileLogo(size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.setupProfileTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = SmileInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = SmileInk.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.clickable {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    SmileAvatar(
                        name = name.ifBlank { "S" },
                        colorHex = avatarColor,
                        avatarDataBase64 = avatarData,
                        size = 96.dp
                    )
                    Surface(
                        color = SmileYellow,
                        shape = CircleShape,
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Pick profile photo",
                                tint = SmileInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorChoices.forEach { hex ->
                        val parsed = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (avatarColor == hex) 2.5.dp else 1.dp,
                                    color = if (avatarColor == hex) SmileInk else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { avatarColor = hex }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; onClearMessage() },
                    label = { Text(strings.displayNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = smileId,
                    onValueChange = {
                        smileId = it.lowercase().replace(Regex("[^a-z0-9_]"), "")
                        onClearMessage()
                    },
                    label = { Text(strings.smileIdLabel) },
                    prefix = { Text("@") },
                    supportingText = { Text("3–32 lowercase letters, digits, or _ (unique ID)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_smile_id_input")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; onClearMessage() },
                    label = { Text(strings.phoneNumberLabel) },
                    prefix = { Text("+91 ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_input")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = about,
                    onValueChange = { about = it },
                    label = { Text(strings.aboutLabel) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_about_input")
                )

                if (!actionMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = actionMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = {
                        saving = true
                        onSave(name, smileId, about, phone, "+91", avatarColor, avatarData)
                    },
                    enabled = !saving && name.isNotBlank() && smileId.length >= 3,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmileYellow,
                        contentColor = SmileInk
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_profile_button")
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SmileInk)
                    } else {
                        Text(
                            text = strings.continueAction,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    onVerifyPin: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(SmileYellowSoft)
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SmileWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                SmileLogo(size = 64.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "SMILE App Lock",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SmileInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Enter your PIN to unlock messages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                            pin = it
                            error = false
                        }
                    },
                    label = { Text("4–6 Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_lock_pin_input")
                )
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Incorrect PIN. Try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (!onVerifyPin(pin)) error = true
                    },
                    enabled = pin.length >= 4,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmileYellow,
                        contentColor = SmileInk
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("unlock_app_button")
                ) {
                    Text("Unlock SMILE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
