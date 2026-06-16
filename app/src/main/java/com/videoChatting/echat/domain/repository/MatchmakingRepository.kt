package com.videoChatting.echat.domain.repository

import kotlinx.coroutines.flow.Flow

data class MatchResult(
    val channelName: String,
    val matchedUserId: String
)

interface MatchmakingRepository {
    suspend fun findMatch(currentUserId: String): Result<MatchResult>
    suspend fun leaveMatch(currentUserId: String)
    fun observeMatchStatus(currentUserId: String): Flow<MatchResult?>
}
