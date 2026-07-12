package com.videoChatting.echat.data.repository_impl

import android.util.Log
import com.videoChatting.echat.data.local.ChatDatabaseHelper
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.SocketManager
import com.videoChatting.echat.domain.repository.Message
import com.videoChatting.echat.domain.repository.MessageRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val socketManager: SocketManager,
    private val dbHelper: ChatDatabaseHelper
) : MessageRepository {

    override fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        // Ensure socket is connected to listen to incoming live messages
        socketManager.connect()

        // 1. Emit cached messages instantly to UI
        val cached = dbHelper.getMessages(chatId)
        trySend(cached)

        // 2. Fetch history from custom REST API
        val fetchJob = launch {
            try {
                val response = apiService.getMessages(chatId)
                if (response.isSuccessful && response.body() != null) {
                    val messagesDto = response.body()!!.messages
                    val domainMessages = messagesDto.map {
                        Message(
                            messageId = it.messageId,
                            chatId = it.chatId,
                            senderId = it.senderId,
                            text = it.text,
                            timestamp = it.timestamp,
                            readStatus = it.readStatus ?: false
                        )
                    }
                    dbHelper.insertMessages(domainMessages)
                    // Emit updated db list
                    trySend(dbHelper.getMessages(chatId))
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Failed to fetch remote messages history: ${e.message}")
            }
        }

        // 3. Listen to real-time chat messages via Socket.io
        val socketJob = launch {
            socketManager.chatMessages.collect { msgDto ->
                if (msgDto.chatId == chatId) {
                    val message = Message(
                        messageId = msgDto.messageId,
                        chatId = msgDto.chatId,
                        senderId = msgDto.senderId,
                        receiverId = "",
                        text = msgDto.text,
                        timestamp = msgDto.timestamp,
                        readStatus = msgDto.readStatus ?: false
                    )
                    dbHelper.insertMessage(message)
                    // Emit updated database list
                    trySend(dbHelper.getMessages(chatId))
                }
            }
        }

        // 4. Listen to read receipts via Socket.io
        val readReceiptJob = launch {
            socketManager.readReceipts.collect { rChatId ->
                if (rChatId == chatId) {
                    dbHelper.markAllAsRead(chatId)
                    // Emit updated database list
                    trySend(dbHelper.getMessages(chatId))
                }
            }
        }

        awaitClose {
            fetchJob.cancel()
            socketJob.cancel()
            readReceiptJob.cancel()
        }
    }

    override suspend fun sendMessage(message: Message) {
        val receiverId = message.receiverId
        
        // Save message locally first to show immediately on sender UI (local-first experience)
        dbHelper.insertMessage(message)
        
        // Emit over sockets
        socketManager.sendMessage(
            messageId = message.messageId,
            chatId = message.chatId,
            text = message.text,
            receiverId = receiverId
        )
    }

    override suspend fun markAsRead(chatId: String, senderId: String) {
        // Update local DB
        // Optionally, we could have a method in dbHelper to mark all messages from senderId in chatId as read
        // For simplicity, just emitting the socket event which is most critical
        socketManager.markAsRead(chatId, senderId)
    }
}
