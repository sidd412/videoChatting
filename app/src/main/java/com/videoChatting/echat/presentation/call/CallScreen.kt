package com.videoChatting.echat.presentation.call

import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.videoChatting.echat.utils.Constants
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.RaiseRequestDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.launch

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.videoChatting.echat.presentation.theme.ElectricIndigo

@Composable
fun CallScreen(
    channelName: String, 
    onCallEnded: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var isVideoMuted by remember { mutableStateOf(false) }
    var showReportDialog1 by remember { mutableStateOf(false) }
    var showReportDialog2 by remember { mutableStateOf(false) }
    var showGiftBottomSheet by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val coinsBalance by viewModel.coinsBalance.collectAsState()
    val activeGiftEvent by viewModel.activeGiftEvent.collectAsState()
    val floatingEmojis by viewModel.floatingEmojis.collectAsState()

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
                config.mAppId = Constants.AGORA_APP_ID
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
                options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
                options.channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION

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

        // Add Profile Button (Top Left)
        if (remoteUid != 0) {
            IconButton(
                onClick = {
                    Toast.makeText(context, "Profile Added to Chats!", Toast.LENGTH_SHORT).show()
                    // TODO: Trigger backend API to save this match to Chats
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Profile",
                    tint = Color.White
                )
            }

            // Report User Button (Top Left - Shifted)
            IconButton(
                onClick = { showReportDialog1 = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 80.dp, top = 16.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Report User",
                    tint = Color.White
                )
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

        // 3. Real-time Full-screen Animation Overlay (Gifts & Floating Emojis)
        InCallAnimationOverlay(
            activeGiftEvent = activeGiftEvent,
            floatingEmojis = floatingEmojis
        )

        // 4. Floating Quick Reactions Bar + In-Call Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating Quick Emojis Bar
            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x880F172A))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GiftCatalog.QUICK_REACTIONS.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                viewModel.sendReaction(channelName, emoji)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            // Main Call Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Audio
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        rtcEngine?.muteLocalAudioStream(isMuted)
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.White else Color.DarkGray.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color.Black else Color.White
                    )
                }

                // Send Gift Button (Special Highlighted)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF261C4E))
                        .border(2.dp, Color(0xFFFFD700), CircleShape)
                        .clickable { showGiftBottomSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 26.sp)
                }

                // End Call
                IconButton(
                    onClick = { onCallEnded() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Mute Video
                IconButton(
                    onClick = {
                        isVideoMuted = !isVideoMuted
                        rtcEngine?.muteLocalVideoStream(isVideoMuted)
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isVideoMuted) Color.White else Color.DarkGray.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Video",
                        tint = if (isVideoMuted) Color.Black else Color.White
                    )
                }
            }
        }

        // 5. Gift Bottom Sheet
        if (showGiftBottomSheet) {
            GiftBottomSheet(
                currentCoins = coinsBalance,
                onDismiss = { showGiftBottomSheet = false },
                onSendGift = { gift ->
                    viewModel.sendGift(channelName, gift)
                    Toast.makeText(context, "Sent ${gift.name}! ${gift.icon}", Toast.LENGTH_SHORT).show()
                },
                onRechargeClicked = {
                    showGiftBottomSheet = false
                    Toast.makeText(context, "Redirecting to Wallet...", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Report Dialog 1 (Reason Selection)
        if (showReportDialog1) {
            AlertDialog(
                onDismissRequest = { 
                    showReportDialog1 = false 
                    selectedReason = ""
                },
                title = { Text("Report User (Step 1 of 2)", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Please select a reason for reporting this user:")
                        Spacer(modifier = Modifier.height(12.dp))
                        val reasons = listOf("Nudity / Sexual Content", "Harassment / Abuse", "Spam / Scam", "Other Violations")
                        reasons.forEach { reason ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReason = reason }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = (selectedReason == reason),
                                    onClick = { selectedReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(reason)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showReportDialog1 = false
                            showReportDialog2 = true
                        },
                        enabled = selectedReason.isNotEmpty()
                    ) {
                        Text("Continue", color = if (selectedReason.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showReportDialog1 = false 
                        selectedReason = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Report Dialog 2 (Confirmation)
        if (showReportDialog2) {
            AlertDialog(
                onDismissRequest = { showReportDialog2 = false },
                title = { Text("Confirm Abuse Report (Step 2 of 2)", fontWeight = FontWeight.Bold) },
                text = { Text("Are you absolutely sure you want to report and block this user? This will end the call immediately.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showReportDialog2 = false
                            coroutineScope.launch {
                                try {
                                    val sessionManager = com.videoChatting.echat.data.local.SessionManager(context)
                                    
                                    val retrofit = Retrofit.Builder()
                                        .baseUrl(com.videoChatting.echat.utils.Constants.BASE_URL)
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build()
                                    val service = retrofit.create(ApiService::class.java)

                                    service.raiseRequest(
                                        RaiseRequestDto(
                                            type = "report_user",
                                            targetId = remoteUid.toString(),
                                            reason = selectedReason
                                        )
                                    )
                                    
                                    Toast.makeText(context, "User reported and blocked successfully.", Toast.LENGTH_LONG).show()
                                    onCallEnded()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Network Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    onCallEnded() // End the call anyway for safety
                                }
                            }
                        }
                    ) {
                        Text("Yes, Report & Block", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog2 = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
