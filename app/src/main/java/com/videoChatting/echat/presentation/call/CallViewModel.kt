package com.videoChatting.echat.presentation.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CallViewModel @Inject constructor(
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _coinsBalance = MutableStateFlow(sessionManager.getUserProfile()?.coinsBalance ?: 0)
    val coinsBalance: StateFlow<Int> = _coinsBalance.asStateFlow()

    private val _activeGiftEvent = MutableStateFlow<GiftReceivedEvent?>(null)
    val activeGiftEvent: StateFlow<GiftReceivedEvent?> = _activeGiftEvent.asStateFlow()

    private val _floatingEmojis = MutableStateFlow<List<FloatingEmojiItem>>(emptyList())
    val floatingEmojis: StateFlow<List<FloatingEmojiItem>> = _floatingEmojis.asStateFlow()

    init {
        // Observe live Gift Events from partner
        viewModelScope.launch {
            socketManager.giftReceivedEvents.collect { giftEvent ->
                _activeGiftEvent.value = giftEvent
                // Refresh local balance if recipient received gift
                val profile = sessionManager.getUserProfile()
                _coinsBalance.value = profile?.coinsBalance ?: 0
            }
        }

        // Observe live Floating Reactions from partner
        viewModelScope.launch {
            socketManager.reactionReceivedEvents.collect { emoji ->
                triggerLocalReaction(emoji)
            }
        }

        // Observe balance updates
        viewModelScope.launch {
            socketManager.userStatusEvents.collect {
                val profile = sessionManager.getUserProfile()
                _coinsBalance.value = profile?.coinsBalance ?: 0
            }
        }
    }

    fun sendGift(partnerId: String, gift: GiftItem) {
        val currentCoins = _coinsBalance.value
        if (currentCoins >= gift.coinCost) {
            // Optimistic update
            _coinsBalance.value = currentCoins - gift.coinCost
            sessionManager.updateCoins(_coinsBalance.value)
            
            // Emit via socket
            socketManager.sendGift(partnerId, gift)

            // Also show celebratory animation on sender's screen too!
            val profile = sessionManager.getUserProfile()
            _activeGiftEvent.value = GiftReceivedEvent(
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
        // Trigger locally on sender screen
        triggerLocalReaction(emoji)
        // Send to partner via socket
        socketManager.sendReaction(partnerId, emoji)
    }

    private fun triggerLocalReaction(emoji: String) {
        // Create 2-3 floating emoji particles with randomized positions
        val newParticles = (0..2).map {
            FloatingEmojiItem(
                emoji = emoji,
                startXPercent = Random.nextFloat() * 0.8f - 0.4f
            )
        }
        _floatingEmojis.value = (_floatingEmojis.value + newParticles).takeLast(15)
    }

    fun refreshBalance() {
        val profile = sessionManager.getUserProfile()
        _coinsBalance.value = profile?.coinsBalance ?: 0
    }
}
