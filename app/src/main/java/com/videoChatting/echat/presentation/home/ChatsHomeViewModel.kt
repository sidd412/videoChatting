package com.videoChatting.echat.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.videoChatting.echat.data.remote.SocketManager
import com.videoChatting.echat.data.remote.SocketEvent

import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.content.SharedPreferences

@HiltViewModel
class ChatsHomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val apiService: ApiService,
    private val socketManager: SocketManager,
    private val sessionManager: com.videoChatting.echat.data.local.SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _interactedUsers = MutableStateFlow<List<DummyChat>>(emptyList())
    val interactedUsers: StateFlow<List<DummyChat>> = _interactedUsers

    private val _pendingConsentCount = MutableStateFlow(0)
    val pendingConsentCount: StateFlow<Int> = _pendingConsentCount
    
    private val _currentCoins = MutableStateFlow(sessionManager.getUserProfile()?.coinsBalance ?: 0)
    val currentCoins: StateFlow<Int> = _currentCoins

    init {
        // Ensure socket is connected so we can receive real-time events
        socketManager.connect()
        
        viewModelScope.launch {
            socketManager.matchEvents.collect { event ->
                if (event is SocketEvent.ConsentNotification) {
                    loadInteractions()
                }
            }
        }
        viewModelScope.launch {
            socketManager.chatMessages.collect {
                // When a normal message arrives, update the interactions list to show new last message
                loadInteractions()
            }
        }
    }

    private val prefs by lazy { context.getSharedPreferences("offline_cache", Context.MODE_PRIVATE) }
    private val gson = com.google.gson.Gson()

    fun loadInteractions() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        
        _currentCoins.value = sessionManager.getUserProfile()?.coinsBalance ?: 0
        
        // Load from cache first for offline support
        val cached = prefs.getString("interactions_$currentUserId", null)
        if (cached != null) {
            try {
                val listType = object : com.google.gson.reflect.TypeToken<List<DummyChat>>() {}.type
                val cachedList: List<DummyChat> = gson.fromJson(cached, listType)
                _interactedUsers.value = cachedList.filter { it.isAdded }
                _pendingConsentCount.value = cachedList.count { it.isLiked && !it.isAdded }
            } catch (e: Exception) {
                // Ignore cache read errors
            }
        }

        viewModelScope.launch {
            try {
                // Fetch interactions
                val response = apiService.getInteractions()
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.interactions.map { item ->
                        DummyChat(
                            id = item.id,
                            name = item.name,
                            avatar = item.avatar,
                            lastMessage = item.lastMessage,
                            time = item.time,
                            categories = item.categories,
                            unreadCount = item.unreadCount,
                            isLiked = item.isLiked,
                            isAdded = item.isAdded
                        )
                    }
                    
                    // Save to offline cache
                    prefs.edit().putString("interactions_$currentUserId", gson.toJson(list)).apply()

                    _interactedUsers.value = list.filter { it.isAdded }
                    _pendingConsentCount.value = list.count { it.isLiked && !it.isAdded }
                }

                // Fetch pending consents count
                val consentResponse = apiService.getPendingConsents(currentUserId)
                if (consentResponse.isSuccessful && consentResponse.body() != null) {
                    _pendingConsentCount.value = consentResponse.body()!!.notifications.size
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
