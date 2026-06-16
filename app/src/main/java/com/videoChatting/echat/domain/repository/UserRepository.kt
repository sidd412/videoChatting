package com.videoChatting.echat.domain.repository

import com.videoChatting.echat.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun getUser(userId: String): Result<User>
    fun getDiscoverableUsers(): Flow<List<User>> // For dummy swipe data
    fun getCurrentUserId(): String?
    suspend fun toggleInteraction(targetUserId: String, interactionType: String, isActive: Boolean): Result<Unit>
}
