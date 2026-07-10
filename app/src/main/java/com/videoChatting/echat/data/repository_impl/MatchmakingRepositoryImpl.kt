package com.videoChatting.echat.data.repository_impl

import com.videoChatting.echat.data.remote.SocketEvent
import com.videoChatting.echat.data.remote.SocketManager
import com.videoChatting.echat.domain.repository.MatchmakingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MatchmakingRepositoryImpl @Inject constructor(
    private val socketManager: SocketManager
) : MatchmakingRepository {

    override fun joinQueue(longitude: Double, latitude: Double) {
        socketManager.joinMatchQueue(longitude, latitude)
    }

    override fun leaveQueue() {
        socketManager.leaveMatchQueue()
    }

    override fun endActiveCall() {
        socketManager.endActiveCall()
    }

    override fun observeEvents(): Flow<SocketEvent> {
        return socketManager.matchEvents
    }

    override fun disconnect() {
        socketManager.disconnect()
    }
}
