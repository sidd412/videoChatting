package com.videoChatting.echat.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatsHomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _interactedUsers = MutableStateFlow<List<DummyChat>>(emptyList())
    val interactedUsers: StateFlow<List<DummyChat>> = _interactedUsers

    fun loadInteractions() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("echat_users")
                    .document(currentUserId)
                    .collection("interactions")
                    .get()
                    .await()
                
                val list = snapshot.documents.map { doc ->
                    val isLiked = doc.getBoolean("liked") == true
                    val isAdded = doc.getBoolean("added") == true
                    
                    var subtitle = ""
                    if (isLiked && isAdded) subtitle = "Liked & Added"
                    else if (isLiked) subtitle = "Liked"
                    else if (isAdded) subtitle = "Added"
                    
                    DummyChat(
                        id = doc.id,
                        name = "User ${doc.id.take(4)}",
                        lastMessage = subtitle,
                        time = "Just now",
                        isLiked = isLiked,
                        isAdded = isAdded
                    )
                }
                _interactedUsers.value = list
            } catch (e: Exception) {
                // Ignore errors for demo
            }
        }
    }
}
