package com.videoChatting.echat.presentation.call

import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

const val AGORA_APP_ID = "021fba375e4b4a73828c10923e12627c"

@Composable
fun CallScreen(channelName: String, onCallEnded: () -> Unit) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var isVideoMuted by remember { mutableStateOf(false) }

    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteUid by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            // Initialize Agora
            try {
                val config = RtcEngineConfig()
                config.mContext = context.applicationContext
                config.mAppId = AGORA_APP_ID
                config.mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        super.onJoinChannelSuccess(channel, uid, elapsed)
                    }

                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        super.onUserJoined(uid, elapsed)
                        remoteUid = uid
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        super.onUserOffline(uid, reason)
                        if (remoteUid == uid) {
                            remoteUid = 0
                            remoteSurfaceView = null
                        }
                    }
                }
                val engine = RtcEngine.create(config)
                rtcEngine = engine
                
                engine.enableVideo()
                
                // Create local surface view
                localSurfaceView = SurfaceView(context).apply { setZOrderMediaOverlay(true) }
                engine.setupLocalVideo(VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                engine.startPreview()

                val options = ChannelMediaOptions()
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

                engine.joinChannel(null, channelName, 0, options)

            } catch (e: Exception) {
                Toast.makeText(context, "Error init Agora: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
            onCallEnded()
        }
    }

    LaunchedEffect(key1 = true) {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        val hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermissions) {
            permissionLauncher.launch(permissions) // Trigger init anyway for simplicity in this demo
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(remoteUid) {
        if (remoteUid != 0 && rtcEngine != null) {
            remoteSurfaceView = SurfaceView(context)
            rtcEngine?.setupRemoteVideo(VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
        }
    }

    DisposableEffect(key1 = true) {
        onDispose {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Remote Video (Full Screen)
        if (remoteSurfaceView != null) {
            AndroidView(
                factory = {
                    FrameLayout(context).apply {
                        addView(remoteSurfaceView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("Waiting for user to join...", color = Color.White)
            }
        }

        // Local Video (PiP)
        if (localSurfaceView != null && !isVideoMuted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(100.dp, 150.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.DarkGray)
            ) {
                AndroidView(
                    factory = {
                        FrameLayout(context).apply {
                            addView(localSurfaceView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Mute Audio
            IconButton(
                onClick = {
                    isMuted = !isMuted
                    rtcEngine?.muteLocalAudioStream(isMuted)
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isMuted) Color.White else Color.DarkGray.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = if (isMuted) Color.Black else Color.White
                )
            }

            // End Call
            IconButton(
                onClick = { onCallEnded() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Red)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Mute Video
            IconButton(
                onClick = {
                    isVideoMuted = !isVideoMuted
                    rtcEngine?.muteLocalVideoStream(isVideoMuted)
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isVideoMuted) Color.White else Color.DarkGray.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    contentDescription = "Video",
                    tint = if (isVideoMuted) Color.Black else Color.White
                )
            }
        }
    }
}
