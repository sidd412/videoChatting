package com.videoChatting.echat.data.repository_impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.videoChatting.echat.domain.repository.MatchResult
import com.videoChatting.echat.domain.repository.MatchmakingRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class MatchmakingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MatchmakingRepository {

    override suspend fun findMatch(currentUserId: String): Result<MatchResult> {
        return try {
            val queueRef = firestore.collection("echat_queue")
            
            // Try to find an available user who is waiting
            val snapshot = queueRef.whereEqualTo("status", "waiting")
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                // Found someone!
                val matchedDoc = snapshot.documents.first()
                val matchedUserId = matchedDoc.id
                
                // Don't match with yourself
                if (matchedUserId != currentUserId) {
                    val channelName = UUID.randomUUID().toString()
                    
                    // Update the waiting user's status to matched
                    queueRef.document(matchedUserId).update(
                        mapOf(
                            "status" to "matched",
                            "matchedWith" to currentUserId,
                            "channelName" to channelName
                        )
                    ).await()
                    
                    // Also update our own status so we exist in the queue as matched
                    queueRef.document(currentUserId).set(
                        mapOf(
                            "status" to "matched",
                            "matchedWith" to matchedUserId,
                            "channelName" to channelName,
                            "timestamp" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                    
                    return Result.success(MatchResult(channelName, matchedUserId))
                }
            }
            
            // No one found, add self to queue
            queueRef.document(currentUserId).set(
                mapOf(
                    "status" to "waiting",
                    "matchedWith" to null,
                    "channelName" to null,
                    "timestamp" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
            
            Result.failure(Exception("Waiting in queue"))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveMatch(currentUserId: String) {
        try {
            firestore.collection("echat_queue").document(currentUserId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun observeMatchStatus(currentUserId: String): Flow<MatchResult?> = callbackFlow {
        val listener = firestore.collection("echat_queue").document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "matched") {
                        val channelName = snapshot.getString("channelName") ?: ""
                        val matchedWith = snapshot.getString("matchedWith") ?: ""
                        if (channelName.isNotEmpty()) {
                            trySend(MatchResult(channelName, matchedWith))
                        }
                    }
                }
            }
            
        awaitClose { listener.remove() }
    }
}
