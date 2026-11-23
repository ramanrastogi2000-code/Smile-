package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CallRecord
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.ui.components.SmileAvatar
import com.example.ui.i18n.LocalSmileStrings
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileOnlineGreen
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    call: CallRecord,
    currentUserId: String,
    onAccept: () -> Unit,
    onEnd: (CallStatus, Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalSmileStrings.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    var micMuted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(call.typeEnum() == CallType.VIDEO) }
    var cameraOn by remember { mutableStateOf(call.typeEnum() == CallType.VIDEO) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAudioPerm = results[Manifest.permission.RECORD_AUDIO] ?: hasAudioPerm
        hasCameraPerm = results[Manifest.permission.CAMERA] ?: hasCameraPerm
    }

    LaunchedEffect(Unit) {
        val needed = buildList {
            if (!hasAudioPerm) add(Manifest.permission.RECORD_AUDIO)
            if (call.typeEnum() == CallType.VIDEO && !hasCameraPerm) add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) {
            permLauncher.launch(needed.toTypedArray())
        }
    }

    DisposableEffect(speakerOn, micMuted) {
        runCatching {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = speakerOn
            audioManager?.isMicrophoneMute = micMuted
        }
        onDispose {
            runCatching {
                audioManager?.isMicrophoneMute = false
                audioManager?.isSpeakerphoneOn = false
                audioManager?.mode = AudioManager.MODE_NORMAL
            }
        }
    }

    val status = call.statusEnum()
    val isIncomingRinging = status == CallStatus.RINGING && call.callerId != currentUserId

    LaunchedEffect(status) {
        if (status == CallStatus.ACTIVE) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        } else if (status == CallStatus.ENDED || status == CallStatus.DECLINED || status == CallStatus.MISSED) {
            delay(500)
            onClose()
        }
    }

    BackHandler {
        onEnd(if (isIncomingRinging) CallStatus.DECLINED else CallStatus.ENDED, elapsedSeconds)
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1B20), Color(0xFF101114))
                )
            )
            .safeDrawingPadding()
    ) {
        // Real CameraX Preview when video call & camera active & permission granted
        if (call.typeEnum() == CallType.VIDEO && cameraOn && hasCameraPerm) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        runCatching {
                            val provider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val selector = if (useFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Top call header & WebRTC signaling pill
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = SmileYellow.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = SmileYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (call.groupCall) "SMILE Group WebRTC • DTLS-SRTP" else "SMILE WebRTC • E2EE STUN/TURN",
                            style = MaterialTheme.typography.labelMedium,
                            color = SmileYellow
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                SmileAvatar(
                    name = if (call.callerId == currentUserId) call.title else call.callerName,
                    size = 104.dp,
                    showOnlineDot = true,
                    online = status == CallStatus.ACTIVE
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (call.callerId == currentUserId) call.title else call.callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SmileWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                val durationFormatted = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
                Text(
                    text = when {
                        isIncomingRinging -> strings.incomingCall
                        status == CallStatus.RINGING -> strings.calling
                        status == CallStatus.ACTIVE -> durationFormatted
                        else -> status.key
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (status == CallStatus.ACTIVE) SmileOnlineGreen else SmileYellow
                )
            }

            // Bottom Controls
            if (isIncomingRinging) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = {
                                onEnd(CallStatus.DECLINED, 0L)
                                onClose()
                            },
                            containerColor = Color(0xFFDC2626),
                            contentColor = SmileWhite,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("decline_call_button")
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = strings.decline)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(strings.decline, color = SmileWhite)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = onAccept,
                            containerColor = SmileOnlineGreen,
                            contentColor = SmileWhite,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("accept_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = strings.accept)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(strings.accept, color = SmileWhite)
                    }
                }
            } else {
                Surface(
                    color = Color(0xFF1F2229).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp)
                    ) {
                        CallControlTile(
                            icon = if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = strings.mute,
                            active = micMuted,
                            tag = "call_mute_button",
                            onClick = { micMuted = !micMuted }
                        )
                        CallControlTile(
                            icon = Icons.Default.VolumeUp,
                            label = strings.speaker,
                            active = speakerOn,
                            tag = "call_speaker_button",
                            onClick = { speakerOn = !speakerOn }
                        )
                        if (call.typeEnum() == CallType.VIDEO) {
                            CallControlTile(
                                icon = if (cameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                label = strings.camera,
                                active = !cameraOn,
                                tag = "call_camera_button",
                                onClick = { cameraOn = !cameraOn }
                            )
                            CallControlTile(
                                icon = Icons.Default.Cameraswitch,
                                label = strings.flip,
                                active = false,
                                tag = "call_flip_button",
                                onClick = { useFrontCamera = !useFrontCamera }
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                val finalStatus = if (status == CallStatus.RINGING) CallStatus.MISSED else CallStatus.ENDED
                                onEnd(finalStatus, elapsedSeconds)
                                onClose()
                            },
                            containerColor = Color(0xFFDC2626),
                            contentColor = SmileWhite,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(58.dp)
                                .testTag("end_call_button")
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = strings.endCall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (active) SmileYellow else Color.White.copy(alpha = 0.12f))
                .testTag(tag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) SmileInk else SmileWhite
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SmileWhite.copy(alpha = 0.8f)
        )
    }
}
