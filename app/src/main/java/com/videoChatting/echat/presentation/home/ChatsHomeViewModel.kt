package com.videoChatting.echat.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsHomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _interactedUsers = MutableStateFlow<List<DummyChat>>(emptyList())
    val interactedUsers: StateFlow<List<DummyChat>> = _interactedUsers

    private val _pendingConsentCount = MutableStateFlow(0)
    val pendingConsentCount: StateFlow<Int> = _pendingConsentCount

    fun loadInteractions() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                // Fetch interactions
                val response = apiService.getInteractions()
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.interactions.map { item ->
                        DummyChat(
                            id = item.id,
                            name = item.name,
                            lastMessage = item.lastMessage,
                            time = item.time,
                            isLiked = item.isLiked,
                            isAdded = item.isAdded
                        )
                    }
                    _interactedUsers.value = list
                }

                // Fetch pending consents count
                val consentResponse = apiService.getPendingConsents(currentUserId)
                if (consentResponse.isSuccessful && consentResponse.body() != null) {
                    _pendingConsentCount.value = consentResponse.body()!!.notifications.size
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
