package com.videoChatting.echat.presentation.contacts

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.videoChatting.echat.data.remote.model.DeviceContact
import com.videoChatting.echat.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteAndContactsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val referralCode by viewModel.referralCode.collectAsState()
    val referralCount by viewModel.referralCount.collectAsState()
    val coinsEarned by viewModel.coinsEarned.collectAsState()
    val bonusPerReferral by viewModel.bonusPerReferral.collectAsState()
    val hasClaimedReferral by viewModel.hasClaimedReferral.collectAsState()
    val blockContactsMatching by viewModel.blockContactsMatching.collectAsState()
    val registeredContacts by viewModel.registeredContacts.collectAsState()
    val inviteContacts by viewModel.inviteContacts.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Invite, 1: On Talksy, 2: Privacy
    var redeemCodeInput by remember { mutableStateOf("") }
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        if (isGranted) {
            viewModel.syncContacts(context)
            Toast.makeText(context, "Contacts synced successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied to read contacts", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            viewModel.syncContacts(context)
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = CyberMidnight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Invite & Contacts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (hasContactsPermission) {
                        IconButton(
                            onClick = { viewModel.syncContacts(context) },
                            enabled = !isSyncing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = if (isSyncing) ElectricIndigo else Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberMidnight)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Hero Referral Card (Always Visible)
            ReferralHeroCard(
                referralCode = referralCode,
                referralCount = referralCount,
                coinsEarned = coinsEarned,
                bonusPerReferral = bonusPerReferral,
                onCopyCode = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Talksy Referral Code", referralCode)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Code copied: $referralCode", Toast.LENGTH_SHORT).show()
                },
                onShareCode = {
                    val shareText = "Hey! Join me on Talksy for fun live video chats and making new friends. Use my invite code *$referralCode* to get 50 free coins! Download now: https://talksy.app"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Talksy Invite Code"))
                }
            )

            // 2. Custom Clean 3-Tab Segmented Switcher (Pixel-Perfect Padding & Clear Labels)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF16122E))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 0: Invite
                TabPillButton(
                    modifier = Modifier.weight(1f),
                    title = "Invite",
                    badgeCount = inviteContacts.size,
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )

                // Tab 1: On Talksy
                TabPillButton(
                    modifier = Modifier.weight(1f),
                    title = "Friends",
                    badgeCount = registeredContacts.size,
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )

                // Tab 2: Redeem & Privacy
                TabPillButton(
                    modifier = Modifier.weight(1f),
                    title = "Privacy",
                    badgeCount = null,
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Main Tab Content / Permission Gate
            if (!hasContactsPermission && (selectedTab == 0 || selectedTab == 1)) {
                ContactsPermissionPrompt(
                    onGrantPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> InviteContactsTab(
                        contacts = inviteContacts,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.onSearchQueryChanged(it) },
                        referralCode = referralCode,
                        context = context
                    )
                    1 -> RegisteredContactsTab(
                        contacts = registeredContacts,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.onSearchQueryChanged(it) },
                        onNavigateToChat = onNavigateToChat
                    )
                    2 -> PrivacyAndRedeemTab(
                        hasClaimedReferral = hasClaimedReferral,
                        blockContactsMatching = blockContactsMatching,
                        redeemCodeInput = redeemCodeInput,
                        onRedeemCodeChange = { redeemCodeInput = it },
                        onClaimReferral = {
                            viewModel.claimReferralCode(redeemCodeInput) { bonus ->
                                redeemCodeInput = ""
                                Toast.makeText(context, "🎉 +$bonus Coins added to your wallet!", Toast.LENGTH_LONG).show()
                            }
                        },
                        onTogglePrivacy = { viewModel.togglePrivacy(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    modifier: Modifier = Modifier,
    title: String,
    badgeCount: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Brush.horizontalGradient(listOf(ElectricIndigo, ElectricViolet))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
            if (badgeCount != null && badgeCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0x33FFFFFF))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$badgeCount",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferralHeroCard(
    referralCode: String,
    referralCount: Int,
    coinsEarned: Int,
    bonusPerReferral: Int,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E103A), Color(0xFF0F172A))
                    )
                )
                .border(BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎁", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Invite & Earn",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoinGold.copy(alpha = 0.15f))
                            .border(1.dp, CoinGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "+$bonusPerReferral 🪙 per Friend",
                            color = CoinGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Referral Code Box + Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x66000000))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YOUR INVITE CODE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = referralCode.ifEmpty { "LOADING..." },
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Copy Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onCopyCode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // WhatsApp / Share Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                                .clickable { onShareCode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$referralCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Friends Joined", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.1f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$coinsEarned 🪙", color = CoinGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Total Earned", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsPermissionPrompt(
    onGrantPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CyberMidnight)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF261C4E)),
                contentAlignment = Alignment.Center
            ) {
                Text("👥", fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Find & Invite Your Friends",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Talksy uses your contacts to find friends who are already using the app and let you invite others to earn 50 free coins each! Your contacts remain 100% private.",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onGrantPermission,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Contacts, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Phone Contacts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun InviteContactsTab(
    contacts: List<DeviceContact>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    referralCode: String,
    context: Context
) {
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Search by name or number...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.5f)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF16122E),
                unfocusedContainerColor = Color(0xFF16122E),
                focusedBorderColor = ElectricIndigo,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        if (filteredContacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) "No contacts found on device" else "No matching contacts found",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts, key = { it.phoneNumber }) { contact ->
                    ContactInviteRow(contact = contact) {
                        val inviteMessage = "Hey ${contact.name}! I'm on Talksy for fun video chatting and meeting cool people. Join with my code *$referralCode* to get 50 free coins! Download: https://talksy.app"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, inviteMessage)
                        }
                        context.startActivity(Intent.createChooser(intent, "Invite ${contact.name} via"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactInviteRow(
    contact: DeviceContact,
    onInvite: () -> Unit
) {
    // Clean formatted name
    val cleanName = remember(contact.name, contact.phoneNumber) {
        val stripped = contact.name.trim().trim('"', '\'', '(', ')', '[', ']', '{', '}')
        if (stripped.isBlank() || stripped.startsWith("+") || stripped.all { it.isDigit() || it == '+' || it == ' ' }) {
            contact.phoneNumber
        } else {
            stripped
        }
    }

    // Extract first letter or null
    val initialLetter = remember(cleanName) {
        cleanName.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16122E))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Avatar Initial with gradient
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2E1065), Color(0xFF1E1B4B)))),
                contentAlignment = Alignment.Center
            ) {
                if (initialLetter != null) {
                    Text(
                        text = initialLetter,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.phoneNumber,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Sleek Gradient Invite Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(ElectricIndigo, Color(0xFF2563EB))))
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable { onInvite() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Invite +50🪙", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RegisteredContactsTab(
    contacts: List<DeviceContact>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Search friends...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.5f)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF16122E),
                unfocusedContainerColor = Color(0xFF16122E),
                focusedBorderColor = ElectricIndigo,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📱", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "None of your phone contacts are on Talksy yet.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Switch to the Invite tab to invite them and earn 50 coins each!",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts, key = { it.phoneNumber }) { contact ->
                    val user = contact.registeredUser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF16122E))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF261C4E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (user?.name ?: contact.name).take(1).uppercase(),
                                        color = Color(0xFF00E676),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                if (user?.isOnline == true) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                            .border(2.dp, CyberMidnight, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = user?.name ?: contact.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (user?.isOnline == true) "Online Now 🟢" else "Offline",
                                    color = if (user?.isOnline == true) Color(0xFF00E676) else Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Chat Action Button
                        IconButton(
                            onClick = { user?.let { onNavigateToChat(it.userId) } },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyAndRedeemTab(
    hasClaimedReferral: Boolean,
    blockContactsMatching: Boolean,
    redeemCodeInput: String,
    onRedeemCodeChange: (String) -> Unit,
    onClaimReferral: () -> Unit,
    onTogglePrivacy: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Redeem Referral Code Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16122E)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎟️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Redeem Friend's Invite Code",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (hasClaimedReferral)
                        "You have already redeemed an invite code and received your 50 free coins!"
                    else
                        "Enter the invite code given by your friend to claim your 50 free welcome coins!",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (!hasClaimedReferral) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = redeemCodeInput,
                            onValueChange = onRedeemCodeChange,
                            placeholder = { Text("e.g. TALK-9X82", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x33000000),
                                unfocusedContainerColor = Color(0x33000000),
                                focusedBorderColor = ElectricIndigo,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Button(
                            onClick = onClaimReferral,
                            enabled = redeemCodeInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                        ) {
                            Text("Claim 🪙", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 2. Privacy Mode: Exclude Contacts Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16122E)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hide from Phone Contacts",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Prevent anyone in your phone contacts from randomly matching with you during video discovery.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = blockContactsMatching,
                    onCheckedChange = onTogglePrivacy,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ElectricIndigo,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }
        }
    }
}
