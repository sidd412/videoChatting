package com.videoChatting.echat.domain.repository

import kotlinx.coroutines.flow.Flow

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val readStatus: Boolean = false
)

interface MessageRepository {
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message)
    suspend fun markAsRead(chatId: String, senderId: String)
}
