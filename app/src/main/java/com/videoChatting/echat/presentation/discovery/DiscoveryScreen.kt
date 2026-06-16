package com.videoChatting.echat.presentation.discovery

import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import io.agora.media.RtcTokenBuilder2
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

const val AGORA_APP_ID = "021fba375e4b4a73828c10923e12627c"
const val AGORA_APP_CERTIFICATE = "e9b8eba92d474ff886c7cb235c181b99"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(viewModel: DiscoveryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var isMuted by remember { mutableStateOf(false) }
    var isVideoMuted by remember { mutableStateOf(false) }
    var chatText by remember { mutableStateOf("") }
    var agoraStatus by remember { mutableStateOf("") }

    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteUid by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            viewModel.startDiscovery()
        } else {
            Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(key1 = true) {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        val hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermissions) {
            viewModel.startDiscovery()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val config = RtcEngineConfig()
            config.mContext = context.applicationContext
            config.mAppId = AGORA_APP_ID
            config.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    agoraStatus = "Joined channel!"
                }
                override fun onError(err: Int) {
                    agoraStatus = "Agora Error: $err"
                }
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    agoraStatus = "User joined!"
                    remoteUid = uid
                }
                override fun onUserOffline(uid: Int, reason: Int) {
                    if (remoteUid == uid) {
                        remoteUid = 0
                        remoteSurfaceView = null
                        viewModel.nextPerson()
                    }
                }
            }
            val engine = RtcEngine.create(config)
            rtcEngine = engine
            engine.enableVideo()

            localSurfaceView = SurfaceView(context).apply { setZOrderMediaOverlay(true) }
            engine.setupLocalVideo(VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            engine.startPreview()
        } catch (e: Exception) {
            Toast.makeText(context, "Error init Agora", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state) {
        if (state is DiscoveryState.Matched) {
            val match = (state as DiscoveryState.Matched).match
            try {
                val engine = rtcEngine ?: return@LaunchedEffect
                
                val options = ChannelMediaOptions()
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

                val tokenBuilder = RtcTokenBuilder2()
                val token = tokenBuilder.buildTokenWithUid(
                    AGORA_APP_ID, 
                    AGORA_APP_CERTIFICATE, 
                    match.channelName, 
                    0, 
                    RtcTokenBuilder2.Role.ROLE_PUBLISHER, 
                    86400, // Token validity
                    86400 // Privilege validity
                )

                engine.joinChannel(token, match.channelName, 0, options)

            } catch (e: Exception) {
                Toast.makeText(context, "Error joining channel", Toast.LENGTH_SHORT).show()
            }
        } else if (state is DiscoveryState.Idle || state is DiscoveryState.Searching) {
            // Leave channel if we were in one
            rtcEngine?.leaveChannel()
            agoraStatus = ""
            // Force reset to guarantee LaunchedEffect triggers on next match
            remoteUid = 0
            remoteSurfaceView = null
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Half: Remote Video
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.DarkGray)
        ) {
            // Branding
            Text(
                "eChat",
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
            )

            if (state is DiscoveryState.Searching) {
                Text(
                    "Searching for someone...",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp), color = MaterialTheme.colorScheme.primary)
            } else if (remoteSurfaceView != null) {
                AndroidView(
                    factory = { 
                        FrameLayout(context).apply { 
                            val view = remoteSurfaceView
                            if (view != null) {
                                (view.parent as? android.view.ViewGroup)?.removeView(view)
                                addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) 
                            }
                        } 
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text("Stranger", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                // Shorts-style vertical action buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 32.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var isLiked by remember { mutableStateOf(false) }
                    var isAdded by remember { mutableStateOf(false) }
                    
                    IconButton(
                        onClick = { 
                            isLiked = !isLiked
                            if (state is DiscoveryState.Matched) {
                                viewModel.toggleLike((state as DiscoveryState.Matched).match.matchedUserId, isLiked)
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(50))
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                            contentDescription = "Like", 
                            tint = if (isLiked) Color.Red else Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            isAdded = !isAdded
                            if (state is DiscoveryState.Matched) {
                                viewModel.toggleAdd((state as DiscoveryState.Matched).match.matchedUserId, isAdded)
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(50))
                    ) {
                        Icon(
                            if (isAdded) Icons.Default.PersonRemove else Icons.Default.PersonAdd, 
                            contentDescription = "Add", 
                            tint = Color.White
                        )
                    }
                }
            } else if (state is DiscoveryState.Matched) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        "Connecting to Stranger...",
                        color = Color.White
                    )
                    if (agoraStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            agoraStatus,
                            color = Color.Yellow,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color.Gray, thickness = 1.dp)

        // Bottom Half: Local Video + Chat
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            if (localSurfaceView != null && !isVideoMuted) {
                AndroidView(
                    factory = { 
                        FrameLayout(context).apply { 
                            val view = localSurfaceView
                            if (view != null) {
                                (view.parent as? android.view.ViewGroup)?.removeView(view)
                                addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) 
                            }
                        } 
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Text(if (isVideoMuted) "Camera Off" else "Starting Camera...", color = Color.White)
                }
            }

            // Controls Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text("You", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Bottom Center Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        isMuted = !isMuted
                        rtcEngine?.muteLocalAudioStream(isMuted)
                    }, modifier = Modifier.background(Color.Black.copy(alpha=0.5f), shape = RoundedCornerShape(50))) {
                        Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { 
                        isVideoMuted = !isVideoMuted
                        rtcEngine?.muteLocalVideoStream(isVideoMuted)
                    }, modifier = Modifier.background(Color.Black.copy(alpha=0.5f), shape = RoundedCornerShape(50))) {
                        Icon(if (isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam, contentDescription = "Video", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { viewModel.nextPerson() },
                        enabled = state is DiscoveryState.Matched,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (state is DiscoveryState.Matched) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next")
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (state is DiscoveryState.Matched) "Next" else "Searching...", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
