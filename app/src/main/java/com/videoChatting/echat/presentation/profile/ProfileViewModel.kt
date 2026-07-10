package com.videoChatting.echat.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.domain.repository.UserRepository
import com.videoChatting.echat.data.remote.model.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserDto?>(null)
    val userProfile: StateFlow<UserDto?> = _userProfile

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val profile = sessionManager.getUserProfile()
        if (profile != null) {
            _userProfile.value = profile
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onLogoutSuccess()
        }
    }
}
