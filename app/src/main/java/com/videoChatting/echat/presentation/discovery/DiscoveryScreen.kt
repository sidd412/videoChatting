package com.videoChatting.echat.presentation.discovery

import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.videoChatting.echat.presentation.theme.*
import com.videoChatting.echat.utils.Constants
import io.agora.media.RtcTokenBuilder2
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

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
            config.mAppId = Constants.AGORA_APP_ID
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
                options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
                options.channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION

                // Use token pre-generated securely by the custom backend server
                engine.joinChannel(match.token, match.channelName, 0, options)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeBackgroundGradient())
    ) {
        val textColor = getThemeTextColor()
        val subTextColor = getThemeSubTextColor()
        val cardBackground = getThemeGlassBackground()
        val cardBorder = getThemeGlassBorder()

        if (state is DiscoveryState.Searching) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Finding Your Match",
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Searching the global queue for the best candidate...",
                    color = subTextColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                RadarScanner()
                
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(color = CyberCyan, strokeWidth = 3.dp)
            }
        } else if (state is DiscoveryState.Error) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, cardBorder),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NeonRose.copy(alpha = 0.2f))
                                .border(BorderStroke(1.5.dp, NeonRose), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Error Icon", tint = CoinGold, modifier = Modifier.size(36.dp))
                        }
                        
                        Text(
                            text = (state as DiscoveryState.Error).message,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Please recharge your coins in the wallet section to continue matching.",
                            color = subTextColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Button(
                            onClick = { viewModel.startDiscovery() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry Matchmaking", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            // Active split screen for video chat
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Half: Remote Video
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                ) {
                    if (remoteSurfaceView != null) {
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
                        
                        // Glassmorphic name label
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBackground)
                                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .align(Alignment.BottomStart)
                        ) {
                            Text(
                                text = if (state is DiscoveryState.Matched) (state as DiscoveryState.Matched).match.partner.name else "Stranger",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Show Coin Balance
                        val currentCoins by viewModel.currentCoins.collectAsState()
                        Box(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBackground)
                                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Coins",
                                    tint = CoinGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$currentCoins",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
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
                                        viewModel.toggleLike((state as DiscoveryState.Matched).match.partner.userId, isLiked)
                                    }
                                },
                                modifier = Modifier
                                    .background(cardBackground, shape = CircleShape)
                                    .border(BorderStroke(1.dp, cardBorder), CircleShape)
                            ) {
                                Icon(
                                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                                    contentDescription = "Like", 
                                    tint = if (isLiked) NeonRose else Color.White
                                )
                            }
                            
                            IconButton(
                                onClick = { 
                                    isAdded = !isAdded
                                    if (state is DiscoveryState.Matched) {
                                        viewModel.toggleAdd((state as DiscoveryState.Matched).match.partner.userId, isAdded)
                                    }
                                },
                                modifier = Modifier
                                    .background(cardBackground, shape = CircleShape)
                                    .border(BorderStroke(1.dp, cardBorder), CircleShape)
                            ) {
                                Icon(
                                    if (isAdded) Icons.Default.PersonRemove else Icons.Default.PersonAdd, 
                                    contentDescription = "Add", 
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Connecting to Stranger...",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            CircularProgressIndicator(color = ElectricIndigo)
                            if (agoraStatus.isNotEmpty()) {
                                Text(
                                    agoraStatus,
                                    color = CyberCyan,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = cardBorder, thickness = 1.dp)

                // Bottom Half: Local Video
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
                        Box(modifier = Modifier.fillMaxSize().background(CyberMidnight), contentAlignment = Alignment.Center) {
                            Text(if (isVideoMuted) "Camera Off" else "Starting Camera...", color = Color.White.copy(alpha = 0.6f))
                        }
                    }

                    // Local Video Overlay Name Tag
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBackground)
                            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text("You", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Bottom Floating Glass Controls Capsule
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .background(cardBackground, shape = RoundedCornerShape(32.dp))
                            .border(BorderStroke(1.dp, cardBorder), shape = RoundedCornerShape(32.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                isMuted = !isMuted
                                rtcEngine?.muteLocalAudioStream(isMuted)
                            },
                            modifier = Modifier
                                .background(if (isMuted) NeonRose.copy(alpha = 0.2f) else Color.Transparent, shape = CircleShape)
                                .border(BorderStroke(1.dp, if (isMuted) NeonRose else cardBorder), CircleShape)
                        ) {
                            Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        IconButton(
                            onClick = { 
                                isVideoMuted = !isVideoMuted
                                rtcEngine?.muteLocalVideoStream(isVideoMuted)
                            },
                            modifier = Modifier
                                .background(if (isVideoMuted) NeonRose.copy(alpha = 0.2f) else Color.Transparent, shape = CircleShape)
                                .border(BorderStroke(1.dp, if (isVideoMuted) NeonRose else cardBorder), CircleShape)
                        ) {
                            Icon(if (isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam, contentDescription = "Video", tint = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        Button(
                            onClick = { viewModel.nextPerson() },
                            enabled = state is DiscoveryState.Matched,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricIndigo,
                                disabledContainerColor = cardBackground
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (state is DiscoveryState.Matched) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (state is DiscoveryState.Matched) "Next" else "Searching...", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Spectacular glowing neon Radar Scanner composable
@Composable
fun RadarScanner(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    
    // Rotating sweep hand
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Expanding rings
    val ring1Scale by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring1Alpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    val ring2Scale by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring2Alpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Alpha"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Background Aura
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        // Ring 1
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(ring1Scale)
                .border(BorderStroke(1.5.dp, CyberCyan.copy(alpha = ring1Alpha)), CircleShape)
        )

        // Ring 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(ring2Scale)
                .border(BorderStroke(1.5.dp, NeonRose.copy(alpha = ring2Alpha)), CircleShape)
        )

        // Sweep rotation overlay
        androidx.compose.foundation.Canvas(modifier = Modifier.size(220.dp)) {
            val radius = size.minDimension / 2
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            
            // Draw outer radar circles
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = radius * 0.65f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.02f),
                radius = radius * 0.35f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Sweep Line
            rotate(rotation, pivot = center) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, CyberCyan.copy(alpha = 0.6f))
                    ),
                    start = center,
                    end = androidx.compose.ui.geometry.Offset(center.x + radius, center.y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Center Pulsing Core
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(ElectricIndigo, NeonRose)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
