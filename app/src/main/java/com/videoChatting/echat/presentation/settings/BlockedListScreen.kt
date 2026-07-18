package com.videoChatting.echat.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.videoChatting.echat.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.gson.annotations.SerializedName

data class BlockedUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("avatar") val avatar: String
)

data class BlockedListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("blockedUsers") val blockedUsers: List<BlockedUserDto>
)

data class UnblockRequest(
    @SerializedName("targetUserId") val targetUserId: String
)

data class UnblockResponse(
    @SerializedName("success") val success: Boolean
)

@HiltViewModel
class BlockedListViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _blockedUsers = MutableStateFlow<List<BlockedUserDto>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUserDto>> = _blockedUsers

    init {
        loadBlockedUsers()
    }

    private fun loadBlockedUsers() {
        viewModelScope.launch {
            try {
                // We assume apiService.getBlockedList() and apiService.unblockUser() are added to ApiService.kt
                val response = apiService.getBlockedList()
                if (response.isSuccessful && response.body() != null) {
                    _blockedUsers.value = response.body()!!.blockedUsers
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.unblockUser(UnblockRequest(targetUserId))
                if (response.isSuccessful) {
                    _blockedUsers.value = _blockedUsers.value.filter { it.id != targetUserId }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedListScreen(
    navController: NavController,
    viewModel: BlockedListViewModel = hiltViewModel()
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Blocked Users", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (blockedUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No blocked users.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items(blockedUsers) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = user.name,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = { viewModel.unblockUser(user.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Unblock", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
