package com.videoChatting.echat.presentation.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.MatchResponse
import com.videoChatting.echat.data.remote.SocketEvent
import com.videoChatting.echat.domain.repository.MatchmakingRepository
import com.videoChatting.echat.domain.repository.UserRepository
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.RaiseRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Searching : DiscoveryState()
    data class Matched(val match: MatchResponse) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val matchmakingRepository: MatchmakingRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val apiService: ApiService,
    private val socketManager: com.videoChatting.echat.data.remote.SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()
    
    private val _currentCoins = MutableStateFlow(sessionManager.getUserProfile()?.coinsBalance ?: 0)
    val currentCoins: StateFlow<Int> = _currentCoins.asStateFlow()

    private val _activeGiftEvent = MutableStateFlow<com.videoChatting.echat.presentation.call.GiftReceivedEvent?>(null)
    val activeGiftEvent: StateFlow<com.videoChatting.echat.presentation.call.GiftReceivedEvent?> = _activeGiftEvent.asStateFlow()

    private val _floatingEmojis = MutableStateFlow<List<com.videoChatting.echat.presentation.call.FloatingEmojiItem>>(emptyList())
    val floatingEmojis: StateFlow<List<com.videoChatting.echat.presentation.call.FloatingEmojiItem>> = _floatingEmojis.asStateFlow()

    private val currentUserId: String? = userRepository.getCurrentUserId()

    init {
        // Observe Socket Matchmaking events in real-time
        viewModelScope.launch {
            matchmakingRepository.observeEvents().collect { event ->
                when (event) {
                    is SocketEvent.Searching -> {
                        _state.value = DiscoveryState.Searching
                    }
                    is SocketEvent.MatchFound -> {
                        _state.value = DiscoveryState.Matched(event.match)
                    }
                    is SocketEvent.PartnerLeft -> {
                        // If partner leaves, automatically restart matching queue
                        _state.value = DiscoveryState.Searching
                        startDiscovery()
                    }
                    is SocketEvent.Error -> {
                        _state.value = DiscoveryState.Error(event.message)
                    }
                    is SocketEvent.ConsentNotification -> {
                        // Ignore in Discovery screen
                    }
                    is SocketEvent.WalletUpdate -> {
                        _currentCoins.value = event.coinsBalance
                        sessionManager.updateCoins(event.coinsBalance)
                    }
                    is SocketEvent.InsufficientFunds -> {
                        _state.value = DiscoveryState.Error(event.message)
                        matchmakingRepository.endActiveCall()
                    }
                }
            }
        }

        // Observe live Gift Events from partner
        viewModelScope.launch {
            socketManager.giftReceivedEvents.collect { giftEvent ->
                _activeGiftEvent.value = giftEvent
                val profile = sessionManager.getUserProfile()
                _currentCoins.value = profile?.coinsBalance ?: 0
            }
        }

        // Observe live Floating Reactions from partner
        viewModelScope.launch {
            socketManager.reactionReceivedEvents.collect { emoji ->
                triggerLocalReaction(emoji)
            }
        }
    }

    fun startDiscovery() {
        if (currentUserId == null) {
            _state.value = DiscoveryState.Error("User not logged in")
            return
        }

        if (_currentCoins.value < 10) {
            _state.value = DiscoveryState.Error("Insufficient coins! (Min: 10)")
            return
        }

        _state.value = DiscoveryState.Searching

        // Get coordinates from local user profile cache saved during onboarding
        val profile = sessionManager.getUserProfile()
        val lat = profile?.preferences?.let { 0.0 } ?: 0.0 // Coordinates can be read if stored, fallback to 0.0
        val lng = profile?.preferences?.let { 0.0 } ?: 0.0
        
        matchmakingRepository.joinQueue(longitude = lng, latitude = lat)
    }

    fun nextPerson() {
        if (currentUserId == null) return
        
        viewModelScope.launch {
            // Inform server to disconnect current call and clear states
            matchmakingRepository.endActiveCall()
            _state.value = DiscoveryState.Searching
            
            // Re-join matchmaking queue
            startDiscovery()
        }
    }

    fun toggleLike(targetUserId: String, isLiked: Boolean) {
        viewModelScope.launch {
            userRepository.toggleInteraction(targetUserId, "liked", isLiked)
        }
    }

    fun toggleAdd(targetUserId: String, isAdded: Boolean) {
        viewModelScope.launch {
            userRepository.toggleInteraction(targetUserId, "added", isAdded)
        }
    }

    fun reportUser(targetUserId: String, reason: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val req = RaiseRequestDto(
                    type = "report_user",
                    targetId = targetUserId,
                    reason = reason
                )
                val response = apiService.raiseRequest(req)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message() ?: "Unknown error"
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun sendGift(partnerId: String, gift: com.videoChatting.echat.presentation.call.GiftItem) {
        val currentCoinsVal = _currentCoins.value
        if (currentCoinsVal >= gift.coinCost) {
            _currentCoins.value = currentCoinsVal - gift.coinCost
            sessionManager.updateCoins(_currentCoins.value)
            socketManager.sendGift(partnerId, gift)

            val profile = sessionManager.getUserProfile()
            _activeGiftEvent.value = com.videoChatting.echat.presentation.call.GiftReceivedEvent(
                senderId = profile?.userId ?: "me",
                senderName = "You",
                giftId = gift.id,
                giftName = gift.name,
                coins = gift.coinCost,
                icon = gift.icon,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun sendReaction(partnerId: String, emoji: String) {
        triggerLocalReaction(emoji)
        socketManager.sendReaction(partnerId, emoji)
    }

    private fun triggerLocalReaction(emoji: String) {
        val newParticles = (0..2).map {
            com.videoChatting.echat.presentation.call.FloatingEmojiItem(
                emoji = emoji,
                startXPercent = kotlin.random.Random.nextFloat() * 0.8f - 0.4f
            )
        }
        _floatingEmojis.value = (_floatingEmojis.value + newParticles).takeLast(15)
    }

    override fun onCleared() {
        super.onCleared()
        // Disconnect sockets cleanly to remove user from active queues
        matchmakingRepository.disconnect()
    }
}
