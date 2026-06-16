package com.videoChatting.echat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.domain.repository.Message
import com.videoChatting.echat.domain.repository.MessageRepository
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
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

    fun sendMessage(text: String) {
        if (text.isBlank() || currentUserId == null) return
        viewModelScope.launch {
            val message = Message(
                chatId = currentChatId,
                senderId = currentUserId,
                text = text
            )
            messageRepository.sendMessage(message)
        }
    }
}
