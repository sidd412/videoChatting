package com.videoChatting.echat.domain.repository

import com.videoChatting.echat.data.remote.SocketEvent
import kotlinx.coroutines.flow.Flow

interface MatchmakingRepository {
    fun joinQueue(longitude: Double, latitude: Double)
    fun leaveQueue()
    fun endActiveCall()
    fun observeEvents(): Flow<SocketEvent>
    fun disconnect()
}
