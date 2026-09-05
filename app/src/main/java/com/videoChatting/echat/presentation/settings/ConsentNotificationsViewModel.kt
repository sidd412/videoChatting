package com.videoChatting.echat.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.model.ConsentNotificationDto
import com.videoChatting.echat.data.remote.model.RespondConsentRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentNotificationsViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<ConsentNotificationDto>>(emptyList())
    val notifications: StateFlow<List<ConsentNotificationDto>> = _notifications

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        val userId = sessionManager.getUserProfile()?.userId ?: return
        viewModelScope.launch {
            try {
                val response = apiService.getPendingConsents(userId)
                if (response.isSuccessful) {
                    _notifications.value = response.body()?.notifications ?: emptyList()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun respondToConsent(consentId: String, action: String) { // "allow" or "deny"
        viewModelScope.launch {
            try {
                val request = RespondConsentRequest(consentId, action)
                val response = apiService.respondToConsent(request)
                if (response.isSuccessful) {
                    // Remove from list locally
                    _notifications.value = _notifications.value.filter { it.id != consentId }
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
