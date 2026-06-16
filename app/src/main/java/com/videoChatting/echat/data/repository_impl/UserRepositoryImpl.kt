package com.videoChatting.echat.data.repository_impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.videoChatting.echat.domain.model.User
import com.videoChatting.echat.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            firestore.collection("echat_users")
                .document(user.userId)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUser(userId: String): Result<User> {
        return try {
            val snapshot = firestore.collection("echat_users")
                .document(userId)
                .get()
                .await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDiscoverableUsers(): Flow<List<User>> = flow {
        // Generating 20 Dummy Users for Demo Swipe Discovery
        val dummyUsers = listOf(
            User("d1", "Aisha", "Love traveling and music!", "", "Female", "India", true, 100),
            User("d2", "Rahul", "Tech enthusiast", "", "Male", "India", false, 50),
            User("d3", "Priya", "Coffee & Books", "", "Female", "UK", true, 200),
            User("d4", "John", "Adventure seeker", "", "Male", "USA", true, 10),
            User("d5", "Sneha", "Art and Design", "", "Female", "India", false, 30),
            User("d6", "Michael", "Fitness freak", "", "Male", "Canada", true, 0),
            User("d7", "Emma", "Nature lover", "", "Female", "Australia", true, 500),
            User("d8", "Amit", "Gamer and coder", "", "Male", "India", false, 150),
            User("d9", "Sophia", "Foodie forever", "", "Female", "Italy", true, 40),
            User("d10", "David", "Photographer", "", "Male", "USA", false, 80),
            User("d11", "Kriti", "Fashion and styling", "", "Female", "India", true, 300),
            User("d12", "James", "Musician", "", "Male", "UK", true, 120),
            User("d13", "Neha", "Dancing is life", "", "Female", "India", false, 60),
            User("d14", "Daniel", "Car enthusiast", "", "Male", "Germany", true, 90),
            User("d15", "Olivia", "Movie buff", "", "Female", "USA", true, 210),
            User("d16", "Vikram", "Entrepreneur", "", "Male", "India", false, 400),
            User("d17", "Mia", "Yoga instructor", "", "Female", "Spain", true, 75),
            User("d18", "William", "Writer", "", "Male", "UK", false, 25),
            User("d19", "Ananya", "Singer", "", "Female", "India", true, 180),
            User("d20", "Lucas", "Surfer", "", "Male", "Australia", true, 55)
        )
        emit(dummyUsers)
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun toggleInteraction(targetUserId: String, interactionType: String, isActive: Boolean): Result<Unit> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
        return try {
            val docRef = firestore.collection("echat_users").document(currentUserId)
                .collection("interactions").document(targetUserId)
            
            if (isActive) {
                docRef.set(mapOf(interactionType to true)).await()
            } else {
                docRef.update(interactionType, com.google.firebase.firestore.FieldValue.delete()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
