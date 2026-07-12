package com.videoChatting.echat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.domain.repository.Message
import com.videoChatting.echat.domain.repository.MessageRepository
import com.videoChatting.echat.domain.repository.UserRepository
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.model.RevokeConsentRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val apiService: ApiService,
    private val socketManager: com.videoChatting.echat.data.remote.SocketManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _onlineStatus = MutableStateFlow<String>("")
    val onlineStatus: StateFlow<String> = _onlineStatus.asStateFlow()

    private var currentChatId: String = ""
    val currentUserId: String? = userRepository.getCurrentUserId()

    private var messagesJob: kotlinx.coroutines.Job? = null
    private var statusJob: kotlinx.coroutines.Job? = null

    fun loadMessages(chatId: String, targetUserId: String) {
        currentChatId = chatId
        
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            messageRepository.getMessages(chatId).collect { msgs ->
                _messages.value = msgs
            }
        }
        
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            socketManager.userStatusEvents.collect { (userId, isOnline) ->
                if (userId == targetUserId) {
                    _onlineStatus.value = if (isOnline) "Online" else ""
                }
            }
        }
        
        fetchOnlineStatus(targetUserId)
    }

    private fun fetchOnlineStatus(targetUserId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getUserProfile(targetUserId)
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!.profile
                    _onlineStatus.value = if (profile.isOnline) "Online" else "" // Can expand to format lastSeen later
                }
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    fun sendMessage(text: String, receiverId: String) {
        if (text.isBlank() || currentUserId == null) return
        viewModelScope.launch {
            val messageId = java.util.UUID.randomUUID().toString()
            val message = Message(
                messageId = messageId,
                chatId = currentChatId,
                senderId = currentUserId,
                receiverId = receiverId,
                text = text
            )
            messageRepository.sendMessage(message)
        }
    }

    fun markAsRead(chatId: String, senderId: String) {
        viewModelScope.launch {
            messageRepository.markAsRead(chatId, senderId)
        }
    }

    fun revokeConsent(targetUserId: String, onSuccess: () -> Unit) {
        if (currentUserId == null) return
        viewModelScope.launch {
            try {
                val request = RevokeConsentRequest(currentUserId, targetUserId)
                val response = apiService.revokeConsent(request)
                if (response.isSuccessful) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
