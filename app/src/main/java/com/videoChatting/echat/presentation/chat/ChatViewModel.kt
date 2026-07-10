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
    private val apiService: ApiService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var currentChatId: String = ""
    val currentUserId: String? = userRepository.getCurrentUserId()

    fun loadMessages(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            messageRepository.getMessages(chatId).collect { msgs ->
                _messages.value = msgs
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
