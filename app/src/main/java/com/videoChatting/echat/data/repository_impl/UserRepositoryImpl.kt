package com.videoChatting.echat.data.repository_impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.domain.model.User
import com.videoChatting.echat.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : UserRepository {

    override suspend fun saveUser(user: User): Result<Unit> {
        // Save user profile state locally in session manager
        val currentProfile = sessionManager.getUserProfile()
        if (currentProfile != null) {
            val updatedProfile = currentProfile.copy(
                name = user.name,
                avatar = user.profileImage,
                country = user.country
            )
            sessionManager.saveUserProfile(updatedProfile)
        }
        
        // In the future, we will add an API call to sync changes back to MongoDB
        return Result.success(Unit)
    }

    override suspend fun getUser(userId: String): Result<User> {
        // Retrieve profile details from local session storage
        val profile = sessionManager.getUserProfile()
        if (profile != null && profile.userId == userId) {
            return Result.success(
                User(
                    userId = profile.userId,
                    name = profile.name,
                    profileImage = profile.avatar ?: "",
                    gender = profile.gender ?: "Not Specified",
                    country = profile.country,
                    isOnline = true,
                    availableMinutes = profile.coinsBalance ?: 100
                )
            )
        }
        
        return Result.failure(Exception("User profile not found in cache"))
    }

    override fun getDiscoverableUsers(): Flow<List<User>> = flow {
        // Dummy users flow for swipe interactions compatibility
        val dummyUsers = listOf(
            User("d1", "Aisha", "Love traveling and music!", "", "Female", "India", true, 100),
            User("d2", "Rahul", "Tech enthusiast", "", "Male", "India", false, 50),
            User("d3", "Priya", "Coffee & Books", "", "Female", "UK", true, 200),
            User("d4", "John", "Adventure seeker", "", "Male", "USA", true, 10)
        )
        emit(dummyUsers)
    }

    override fun getCurrentUserId(): String? {
        // Retrieve userId directly from custom session JWT registry instead of Firebase Authentication state
        return sessionManager.getUserProfile()?.userId
    }

    override suspend fun toggleInteraction(
        targetUserId: String,
        interactionType: String,
        isActive: Boolean
    ): Result<Unit> {
        return try {
            val response = apiService.toggleInteraction(
                com.videoChatting.echat.data.remote.model.ToggleInteractionRequest(
                    targetUserId = targetUserId,
                    interactionType = interactionType,
                    isActive = isActive
                )
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to sync interaction"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
