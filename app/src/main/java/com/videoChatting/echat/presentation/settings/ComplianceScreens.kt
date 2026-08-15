package com.videoChatting.echat.presentation.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
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
import androidx.navigation.NavController
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.RaiseRequestDto
import com.videoChatting.echat.presentation.theme.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getThemeBackgroundGradient())
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground())
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Effective Date: August 1, 2026", fontWeight = FontWeight.Bold, color = getThemeTextColor())
                        Text("Last Updated: August 1, 2026", fontSize = 12.sp, color = getThemeSubTextColor())
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PolicySection(
                            title = "1. Introduction & Global Standards",
                            content = "Talksy (collectively referred to as 'we', 'our', or 'us') is committed to protecting your privacy. This Privacy Policy outlines how we collect, process, secure, and share your personal data. We comply with global data protection standards, including the General Data Protection Regulation (GDPR), California Consumer Privacy Act (CCPA), and Children's Online Privacy Protection Act (COPPA). By creating an account or using the app, you consent to our practices.",
                            icon = Icons.Default.Info
                        )
                        
                        PolicySection(
                            title = "2. Comprehensive Information We Collect",
                            content = "• Account Profile: Authentication is handled securely via Google Sign-In or Guest session parameters. We receive and store your email address, full name, avatar picture url, and Google unique identifier.\n\n• Video & Audio Streams: To support live matches, your camera and mic streams are transmitted directly to your partner using Agora RTC peer-to-peer nodes. We never record, intercept, intercept metadata of, or store video/audio calls on our databases. All real-time streams are private.\n\n• Location Data: The app processes geolocation parameters locally to match users by region/country. Precise coordinates (latitude/longitude) are only used locally and are not persistently stored on our servers. Your matches only see your generalized country.\n\n• Push Notification Tokens: We collect and upload Firebase Cloud Messaging (FCM) tokens to route call invites and chat notifications. You can disable this in your system settings.",
                            icon = Icons.Default.Assignment
                        )
                        
                        PolicySection(
                            title = "3. Matchmaking Policy & Data Processing",
                            content = "Matchmaking runs dynamically using in-memory caches (Redis) to group online users. Your matching filters (age range, gender preference, and country) are processed instantly to generate pairs and are automatically cleared from active memory when you leave the queue. We do not track or build behavioral profiling directories of your calling history.",
                            icon = Icons.Default.Group
                        )
                        
                        PolicySection(
                            title = "4. Consent-Before-Chat Policy",
                            content = "To protect user privacy and prevent harassment, Talksy enforces a strict Consent-Before-Chat policy. You cannot initiate direct text messages or call users outside the discovery queue unless both parties mutually add each other during a video match. Once mutual consent is established, a chat room is created, allowing messaging. Consent can be revoked at any time by blocking the user.",
                            icon = Icons.Default.Verified
                        )
                        
                        PolicySection(
                            title = "5. Notification & Foreground Services Policy",
                            content = "We utilize foreground services and Firebase Cloud Messaging (FCM) to trigger instant notifications for call invitations. We collect, refresh, and store device notification tokens solely to deliver real-time chat alerts and call signaling. Users have granular control to toggle notifications within Settings or completely disable them via Android System Settings.",
                            icon = Icons.Default.Notifications
                        )
                        
                        PolicySection(
                            title = "6. Security & Encryption Standards",
                            content = "All API communications between the application and the backend are encrypted using Transport Layer Security (TLS/HTTPS). User profiles and metadata are stored on MongoDB databases protected by strict IAM policies. Agora call sessions use dynamic security token tokens generated by our backend node.js server for every single call to prevent unauthorized eavesdropping.\n\n• Chat Privacy: All direct messages are encrypted at rest using industry-standard AES-256-CBC algorithm. Nobody, including developers or database administrators, can access or read your chats.\n\n• Media Upload Security: Profile picture uploads are compressed locally (under 30KB) before transmission, and old avatars are immediately deleted from server disks to prevent unused media footprint.\n\n• Reverse Engineering Protection: Release builds are minified and obfuscated using Android's R8 optimizer, securing our source code from decompilers.",
                            icon = Icons.Default.Security
                        )
                        
                        PolicySection(
                            title = "7. Data Deletion & Retention",
                            content = "Under Play Store guidelines, we provide a clear in-app mechanism to delete your account. You can request permanent account deletion via Settings -> Delete Account. This request instantly submits a job to wipe your profile data, email records, friend lists, chat logs, and coin history from our active databases. Requests are logged in our compliance queue and completed within 14 days.",
                            icon = Icons.Default.Delete
                        )
                        
                        PolicySection(
                            title = "8. Policy Updates & Contact",
                            content = "We may update this policy periodically. If you have questions regarding data security, GDPR compliance, or account deletion, contact our Privacy Office at shriramasociate17@gmail.com.",
                            icon = Icons.Default.Email
                        )

                        PolicySection(
                            title = "9. Live Reporting & Moderation System",
                            content = "To maintain a safe and respectful community, Talksy enforces a strict Zero-Tolerance Policy for harassment, nudity, and abuse. \n\n• Instant Shielding: When you report a user during a live call, the video session is instantly terminated, immediately shielding you from the reported user.\n\n• Backend Review & Actions: Our server automatically logs a formal abuse request against the reported user. If a user receives frequent or repetitive reports from different participants, our moderation team will investigate the logs and take appropriate action. This may include matching restrictions, temporary suspension, or permanent account termination to protect the community.",
                            icon = Icons.Default.Security
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getThemeBackgroundGradient())
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground())
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Effective Date: August 1, 2026", fontWeight = FontWeight.Bold, color = getThemeTextColor())
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PolicySection(
                            title = "1. License & Eligibility",
                            content = "By accessing Talksy, you warrant that you are at least 18 years old (or the legal age of majority in your jurisdiction). We grant you a limited, non-exclusive, non-transferable, revocable license to use the app for personal, non-commercial communication purposes."
                        )
                        
                        PolicySection(
                            title = "2. User-Generated Content (UGC) Guidelines",
                            content = "Talksy maintains a Zero-Tolerance policy for inappropriate or abusive content. You are strictly forbidden from showing, sharing, or promoting:\n\n• Explicit nudity, sexual expressions, pornography, or sexually suggestive behavior during video calls and text chats.\n• Threats, harassment, bullying, verbal abuse, hate speech, or discrimination based on race, religion, gender, or nationality.\n• Scam campaigns, fraud, financial requests, spam, phishing links, or unauthorized advertising.\n• Intellectual property theft or impersonating other individuals.\n\nFailure to comply will result in immediate suspension, account termination, and permanent device-level bans."
                        )
                        
                        PolicySection(
                            title = "3. Reporting & Moderation Infrastructure",
                            content = "We have implemented automated and manual moderation tools. During any live call, you can tap the 'Report User' flag. Submitting a report instantly terminates the call, automatically blocks the user to prevent future matching, and logs the report under our backend requests database for immediate administrator review. Users found violating terms will be banned permanently."
                        )
                        
                        PolicySection(
                            title = "4. Consent & User Interaction Rules",
                            content = "Direct messaging is locked until both users provide mutual consent. Users must respect other users' privacy. Attempting to bypass consent limits (e.g. through screen recording, reverse engineering, or exploiting network sockets) constitutes a material breach of these terms."
                        )
                        
                        PolicySection(
                            title = "5. Wallet, Billing & Refund Policy",
                            content = "Talksy matches consume virtual coins (10 coins per minute of call time). Coin packages are purchased using official payment gateways (UPI, Cards) routed via Juspay. All purchases are final. Coins are non-transferable and non-refundable. If an account is banned due to a violation of our UGC rules, all remaining virtual coin balances are forfeited."
                        )
                        
                        PolicySection(
                            title = "6. Limitation of Liability",
                            content = "Talksy provides services on an 'as-is' and 'as-available' basis. We make no guarantees regarding uninterrupted call connectivity, Agora stream quality, or backend uptime. We are not liable for user behavior during calls or direct chats. You use the platform at your own discretion."
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(navController: NavController) {
    val context = LocalContext.current
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = pInfo.versionName ?: "1.1"
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        pInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION") pInfo.versionCode.toLong()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Us", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getThemeBackgroundGradient())
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Talksy", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = getThemeTextColor())
                Text("Version $versionName ($versionCode)", fontSize = 14.sp, color = getThemeSubTextColor())
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground())
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Talksy is a premium, real-time video matchmaking and direct messaging application designed for modern mobile users. Our mission is to facilitate high-quality, instantaneous, and secure global connections.\n\nBuilt using bleeding-edge technologies including Jetpack Compose, Agora RTC, Socket.io, and MongoDB, Talksy delivers latency-free calling with robust user safety and payment security integrations.",
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = getThemeTextColor()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // FAQs Expandable States
    var faq1Expanded by remember { mutableStateOf(false) }
    var faq2Expanded by remember { mutableStateOf(false) }
    var faq3Expanded by remember { mutableStateOf(false) }
    var faq4Expanded by remember { mutableStateOf(false) }
    var faq5Expanded by remember { mutableStateOf(false) }
    var faq6Expanded by remember { mutableStateOf(false) }
    var faq7Expanded by remember { mutableStateOf(false) }
    var faq8Expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getThemeBackgroundGradient())
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // FAQs Section
                Text("Frequently Asked Questions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = getThemeTextColor())
                Spacer(modifier = Modifier.height(12.dp))
                
                FAQItem(
                    question = "1. How does the matchmaking algorithm pair users?",
                    answer = "Our matchmaking algorithm groups active online users using a dynamic Redis server. It matches you based on your selected target age, gender preference, and country region filters. If no local match is found, it widens parameters to regional or global levels to guarantee instant call connections.",
                    expanded = faq1Expanded,
                    onToggle = { faq1Expanded = !faq1Expanded }
                )
                FAQItem(
                    question = "2. Are my video calls and text messages private and secure?",
                    answer = "Absolutely. Video and audio streams are fully peer-to-peer (P2P) using encrypted Agora RTC nodes, which means they never touch or record on our servers. Direct text messages are sent over secure WebSocket connections and stored in encrypted MongoDB databases. We maintain a strict Privacy-first policy.",
                    expanded = faq2Expanded,
                    onToggle = { faq2Expanded = !faq2Expanded }
                )
                FAQItem(
                    question = "3. What is the coin-based calling policy?",
                    answer = "Connecting to a matched partner consumes virtual coins. The current deduction rate is 10 coins per minute of active calling. The system automatically monitors your coin balance and terminates the call safely if your balance reaches zero. You receive 100 free coins on sign-up as a trial.",
                    expanded = faq3Expanded,
                    onToggle = { faq3Expanded = !faq3Expanded }
                )
                FAQItem(
                    question = "4. How does the blocking and reporting system work?",
                    answer = "If you encounter an abusive user, tap the 'Report User' flag at the top-left of the call screen. Choose the violation category and submit. The call ends instantly, the user is permanently added to your Blocked list to prevent future matchmaking, and a ticket is logged in our database for manual review.",
                    expanded = faq4Expanded,
                    onToggle = { faq4Expanded = !faq4Expanded }
                )
                FAQItem(
                    question = "5. How do I delete my account and associated data?",
                    answer = "You can delete your account by going to Settings -> Delete Account. This requires double-confirmation. Once confirmed, you are logged out, and a delete request is submitted to our backend requests collection. We permanently wipe your email, name, avatar, coins, friend lists, and chats from MongoDB within 14 days.",
                    expanded = faq5Expanded,
                    onToggle = { faq5Expanded = !faq5Expanded }
                )
                FAQItem(
                    question = "6. What is the Consent-Before-Chat policy?",
                    answer = "To protect users from unwanted messages and spam, you cannot message or view the profile details of other users outside active call matching unless both users mutually click the 'Add Friend' button during the match call. Once mutual consent is granted, a secure chat room opens.",
                    expanded = faq6Expanded,
                    onToggle = { faq6Expanded = !faq6Expanded }
                )
                FAQItem(
                    question = "7. How do push notifications work and how can I disable them?",
                    answer = "Talksy uses Firebase Cloud Messaging (FCM) to trigger alerts for incoming chats and call match invites. This requires background token registration. You can customize which notifications you receive inside Settings -> Notification Settings, or disable notifications entirely through your Android app system settings.",
                    expanded = faq7Expanded,
                    onToggle = { faq7Expanded = !faq7Expanded }
                )
                FAQItem(
                    question = "8. Can I get a refund for purchased coin packages?",
                    answer = "All coin package recharges routed through Juspay are final and non-refundable, as they represent virtual goods consumed in real-time. If your account is banned due to violation of our Terms of Service (such as sharing nudity or engaging in abuse), all unused coins are permanently forfeited.",
                    expanded = faq8Expanded,
                    onToggle = { faq8Expanded = !faq8Expanded }
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                // Contact Form Section
                Text("Raise a Support Ticket", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = getThemeTextColor())
                Spacer(modifier = Modifier.height(8.dp))
                Text("If you have any queries or issues, submit a ticket below. Our support team will resolve it and email you back.", fontSize = 13.sp, color = getThemeSubTextColor())
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Describe your issue/query", color = getThemeSubTextColor()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricIndigo,
                        unfocusedBorderColor = getThemeGlassBorder(),
                        focusedContainerColor = getThemeGlassBackground(),
                        unfocusedContainerColor = getThemeGlassBackground()
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (messageText.isBlank()) {
                            Toast.makeText(context, "Please enter your query first", Toast.LENGTH_SHORT).show()
                        } else {
                            showConfirmDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Submit Ticket", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Double Confirmation Dialog for Ticket Submission
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Confirm Ticket Submission") },
                    text = { Text("Are you sure you want to submit this support ticket? This action will log a request in our support database.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmDialog = false
                                isSending = true
                                coroutineScope.launch {
                                    try {
                                        val sessionManager = SessionManager(context)
                                        val token = sessionManager.getAuthToken()
                                        
                                        // Simple local Retrofit instance builder pointing to constants SOCKET_URL
                                        val retrofit = Retrofit.Builder()
                                            .baseUrl(com.videoChatting.echat.utils.Constants.BASE_URL)
                                            .addConverterFactory(GsonConverterFactory.create())
                                            .build()
                                        val service = retrofit.create(ApiService::class.java)

                                        val res = service.raiseRequest(
                                            RaiseRequestDto(
                                                type = "support",
                                                reason = messageText
                                            )
                                        )
                                        if (res.isSuccessful && res.body()?.success == true) {
                                            Toast.makeText(context, "Support ticket submitted. Check your email for updates.", Toast.LENGTH_LONG).show()
                                            messageText = ""
                                        } else {
                                            Toast.makeText(context, "Failed to submit ticket: ${res.message()}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Connection Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        ) {
                            Text("Yes, Submit", fontWeight = FontWeight.Bold, color = ElectricViolet)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FAQItem(question: String, answer: String, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = question,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = getThemeTextColor(),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = getThemeSubTextColor()
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = answer, fontSize = 14.sp, color = getThemeSubTextColor(), lineHeight = 20.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Us", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getThemeBackgroundGradient())
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground())
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Get in Touch", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElectricViolet)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ContactRow(icon = Icons.Default.Email, label = "Support Email", value = "shriramasociate17@gmail.com")
                        Spacer(modifier = Modifier.height(16.dp))
                        ContactRow(icon = Icons.Default.Phone, label = "Helpline", value = "+919896706009")
                        Spacer(modifier = Modifier.height(16.dp))
                        ContactRow(icon = Icons.Default.LocationOn, label = "Office Address", value = "Gulab Singh\nNear Rksd College, Ambala Road\nKaithal - 136027\nIndia (IN)")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Our support team typically responds to inquiries within 24 hours.",
                    fontSize = 13.sp,
                    color = getThemeSubTextColor(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = getThemeSubTextColor(), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 15.sp, color = getThemeTextColor())
        }
    }
}

@Composable
fun PolicySection(title: String, content: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ElectricViolet)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(content, fontSize = 14.sp, color = getThemeTextColor(), lineHeight = 20.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var currentSelection by remember { mutableStateOf(ThemeConfig.themeSelection.value) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getThemeBackgroundGradient())
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, getThemeGlassBorder(), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = getThemeGlassBackground()),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose App Theme",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = getThemeTextColor()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ThemeOptionRow(
                            title = "System Default",
                            isSelected = currentSelection == "system",
                            onSelect = {
                                currentSelection = "system"
                                ThemeConfig.themeSelection.value = "system"
                                sessionManager.saveTheme("system")
                                Toast.makeText(context, "System Default Theme Selected", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = getThemeGlassBorder(), modifier = Modifier.padding(vertical = 4.dp))
                        ThemeOptionRow(
                            title = "Light Theme",
                            isSelected = currentSelection == "light",
                            onSelect = {
                                currentSelection = "light"
                                ThemeConfig.themeSelection.value = "light"
                                sessionManager.saveTheme("light")
                                Toast.makeText(context, "Light Theme Selected", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = getThemeGlassBorder(), modifier = Modifier.padding(vertical = 4.dp))
                        ThemeOptionRow(
                            title = "Dark Theme",
                            isSelected = currentSelection == "dark",
                            onSelect = {
                                currentSelection = "dark"
                                ThemeConfig.themeSelection.value = "dark"
                                sessionManager.saveTheme("dark")
                                Toast.makeText(context, "Dark Theme Selected", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = getThemeTextColor(),
            fontWeight = FontWeight.Medium
        )
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = ElectricViolet,
                unselectedColor = getThemeSubTextColor().copy(alpha = 0.5f)
            )
        )
    }
}

