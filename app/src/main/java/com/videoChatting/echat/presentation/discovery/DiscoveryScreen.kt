package com.videoChatting.echat.presentation.discovery

import android.Manifest
import android.content.pm.PackageManager
import android.view.TextureView
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
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
import io.agora.rtc2.video.BeautyOptions

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
    
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var showGiftBottomSheet by remember { mutableStateOf(false) }
    var showDailyRewardsSheet by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var isFullscreenMode by remember { mutableStateOf(false) }
    var isBeautyOn by remember { mutableStateOf(false) }
    var callSeconds by remember { mutableIntStateOf(0) }

    val activeGiftEvent by viewModel.activeGiftEvent.collectAsState()
    val floatingEmojis by viewModel.floatingEmojis.collectAsState()
    val currentCoins by viewModel.currentCoins.collectAsState()

    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var localTextureView by remember { mutableStateOf<TextureView?>(null) }
    var remoteTextureView by remember { mutableStateOf<TextureView?>(null) }
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
                        remoteTextureView = null
                        viewModel.nextPerson()
                    }
                }
            }
            val engine = RtcEngine.create(config)
            rtcEngine = engine
            engine.enableVideo()

            localTextureView = TextureView(context)
            engine.setupLocalVideo(VideoCanvas(localTextureView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            engine.startPreview()
        } catch (e: Exception) {
            Toast.makeText(context, "Error init Agora", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state) {
        if (state is DiscoveryState.Matched) {
            val match = (state as DiscoveryState.Matched).match
            callSeconds = 0
            showEmojiPicker = false
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
            callSeconds = 0
            showEmojiPicker = false
            // Force reset to guarantee LaunchedEffect triggers on next match
            remoteUid = 0
            remoteTextureView = null
        }
    }

    // Match duration timer
    LaunchedEffect(state) {
        if (state is DiscoveryState.Matched) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                callSeconds++
            }
        }
    }

    LaunchedEffect(remoteUid) {
        if (remoteUid != 0 && rtcEngine != null) {
            remoteTextureView = TextureView(context)
            rtcEngine?.setupRemoteVideo(VideoCanvas(remoteTextureView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
        }
    }

    // Observe Beauty Filter changes
    LaunchedEffect(isBeautyOn) {
        val engine = rtcEngine ?: return@LaunchedEffect
        val options = BeautyOptions().apply {
            lighteningContrastLevel = BeautyOptions.LIGHTENING_CONTRAST_NORMAL
            lighteningLevel = 0.7f
            smoothnessLevel = 0.85f
            rednessLevel = 0.35f
            sharpnessLevel = 0.3f
        }
        engine.setBeautyEffectOptions(isBeautyOn, options)
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
            }
        }
        else if (state is DiscoveryState.Error) {
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
        }
        else {
            // Dynamic Video Layout: Supports both Split Screen and Fullscreen PiP with Double-Tap gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (state is DiscoveryState.Matched) {
                                    val partnerId = (state as DiscoveryState.Matched).match.partner.userId
                                    viewModel.sendReaction(partnerId, "❤️")
                                }
                            }
                        )
                    }
            ) {
                if (!isFullscreenMode) {
                    // MODE 1: CLEAN 50-50 SPLIT SCREEN
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Half: Remote Video (Full rectangular view)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                        ) {
                            if (remoteTextureView != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        FrameLayout(ctx).apply {
                                            layoutParams = android.view.ViewGroup.LayoutParams(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    update = { container ->
                                        val view = remoteTextureView
                                        if (view != null) {
                                            if (view.parent !== container) {
                                                (view.parent as? android.view.ViewGroup)?.removeView(view)
                                                container.removeAllViews()
                                                view.layoutParams = FrameLayout.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                container.addView(view)
                                            }
                                        } else {
                                            container.removeAllViews()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                RemoteConnectingIndicator(agoraStatus)
                            }
                        }

                        HorizontalDivider(color = cardBorder, thickness = 1.dp)

                        // Bottom Half: Local Video (Full rectangular view)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                        ) {
                            if (localTextureView != null && !isVideoMuted) {
                                AndroidView(
                                    factory = { ctx ->
                                        FrameLayout(ctx).apply {
                                            layoutParams = android.view.ViewGroup.LayoutParams(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    update = { container ->
                                        val view = localTextureView
                                        if (view != null) {
                                            if (view.parent !== container) {
                                                (view.parent as? android.view.ViewGroup)?.removeView(view)
                                                container.removeAllViews()
                                                view.layoutParams = FrameLayout.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                container.addView(view)
                                            }
                                        } else {
                                            container.removeAllViews()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(CyberMidnight), contentAlignment = Alignment.Center) {
                                    Text(if (isVideoMuted) "Camera Off" else "Starting Camera...", color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                } else {
                    // MODE 2: FULLSCREEN WITH PIXEL-PERFECT CIRCULAR PiP (WhatsApp / FaceTime style)
                    // Fullscreen Remote Video
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        if (remoteTextureView != null) {
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                update = { container ->
                                    val view = remoteTextureView
                                    if (view != null) {
                                        if (view.parent !== container) {
                                            (view.parent as? android.view.ViewGroup)?.removeView(view)
                                            container.removeAllViews()
                                            view.layoutParams = FrameLayout.LayoutParams(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            container.addView(view)
                                        }
                                    } else {
                                        container.removeAllViews()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            RemoteConnectingIndicator(agoraStatus)
                        }
                    }

                    // Floating Local Video: Pixel-Perfect Circular PiP Bubble (Top-Start)
                    if (localTextureView != null && !isVideoMuted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 16.dp, top = 28.dp)
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(2.dp, Color(0xFF38BDF8), CircleShape)
                                .clickable { isFullscreenMode = false }
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                update = { container ->
                                    val view = localTextureView
                                    if (view != null) {
                                        if (view.parent !== container) {
                                            (view.parent as? android.view.ViewGroup)?.removeView(view)
                                            container.removeAllViews()
                                            view.layoutParams = FrameLayout.LayoutParams(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            container.addView(view)
                                        }
                                    } else {
                                        container.removeAllViews()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // --- COMMON IN-CALL OVERLAYS & CONTROLS ---

                // 1. Top Header Row: [ ⏱️ Timer ] + [ 🎡 Bonus ] + [ 🪙 Coins ]
                val currentCoinsVal by viewModel.currentCoins.collectAsState()
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ⏱️ Live Call Duration Pill (Only during active match)
                    if (state is DiscoveryState.Matched) {
                        val mins = callSeconds / 60
                        val secs = callSeconds % 60
                        val timeStr = String.format("%02d:%02d", mins, secs)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x880F172A))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = timeStr,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 🎡 Daily Rewards / Spin Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF261C4E))
                            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                            .clickable { showDailyRewardsSheet = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎡", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Bonus", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Coins Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBackground)
                            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = CoinGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$currentCoinsVal",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // 2. Right Vertical Action Column (Layout Toggle, Beauty, Camera Flip, Like, Add, Report)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var isLiked by remember { mutableStateOf(false) }
                    var isAdded by remember { mutableStateOf(false) }

                    // Fullscreen / Split Screen Toggle Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isFullscreenMode) Color(0xFF2563EB) else cardBackground, shape = CircleShape)
                            .border(BorderStroke(1.dp, if (isFullscreenMode) Color(0xFF38BDF8) else cardBorder), CircleShape)
                            .clickable {
                                isFullscreenMode = !isFullscreenMode
                                Toast.makeText(context, if (isFullscreenMode) "Fullscreen PiP Mode" else "Split View Mode", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio, 
                            contentDescription = "Toggle Layout", 
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // ✨ Beauty & Glow Filter Toggle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isBeautyOn) Color(0xFF9333EA) else cardBackground, shape = CircleShape)
                            .border(BorderStroke(1.dp, if (isBeautyOn) Color(0xFFFFD700) else cardBorder), CircleShape)
                            .clickable {
                                isBeautyOn = !isBeautyOn
                                Toast.makeText(context, if (isBeautyOn) "Beauty Filter ON ✨" else "Beauty Filter OFF", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome, 
                            contentDescription = "Beauty Filter", 
                            tint = if (isBeautyOn) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // 🔄 Front / Back Camera Flip
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(cardBackground, shape = CircleShape)
                            .border(BorderStroke(1.dp, cardBorder), CircleShape)
                            .clickable { rtcEngine?.switchCamera() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch, 
                            contentDescription = "Flip Camera", 
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    
                    // Like Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(cardBackground, shape = CircleShape)
                            .border(BorderStroke(1.dp, cardBorder), CircleShape)
                            .clickable {
                                isLiked = !isLiked
                                if (state is DiscoveryState.Matched) {
                                    viewModel.toggleLike((state as DiscoveryState.Matched).match.partner.userId, isLiked)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                            contentDescription = "Like", 
                            tint = if (isLiked) NeonRose else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    
                    // Add Friend Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(cardBackground, shape = CircleShape)
                            .border(BorderStroke(1.dp, cardBorder), CircleShape)
                            .clickable {
                                isAdded = !isAdded
                                if (state is DiscoveryState.Matched) {
                                    viewModel.toggleAdd((state as DiscoveryState.Matched).match.partner.userId, isAdded)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isAdded) Icons.Default.PersonRemove else Icons.Default.PersonAdd, 
                            contentDescription = "Add", 
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Report User Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(cardBackground, shape = CircleShape)
                            .border(BorderStroke(1.dp, cardBorder), CircleShape)
                            .clickable {
                                if (state is DiscoveryState.Matched) {
                                    showReportDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag, 
                            contentDescription = "Report User", 
                            tint = Color.Red,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // 4. In-Call Full-Screen Celebration & Floating Emojis Layer
                com.videoChatting.echat.presentation.call.InCallAnimationOverlay(
                    activeGiftEvent = activeGiftEvent,
                    floatingEmojis = floatingEmojis
                )

                // 5. Bottom Unified Capsule & Animated Emoji Popup
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Emoji Popup (Opens on tapping 😃)
                    AnimatedVisibility(
                        visible = showEmojiPicker && state is DiscoveryState.Matched,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xEE0F172A))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.videoChatting.echat.presentation.call.GiftCatalog.QUICK_REACTIONS.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val partnerId = (state as DiscoveryState.Matched).match.partner.userId
                                            viewModel.sendReaction(partnerId, emoji)
                                            showEmojiPicker = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    // Single Clean Master Control Bar with Elevated Center Hero Next Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0x880F172A))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Mic Audio Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) NeonRose.copy(alpha = 0.25f) else Color(0x33FFFFFF), shape = CircleShape)
                                .border(BorderStroke(1.dp, if (isMuted) NeonRose else Color.White.copy(alpha = 0.2f)), CircleShape)
                                .clickable {
                                    isMuted = !isMuted
                                    rtcEngine?.muteLocalAudioStream(isMuted)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, 
                                contentDescription = "Mute", 
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 2. Camera Video Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isVideoMuted) NeonRose.copy(alpha = 0.25f) else Color(0x33FFFFFF), shape = CircleShape)
                                .border(BorderStroke(1.dp, if (isVideoMuted) NeonRose else Color.White.copy(alpha = 0.2f)), CircleShape)
                                .clickable {
                                    isVideoMuted = !isVideoMuted
                                    rtcEngine?.muteLocalVideoStream(isVideoMuted)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam, 
                                contentDescription = "Video", 
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 3. CENTER HERO NEXT BUTTON (Slightly larger, glowing gradient & elevated)
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state is DiscoveryState.Matched)
                                        Brush.linearGradient(listOf(ElectricIndigo, Color(0xFF9333EA)))
                                    else
                                        Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF))),
                                    shape = CircleShape
                                )
                                .border(
                                    BorderStroke(
                                        1.5.dp, 
                                        if (state is DiscoveryState.Matched) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f)
                                    ), 
                                    CircleShape
                                )
                                .clickable(enabled = state is DiscoveryState.Matched) {
                                    viewModel.nextPerson()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Person",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 4. Gift Button (Only during active match)
                        if (state is DiscoveryState.Matched) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF261C4E))
                                    .border(1.2.dp, Color(0xFFFFD700), CircleShape)
                                    .clickable { showGiftBottomSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎁", fontSize = 17.sp)
                            }
                        }

                        // 5. 😃 Emoji Reaction Picker Button (Only during active match)
                        if (state is DiscoveryState.Matched) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (showEmojiPicker) Color(0x669333EA) else Color(0x33FFFFFF), shape = CircleShape)
                                    .border(BorderStroke(1.dp, if (showEmojiPicker) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f)), CircleShape)
                                    .clickable { showEmojiPicker = !showEmojiPicker },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("😃", fontSize = 17.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { 
                showReportDialog = false 
                reportReason = ""
            },
            title = {
                Text(
                    text = "Report User",
                    fontWeight = FontWeight.Bold,
                    color = getThemeTextColor()
                )
            },
            text = {
                Column {
                    Text(
                        text = "Specify a reason to report this user for administrative review:",
                        fontSize = 14.sp,
                        color = getThemeSubTextColor(),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Reason for report") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = getThemeTextColor(),
                            unfocusedTextColor = getThemeTextColor()
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a reason", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (state is DiscoveryState.Matched) {
                            val partnerId = (state as DiscoveryState.Matched).match.partner.userId
                            viewModel.reportUser(
                                targetUserId = partnerId,
                                reason = reportReason,
                                onSuccess = {
                                    Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
                                    showReportDialog = false
                                    reportReason = ""
                                    viewModel.nextPerson()
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Failed to submit report: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Submit Report", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showReportDialog = false 
                        reportReason = ""
                    }
                ) {
                    Text("Cancel", color = getThemeTextColor())
                }
            },
            containerColor = getThemeGlassBackground(),
            modifier = Modifier.border(1.dp, getThemeGlassBorder(), RoundedCornerShape(28.dp))
        )
    }

    // Virtual Gift Selection Modal Bottom Sheet
    if (showGiftBottomSheet && state is DiscoveryState.Matched) {
        val partnerId = (state as DiscoveryState.Matched).match.partner.userId
        com.videoChatting.echat.presentation.call.GiftBottomSheet(
            currentCoins = currentCoins,
            onDismiss = { showGiftBottomSheet = false },
            onSendGift = { gift ->
                viewModel.sendGift(partnerId, gift)
                Toast.makeText(context, "Sent ${gift.name}! ${gift.icon}", Toast.LENGTH_SHORT).show()
            },
            onRechargeClicked = {
                showGiftBottomSheet = false
                Toast.makeText(context, "Redirecting to Coin Wallet...", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Daily Rewards (Streak & Lucky Spin Wheel) Bottom Sheet
    if (showDailyRewardsSheet) {
        com.videoChatting.echat.presentation.rewards.DailyRewardsBottomSheet(
            onDismiss = { showDailyRewardsSheet = false }
        )
    }
}

@Composable
fun RemoteConnectingIndicator(agoraStatus: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Connecting to Stranger...",
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        CircularProgressIndicator(color = ElectricIndigo)
        if (agoraStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                agoraStatus,
                color = CyberCyan,
                fontSize = 12.sp
            )
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
