package com.videoChatting.echat.data.remote

import android.util.Log
import com.google.gson.Gson
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.model.MessageDto
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val sessionManager: SessionManager
) {
    private var socket: Socket? = null
    private val gson = Gson()

    private val _matchEvents = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 10)
    val matchEvents: SharedFlow<SocketEvent> = _matchEvents.asSharedFlow()

    private val _chatMessages = MutableSharedFlow<MessageDto>(extraBufferCapacity = 100)
    val chatMessages: SharedFlow<MessageDto> = _chatMessages.asSharedFlow()

    private val _readReceipts = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val readReceipts: SharedFlow<String> = _readReceipts.asSharedFlow()

    private val _userStatusEvents = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 10)
    val userStatusEvents: SharedFlow<Pair<String, Boolean>> = _userStatusEvents.asSharedFlow()

    companion object {
        private const val TAG = "SocketManager"
    }

    fun connect() {
        Log.d(TAG, "🔌 connect() called. socket=${socket}, connected=${socket?.connected()}")
        if (socket?.connected() == true) {
            Log.d(TAG, "🔌 Already connected, skipping")
            return
        }

        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
            }
            socket = IO.socket(com.videoChatting.echat.utils.Constants.SOCKET_URL, opts)
            Log.d(TAG, "🔌 Socket created, attaching listeners...")

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅ EVENT_CONNECT fired! Socket is now connected.")
                registerUser()
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "❌ EVENT_CONNECT_ERROR: ${args.getOrNull(0)}")
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d(TAG, "🔌 Socket Disconnected: ${args.getOrNull(0)}")
            }

            socket?.on("searching") {
                Log.d(TAG, "🔍 Server says: searching for a match...")
                _matchEvents.tryEmit(SocketEvent.Searching)
            }

            socket?.on("match_found") { args ->
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    Log.d(TAG, "🔗 Match Found: $data")
                    try {
                        val matchResponse = gson.fromJson(data.toString(), MatchResponse::class.java)
                        _matchEvents.tryEmit(SocketEvent.MatchFound(matchResponse))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing match_found", e)
                    }
                }
            }

            socket?.on("partner_left") {
                Log.d(TAG, "❌ Partner disconnected call")
                _matchEvents.tryEmit(SocketEvent.PartnerLeft)
            }

            socket?.on("match_error") { args ->
                val data = args.getOrNull(0) as? JSONObject
                val message = data?.optString("message") ?: "Unknown match error"
                Log.e(TAG, "❌ Match Error from server: $message")
                _matchEvents.tryEmit(SocketEvent.Error(message))
            }

            socket?.on("receive_message") { args ->
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    Log.d(TAG, "💬 Received Message: $data")
                    try {
                        val message = gson.fromJson(data.toString(), MessageDto::class.java)
                        _chatMessages.tryEmit(message)
                        
                        // Auto mark as read if currently on this chat screen
                        if (com.videoChatting.echat.utils.ActiveChatManager.currentActiveChatId == message.senderId) {
                            markAsRead(message.chatId, message.senderId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing receive_message", e)
                    }
                }
            }

            socket?.on("message_delivered") { args ->
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    Log.d(TAG, "✅ Message Delivered: $data")
                    try {
                        val message = gson.fromJson(data.toString(), MessageDto::class.java)
                        _chatMessages.tryEmit(message)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message_delivered", e)
                    }
                }
            }

            socket?.on("message_read") { args ->
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    Log.d(TAG, "👁️ Message Read: $data")
                    val chatId = data.optString("chatId")
                    if (chatId.isNotEmpty()) {
                        _readReceipts.tryEmit(chatId)
                    }
                }
            }

            socket?.on("user_status_changed") { args ->
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    val userId = data.optString("userId")
                    val isOnline = data.optBoolean("isOnline")
                    if (userId.isNotEmpty()) {
                        _userStatusEvents.tryEmit(Pair(userId, isOnline))
                    }
                }
            }

            socket?.on("wallet_update") { args ->
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    val coinsBalance = data.optInt("coinsBalance", -1)
                    if (coinsBalance >= 0) {
                        Log.d(TAG, "💰 Wallet Updated: $coinsBalance coins")
                        val user = sessionManager.getUserProfile()
                        if (user != null) {
                            sessionManager.updateCoins(coinsBalance)
                            _userStatusEvents.tryEmit(Pair(user.userId, true)) // trigger UI update
                        }
                    }
                }
            }

            socket?.on("wallet_update") { args ->
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    val coinsBalance = data.optInt("coinsBalance")
                    _matchEvents.tryEmit(SocketEvent.WalletUpdate(coinsBalance))
                }
            }

            socket?.on("insufficient_funds") { args ->
                val data = args.getOrNull(0) as? JSONObject
                if (data != null) {
                    val message = data.optString("message", "Insufficient funds")
                    _matchEvents.tryEmit(SocketEvent.InsufficientFunds(message))
                }
            }

            socket?.on("consent_notification") { args ->
                Log.d(TAG, "🔔 Consent Notification Received via Socket!")
                _matchEvents.tryEmit(SocketEvent.ConsentNotification)
            }

            Log.d(TAG, "🔌 Calling socket.connect()...")
            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in connect()", e)
        }
    }

    private fun registerUser() {
        val profile = sessionManager.getUserProfile()
        Log.d(TAG, "👤 registerUser() called. profile=${profile?.userId ?: "NULL"}")
        if (profile == null) {
            Log.e(TAG, "❌ Cannot register user - profile is NULL!")
            return
        }
        
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val fcmToken = if (task.isSuccessful) task.result else null
            val json = JSONObject().put("userId", profile.userId).apply {
                if (fcmToken != null) put("fcmToken", fcmToken)
            }
            socket?.emit("register_user", json)
            Log.d(TAG, "👤 Emitted register_user for ${profile.userId} with fcmToken")
        }
    }

    fun joinMatchQueue(
        longitude: Double,
        latitude: Double
    ) {
        Log.d(TAG, "🎯 joinMatchQueue() called. lng=$longitude, lat=$latitude")
        
        val profile = sessionManager.getUserProfile()
        Log.d(TAG, "🎯 Profile loaded: userId=${profile?.userId ?: "NULL"}, gender=${profile?.gender}, country=${profile?.country}")
        
        if (profile == null) {
            Log.e(TAG, "❌ joinMatchQueue ABORTED - profile is NULL!")
            return
        }
        
        val data = JSONObject().apply {
            put("userId", profile.userId)
            put("name", profile.name)
            put("gender", profile.gender ?: "Not Specified")
            put("age", profile.age ?: 22)
            put("country", profile.country)
            put("longitude", longitude)
            put("latitude", latitude)
            put("prefGender", profile.preferences?.gender ?: "All")
            put("prefMinAge", profile.preferences?.minAge ?: 18)
            put("prefMaxAge", profile.preferences?.maxAge ?: 99)
            put("filterType", profile.preferences?.filterType ?: "country")
            put("kmRadius", profile.preferences?.kmRadius ?: 50)
        }
        
        Log.d(TAG, "🎯 Built queue data: $data")

        val isConnected = socket?.connected() == true
        Log.d(TAG, "🎯 Socket connected? $isConnected, socket object: ${socket != null}")

        if (isConnected) {
            Log.d(TAG, "🎯 ✅ Emitting join_match_queue NOW (socket already connected)")
            socket?.emit("join_match_queue", data)
        } else {
            Log.d(TAG, "🎯 ⏳ Socket not connected. Will connect and emit after EVENT_CONNECT")
            // We need to connect first, then emit join_match_queue after connection
            val currentSocket = socket
            if (currentSocket != null) {
                // Socket exists but not connected - add one-time listener
                Log.d(TAG, "🎯 Socket exists but disconnected, adding one-time connect listener")
                currentSocket.once(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "🎯 ✅ Deferred: Socket connected! Emitting join_match_queue NOW")
                    currentSocket.emit("join_match_queue", data)
                }
                if (!currentSocket.connected()) {
                    currentSocket.connect()
                }
            } else {
                // Socket doesn't exist - create it via connect(), then emit
                Log.d(TAG, "🎯 No socket exists, calling connect() first")
                connect()
                // After connect(), socket should exist. Add one-time listener
                socket?.once(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "🎯 ✅ Deferred (new socket): Socket connected! Emitting join_match_queue NOW")
                    socket?.emit("join_match_queue", data)
                }
            }
        }
    }

    fun leaveMatchQueue() {
        val profile = sessionManager.getUserProfile() ?: return
        val data = JSONObject().apply {
            put("userId", profile.userId)
            put("gender", profile.gender ?: "Not Specified")
            put("country", profile.country)
        }
        if (socket?.connected() == true) {
            socket?.emit("leave_match_queue", data)
        }
    }

    fun endActiveCall() {
        socket?.emit("end_active_call")
    }

    fun sendMessage(messageId: String, chatId: String, text: String, receiverId: String) {
        connect()
        val profile = sessionManager.getUserProfile() ?: return
        val data = JSONObject().apply {
            put("messageId", messageId)
            put("chatId", chatId)
            put("senderId", profile.userId)
            put("text", text)
            put("receiverId", receiverId)
        }
        socket?.emit("send_message", data)
    }

    fun sendInteractionAction(action: String, targetUserId: String) {
        val profile = sessionManager.getUserProfile() ?: return
        val json = JSONObject()
            .put("action", action)
            .put("userId", profile.userId)
            .put("targetUserId", targetUserId)
        Log.d(TAG, "Sending interaction: $json")
        socket?.emit("interaction", json)
    }

    fun markAsRead(chatId: String, senderId: String) {
        val profile = sessionManager.getUserProfile() ?: return
        val json = JSONObject()
            .put("chatId", chatId)
            .put("senderId", senderId)
            .put("receiverId", profile.userId)
        Log.d(TAG, "Sending mark_as_read: $json")
        socket?.emit("mark_as_read", json)
    }

    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting socket...")
        socket?.disconnect()
        socket = null
    }
}

sealed class SocketEvent {
    object Searching : SocketEvent()
    data class MatchFound(val match: MatchResponse) : SocketEvent()
    object PartnerLeft : SocketEvent()
    data class Error(val message: String) : SocketEvent()
    object ConsentNotification : SocketEvent()
    data class WalletUpdate(val coinsBalance: Int) : SocketEvent()
    data class InsufficientFunds(val message: String) : SocketEvent()
}

data class MatchPartner(
    val userId: String,
    val name: String,
    val gender: String,
    val age: Int,
    val country: String
)

data class MatchResponse(
    val channelName: String,
    val token: String,
    val partner: MatchPartner
)
