package com.videoChatting.echat.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.videoChatting.echat.presentation.theme.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

data class DummyChat(
    val id: String, 
    val name: String, 
    val lastMessage: String, 
    val time: String, 
    val categories: List<String> = emptyList(),
    val unreadCount: Int = 0,
    val avatar: String = "",
    val isLiked: Boolean = false,
    val isAdded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsHomeScreen(
    navController: NavController, 
    onNavigateToDiscovery: () -> Unit = {},
    viewModel: ChatsHomeViewModel = hiltViewModel()
) {
    val interactedUsers by viewModel.interactedUsers.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadInteractions()
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Liked", "Added", "Consent")

    val pendingConsentCount by viewModel.pendingConsentCount.collectAsState()

    val filteredChats = interactedUsers.filter { chat ->
        val matchesSearch = chat.name.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Liked" -> chat.categories.contains("liked")
            "Added" -> chat.categories.contains("added")
            "Consent" -> chat.categories.contains("consent")
            else -> true
        }
        matchesSearch && matchesFilter
    }.distinctBy { it.id }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(bottom = 10.dp)){
                // Title bar
                TopAppBar(
                    title = { Text("Talksy", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                    actions = {
                        val currentCoins by viewModel.currentCoins.collectAsState()
                        IconButton(onClick = { navController.navigate("invite_and_contacts") }) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Invite Friends",
                                tint = ElectricIndigo
                            )
                        }
                        TextButton(onClick = { navController.navigate("wallet") }) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = "Wallet", tint = CoinGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(currentCoins.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { navController.navigate("consent_notifications") }) {
                            if (pendingConsentCount > 0) {
                                BadgedBox(badge = { Badge { Text(pendingConsentCount.toString()) } }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                }
                            } else {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                    },
                    windowInsets = WindowInsets(top = 0.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Search Bar
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(42.dp) // 25% height reduction from standard 56.dp
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToDiscovery,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat (Omegle)")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 115.dp)
//                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
                ) {
                    // Filters
                    item {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filters,key = {it}) { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(35.dp),
                                    modifier = Modifier.height(31.dp)
                                )
                            }
                        }
                    }
                    if (filteredChats.isEmpty()) {
                        item{
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Empty",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No chats yet.",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Start discovering people to chat!",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                    else {
                    items(filteredChats) { chat ->
                        ChatItem(chat = chat) {
                            navController.navigate("chat/${chat.id}/${chat.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: DummyChat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp)
        ) {
            if (chat.avatar.isNotEmpty()) {
                AsyncImage(
                    model = chat.avatar,
                    contentDescription = "Profile",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "It's a ${chat.lastMessage} profile",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                fontSize = 13.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = chat.time,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
